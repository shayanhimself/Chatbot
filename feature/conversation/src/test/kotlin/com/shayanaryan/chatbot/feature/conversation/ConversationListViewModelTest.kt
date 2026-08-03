package com.shayanaryan.chatbot.feature.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.conversation.FakeConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock(instant = Instant.fromEpochMilliseconds(1_000_000_000L))

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    /**
     * `stateIn(WhileSubscribed)` produces nothing until something collects, so every test needs a
     * collector before it reads `value`.
     */
    private fun TestScope.collecting(viewModel: ConversationListViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `starts loading with no items`() =
        runTest(dispatcher) {
            val viewModel = ConversationListViewModel(FakeConversationRepository(clock), clock)

            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(
                emptyList(),
                viewModel.uiState.value.conversations,
            )
        }

    @Test
    fun `stops loading on the first emission`() =
        runTest(dispatcher) {
            val viewModel = ConversationListViewModel(FakeConversationRepository(clock), clock)
            collecting(viewModel)
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoading)
        }

    @Test
    fun `carries title, snippet and id from the repository`() =
        runTest(dispatcher) {
            val repository = FakeConversationRepository(clock)
            val id = repository.send(null, "plan a weekend")
            repository.emitDelta(id, "Powell's Books first.")
            repository.completeTurn(id)
            val viewModel = ConversationListViewModel(repository, clock)
            collecting(viewModel)
            advanceUntilIdle()

            val item =
                viewModel.uiState.value.conversations
                    .single()

            assertEquals(id, item.id)
            assertEquals("plan a weekend", item.title)
            assertEquals("Powell's Books first.", item.snippet)
        }

    @Test
    fun `formats the timestamp against the injected clock`() =
        runTest(dispatcher) {
            val repository = FakeConversationRepository(clock)
            repository.send(null, "hello")
            clock.advanceBy(2.hours)
            val viewModel = ConversationListViewModel(repository, clock)
            collecting(viewModel)
            advanceUntilIdle()

            assertEquals(
                RelativeTime(R.string.conversation_time_hours, 2),
                viewModel.uiState.value.conversations
                    .single()
                    .relativeTime,
            )
        }
}
