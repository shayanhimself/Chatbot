package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.FakeScriptedChatEngine
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
private const val LONG_TITLE_SOURCE = "y"

// Longer than the title cap, so the repository has something to truncate.
private const val LONG_TITLE_LENGTH = 120
private const val FIRST_CONVERSATION_MESSAGE = "one"
private const val SECOND_CONVERSATION_MESSAGE = "two"
private const val TARGET_MESSAGE = "target"
private const val OTHER_MESSAGE = "other"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationRepositoryTest {
    private val turnScopes = mutableListOf<CoroutineScope>()

    /**
     * A scope for turns, separate from the one the test body runs in — a turn must outlive its
     * collector. Its own [Job] keeps `runTest` from waiting on it, so cancelling after the body
     * returns is both safe and enough.
     */
    private fun TestScope.turnScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job()).also { turnScopes += it }

    @AfterTest
    fun cancelTurnScopes() {
        turnScopes.forEach { it.cancel() }
    }

    private fun TestScope.repository(
        database: ChatbotDatabase,
        scope: CoroutineScope,
        clock: FakeClock = FakeClock(),
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine =
                FakeScriptedChatEngine(
                    events =
                        listOf(
                            ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)),
                        ),
                ),
            externalScope = scope,
            clock = clock,
        )

    @Test
    fun `a new conversation carries the requested model and a truncated title`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val id =
                repository.send(
                    conversationId = null,
                    text = LONG_TITLE_SOURCE.repeat(LONG_TITLE_LENGTH),
                    model = ClaudeModel.Opus,
                )
            advanceUntilIdle()

            val conversation = repository.getConversationsFlow().first().single()
            assertEquals(id, conversation.id)
            assertEquals(ConversationRepository.MAX_TITLE_LENGTH, conversation.title.length)
            assertEquals(ClaudeModel.Opus, conversation.model)
            assertEquals(Instant.fromEpochMilliseconds(1_000), conversation.createdAt)
        }

    @Test
    fun `conversations are listed most recently updated first`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val first = repository.send(null, FIRST_CONVERSATION_MESSAGE)
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, SECOND_CONVERSATION_MESSAGE)
            advanceUntilIdle()

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )
        }

    @Test
    fun `the sent message is exposed as a complete user message`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()

            val first = repository.getMessagesFlow(id).first().first()
            assertEquals(Role.User, first.role)
            assertEquals(USER_MESSAGE, (first.content.single() as ContentBlock.Text).text)
            assertEquals(MessageStatus.Complete, first.status)
            assertEquals(Instant.fromEpochMilliseconds(1_000), first.createdAt)
            assertEquals(id, first.conversationId)
        }

    @Test
    fun `a conversation with no turn reports idle`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val unknownId = 1_000L

            assertEquals(TurnState.Idle, repository.getTurnFlow(unknownId).first())
        }

    @Test
    fun `sending to a conversation that does not exist is rejected`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val unknownId = 1_000L

            assertFailsWith<IllegalArgumentException> { repository.send(unknownId, USER_MESSAGE) }
        }

    @Test
    fun `sending to a deleted conversation is rejected`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()
            repository.delete(id)

            assertFailsWith<IllegalArgumentException> { repository.send(id, FOLLOW_UP_MESSAGE) }
        }

    @Test
    fun `changing the model rewrites only that conversation`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val target = repository.send(null, TARGET_MESSAGE)
            advanceUntilIdle()
            val other = repository.send(null, OTHER_MESSAGE)
            advanceUntilIdle()

            repository.setModel(target, ClaudeModel.Haiku)

            val byId = repository.getConversationsFlow().first().associateBy { it.id }
            assertEquals(ClaudeModel.Haiku, byId.getValue(target).model)
            assertEquals(ClaudeModel.Default, byId.getValue(other).model)
        }

    @Test
    fun `deleting removes the conversation and its messages`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()

            repository.delete(id)

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
        }
}
