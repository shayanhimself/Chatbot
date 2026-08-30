package com.shayanaryan.chatbot.feature.chat.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.detail.component.ChatDetailBottomBar
import com.shayanaryan.chatbot.feature.chat.detail.component.ChatDetailTopBar
import com.shayanaryan.chatbot.feature.chat.detail.component.DeleteChatDialog
import com.shayanaryan.chatbot.feature.chat.detail.component.ErrorItem
import com.shayanaryan.chatbot.feature.chat.detail.component.MessageBubble
import com.shayanaryan.chatbot.feature.chat.detail.component.NewChatEmptyState
import com.shayanaryan.chatbot.feature.chat.detail.component.TailFollower
import com.shayanaryan.chatbot.feature.chat.detail.component.ThinkingIndicator
import com.shayanaryan.chatbot.feature.chat.detail.component.rememberTailFollower
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import com.shayanaryan.chatbot.shared.textContent

/**
 * One chat / chat.
 *
 * @param onBack null on a wide window, where the chat sits beside the list and there is nothing to
 *   go back to.
 * @param composerState hoisted so a test can drive it; the default is saveable, so composed text
 *   survives rotation without ever entering `UiState`.
 */
@Composable
fun ChatDetailScreen(
    uiState: ChatDetailUiState,
    onBack: (() -> Unit)?,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onModelSelected: (ClaudeModel) -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    composerState: TextFieldState = rememberTextFieldState(),
) {
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    val tailFollower = rememberTailFollower()
    Scaffold(
        modifier = modifier,
        topBar = {
            ChatDetailTopBar(
                title = uiState.title,
                deletable = uiState.chatId != null,
                onBack = onBack,
                onDeleteRequested = { deleteDialogVisible = true },
            )
        },
        bottomBar = {
            ChatDetailBottomBar(
                model = uiState.model,
                isStreaming = uiState.isStreaming,
                composerState = composerState,
                onModelSelected = onModelSelected,
                onSend = { text ->
                    tailFollower.follow()
                    onSend(text)
                },
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
                tailFollower = tailFollower,
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (deleteDialogVisible) {
        DeleteChatDialog(
            onConfirm = {
                deleteDialogVisible = false
                onDeleteConfirmed()
            },
            onDismiss = { deleteDialogVisible = false },
        )
    }
}

/**
 * @param tailIndex the item to follow while tokens arrive, from `UiState` rather than derived here.
 */
@Composable
private fun MessageList(
    chatItems: List<ChatDetailItem>,
    tailIndex: Int,
    tailFollower: TailFollower,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(chatItems) { tailFollower.scrollIfFollowing(tailIndex) }
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(tailFollower.nestedScrollConnection),
        state = tailFollower.listState,
        contentPadding =
            PaddingValues(
                start = Spacing.s4,
                end = Spacing.s4,
                top = Spacing.s2,
                bottom = Spacing.s3,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        items(chatItems, key = { it.key }) { item ->
            when (item) {
                is ChatDetailItem.Persisted -> {
                    MessageBubble(
                        text = item.message.content.textContent(),
                        role = item.message.role,
                    )
                }

                ChatDetailItem.Thinking -> {
                    ThinkingIndicator()
                }

                is ChatDetailItem.Streaming -> {
                    MessageBubble(
                        text = item.text,
                        role = Role.Assistant,
                        streaming = true,
                    )
                }

                is ChatDetailItem.Error -> {
                    ErrorItem(error = item.error, onRetry = onRetry)
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChatDetailNewPreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.newChat) }
}

@Preview
@Composable
private fun ChatDetailIdlePreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.openChat) }
}

@Preview
@Composable
private fun ChatDetailThinkingPreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.thinking) }
}

@Preview
@Composable
private fun ChatDetailStreamingPreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.streaming) }
}

@Preview
@Composable
private fun ChatDetailRateLimitedPreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.rateLimited) }
}

@Preview
@Composable
private fun ChatDetailNetworkFailurePreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.network) }
}

@Preview
@Composable
private fun ChatDetailTwoPanePreview() {
    ChatbotTheme(darkTheme = true) {
        ChatDetailScreenStubbed(ChatDetailPreviewData.openChat, onBack = null)
    }
}
