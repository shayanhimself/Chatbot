package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.time.Instant

data class Conversation(
    val id: Long,
    val title: String,
    val model: ClaudeModel,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val role: Role,
    val content: List<ContentBlock>,
    val status: MessageStatus,
    val createdAt: Instant,
)

/**
 * How a message ended. Only [Complete] messages are sent back to the model.
 */
enum class MessageStatus { Complete, Failed, Cancelled }
