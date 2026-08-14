package com.shayanaryan.chatbot.di

import android.content.Context
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import com.shayanaryan.chatbot.shared.chat.createChatRepository
import com.shayanaryan.chatbot.shared.claude.ClaudeEngine
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.chatbotDatabaseBuilder
import com.shayanaryan.chatbot.shared.database.createChatbotDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Registers `:shared`'s storage layer. Hilt cannot enter `:shared`, so the database and the
 * repository are assembled here through the module's public factories; the DAOs stay inside it.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideChatbotDatabase(
        @ApplicationContext context: Context,
    ): ChatbotDatabase = createChatbotDatabase(chatbotDatabaseBuilder(context))

    @Provides
    @Singleton
    fun provideChatRepository(
        database: ChatbotDatabase,
        engine: ClaudeEngine,
        @ApplicationScope externalScope: CoroutineScope,
        clock: Clock,
    ): ChatRepository =
        createChatRepository(
            database = database,
            engine = engine,
            externalScope = externalScope,
            clock = clock,
        )
}
