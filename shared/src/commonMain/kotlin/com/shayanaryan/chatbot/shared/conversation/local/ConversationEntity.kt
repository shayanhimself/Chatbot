package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shayanaryan.chatbot.shared.model.ClaudeModel

@Entity(tableName = "conversations")
internal data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val model: ClaudeModel,
    val createdAt: Long,
    val updatedAt: Long,
)
