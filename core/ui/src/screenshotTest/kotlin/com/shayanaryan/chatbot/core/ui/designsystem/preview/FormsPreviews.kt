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
import com.shayanaryan.chatbot.core.ui.designsystem.component.ChipVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsChip
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsSwitch
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsTextField
import com.shayanaryan.chatbot.core.ui.designsystem.component.TextFieldVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun FormsGallery() {
    Surface {
        Column(
            modifier = Modifier.padding(Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            DsTextField(value = "", onValueChange = {}, label = "Label")
            DsTextField(value = "sk-ant-api03-xxxx", onValueChange = {
            }, label = "API key", mono = true, variant = TextFieldVariant.Filled)
            DsTextField(value = "bad", onValueChange = {
            }, label = "Key", isError = true, supportingText = "Invalid key")
            DsTextField(
                value = "clearable",
                onValueChange = {},
                label = "Search",
                // No preview shows the placeholder, it's painted just when focused+empty.
                placeholder = "Type to search",
                leadingGlyph = Glyphs.ARROW_FORWARD,
                trailingGlyph = Glyphs.CLOSE,
                onTrailingClick = {},
            )
            DsTextField(
                value = "read only",
                onValueChange = {},
                label = "Status",
                trailingGlyph = Glyphs.ERROR,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                DsSwitch(checked = true, onCheckedChange = {})
                DsSwitch(checked = false, onCheckedChange = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                ChipVariant.entries.forEach { variant ->
                    DsChip(
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
