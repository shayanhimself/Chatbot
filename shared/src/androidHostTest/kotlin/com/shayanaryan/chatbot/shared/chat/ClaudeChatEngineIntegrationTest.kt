package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private const val API_KEY_ENVIRONMENT_VARIABLE = "ANTHROPIC_API_KEY"
private const val LOCAL_PROPERTIES_PATH = "../local.properties"
private const val API_KEY_PROPERTY = "anthropic.api.key"
private const val SKIP_MESSAGE = "SKIPPED: no dev key in ANTHROPIC_API_KEY or local.properties."

// The reply is pinned so the assertion can be exact rather than a guess at what the model says.
private const val EXACT_REPLY_PROMPT = "Reply with exactly: Hello"
private const val EXPECTED_REPLY = "Hello"

private const val MAX_TOKENS = 64
private const val REJECTED_KEY = "sk-ant-definitely-not-valid"
private const val USER_MESSAGE = "hi"

/**
 * Hits the real Messages API. Skipped — silently, so CI stays green — unless a developer key is
 * available in `ANTHROPIC_API_KEY` or as `anthropic.api.key` in `local.properties`.
 *
 * Its captured stream is what seeds the fixtures in `SseFixtures`; see
 * `scripts/record-sse-fixture.sh`.
 */
class ClaudeChatEngineIntegrationTest {
    private fun devKey(): String? {
        System.getenv(API_KEY_ENVIRONMENT_VARIABLE)?.takeIf { it.isNotBlank() }?.let { return it }
        val properties = File(LOCAL_PROPERTIES_PATH).takeIf { it.exists() } ?: return null
        return properties
            .inputStream()
            .use { Properties().apply { load(it) } }
            .getProperty(API_KEY_PROPERTY)
            ?.takeIf { it.isNotBlank() }
    }

    @Test
    fun `streams a real turn end to end`() =
        runTest(timeout = 2.minutes) {
            val key = devKey()
            if (key == null) {
                println(SKIP_MESSAGE)
                return@runTest
            }

            val engine = createChatEngine(createChatHttpClient()) { key }
            val request =
                ChatRequest(
                    messages =
                        listOf(
                            ChatMessage(
                                role = Role.User,
                                content = listOf(ContentBlock.Text(EXACT_REPLY_PROMPT)),
                            ),
                        ),
                    maxTokens = MAX_TOKENS,
                )

            val events = engine.stream(request).toList()

            val completed = assertIs<ChatStreamEvent.Completed>(events.last())
            assertEquals(StopReason.EndTurn, completed.stopReason)
            assertTrue(completed.usage.inputTokens > 0, "expected input tokens, got $completed")
            assertTrue(completed.usage.outputTokens > 0, "expected output tokens, got $completed")

            val text = events.filterIsInstance<ChatStreamEvent.Delta>().joinToString("") { it.text }
            assertTrue(
                text.contains(EXPECTED_REPLY),
                "expected '$EXPECTED_REPLY' in reply, got: $text",
            )
        }

    @Test
    fun `a bad key maps to authentication`() =
        runTest(timeout = 2.minutes) {
            if (devKey() == null) {
                println(SKIP_MESSAGE)
                return@runTest
            }

            val engine = createChatEngine(createChatHttpClient()) { REJECTED_KEY }
            val request =
                ChatRequest(
                    messages =
                        listOf(
                            ChatMessage(Role.User, listOf(ContentBlock.Text(USER_MESSAGE))),
                        ),
                )

            val events = engine.stream(request).toList()

            assertEquals(
                ChatError.Authentication,
                assertIs<ChatStreamEvent.Failed>(events.single()).error,
            )
        }
}
