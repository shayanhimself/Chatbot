package com.shayanaryan.chatbot.shared.claude

import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val USER_MESSAGE = "hi"
private const val API_KEY = "sk-ant-test"

class DefaultClaudeEngineCancellationTest {
    private val request =
        ClaudeMessageRequest(
            messages = listOf(ClaudeMessage(Role.User, listOf(ContentBlock.Text(USER_MESSAGE)))),
        )

    @Test
    fun `cancelling mid stream emits no terminal event`() =
        runTest {
            val engine = testChatEngine { respondSse(SseFixtures.HAPPY_PATH) }

            val events = engine.stream(request).take(1).toList()

            assertEquals(1, events.size)
            assertIs<ClaudeStreamEvent.Delta>(events.single())
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
        val engine =
            createClaudeEngine(createClaudeHttpClient(), FakeApiKeyRepository(initialKey = API_KEY))

        assertIs<ClaudeEngine>(engine)
    }
}
