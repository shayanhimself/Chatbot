package com.shayanaryan.chatbot.shared.database

import androidx.sqlite.execSQL
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.chat.local.MessageEntity
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// The version the error column was added to, and the one before it.
private const val VERSION_WITHOUT_ERROR = 1

private const val DATABASE_FILE = "chatbot.db"
private const val CHAT_ID = 1L
private const val CHAT_TITLE = "asked before the upgrade"
private const val FAILED_REPLY = "half a re"
private const val LATER_REPLY = "nothing at all"
private const val TIMESTAMP = 1_000L
private const val RETRY_AFTER_SECONDS = 30

@RunWith(RobolectricTestRunner::class)
class ChatbotDatabaseMigrationTest {
    @get:Rule
    val databaseFolder = TemporaryFolder()

    private fun storedText(text: String): String =
        storageJson.encodeToString(listOf<ContentBlock>(ContentBlock.Text(text)))

    /**
     * One chat and the reply that failed in it, written the way the version before the error
     * column did.
     */
    private fun createChatWithFailedReply(): String {
        val path = databaseFolder.newFile(DATABASE_FILE).absolutePath
        createDatabaseAtVersion(path, VERSION_WITHOUT_ERROR) {
            execSQL(
                "INSERT INTO chats (id, title, model, createdAt, updatedAt) VALUES " +
                    "($CHAT_ID, '$CHAT_TITLE', '${ClaudeModel.Default.name}', " +
                    "$TIMESTAMP, $TIMESTAMP)",
            )
            execSQL(
                "INSERT INTO messages (chatId, role, content, status, createdAt) VALUES " +
                    "($CHAT_ID, '${Role.Assistant.name}', '${storedText(FAILED_REPLY)}', " +
                    "'${MessageStatus.Failed.name}', $TIMESTAMP)",
            )
        }
        return path
    }

    @Test
    fun `a reply stored before the error column survives with no error`() =
        runTest {
            val path = createChatWithFailedReply()

            val database = fileDatabase(path)
            val message =
                database
                    .messageDao()
                    .observeForChat(CHAT_ID)
                    .first()
                    .single()

            assertEquals(listOf(ContentBlock.Text(FAILED_REPLY)), message.content)
            assertEquals(MessageStatus.Failed, message.status)
            assertNull(message.error)
        }

    @Test
    fun `a reply written after the migration keeps its error`() =
        runTest {
            val path = createChatWithFailedReply()
            val database = fileDatabase(path)

            database.chatDao().insertMessage(
                MessageEntity(
                    chatId = CHAT_ID,
                    role = Role.Assistant,
                    content = listOf(ContentBlock.Text(LATER_REPLY)),
                    status = MessageStatus.Failed,
                    error = ApiError.RateLimited(RETRY_AFTER_SECONDS),
                    createdAt = TIMESTAMP,
                ),
            )

            val messages = database.messageDao().observeForChat(CHAT_ID).first()
            assertEquals(
                ApiError.RateLimited(RETRY_AFTER_SECONDS),
                messages.last().error,
            )
        }
}
