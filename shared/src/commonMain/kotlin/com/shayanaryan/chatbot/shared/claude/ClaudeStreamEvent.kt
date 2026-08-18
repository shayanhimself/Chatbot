package com.shayanaryan.chatbot.shared.claude
import com.shayanaryan.chatbot.shared.ApiError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One event in a streamed assistant turn. A stream emits zero or more [Delta]s and then exactly
 * one terminal event: [Completed] on success, [Failed] on any API or domain error.
 */
sealed interface ClaudeStreamEvent {
    data class Delta(
        val text: String,
    ) : ClaudeStreamEvent

    data class Completed(
        val stopReason: StopReason,
        val usage: TokenUsage,
    ) : ClaudeStreamEvent

    data class Failed(
        val error: ApiError,
    ) : ClaudeStreamEvent
}

/**
 * Why an assistant turn stopped. Decoded straight off the wire (`@SerialName` per case) with an
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
