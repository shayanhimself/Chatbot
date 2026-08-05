package com.shayanaryan.chatbot.shared.chat.sse

import com.shayanaryan.chatbot.shared.chat.SseFixtures
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val MESSAGE_START_EVENT = "message_start"
private const val MESSAGE_STOP_EVENT = "message_stop"
private const val MESSAGE_STOP_DATA = """{"type":"message_stop"}"""
private const val PING_EVENT = "ping"
private const val PING_DATA = """{"type": "ping"}"""
private const val PING_FRAME = "event: ping\ndata: {\"type\": \"ping\"}\n\n"

// One frame's data split across two `data:` lines, which the reader joins with a newline.
private const val TWO_DATA_LINE_FRAME = "data: one\ndata: two\n\n"
private const val TWO_DATA_LINES_JOINED = "one\ntwo"

// A comment line, which carries no data and only keeps the connection alive.
private const val KEEP_ALIVE_FRAME = ": keep-alive\ndata: payload\n\n"
private const val PAYLOAD_DATA = "payload"

// A stream that ends without its blank-line terminator.
private const val UNTERMINATED_FRAME = "event: message_stop\ndata: {}"
private const val EMPTY_JSON_DATA = "{}"

// An event line with no data, which yields no frame at all.
private const val EVENT_WITHOUT_DATA_FRAME = "event: ping\n\ndata: real\n\n"
private const val REAL_DATA = "real"

private const val CARRIAGE_RETURN_FRAME = "event: ping\r\ndata: payload\r\n\r\n"

class SseFrameReaderTest {
    private suspend fun framesOf(text: String): List<SseFrame> {
        val collected = mutableListOf<SseFrame>()
        ByteReadChannel(text.encodeToByteArray()).forEachSseFrame { collected += it }
        return collected
    }

    @Test
    fun `splits a recorded stream into frames`() =
        runTest {
            val frames = framesOf(SseFixtures.HAPPY_PATH)

            assertEquals(10, frames.size)
            assertEquals(MESSAGE_START_EVENT, frames.first().event)
            assertEquals(MESSAGE_STOP_EVENT, frames.last().event)
            assertEquals(MESSAGE_STOP_DATA, frames.last().data)
        }

    @Test
    fun `strips exactly one space after the field colon`() =
        runTest {
            val frames = framesOf(PING_FRAME)

            assertEquals(PING_EVENT, frames.single().event)
            assertEquals(PING_DATA, frames.single().data)
        }

    @Test
    fun `joins multi line data with newlines`() =
        runTest {
            val frames = framesOf(TWO_DATA_LINE_FRAME)

            assertEquals(TWO_DATA_LINES_JOINED, frames.single().data)
        }

    @Test
    fun `ignores comment lines`() =
        runTest {
            val frames = framesOf(KEEP_ALIVE_FRAME)

            assertEquals(PAYLOAD_DATA, frames.single().data)
        }

    @Test
    fun `emits a trailing frame that was never blank terminated`() =
        runTest {
            val frames = framesOf(UNTERMINATED_FRAME)

            assertEquals(EMPTY_JSON_DATA, frames.single().data)
        }

    @Test
    fun `skips frames carrying no data`() =
        runTest {
            assertEquals(listOf(REAL_DATA), framesOf(EVENT_WITHOUT_DATA_FRAME).map { it.data })
        }

    @Test
    fun `handles crlf line endings`() =
        runTest {
            val frames = framesOf(CARRIAGE_RETURN_FRAME)

            assertEquals(PAYLOAD_DATA, frames.single().data)
        }
}
