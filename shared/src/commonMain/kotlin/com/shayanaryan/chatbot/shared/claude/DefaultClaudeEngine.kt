package com.shayanaryan.chatbot.shared.claude
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.claude.dto.ApiErrorType
import com.shayanaryan.chatbot.shared.claude.dto.ContentDeltaDto
import com.shayanaryan.chatbot.shared.claude.dto.MessageRequestDto
import com.shayanaryan.chatbot.shared.claude.dto.SseEventDto
import com.shayanaryan.chatbot.shared.claude.dto.toDto
import com.shayanaryan.chatbot.shared.claude.sse.forEachSseFrame
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
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

/** The one production [ClaudeEngine]: Ktor over the Anthropic Messages API with hand-rolled SSE. */
internal class DefaultClaudeEngine(
    private val client: HttpClient,
    private val repository: ApiKeyRepository,
) : ClaudeEngine {
    override fun stream(request: ClaudeMessageRequest): Flow<ClaudeStreamEvent> =
        flow {
            val apiKey =
                checkNotNull(repository.apiKey()) {
                    "No stored API key. The gate should have kept the app on onboarding."
                }
            val body = claudeJson.encodeToString(MessageRequestDto.serializer(), request.toDto())
            client
                .preparePost(MESSAGES_URL) {
                    header(API_KEY_HEADER, apiKey)
                    header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                    header(HttpHeaders.Accept, SSE_CONTENT_TYPE)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        emit(ClaudeStreamEvent.Failed(response.toApiError()))
                        return@execute
                    }
                    emitStream(response.bodyAsChannel())
                }
        }.catch { cause ->
            if (cause is CancellationException) throw cause
            emit(ClaudeStreamEvent.Failed(cause.toApiError()))
        }
}

/**
 * Thrown once a terminal frame has been emitted to stop reading the channel immediately, so no
 * later frame or transport error can produce a second terminal event.
 */
private class TerminalReached : Exception()

/**
 * Folds SSE frames into stream events. Accumulates usage and stop reason as they arrive, emits a
 * [ClaudeStreamEvent.Delta] per `text_delta`, and guarantees exactly one terminal event: reading
 * stops the moment one is emitted, and a stream that closes without `message_stop` is a
 * truncation, which is [ApiError.Unexpected].
 */
private suspend fun FlowCollector<ClaudeStreamEvent>.emitStream(channel: ByteReadChannel) {
    var inputTokens = 0
    var outputTokens = 0
    var stopReason = StopReason.Unknown

    try {
        channel.forEachSseFrame { frame ->
            val event =
                try {
                    claudeJson.decodeFromString(SseEventDto.serializer(), frame.data)
                } catch (_: IllegalArgumentException) {
                    // SerializationException extends IllegalArgumentException, so this covers both
                    // a malformed payload and a truncated frame.
                    emit(ClaudeStreamEvent.Failed(ApiError.Unexpected))
                    throw TerminalReached()
                }
            when (event) {
                is SseEventDto.MessageStart -> {
                    // The opening frame, and the only one carrying the input token count.
                    // {"type":"message_start","message":{"usage":{"input_tokens":42}}}
                    inputTokens = event.message.usage.inputTokens
                }

                is SseEventDto.ContentBlockDelta -> {
                    // One slice of one content block, arriving many times per response.
                    // {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hi"}}
                    (event.delta as? ContentDeltaDto.Text)?.let {
                        emit(ClaudeStreamEvent.Delta(it.text))
                    }
                }

                is SseEventDto.MessageDelta -> {
                    // The last data frame, and the only one carrying stop reason and output tokens.
                    // {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{...}}
                    stopReason = event.delta.stopReason
                    event.usage?.let { outputTokens = it.outputTokens }
                }

                is SseEventDto.MessageStop -> {
                    // The bare terminator, which makes the totals gathered above final.
                    // {"type":"message_stop"}
                    emit(
                        ClaudeStreamEvent.Completed(
                            stopReason,
                            TokenUsage(inputTokens, outputTokens),
                        ),
                    )
                    throw TerminalReached()
                }

                is SseEventDto.Error -> {
                    // A failure raised mid-stream, after the response status already said success.
                    // {"type":"error","error":{"type":"overloaded_error","message":"Overloaded"}}
                    emit(ClaudeStreamEvent.Failed(event.error.type.toApiError()))
                    throw TerminalReached()
                }

                SseEventDto.Unknown -> {
                    // Frame types the engine does not act on (ping, block start/stop) are ignored.
                }
            }
        }
        emit(ClaudeStreamEvent.Failed(ApiError.Unexpected))
    } catch (_: TerminalReached) {
        // no-op. Terminal event already emitted; the throw only stops the read.
    }
}

private fun ApiErrorType.toApiError(): ApiError =
    when (this) {
        ApiErrorType.Authentication, ApiErrorType.Permission -> {
            ApiError.Authentication
        }

        ApiErrorType.RateLimit -> {
            ApiError.RateLimited(null)
        }

        ApiErrorType.Overloaded -> {
            ApiError.Overloaded
        }

        ApiErrorType.InvalidRequest, ApiErrorType.NotFound, ApiErrorType.RequestTooLarge -> {
            ApiError.InvalidRequest
        }

        ApiErrorType.Api -> {
            ApiError.Server
        }

        ApiErrorType.Unknown -> {
            ApiError.Unexpected
        }
    }
