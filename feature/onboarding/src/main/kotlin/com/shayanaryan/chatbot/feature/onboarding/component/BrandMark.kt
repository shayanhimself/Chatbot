package com.shayanaryan.chatbot.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.RadiusPrimitives
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.onboarding.ONBOARDING_PREVIEW_WIDTH_DP

private val brandMarkSize = 72.dp
private val brandMarkGlyphSize = 42.dp

/** The app's mark, on the tinted rounded square onboarding opens with. */
@Composable
internal fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(brandMarkSize)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(RadiusPrimitives.radius7),
                ),
        contentAlignment = Alignment.Center,
    ) {
        DsIcon(
            glyph = Glyphs.BRAND,
            contentDescription = null,
            size = brandMarkGlyphSize,
            filled = true,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Preview
@Composable
private fun BrandMarkPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            // Surface propagates its minimum constraints, and the preview's width is a fixed one,
            // so the mark is only square inside something that relaxes them, as its Column does.
            Box(modifier = Modifier.padding(Spacing.gutter)) {
                BrandMark()
            }
        }
    }
}
