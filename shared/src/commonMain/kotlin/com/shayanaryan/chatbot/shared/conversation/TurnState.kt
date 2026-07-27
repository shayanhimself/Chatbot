package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError

/**
 * The in-memory half of a conversation: the reply currently arriving, and how the last one ended.
 * Everything else about a conversation comes from the database.
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
     * The last turn did not finish. Readable until the next turn on this conversation replaces it;
     * the persisted message records only that it failed, never why.
     */
    data class Failed(
        val error: ChatError,
    ) : TurnState
}
