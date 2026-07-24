package com.shayanaryan.chatbot.shared.chat

import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ClaudeChatEngineErrorTest {
    private val request =
        ChatRequest(messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))))

    private suspend fun errorFor(status: Int): ChatError {
        val events =
            testChatEngine {
                respondError(
                    HttpStatusCode(status, "mapped"),
                    """{"type":"error","error":{"type":"api_error","message":"boom"}}""",
                )
            }.stream(request).toList()
        return assertIs<ChatStreamEvent.Failed>(events.single()).error
    }

    @Test
    fun maps_authentication_statuses() =
        runTest {
            assertEquals(ChatError.Authentication, errorFor(401))
            assertEquals(ChatError.Authentication, errorFor(403))
        }

    @Test
    fun maps_client_server_and_overload_statuses() =
        runTest {
            assertEquals(ChatError.InvalidRequest, errorFor(400))
            assertEquals(ChatError.InvalidRequest, errorFor(404))
            assertEquals(ChatError.InvalidRequest, errorFor(413))
            assertEquals(ChatError.Server, errorFor(500))
            assertEquals(ChatError.Overloaded, errorFor(529))
        }

    @Test
    fun maps_timeout_statuses() =
        runTest {
            assertEquals(ChatError.Timeout, errorFor(408))
            assertEquals(ChatError.Timeout, errorFor(504))
        }

    @Test
    fun parses_retry_after_on_a_rate_limit() =
        runTest {
            val events =
                testChatEngine {
                    respondError(
                        HttpStatusCode.TooManyRequests,
                        "",
                        headersOf("retry-after", "42"),
                    )
                }.stream(request).toList()

            assertEquals(
                ChatError.RateLimited(42),
                assertIs<ChatStreamEvent.Failed>(events.single()).error,
            )
        }

    @Test
    fun rate_limit_without_a_usable_header_carries_no_hint() =
        runTest {
            assertEquals(ChatError.RateLimited(null), errorFor(429))
        }

    @Test
    fun a_mid_stream_error_event_terminates_the_flow() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.MID_STREAM_ERROR) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Delta("Hel"), events.first())
            assertEquals(ChatStreamEvent.Failed(ChatError.Overloaded), events.last())
            assertEquals(2, events.size)
        }

    @Test
    fun stops_at_the_first_terminal_and_ignores_trailing_frames() =
        runTest {
            val trailing =
                SseFixtures.HAPPY_PATH +
                    "\n\nevent: error\ndata: " +
                    """{"type":"error","error":{"type":"overloaded_error"}}""" + "\n\n"
            val events =
                testChatEngine { respondSse(trailing) }.stream(request).toList()

            assertEquals(
                1,
                events.count { it is ChatStreamEvent.Completed || it is ChatStreamEvent.Failed },
            )
            assertIs<ChatStreamEvent.Completed>(events.last())
        }

    @Test
    fun a_malformed_frame_is_unexpected() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.MALFORMED_JSON) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.single())
        }

    @Test
    fun a_truncated_stream_is_unexpected() =
        runTest {
            val events =
                testChatEngine { respondSse(SseFixtures.TRUNCATED) }.stream(request).toList()

            assertEquals(ChatStreamEvent.Delta("Hel"), events.first())
            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.last())
        }

    @Test
    fun lost_connectivity_is_a_network_error() =
        runTest {
            val events =
                testChatEngine { throw IOException("unreachable") }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Network), events.single())
        }

    @Test
    fun a_failing_key_provider_surfaces_as_unexpected() =
        runTest {
            val engine =
                testChatEngine(
                    keyProvider = ApiKeyProvider { error("no key stored") },
                ) { respondSse(SseFixtures.HAPPY_PATH) }

            val events = engine.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Unexpected), events.single())
        }

    @Test
    fun a_byte_gap_stall_is_a_timeout() =
        runTest {
            val events =
                testChatEngine {
                    throw io.ktor.client.network.sockets
                        .SocketTimeoutException("stalled")
                }.stream(request).toList()

            assertEquals(ChatStreamEvent.Failed(ChatError.Timeout), events.single())
        }
}
