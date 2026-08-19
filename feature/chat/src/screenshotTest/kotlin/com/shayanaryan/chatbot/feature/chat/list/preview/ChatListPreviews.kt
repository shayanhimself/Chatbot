package com.shayanaryan.chatbot.feature.chat.list.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.list.ChatListScreen
import com.shayanaryan.chatbot.feature.chat.list.ChatListUiState
import com.shayanaryan.chatbot.feature.chat.list.PREVIEW_CHATS

@Composable
private fun ListScreen(uiState: ChatListUiState) {
    ChatListScreen(
        uiState = uiState,
        selectedChatId = null,
        onChatClick = {},
        onNewChat = {},
    )
}

private val populated =
    ChatListUiState(isLoading = false, chats = PREVIEW_CHATS)
private val empty = ChatListUiState(isLoading = false, chats = emptyList())
private val loading = ChatListUiState(isLoading = true)

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

@PreviewTest
@FontScalePreviews
@Composable
private fun ListFontScalePreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(populated) }
}
