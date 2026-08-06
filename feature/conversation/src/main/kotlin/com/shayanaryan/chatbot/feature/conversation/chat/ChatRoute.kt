package com.shayanaryan.chatbot.feature.conversation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the chat screen. Seeds its ViewModel with the id the navigation key carried and
 * reports the live id back up, because on a wide window the list beside it highlights the open
 * conversation and the key deliberately never learns the id.
 *
 * @param onBack null on a wide window, which hides the back arrow.
 * @param onDeleted called once a confirmed delete has finished: the caller pops on a narrow
 *   window, or returns the detail pane to a new chat on a wide one.
 */
@Composable
fun ChatRoute(
    conversationId: Long?,
    onBack: (() -> Unit)?,
    onDeleted: () -> Unit,
    onConversationIdChanged: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel =
        hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
            creationCallback = { factory -> factory.create(conversationId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.conversationId) { onConversationIdChanged(uiState.conversationId) }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onDeleted() }

    ChatScreen(
        uiState = uiState,
        onBack = onBack,
        onSend = viewModel::onSend,
        onCancel = viewModel::onCancel,
        onRetry = viewModel::onRetry,
        onModelSelected = viewModel::onModelSelected,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        modifier = modifier,
    )
}
