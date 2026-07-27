package com.shayanaryan.chatbot.shared.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE_NAME = "chatbot.db"

/**
 * Builds the platform database builder. The file path is the only platform-specific part of the
 * storage layer; the driver and query context are chosen by [createChatbotDatabase].
 */
fun chatbotDatabaseBuilder(context: Context): RoomDatabase.Builder<ChatbotDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<ChatbotDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
}
