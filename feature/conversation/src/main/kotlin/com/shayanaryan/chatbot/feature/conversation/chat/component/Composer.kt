package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsIconButton
import com.shayanaryan.chatbot.core.ui.designsystem.component.IconButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_PREVIEW_WIDTH_DP

/**
 * The message input and its trailing action.
 *
 * @param state the composed text. Held by the screen in a saveable [TextFieldState] rather than in
 *   `UiState`: it survives rotation on its own, and the ViewModel only ever sees the finished
 *   string.
 * @param isStreaming turns the trailing button from send into stop, which is the only control a
 *   user has over a turn in flight.
 */
@Composable
fun Composer(
    state: TextFieldState,
    isStreaming: Boolean,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend by remember(state) { derivedStateOf { state.text.isNotBlank() } }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = Spacing.touchTargetMin)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.extraLarge,
                    ).padding(start = Spacing.s4, end = Spacing.s1),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                state = state,
                modifier = Modifier.padding(vertical = Spacing.s3),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorator = { inner ->
                    if (state.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.conversation_composer_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
        if (isStreaming) {
            DsIconButton(
                glyph = Glyphs.STOP,
                contentDescription = stringResource(R.string.conversation_stop),
                onClick = onCancel,
                modifier = Modifier.size(Spacing.touchTargetMin),
                variant = IconButtonVariant.Filled,
            )
        } else {
            DsIconButton(
                glyph = Glyphs.ARROW_UPWARD,
                contentDescription = stringResource(R.string.conversation_send),
                onClick = {
                    onSend(state.text.toString())
                    state.clearText()
                },
                modifier = Modifier.size(Spacing.touchTargetMin),
                variant = IconButtonVariant.Filled,
                enabled = canSend,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ComposerEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Composer(
                state = rememberTextFieldState(),
                isStreaming = false,
                onSend = {},
                onCancel = {},
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ComposerWrittenPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Composer(
                state = rememberTextFieldState(initialText = "how do i make a miso glaze"),
                isStreaming = false,
                onSend = {},
                onCancel = {},
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ComposerStreamingPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Composer(
                state = rememberTextFieldState(),
                isStreaming = true,
                onSend = {},
                onCancel = {},
                modifier = Modifier.padding(Spacing.s3),
            )
        }
    }
}
