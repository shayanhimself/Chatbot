package com.shayanaryan.chatbot.apikey

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shayanaryan.chatbot.shared.apikey.createApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.createKeystoreKeyCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val STORED_KEY = "sk-ant-api03-keystore-round-trip"
private const val STORE_NAME_PREFIX = "keystore_storage_test"

/**
 * Numbers the store file each test method gets.
 *
 * A store refuses to open a file another live instance already holds, and the test class is built
 * once per test method, so sharing one name would have the second method fail on the first's file.
 *
 * Written outside the test class because JUnit builds a new instance of the test class for every
 * @Test method. Instance state does not carry across methods.
 */
private val storeCount = AtomicInteger()

/**
 * The repository over the cipher the app really ships, on hardware that really has a Keystore.
 *
 * The JVM equivalent substitutes the cipher, so a Keystore or Tink failure would pass there and
 * only surface on a device. The store is built here rather than injected because the app's own is a
 * single instance over a single file, which is the app's to hold and not a test's to reopen.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val storeFile =
        context.preferencesDataStoreFile("$STORE_NAME_PREFIX-${storeCount.incrementAndGet()}")

    private val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { storeFile })

    private val repository =
        createApiKeyRepository(
            dataStore = dataStore,
            cipher = createKeystoreKeyCipher(),
        )

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun `a key encrypted by the Keystore decrypts back to itself`() =
        runTest {
            repository.save(STORED_KEY)

            assertEquals(STORED_KEY, repository.apiKey())
        }

    @Test
    fun `the key is not stored in the clear`() =
        runTest {
            repository.save(STORED_KEY)

            assertFalse(storedBytesContain(STORED_KEY))
        }

    /**
     * Whether [plaintext] appears in the bytes the store holds.
     *
     * The ciphertext is the store's only entry, so reading the single value is what reads the key
     * back as it was written rather than as the repository reports it.
     */
    private suspend fun storedBytesContain(plaintext: String): Boolean {
        val stored =
            dataStore.data
                .first()
                .asMap()
                .values
                .single() as ByteArray
        return stored.decodeToString().contains(plaintext)
    }
}
