package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
internal interface MessageDao {

    /**
     * Message reads. Ordering is by `id`, not `createdAt`: autoincrement is monotonic, so it is
     * insertion order without ties. Timestamps are for display.
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEntity>>

    /** The history sent to the model: everything a turn actually produced, nothing that failed. */
    @Query(
        "SELECT * FROM messages WHERE conversationId = :conversationId " +
            "AND status = 'Complete' ORDER BY id",
    )
    suspend fun completeForConversation(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id DESC LIMIT 1")
    suspend fun lastForConversation(conversationId: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
