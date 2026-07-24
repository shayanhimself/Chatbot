package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.Flow

/**
 * The app's sole interface to Claude. Stateless: callers pass the full conversation in, nothing
 * is persisted here.
 *
 * Retained despite the single production implementation so tests can substitute a fake and an
 * alternate implementation can slot in without touching callers.
 */
interface ChatEngine {
    /**
     * Streams one assistant turn.
     *
     * Returns a cold [Flow] — a new HTTP call starts per collection. Main-safe. The flow emits
     * exactly one terminal [ChatStreamEvent.Completed] or [ChatStreamEvent.Failed] and then
     * completes; it never throws for API or domain errors. Only structured cancellation
     * propagates, cancelling the in-flight HTTP call.
     *
     * @param request the full conversation plus model and generation settings.
     * @return the assistant's reply as it arrives.
     */
    fun stream(request: ChatRequest): Flow<ChatStreamEvent>
}
