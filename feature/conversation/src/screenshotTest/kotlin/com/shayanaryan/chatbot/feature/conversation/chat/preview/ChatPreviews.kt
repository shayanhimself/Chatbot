package com.shayanaryan.chatbot.feature.conversation.chat.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.preview.FormFactorPreviews
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_DETAIL_PANE_HEIGHT_DP
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_DETAIL_PANE_WIDTH_DP
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_PREVIEW_HEIGHT_DP
import com.shayanaryan.chatbot.feature.conversation.chat.ChatPreviewData
import com.shayanaryan.chatbot.feature.conversation.chat.PreviewChat

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNewDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.newChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNewLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.newChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatStreamingDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.streaming) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatStreamingLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.streaming) }
}

// The picker's menu is driven by the chip's own state, which a preview cannot open, so the
// golden captures the chip collapsed on an idle chat. The open menu is covered by the
// Compose test instead.
@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatPickerDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatPickerLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.openChat) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatThinkingDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.thinking) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatThinkingLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.thinking) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatRateLimitedDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.rateLimited) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatRateLimitedLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.rateLimited) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNetworkDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.network) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatNetworkLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.network) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatDeleteDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.deleting) }
}

@PreviewTest
@Preview(heightDp = CHAT_PREVIEW_HEIGHT_DP)
@Composable
private fun ChatDeleteLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.deleting) }
}

// The same screen rendered as the detail pane, where the whole visual difference is the
// missing back arrow. The two-pane composition itself lives in :app, covered by the journey.
@PreviewTest
@Preview(heightDp = CHAT_DETAIL_PANE_HEIGHT_DP, widthDp = CHAT_DETAIL_PANE_WIDTH_DP)
@Composable
private fun ChatDetailPaneDarkPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat, onBack = null) }
}

@PreviewTest
@Preview(heightDp = CHAT_DETAIL_PANE_HEIGHT_DP, widthDp = CHAT_DETAIL_PANE_WIDTH_DP)
@Composable
private fun ChatDetailPaneLightPreview() {
    ChatbotTheme(darkTheme = false) { PreviewChat(ChatPreviewData.openChat, onBack = null) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat) }
}
