package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.FakeScriptedChatEngine
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationSnippetTest {
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
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine =
                FakeScriptedChatEngine(
                    events =
                        listOf(
                            ChatStreamEvent.Delta("a reply"),
                            ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)),
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

            val conversation = repository.getConversationsFlow().first().single()

            assertEquals("a reply", conversation.snippet)
        }

    @Test
    fun `an unfinished first turn is summarized by the user's own message`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")

            val conversation = repository.getConversationFlow(id).first()

            assertEquals("hello", conversation?.snippet)
        }

    @Test
    fun `the single-conversation read carries the title`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "plan a weekend")
            advanceUntilIdle()

            assertEquals("plan a weekend", repository.getConversationFlow(id).first()?.title)
        }

    @Test
    fun `the single-conversation read emits null after delete`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")
            advanceUntilIdle()
            repository.delete(id)
            advanceUntilIdle()

            assertNull(repository.getConversationFlow(id).first())
        }

    @Test
    fun `the single-conversation read emits null for an id that never existed`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())

            assertNull(repository.getConversationFlow(404L).first())
        }
}
