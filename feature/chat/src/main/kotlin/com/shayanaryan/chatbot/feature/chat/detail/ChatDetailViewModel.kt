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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val KEY_CHAT_ID = "chatId"
private const val KEY_PENDING_MODEL = "pendingModel"

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
        private val pendingModel =
            savedStateHandle.getStateFlow(KEY_PENDING_MODEL, ClaudeModel.Default)

        private val isDeleted = MutableStateFlow(false)

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ChatDetailUiState> =
            combine(
                chatId.flatMapLatest(repository::getChatSnapshotFlow),
                pendingModel,
                isDeleted,
            ) { snapshot, pendingModel, isDeleted ->
                if (snapshot == null) {
                    // No row means no stored model, so the pending pick is the one to show.
                    ChatDetailUiState(
                        model = pendingModel,
                        isDeleted = isDeleted,
                    )
                } else {
                    ChatDetailUiState(
                        chatId = snapshot.chat.id,
                        title = snapshot.chat.title,
                        model = snapshot.chat.model,
                        items = snapshot.messages.toChatDetailItems(snapshot.turn),
                        isStreaming = snapshot.turn is TurnState.Streaming,
                        isDeleted = isDeleted,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = ChatDetailUiState(),
            )

        fun onSend(text: String) {
            if (text.isBlank() || uiState.value.isStreaming) return
            val id = chatId.value
            viewModelScope.launch {
                ignoringRejection {
                    val created =
                        repository.send(
                            chatId = id,
                            text = text,
                            model = pendingModel.value,
                        )
                    if (id == null) rememberChat(created)
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
            savedStateHandle[KEY_PENDING_MODEL] = model
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

/**
 * Runs [block], dropping the rejection the repository throws for a chat that no longer exists or
 * one already running a turn.
 *
 * Cancellation is how the scope shuts down and arrives as an `IllegalStateException`, so it is
 * rethrown before the rejection catch can swallow it.
 */
private inline fun ignoringRejection(block: () -> Unit) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: RuntimeException) {
    }
}
