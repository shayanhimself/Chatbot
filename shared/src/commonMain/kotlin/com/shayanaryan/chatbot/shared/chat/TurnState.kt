package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError

/**
 * The in-memory half of a chat: the reply currently arriving, and how the last one ended.
 * Everything else about a chat comes from the database.
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

    /**
     * The last turn did not finish. Readable until nothing is collecting the chat's turn anymore,
     * or until the next turn on this chat replaces it; the persisted message records only
     * that it failed, never why.
     */
    data class Failed(
        val error: ApiError,
    ) : TurnState
}
