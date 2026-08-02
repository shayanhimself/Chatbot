package com.shayanaryan.chatbot.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * How long a `stateIn` pipeline keeps collecting after its last subscriber leaves. Long enough to
 * survive a configuration change, short enough that a backgrounded screen stops reading Room.
 */
internal const val SUBSCRIPTION_TIMEOUT_MILLIS: Long = 5_000L

/**
 * The conversation list. Read-only: every mutation belongs to the chat screen, so this holds no
 * events at all.
 */
@HiltViewModel
class ConversationListViewModel
    @Inject
    constructor(
        repository: ConversationRepository,
        clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<ConversationListUiState> =
            repository
                .getConversationsFlow()
                .map { conversations ->
                    val now = clock.now()
                    ConversationListUiState(
                        isLoading = false,
                        conversations = conversations.map { it.toUiState(now) },
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                    initialValue = ConversationListUiState(),
                )
    }

private fun Conversation.toUiState(now: Instant) =
    ConversationListItemUiState(
        id = id,
        title = title,
        snippet = snippet,
        // Timestamps are resolved once per emission rather than on a ticker, so a row reading "2h" does
        // not become "3h" while the screen stays open. Room re-emits on every message write, which in
        // practice refreshes them often enough.
        relativeTime = updatedAt.relativeTo(now),
    )
