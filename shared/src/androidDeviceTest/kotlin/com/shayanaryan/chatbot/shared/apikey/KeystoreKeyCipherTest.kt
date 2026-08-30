package com.shayanaryan.chatbot.shared.apikey

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val API_KEY = "sk-ant-api03-not-a-real-key"

@RunWith(AndroidJUnit4::class)
class KeystoreKeyCipherTest {
    @Test
    fun `ciphertext round trips through the keystore`() =
        runTest {
            val cipher = createKeystoreKeyCipher()

            val ciphertext = cipher.encrypt(API_KEY)

            assertFalse(ciphertext.decodeToString().contains(API_KEY))
            assertEquals(API_KEY, cipher.decrypt(ciphertext))
        }

    @Test
    fun `a second cipher reads what the first wrote`() =
        runTest {
            val ciphertext = createKeystoreKeyCipher().encrypt(API_KEY)

            // The master key outlives the cipher object, which is what makes a stored ciphertext
            // readable on the next launch.
            assertEquals(API_KEY, createKeystoreKeyCipher().decrypt(ciphertext))
        }

    @Test
    fun `encrypting twice produces different ciphertext`() =
        runTest {
            val cipher = createKeystoreKeyCipher()

            val first = cipher.encrypt(API_KEY)
            val second = cipher.encrypt(API_KEY)

            // AES-GCM uses a fresh nonce per call, so identical plaintext must not produce identical
            // bytes. Equal ciphertext would mean the nonce is being reused, which breaks GCM outright.
            assertFalse(first.contentEquals(second))
            assertEquals(cipher.decrypt(first), cipher.decrypt(second))
        }
}
