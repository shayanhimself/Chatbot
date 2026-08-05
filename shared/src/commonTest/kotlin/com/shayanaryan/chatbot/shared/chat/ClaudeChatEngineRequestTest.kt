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

private const val API_KEY = "sk-ant-test"
private const val USER_MESSAGE = "hi"
private const val ASSISTANT_MESSAGE = "hello"
private const val SYSTEM_PROMPT = "be brief"
private const val MAX_TOKENS = 512

// The provider hands out a different key per call, so the last one names the call it came from.
private const val ROTATING_KEY_PREFIX = "key-"
private const val SECOND_ROTATED_KEY = "key-2"

// The wire contract: the endpoint, the headers it requires, and the field names of its body.
private const val MESSAGES_ENDPOINT = "https://api.anthropic.com/v1/messages"
private const val POST_METHOD = "POST"
private const val API_KEY_HEADER = "x-api-key"
private const val API_VERSION_HEADER = "anthropic-version"
private const val API_VERSION = "2023-06-01"
private const val ACCEPT_HEADER = "Accept"
private const val EVENT_STREAM_CONTENT_TYPE = "text/event-stream"
private const val JSON_CONTENT_TYPE = "application/json"

private const val MODEL_FIELD = "model"
private const val MAX_TOKENS_FIELD = "max_tokens"
private const val SYSTEM_FIELD = "system"
private const val STREAM_FIELD = "stream"
private const val MESSAGES_FIELD = "messages"
private const val ROLE_FIELD = "role"
private const val CONTENT_FIELD = "content"
private const val TYPE_FIELD = "type"
private const val TEXT_FIELD = "text"
private const val THINKING_FIELD = "thinking"

private const val HAIKU_MODEL_ID = "claude-haiku-4-5"
private const val USER_ROLE = "user"
private const val ASSISTANT_ROLE = "assistant"
private const val TEXT_BLOCK_TYPE = "text"
private const val THINKING_DISABLED = "disabled"

private const val MESSAGE_COUNT = 2
private const val KEY_PROVIDER_CALLS = 2

class ClaudeChatEngineRequestTest {
    private var captured: HttpRequestData? = null

    private fun engine(apiKey: String = API_KEY) =
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
                    ChatMessage(Role.User, listOf(ContentBlock.Text(USER_MESSAGE))),
                    ChatMessage(Role.Assistant, listOf(ContentBlock.Text(ASSISTANT_MESSAGE))),
                ),
            model = ClaudeModel.Haiku,
            system = SYSTEM_PROMPT,
            maxTokens = MAX_TOKENS,
        )

    @Test
    fun `posts to the messages endpoint with the required headers`() =
        runTest {
            engine().stream(request).toList()

            val sent = captured!!
            assertEquals(MESSAGES_ENDPOINT, sent.url.toString())
            assertEquals(POST_METHOD, sent.method.value)
            assertEquals(API_KEY, sent.headers[API_KEY_HEADER])
            assertEquals(API_VERSION, sent.headers[API_VERSION_HEADER])
            assertEquals(EVENT_STREAM_CONTENT_TYPE, sent.headers[ACCEPT_HEADER])
            assertEquals(JSON_CONTENT_TYPE, (sent.body as TextContent).contentType.toString())
        }

    @Test
    fun `asks the key provider for a fresh key on every call`() =
        runTest {
            var calls = 0
            val counting =
                testChatEngine(
                    keyProvider =
                        ApiKeyProvider {
                            calls++
                            "$ROTATING_KEY_PREFIX$calls"
                        },
                ) {
                    captured = it
                    respondSse(SseFixtures.HAPPY_PATH)
                }

            counting.stream(request).toList()
            counting.stream(request).toList()

            assertEquals(KEY_PROVIDER_CALLS, calls)
            assertEquals(SECOND_ROTATED_KEY, captured!!.headers[API_KEY_HEADER])
        }

    @Test
    fun `serialises the body in the wire shape`() =
        runTest {
            engine().stream(request).toList()

            val body = capturedBody()
            assertEquals(HAIKU_MODEL_ID, body[MODEL_FIELD]!!.jsonPrimitive.content)
            assertEquals(MAX_TOKENS, body[MAX_TOKENS_FIELD]!!.jsonPrimitive.int)
            assertEquals(SYSTEM_PROMPT, body[SYSTEM_FIELD]!!.jsonPrimitive.content)
            assertEquals(true, body[STREAM_FIELD]!!.jsonPrimitive.boolean)

            val messages = body[MESSAGES_FIELD]!!.jsonArray
            assertEquals(MESSAGE_COUNT, messages.size)
            assertEquals(USER_ROLE, messages[0].jsonObject[ROLE_FIELD]!!.jsonPrimitive.content)
            assertEquals(ASSISTANT_ROLE, messages[1].jsonObject[ROLE_FIELD]!!.jsonPrimitive.content)

            val block =
                messages[0]
                    .jsonObject[CONTENT_FIELD]!!
                    .jsonArray
                    .single()
                    .jsonObject
            assertEquals(TEXT_BLOCK_TYPE, block[TYPE_FIELD]!!.jsonPrimitive.content)
            assertEquals(USER_MESSAGE, block[TEXT_FIELD]!!.jsonPrimitive.content)
        }

    @Test
    fun `omits system entirely when absent`() =
        runTest {
            engine().stream(request.copy(system = null)).toList()

            assertNull(capturedBody()[SYSTEM_FIELD])
        }

    @Test
    fun `disables thinking`() =
        runTest {
            engine().stream(request).toList()

            val thinking = capturedBody()[THINKING_FIELD]!!.jsonObject
            assertEquals(THINKING_DISABLED, thinking[TYPE_FIELD]!!.jsonPrimitive.content)
        }
}
