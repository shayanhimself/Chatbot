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
    fun splits_a_recorded_stream_into_frames() =
        runTest {
            val frames = framesOf(SseFixtures.HAPPY_PATH)

            assertEquals(10, frames.size)
            assertEquals("message_start", frames.first().event)
            assertEquals("message_stop", frames.last().event)
            assertEquals("""{"type":"message_stop"}""", frames.last().data)
        }

    @Test
    fun strips_exactly_one_space_after_the_field_colon() =
        runTest {
            val frames = framesOf("event: ping\ndata: {\"type\": \"ping\"}\n\n")

            assertEquals("ping", frames.single().event)
            assertEquals("""{"type": "ping"}""", frames.single().data)
        }

    @Test
    fun joins_multi_line_data_with_newlines() =
        runTest {
            val frames = framesOf("data: one\ndata: two\n\n")

            assertEquals("one\ntwo", frames.single().data)
        }

    @Test
    fun ignores_comment_lines() =
        runTest {
            val frames = framesOf(": keep-alive\ndata: payload\n\n")

            assertEquals("payload", frames.single().data)
        }

    @Test
    fun emits_a_trailing_frame_that_was_never_blank_terminated() =
        runTest {
            val frames = framesOf("event: message_stop\ndata: {}")

            assertEquals("{}", frames.single().data)
        }

    @Test
    fun skips_frames_carrying_no_data() =
        runTest {
            assertEquals(listOf("real"), framesOf("event: ping\n\ndata: real\n\n").map { it.data })
        }

    @Test
    fun handles_crlf_line_endings() =
        runTest {
            val frames = framesOf("event: ping\r\ndata: payload\r\n\r\n")

            assertEquals("payload", frames.single().data)
        }
}
