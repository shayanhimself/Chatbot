package com.shayanaryan.chatbot.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** First-launch key entry. Carries no list-detail metadata, so it is full-screen at every width. */
@Serializable
data object OnboardingKey : NavKey

@Serializable
data object ChatListKey : NavKey

/**
 * @property chatId null for a chat with no first message.
 */
@Serializable
data class ChatDetailKey(
    val chatId: Long? = null,
) : NavKey
