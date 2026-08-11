package com.shayanaryan.chatbot.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.KeyCipher
import com.shayanaryan.chatbot.shared.apikey.apiKeyDataStore
import com.shayanaryan.chatbot.shared.apikey.createApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.createKeystoreKeyCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Registers the encrypted key store. Hilt cannot enter `:shared`, so the DataStore and the cipher
 * are built here through that module's platform builders and handed to its commonMain factory.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    @Provides
    @Singleton
    fun provideApiKeyDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = apiKeyDataStore(context)

    @Provides
    @Singleton
    fun provideKeyCipher(): KeyCipher = createKeystoreKeyCipher()

    @Provides
    @Singleton
    fun provideApiKeyRepository(
        dataStore: DataStore<Preferences>,
        cipher: KeyCipher,
    ): ApiKeyRepository = createApiKeyRepository(dataStore = dataStore, cipher = cipher)
}
