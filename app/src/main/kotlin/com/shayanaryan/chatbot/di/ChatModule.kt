package com.shayanaryan.chatbot.di

import com.shayanaryan.chatbot.shared.chat.ApiKeyProvider
import com.shayanaryan.chatbot.shared.chat.ApiKeyValidator
import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.chat.ChatHttpClient
import com.shayanaryan.chatbot.shared.chat.createApiKeyValidator
import com.shayanaryan.chatbot.shared.chat.createChatEngine
import com.shayanaryan.chatbot.shared.chat.createChatHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The only DI registration `:shared`'s chat stack needs. Hilt cannot enter `:shared`, so the
 * engine is assembled here through the module's public factory; `:app` never sees the Ktor
 * client, only the opaque [ChatHttpClient] handle the engine and the validator share.
 *
 * The [ApiKeyProvider] binding is contributed separately by its own implementation module.
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {
    @Provides
    @Singleton
    fun provideChatHttpClient(): ChatHttpClient = createChatHttpClient()

    @Provides
    @Singleton
    fun provideChatEngine(
        httpClient: ChatHttpClient,
        keyProvider: ApiKeyProvider,
    ): ChatEngine = createChatEngine(httpClient, keyProvider)

    @Provides
    @Singleton
    fun provideApiKeyValidator(httpClient: ChatHttpClient): ApiKeyValidator =
        createApiKeyValidator(httpClient)
}
