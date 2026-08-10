package com.shayanaryan.chatbot.shared.apikey

import kotlinx.coroutines.flow.Flow

/**
 * The user's Anthropic API key, encrypted at rest. The only entry to the key's storage.
 */
interface ApiKeyRepository {
    /**
     * Whether a key is stored. Reports the presence of a ciphertext entry and never decrypts, so
     * the gate that collects this for the app's whole lifetime costs no crypto.
     */
    fun hasKeyFlow(): Flow<Boolean>

    /**
     * @return the decrypted key, or null when none is stored.
     */
    suspend fun apiKey(): String?

    /**
     * Stores [key] encrypted, replacing whatever was there.
     */
    suspend fun save(key: String)

    /**
     * Removes the stored key.
     */
    suspend fun clear()
}
