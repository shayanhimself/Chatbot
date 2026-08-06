package com.shayanaryan.chatbot.feature.conversation.conversationlist

/**
 * @property isLoading true until Room's first emission.
 * @property conversations every conversation, most recently updated first.
 */
data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationListItemUiState> = emptyList(),
)

/**
 * One item. A UI model rather than the domain `Conversation` because the timestamp is already
 * resolved here, against the clock the ViewModel was given.
 *
 * @property id the conversation a tap on this item opens.
 * @property title the conversation's first message, truncated by the repository.
 * @property snippet the last complete message's text, the item's second line.
 * @property relativeTime how long ago the conversation was last written to, already reduced to a
 *   unit and a count so the item only has to resolve a string.
 */
data class ConversationListItemUiState(
    val id: Long,
    val title: String,
    val snippet: String?,
    val relativeTime: RelativeTime,
)
