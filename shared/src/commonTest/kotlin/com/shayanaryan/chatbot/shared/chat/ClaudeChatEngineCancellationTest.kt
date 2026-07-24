package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ClaudeChatEngineCancellationTest {
    private val request =
        ChatRequest(messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))))

    @Test
    fun cancelling_mid_stream_emits_no_terminal_event() =
        runTest {
            val engine = testChatEngine { respondSse(SseFixtures.HAPPY_PATH) }

            val events = engine.stream(request).take(1).toList()

            assertEquals(1, events.size)
            assertIs<ChatStreamEvent.Delta>(events.single())
        }

    @Test
    fun the_flow_is_cold_and_restarts_per_collection() =
        runTest {
            var calls = 0
            val engine =
                testChatEngine {
                    calls++
                    respondSse(SseFixtures.HAPPY_PATH)
                }

            val flow = engine.stream(request)
            assertEquals(0, calls)

            flow.toList()
            flow.toList()

            assertEquals(2, calls)
        }

    @Test
    fun the_factory_builds_an_engine() {
        val engine = createChatEngine { "sk-ant-test" }

        assertIs<ChatEngine>(engine)
    }
}
