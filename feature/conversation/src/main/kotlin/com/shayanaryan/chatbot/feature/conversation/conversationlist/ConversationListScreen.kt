package com.shayanaryan.chatbot.feature.conversation.conversationlist

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
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.conversationlist.component.ConversationListEmpty
import com.shayanaryan.chatbot.feature.conversation.conversationlist.component.ConversationListItem
import com.shayanaryan.chatbot.feature.conversation.conversationlist.component.ConversationListSkeleton

/**
 * The app's home screen: browse and resume conversations, or start one.
 *
 * Stateless. This overload is what previews, screenshot tests and Compose tests drive.
 *
 * @param selectedConversationId the conversation open in the detail pane, highlighted in the item.
 *   Always null on a narrow window, which never shows the list beside a chat.
 */
@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { ConversationListTopBar() },
        floatingActionButton = { NewChatButton(onClick = onNewChat) },
    ) { padding ->
        when {
            uiState.isLoading -> {
                ConversationListSkeleton(Modifier.padding(padding))
            }

            uiState.conversations.isEmpty() -> {
                ConversationListEmpty(Modifier.padding(padding))
            }

            else -> {
                ConversationList(
                    conversations = uiState.conversations,
                    selectedConversationId = selectedConversationId,
                    onConversationClick = onConversationClick,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<ConversationListItemUiState>,
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.s2, vertical = Spacing.s1),
    ) {
        itemsIndexed(conversations, key = { _, item -> item.id }) { index, item ->
            ConversationListItem(
                title = item.title,
                snippet = item.snippet,
                relativeTime = item.relativeTime,
                selected = item.id == selectedConversationId,
                onClick = { onConversationClick(item.id) },
            )
            // Divider only between items.
            if (index != conversations.lastIndex) {
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
private fun ConversationListTopBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.conversation_list_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

@Composable
private fun NewChatButton(onClick: () -> Unit) {
    DsButton(
        text = stringResource(R.string.conversation_list_new_chat),
        onClick = onClick,
        variant = ButtonVariant.Filled,
        leadingGlyph = Glyphs.ADD,
    )
}

internal val PREVIEW_CONVERSATIONS =
    listOf(
        ConversationListItemUiState(
            1L,
            "Weekend trip to Portland",
            "Booked — I'll remind you to check in Friday.",
            RelativeTime(R.string.conversation_time_hours, 2),
        ),
        ConversationListItemUiState(
            2L,
            "Miso glaze recipe",
            "Try broiling the last 2 minutes.",
            RelativeTime(R.string.conversation_time_days, 1),
        ),
        ConversationListItemUiState(
            3L,
            "Standup notes",
            "Got it — remembered you're on the payments team.",
            RelativeTime(R.string.conversation_time_days, 3),
        ),
        ConversationListItemUiState(
            4L,
            "Coroutine leak in onCleared when the ViewModelScope isn't cancelled",
            "Cancel the viewModelScope — it's automatic, actually.",
            RelativeTime(R.string.conversation_time_days, 5),
        ),
        ConversationListItemUiState(
            5L,
            "Gift ideas for dad",
            "A cast-iron skillet + a good chef's apron.",
            RelativeTime(R.string.conversation_time_weeks, 1),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun ConversationListPopulatedPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState =
                ConversationListUiState(
                    isLoading = false,
                    conversations = PREVIEW_CONVERSATIONS,
                ),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(isLoading = false, conversations = emptyList()),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListLoadingPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(isLoading = true),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}
