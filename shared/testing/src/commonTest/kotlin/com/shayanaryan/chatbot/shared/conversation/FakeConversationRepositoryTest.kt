package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
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
private const val UNKNOWN_CONVERSATION_ID = 404L

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
private const val FIRST_CONVERSATION_MESSAGE = "one"
private const val SECOND_CONVERSATION_MESSAGE = "two"

class FakeConversationRepositoryTest {
    private val repository = FakeConversationRepository()

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    @Test
    fun `sending with no id creates a conversation titled from the message`() =
        runTest {
            val id = repository.send(null, LISBON_MESSAGE)

            val conversation = repository.getConversationsFlow().first().single()
            assertEquals(id, conversation.id)
            assertEquals(LISBON_MESSAGE, conversation.title)
            assertEquals(ClaudeModel.Default, conversation.model)
        }

    @Test
    fun `a long first message is truncated into the title`() =
        runTest {
            val id = repository.send(null, LONG_TITLE_SOURCE.repeat(LONG_TITLE_LENGTH))

            val title =
                repository
                    .getConversationsFlow()
                    .first()
                    .single { it.id == id }
                    .title
            assertEquals(ConversationRepository.MAX_TITLE_LENGTH, title.length)
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

            repository.failTurn(id, ChatError.Overloaded)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(PARTIAL_REPLY, last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(TurnState.Failed(ChatError.Overloaded), repository.getTurnFlow(id).first())
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
            repository.failTurn(id, ChatError.Network)

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
    fun `changing the model rewrites the conversation`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)

            repository.setModel(id, ClaudeModel.Opus)

            assertEquals(
                ClaudeModel.Opus,
                repository
                    .getConversationsFlow()
                    .first()
                    .single { it.id == id }
                    .model,
            )
        }

    @Test
    fun `deleting removes the conversation, its messages and its turn`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.failTurn(id, ChatError.Network)

            repository.delete(id)

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertIs<TurnState.Idle>(repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second conversation sorts ahead of the first`() =
        runTest {
            val first = repository.send(null, FIRST_CONVERSATION_MESSAGE)
            repository.completeTurn(first)
            val second = repository.send(null, SECOND_CONVERSATION_MESSAGE)

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )
        }

    @Test
    fun `sending into an older conversation floats it back to the head`() =
        runTest {
            val older = repository.send(null, FIRST_CONVERSATION_MESSAGE)
            repository.completeTurn(older)
            val newer = repository.send(null, SECOND_CONVERSATION_MESSAGE)
            repository.completeTurn(newer)

            repository.send(older, FOLLOW_UP_MESSAGE)

            assertEquals(
                listOf(older, newer),
                repository.getConversationsFlow().first().map { it.id },
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
    fun `sending into a conversation that does not exist is rejected`() =
        runTest {
            assertFailsWith<IllegalArgumentException> {
                repository.send(
                    UNKNOWN_CONVERSATION_ID,
                    USER_MESSAGE,
                )
            }

            assertTrue(repository.getMessagesFlow(UNKNOWN_CONVERSATION_ID).first().isEmpty())
        }

    @Test
    fun `sending into a deleted conversation is rejected`() =
        runTest {
            val id = repository.send(null, USER_MESSAGE)
            repository.completeTurn(id)
            repository.delete(id)

            assertFailsWith<IllegalArgumentException> { repository.send(id, FOLLOW_UP_MESSAGE) }
        }

    @Test
    fun `the snippet follows the last complete message`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, SNIPPET_REPLY)
            repository.completeTurn(id)

            assertEquals(
                SNIPPET_REPLY,
                repository
                    .getConversationsFlow()
                    .first()
                    .single()
                    .snippet,
            )
        }

    @Test
    fun `a failed reply leaves the snippet on the user's own message`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.emitDelta(id, FAILED_PARTIAL_REPLY)
            repository.failTurn(id, ChatError.Network)

            assertEquals(
                USER_MESSAGE,
                repository
                    .getConversationsFlow()
                    .first()
                    .single()
                    .snippet,
            )
        }

    @Test
    fun `the single-conversation flow emits null after delete`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, USER_MESSAGE)
            repository.delete(id)

            assertNull(repository.getConversationFlow(id).first())
        }
}
