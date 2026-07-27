package com.shayanaryan.chatbot.shared.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.shayanaryan.chatbot.shared.conversation.local.ConversationDao
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageDao
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity

/**
 * The app's only database. Callers outside this module hold it as an opaque handle and hand it to
 * a repository factory; the DAOs it exposes are module-internal.
 */
@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ChatbotConverters::class)
@ConstructedBy(ChatbotDatabaseConstructor::class)
abstract class ChatbotDatabase : RoomDatabase() {
    internal abstract fun conversationDao(): ConversationDao

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
