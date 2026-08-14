package com.shayanaryan.chatbot.shared.claude
import com.shayanaryan.chatbot.shared.ApiError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import kotlinx.io.IOException

private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1"
internal const val MESSAGES_URL = "$ANTHROPIC_BASE_URL/messages"
internal const val MODELS_URL = "$ANTHROPIC_BASE_URL/models"
internal const val ANTHROPIC_VERSION_HEADER = "anthropic-version"
internal const val ANTHROPIC_VERSION = "2023-06-01"
internal const val API_KEY_HEADER = "x-api-key"

internal const val LIMIT_PARAM = "limit"
internal const val SSE_CONTENT_TYPE = "text/event-stream"

private const val RETRY_AFTER_HEADER = "retry-after"

/**
 * Maps a non-2xx response onto a [ApiError], reading the `retry-after` hint when present.
 *
 * Only 401 and 403 say anything about the key itself. Everything else leaves the key's validity
 * unknown, which is what stops a throttled request from being reported as a bad key.
 */
internal fun HttpResponse.toApiError(): ApiError =
    when (status.value) {
        401, 403 -> ApiError.Authentication
        408, 504 -> ApiError.Timeout
        429 -> ApiError.RateLimited(headers[RETRY_AFTER_HEADER]?.toIntOrNull())
        529 -> ApiError.Overloaded
        in 400..499 -> ApiError.InvalidRequest
        in 500..599 -> ApiError.Server
        else -> ApiError.Unexpected
    }

/**
 * Maps a transport or decoding failure onto a [ApiError]. Order matters: timeouts are
 * IOExceptions.
 */
internal fun Throwable.toApiError(): ApiError =
    when (this) {
        is HttpRequestTimeoutException, is SocketTimeoutException, is ConnectTimeoutException -> {
            ApiError.Timeout
        }

        is IOException -> {
            ApiError.Network
        }

        else -> {
            ApiError.Unexpected
        }
    }
