package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/**
 * Conversation reads, plus every write that spans both tables. A transaction can only call
 * methods on its own DAO, which is why the message insert is declared here rather than on
 * [MessageDao].
 */
@Dao
internal abstract class ConversationDao {
    @Insert
    abstract suspend fun insert(conversation: ConversationEntity): Long

    /**
     * Here rather than on [MessageDao] because the transactions below need it.
     */
    @Insert
    abstract suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Every conversation, most recently updated first, each with the content of its last complete
     * message. Room re-runs this flow whenever a table the query references is invalidated, and
     * the subquery makes `messages` one of them.
     */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.conversationId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM conversations c ORDER BY c.updatedAt DESC",
    )
    abstract fun observeAllWithSnippet(): Flow<List<ConversationWithSnippet>>

    /** The same projection for one conversation. Emits null once the row is gone. */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.conversationId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM conversations c WHERE c.id = :id",
    )
    abstract fun observeByIdWithSnippet(id: Long): Flow<ConversationWithSnippet?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract suspend fun findById(id: Long): ConversationEntity?

    @Query("UPDATE conversations SET model = :model WHERE id = :id")
    abstract suspend fun setModel(
        id: Long,
        model: ClaudeModel,
    )

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun touch(
        id: Long,
        updatedAt: Long,
    )

    @Query("DELETE FROM conversations WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /**
     * Creates a conversation and its first message together, so an abandoned empty chat can never
     * appear in the list.
     *
     * @param message its `conversationId` is ignored — the conversation is inserted first and its
     *   generated id is substituted.
     * @return the new conversation's id.
     */
    @Transaction
    open suspend fun createWithFirstMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
    ): Long {
        val conversationId = insert(conversation)
        insertMessage(message.copy(conversationId = conversationId))
        return conversationId
    }

    /**
     * Appends a message and bumps its conversation's `updatedAt`, making the list's ordering rule
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
        touch(message.conversationId, updatedAt)
        return id
    }
}
