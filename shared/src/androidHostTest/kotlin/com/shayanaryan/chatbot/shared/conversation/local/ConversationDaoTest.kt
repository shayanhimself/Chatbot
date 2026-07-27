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
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ConversationDaoTest {
    private suspend fun ChatbotDatabase.newConversation(
        title: String,
        updatedAt: Long,
    ): Long =
        conversationDao().insert(
            ConversationEntity(
                title = title,
                model = ClaudeModel.Default,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            ),
        )

    /**
     * @param conversationId omitted when the message is handed to a transaction that assigns it.
     */
    private fun message(
        text: String,
        createdAt: Long,
        conversationId: Long = 0,
    ) = MessageEntity(
        conversationId = conversationId,
        role = Role.User,
        content = listOf(ContentBlock.Text(text)),
        status = MessageStatus.Complete,
        createdAt = createdAt,
    )

    @Test
    fun `orders conversations by most recently updated`() =
        runDatabaseTest { database ->
            val older = database.newConversation("older", updatedAt = 10L)
            val newer = database.newConversation("newer", updatedAt = 20L)

            val ids =
                database
                    .conversationDao()
                    .observeAll()
                    .first()
                    .map { it.id }

            assertEquals(listOf(newer, older), ids)
        }

    @Test
    fun `touching a conversation moves it to the head of the list`() =
        runDatabaseTest { database ->
            val older = database.newConversation("older", updatedAt = 10L)
            database.newConversation("newer", updatedAt = 20L)

            database.conversationDao().touch(older, updatedAt = 30L)

            assertEquals(
                older,
                database
                    .conversationDao()
                    .observeAll()
                    .first()
                    .first()
                    .id,
            )
        }

    @Test
    fun `creating with a first message writes both rows`() =
        runDatabaseTest { database ->
            val conversationId =
                database.conversationDao().createWithFirstMessage(
                    conversation =
                        ConversationEntity(
                            title = "plan a trip",
                            model = ClaudeModel.Haiku,
                            createdAt = 5L,
                            updatedAt = 5L,
                        ),
                    message = message(text = "plan a trip", createdAt = 5L),
                )

            val messages = database.messageDao().completeForConversation(conversationId)

            assertEquals(
                ClaudeModel.Haiku,
                database.conversationDao().findById(conversationId)?.model,
            )
            assertEquals(conversationId, messages.single().conversationId)
        }

    @Test
    fun `appending a message bumps the conversation`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation("chat", updatedAt = 10L)

            database.conversationDao().appendMessage(
                message = message("hello", createdAt = 40L, conversationId = conversationId),
                updatedAt = 40L,
            )

            assertEquals(40L, database.conversationDao().findById(conversationId)?.updatedAt)
        }

    @Test
    fun `changing the model rewrites only that conversation`() =
        runDatabaseTest { database ->
            val target = database.newConversation("target", updatedAt = 10L)
            val other = database.newConversation("other", updatedAt = 20L)

            database.conversationDao().setModel(target, ClaudeModel.Opus)

            assertEquals(ClaudeModel.Opus, database.conversationDao().findById(target)?.model)
            assertEquals(ClaudeModel.Default, database.conversationDao().findById(other)?.model)
        }

    @Test
    fun `deleting a conversation cascades to its messages`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation("chat", updatedAt = 10L)
            database.conversationDao().insertMessage(message("hello", 10L, conversationId))
            database.conversationDao().insertMessage(message("again", 11L, conversationId))

            database.conversationDao().delete(conversationId)

            assertNull(database.conversationDao().findById(conversationId))
            assertEquals(
                emptyList(),
                database.messageDao().observeForConversation(conversationId).first(),
            )
        }
}
