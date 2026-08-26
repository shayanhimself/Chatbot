package com.shayanaryan.chatbot.feature.chat.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import com.shayanaryan.chatbot.shared.chat.TurnState
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_CHAT_ID = "chatId"

/** The state read from the repository, for one chat or for none. */
private data class ChatDetailState(
    val chatId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel? = null,
    val items: List<ChatDetailItem> = emptyList(),
    val isStreaming: Boolean = false,
)

/** The state the ViewModel writes itself. */
private data class LocalState(
    val pendingModel: ClaudeModel = ClaudeModel.Default,
    val deleteDialogVisible: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * @param initialChatId the id the navigation key carried, null for a new chat.
 */
@HiltViewModel(assistedFactory = ChatDetailViewModel.Factory::class)
class ChatDetailViewModel
    @AssistedInject
    constructor(
        @Assisted private val initialChatId: Long?,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(initialChatId: Long?): ChatDetailViewModel
        }

        // Owned here rather than by the navigation key: a new chat keeps `ChatDetailKey(null)` for its
        // whole life, because replacing the top of the back stack would recreate the entry and
        // therefore this ViewModel, mid-stream, resetting scroll. The saved value wins over the
        // key's, since only it survives process death.
        private val chatId =
            MutableStateFlow(
                savedStateHandle.get<Long>(KEY_CHAT_ID) ?: initialChatId,
            )

        private val localState = MutableStateFlow(LocalState())

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ChatDetailUiState> =
            combine(
                chatId.flatMapLatest(::chatDetailState),
                localState,
            ) { repo, local ->
                ChatDetailUiState(
                    chatId = repo.chatId,
                    title = repo.title,
                    model = repo.model ?: local.pendingModel,
                    items = repo.items,
                    isStreaming = repo.isStreaming,
                    deleteDialogVisible = local.deleteDialogVisible,
                    deleted = local.deleted,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = ChatDetailUiState(),
            )

        private fun chatDetailState(id: Long?): Flow<ChatDetailState> {
            // A new chat subscribes to nothing, so no flow is opened on a chat that
            // does not exist.
            if (id == null) return flowOf(ChatDetailState())
            val chats =
                repository.getChatFlow(id).onEach { chat ->
                    // The chat was deleted under the screen, so fall back to a new chat.
                    if (chat == null) forgetChat()
                }
            return combine(
                chats,
                repository.getMessagesFlow(id),
                repository.getTurnFlow(id),
            ) { chat, messages, turn ->
                if (chat == null) {
                    ChatDetailState()
                } else {
                    ChatDetailState(
                        chatId = id,
                        title = chat.title,
                        model = chat.model,
                        items = messages.toChatDetailItems(turn),
                        isStreaming = turn is TurnState.Streaming,
                    )
                }
            }
        }

        fun onSend(text: String) {
            if (text.isBlank() || uiState.value.isStreaming) return
            val id = chatId.value
            viewModelScope.launch {
                try {
                    val created = repository.send(id, text, localState.value.pendingModel)
                    if (id == null) rememberChat(created)
                } catch (cancellation: CancellationException) {
                    // Cancellation is how the scope shuts down, and it arrives as an
                    // IllegalStateException, which the catch below would otherwise swallow.
                    throw cancellation
                } catch (_: RuntimeException) {
                    // `send` rejects a chat that no longer exists and a chat already running a
                    // turn
                }
            }
        }

        fun onCancel() {
            val id = chatId.value ?: return
            viewModelScope.launch { repository.cancel(id) }
        }

        fun onRetry() {
            val id = chatId.value ?: return
            viewModelScope.launch { repository.retry(id) }
        }

        fun onModelSelected(model: ClaudeModel) {
            localState.update { it.copy(pendingModel = model) }
            val id = chatId.value ?: return
            viewModelScope.launch { repository.setModel(id, model) }
        }

        fun onDeleteRequested() {
            localState.update { it.copy(deleteDialogVisible = true) }
        }

        fun onDeleteDismissed() {
            localState.update { it.copy(deleteDialogVisible = false) }
        }

        /**
         * `deleted` is set only once the repository has finished, which is what keeps
         * [viewModelScope] alive through the delete: the screen is popped in response to it, and a
         * pop cancels the scope.
         */
        fun onDeleteConfirmed() {
            val id = chatId.value ?: return
            localState.update { it.copy(deleteDialogVisible = false) }
            viewModelScope.launch {
                repository.delete(id)
                localState.update { it.copy(deleted = true) }
            }
        }

        /**
         * Writes the created id where it survives process death: the restored back stack still
         * says `ChatDetailKey(null)`, and without this the user would come back to an empty new chat
         * instead of the chat they were in.
         */
        private fun rememberChat(id: Long) {
            savedStateHandle[KEY_CHAT_ID] = id
            chatId.value = id
        }

        private fun forgetChat() {
            savedStateHandle[KEY_CHAT_ID] = null
            chatId.value = null
        }
    }
