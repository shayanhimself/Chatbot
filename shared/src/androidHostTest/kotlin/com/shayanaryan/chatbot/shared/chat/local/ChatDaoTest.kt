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
import kotlin.test.assertNull

private const val UNKNOWN_CHAT_ID = 404L

private const val CHAT_TITLE = "chat"
private const val OLDER_TITLE = "older"
private const val NEWER_TITLE = "newer"
private const val TARGET_TITLE = "target"
private const val OTHER_TITLE = "other"
private const val TRIP_TITLE = "plan a trip"
private const val USER_MESSAGE = "hello"
private const val FOLLOW_UP_MESSAGE = "again"
private const val FIRST_MESSAGE = "first"
private const val SECOND_MESSAGE = "second"
private const val ONLY_MESSAGE = "only"
private const val QUESTION = "asked"

// A reply the turn never finished, which the snippet subquery skips.
private const val FAILED_REPLY = "half"

// The literal the snippet subquery filters on, which no compiler checks against the enum.
private const val COMPLETE_STATUS_NAME = "Complete"

@RunWith(RobolectricTestRunner::class)
class ChatDaoTest {
    private suspend fun ChatbotDatabase.newChat(
        title: String,
        updatedAt: Long,
    ): Long =
        chatDao().insert(
            ChatEntity(
                title = title,
                model = ClaudeModel.Default,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            ),
        )

    /**
     * @param chatId omitted when the message is handed to a transaction that assigns it.
     */
    private fun message(
        text: String,
        createdAt: Long,
        chatId: Long = 0,
    ) = MessageEntity(
        chatId = chatId,
        role = Role.User,
        content = listOf(ContentBlock.Text(text)),
        status = MessageStatus.Complete,
        createdAt = createdAt,
    )

    @Test
    fun `orders chats by most recently updated`() =
        runDatabaseTest { database ->
            val older = database.newChat(OLDER_TITLE, updatedAt = 10L)
            val newer = database.newChat(NEWER_TITLE, updatedAt = 20L)

            val ids =
                database
                    .chatDao()
                    .observeAllWithSnippet()
                    .first()
                    .map { it.chat.id }

            assertEquals(listOf(newer, older), ids)
        }

    @Test
    fun `touching a chat moves it to the head of the list`() =
        runDatabaseTest { database ->
            val older = database.newChat(OLDER_TITLE, updatedAt = 10L)
            database.newChat(NEWER_TITLE, updatedAt = 20L)

            database.chatDao().touch(older, updatedAt = 30L)

            assertEquals(
                older,
                database
                    .chatDao()
                    .observeAllWithSnippet()
                    .first()
                    .first()
                    .chat.id,
            )
        }

    @Test
    fun `creating with a first message writes both rows`() =
        runDatabaseTest { database ->
            val chatId =
                database.chatDao().createWithFirstMessage(
                    chat =
                        ChatEntity(
                            title = TRIP_TITLE,
                            model = ClaudeModel.Haiku,
                            createdAt = 5L,
                            updatedAt = 5L,
                        ),
                    message = message(text = TRIP_TITLE, createdAt = 5L),
                )

            val messages = database.messageDao().completeForChat(chatId)

            assertEquals(
                ClaudeModel.Haiku,
                database.chatDao().findById(chatId)?.model,
            )
            assertEquals(chatId, messages.single().chatId)
        }

    @Test
    fun `appending a message bumps the chat`() =
        runDatabaseTest { database ->
            val chatId = database.newChat(CHAT_TITLE, updatedAt = 10L)

            database.chatDao().appendMessage(
                message = message(USER_MESSAGE, createdAt = 40L, chatId = chatId),
                updatedAt = 40L,
            )

            assertEquals(40L, database.chatDao().findById(chatId)?.updatedAt)
        }

    @Test
    fun `changing the model rewrites only that chat`() =
        runDatabaseTest { database ->
            val target = database.newChat(TARGET_TITLE, updatedAt = 10L)
            val other = database.newChat(OTHER_TITLE, updatedAt = 20L)

            database.chatDao().setModel(target, ClaudeModel.Opus)

            assertEquals(ClaudeModel.Opus, database.chatDao().findById(target)?.model)
            assertEquals(ClaudeModel.Default, database.chatDao().findById(other)?.model)
        }

    @Test
    fun `deleting a chat cascades to its messages`() =
        runDatabaseTest { database ->
            val chatId = database.newChat(CHAT_TITLE, updatedAt = 10L)
            database.chatDao().insertMessage(message(USER_MESSAGE, 10L, chatId))
            database.chatDao().insertMessage(
                message(FOLLOW_UP_MESSAGE, 11L, chatId),
            )

            database.chatDao().delete(chatId)

            assertNull(database.chatDao().findById(chatId))
            assertEquals(
                emptyList(),
                database.messageDao().observeForChat(chatId).first(),
            )
        }

    @Test
    fun `the snippet is the last complete message`() =
        runDatabaseTest { database ->
            val id = database.newChat(CHAT_TITLE, updatedAt = 1L)
            database.chatDao().insertMessage(
                message(FIRST_MESSAGE, createdAt = 1L, chatId = id),
            )
            database.chatDao().insertMessage(
                message(SECOND_MESSAGE, createdAt = 2L, chatId = id),
            )

            val row =
                database
                    .chatDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertEquals(SECOND_MESSAGE, (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the snippet skips a message the app never completed`() =
        runDatabaseTest { database ->
            val id = database.newChat(CHAT_TITLE, updatedAt = 1L)
            database.chatDao().insertMessage(
                message(QUESTION, createdAt = 1L, chatId = id),
            )
            database.chatDao().insertMessage(
                message(FAILED_REPLY, createdAt = 2L, chatId = id)
                    .copy(role = Role.Assistant, status = MessageStatus.Failed),
            )

            val row =
                database
                    .chatDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertEquals(QUESTION, (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `a chat with no complete message has no snippet`() =
        runDatabaseTest { database ->
            database.newChat(CHAT_TITLE, updatedAt = 1L)

            val row =
                database
                    .chatDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertNull(row.snippet)
        }

    @Test
    fun `the single-chat read carries the same snippet`() =
        runDatabaseTest { database ->
            val id = database.newChat(CHAT_TITLE, updatedAt = 1L)
            database.chatDao().insertMessage(
                message(ONLY_MESSAGE, createdAt = 1L, chatId = id),
            )

            val row = database.chatDao().observeByIdWithSnippet(id).first()

            assertEquals(ONLY_MESSAGE, (row?.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the single-chat read is null for a row that does not exist`() =
        runDatabaseTest { database ->
            assertNull(
                database.chatDao().observeByIdWithSnippet(UNKNOWN_CHAT_ID).first(),
            )
        }

    /**
     * The subquery filters on a string literal that no compiler checks against the enum. This is
     * what fails if the constant is ever renamed.
     */
    @Test
    fun `the snippet filter names a real status`() {
        assertEquals(COMPLETE_STATUS_NAME, MessageStatus.Complete.name)
    }
}
