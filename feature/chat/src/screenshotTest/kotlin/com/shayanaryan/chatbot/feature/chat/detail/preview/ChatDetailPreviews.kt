package com.shayanaryan.chatbot.feature.chat.detail.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_DETAIL_PANE_HEIGHT_DP
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_DETAIL_PANE_WIDTH_DP
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_PREVIEW_HEIGHT_DP
import com.shayanaryan.chatbot.feature.chat.detail.ChatDetailPreviewData
import com.shayanaryan.chatbot.feature.chat.detail.PreviewChat

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNewDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.newChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNewLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.newChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatStreamingDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.streaming) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatStreamingLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.streaming) }
}

// The picker's menu is driven by the chip's own state, which a preview cannot open, so the
// golden captures the chip collapsed on an idle chat. The open menu is covered by the
// Compose test instead.
@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatPickerDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatPickerLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.openChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatThinkingDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.thinking) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatThinkingLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.thinking) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatRateLimitedDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.rateLimited) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatRateLimitedLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.rateLimited) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNetworkDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.network) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNetworkLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.network) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatDeleteDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.deleting) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatDeleteLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.deleting) }
}

// The same screen rendered as the detail pane, where the whole visual difference is the
// missing back arrow. The two-pane composition itself lives in :app, covered by the journey.
@PreviewTest
@Preview(heightDp = CHAT_DETAIL_PANE_HEIGHT_DP, widthDp = CHAT_DETAIL_PANE_WIDTH_DP)
@Composable
private fun ChatDetailPaneDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat, onBack = null) }
}

@PreviewTest
@Preview(heightDp = CHAT_DETAIL_PANE_HEIGHT_DP, widthDp = CHAT_DETAIL_PANE_WIDTH_DP)
@Composable
private fun ChatDetailPaneLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatDetailPreviewData.openChat, onBack = null) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatDetailPreviewData.openChat) }
}
