package com.shayanaryan.chatbot.shared.chat.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/**
 * Chat reads, plus every write that spans both tables. A transaction can only call
 * methods on its own DAO, which is why the message insert is declared here rather than on
 * [MessageDao].
 */
@Dao
internal abstract class ChatDao {
    @Insert
    abstract suspend fun insert(chat: ChatEntity): Long

    /**
     * Here rather than on [MessageDao] because the transactions below need it.
     */
    @Insert
    abstract suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Every chat, most recently updated first, each with the content of its last complete
     * message. Room re-runs this flow whenever a table the query references is invalidated, and
     * the subquery makes `messages` one of them.
     */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.chatId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM chats c ORDER BY c.updatedAt DESC",
    )
    abstract fun observeAllWithSnippet(): Flow<List<ChatWithSnippet>>

    /** The same projection for one chat. Emits null once the row is gone. */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.chatId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM chats c WHERE c.id = :id",
    )
    abstract fun observeByIdWithSnippet(id: Long): Flow<ChatWithSnippet?>

    @Query("SELECT * FROM chats WHERE id = :id")
    abstract suspend fun findById(id: Long): ChatEntity?

    @Query("UPDATE chats SET model = :model WHERE id = :id")
    abstract suspend fun setModel(
        id: Long,
        model: ClaudeModel,
    )

    @Query("UPDATE chats SET updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun touch(
        id: Long,
        updatedAt: Long,
    )

    @Query("DELETE FROM chats WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /**
     * Creates a chat and its first message together, so an abandoned empty chat can never
     * appear in the list.
     *
     * @param message its `chatId` is ignored: the chat is inserted first and its
     *   generated id is substituted.
     * @return the new chat's id.
     */
    @Transaction
    open suspend fun createWithFirstMessage(
        chat: ChatEntity,
        message: MessageEntity,
    ): Long {
        val chatId = insert(chat)
        insertMessage(message.copy(chatId = chatId))
        return chatId
    }

    /**
     * Appends a message and bumps its chat's `updatedAt`, making the list's ordering rule
     * structural rather than something every caller has to remember.
     *
     * @return the new message's id.
     */
    @Transaction
    open suspend fun appendMessage(
        message: MessageEntity,
        updatedAt: Long,
    ): Long {
        val id = insertMessage(message)
        touch(message.chatId, updatedAt)
        return id
    }
}
