package com.shayanaryan.chatbot.feature.chat.detail.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.testing.preview.ThemePreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.detail.ChatDetailPreviewData
import com.shayanaryan.chatbot.feature.chat.detail.PreviewChat

@PreviewTest
@ThemePreviews
@Composable
private fun ChatNewPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.newChat) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatStreamingPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.streaming) }
}

// The picker's menu is driven by the chip's own state, which a preview cannot open, so the
// golden captures the chip collapsed on an idle chat. The open menu is covered by the
// Compose test instead.
@PreviewTest
@ThemePreviews
@Composable
private fun ChatPickerPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatThinkingPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.thinking) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatRateLimitedPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.rateLimited) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatNetworkPreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.network) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDeletePreview() {
    ChatbotTheme { PreviewChat(ChatDetailPreviewData.deleting) }
}

// The same screen rendered as the detail pane, where the whole visual difference is the
// missing back arrow.
@PreviewTest
@Preview
@Composable
private fun ChatDetailPanePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat, onBack = null) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun ChatFontScalePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat) }
}
