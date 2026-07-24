package com.shayanaryan.chatbot.shared.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface ChatStreamEvent {
    data class Delta(
        val text: String,
    ) : ChatStreamEvent

    data class Completed(
        val stopReason: StopReason,
        val usage: TokenUsage,
    ) : ChatStreamEvent

    data class Failed(
        val error: ChatError,
    ) : ChatStreamEvent
}

/**
 * Why an assistant turn stopped. Decoded straight off the wire — `@SerialName` per case — with an
 * unknown or absent server value coerced to [Unknown] (Json `coerceInputValues`), so a new stop
 * reason never fails the stream.
 */
@Serializable
enum class StopReason {
    @SerialName("end_turn")
    EndTurn,

    @SerialName("max_tokens")
    MaxTokens,

    @SerialName("stop_sequence")
    StopSequence,

    @SerialName("refusal")
    Refusal,

    Unknown,
}

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
)
