package com.shayanaryan.chatbot.shared.chat

/**
 * Every way a turn can fail, as data. Deliberately carries no text a user reads — feature
 * ViewModels map each case to a string resource.
 */
sealed interface ChatError {
    data object Authentication : ChatError

    /**
     * @property retryAfterSeconds the server's `retry-after` hint, absent when the header was
     *   missing or unparseable. The engine itself never retries.
     */
    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : ChatError

    data object Overloaded : ChatError

    data object InvalidRequest : ChatError

    data object Server : ChatError

    data object Network : ChatError

    data object Timeout : ChatError

    data object Unexpected : ChatError
}
