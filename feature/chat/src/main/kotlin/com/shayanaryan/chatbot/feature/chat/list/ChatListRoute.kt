package com.shayanaryan.chatbot.feature.chat.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the chat list: resolves the ViewModel and hands its state down.
 */
@Composable
fun ChatListRoute(
    selectedChatId: Long?,
    onChatClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChatListScreen(
        uiState = uiState,
        selectedChatId = selectedChatId,
        onChatClick = onChatClick,
        onNewChat = onNewChat,
        modifier = modifier,
    )
}
