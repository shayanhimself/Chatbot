package com.shayanaryan.chatbot.shared

/**
 * Every way a call to the API can fail, as data. Deliberately carries no text a user reads —
 * feature ViewModels map each case to a string resource.
 *
 * Lives outside both the wire package and the storage package because neither owns it: the engine
 * and the key validator produce it, and unrelated features consume it.
 */
sealed interface ApiError {
    data object Authentication : ApiError

    /**
     * @property retryAfterSeconds the server's `retry-after` hint, absent when the header was
     *   missing or unparseable. The engine itself never retries.
     */
    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : ApiError

    data object Overloaded : ApiError

    data object InvalidRequest : ApiError

    data object Server : ApiError

    data object Network : ApiError

    data object Timeout : ApiError

    data object Unexpected : ApiError
}
