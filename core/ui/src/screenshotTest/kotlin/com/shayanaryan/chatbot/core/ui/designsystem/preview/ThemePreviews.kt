package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotExtendedTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.MonoTextStyle
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun ThemeSwatch() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            Text("Display", style = MaterialTheme.typography.displaySmall)
            Text("Title", style = MaterialTheme.typography.titleMedium)
            Text("Body", style = MaterialTheme.typography.bodyMedium)
            Text("api-key-0000", style = MonoTextStyle)
            Box(
                Modifier
                    .size(Spacing.x4l)
                    .background(MaterialTheme.colorScheme.primary, ChatbotShapes.card),
            )
            Box(
                Modifier
                    .size(Spacing.x4l)
                    .background(ChatbotExtendedTheme.colors.success, ChatbotShapes.card),
            )
        }
    }
}

@PreviewTest
@Preview(name = "theme-dark")
@Composable
private fun ThemeSwatchDarkPreview() {
    ChatbotTheme(darkTheme = true) { ThemeSwatch() }
}

@PreviewTest
@Preview(name = "theme-light")
@Composable
private fun ThemeSwatchLightPreview() {
    ChatbotTheme(darkTheme = false) { ThemeSwatch() }
}
