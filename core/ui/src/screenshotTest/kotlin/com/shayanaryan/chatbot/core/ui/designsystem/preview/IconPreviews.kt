package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun IconRow() {
    Surface {
        Row(Modifier.padding(Spacing.md)) {
            DsIcon(Glyphs.CLOSE, contentDescription = null)
            DsIcon(Glyphs.ERROR, contentDescription = null, filled = true)
            DsIcon(Glyphs.ARROW_FORWARD, contentDescription = null, weight = 600)
            DsIcon(Glyphs.BRAND, contentDescription = null, filled = true)
        }
    }
}

@PreviewTest
@Preview(name = "icons-dark")
@Composable
private fun IconRowDarkPreview() {
    ChatbotTheme(darkTheme = true) { IconRow() }
}

@PreviewTest
@Preview(name = "icons-light")
@Composable
private fun IconRowLightPreview() {
    ChatbotTheme(darkTheme = false) { IconRow() }
}
