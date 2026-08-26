package com.shayanaryan.chatbot.shared.chat

/**
 * The in-memory half of a chat: the reply currently arriving. Everything else about a chat comes
 * from the database, including the error that ended a turn.
 */
sealed interface TurnState {
    /** No reply is in flight. Whatever the last turn produced is already a persisted message. */
    data object Idle : TurnState

    /**
     * @property text the reply so far, cumulative rather than the latest delta, so a collector
     *   renders it directly and accumulates nothing.
     */
    data class Streaming(
        val text: String,
    ) : TurnState
}
