package com.shayanaryan.chatbot.shared.chat.local

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Chat
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.claude.ClaudeMessage
import com.shayanaryan.chatbot.shared.textContent
import kotlin.time.Instant

internal fun ChatWithSnippet.toDomain(): Chat =
    Chat(
        id = chat.id,
        title = chat.title,
        model = chat.model,
        snippet = snippet?.textContent()?.takeIf { it.isNotBlank() },
        createdAt = Instant.fromEpochMilliseconds(chat.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(chat.updatedAt),
    )

internal fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        status = status,
        error = error,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )

internal fun List<MessageEntity>.toClaudeMessages(): List<ClaudeMessage> =
    mapNotNull { entity ->
        // The stored messages as the engine sees them. Blank text blocks are dropped, and a message left
        // with no blocks goes with them: the API rejects an empty block, and a stored one would be
        // replayed on every later turn. Filtering on read rather than skipping the write keeps the row
        // for the UI.
        val content = entity.content.filterNot { it is ContentBlock.Text && it.text.isBlank() }
        if (content.isEmpty()) null else ClaudeMessage(role = entity.role, content = content)
    }
