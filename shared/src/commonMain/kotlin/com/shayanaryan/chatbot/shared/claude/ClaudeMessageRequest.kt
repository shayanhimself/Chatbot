package com.shayanaryan.chatbot.shared.claude

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * Output-token ceiling applied when a caller has no reason to choose its own. Sized for a chat
 * turn: long enough for a detailed answer, short enough that a runaway response stays cheap on
 * the user's own key.
 */
const val DEFAULT_MAX_TOKENS: Int = 8192

/** Domain model for one call to the Messages API. */
data class ClaudeMessageRequest(
    val messages: List<ClaudeMessage>,
    val model: ClaudeModel = ClaudeModel.Default,
    val system: String? = null,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
)

/** One message in a Messages API call. Carries no identity: it is a payload, not a stored row. */
data class ClaudeMessage(
    val role: Role,
    val content: List<ContentBlock>,
)
