package com.shayanaryan.chatbot.feature.chat.list

import androidx.compose.runtime.Composable
import com.shayanaryan.chatbot.feature.chat.R

/**
 * The states as one fixture the colocated previews and the screenshot goldens both read.
 */
internal object ChatListPreviewData {
    private val chats =
        listOf(
            ChatListItemUiState(
                1L,
                "Weekend trip to Portland",
                "Booked. I'll remind you to check in Friday.",
                RelativeTime(R.string.chat_time_hours, 2),
            ),
            ChatListItemUiState(
                2L,
                "Miso glaze recipe",
                "Try broiling the last 2 minutes.",
                RelativeTime(R.string.chat_time_days, 1),
            ),
            ChatListItemUiState(
                3L,
                "Standup notes",
                "Got it. Remembered you're on the payments team.",
                RelativeTime(R.string.chat_time_days, 3),
            ),
            ChatListItemUiState(
                4L,
                "Coroutine leak in onCleared when the ViewModelScope isn't cancelled",
                "No need to cancel the viewModelScope, it's automatic.",
                RelativeTime(R.string.chat_time_days, 5),
            ),
            ChatListItemUiState(
                5L,
                "Gift ideas for dad",
                "A cast-iron skillet + a good chef's apron.",
                RelativeTime(R.string.chat_time_weeks, 1),
            ),
        )

    val populated = ChatListUiState(isLoading = false, chats = chats)

    val empty = ChatListUiState(isLoading = false, chats = emptyList())

    val loading = ChatListUiState(isLoading = true)
}

/**
 * [ChatListScreen] with every event lambda stubbed out, so a preview or a golden only has to name
 * the state it renders.
 */
@Composable
internal fun ChatListScreenStubbed(uiState: ChatListUiState) {
    ChatListScreen(
        uiState = uiState,
        selectedChatId = null,
        onChatClick = {},
        onNewChat = {},
    )
}
