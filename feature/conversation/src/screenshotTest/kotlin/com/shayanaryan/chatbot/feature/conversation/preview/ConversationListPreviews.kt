package com.shayanaryan.chatbot.feature.conversation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.preview.FormFactorPreviews
import com.shayanaryan.chatbot.feature.conversation.ConversationListScreen
import com.shayanaryan.chatbot.feature.conversation.ConversationListUiState
import com.shayanaryan.chatbot.feature.conversation.PREVIEW_CONVERSATIONS

@Composable
private fun ListScreen(uiState: ConversationListUiState) {
    ConversationListScreen(
        uiState = uiState,
        selectedConversationId = null,
        onConversationClick = {},
        onNewChat = {},
    )
}

private val populated =
    ConversationListUiState(isLoading = false, conversations = PREVIEW_CONVERSATIONS)
private val empty = ConversationListUiState(isLoading = false, conversations = emptyList())
private val loading = ConversationListUiState(isLoading = true)

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListPopulatedDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(populated) }
}

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListPopulatedLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(populated) }
}

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListEmptyDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(empty) }
}

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListEmptyLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(empty) }
}

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListLoadingDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(loading) }
}

@PreviewTest
@Preview(heightDp = 780)
@Composable
private fun ListLoadingLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(loading) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ListFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(populated) }
}
