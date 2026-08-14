package com.shayanaryan.chatbot.feature.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.core.ui.viewmodel.SUBSCRIPTION_TIMEOUT_MILLIS
import com.shayanaryan.chatbot.shared.chat.Chat
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The chat list. Read-only: every mutation belongs to the chat screen, so this holds no
 * events at all.
 */
@HiltViewModel
class ChatListViewModel
    @Inject
    constructor(
        repository: ChatRepository,
        clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<ChatListUiState> =
            repository
                .getChatsFlow()
                .map { chats ->
                    val now = clock.now()
                    ChatListUiState(
                        isLoading = false,
                        chats = chats.map { it.toUiState(now) },
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                    initialValue = ChatListUiState(),
                )
    }

private fun Chat.toUiState(now: Instant) =
    ChatListItemUiState(
        id = id,
        title = title,
        snippet = snippet,
        // Timestamps are resolved once per emission rather than on a ticker, so an item reading
        // "2h" does not become "3h" while the screen stays open. Room re-emits on every message
        // write, which in practice refreshes them often enough.
        relativeTime = updatedAt.relativeTo(now),
    )
