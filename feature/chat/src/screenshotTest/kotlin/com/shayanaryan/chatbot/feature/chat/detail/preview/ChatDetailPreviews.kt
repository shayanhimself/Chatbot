package com.shayanaryan.chatbot.feature.chat.detail.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.testing.preview.ThemePreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.detail.ChatDetailPreviewData
import com.shayanaryan.chatbot.feature.chat.detail.ChatDetailScreenStubbed

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailNewPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.newChat) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailStreamingPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.streaming) }
}

// The picker's menu is driven by the chip's own state, which a preview cannot open, so the
// golden captures the chip collapsed on an idle chat. The open menu is covered by the
// Compose test instead.
@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailPickerPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailThinkingPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.thinking) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailRateLimitedPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.rateLimited) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailNetworkPreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.network) }
}

@PreviewTest
@ThemePreviews
@Composable
private fun ChatDetailDeletePreview() {
    ChatbotTheme { ChatDetailScreenStubbed(ChatDetailPreviewData.deleting) }
}

// The same screen rendered as the detail pane, where the whole visual difference is the
// missing back arrow.
@PreviewTest
@Preview
@Composable
private fun ChatDetailPanePreview() {
    ChatbotTheme(darkTheme = true) {
        ChatDetailScreenStubbed(ChatDetailPreviewData.openChat, onBack = null)
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatDetailFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun ChatDetailFontScalePreview() {
    ChatbotTheme(darkTheme = true) { ChatDetailScreenStubbed(ChatDetailPreviewData.openChat) }
}
