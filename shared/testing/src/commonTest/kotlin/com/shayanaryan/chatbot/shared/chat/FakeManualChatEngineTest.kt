package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FakeManualChatEngineTest {
    private val request =
        ChatRequest(messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))))

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `holds the stream open until it is closed`() =
        runTest {
            val engine = FakeManualChatEngine()
            val collected = mutableListOf<ChatStreamEvent>()
            val collector = launch { engine.stream(request).toList(collected) }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("one"))
            runCurrent()

            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("one")), collected)
            assertTrue(collector.isActive)

            engine.send(ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)))
            engine.close()
            collector.join()

            assertEquals(2, collected.size)
        }

    @Test
    fun `records the request it was asked to stream`() =
        runTest {
            val engine = FakeManualChatEngine()
            val collector = launch { engine.stream(request).toList() }

            engine.awaitStream()
            engine.close()
            collector.join()

            assertEquals(listOf(request), engine.requests)
        }

    @Test
    fun `serves a second stream after the first is closed`() =
        runTest {
            val engine = FakeManualChatEngine()
            val first = mutableListOf<ChatStreamEvent>()
            val firstCollector = launch { engine.stream(request).toList(first) }
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("one"))
            engine.close()
            firstCollector.join()

            val second = mutableListOf<ChatStreamEvent>()
            val secondCollector = launch { engine.stream(request).toList(second) }
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("two"))
            engine.close()
            secondCollector.join()

            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("one")), first)
            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("two")), second)
            assertEquals(2, engine.requests.size)
        }

    @Test
    fun `refuses to await a second stream while one is still open`() =
        runTest {
            val engine = FakeManualChatEngine()
            val first = launch { engine.stream(request).toList() }
            engine.awaitStream()
            val second = launch { engine.stream(request).toList() }

            assertFailsWith<IllegalStateException> { engine.awaitStream() }

            engine.close()
            first.join()
            second.cancel()
        }

    @Test
    fun `refuses an event once its collector is gone`() =
        runTest {
            val engine = FakeManualChatEngine()
            val collector = launch { engine.stream(request).toList() }
            engine.awaitStream()
            collector.cancel()
            collector.join()

            assertFailsWith<IllegalStateException> { engine.send(ChatStreamEvent.Delta("late")) }
        }
}
