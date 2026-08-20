package com.shayanaryan.chatbot.shared.claude

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private const val MODELS_PAGE_SIZE = 1000
private const val DATA_FIELD = "data"
private const val ID_FIELD = "id"

private const val API_TIMEOUT_MINUTES = 2

private const val PING_MESSAGE = "hi"
private const val MAX_TOKENS = 8

/**
 * Tests whether every Claude model the picker offers is legit.
 *
 * Hits the real API.
 */
class ClaudeModelAvailabilityTest {
    @Test
    fun `every model the picker offers is one the API lists`() =
        runTest(timeout = API_TIMEOUT_MINUTES.minutes) {
            val key = devApiKey()
            if (key == null) {
                println(SKIP_MESSAGE)
                return@runTest
            }

            val body =
                createClaudeHttpClient()
                    .client
                    .get(MODELS_URL) {
                        header(API_KEY_HEADER, key)
                        header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                        parameter(LIMIT_PARAM, MODELS_PAGE_SIZE)
                    }.bodyAsText()
            val listed =
                Json
                    .parseToJsonElement(body)
                    .jsonObject
                    .getValue(DATA_FIELD)
                    .jsonArray
                    .map {
                        it.jsonObject
                            .getValue(ID_FIELD)
                            .jsonPrimitive.content
                    }

            ClaudeModel.entries.forEach { model ->
                assertTrue(
                    // Model id either exactly matches, or it has a date postfix
                    // e.g. claude-haiku-4-5-20251001
                    actual = listed.any { it == model.id || it.startsWith("${model.id}-") },
                    message = "${model.id} is not one of $listed",
                )
            }
        }

    @Test
    fun `every model the picker offers answers`() =
        runTest(timeout = API_TIMEOUT_MINUTES.minutes) {
            val key = devApiKey()
            if (key == null) {
                println(SKIP_MESSAGE)
                return@runTest
            }

            val engine =
                createClaudeEngine(createClaudeHttpClient(), FakeApiKeyRepository(initialKey = key))

            ClaudeModel.entries.forEach { model ->
                val request =
                    ClaudeMessageRequest(
                        messages =
                            listOf(
                                ClaudeMessage(Role.User, listOf(ContentBlock.Text(PING_MESSAGE))),
                            ),
                        model = model,
                        maxTokens = MAX_TOKENS,
                    )

                val events = engine.stream(request).toList()

                assertIs<ClaudeStreamEvent.Completed>(
                    events.last(),
                    "${model.id} did not complete: $events",
                )
            }
        }
}
