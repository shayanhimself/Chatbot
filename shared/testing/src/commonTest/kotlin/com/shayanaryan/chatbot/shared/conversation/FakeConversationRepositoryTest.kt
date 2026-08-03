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

class FakeConversationRepositoryTest {
    private val repository = FakeConversationRepository()

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    @Test
    fun `sending with no id creates a conversation titled from the message`() =
        runTest {
            val id = repository.send(null, "plan a trip to Lisbon")

            val conversation = repository.getConversationsFlow().first().single()
            assertEquals(id, conversation.id)
            assertEquals("plan a trip to Lisbon", conversation.title)
            assertEquals(ClaudeModel.Default, conversation.model)
        }

    @Test
    fun `a long first message is truncated into the title`() =
        runTest {
            val id = repository.send(null, "x".repeat(200))

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
            val id = repository.send(null, "hello")

            assertEquals(listOf("hello"), repository.getMessagesFlow(id).first().map { it.text() })
            assertEquals(TurnState.Streaming(""), repository.getTurnFlow(id).first())
        }

    @Test
    fun `deltas accumulate into the turn state`() =
        runTest {
            val id = repository.send(null, "hello")

            repository.emitDelta(id, "Hi ")
            repository.emitDelta(id, "there")

            assertEquals(TurnState.Streaming("Hi there"), repository.getTurnFlow(id).first())
        }

    @Test
    fun `completing a turn persists the reply and returns to idle`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi there")

            repository.completeTurn(id)

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("Hi there", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `failing a turn persists the partial reply and keeps the error readable`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi th")

            repository.failTurn(id, ChatError.Overloaded)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("Hi th", last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(TurnState.Failed(ChatError.Overloaded), repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second send on a live turn is rejected`() =
        runTest {
            val id = repository.send(null, "hello")

            assertFailsWith<IllegalStateException> { repository.send(id, "again") }
        }

    @Test
    fun `cancelling persists the partial reply and returns to idle`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi th")

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals("Hi th", last.text())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying drops the unfinished reply and reopens the turn`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.failTurn(id, ChatError.Network)

            repository.retry(id)

            assertEquals(listOf(Role.User), repository.getMessagesFlow(id).first().map { it.role })
            assertEquals(TurnState.Streaming(""), repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying with nothing to retry does nothing`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "hi there")
            repository.completeTurn(id)

            repository.retry(id)

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `changing the model rewrites the conversation`() =
        runTest {
            val id = repository.send(null, "hello")

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
            val id = repository.send(null, "hello")
            repository.failTurn(id, ChatError.Network)

            repository.delete(id)

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertIs<TurnState.Idle>(repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second conversation sorts ahead of the first`() =
        runTest {
            val first = repository.send(null, "one")
            repository.completeTurn(first)
            val second = repository.send(null, "two")

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )
        }

    @Test
    fun `sending into an older conversation floats it back to the head`() =
        runTest {
            val older = repository.send(null, "one")
            repository.completeTurn(older)
            val newer = repository.send(null, "two")
            repository.completeTurn(newer)

            repository.send(older, "again")

            assertEquals(
                listOf(older, newer),
                repository.getConversationsFlow().first().map { it.id },
            )
        }

    @Test
    fun `each write gets a distinct timestamp`() =
        runTest {
            val id = repository.send(null, "hello")
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
                    "hello",
                )
            }

            assertTrue(repository.getMessagesFlow(UNKNOWN_CONVERSATION_ID).first().isEmpty())
        }

    @Test
    fun `sending into a deleted conversation is rejected`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.completeTurn(id)
            repository.delete(id)

            assertFailsWith<IllegalArgumentException> { repository.send(id, "again") }
        }

    @Test
    fun `the snippet follows the last complete message`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "a reply")
            repository.completeTurn(id)

            assertEquals(
                "a reply",
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
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "half")
            repository.failTurn(id, ChatError.Network)

            assertEquals(
                "hello",
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
            val id = repository.send(null, "hello")
            repository.delete(id)

            assertNull(repository.getConversationFlow(id).first())
        }
}
