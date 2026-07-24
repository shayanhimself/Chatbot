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
    fun `emits one delta per text delta then completes`() =
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
    fun `maps a refusal stop reason`() =
        runTest {
            val completed =
                assertIs<ChatStreamEvent.Completed>(eventsFor(SseFixtures.REFUSAL).last())

            assertEquals(StopReason.Refusal, completed.stopReason)
        }

    @Test
    fun `maps an unrecognised stop reason to unknown`() =
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
    fun `ignores thinking deltas and non delta frames`() =
        runTest {
            val events = eventsFor(SseFixtures.THINKING_ONLY_DELTA)

            assertEquals(listOf(ChatStreamEvent.Delta("Done")), events.dropLast(1))
            assertEquals(
                ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(9, 3)),
                events.last(),
            )
        }

    @Test
    fun `emits exactly one terminal event`() =
        runTest {
            val events = eventsFor(SseFixtures.HAPPY_PATH)

            assertEquals(
                1,
                events.count { it is ChatStreamEvent.Completed || it is ChatStreamEvent.Failed },
            )
            assertIs<ChatStreamEvent.Completed>(events.last())
        }

    @Test
    fun `reports zero usage when the server omits it`() =
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
