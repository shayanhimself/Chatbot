package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the conversation list: resolves the ViewModel and hands its state down.
 */
@Composable
fun ConversationListRoute(
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationListScreen(
        uiState = uiState,
        selectedConversationId = selectedConversationId,
        onConversationClick = onConversationClick,
        onNewChat = onNewChat,
        modifier = modifier,
    )
}
