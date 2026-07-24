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

/**
 * Hits the real Messages API. Skipped — silently, so CI stays green — unless a developer key is
 * available in `ANTHROPIC_API_KEY` or as `anthropic.api.key` in `local.properties`.
 *
 * Its captured stream is what seeds the fixtures in `SseFixtures`; see
 * `scripts/record-sse-fixture.sh`.
 */
class ClaudeChatEngineIntegrationTest {
    private fun devKey(): String? {
        System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }?.let { return it }
        val properties = File("../local.properties").takeIf { it.exists() } ?: return null
        return properties
            .inputStream()
            .use { Properties().apply { load(it) } }
            .getProperty("anthropic.api.key")
            ?.takeIf { it.isNotBlank() }
    }

    @Test
    fun streams_a_real_turn_end_to_end() =
        runTest(timeout = 2.minutes) {
            val key = devKey()
            if (key == null) {
                println("SKIPPED: no dev key in ANTHROPIC_API_KEY or local.properties.")
                return@runTest
            }

            val engine = createChatEngine { key }
            val request =
                ChatRequest(
                    messages =
                        listOf(
                            ChatMessage(
                                role = Role.User,
                                content = listOf(ContentBlock.Text("Reply with exactly: Hello")),
                            ),
                        ),
                    maxTokens = 64,
                )

            val events = engine.stream(request).toList()

            val completed = assertIs<ChatStreamEvent.Completed>(events.last())
            assertEquals(StopReason.EndTurn, completed.stopReason)
            assertTrue(completed.usage.inputTokens > 0, "expected input tokens, got $completed")
            assertTrue(completed.usage.outputTokens > 0, "expected output tokens, got $completed")

            val text = events.filterIsInstance<ChatStreamEvent.Delta>().joinToString("") { it.text }
            assertTrue(text.contains("Hello"), "expected 'Hello' in reply, got: $text")
        }

    @Test
    fun a_bad_key_maps_to_authentication() =
        runTest(timeout = 2.minutes) {
            if (devKey() == null) {
                println("SKIPPED: no dev key in ANTHROPIC_API_KEY or local.properties.")
                return@runTest
            }

            val engine = createChatEngine { "sk-ant-definitely-not-valid" }
            val request =
                ChatRequest(
                    messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))),
                )

            val events = engine.stream(request).toList()

            assertEquals(
                ChatError.Authentication,
                assertIs<ChatStreamEvent.Failed>(events.single()).error,
            )
        }
}
