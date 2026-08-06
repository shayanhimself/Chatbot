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
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val USER_MESSAGE = "hello"
private const val FOLLOW_UP_MESSAGE = "again"
private const val SECOND_QUESTION = "are you there"
private const val FIRST_CONVERSATION_MESSAGE = "one"
private const val SECOND_CONVERSATION_MESSAGE = "two"
private const val FIRST_DELTA = "Hi "
private const val SECOND_DELTA = "there"
private const val FULL_REPLY = "Hi there"
private const val PARTIAL_REPLY = "Hi th"
private const val FINISHED_REPLY = "done"
private const val RESUMED_REPLY = "here you go"

// Deltas that carry only whitespace, which the repository stores as no text at all.
private const val BLANK_DELTA = "  \n "
private const val NO_TEXT = ""

private const val FIRST_HALF_DELTA = "already "
private const val SECOND_HALF_DELTA = "here"
private const val FULL_SECOND_REPLY = "already here"
private const val CANCELLED_FIRST_DELTA = "par"
private const val CANCELLED_SECOND_DELTA = "tial"
private const val CANCELLED_REPLY = "partial"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationTurnTest {
    private val turnScopes = mutableListOf<CoroutineScope>()

    @AfterTest
    fun cancelTurnScopes() {
        turnScopes.forEach { it.cancel() }
    }

    @Test
    fun `deltas accumulate on the turn and the reply lands as a complete message`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(FIRST_DELTA))
            runCurrent()
            assertEquals(TurnState.Streaming(FIRST_DELTA), repository.getTurnFlow(id).first())

            engine.send(ChatStreamEvent.Delta(SECOND_DELTA))
            runCurrent()
            assertEquals(TurnState.Streaming(FULL_REPLY), repository.getTurnFlow(id).first())

            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals(FULL_REPLY, messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `the reply row exists before the turn reports idle`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            val rowsWhenIdle = mutableListOf<Int>()
            val collector =
                launch {
                    repository.getTurnFlow(id).collect { state ->
                        if (state == TurnState.Idle) {
                            rowsWhenIdle += repository.getMessagesFlow(id).first().size
                        }
                    }
                }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(FINISHED_REPLY))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(listOf(2), rowsWhenIdle)
            collector.cancel()
        }

    @Test
    fun `bumping updatedAt on the reply reorders the conversation list`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = createConversationRepository(database, engine, turnScope, clock)
            val first = repository.send(null, FIRST_CONVERSATION_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, SECOND_CONVERSATION_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )

            clock.advanceBy(60.seconds)
            repository.send(first, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            // Moves the clock between the user message and the reply, so the assertion below
            // can only pass if the reply did the bumping. The reply has to carry text — a turn
            // that produces none stores no row and so bumps nothing.
            clock.advanceBy(60.seconds)
            engine.send(ChatStreamEvent.Delta(RESUMED_REPLY))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val conversations = repository.getConversationsFlow().first()
            assertEquals(listOf(first, second), conversations.map { it.id })
            assertEquals(Instant.fromEpochMilliseconds(181_000), conversations.first().updatedAt)
        }

    @Test
    fun `a turn that produced no text stores a row the next turn leaves out`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(Role.User, Role.Assistant),
                repository.getMessagesFlow(id).first().map { it.role },
            )
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())

            repository.send(id, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(Role.User, Role.User),
                engine.requests
                    .last()
                    .messages
                    .map { it.role },
            )
        }

    @Test
    fun `a turn that produced only whitespace is left out of the next turn's history`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(BLANK_DELTA))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.send(id, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(Role.User, Role.Assistant, Role.User, Role.Assistant),
                repository.getMessagesFlow(id).first().map { it.role },
            )
            assertEquals(
                listOf(Role.User, Role.User),
                engine.requests
                    .last()
                    .messages
                    .map { it.role },
            )
        }

    @Test
    fun `a failure before any text still stores a row for retry to find`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Failed(ChatError.Network))
            engine.close()
            advanceUntilIdle()

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(Role.Assistant, last.role)
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(NO_TEXT, last.text())
        }

    @Test
    fun `a failed stream persists the partial reply and keeps the error readable`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(PARTIAL_REPLY))
            engine.send(ChatStreamEvent.Failed(ChatError.Overloaded))
            engine.close()
            advanceUntilIdle()

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(PARTIAL_REPLY, last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(
                TurnState.Failed(ChatError.Overloaded),
                repository.getTurnFlow(id).first(),
            )
        }

    @Test
    fun `a failed reply is left out of the next turn's history`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(PARTIAL_REPLY))
            engine.send(ChatStreamEvent.Failed(ChatError.Network))
            engine.close()
            advanceUntilIdle()

            repository.send(id, SECOND_QUESTION)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val history = engine.requests.last().messages
            assertEquals(listOf(Role.User, Role.User), history.map { it.role })
            assertEquals(
                listOf(USER_MESSAGE, SECOND_QUESTION),
                history.map { (it.content.single() as ContentBlock.Text).text },
            )
        }

    @Test
    fun `the turn uses the conversation's own model, not the send default`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE, ClaudeModel.Opus)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.setModel(id, ClaudeModel.Haiku)
            repository.send(id, FOLLOW_UP_MESSAGE)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(ClaudeModel.Opus, ClaudeModel.Haiku),
                engine.requests.map { it.model },
            )
        }

    @Test
    fun `a second send while a turn is in flight is rejected`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()

            assertFailsWith<IllegalStateException> { repository.send(id, FOLLOW_UP_MESSAGE) }

            assertEquals(1, repository.getMessagesFlow(id).first().size)
            engine.send(completed())
            engine.close()
            advanceUntilIdle()
        }

    @Test
    fun `a collector attaching mid stream sees the text already accumulated`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(FIRST_HALF_DELTA))
            engine.send(ChatStreamEvent.Delta(SECOND_HALF_DELTA))
            runCurrent()

            assertEquals(TurnState.Streaming(FULL_SECOND_REPLY), repository.getTurnFlow(id).first())

            engine.send(completed())
            engine.close()
            advanceUntilIdle()
        }

    @Test
    fun `a turn outlives the scope that was collecting it`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, USER_MESSAGE)
            val screenScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            val seen = mutableListOf<TurnState>()
            screenScope.launch { repository.getTurnFlow(id).toList(seen) }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta(CANCELLED_FIRST_DELTA))
            runCurrent()
            screenScope.cancel()
            runCurrent()

            engine.send(ChatStreamEvent.Delta(CANCELLED_SECOND_DELTA))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(CANCELLED_REPLY, messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertTrue(seen.none { it == TurnState.Idle })
        }

    /**
     * A scope for turns, separate from the one the test body runs in — a turn must outlive its
     * collector. Its own [Job] keeps `runTest` from waiting on it, so cancelling after the body
     * returns is both safe and enough.
     */
    private fun TestScope.turnScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job()).also { turnScopes += it }

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    private fun completed() = ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))
}
