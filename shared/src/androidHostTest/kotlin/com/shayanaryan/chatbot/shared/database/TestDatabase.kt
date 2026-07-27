package com.shayanaryan.chatbot.shared.database

import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * An empty in-memory database on the test scheduler.
 *
 * The Android driver, not the bundled one: `sqlite-bundled` ships host natives only in its `jvm`
 * variant, and an Android consumer resolves the `android` one, whose `.so` payload never reaches
 * the unit-test classpath. Robolectric supplies the host SQLite implementation instead.
 */
internal fun TestScope.testDatabase(): ChatbotDatabase =
    createChatbotDatabase(
        builder =
            Room.inMemoryDatabaseBuilder<ChatbotDatabase>(
                ApplicationProvider.getApplicationContext(),
            ),
        driver = AndroidSQLiteDriver(),
        queryContext = StandardTestDispatcher(testScheduler),
    )

/**
 * Runs [body] against a database of its own.
 *
 * The database is deliberately not closed: each test builds a fresh in-memory one that is
 * released with the test JVM, and closing inside the test body would race any collector still
 * running on the scope that outlives it.
 */
internal fun runDatabaseTest(body: suspend TestScope.(ChatbotDatabase) -> Unit): TestResult =
    runTest { body(testDatabase()) }
