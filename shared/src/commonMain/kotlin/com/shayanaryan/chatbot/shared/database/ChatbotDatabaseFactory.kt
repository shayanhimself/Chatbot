package com.shayanaryan.chatbot.shared.database

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Finishes a platform builder into a usable database.
 *
 * @param builder supplied per platform, since only the file path differs.
 * @param driver a parameter rather than a hardcoded call so tests can swap it; the bundled driver
 *   is compiled from source, so behaviour is identical across OS versions.
 * @param queryContext must contain a dispatcher — Room rejects a context without one.
 */
fun createChatbotDatabase(
    builder: RoomDatabase.Builder<ChatbotDatabase>,
    driver: SQLiteDriver = BundledSQLiteDriver(),
    queryContext: CoroutineContext = Dispatchers.IO,
): ChatbotDatabase =
    builder
        .setDriver(driver)
        .setQueryCoroutineContext(queryContext)
        .build()
