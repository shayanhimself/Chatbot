package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.model.ClaudeModel
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClaudeChatEngineRequestTest {
    private var captured: HttpRequestData? = null

    private fun engine(apiKey: String = "sk-ant-test") =
        testChatEngine(apiKey) { request ->
            captured = request
            respondSse(SseFixtures.HAPPY_PATH)
        }

    private fun capturedBody(): JsonObject =
        Json.parseToJsonElement((captured!!.body as TextContent).text).jsonObject

    private val request =
        ChatRequest(
            messages =
                listOf(
                    ChatMessage(Role.User, listOf(ContentBlock.Text("hi"))),
                    ChatMessage(Role.Assistant, listOf(ContentBlock.Text("hello"))),
                ),
            model = ClaudeModel.Haiku,
            system = "be brief",
            maxTokens = 512,
        )

    @Test
    fun posts_to_the_messages_endpoint_with_the_required_headers() =
        runTest {
            engine().stream(request).toList()

            val sent = captured!!
            assertEquals("https://api.anthropic.com/v1/messages", sent.url.toString())
            assertEquals("POST", sent.method.value)
            assertEquals("sk-ant-test", sent.headers["x-api-key"])
            assertEquals("2023-06-01", sent.headers["anthropic-version"])
            assertEquals("text/event-stream", sent.headers["Accept"])
            assertEquals("application/json", (sent.body as TextContent).contentType.toString())
        }

    @Test
    fun asks_the_key_provider_for_a_fresh_key_on_every_call() =
        runTest {
            var calls = 0
            val counting =
                testChatEngine(
                    keyProvider =
                        ApiKeyProvider {
                            calls++
                            "key-$calls"
                        },
                ) {
                    captured = it
                    respondSse(SseFixtures.HAPPY_PATH)
                }

            counting.stream(request).toList()
            counting.stream(request).toList()

            assertEquals(2, calls)
            assertEquals("key-2", captured!!.headers["x-api-key"])
        }

    @Test
    fun serialises_the_body_in_the_wire_shape() =
        runTest {
            engine().stream(request).toList()

            val body = capturedBody()
            assertEquals("claude-haiku-4-5", body["model"]!!.jsonPrimitive.content)
            assertEquals(512, body["max_tokens"]!!.jsonPrimitive.int)
            assertEquals("be brief", body["system"]!!.jsonPrimitive.content)
            assertEquals(true, body["stream"]!!.jsonPrimitive.boolean)

            val messages = body["messages"]!!.jsonArray
            assertEquals(2, messages.size)
            assertEquals("user", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
            assertEquals("assistant", messages[1].jsonObject["role"]!!.jsonPrimitive.content)

            val block =
                messages[0]
                    .jsonObject["content"]!!
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals("text", block["type"]!!.jsonPrimitive.content)
            assertEquals("hi", block["text"]!!.jsonPrimitive.content)
        }

    @Test
    fun omits_system_entirely_when_absent() =
        runTest {
            engine().stream(request.copy(system = null)).toList()

            assertNull(capturedBody()["system"])
        }

    @Test
    fun disables_thinking() =
        runTest {
            engine().stream(request).toList()

            val thinking = capturedBody()["thinking"]!!.jsonObject
            assertEquals("disabled", thinking["type"]!!.jsonPrimitive.content)
        }
}
