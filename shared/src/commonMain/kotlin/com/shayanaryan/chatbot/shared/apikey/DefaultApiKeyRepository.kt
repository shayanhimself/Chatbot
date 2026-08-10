package com.shayanaryan.chatbot.shared.apikey

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val ciphertextKey = byteArrayPreferencesKey("api_key_ciphertext")

internal class DefaultApiKeyRepository(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeyCipher,
) : ApiKeyRepository {
    override fun hasKeyFlow(): Flow<Boolean> =
        dataStore.data
            .map { it.contains(ciphertextKey) }
            .distinctUntilChanged()

    override suspend fun apiKey(): String? {
        val ciphertext = dataStore.data.first()[ciphertextKey] ?: return null
        return cipher.decrypt(ciphertext)
    }

    override suspend fun save(key: String) {
        val ciphertext = cipher.encrypt(key)
        dataStore.edit { it[ciphertextKey] = ciphertext }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(ciphertextKey) }
    }
}
