package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.BadgeTone
import com.shayanaryan.chatbot.core.ui.designsystem.component.CardVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsBadge
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsCard
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun CardBadgeGallery() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            CardVariant.entries.forEach { variant ->
                DsCard(variant = variant, modifier = Modifier.padding(top = Spacing.xs)) {
                    Text(variant.name, Modifier.padding(Spacing.md))
                }
            }
            Row(Modifier.padding(top = Spacing.xs)) {
                BadgeTone.entries.forEach { tone ->
                    DsBadge(tone = tone, text = "3", modifier = Modifier.padding(end = Spacing.xxs))
                }
                // Render a dot badge
                DsBadge()
            }
        }
    }
}

@PreviewTest
@Preview(name = "cards-badges-dark")
@Composable
private fun CardBadgeGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { CardBadgeGallery() }
}

@PreviewTest
@Preview(name = "cards-badges-light")
@Composable
private fun CardBadgeGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { CardBadgeGallery() }
}
