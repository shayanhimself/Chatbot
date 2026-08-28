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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val KEY_CHAT_ID = "chatId"
private const val KEY_PENDING_MODEL = "pendingModel"

/** The state read from the repository, for one chat or for none. */
private data class ChatDetailState(
    val chatId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel? = null,
    val items: List<ChatDetailItem> = emptyList(),
    val isStreaming: Boolean = false,
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

        // The model the new created chat gets, for a chat with no row of its own to hold one yet.
        private val pendingModelName =
            savedStateHandle.getStateFlow(KEY_PENDING_MODEL, ClaudeModel.Default.name)

        private val isDeleted = MutableStateFlow(false)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ChatDetailUiState> =
            combine(
                chatId.flatMapLatest(::chatDetailState),
                pendingModelName,
                isDeleted,
            ) { repo, pendingName, isDeleted ->
                ChatDetailUiState(
                    chatId = repo.chatId,
                    title = repo.title,
                    // new chat doesn't have a stored entity and model yet
                    model = repo.model ?: ClaudeModel.valueOf(pendingName),
                    items = repo.items,
                    isStreaming = repo.isStreaming,
                    isDeleted = isDeleted,
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
            return combine(
                repository.getChatFlow(id),
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
                val created =
                    repository.send(
                        chatId = id,
                        text = text,
                        model = ClaudeModel.valueOf(pendingModelName.value),
                    )
                if (id == null) rememberChat(created)
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
            savedStateHandle[KEY_PENDING_MODEL] = model.name
            val id = chatId.value ?: return
            viewModelScope.launch { repository.setModel(id, model) }
        }

        /**
         * `isDeleted` is set only once the repository has finished, which is what keeps
         * [viewModelScope] alive through the delete: the screen is popped in response to it, and a
         * pop cancels the scope.
         */
        fun onDeleteConfirmed() {
            val id = chatId.value ?: return
            viewModelScope.launch {
                repository.delete(id)
                isDeleted.value = true
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
    }
