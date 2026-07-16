package com.shayanaryan.chatbot.shared.model

enum class ClaudeModel(
    val id: String,
    val displayName: String,
) {
    Sonnet("claude-sonnet-5", "Sonnet"),
    Haiku("claude-haiku-4-5", "Haiku"),
    Opus("claude-opus-4-8", "Opus"),
    ;

    companion object {
        val Default = Sonnet
    }
}
