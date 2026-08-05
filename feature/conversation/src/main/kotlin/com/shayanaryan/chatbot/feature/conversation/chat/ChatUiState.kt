package com.shayanaryan.chatbot.feature.conversation.chat

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * One item in the message list. Persisted history and the turn in flight are folded into a single
 * list so the `LazyColumn` reads one source and no composable reconciles two.
 */
sealed interface ChatItem {
    /** A message Room has stored, whatever status it ended with. */
    data class Persisted(
        val message: Message,
    ) : ChatItem

    /** A turn that has started but produced no token yet. */
    data object Thinking : ChatItem

    /** @property text the reply so far, cumulative rather than the latest delta. */
    data class Streaming(
        val text: String,
    ) : ChatItem

    /** The last turn failed. Renders after the failed message rather than instead of it. */
    data class Error(
        val error: ChatError,
    ) : ChatItem
}

/**
 * @property conversationId null until the first send creates a conversation. Also what hides the
 *   overflow menu: a chat with no first message has nothing to delete.
 * @property title null for a chat with no first message yet, which the screen renders as the
 *   new-chat copy.
 * @property model the conversation's own model once it exists, and the model the first send will
 *   create it with before that.
 * @property deleted true once a confirmed delete has finished, which is the navigation trigger
 *   `:app` reads to pop or to reset the detail pane.
 */
data class ChatUiState(
    val conversationId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel = ClaudeModel.Default,
    val items: List<ChatItem> = emptyList(),
    val isStreaming: Boolean = false,
    val deleteDialogVisible: Boolean = false,
    val deleted: Boolean = false,
) {
    /**
     * The item the message list follows while tokens arrive, null when there is nothing to render.
     * Derived here rather than at render time, so the screen reads the list's shape instead of
     * recomputing it.
     */
    val tailIndex: Int? = items.indices.lastOrNull()
}
