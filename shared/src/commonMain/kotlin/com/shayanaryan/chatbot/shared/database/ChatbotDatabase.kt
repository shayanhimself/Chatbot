package com.shayanaryan.chatbot.shared.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.shayanaryan.chatbot.shared.chat.local.ChatDao
import com.shayanaryan.chatbot.shared.chat.local.ChatEntity
import com.shayanaryan.chatbot.shared.chat.local.MessageDao
import com.shayanaryan.chatbot.shared.chat.local.MessageEntity

/**
 * The app's only database. Callers outside this module hold it as an opaque handle and hand it to
 * a repository factory; the DAOs it exposes are module-internal.
 */
@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(ChatbotConverters::class)
@ConstructedBy(ChatbotDatabaseConstructor::class)
abstract class ChatbotDatabase : RoomDatabase() {
    internal abstract fun chatDao(): ChatDao

    internal abstract fun messageDao(): MessageDao
}

/**
 * Instantiates the generated database implementation. The `actual` is produced by Room's KSP
 * processor, which is why no matching declaration appears in source.
 */
@Suppress("KotlinNoActualForExpect")
expect object ChatbotDatabaseConstructor : RoomDatabaseConstructor<ChatbotDatabase> {
    override fun initialize(): ChatbotDatabase
}
