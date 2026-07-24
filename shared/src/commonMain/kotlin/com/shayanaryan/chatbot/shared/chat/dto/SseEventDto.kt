package com.shayanaryan.chatbot.shared.chat.dto

import com.shayanaryan.chatbot.shared.chat.StopReason
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * One decoded SSE frame from the Messages stream, dispatched on the wire `type` discriminator.
 * Frame types the engine does not act on (`ping`, `content_block_start`, `content_block_stop`)
 * decode to [Unknown] via a polymorphic default rather than failing the parse.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
internal sealed interface SseEventDto {
    @Serializable
    @SerialName("message_start")
    data class MessageStart(
        val message: MessageStartBody,
    ) : SseEventDto

    @Serializable
    @SerialName("content_block_delta")
    data class ContentBlockDelta(
        val delta: ContentDeltaDto,
    ) : SseEventDto

    @Serializable
    @SerialName("message_delta")
    data class MessageDelta(
        val delta: MessageDeltaBody,
        val usage: UsageDto? = null,
    ) : SseEventDto

    @Serializable
    @SerialName("message_stop")
    data object MessageStop : SseEventDto

    @Serializable
    @SerialName("error")
    data class Error(
        val error: ApiErrorDto,
    ) : SseEventDto

    @Serializable
    data object Unknown : SseEventDto
}

@Serializable
internal data class MessageStartBody(
    val usage: UsageDto = UsageDto(),
)

/**
 * A `content_block_delta`'s inner delta, sealed on its own `type`. Only [Text] carries
 * user-visible output; every other kind (e.g. `thinking_delta`) decodes to [Other] and is skipped.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
internal sealed interface ContentDeltaDto {
    @Serializable
    @SerialName("text_delta")
    data class Text(
        val text: String,
    ) : ContentDeltaDto

    @Serializable
    data object Other : ContentDeltaDto
}

@Serializable
internal data class MessageDeltaBody(
    @SerialName("stop_reason") val stopReason: StopReason = StopReason.Unknown,
)

@Serializable
internal data class UsageDto(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
)

@Serializable
internal data class ApiErrorDto(
    val type: ApiErrorType = ApiErrorType.Unknown,
)

/**
 * The Anthropic SSE `error.type` vocabulary. Decoded off the wire — `@SerialName` per case — with
 * any unknown or absent value coerced to [Unknown] (Json `coerceInputValues`), so a new server
 * error type never fails the stream. Collapsed onto a [ChatError] by the engine.
 */
@Serializable
internal enum class ApiErrorType {
    @SerialName("authentication_error")
    Authentication,

    @SerialName("permission_error")
    Permission,

    @SerialName("rate_limit_error")
    RateLimit,

    @SerialName("overloaded_error")
    Overloaded,

    @SerialName("invalid_request_error")
    InvalidRequest,

    @SerialName("not_found_error")
    NotFound,

    @SerialName("request_too_large")
    RequestTooLarge,

    @SerialName("api_error")
    Api,

    Unknown,
}
