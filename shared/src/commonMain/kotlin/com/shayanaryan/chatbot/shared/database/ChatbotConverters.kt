package com.shayanaryan.chatbot.shared.database

import androidx.room.TypeConverter
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * Column codecs. Enums are stored as their constant name, which is what lets a query compare
 * `status` against a string literal.
 */
internal class ChatbotConverters {
    @TypeConverter
    fun fromModel(model: ClaudeModel): String = model.name

    /**
     * Falls back rather than throwing: a model retired from the picker would otherwise make every
     * conversation row naming it unreadable.
     */
    @TypeConverter
    fun toModel(value: String): ClaudeModel =
        ClaudeModel.entries.firstOrNull { it.name == value } ?: ClaudeModel.Default

    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    /**
     * Throws on an unrecognised value, unlike the model converter. The set of statuses is
     * closed and written only by this app, so anything else is corruption, not an old row.
     */
    @TypeConverter
    fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    /**
     * Message content is stored as JSON text.
     */
    @TypeConverter
    fun fromContent(content: List<ContentBlock>): String = storageJson.encodeToString(content)

    @TypeConverter
    fun toContent(value: String): List<ContentBlock> = storageJson.decodeFromString(value)
}
