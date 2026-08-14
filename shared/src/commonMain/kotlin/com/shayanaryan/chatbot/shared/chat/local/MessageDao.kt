package com.shayanaryan.chatbot.shared.chat.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MessageDao {
    /**
     * Message reads. Ordering is by `id`, not `createdAt`: autoincrement is monotonic, so it is
     * insertion order without ties. Timestamps are for display.
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY id")
    fun observeForChat(chatId: Long): Flow<List<MessageEntity>>

    /** The history sent to the model: everything a turn actually produced, nothing that failed. */
    @Query(
        "SELECT * FROM messages WHERE chatId = :chatId " +
            "AND status = 'Complete' ORDER BY id",
    )
    suspend fun completeForChat(chatId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY id DESC LIMIT 1")
    suspend fun lastForChat(chatId: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
