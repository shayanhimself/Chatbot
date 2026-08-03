package com.shayanaryan.chatbot.feature.conversation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import com.shayanaryan.chatbot.shared.conversation.TurnState
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_CONVERSATION_ID = "conversationId"

/** The state read from the repository, for one conversation or for none. */
private data class ChatState(
    val conversationId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel? = null,
    val items: List<ChatItem> = emptyList(),
    val isStreaming: Boolean = false,
)

/** The state the ViewModel writes itself. */
private data class LocalState(
    val pendingModel: ClaudeModel = ClaudeModel.Default,
    val deleteDialogVisible: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * @param initialConversationId the id the navigation key carried, null for a new chat.
 */
@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel
    @AssistedInject
    constructor(
        @Assisted private val initialConversationId: Long?,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ConversationRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(initialConversationId: Long?): ChatViewModel
        }

        // Owned here rather than by the navigation key: a new chat keeps `ChatKey(null)` for its
        // whole life, because replacing the top of the back stack would recreate the entry and
        // therefore this ViewModel, mid-stream, resetting scroll. The saved value wins over the
        // key's, since only it survives process death.
        private val conversationId =
            MutableStateFlow(
                savedStateHandle.get<Long>(KEY_CONVERSATION_ID) ?: initialConversationId,
            )

        private val localState = MutableStateFlow(LocalState())

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ChatUiState> =
            combine(
                conversationId.flatMapLatest(::chatState),
                localState,
            ) { repo, local ->
                ChatUiState(
                    conversationId = repo.conversationId,
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
                initialValue = ChatUiState(),
            )

        private fun chatState(id: Long?): Flow<ChatState> {
            // A new chat subscribes to nothing, so no flow is opened on a conversation that
            // does not exist.
            if (id == null) return flowOf(ChatState())
            val conversations =
                repository.getConversationFlow(id).onEach { conversation ->
                    // The conversation was deleted under the screen, so fall back to a new chat.
                    if (conversation == null) forgetConversation()
                }
            return combine(
                conversations,
                repository.getMessagesFlow(id),
                repository.getTurnFlow(id),
            ) { conversation, messages, turn ->
                if (conversation == null) {
                    ChatState()
                } else {
                    ChatState(
                        conversationId = id,
                        title = conversation.title,
                        model = conversation.model,
                        items = messages.toChatItems(turn),
                        isStreaming = turn is TurnState.Streaming,
                    )
                }
            }
        }

        fun onSend(text: String) {
            if (text.isBlank() || uiState.value.isStreaming) return
            val id = conversationId.value
            viewModelScope.launch {
                val created = repository.send(id, text, localState.value.pendingModel)
                if (id == null) rememberConversation(created)
            }
        }

        fun onCancel() {
            val id = conversationId.value ?: return
            viewModelScope.launch { repository.cancel(id) }
        }

        fun onRetry() {
            val id = conversationId.value ?: return
            viewModelScope.launch { repository.retry(id) }
        }

        fun onModelSelected(model: ClaudeModel) {
            localState.update { it.copy(pendingModel = model) }
            val id = conversationId.value ?: return
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
            val id = conversationId.value ?: return
            localState.update { it.copy(deleteDialogVisible = false) }
            viewModelScope.launch {
                repository.delete(id)
                localState.update { it.copy(deleted = true) }
            }
        }

        /**
         * Writes the created id where it survives process death: the restored back stack still
         * says `ChatKey(null)`, and without this the user would come back to an empty new chat
         * instead of the conversation they were in.
         */
        private fun rememberConversation(id: Long) {
            savedStateHandle[KEY_CONVERSATION_ID] = id
            conversationId.value = id
        }

        private fun forgetConversation() {
            savedStateHandle[KEY_CONVERSATION_ID] = null
            conversationId.value = null
        }
    }
