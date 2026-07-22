package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsIconButton
import com.shayanaryan.chatbot.core.ui.designsystem.component.IconButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun ButtonGallery() {
    Surface {
        Column(Modifier.padding(Spacing.s4)) {
            ButtonVariant.entries.forEach { variant ->
                DsButton(
                    text = variant.name,
                    onClick = {},
                    variant = variant,
                    leadingGlyph = Glyphs.CLOSE,
                )
            }
            DsButton(text = "Continue", onClick = {}, trailingGlyph = Glyphs.ARROW_FORWARD)
            DsButton(text = "Disabled", onClick = {}, enabled = false)
            // Loading sits beside disabled on purpose — the golden is what proves they look different.
            DsButton(text = "Loading", onClick = {}, loading = true)
            Row {
                DsIconButton(
                    glyph = Glyphs.CLOSE,
                    contentDescription = "Selected",
                    onClick = {},
                    selected = true,
                )
                IconButtonVariant.entries.forEach { variant ->
                    DsIconButton(
                        glyph = Glyphs.CLOSE,
                        contentDescription = variant.name,
                        onClick = {},
                        variant = variant,
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "buttons-dark")
@Composable
private fun ButtonGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { ButtonGallery() }
}

@PreviewTest
@Preview(name = "buttons-light")
@Composable
private fun ButtonGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { ButtonGallery() }
}
