package com.shayanaryan.chatbot.shared.chat.sse

import com.shayanaryan.chatbot.shared.chat.SseFixtures
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
            assertEquals("message_start", frames.first().event)
            assertEquals("message_stop", frames.last().event)
            assertEquals("""{"type":"message_stop"}""", frames.last().data)
        }

    @Test
    fun `strips exactly one space after the field colon`() =
        runTest {
            val frames = framesOf("event: ping\ndata: {\"type\": \"ping\"}\n\n")

            assertEquals("ping", frames.single().event)
            assertEquals("""{"type": "ping"}""", frames.single().data)
        }

    @Test
    fun `joins multi line data with newlines`() =
        runTest {
            val frames = framesOf("data: one\ndata: two\n\n")

            assertEquals("one\ntwo", frames.single().data)
        }

    @Test
    fun `ignores comment lines`() =
        runTest {
            val frames = framesOf(": keep-alive\ndata: payload\n\n")

            assertEquals("payload", frames.single().data)
        }

    @Test
    fun `emits a trailing frame that was never blank terminated`() =
        runTest {
            val frames = framesOf("event: message_stop\ndata: {}")

            assertEquals("{}", frames.single().data)
        }

    @Test
    fun `skips frames carrying no data`() =
        runTest {
            assertEquals(listOf("real"), framesOf("event: ping\n\ndata: real\n\n").map { it.data })
        }

    @Test
    fun `handles crlf line endings`() =
        runTest {
            val frames = framesOf("event: ping\r\ndata: payload\r\n\r\n")

            assertEquals("payload", frames.single().data)
        }
}
