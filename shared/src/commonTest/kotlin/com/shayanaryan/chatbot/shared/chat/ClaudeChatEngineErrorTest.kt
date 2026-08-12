package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val USER_MESSAGE = "hi"
private const val MAPPED_STATUS_DESCRIPTION = "mapped"
private const val ERROR_BODY = """{"type":"error","error":{"type":"api_error","message":"boom"}}"""
private const val EMPTY_BODY = ""
private const val RETRY_AFTER_HEADER = "retry-after"
private const val RETRY_AFTER_SECONDS = "42"
private const val PARTIAL_REPLY = "Hel"

// An error frame the server sends mid-stream, after some text has already arrived.
private const val OVERLOADED_ERROR_FRAME =
    "\n\nevent: error\ndata: " + """{"type":"error","error":{"type":"overloaded_error"}}""" + "\n\n"

private const val UNREACHABLE_HOST_MESSAGE = "unreachable"
private const val STALLED_SOCKET_MESSAGE = "stalled"

class ClaudeChatEngineErrorTest {
    private val request =
        ChatRequest(
            messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text(USER_MESSAGE)))),
        )

    private suspend fun errorFor(status: Int): ChatError {
        val events =
            testChatEngine {
                respondError(
                    HttpStatusCode(status, MAPPED_STATUS_DESCRIPTION),
                    ERROR_BODY,
                )
            }.stream(request).toList()
        return assertIs<ChatStreamEvent.Failed>(events.single()).error
    }

    @Test
    fun `maps authentication statuses`() =
        runTest {
            assertEquals(ChatError.Authentication, errorFor(401))
            assertEquals(ChatError.Authentication, errorFor(403))
        }

    @Test
    fun `maps client server and overload statuses`() =
        runTest {
            assertEquals(ChatError.InvalidRequest, errorFor(400))
            assertEquals(ChatError.InvalidRequest, errorFor(404))
            assertEquals(ChatError.InvalidRequest, errorFor(413))
            assertEquals(ChatError.Server, errorFor(500))
            assertEquals(ChatError.Overloaded, errorFor(529))
        }

    @Test
    fun `maps timeout statuses`() =
        runTest {
            assertEquals(ChatError.Timeout, errorFor(408))
            assertEquals(ChatError.Timeout, errorFor(504))
        }

    @Test
    fun `parses retry after on a rate limit`() =
        runTest {
            val events =
                testChatEngine {
                    respondError(
                        HttpStatusCode.TooManyRequests,
                        EMPTY_BODY,
                        headersOf(RETRY_AFTER_HEADER, RETRY_AFTER_SECONDS),
                    )
                }.stream(request).toList()

            assertEquals(
                ChatError.RateLimited(42),
                assertIs<ChatStreamEvent.Failed>(events.single()).error,
            )
        }

    @Test
    fun `rate limit without a usable header carries no hint`() =
        runTest {
            assertEquals(ChatError.RateLimited(null), errorFor(429))
        }

    @Test
    fun `a mid stream error event terminates the flow`() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.MID_STREAM_ERROR) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Delta(PARTIAL_REPLY), events.first())
            assertEquals(ChatStreamEvent.Failed(ChatError.Overloaded), events.last())
            assertEquals(2, events.size)
        }

    @Test
    fun `stops at the first terminal and ignores trailing frames`() =
        runTest {
            val trailing =
                SseFixtures.HAPPY_PATH +
                    OVERLOADED_ERROR_FRAME
            val events =
                testChatEngine { respondSse(trailing) }.stream(request).toList()

            assertEquals(
                1,
                events.count { it is ChatStreamEvent.Completed || it is ChatStreamEvent.Failed },
            )
            assertIs<ChatStreamEvent.Completed>(events.last())
        }

    @Test
    fun `a malformed frame is unexpected`() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.MALFORMED_JSON) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.single())
        }

    @Test
    fun `a truncated stream is unexpected`() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.TRUNCATED) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Delta(PARTIAL_REPLY), events.first())
            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.last())
        }

    @Test
    fun `lost connectivity is a network error`() =
        runTest {
            val events =
                testChatEngine {
                    throw IOException(
                        UNREACHABLE_HOST_MESSAGE,
                    )
                }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Network), events.single())
        }

    @Test
    fun `an empty key store surfaces as unexpected`() =
        runTest {
            val engine =
                testChatEngine(
                    FakeApiKeyRepository(),
                ) { respondSse(SseFixtures.HAPPY_PATH) }

            val events = engine.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.single())
        }

    @Test
    fun `a byte gap stall is a timeout`() =
        runTest {
            val events =
                testChatEngine {
                    throw io.ktor.client.network.sockets
                        .SocketTimeoutException(STALLED_SOCKET_MESSAGE)
                }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Timeout), events.single())
        }
}
