package com.shayanaryan.chatbot.shared.apikey

import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeystore

private const val MASTER_KEY_ALIAS = "chatbot_api_key_master"

// Bound into every ciphertext, decrypt only works if you pass the same label.
private val associatedData = "chatbot.api_key".encodeToByteArray()

/**
 * AES-256-GCM through a hardware-backed Android Keystore key. The key material never leaves the
 * Keystore; only its handle does, so a copy of the DataStore file is useless off the device.
 */
internal class KeystoreKeyCipher : KeyCipher {
    // AEAD = Authenticated Encryption with Associated Data.
    // Generation and lookup are not atomic against each other, so they run once behind lazy's
    // lock. The app is single-process, which is the boundary that makes that sufficient.
    private val aead: Aead by lazy {
        if (!AndroidKeystore.hasKey(MASTER_KEY_ALIAS)) {
            AndroidKeystore.generateNewAes256GcmKey(MASTER_KEY_ALIAS)
        }
        AndroidKeystore.getAead(MASTER_KEY_ALIAS)
    }

    override fun encrypt(plaintext: String): ByteArray =
        aead.encrypt(plaintext.encodeToByteArray(), associatedData)

    override fun decrypt(ciphertext: ByteArray): String =
        aead.decrypt(ciphertext, associatedData).decodeToString()
}

/** Builds the platform cipher. The master key is created on first use and reused after that. */
fun createKeystoreKeyCipher(): KeyCipher = KeystoreKeyCipher()
