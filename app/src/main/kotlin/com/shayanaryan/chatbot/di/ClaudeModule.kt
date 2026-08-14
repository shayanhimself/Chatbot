package com.shayanaryan.chatbot.di

import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.claude.ApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.ClaudeEngine
import com.shayanaryan.chatbot.shared.claude.ClaudeHttpClient
import com.shayanaryan.chatbot.shared.claude.createApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.createClaudeEngine
import com.shayanaryan.chatbot.shared.claude.createClaudeHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The only DI registration `:shared`'s Claude stack needs. Hilt cannot enter `:shared`, so the
 * engine is assembled here through the module's public factory; `:app` never sees the Ktor
 * client, only the opaque [ClaudeHttpClient] handle the engine and the validator share.
 *
 * The [ApiKeyRepository] binding is contributed separately, by the module that owns key storage.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClaudeModule {
    @Provides
    @Singleton
    fun provideChatHttpClient(): ClaudeHttpClient = createClaudeHttpClient()

    @Provides
    @Singleton
    fun provideChatEngine(
        httpClient: ClaudeHttpClient,
        repository: ApiKeyRepository,
    ): ClaudeEngine = createClaudeEngine(httpClient, repository)

    @Provides
    @Singleton
    fun provideApiKeyValidator(httpClient: ClaudeHttpClient): ApiKeyValidator =
        createApiKeyValidator(httpClient)
}
