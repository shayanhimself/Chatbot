package com.shayanaryan.chatbot.feature.chat.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.R
import com.shayanaryan.chatbot.feature.chat.list.component.ChatListEmpty
import com.shayanaryan.chatbot.feature.chat.list.component.ChatListItem
import com.shayanaryan.chatbot.feature.chat.list.component.ChatListSkeleton

/**
 * The app's home screen: browse and resume chats, or start one.
 *
 * Stateless. This overload is what previews, screenshot tests and Compose tests drive.
 *
 * @param selectedChatId the chat open in the detail pane, highlighted in the item.
 *   Always null on a narrow window, which never shows the list beside a chat.
 */
@Composable
fun ChatListScreen(
    uiState: ChatListUiState,
    selectedChatId: Long?,
    onChatClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { ChatListTopBar() },
        floatingActionButton = { NewChatButton(onClick = onNewChat) },
    ) { padding ->
        when {
            uiState.isLoading -> {
                ChatListSkeleton(Modifier.padding(padding))
            }

            uiState.chats.isEmpty() -> {
                ChatListEmpty(Modifier.padding(padding))
            }

            else -> {
                ChatList(
                    chats = uiState.chats,
                    selectedChatId = selectedChatId,
                    onChatClick = onChatClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ChatList(
    chats: List<ChatListItemUiState>,
    selectedChatId: Long?,
    onChatClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.s2, vertical = Spacing.s1),
    ) {
        itemsIndexed(chats, key = { _, item -> item.id }) { index, item ->
            ChatListItem(
                title = item.title,
                snippet = item.snippet,
                relativeTime = item.relativeTime,
                selected = item.id == selectedChatId,
                onClick = { onChatClick(item.id) },
            )
            // Divider only between items.
            if (index != chats.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.s3),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListTopBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.chat_list_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

@Composable
private fun NewChatButton(onClick: () -> Unit) {
    DsButton(
        text = stringResource(R.string.chat_list_new_chat),
        onClick = onClick,
        variant = ButtonVariant.Filled,
        leadingGlyph = Glyphs.ADD,
    )
}

@Preview
@Composable
private fun ChatListPopulatedPreview() {
    ChatbotTheme(darkTheme = true) { ChatListScreenStubbed(ChatListPreviewData.populated) }
}

@Preview
@Composable
private fun ChatListEmptyPreview() {
    ChatbotTheme(darkTheme = true) { ChatListScreenStubbed(ChatListPreviewData.empty) }
}

@Preview
@Composable
private fun ChatListLoadingPreview() {
    ChatbotTheme(darkTheme = true) { ChatListScreenStubbed(ChatListPreviewData.loading) }
}
