package com.shayanaryan.chatbot.shared.apikey

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val API_KEY = "sk-ant-api03-not-a-real-key"
private const val OTHER_API_KEY = "sk-ant-api03-also-not-real"
private const val STORE_FILE_NAME = "api_key.preferences_pb"

@OptIn(ExperimentalCoroutinesApi::class)
class ApiKeyRepositoryTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val cipher = FakeKeyCipher()

    // The file is named but never created: DataStore writes it on first edit, and handing it an
    // existing zero-length file is a different code path from the one production takes.
    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job()),
            produceFile = { File(temporaryFolder.root, STORE_FILE_NAME) },
        )

    private fun TestScope.repository(): ApiKeyRepository =
        createApiKeyRepository(dataStore = dataStore(), cipher = cipher)

    @Test
    fun `a saved key reads back`() =
        runTest {
            val repository = repository()

            repository.save(API_KEY)

            assertEquals(API_KEY, repository.apiKey())
        }

    @Test
    fun `reading with nothing stored returns null rather than throwing`() =
        runTest {
            assertNull(repository().apiKey())
        }

    @Test
    fun `saving again replaces the stored key`() =
        runTest {
            val repository = repository()

            repository.save(API_KEY)
            repository.save(OTHER_API_KEY)

            assertEquals(OTHER_API_KEY, repository.apiKey())
        }

    @Test
    fun `hasKeyFlow follows a store and a clear`() =
        runTest {
            val repository = repository()

            assertFalse(repository.hasKeyFlow().first())
            repository.save(API_KEY)
            assertTrue(repository.hasKeyFlow().first())
            repository.clear()
            assertFalse(repository.hasKeyFlow().first())
        }

    @Test
    fun `clearing an empty store is a no-op`() =
        runTest {
            val repository = repository()

            repository.clear()

            assertNull(repository.apiKey())
        }

    @Test
    fun `what reaches the store is ciphertext`() =
        runTest {
            val store = dataStore()
            val repository = createApiKeyRepository(dataStore = store, cipher = cipher)

            repository.save(API_KEY)

            val stored =
                store.data
                    .first()
                    .asMap()
                    .values
                    .single() as ByteArray
            assertFalse(stored.decodeToString().contains(API_KEY))
        }
}
