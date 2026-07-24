package com.shayanaryan.chatbot.shared.chat.sse

/**
 * One server-sent event (SSE).
 *
 * SSE is a streaming HTTP protocol: the server holds one long-lived response open and pushes a
 * sequence of text events as they are produced, rather than sending a single buffered body. Each
 * event is a block of `field: value` lines terminated by a blank line; the Messages API uses it to
 * stream a reply token by token.
 *
 * A frame is one such block — the unit the stream is chopped into. This is its parsed form: the
 * `event`/`data` fields lifted out, everything else (comments, blank-line terminator) discarded.
 *
 * @property event the `event:` field, absent when the server omitted it.
 * @property data the `data:` field; multiple data lines are joined with newlines.
 */
internal data class SseFrame(
    val event: String?,
    val data: String,
)
