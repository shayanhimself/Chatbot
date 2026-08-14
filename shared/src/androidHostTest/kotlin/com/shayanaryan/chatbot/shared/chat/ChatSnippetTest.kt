package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.claude.ClaudeStreamEvent
import com.shayanaryan.chatbot.shared.claude.FakeScriptedClaudeEngine
import com.shayanaryan.chatbot.shared.claude.StopReason
import com.shayanaryan.chatbot.shared.claude.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val UNKNOWN_CHAT_ID = 404L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatSnippetTest {
    private val turnScopes = mutableListOf<CoroutineScope>()

    private fun TestScope.turnScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job()).also { turnScopes += it }

    @AfterTest
    fun cancelTurnScopes() {
        turnScopes.forEach { it.cancel() }
    }

    private fun repository(
        database: ChatbotDatabase,
        scope: CoroutineScope,
    ): ChatRepository =
        createChatRepository(
            database = database,
            engine =
                FakeScriptedClaudeEngine(
                    events =
                        listOf(
                            ClaudeStreamEvent.Delta("a reply"),
                            ClaudeStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)),
                        ),
                ),
            externalScope = scope,
        )

    @Test
    fun `the list snippet is the last complete message`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            repository.send(null, "hello")
            advanceUntilIdle()

            val chat = repository.getChatsFlow().first().single()

            assertEquals("a reply", chat.snippet)
        }

    @Test
    fun `an unfinished first turn is summarized by the user's own message`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")

            val chat = repository.getChatFlow(id).first()

            assertEquals("hello", chat?.snippet)
        }

    @Test
    fun `the single-chat read carries the title`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "plan a weekend")
            advanceUntilIdle()

            assertEquals("plan a weekend", repository.getChatFlow(id).first()?.title)
        }

    @Test
    fun `the single-chat read emits null after delete`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")
            advanceUntilIdle()
            repository.delete(id)
            advanceUntilIdle()

            assertNull(repository.getChatFlow(id).first())
        }

    @Test
    fun `the single-chat read emits null for an id that never existed`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())

            assertNull(repository.getChatFlow(UNKNOWN_CHAT_ID).first())
        }
}
