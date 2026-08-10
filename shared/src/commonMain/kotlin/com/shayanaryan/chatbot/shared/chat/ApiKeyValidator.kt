package com.shayanaryan.chatbot.shared.chat

/**
 * Checks a key against the Anthropic API before it is stored.
 */
fun interface ApiKeyValidator {
    /**
     * Performs one authenticated request that consumes no tokens.
     *
     * @return [KeyValidationResult.Valid] when the API accepted the key, otherwise the failure as
     *   data. Never throws for a network or server problem.
     */
    suspend fun validate(key: String): KeyValidationResult
}

/**
 * The outcome of one validation attempt.
 *
 * [Failed] does not mean the key is wrong. Only [ChatError.Authentication] says that; every other
 * error leaves the key's validity unknown, so that the user can understand the error.
 */
sealed interface KeyValidationResult {
    data object Valid : KeyValidationResult

    data class Failed(
        val error: ChatError,
    ) : KeyValidationResult
}
