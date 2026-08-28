package com.shayanaryan.chatbot.feature.chat.detail

import androidx.compose.runtime.Composable
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.time.Instant

/**
 * The width every chat item renders at in a preview. Each of them sizes itself as a fraction of
 * the width it is given, so without one they wrap to their content and read nothing like the
 * screen.
 */
internal const val CHAT_PREVIEW_WIDTH_DP = 360

/**
 * The states as one fixture the colocated previews and the screenshot goldens both read.
 */
internal object ChatDetailPreviewData {
    private fun message(
        id: Long,
        role: Role,
        text: String,
    ) = ChatDetailItem.Persisted(
        Message(
            id = id,
            chatId = 1L,
            role = role,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = Instant.fromEpochMilliseconds(id),
        ),
    )

    val newChat = ChatDetailUiState()

    val openChat =
        ChatDetailUiState(
            chatId = 1L,
            title = "Weekend trip to Portland",
            model = ClaudeModel.Sonnet,
            items =
                listOf(
                    message(1L, Role.User, "help me plan a weekend in portland"),
                    message(
                        2L,
                        Role.Assistant,
                        "Powell's Books + a food-cart lunch Saturday, then Forest Park in the morning.",
                    ),
                ),
        )

    val thinking =
        openChat.copy(
            items = listOf(openChat.items.first()) + ChatDetailItem.Thinking,
            isStreaming = true,
        )

    val streaming =
        openChat.copy(
            items =
                listOf(openChat.items.first()) +
                    ChatDetailItem.Streaming(
                        "Love it. Two nights? I'd do Powell's Books + a food-cart lunch " +
                            "Saturday, then Forest Park in the",
                    ),
            isStreaming = true,
        )

    val rateLimited =
        openChat.copy(
            items =
                openChat.items +
                    ChatDetailItem.Error(
                        ApiError.RateLimited(retryAfterSeconds = null),
                    ),
        )

    val network = openChat.copy(items = openChat.items + ChatDetailItem.Error(ApiError.Network))
}

/**
 * [ChatDetailScreen] with every event lambda stubbed out, so a preview or a golden only has to name the
 * state it renders.
 *
 * @param onBack null renders the detail pane, which is the same screen without its back arrow.
 */
@Composable
internal fun ChatDetailScreenStubbed(
    uiState: ChatDetailUiState,
    onBack: (() -> Unit)? = {},
) {
    ChatDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onSend = {},
        onCancel = {},
        onRetry = {},
        onModelSelected = {},
        onDeleteConfirmed = {},
    )
}
