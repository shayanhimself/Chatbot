package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.FakeManualChatEngine
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
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

/** An id no test ever creates, so a call naming it can only be reaching a conversation that is gone. */
private const val UNKNOWN_CONVERSATION_ID = 404L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationCancelRetryTest {
    private fun TestScope.scope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    private fun completed() = ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))

    @Test
    fun `cancelling stores the partial reply and returns the turn to idle`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half way"))
            runCurrent()

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("half way", last.text())
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals(Role.Assistant, last.role)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `cancelling with no turn in flight does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
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
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            runCurrent()
            repository.cancel(id)

            repository.send(id, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf("hello", "again"),
                engine.requests.last().messages.map {
                    (it.content.single() as ContentBlock.Text).text
                },
            )
            turnScope.cancel()
        }

    @Test
    fun `sending is allowed again once a turn is cancelled`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            repository.cancel(id)

            repository.send(id, "again")
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
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            engine.send(ChatStreamEvent.Failed(ChatError.Overloaded))
            engine.close()
            advanceUntilIdle()

            repository.retry(id)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("second try"))
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
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
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
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
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
    fun `retrying an unknown conversation does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            repository.retry(UNKNOWN_CONVERSATION_ID)
            advanceUntilIdle()

            assertTrue(engine.requests.isEmpty())
            turnScope.cancel()
        }

    @Test
    fun `deleting during a turn leaves nothing behind`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            runCurrent()

            repository.delete(id)
            advanceUntilIdle()

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }
}
