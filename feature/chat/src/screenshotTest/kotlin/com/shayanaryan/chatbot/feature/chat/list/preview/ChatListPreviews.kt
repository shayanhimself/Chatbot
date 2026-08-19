package com.shayanaryan.chatbot.feature.chat.list.preview

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.testing.preview.ThemePreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.list.ChatListPreviewData
import com.shayanaryan.chatbot.feature.chat.list.ChatListScreenStubbed

@PreviewTest
@ThemePreviews
@Composable
private fun ChatListPopulatedPreview() {
    ChatbotTheme { ChatListScreenStubbed(ChatListPreviewData.populated) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatListEmptyPreview() {
    ChatbotTheme { ChatListScreenStubbed(ChatListPreviewData.empty) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatListLoadingPreview() {
    ChatbotTheme { ChatListScreenStubbed(ChatListPreviewData.loading) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatListFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { ChatListScreenStubbed(ChatListPreviewData.populated) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun ChatListFontScalePreview() {
    ChatbotTheme(darkTheme = true) { ChatListScreenStubbed(ChatListPreviewData.populated) }
}
