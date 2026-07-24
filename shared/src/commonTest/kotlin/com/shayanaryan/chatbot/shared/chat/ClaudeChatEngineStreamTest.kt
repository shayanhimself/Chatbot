package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ClaudeChatEngineStreamTest {
    private val request =
        ChatRequest(messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))))

    private suspend fun eventsFor(fixture: String): List<ChatStreamEvent> =
        testChatEngine { respondSse(fixture) }.stream(request).toList()

    @Test
    fun emits_one_delta_per_text_delta_then_completes() =
        runTest {
            val events = eventsFor(SseFixtures.HAPPY_PATH)

            val deltas = events.dropLast(1)
            assertEquals(4, deltas.size)
            assertEquals(
                "**SSE (Server-Sent Events)** is a web technology that lets a server push " +
                    "real-time updates to a client over a single, long-lived HTTP connection. " +
                    "It's a one-way communication channel—only the server sends data to the " +
                    "client—commonly used for live feeds, notifications, or streaming updates.",
                deltas.filterIsInstance<ChatStreamEvent.Delta>().joinToString("") { it.text },
            )
            assertEquals(
                ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(28, 93)),
                events.last(),
            )
        }

    @Test
    fun maps_a_refusal_stop_reason() =
        runTest {
            val completed =
                assertIs<ChatStreamEvent.Completed>(eventsFor(SseFixtures.REFUSAL).last())

            assertEquals(StopReason.Refusal, completed.stopReason)
        }

    @Test
    fun maps_an_unrecognised_stop_reason_to_unknown() =
        runTest {
            val fixture =
                SseFixtures.HAPPY_PATH.replace(
                    "\"stop_reason\":\"end_turn\"",
                    "\"stop_reason\":\"tool_use\"",
                )
            val completed = assertIs<ChatStreamEvent.Completed>(eventsFor(fixture).last())

            assertEquals(StopReason.Unknown, completed.stopReason)
        }

    @Test
    fun ignores_thinking_deltas_and_non_delta_frames() =
        runTest {
            val events = eventsFor(SseFixtures.THINKING_ONLY_DELTA)

            assertEquals(listOf(ChatStreamEvent.Delta("Done")), events.dropLast(1))
            assertEquals(
                ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(9, 3)),
                events.last(),
            )
        }

    @Test
    fun emits_exactly_one_terminal_event() =
        runTest {
            val events = eventsFor(SseFixtures.HAPPY_PATH)

            assertEquals(
                1,
                events.count { it is ChatStreamEvent.Completed || it is ChatStreamEvent.Failed },
            )
            assertIs<ChatStreamEvent.Completed>(events.last())
        }

    @Test
    fun reports_zero_usage_when_the_server_omits_it() =
        runTest {
            val fixture =
                """
                event: message_stop
                data: {"type":"message_stop"}

                """.trimIndent()
            val completed = assertIs<ChatStreamEvent.Completed>(eventsFor(fixture).single())

            assertEquals(TokenUsage(0, 0), completed.usage)
            assertEquals(StopReason.Unknown, completed.stopReason)
        }
}
