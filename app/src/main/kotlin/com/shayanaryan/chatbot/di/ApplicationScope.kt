package com.shayanaryan.chatbot.di

import javax.inject.Qualifier

/**
 * Marks the coroutine scope that lives as long as the process. Work launched there survives the
 * screen that started it: a reply still being streamed is persisted whether or not anything is
 * left collecting.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
