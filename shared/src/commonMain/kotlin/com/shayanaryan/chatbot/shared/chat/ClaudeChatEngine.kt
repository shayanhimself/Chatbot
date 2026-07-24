package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.chat.dto.ApiErrorType
import com.shayanaryan.chatbot.shared.chat.dto.ContentDeltaDto
import com.shayanaryan.chatbot.shared.chat.dto.MessageRequestDto
import com.shayanaryan.chatbot.shared.chat.dto.SseEventDto
import com.shayanaryan.chatbot.shared.chat.dto.toDto
import com.shayanaryan.chatbot.shared.chat.sse.forEachSseFrame
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException

private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION_HEADER = "anthropic-version"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val API_KEY_HEADER = "x-api-key"
private const val SSE_CONTENT_TYPE = "text/event-stream"
private const val RETRY_AFTER_HEADER = "retry-after"
private const val STATUS_OVERLOADED = 529

/** The one production [ChatEngine]: Ktor over the Anthropic Messages API with hand-rolled SSE. */
internal class ClaudeChatEngine(
    private val client: HttpClient,
    private val keyProvider: ApiKeyProvider,
) : ChatEngine {
    override fun stream(request: ChatRequest): Flow<ChatStreamEvent> =
        flow {
            val apiKey = keyProvider.apiKey()
            val body = chatJson.encodeToString(MessageRequestDto.serializer(), request.toDto())
            client
                .preparePost(MESSAGES_URL) {
                    header(API_KEY_HEADER, apiKey)
                    header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                    header(HttpHeaders.Accept, SSE_CONTENT_TYPE)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        emit(ChatStreamEvent.Failed(response.toChatError()))
                        return@execute
                    }
                    emitStream(response.bodyAsChannel())
                }
        }.catch { cause ->
            if (cause is CancellationException) throw cause
            emit(ChatStreamEvent.Failed(cause.toChatError()))
        }
}

/**
 * Thrown once a terminal frame has been emitted to stop reading the channel immediately, so no
 * later frame or transport error can produce a second terminal event.
 */
private class TerminalReached : Exception()

/**
 * Folds SSE frames into stream events. Accumulates usage and stop reason as they arrive, emits a
 * [ChatStreamEvent.Delta] per `text_delta`, and guarantees exactly one terminal event: reading
 * stops the moment one is emitted, and a stream that closes without `message_stop` is a
 * truncation, which is [ChatError.Unexpected].
 */
private suspend fun FlowCollector<ChatStreamEvent>.emitStream(channel: ByteReadChannel) {
    var inputTokens = 0
    var outputTokens = 0
    var stopReason = StopReason.Unknown

    try {
        channel.forEachSseFrame { frame ->
            val event =
                try {
                    chatJson.decodeFromString(SseEventDto.serializer(), frame.data)
                } catch (_: IllegalArgumentException) {
                    // SerializationException extends IllegalArgumentException, so this covers both
                    // a malformed payload and a truncated frame.
                    emit(ChatStreamEvent.Failed(ChatError.Unexpected))
                    throw TerminalReached()
                }
            when (event) {
                is SseEventDto.MessageStart -> {
                    inputTokens = event.message.usage.inputTokens
                }

                is SseEventDto.ContentBlockDelta -> {
                    (event.delta as? ContentDeltaDto.Text)?.let {
                        emit(ChatStreamEvent.Delta(it.text))
                    }
                }

                is SseEventDto.MessageDelta -> {
                    stopReason = event.delta.stopReason
                    event.usage?.let { outputTokens = it.outputTokens }
                }

                is SseEventDto.MessageStop -> {
                    emit(
                        ChatStreamEvent.Completed(
                            stopReason,
                            TokenUsage(inputTokens, outputTokens),
                        ),
                    )
                    throw TerminalReached()
                }

                is SseEventDto.Error -> {
                    emit(ChatStreamEvent.Failed(event.error.type.toChatError()))
                    throw TerminalReached()
                }

                SseEventDto.Unknown -> {
                    // Frame types the engine does not act on (ping, block start/stop) are ignored.
                }
            }
        }
        emit(ChatStreamEvent.Failed(ChatError.Unexpected))
    } catch (_: TerminalReached) {
        // no-op. Terminal event already emitted; the throw only stops the read.
    }
}

/** Maps a non-2xx response onto a [ChatError], reading the `retry-after` hint when present. */
private fun HttpResponse.toChatError(): ChatError =
    when (status.value) {
        401, 403 -> ChatError.Authentication
        408, 504 -> ChatError.Timeout
        429 -> ChatError.RateLimited(headers[RETRY_AFTER_HEADER]?.toIntOrNull())
        STATUS_OVERLOADED -> ChatError.Overloaded
        in 400..499 -> ChatError.InvalidRequest
        in 500..599 -> ChatError.Server
        else -> ChatError.Unexpected
    }

/**
 * Maps a transport or decoding failure onto a [ChatError]. Order matters: timeouts are
 * IOExceptions.
 */
private fun Throwable.toChatError(): ChatError =
    when (this) {
        is HttpRequestTimeoutException, is SocketTimeoutException, is ConnectTimeoutException -> {
            ChatError.Timeout
        }

        is IOException -> {
            ChatError.Network
        }

        else -> {
            ChatError.Unexpected
        }
    }

private fun ApiErrorType.toChatError(): ChatError =
    when (this) {
        ApiErrorType.Authentication, ApiErrorType.Permission -> {
            ChatError.Authentication
        }

        ApiErrorType.RateLimit -> {
            ChatError.RateLimited(null)
        }

        ApiErrorType.Overloaded -> {
            ChatError.Overloaded
        }

        ApiErrorType.InvalidRequest, ApiErrorType.NotFound, ApiErrorType.RequestTooLarge -> {
            ChatError.InvalidRequest
        }

        ApiErrorType.Api -> {
            ChatError.Server
        }

        ApiErrorType.Unknown -> {
            ChatError.Unexpected
        }
    }
