package com.shayanaryan.chatbot.di

import android.content.Context
import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import com.shayanaryan.chatbot.shared.conversation.createConversationRepository
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
    fun provideConversationRepository(
        database: ChatbotDatabase,
        engine: ChatEngine,
        @ApplicationScope externalScope: CoroutineScope,
        clock: Clock,
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine = engine,
            externalScope = externalScope,
            clock = clock,
        )
}
