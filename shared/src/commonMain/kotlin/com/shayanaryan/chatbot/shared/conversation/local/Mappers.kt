package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ChatMessage
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.Message
import kotlin.time.Instant

internal fun ConversationEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        model = model,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

internal fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        status = status,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )

internal fun MessageEntity.toChatMessage(): ChatMessage =
    ChatMessage(role = role, content = content)
