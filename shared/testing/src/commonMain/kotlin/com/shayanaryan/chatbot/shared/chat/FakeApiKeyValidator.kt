package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.CompletableDeferred

/**
 * [ApiKeyValidator] whose answer a test chooses.
 *
 * By default [validate] returns immediately. Set [gate] to hold it open, which is how a test
 * observes the screen while a check is in flight.
 */
class FakeApiKeyValidator(
    var result: KeyValidationResult = KeyValidationResult.Valid,
) : ApiKeyValidator {
    /** The keys passed to [validate], in order. */
    val validated: MutableList<String> = mutableListOf()

    /** Completed to release a held call; null lets every call return at once. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun validate(key: String): KeyValidationResult {
        validated += key
        gate?.await()
        return result
    }
}
