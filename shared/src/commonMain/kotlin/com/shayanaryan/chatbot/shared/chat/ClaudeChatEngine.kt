package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.chat.dto.ApiErrorType
import com.shayanaryan.chatbot.shared.chat.dto.ContentDeltaDto
import com.shayanaryan.chatbot.shared.chat.dto.MessageRequestDto
import com.shayanaryan.chatbot.shared.chat.dto.SseEventDto
import com.shayanaryan.chatbot.shared.chat.dto.toDto
import com.shayanaryan.chatbot.shared.chat.sse.forEachSseFrame
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION_HEADER = "anthropic-version"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val API_KEY_HEADER = "x-api-key"
private const val SSE_CONTENT_TYPE = "text/event-stream"

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
                    emitStream(response.bodyAsChannel())
                }
        }
}

/**
 * Folds SSE frames into stream events. Accumulates usage and stop reason as they arrive, emits a
 * [ChatStreamEvent.Delta] per `text_delta`, and guarantees exactly one terminal event: a stream
 * that ends without `message_stop` is a truncation, which is [ChatError.Unexpected].
 */
private suspend fun FlowCollector<ChatStreamEvent>.emitStream(channel: ByteReadChannel) {
    var inputTokens = 0
    var outputTokens = 0
    var stopReason = StopReason.Unknown
    var terminated = false

    channel.forEachSseFrame { frame ->
        if (terminated) return@forEachSseFrame
        val event =
            try {
                chatJson.decodeFromString(SseEventDto.serializer(), frame.data)
            } catch (_: IllegalArgumentException) {
                // SerializationException extends IllegalArgumentException, so this covers both a
                // malformed payload and a truncated frame.
                emit(ChatStreamEvent.Failed(ChatError.Unexpected))
                terminated = true
                return@forEachSseFrame
            }
        when (event) {
            is SseEventDto.MessageStart -> {
                inputTokens = event.message.usage.inputTokens
            }

            is SseEventDto.ContentBlockDelta -> {
                (event.delta as? ContentDeltaDto.Text)?.let { emit(ChatStreamEvent.Delta(it.text)) }
            }

            is SseEventDto.MessageDelta -> {
                stopReason = event.delta.stopReason
                event.usage?.let { outputTokens = it.outputTokens }
            }

            is SseEventDto.MessageStop -> {
                emit(
                    ChatStreamEvent.Completed(stopReason, TokenUsage(inputTokens, outputTokens)),
                )
                terminated = true
            }

            is SseEventDto.Error -> {
                emit(ChatStreamEvent.Failed(event.error.type.toChatError()))
                terminated = true
            }

            SseEventDto.Unknown -> Unit
        }
    }

    if (!terminated) {
        emit(ChatStreamEvent.Failed(ChatError.Unexpected))
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
