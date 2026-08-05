package com.shayanaryan.chatbot.feature.conversation.chat

import androidx.lifecycle.SavedStateHandle
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.FakeConversationRepository
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

private const val UNKNOWN_CONVERSATION_ID = 404L
private const val RETRY_AFTER_SECONDS = 30

private const val USER_MESSAGE = "hello"
private const val CONVERSATION_TITLE = "plan a weekend"
private const val PARTIAL_TEXT = "hel"
private const val CANCELLED_TEXT = "half a th"

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // A non-zero step makes successive writes distinguishable, which is what ordering needs.
    private val clock = FakeClock(autoAdvanceBy = 1.milliseconds)
    private val repository = FakeConversationRepository(clock)

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        initialConversationId: Long? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = ChatViewModel(initialConversationId, savedStateHandle, repository)

    private fun TestScope.collecting(viewModel: ChatViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `a new chat has no id, no title and no items`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.conversationId)
            assertNull(viewModel.uiState.value.title)
            assertEquals(emptyList<ChatItem>(), viewModel.uiState.value.items)
        }

    @Test
    fun `the first send creates a conversation and adopts its id`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(CONVERSATION_TITLE)
            advanceUntilIdle()

            assertEquals(1L, viewModel.uiState.value.conversationId)
            assertEquals(CONVERSATION_TITLE, viewModel.uiState.value.title)
        }

    @Test
    fun `the adopted id survives process death through the saved state handle`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(savedStateHandle = handle)
            collecting(first)
            first.onSend(USER_MESSAGE)
            advanceUntilIdle()

            // The restored back stack still says ChatKey(null); only the handle remembers.
            val restored = viewModel(initialConversationId = null, savedStateHandle = handle)
            collecting(restored)
            advanceUntilIdle()

            assertEquals(1L, restored.uiState.value.conversationId)
        }

    @Test
    fun `thinking, then streaming, then the stored reply`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()
            assertEquals(
                ChatItem.Thinking,
                viewModel.uiState.value.items
                    .last(),
            )

            repository.emitDelta(1L, PARTIAL_TEXT)
            advanceUntilIdle()
            assertEquals(
                ChatItem.Streaming(PARTIAL_TEXT),
                viewModel.uiState.value.items
                    .last(),
            )

            repository.completeTurn(1L)
            advanceUntilIdle()
            assertTrue(
                viewModel.uiState.value.items
                    .last() is ChatItem.Persisted,
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
            repository.failTurn(1L, ChatError.RateLimited(retryAfterSeconds = RETRY_AFTER_SECONDS))
            advanceUntilIdle()

            assertEquals(
                ChatItem.Error(ChatError.RateLimited(RETRY_AFTER_SECONDS)),
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
            repository.failTurn(1L, ChatError.Network)
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(
                ChatItem.Thinking,
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
            assertTrue(last is ChatItem.Persisted && last.message.content.isNotEmpty())
            assertEquals(false, viewModel.uiState.value.isStreaming)
        }

    @Test
    fun `picking a model before the first send creates the conversation with it`() =
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
    fun `picking a model on an existing conversation persists it`() =
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
                    .getConversationsFlow()
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
    fun `confirming delete removes the conversation and reports it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend(USER_MESSAGE)
            advanceUntilIdle()

            viewModel.onDeleteRequested()
            viewModel.onDeleteConfirmed()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.deleted)
            assertEquals(emptyList<Conversation>(), repository.getConversationsFlow().first())
        }

    /**
     * A reminder notification for a conversation deleted between scheduling and firing is this
     * case in production: the id arrives from outside the app with no guarantee the row still
     * exists.
     */
    @Test
    fun `an id that names no conversation falls back to a new chat`() =
        runTest(dispatcher) {
            val viewModel = viewModel(initialConversationId = UNKNOWN_CONVERSATION_ID)
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.conversationId)
            assertNull(viewModel.uiState.value.title)
        }
}
