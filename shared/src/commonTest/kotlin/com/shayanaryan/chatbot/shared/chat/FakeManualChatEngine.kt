package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * A [ChatEngine] whose stream the test opens, feeds, and closes by hand, so an assertion can be
 * taken while a turn is still in flight.
 *
 * Usage is always: start collecting, [awaitStream] to wait for the collector to arrive, then
 * [send] events and [close] when the turn is over. A second collection is served the same way
 * once the first is closed.
 *
 * @property requests every request this engine was asked to stream, in call order.
 */
class FakeManualChatEngine : ChatEngine {
    private val recorded = mutableListOf<ChatRequest>()
    private val opened = Channel<Channel<ChatStreamEvent>>(Channel.UNLIMITED)
    private var current: Channel<ChatStreamEvent>? = null

    val requests: List<ChatRequest> get() = recorded.toList()

    /** Suspends until a collector opens a stream, which then becomes the target of [send]. */
    suspend fun awaitStream() {
        current = opened.receive()
    }

    suspend fun send(event: ChatStreamEvent) {
        requireNotNull(current) { "no stream is open; call awaitStream() first" }.send(event)
    }

    /** Ends the open stream, completing the collector. */
    fun close() {
        requireNotNull(current) { "no stream is open; call awaitStream() first" }.close()
        current = null
    }

    override fun stream(request: ChatRequest): Flow<ChatStreamEvent> =
        flow {
            val events = Channel<ChatStreamEvent>(Channel.UNLIMITED)
            recorded += request
            opened.send(events)
            emitAll(events.consumeAsFlow())
        }
}
