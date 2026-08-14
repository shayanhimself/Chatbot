package com.shayanaryan.chatbot.shared.chat.local

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CHAT_TITLE = "chat"
private const val FIRST_MESSAGE = "first"
private const val SECOND_MESSAGE = "second"
private const val THIRD_MESSAGE = "third"
private const val QUESTION = "ask"
private const val FOLLOW_UP_QUESTION = "again"

// A reply the turn never finished, so the history query skips it.
private const val FAILED_REPLY = "half"
private const val CANCELLED_REPLY = "stub"

private const val OWN_MESSAGE = "mine"
private const val OTHER_CHAT_MESSAGE = "theirs"

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private suspend fun ChatbotDatabase.newChat(): Long =
        chatDao().insert(
            ChatEntity(
                title = CHAT_TITLE,
                model = ClaudeModel.Default,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

    private suspend fun ChatbotDatabase.append(
        chatId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
        createdAt: Long,
    ): Long =
        chatDao().insertMessage(
            MessageEntity(
                chatId = chatId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = createdAt,
            ),
        )

    @Test
    fun `orders messages by insertion, not by timestamp`() =
        runDatabaseTest { database ->
            val chatId = database.newChat()
            database.append(chatId, Role.User, FIRST_MESSAGE, MessageStatus.Complete, 100L)
            database.append(
                chatId = chatId,
                role = Role.Assistant,
                text = SECOND_MESSAGE,
                status = MessageStatus.Complete,
                createdAt = 100L,
            )
            database.append(chatId, Role.User, THIRD_MESSAGE, MessageStatus.Complete, 5L)

            val texts =
                database
                    .messageDao()
                    .observeForChat(chatId)
                    .first()
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf(FIRST_MESSAGE, SECOND_MESSAGE, THIRD_MESSAGE), texts)
        }

    @Test
    fun `history excludes a row the app never completed`() =
        runDatabaseTest { database ->
            val chatId = database.newChat()
            database.append(chatId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            database.append(chatId, Role.Assistant, FAILED_REPLY, MessageStatus.Failed, 2L)
            database.append(
                chatId,
                Role.User,
                FOLLOW_UP_QUESTION,
                MessageStatus.Complete,
                3L,
            )
            database.append(
                chatId = chatId,
                role = Role.Assistant,
                text = CANCELLED_REPLY,
                status = MessageStatus.Cancelled,
                createdAt = 4L,
            )

            val texts =
                database
                    .messageDao()
                    .completeForChat(chatId)
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf(QUESTION, FOLLOW_UP_QUESTION), texts)
        }

    @Test
    fun `history is scoped to one chat`() =
        runDatabaseTest { database ->
            val mine = database.newChat()
            val theirs = database.newChat()
            database.append(mine, Role.User, OWN_MESSAGE, MessageStatus.Complete, 1L)
            database.append(
                chatId = theirs,
                role = Role.User,
                text = OTHER_CHAT_MESSAGE,
                status = MessageStatus.Complete,
                createdAt = 1L,
            )

            assertEquals(1, database.messageDao().completeForChat(mine).size)
        }

    @Test
    fun `the last message is the most recently inserted one`() =
        runDatabaseTest { database ->
            val chatId = database.newChat()
            database.append(chatId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            val lastId =
                database.append(
                    chatId = chatId,
                    role = Role.Assistant,
                    text = FAILED_REPLY,
                    status = MessageStatus.Failed,
                    createdAt = 2L,
                )

            val last = database.messageDao().lastForChat(chatId)

            assertEquals(lastId, last?.id)
            assertEquals(MessageStatus.Failed, last?.status)
        }

    @Test
    fun `deleting a message by id leaves the rest`() =
        runDatabaseTest { database ->
            val chatId = database.newChat()
            database.append(chatId, Role.User, QUESTION, MessageStatus.Complete, 1L)
            val failed =
                database.append(
                    chatId = chatId,
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
                    .observeForChat(chatId)
                    .first()
                    .size,
            )
        }
}
