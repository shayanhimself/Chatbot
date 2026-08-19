package com.shayanaryan.chatbot.apikey

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.shared.apikey.TestApiKeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val STORED_KEY = "sk-ant-api03-keystore-round-trip"

/**
 * The repository over the cipher the app really ships, on hardware that really has a Keystore.
 *
 * The JVM equivalent substitutes the cipher, so a Keystore or Tink failure would pass there and
 * only surface on a device.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreStorageTest {
    @get:Rule
    val keyStore = TestApiKeyStore()

    @Test
    fun `a key encrypted by the Keystore decrypts back to itself`() =
        runTest {
            keyStore.repository.save(STORED_KEY)

            assertEquals(STORED_KEY, keyStore.repository.apiKey())
        }

    @Test
    fun `the key is not stored in the clear`() =
        runTest {
            keyStore.repository.save(STORED_KEY)

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
            keyStore.dataStore.data
                .first()
                .asMap()
                .values
                .single() as ByteArray
        return stored.decodeToString().contains(plaintext)
    }
}
