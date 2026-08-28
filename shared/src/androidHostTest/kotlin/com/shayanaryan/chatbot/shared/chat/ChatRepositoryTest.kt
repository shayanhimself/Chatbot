package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.claude.ClaudeStreamEvent
import com.shayanaryan.chatbot.shared.claude.FakeScriptedClaudeEngine
import com.shayanaryan.chatbot.shared.claude.StopReason
import com.shayanaryan.chatbot.shared.claude.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import com.shayanaryan.chatbot.shared.textContent
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val USER_MESSAGE = "hello"
private const val FOLLOW_UP_MESSAGE = "again"
private const val LONG_TITLE_SOURCE = "y"

// Longer than the title cap, so the repository has something to truncate.
private const val LONG_TITLE_LENGTH = 120
private const val FIRST_CHAT_MESSAGE = "one"
private const val SECOND_CHAT_MESSAGE = "two"
private const val TARGET_MESSAGE = "target"
private const val OTHER_MESSAGE = "other"

// An id no send ever returns, which is what a snapshot of a missing chat needs.
private const val UNKNOWN_CHAT_ID = 404L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {
    private val turnScopes = mutableListOf<CoroutineScope>()

    /**
     * A scope for turns, separate from the one the test body runs in: a turn must outlive its
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
    ): ChatRepository =
        createChatRepository(
            database = database,
            engine =
                FakeScriptedClaudeEngine(
                    events =
                        listOf(
                            ClaudeStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)),
                        ),
                ),
            externalScope = scope,
            clock = clock,
        )

    @Test
    fun `a snapshot carries one chat with its messages and its turn`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)

            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()

            val snapshot = assertNotNull(repository.getChatSnapshotFlow(id).first())
            assertEquals(id, snapshot.chat.id)
            assertEquals(
                USER_MESSAGE,
                snapshot.messages
                    .first()
                    .content
                    .textContent(),
            )
            assertEquals(TurnState.Idle, snapshot.turn)
        }

    @Test
    fun `a snapshot is null for a chat that has no id yet`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)

            assertNull(repository.getChatSnapshotFlow(null).first())
        }

    @Test
    fun `a snapshot is null for a chat that does not exist`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)

            assertNull(repository.getChatSnapshotFlow(UNKNOWN_CHAT_ID).first())
        }

    @Test
    fun `a new chat carries the requested model and a truncated title`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val id =
                repository.send(
                    chatId = null,
                    text = LONG_TITLE_SOURCE.repeat(LONG_TITLE_LENGTH),
                    model = ClaudeModel.Opus,
                )
            advanceUntilIdle()

            val chat = repository.getChatsFlow().first().single()
            assertEquals(id, chat.id)
            assertEquals(ChatRepository.MAX_TITLE_LENGTH, chat.title.length)
            assertEquals(ClaudeModel.Opus, chat.model)
            assertEquals(Instant.fromEpochMilliseconds(1_000), chat.createdAt)
        }

    @Test
    fun `chats are listed most recently updated first`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val first = repository.send(null, FIRST_CHAT_MESSAGE)
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, SECOND_CHAT_MESSAGE)
            advanceUntilIdle()

            assertEquals(
                listOf(second, first),
                repository.getChatsFlow().first().map { it.id },
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
            assertEquals(id, first.chatId)
        }

    @Test
    fun `a chat with no turn reports idle`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val unknownId = 1_000L

            assertEquals(TurnState.Idle, repository.getTurnFlow(unknownId).first())
        }

    @Test
    fun `sending to a chat that does not exist is rejected`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val unknownId = 1_000L

            assertFailsWith<IllegalArgumentException> { repository.send(unknownId, USER_MESSAGE) }
        }

    @Test
    fun `sending to a deleted chat is rejected`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()
            repository.delete(id)

            assertFailsWith<IllegalArgumentException> { repository.send(id, FOLLOW_UP_MESSAGE) }
        }

    @Test
    fun `changing the model rewrites only that chat`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val target = repository.send(null, TARGET_MESSAGE)
            advanceUntilIdle()
            val other = repository.send(null, OTHER_MESSAGE)
            advanceUntilIdle()

            repository.setModel(target, ClaudeModel.Haiku)

            val byId = repository.getChatsFlow().first().associateBy { it.id }
            assertEquals(ClaudeModel.Haiku, byId.getValue(target).model)
            assertEquals(ClaudeModel.Default, byId.getValue(other).model)
        }

    @Test
    fun `deleting removes the chat and its messages`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val id = repository.send(null, USER_MESSAGE)
            advanceUntilIdle()

            repository.delete(id)

            assertTrue(repository.getChatsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
        }
}
