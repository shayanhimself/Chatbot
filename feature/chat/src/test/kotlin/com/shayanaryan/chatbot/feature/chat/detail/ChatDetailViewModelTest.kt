package com.shayanaryan.chatbot.feature.chat.detail

import androidx.lifecycle.SavedStateHandle
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.Chat
import com.shayanaryan.chatbot.shared.chat.FakeChatRepository
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private const val UNKNOWN_CHAT_ID = 404L
private const val RETRY_AFTER_SECONDS = 30

private const val USER_MESSAGE = "hello"
private const val CHAT_TITLE = "plan a weekend"
private const val PARTIAL_TEXT = "hel"
private const val CANCELLED_TEXT = "half a th"

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // A non-zero step makes successive writes distinguishable, which is what ordering needs.
    private val clock = FakeClock(autoAdvanceBy = 1.milliseconds)
    private val repository = FakeChatRepository(clock)

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        initialChatId: Long? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = ChatDetailViewModel(initialChatId, savedStateHandle, repository)

    private fun TestScope.collecting(viewModel: ChatDetailViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `a new chat has no id, no title and no items`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.chatId)
            assertNull(viewModel.uiState.value.title)
            assertEquals(emptyList<ChatDetailItem>(), viewModel.uiState.value.items)
        }

    @Test
    fun `the first send creates a chat and adopts its id`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(CHAT_TITLE)
            advanceUntilIdle()

            assertEquals(1L, viewModel.uiState.value.chatId)
            assertEquals(CHAT_TITLE, viewModel.uiState.value.title)
        }

    @Test
    fun `the adopted id survives process death through the saved state handle`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(savedStateHandle = handle)
            collecting(first)
            first.onSend(USER_MESSAGE)
            advanceUntilIdle()

            // The restored back stack still says ChatDetailKey(null); only the handle remembers.
            val restored = viewModel(initialChatId = null, savedStateHandle = handle)
            collecting(restored)
            advanceUntilIdle()

            assertEquals(1L, restored.uiState.value.chatId)
        }

    @Test
    fun `thinking, then streaming, then the stored reply`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()
            assertEquals(
                ChatDetailItem.Thinking,
                viewModel.uiState.value.items
                    .last(),
            )

            repository.emitDelta(1L, PARTIAL_TEXT)
            advanceUntilIdle()
            assertEquals(
                ChatDetailItem.Streaming(PARTIAL_TEXT),
                viewModel.uiState.value.items
                    .last(),
            )

            repository.completeTurn(1L)
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.items
                    .last() is ChatDetailItem.Persisted,
            )
            assertEquals(false, viewModel.uiState.value.isStreaming)
        }

    @Test
    fun `a failed turn shows the error item`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()
            repository.failTurn(1L, ApiError.RateLimited(retryAfterSeconds = RETRY_AFTER_SECONDS))
            advanceUntilIdle()

            assertEquals(
                ChatDetailItem.Error(ApiError.RateLimited(RETRY_AFTER_SECONDS)),
                viewModel.uiState.value.items
                    .last(),
            )
        }

    @Test
    fun `retry clears the error and runs the turn again`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()
            repository.failTurn(1L, ApiError.Network)
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(
                ChatDetailItem.Thinking,
                viewModel.uiState.value.items
                    .last(),
            )
        }

    @Test
    fun `cancel keeps the partial text as an ordinary message`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()
            repository.emitDelta(1L, CANCELLED_TEXT)

            viewModel.onCancel()
            advanceUntilIdle()

            val last =
                viewModel.uiState.value.items
                    .last()
            assertTrue(last is ChatDetailItem.Persisted && last.message.content.isNotEmpty())
            assertEquals(false, viewModel.uiState.value.isStreaming)
        }

    @Test
    fun `picking a model before the first send creates the chat with it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onModelSelected(ClaudeModel.Haiku)
            advanceUntilIdle()
            assertEquals(ClaudeModel.Haiku, viewModel.uiState.value.model)

            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()

            assertEquals(ClaudeModel.Haiku, viewModel.uiState.value.model)
        }

    @Test
    fun `picking a model on an existing chat persists it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()

            viewModel.onModelSelected(ClaudeModel.Opus)
            advanceUntilIdle()

            assertEquals(
                ClaudeModel.Opus,
                repository
                    .getChatsFlow()
                    .first()
                    .single()
                    .model,
            )
        }

    @Test
    fun `the delete dialog opens and closes`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()

            // Every read of uiState needs an advance first: localState reaches it through
            // combine(…).stateIn(viewModelScope), and viewModelScope runs on the
            // StandardTestDispatcher, which executes nothing until it is advanced.
            viewModel.onDeleteRequested()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.deleteDialogVisible)

            viewModel.onDeleteDismissed()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.deleteDialogVisible)
        }

    @Test
    fun `confirming delete removes the chat and reports it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()

            viewModel.onDeleteRequested()
            viewModel.onDeleteConfirmed()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.deleted)
            assertEquals(emptyList<Chat>(), repository.getChatsFlow().first())
        }

    /**
     * A reminder notification for a chat deleted between scheduling and firing is this
     * case in production: the id arrives from outside the app with no guarantee the row still
     * exists.
     */
    @Test
    fun `an id that names no chat falls back to a new chat`() =
        runTest(dispatcher) {
            val viewModel = viewModel(initialChatId = UNKNOWN_CHAT_ID)
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.chatId)
            assertNull(viewModel.uiState.value.title)
        }
}
