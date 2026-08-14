package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_PREVIEW_WIDTH_DP
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * The per-chat model switch.
 *
 * @param enabled false during a turn, for the same reason the composer's send button is: the model
 *   a request already went out with cannot be changed.
 */
@Composable
fun ModelPickerChip(
    model: ClaudeModel,
    enabled: Boolean,
    onModelSelected: (ClaudeModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier =
                Modifier
                    .height(36.dp)
                    .background(
                        color =
                            if (expanded) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                Color.Transparent
                            },
                        shape = CircleShape,
                    ).border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(start = Spacing.s3, end = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DsIcon(
                glyph = if (expanded) Glyphs.EXPAND_LESS else Glyphs.EXPAND_MORE,
                contentDescription = null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ClaudeModel.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onModelSelected(option)
                    },
                    trailingIcon = {
                        if (option == model) {
                            DsIcon(
                                glyph = Glyphs.CHECK,
                                contentDescription = null,
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ModelPickerChipPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ModelPickerChip(
                model = ClaudeModel.Sonnet,
                enabled = true,
                onModelSelected = {},
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ModelPickerChipDisabledPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ModelPickerChip(
                model = ClaudeModel.Haiku,
                enabled = false,
                onModelSelected = {},
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}
