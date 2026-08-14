package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests a test double, which is normally waste. This double is different:
 * It carries a real turn state machine, and it is the repository every test above the data layer
 * runs against. A defect in it therefore makes those tests pass rather than fail.
 */
private const val UNKNOWN_CHAT_ID = 404L

private const val USER_MESSAGE = "hello"
private const val FOLLOW_UP_MESSAGE = "again"
private const val LONG_TITLE_SOURCE = "x"

// Longer than the title cap, so the repository has something to truncate.
private const val LONG_TITLE_LENGTH = 200
private const val LISBON_MESSAGE = "plan a trip to Lisbon"
private const val FIRST_DELTA = "Hi "
private const val SECOND_DELTA = "there"
private const val FULL_REPLY = "Hi there"
private const val PARTIAL_REPLY = "Hi th"
private const val LOWERCASE_REPLY = "hi there"
private const val SNIPPET_REPLY = "a reply"
private const val FAILED_PARTIAL_REPLY = "half"
private const val NO_TOKEN_YET = ""
private const val FIRST_CHAT_MESSAGE = "one"
private const val SECOND_CHAT_MESSAGE = "two"

class FakeChatRepositoryTest {
    private val repository = FakeChatRepository()

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    @Test
    fun `sending with no id creates a chat titled from the message`() =
        runTest {
            val id = repository.send(null, LISBON_MESSAGE)

            val chat = repository.getChatsFlow().first().single()
            assertEquals(id, chat.id)
            assertEquals(LISBON_MESSAGE, chat.title)
            assertEquals(ClaudeModel.Default, chat.model)
        }

    @Test
    fun `a long first message is truncated into the title`() =
        runTest {
            val id = repository.send(null, LONG_TITLE_SOURCE.repeat(LONG_TITLE_LENGTH))

            val title =
                repository
                    .getChatsFlow()
                    .first()
                    .single { it.id == id }
                    .title
            assertEquals(ChatRepository.MAX_TITLE_LENGTH, title.length)
        }

    @Test
    fun `sending persists the user message and opens a turn`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)

            assertEquals(
                listOf(USER_MESSAGE),
                repository.getMessagesFlow(id).first().map { it.text() },
            )
            assertEquals(TurnState.Streaming(NO_TOKEN_YET), repository.getTurnFlow(id).first())
        }

    @Test
    fun `deltas accumulate into the turn state`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)

            repository.emitDelta(id, FIRST_DELTA)
            repository.emitDelta(id, SECOND_DELTA)

            assertEquals(TurnState.Streaming(FULL_REPLY), repository.getTurnFlow(id).first())
        }

    @Test
    fun `completing a turn persists the reply and returns to idle`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, FULL_REPLY)

            repository.completeTurn(id)

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals(FULL_REPLY, messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `failing a turn persists the partial reply and keeps the error readable`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, PARTIAL_REPLY)

            repository.failTurn(id, ApiError.Overloaded)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(PARTIAL_REPLY, last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(TurnState.Failed(ApiError.Overloaded), repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second send on a live turn is rejected`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)

            assertFailsWith<IllegalStateException> { repository.send(id, FOLLOW_UP_MESSAGE) }
        }

    @Test
    fun `cancelling persists the partial reply and returns to idle`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, PARTIAL_REPLY)

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals(PARTIAL_REPLY, last.text())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying drops the unfinished reply and reopens the turn`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.failTurn(id, ApiError.Network)

            repository.retry(id)

            assertEquals(listOf(Role.User), repository.getMessagesFlow(id).first().map { it.role })
            assertEquals(TurnState.Streaming(NO_TOKEN_YET), repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying with nothing to retry does nothing`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, LOWERCASE_REPLY)
            repository.completeTurn(id)

            repository.retry(id)

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `changing the model rewrites the chat`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)

            repository.setModel(id, ClaudeModel.Opus)

            assertEquals(
                ClaudeModel.Opus,
                repository
                    .getChatsFlow()
                    .first()
                    .single { it.id == id }
                    .model,
            )
        }

    @Test
    fun `deleting removes the chat, its messages and its turn`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.failTurn(id, ApiError.Network)

            repository.delete(id)

            assertTrue(repository.getChatsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertIs<TurnState.Idle>(repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second chat sorts ahead of the first`() =
        runTest {
            val first = repository.send(null, FIRST_CHAT_MESSAGE)
            repository.completeTurn(first)
            val second = repository.send(null, SECOND_CHAT_MESSAGE)

            assertEquals(
                listOf(second, first),
                repository.getChatsFlow().first().map { it.id },
            )
        }

    @Test
    fun `sending into an older chat floats it back to the head`() =
        runTest {
            val older = repository.send(null, FIRST_CHAT_MESSAGE)
            repository.completeTurn(older)
            val newer = repository.send(null, SECOND_CHAT_MESSAGE)
            repository.completeTurn(newer)

            repository.send(older, FOLLOW_UP_MESSAGE)

            assertEquals(
                listOf(older, newer),
                repository.getChatsFlow().first().map { it.id },
            )
        }

    @Test
    fun `each write gets a distinct timestamp`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.completeTurn(id)

            val timestamps = repository.getMessagesFlow(id).first().map { it.createdAt }

            assertEquals(timestamps.distinct(), timestamps)
        }

    @Test
    fun `sending into a chat that does not exist is rejected`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                repository.send(
                    UNKNOWN_CHAT_ID,
                    USER_MESSAGE,
                )
            }

            assertTrue(repository.getMessagesFlow(UNKNOWN_CHAT_ID).first().isEmpty())
        }

    @Test
    fun `sending into a deleted chat is rejected`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.completeTurn(id)
            repository.delete(id)

            assertFailsWith<IllegalArgumentException> { repository.send(id, FOLLOW_UP_MESSAGE) }
        }

    @Test
    fun `the snippet follows the last complete message`() =
        runTest {
            val repository = FakeChatRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, SNIPPET_REPLY)
            repository.completeTurn(id)

            assertEquals(
                SNIPPET_REPLY,
                repository
                    .getChatsFlow()
                    .first()
                    .single()
                    .snippet,
            )
        }

    @Test
    fun `a failed reply leaves the snippet on the user's own message`() =
        runTest {
            val repository = FakeChatRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, FAILED_PARTIAL_REPLY)
            repository.failTurn(id, ApiError.Network)

            assertEquals(
                USER_MESSAGE,
                repository
                    .getChatsFlow()
                    .first()
                    .single()
                    .snippet,
            )
        }

    @Test
    fun `the single-chat flow emits null after delete`() =
        runTest {
            val repository = FakeChatRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.delete(id)

            assertNull(repository.getChatFlow(id).first())
        }
}
