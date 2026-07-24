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
    fun `cancelling mid stream emits no terminal event`() =
        runTest {
            val engine = testChatEngine { respondSse(SseFixtures.HAPPY_PATH) }

            val events = engine.stream(request).take(1).toList()

            assertEquals(1, events.size)
            assertIs<ChatStreamEvent.Delta>(events.single())
        }

    @Test
    fun `the flow is cold and restarts per collection`() =
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
    fun `the factory builds an engine`() {
        val engine = createChatEngine { "sk-ant-test" }

        assertIs<ChatEngine>(engine)
    }
}
