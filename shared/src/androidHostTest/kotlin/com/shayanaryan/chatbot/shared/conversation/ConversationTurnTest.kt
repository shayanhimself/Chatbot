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

            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi "))
            runCurrent()
            assertEquals(TurnState.Streaming("Hi "), repository.getTurnFlow(id).first())

            engine.send(ChatStreamEvent.Delta("there"))
            runCurrent()
            assertEquals(TurnState.Streaming("Hi there"), repository.getTurnFlow(id).first())

            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("Hi there", messages.last().text())
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
            val id = repository.send(null, "hello")
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
            engine.send(ChatStreamEvent.Delta("done"))
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
            val first = repository.send(null, "one")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, "two")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )

            clock.advanceBy(60.seconds)
            repository.send(first, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(first, second),
                repository.getConversationsFlow().first().map { it.id },
            )
        }

    @Test
    fun `a failed stream persists the partial reply and keeps the error readable`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = turnScope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi th"))
            engine.send(ChatStreamEvent.Failed(ChatError.Overloaded))
            engine.close()
            advanceUntilIdle()

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("Hi th", last.text())
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
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi th"))
            engine.send(ChatStreamEvent.Failed(ChatError.Network))
            engine.close()
            advanceUntilIdle()

            repository.send(id, "are you there")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val history = engine.requests.last().messages
            assertEquals(listOf(Role.User, Role.User), history.map { it.role })
            assertEquals(
                listOf("hello", "are you there"),
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
            val id = repository.send(null, "hello", ClaudeModel.Opus)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.setModel(id, ClaudeModel.Haiku)
            repository.send(id, "again")
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
            val id = repository.send(null, "hello")
            engine.awaitStream()

            assertFailsWith<IllegalStateException> { repository.send(id, "again") }

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
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("already "))
            engine.send(ChatStreamEvent.Delta("here"))
            runCurrent()

            assertEquals(TurnState.Streaming("already here"), repository.getTurnFlow(id).first())

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
            val id = repository.send(null, "hello")
            val screenScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            val seen = mutableListOf<TurnState>()
            screenScope.launch { repository.getTurnFlow(id).toList(seen) }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("par"))
            runCurrent()
            screenScope.cancel()
            runCurrent()

            engine.send(ChatStreamEvent.Delta("tial"))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals("partial", messages.last().text())
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
