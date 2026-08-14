package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.chat.local.ChatEntity
import com.shayanaryan.chatbot.shared.chat.local.MessageEntity
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CHAT_TITLE = "plan a trip"
private const val FIRST_BLOCK_TEXT = "one"
private const val SECOND_BLOCK_TEXT = "two"

@RunWith(RobolectricTestRunner::class)
class ChatbotDatabaseTest {
    @Test
    fun `stores and reads back a chat with a message`() =
        runDatabaseTest { database ->
            val chatId =
                database.chatDao().insert(
                    ChatEntity(
                        title = CHAT_TITLE,
                        model = ClaudeModel.Opus,
                        createdAt = 10L,
                        updatedAt = 10L,
                    ),
                )
            database.chatDao().insertMessage(
                MessageEntity(
                    chatId = chatId,
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

            val chat = database.chatDao().findById(chatId)
            val messages = database.messageDao().completeForChat(chatId)

            assertEquals(ClaudeModel.Opus, chat?.model)
            assertEquals(CHAT_TITLE, chat?.title)
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
