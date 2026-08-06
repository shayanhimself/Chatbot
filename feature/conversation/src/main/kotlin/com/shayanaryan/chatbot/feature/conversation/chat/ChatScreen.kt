package com.shayanaryan.chatbot.feature.conversation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.chat.component.ChatBottomBar
import com.shayanaryan.chatbot.feature.conversation.chat.component.ChatTopBar
import com.shayanaryan.chatbot.feature.conversation.chat.component.DeleteChatDialog
import com.shayanaryan.chatbot.feature.conversation.chat.component.ErrorItem
import com.shayanaryan.chatbot.feature.conversation.chat.component.MessageBubble
import com.shayanaryan.chatbot.feature.conversation.chat.component.NewChatEmptyState
import com.shayanaryan.chatbot.feature.conversation.chat.component.ThinkingIndicator
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.textContent
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * One chat / conversation.
 *
 * @param onBack null on a wide window, where the chat sits beside the list and there is nothing to
 *   go back to.
 * @param composerState hoisted so a test can drive it; the default is saveable, so composed text
 *   survives rotation without ever entering `UiState`.
 */
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onBack: (() -> Unit)?,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onModelSelected: (ClaudeModel) -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    composerState: TextFieldState = rememberTextFieldState(),
) {
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            ChatTopBar(
                title = uiState.title,
                deletable = uiState.conversationId != null,
                onBack = onBack,
                onDeleteRequested = onDeleteRequested,
            )
        },
        bottomBar = {
            ChatBottomBar(
                model = uiState.model,
                isStreaming = uiState.isStreaming,
                composerState = composerState,
                onModelSelected = onModelSelected,
                onSend = onSend,
                onCancel = onCancel,
            )
        },
    ) { padding ->
        if (uiState.items.isEmpty()) {
            NewChatEmptyState(Modifier.padding(padding))
        } else {
            MessageList(
                chatItems = uiState.items,
                tailIndex = checkNotNull(uiState.tailIndex),
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (uiState.deleteDialogVisible) {
        DeleteChatDialog(onConfirm = onDeleteConfirmed, onDismiss = onDeleteDismissed)
    }
}

/**
 * @param tailIndex the item to follow while tokens arrive, from `UiState` rather than derived here.
 */
@Composable
private fun MessageList(
    chatItems: List<ChatItem>,
    tailIndex: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // `canScrollForward` is false when there is nothing left below the viewport, which is to say
    // the list is already scrolled to its end. So the guard reads as "only follow the tail when the
    // user is sitting at the bottom", and following stops the moment they scroll up.
    LaunchedEffect(chatItems) {
        if (!listState.canScrollForward) {
            listState.scrollToItem(tailIndex)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = Spacing.s4,
                end = Spacing.s4,
                top = Spacing.s2,
                bottom = Spacing.s3,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        items(chatItems) { item ->
            when (item) {
                is ChatItem.Persisted -> {
                    MessageBubble(
                        text = item.message.content.textContent(),
                        role = item.message.role,
                    )
                }

                ChatItem.Thinking -> {
                    ThinkingIndicator()
                }

                is ChatItem.Streaming -> {
                    MessageBubble(
                        text = item.text,
                        role = Role.Assistant,
                        streaming = true,
                    )
                }

                is ChatItem.Error -> {
                    ErrorItem(error = item.error, onRetry = onRetry)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNewPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.newChat) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatIdlePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatThinkingPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.thinking) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatStreamingPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.streaming) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatRateLimitedPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.rateLimited) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNetworkFailurePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.network) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatDeleteDialogPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.deleting) }
}

@Preview(showBackground = true, heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatTwoPanePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat, onBack = null) }
}
