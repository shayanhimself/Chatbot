package com.shayanaryan.chatbot.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * The app's only source of time. Both the repository's stored timestamps and the chat
 * list's relative labels read it, so the two can never disagree about "now".
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
