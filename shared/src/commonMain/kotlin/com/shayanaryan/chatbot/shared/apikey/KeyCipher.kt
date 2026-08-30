package com.shayanaryan.chatbot.shared.apikey

/**
 * Encrypts and decrypts the user's API key. The one platform seam in the storage layer: on Android
 * a hardware-backed Keystore key, on a future iOS target the Keychain, and in tests a fake.
 *
 * The suspend calls are main-safe, implementations should dispatch their own work.
 */
interface KeyCipher {
    /** @return the ciphertext, safe to persist. */
    suspend fun encrypt(plaintext: String): ByteArray

    /** @return the original plaintext of [ciphertext] produced by this cipher. */
    suspend fun decrypt(ciphertext: ByteArray): String
}
