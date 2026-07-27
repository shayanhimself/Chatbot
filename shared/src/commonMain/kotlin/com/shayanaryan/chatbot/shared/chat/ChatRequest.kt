package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Output-token ceiling applied when a caller has no reason to choose its own. Sized for a chat
 * turn: long enough for a detailed answer, short enough that a runaway response stays cheap on
 * the user's own key.
 */
const val DEFAULT_MAX_TOKENS: Int = 8192

data class ChatRequest(
    val messages: List<ChatMessage>,
    val model: ClaudeModel = ClaudeModel.Default,
    val system: String? = null,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
)

data class ChatMessage(
    val role: Role,
    val content: List<ContentBlock>,
)

enum class Role { User, Assistant }

/**
 * A single piece of a message. Modelled as a list on [ChatMessage] rather than a bare string so
 * additional block types can be added without reshaping the message type.
 *
 * `@Serializable` because blocks are stored as JSON text.
 */
@Serializable
sealed interface ContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
    ) : ContentBlock
}
