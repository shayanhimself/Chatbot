package com.shayanaryan.chatbot.feature.chat.detail

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * Everything the chat detail screen renders, for an existing chat or one not yet created.
 *
 * @property chatId null until the first send creates a chat. Also what hides the
 *   overflow menu: a chat with no first message has nothing to delete.
 * @property title null for a chat with no first message yet, which the screen renders as the
 *   new-chat copy.
 * @property model the chat's own model once it exists, and the model the first send will
 *   create it with before that.
 * @property deleted true once a confirmed delete has finished, which is the navigation trigger
 *   `:app` reads to pop or to reset the detail pane.
 */
data class ChatDetailUiState(
    val chatId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel = ClaudeModel.Default,
    val items: List<ChatDetailItem> = emptyList(),
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

/**
 * One item in the message list. Persisted history and the turn in flight are folded into a single
 * list so the `LazyColumn` reads one source and no composable reconciles two.
 */
sealed interface ChatDetailItem {
    /**
     * What the message list identifies this item by.
     */
    val key: String

    /** A message Room has stored, whatever status it ended with. */
    data class Persisted(
        val message: Message,
    ) : ChatDetailItem {
        override val key: String = "$PERSISTED_KEY_PREFIX${message.id}"
    }

    /** A turn that has started but produced no token yet. */
    data object Thinking : ChatDetailItem {
        override val key: String = THINKING_KEY
    }

    /** @property text the reply so far, cumulative rather than the latest delta. */
    data class Streaming(
        val text: String,
    ) : ChatDetailItem {
        override val key: String = STREAMING_KEY
    }

    /** The last turn failed. Renders after the failed message rather than instead of it. */
    data class Error(
        val error: ApiError,
    ) : ChatDetailItem {
        override val key: String = ERROR_KEY
    }
}

private const val PERSISTED_KEY_PREFIX = "persisted-"
private const val THINKING_KEY = "thinking"
private const val STREAMING_KEY = "streaming"
private const val ERROR_KEY = "error"
