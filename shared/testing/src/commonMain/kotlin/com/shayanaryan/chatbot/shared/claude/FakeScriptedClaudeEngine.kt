package com.shayanaryan.chatbot.shared.claude

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits a scripted event list so feature specs can test their ViewModels against the
 * [ClaudeEngine] contract, never the network.
 *
 * @property events emitted in order on every collection.
 * @property requests every request this engine was asked to stream, in collection order. A
 *   request is recorded when a collector arrives, not when [stream] is called.
 */
class FakeScriptedClaudeEngine(
    var events: List<ClaudeStreamEvent> = emptyList(),
) : ClaudeEngine {
    private val recorded = mutableListOf<ClaudeMessageRequest>()

    val requests: List<ClaudeMessageRequest> get() = recorded.toList()

    override fun stream(request: ClaudeMessageRequest): Flow<ClaudeStreamEvent> =
        flow {
            recorded += request
            events.forEach { emit(it) }
        }
}
