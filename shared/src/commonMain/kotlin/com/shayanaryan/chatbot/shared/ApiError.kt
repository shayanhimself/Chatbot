package com.shayanaryan.chatbot.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every way a call to the API can fail, as data. Deliberately carries no text a user reads:
 * feature ViewModels map each case to a string resource.
 *
 * Lives outside both the wire package and the storage package because neither owns it: the engine
 * and the key validator produce it, and unrelated features consume it.
 *
 * Serializable because a reply that failed stores the error that ended it.
 */
@Serializable
sealed interface ApiError {
    /**
     * Adding `@SerialName` so renaming a case leaves rows already on disk readable.
     */
    @Serializable
    @SerialName("authentication")
    data object Authentication : ApiError

    /**
     * @property retryAfterSeconds the server's `retry-after` hint, absent when the header was
     *   missing or unparseable. The engine itself never retries.
     */
    @Serializable
    @SerialName("rate_limited")
    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : ApiError

    @Serializable
    @SerialName("overloaded")
    data object Overloaded : ApiError

    @Serializable
    @SerialName("invalid_request")
    data object InvalidRequest : ApiError

    @Serializable
    @SerialName("server")
    data object Server : ApiError

    @Serializable
    @SerialName("network")
    data object Network : ApiError

    @Serializable
    @SerialName("timeout")
    data object Timeout : ApiError

    @Serializable
    @SerialName("unexpected")
    data object Unexpected : ApiError
}
