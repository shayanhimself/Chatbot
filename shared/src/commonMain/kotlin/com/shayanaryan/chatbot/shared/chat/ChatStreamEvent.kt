package com.shayanaryan.chatbot.shared.chat

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

enum class StopReason { EndTurn, MaxTokens, StopSequence, Refusal, Unknown }

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
)
