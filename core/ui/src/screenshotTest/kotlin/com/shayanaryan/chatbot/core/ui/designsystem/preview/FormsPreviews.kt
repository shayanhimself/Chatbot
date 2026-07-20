package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.Chip
import com.shayanaryan.chatbot.core.ui.designsystem.component.ChipVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.Switch
import com.shayanaryan.chatbot.core.ui.designsystem.component.TextField
import com.shayanaryan.chatbot.core.ui.designsystem.component.TextFieldVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun FormsGallery() {
    Surface {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextField(value = "", onValueChange = {}, label = "Label")
            TextField(value = "sk-ant-api03-xxxx", onValueChange = {
            }, label = "API key", mono = true, variant = TextFieldVariant.Filled)
            TextField(value = "bad", onValueChange = {
            }, label = "Key", isError = true, supportingText = "Invalid key")
            TextField(
                value = "clearable",
                onValueChange = {},
                label = "Search",
                // No preview shows the placeholder, it's painted just when focused+empty.
                placeholder = "Type to search",
                leadingGlyph = Glyphs.ARROW_FORWARD,
                trailingGlyph = Glyphs.CLOSE,
                onTrailingClick = {},
            )
            TextField(
                value = "read only",
                onValueChange = {},
                label = "Status",
                trailingGlyph = Glyphs.ERROR,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Switch(checked = true, onCheckedChange = {})
                Switch(checked = false, onCheckedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                ChipVariant.entries.forEach { variant ->
                    Chip(
                        label = variant.name,
                        onClick = {},
                        variant = variant,
                        selected = variant == ChipVariant.Filter,
                        onDismiss =
                            if (variant == ChipVariant.Input) {
                                fun() {}
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "forms-dark")
@Composable
private fun FormsGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { FormsGallery() }
}

@PreviewTest
@Preview(name = "forms-light")
@Composable
private fun FormsGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { FormsGallery() }
}
