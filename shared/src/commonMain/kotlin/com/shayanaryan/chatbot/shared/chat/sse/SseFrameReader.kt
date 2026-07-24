package com.shayanaryan.chatbot.shared.chat.sse

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.LineEnding
import io.ktor.utils.io.readLine

/**
 * Reads the channel line by line, invoking [onFrame] for each complete server-sent event.
 *
 * The channel is consumed as it arrives — never buffered whole — so deltas surface the moment
 * the server writes them. A blank line ends the current frame; comment lines (starting `:`) are
 * ignored, as are frames that carry no data. Returns when the channel closes.
 */
internal suspend fun ByteReadChannel.forEachSseFrame(onFrame: suspend (SseFrame) -> Unit) {
    val builder = SseFrameBuilder()
    while (true) {
        val line = readLine(LineEnding.Lenient) ?: break
        when {
            line.isEmpty() -> builder.take()?.let { onFrame(it) }
            line.startsWith(":") -> Unit
            else -> builder.add(line)
        }
    }
    builder.take()?.let { onFrame(it) }
}

/**
 * Accumulates the `event:`/`data:` field lines of one frame. [take] returns the built frame and
 * resets for the next one, or null when no data accumulated — an event carrying no data is never
 * dispatched.
 */
private class SseFrameBuilder {
    private var event: String? = null
    private val data = StringBuilder()

    fun add(fieldLine: String) {
        val (field, value) = splitField(fieldLine)
        when (field) {
            "event" -> {
                event = value
            }

            "data" -> {
                if (data.isNotEmpty()) data.append('\n')
                data.append(value)
            }
        }
    }

    fun take(): SseFrame? =
        (if (data.isEmpty()) null else SseFrame(event, data.toString())).also {
            event = null
            data.clear()
        }
}

/** Splits a field line into its name and value, stripping a single optional space after the colon. */
private fun splitField(line: String): Pair<String, String> {
    val colon = line.indexOf(':')
    if (colon == -1) return line to ""
    return line.substring(0, colon) to line.substring(colon + 1).removePrefix(" ")
}
