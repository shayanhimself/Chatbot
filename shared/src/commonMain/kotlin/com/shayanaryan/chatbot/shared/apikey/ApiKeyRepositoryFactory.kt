package com.shayanaryan.chatbot.shared.apikey

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Assembles the repository over a platform store and cipher. Both are built per platform, since
 * the file location and the crypto are the only platform-specific parts of key storage.
 */
fun createApiKeyRepository(
    dataStore: DataStore<Preferences>,
    cipher: KeyCipher,
): ApiKeyRepository = DefaultApiKeyRepository(dataStore = dataStore, cipher = cipher)
