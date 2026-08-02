package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ChatMessage
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.textContent
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.Message
import kotlin.time.Instant

internal fun ConversationWithSnippet.toDomain(): Conversation =
    Conversation(
        id = conversation.id,
        title = conversation.title,
        model = conversation.model,
        snippet = snippet?.textContent()?.takeIf { it.isNotBlank() },
        createdAt = Instant.fromEpochMilliseconds(conversation.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(conversation.updatedAt),
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

internal fun List<MessageEntity>.toChatHistory(): List<ChatMessage> =
    mapNotNull { entity ->
        // The stored messages as the engine sees them. Blank text blocks are dropped, and a message left
        // with no blocks goes with them: the API rejects an empty block, and a stored one would be
        // replayed on every later turn. Filtering on read rather than skipping the write keeps the row
        // for the UI.
        val content = entity.content.filterNot { it is ContentBlock.Text && it.text.isBlank() }
        if (content.isEmpty()) null else ChatMessage(role = entity.role, content = content)
    }
