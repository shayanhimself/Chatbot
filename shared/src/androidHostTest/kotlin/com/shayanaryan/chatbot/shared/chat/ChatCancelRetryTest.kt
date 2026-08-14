package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.claude.ClaudeStreamEvent
import com.shayanaryan.chatbot.shared.claude.FakeManualClaudeEngine
import com.shayanaryan.chatbot.shared.claude.StopReason
import com.shayanaryan.chatbot.shared.claude.TokenUsage
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** An id no test ever creates, so a call naming it can only be reaching a chat that is gone. */
private const val UNKNOWN_CHAT_ID = 404L

private const val USER_MESSAGE = "hello"
private const val FOLLOW_UP_MESSAGE = "again"
private const val CANCELLED_REPLY = "half way"
private const val FAILED_PARTIAL_REPLY = "half"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatCancelRetryTest {
    private fun TestScope.scope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    private fun completed() = ClaudeStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))

    @Test
    fun `cancelling stores the partial reply and returns the turn to idle`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta(CANCELLED_REPLY))
            runCurrent()

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(CANCELLED_REPLY, last.text())
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals(Role.Assistant, last.role)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `cancelling with no turn in flight does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.cancel(id)

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `a cancelled reply is left out of the next turn's history`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta(FAILED_PARTIAL_REPLY))
            runCurrent()
            repository.cancel(id)

            repository.send(id, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(USER_MESSAGE, FOLLOW_UP_MESSAGE),
                engine.requests.last().messages.map {
                    (it.content.single() as ContentBlock.Text).text
                },
            )
            turnScope.cancel()
        }

    @Test
    fun `sending is allowed again once a turn is cancelled`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            repository.cancel(id)

            repository.send(id, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(4, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `retrying drops the failed reply and runs the turn again`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta(FAILED_PARTIAL_REPLY))
            engine.send(ClaudeStreamEvent.Failed(ApiError.Overloaded))
            engine.close()
            advanceUntilIdle()

            repository.retry(id)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta("second try"))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("second try", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `retrying drops a cancelled reply too`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta(FAILED_PARTIAL_REPLY))
            runCurrent()
            repository.cancel(id)

            repository.retry(id)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `retrying after a completed turn does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.retry(id)
            advanceUntilIdle()

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            assertEquals(1, engine.requests.size)
            turnScope.cancel()
        }

    @Test
    fun `retrying an unknown chat does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())

            repository.retry(UNKNOWN_CHAT_ID)
            advanceUntilIdle()

            assertTrue(engine.requests.isEmpty())
            turnScope.cancel()
        }

    @Test
    fun `deleting during a turn leaves nothing behind`() =
        runDatabaseTest { database ->
            val engine = FakeManualClaudeEngine()
            val turnScope = scope()
            val repository =
                createChatRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ClaudeStreamEvent.Delta(FAILED_PARTIAL_REPLY))
            runCurrent()

            repository.delete(id)
            advanceUntilIdle()

            assertTrue(repository.getChatsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }
}
