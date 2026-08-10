package com.shayanaryan.chatbot.shared.apikey

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * In-memory [ApiKeyRepository] for tests above the storage layer. There is no cipher: what is
 * stored is what was saved, since nothing above this layer can tell the difference.
 *
 * @param initialKey the key the store starts with, or null for a first launch.
 */
class FakeApiKeyRepository(
    initialKey: String? = null,
) : ApiKeyRepository {
    private val stored = MutableStateFlow(initialKey)

    /** How many times [save] has been called, so a test can assert a key was written exactly once. */
    var saveCount: Int = 0
        private set

    override fun hasKeyFlow(): Flow<Boolean> = stored.map { it != null }.distinctUntilChanged()

    override suspend fun apiKey(): String? = stored.value

    override suspend fun save(key: String) {
        saveCount++
        stored.value = key
    }

    override suspend fun clear() {
        stored.value = null
    }
}
