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

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private suspend fun ChatbotDatabase.newConversation(): Long =
        conversationDao().insert(
            ConversationEntity(
                title = "chat",
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
            database.append(conversationId, Role.User, "first", MessageStatus.Complete, 100L)
            database.append(conversationId, Role.Assistant, "second", MessageStatus.Complete, 100L)
            database.append(conversationId, Role.User, "third", MessageStatus.Complete, 5L)

            val texts =
                database
                    .messageDao()
                    .observeForConversation(conversationId)
                    .first()
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf("first", "second", "third"), texts)
        }

    @Test
    fun `history excludes a row the app never completed`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)
            database.append(conversationId, Role.User, "again", MessageStatus.Complete, 3L)
            database.append(conversationId, Role.Assistant, "stub", MessageStatus.Cancelled, 4L)

            val texts =
                database
                    .messageDao()
                    .completeForConversation(conversationId)
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf("ask", "again"), texts)
        }

    @Test
    fun `history is scoped to one conversation`() =
        runDatabaseTest { database ->
            val mine = database.newConversation()
            val theirs = database.newConversation()
            database.append(mine, Role.User, "mine", MessageStatus.Complete, 1L)
            database.append(theirs, Role.User, "theirs", MessageStatus.Complete, 1L)

            assertEquals(1, database.messageDao().completeForConversation(mine).size)
        }

    @Test
    fun `the last message is the most recently inserted one`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            val lastId =
                database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)

            val last = database.messageDao().lastForConversation(conversationId)

            assertEquals(lastId, last?.id)
            assertEquals(MessageStatus.Failed, last?.status)
        }

    @Test
    fun `deleting a message by id leaves the rest`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            val failed =
                database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)

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
