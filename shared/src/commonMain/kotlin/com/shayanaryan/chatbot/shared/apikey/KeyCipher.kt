package com.shayanaryan.chatbot.shared.apikey

/**
 * Encrypts and decrypts the user's API key. The one platform seam in the storage layer: on Android
 * a hardware-backed Keystore key, on a future iOS target the Keychain, and in tests a fake. Both
 * directions are synchronous and cheap, so callers do not need to dispatch around them.
 */
interface KeyCipher {
    /** @return the ciphertext, safe to persist. */
    fun encrypt(plaintext: String): ByteArray

    /** @return the original plaintext of [ciphertext] produced by this cipher. */
    fun decrypt(ciphertext: ByteArray): String
}
