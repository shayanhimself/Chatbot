package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CONVERSATION_TITLE = "plan a trip"
private const val FIRST_BLOCK_TEXT = "one"
private const val SECOND_BLOCK_TEXT = "two"

@RunWith(RobolectricTestRunner::class)
class ChatbotDatabaseTest {
    @Test
    fun `stores and reads back a conversation with a message`() =
        runDatabaseTest { database ->
            val conversationId =
                database.conversationDao().insert(
                    ConversationEntity(
                        title = CONVERSATION_TITLE,
                        model = ClaudeModel.Opus,
                        createdAt = 10L,
                        updatedAt = 10L,
                    ),
                )
            database.conversationDao().insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = Role.User,
                    content =
                        listOf(
                            ContentBlock.Text(FIRST_BLOCK_TEXT),
                            ContentBlock.Text(SECOND_BLOCK_TEXT),
                        ),
                    status = MessageStatus.Complete,
                    createdAt = 10L,
                ),
            )

            val conversation = database.conversationDao().findById(conversationId)
            val messages = database.messageDao().completeForConversation(conversationId)

            assertEquals(ClaudeModel.Opus, conversation?.model)
            assertEquals(CONVERSATION_TITLE, conversation?.title)
            assertEquals(
                listOf<ContentBlock>(
                    ContentBlock.Text(FIRST_BLOCK_TEXT),
                    ContentBlock.Text(SECOND_BLOCK_TEXT),
                ),
                messages.single().content,
            )
            assertEquals(Role.User, messages.single().role)
        }
}
