package com.shayanaryan.chatbot.shared.apikey

// Any non-zero mask works. It exists so ciphertext never equals plaintext, which is what lets a
// test assert the plaintext actually goes through the cipher.
private const val MASK = 0x5A

/**
 * Reversible byte flip standing in for real encryption.
 */
class FakeKeyCipher : KeyCipher {
    override fun encrypt(plaintext: String): ByteArray = plaintext.encodeToByteArray().mask()

    override fun decrypt(ciphertext: ByteArray): String = ciphertext.mask().decodeToString()

    /**
     * XORs every byte with [MASK]. Its own inverse, so one function serves both directions.
     */
    private fun ByteArray.mask(): ByteArray =
        ByteArray(size) { (this[it].toInt() xor MASK).toByte() }
}
