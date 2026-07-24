package com.shayanaryan.chatbot.shared.chat

/**
 * Supplies the user's LLM API key. Called once per request; the returned key is never
 * held in a field, cached, or logged.
 */
fun interface ApiKeyProvider {
    suspend fun apiKey(): String
}
