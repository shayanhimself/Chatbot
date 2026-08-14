package com.shayanaryan.chatbot.shared.claude
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val USER_MESSAGE = "hi"

// The recorded stream's four deltas, joined.
private const val HAPPY_PATH_REPLY =
    "**SSE (Server-Sent Events)** is a web technology that lets a server push " +
        "real-time updates to a client over a single, long-lived HTTP connection. " +
        "It's a one-way communication channel—only the server sends data to the " +
        "client—commonly used for live feeds, notifications, or streaming updates."

private const val END_TURN_STOP_REASON = "\"stop_reason\":\"end_turn\""

// A stop reason this version of the app has no case for.
private const val UNRECOGNISED_STOP_REASON = "\"stop_reason\":\"tool_use\""

private const val THINKING_ONLY_REPLY = "Done"

// A stream that stops without ever reporting usage.
private val USAGE_FREE_STREAM =
    """
    event: message_stop
    data: {"type":"message_stop"}

    """.trimIndent()

class DefaultClaudeEngineStreamTest {
    private val request =
        ClaudeMessageRequest(
            messages = listOf(ClaudeMessage(Role.User, listOf(ContentBlock.Text(USER_MESSAGE)))),
        )

    private suspend fun eventsFor(fixture: String): List<ClaudeStreamEvent> =
        testChatEngine { respondSse(fixture) }.stream(request).toList()

    @Test
    fun `emits one delta per text delta then completes`() =
        runTest {
            val events = eventsFor(SseFixtures.HAPPY_PATH)

            val deltas = events.dropLast(1)
            assertEquals(4, deltas.size)
            assertEquals(
                HAPPY_PATH_REPLY,
                deltas.filterIsInstance<ClaudeStreamEvent.Delta>().joinToString("") { it.text },
            )
            assertEquals(
                ClaudeStreamEvent.Completed(StopReason.EndTurn, TokenUsage(28, 93)),
                events.last(),
            )
        }

    @Test
    fun `maps a refusal stop reason`() =
        runTest {
            val completed =
                assertIs<ClaudeStreamEvent.Completed>(eventsFor(SseFixtures.REFUSAL).last())

            assertEquals(StopReason.Refusal, completed.stopReason)
        }

    @Test
    fun `maps an unrecognised stop reason to unknown`() =
        runTest {
            val fixture =
                SseFixtures.HAPPY_PATH.replace(
                    END_TURN_STOP_REASON,
                    UNRECOGNISED_STOP_REASON,
                )
            val completed = assertIs<ClaudeStreamEvent.Completed>(eventsFor(fixture).last())

            assertEquals(StopReason.Unknown, completed.stopReason)
        }

    @Test
    fun `ignores thinking deltas and non delta frames`() =
        runTest {
            val events = eventsFor(SseFixtures.THINKING_ONLY_DELTA)

            assertEquals(listOf(ClaudeStreamEvent.Delta(THINKING_ONLY_REPLY)), events.dropLast(1))
            assertEquals(
                ClaudeStreamEvent.Completed(StopReason.EndTurn, TokenUsage(9, 3)),
                events.last(),
            )
        }

    @Test
    fun `emits exactly one terminal event`() =
        runTest {
            val events = eventsFor(SseFixtures.HAPPY_PATH)

            assertEquals(
                1,
                events.count {
                    it is ClaudeStreamEvent.Completed || it is ClaudeStreamEvent.Failed
                },
            )
            assertIs<ClaudeStreamEvent.Completed>(events.last())
        }

    @Test
    fun `reports zero usage when the server omits it`() =
        runTest {
            val completed =
                assertIs<ClaudeStreamEvent.Completed>(eventsFor(USAGE_FREE_STREAM).single())

            assertEquals(TokenUsage(0, 0), completed.usage)
            assertEquals(StopReason.Unknown, completed.stopReason)
        }
}
