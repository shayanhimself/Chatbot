package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CONVERSATION_TITLE = "chat"
private const val FIRST_MESSAGE = "first"
private const val SECOND_MESSAGE = "second"
private const val THIRD_MESSAGE = "third"
private const val QUESTION = "ask"
private const val FOLLOW_UP_QUESTION = "again"

// A reply the turn never finished, so the history query skips it.
private const val FAILED_REPLY = "half"
private const val CANCELLED_REPLY = "stub"

private const val OWN_MESSAGE = "mine"
private const val OTHER_CONVERSATION_MESSAGE = "theirs"

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private suspend fun ChatbotDatabase.newConversation(): Long =
        conversationDao().insert(
            ConversationEntity(
                title = CONVERSATION_TITLE,
                model = ClaudeModel.Default,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

    private suspend fun ChatbotDatabase.append(
        conversationId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
        createdAt: Long,
    ): Long =
        conversationDao().insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = createdAt,
            ),
        )

    @Test
    fun `orders messages by insertion, not by timestamp`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, FIRST_MESSAGE, MessageStatus.Complete, 100L)
            database.append(
                conversationId = conversationId,
                role = Role.Assistant,
                text = SECOND_MESSAGE,
                status = MessageStatus.Complete,
                createdAt = 100L,
            )
            database.append(conversationId, Role.User, THIRD_MESSAGE, MessageStatus.Complete, 5L)

            val texts =
                database
                    .messageDao()
                    .observeForConversation(conversationId)
                    .first()
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf(FIRST_MESSAGE, SECOND_MESSAGE, THIRD_MESSAGE), texts)
        }

    @Test
    fun `history excludes a row the app never completed`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            database.append(conversationId, Role.Assistant, FAILED_REPLY, MessageStatus.Failed, 2L)
            database.append(
                conversationId,
                Role.User,
                FOLLOW_UP_QUESTION,
                MessageStatus.Complete,
                3L,
            )
            database.append(
                conversationId = conversationId,
                role = Role.Assistant,
                text = CANCELLED_REPLY,
                status = MessageStatus.Cancelled,
                createdAt = 4L,
            )

            val texts =
                database
                    .messageDao()
                    .completeForConversation(conversationId)
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf(QUESTION, FOLLOW_UP_QUESTION), texts)
        }

    @Test
    fun `history is scoped to one conversation`() =
        runDatabaseTest { database ->
            val mine = database.newConversation()
            val theirs = database.newConversation()
            database.append(mine, Role.User, OWN_MESSAGE, MessageStatus.Complete, 1L)
            database.append(
                conversationId = theirs,
                role = Role.User,
                text = OTHER_CONVERSATION_MESSAGE,
                status = MessageStatus.Complete,
                createdAt = 1L,
            )

            assertEquals(1, database.messageDao().completeForConversation(mine).size)
        }

    @Test
    fun `the last message is the most recently inserted one`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            val lastId =
                database.append(
                    conversationId = conversationId,
                    role = Role.Assistant,
                    text = FAILED_REPLY,
                    status = MessageStatus.Failed,
                    createdAt = 2L,
                )

            val last = database.messageDao().lastForConversation(conversationId)

            assertEquals(lastId, last?.id)
            assertEquals(MessageStatus.Failed, last?.status)
        }

    @Test
    fun `deleting a message by id leaves the rest`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            val failed =
                database.append(
                    conversationId = conversationId,
                    role = Role.Assistant,
                    text = FAILED_REPLY,
                    status = MessageStatus.Failed,
                    createdAt = 2L,
                )

            database.messageDao().deleteById(failed)

            assertEquals(
                1,
                database
                    .messageDao()
                    .observeForConversation(conversationId)
                    .first()
                    .size,
            )
        }
}
