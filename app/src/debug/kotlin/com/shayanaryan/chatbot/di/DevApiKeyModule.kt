package com.shayanaryan.chatbot.di

import com.shayanaryan.chatbot.BuildConfig
import com.shayanaryan.chatbot.shared.chat.ApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The developer's own key, for debug builds only. 006 replaces this with the real provider over
 * the encrypted store, and until then a release build has no [ApiKeyProvider] binding and does not
 * assemble. That is deliberate, since the alternative is a release-only stub that has to be remembered
 * and removed. The M1 sideload checkpoint is a debug build.
 */
@Module
@InstallIn(SingletonComponent::class)
object DevApiKeyModule {
    @Provides
    @Singleton
    fun provideApiKeyProvider(): ApiKeyProvider = DevApiKeyProvider(BuildConfig.DEV_API_KEY)
}

internal class DevApiKeyProvider(
    private val key: String,
) : ApiKeyProvider {
    override suspend fun apiKey(): String {
        check(key.isNotBlank()) {
            "No developer key. Set ANTHROPIC_API_KEY or anthropic.api.key in local.properties, " +
                "then rebuild."
        }
        return key
    }
}
