package com.shayanaryan.chatbot.shared.claude

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * A [ClaudeEngine] whose stream the test opens, feeds, and closes by hand, so an assertion can be
 * taken while a turn is still in flight.
 *
 * Usage is always: start collecting, [awaitStream] to wait for the collector to arrive, then
 * [send] events and [close] when the turn is over. A second collection is served the same way
 * once the first is closed.
 *
 * @property requests every request this engine was asked to stream, in collection order. A
 *   request is recorded when a collector arrives, not when [stream] is called.
 */
class FakeManualClaudeEngine : ClaudeEngine {
    private val recorded = mutableListOf<ClaudeMessageRequest>()
    private val opened = Channel<Channel<ClaudeStreamEvent>>(Channel.UNLIMITED)
    private var current: Channel<ClaudeStreamEvent>? = null

    val requests: List<ClaudeMessageRequest> get() = recorded.toList()

    /**
     * Suspends until a collector opens a stream, which then becomes the target of [send].
     *
     * Rejects a second call while a stream is still live: streams are handed over in the order
     * they were opened, so binding to one while another is running would silently target the
     * wrong collector and leave the test hanging until its timeout.
     * `isClosedForSend` is delicate because it is a snapshot that can go stale; here it is read
     * only as a guard, from the single test coroutine that also opens and closes every stream.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun awaitStream() {
        val open = current
        check(open == null || open.isClosedForSend) {
            "a stream is already open; close it before awaiting the next one"
        }
        current = opened.receive()
    }

    /**
     * Feeds one event to the open stream.
     *
     * Fails rather than suspending or throwing a cancellation if the collector is already gone,
     * which is otherwise indistinguishable from the test's own scope being cancelled.
     */
    suspend fun send(event: ClaudeStreamEvent) {
        val events = checkNotNull(current) { "no stream is open; call awaitStream() first" }
        check(events.trySend(event).isSuccess) {
            "the open stream has ended; nothing will receive $event"
        }
    }

    /**
     * Ends the open stream, completing the collector. A turn that ended the way the [ClaudeEngine]
     * contract describes sends its terminal `Completed` or `Failed` first; ending without one
     * stands for a stream that was cut off.
     */
    fun close() {
        checkNotNull(current) { "no stream is open; call awaitStream() first" }.close()
        current = null
    }

    override fun stream(request: ClaudeMessageRequest): Flow<ClaudeStreamEvent> =
        flow {
            val events = Channel<ClaudeStreamEvent>(Channel.UNLIMITED)
            recorded += request
            opened.send(events)
            emitAll(events.consumeAsFlow())
        }
}
