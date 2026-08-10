package com.shayanaryan.chatbot.shared.apikey

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

private const val STORE_NAME = "api_key"

/**
 * Builds the store the ciphertext lives in. The file location is the only platform-specific part
 * of the storage layer; [createApiKeyRepository] does the rest.
 */
fun apiKeyDataStore(context: Context): DataStore<Preferences> {
    val appContext = context.applicationContext
    return PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile(STORE_NAME) },
    )
}
