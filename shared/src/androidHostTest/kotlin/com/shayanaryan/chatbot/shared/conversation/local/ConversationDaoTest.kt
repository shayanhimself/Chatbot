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

private const val UNKNOWN_CONVERSATION_ID = 404L

private const val CONVERSATION_TITLE = "chat"
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
            val older = database.newConversation(OLDER_TITLE, updatedAt = 10L)
            val newer = database.newConversation(NEWER_TITLE, updatedAt = 20L)

            val ids =
                database
                    .conversationDao()
                    .observeAllWithSnippet()
                    .first()
                    .map { it.conversation.id }

            assertEquals(listOf(newer, older), ids)
        }

    @Test
    fun `touching a conversation moves it to the head of the list`() =
        runDatabaseTest { database ->
            val older = database.newConversation(OLDER_TITLE, updatedAt = 10L)
            database.newConversation(NEWER_TITLE, updatedAt = 20L)

            database.conversationDao().touch(older, updatedAt = 30L)

            assertEquals(
                older,
                database
                    .conversationDao()
                    .observeAllWithSnippet()
                    .first()
                    .first()
                    .conversation.id,
            )
        }

    @Test
    fun `creating with a first message writes both rows`() =
        runDatabaseTest { database ->
            val conversationId =
                database.conversationDao().createWithFirstMessage(
                    conversation =
                        ConversationEntity(
                            title = TRIP_TITLE,
                            model = ClaudeModel.Haiku,
                            createdAt = 5L,
                            updatedAt = 5L,
                        ),
                    message = message(text = TRIP_TITLE, createdAt = 5L),
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
            val conversationId = database.newConversation(CONVERSATION_TITLE, updatedAt = 10L)

            database.conversationDao().appendMessage(
                message = message(USER_MESSAGE, createdAt = 40L, conversationId = conversationId),
                updatedAt = 40L,
            )

            assertEquals(40L, database.conversationDao().findById(conversationId)?.updatedAt)
        }

    @Test
    fun `changing the model rewrites only that conversation`() =
        runDatabaseTest { database ->
            val target = database.newConversation(TARGET_TITLE, updatedAt = 10L)
            val other = database.newConversation(OTHER_TITLE, updatedAt = 20L)

            database.conversationDao().setModel(target, ClaudeModel.Opus)

            assertEquals(ClaudeModel.Opus, database.conversationDao().findById(target)?.model)
            assertEquals(ClaudeModel.Default, database.conversationDao().findById(other)?.model)
        }

    @Test
    fun `deleting a conversation cascades to its messages`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation(CONVERSATION_TITLE, updatedAt = 10L)
            database.conversationDao().insertMessage(message(USER_MESSAGE, 10L, conversationId))
            database.conversationDao().insertMessage(
                message(FOLLOW_UP_MESSAGE, 11L, conversationId),
            )

            database.conversationDao().delete(conversationId)

            assertNull(database.conversationDao().findById(conversationId))
            assertEquals(
                emptyList(),
                database.messageDao().observeForConversation(conversationId).first(),
            )
        }

    @Test
    fun `the snippet is the last complete message`() =
        runDatabaseTest { database ->
            val id = database.newConversation(CONVERSATION_TITLE, updatedAt = 1L)
            database.conversationDao().insertMessage(
                message(FIRST_MESSAGE, createdAt = 1L, conversationId = id),
            )
            database.conversationDao().insertMessage(
                message(SECOND_MESSAGE, createdAt = 2L, conversationId = id),
            )

            val row =
                database
                    .conversationDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertEquals(SECOND_MESSAGE, (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the snippet skips a message the app never completed`() =
        runDatabaseTest { database ->
            val id = database.newConversation(CONVERSATION_TITLE, updatedAt = 1L)
            database.conversationDao().insertMessage(
                message(QUESTION, createdAt = 1L, conversationId = id),
            )
            database.conversationDao().insertMessage(
                message(FAILED_REPLY, createdAt = 2L, conversationId = id)
                    .copy(role = Role.Assistant, status = MessageStatus.Failed),
            )

            val row =
                database
                    .conversationDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertEquals(QUESTION, (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `a conversation with no complete message has no snippet`() =
        runDatabaseTest { database ->
            database.newConversation(CONVERSATION_TITLE, updatedAt = 1L)

            val row =
                database
                    .conversationDao()
                    .observeAllWithSnippet()
                    .first()
                    .single()

            assertNull(row.snippet)
        }

    @Test
    fun `the single-conversation read carries the same snippet`() =
        runDatabaseTest { database ->
            val id = database.newConversation(CONVERSATION_TITLE, updatedAt = 1L)
            database.conversationDao().insertMessage(
                message(ONLY_MESSAGE, createdAt = 1L, conversationId = id),
            )

            val row = database.conversationDao().observeByIdWithSnippet(id).first()

            assertEquals(ONLY_MESSAGE, (row?.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the single-conversation read is null for a row that does not exist`() =
        runDatabaseTest { database ->
            assertNull(
                database.conversationDao().observeByIdWithSnippet(UNKNOWN_CONVERSATION_ID).first(),
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
