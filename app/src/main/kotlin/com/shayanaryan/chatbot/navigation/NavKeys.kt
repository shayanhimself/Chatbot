package com.shayanaryan.chatbot.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ConversationListKey : NavKey

/**
 * @property conversationId null for a chat with no first message.
 */
@Serializable
data class ChatKey(
    val conversationId: Long? = null,
) : NavKey
