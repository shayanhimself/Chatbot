package com.shayanaryan.chatbot.feature.chat.list

/**
 * @property isLoading true until Room's first emission.
 * @property chats every chat, most recently updated first.
 */
data class ChatListUiState(
    val isLoading: Boolean = true,
    val chats: List<ChatListItemUiState> = emptyList(),
)

/**
 * One item. A UI model rather than the domain `Chat` because the timestamp is already
 * resolved here, against the clock the ViewModel was given.
 *
 * @property id the chat a tap on this item opens.
 * @property title the chat's first message, truncated by the repository.
 * @property snippet the last complete message's text, the item's second line.
 * @property relativeTime how long ago the chat was last written to, already reduced to a
 *   unit and a count so the item only has to resolve a string.
 */
data class ChatListItemUiState(
    val id: Long,
    val title: String,
    val snippet: String?,
    val relativeTime: RelativeTime,
)
