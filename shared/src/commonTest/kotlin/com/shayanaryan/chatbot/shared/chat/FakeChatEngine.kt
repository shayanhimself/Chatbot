package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits a scripted event list so feature specs can test their ViewModels against the
 * [ChatEngine] contract, never the network.
 *
 * @property events emitted in order on every collection.
 * @property requests every request this engine was asked to stream, in call order.
 */
class FakeChatEngine(
    var events: List<ChatStreamEvent> = emptyList(),
) : ChatEngine {
    private val recorded = mutableListOf<ChatRequest>()

    val requests: List<ChatRequest> get() = recorded.toList()

    override fun stream(request: ChatRequest): Flow<ChatStreamEvent> =
        flow {
            recorded += request
            events.forEach { emit(it) }
        }
}
