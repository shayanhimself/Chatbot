package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.modifier.bottomBarSafeDrawingPadding
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_PREVIEW_WIDTH_DP
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * Everything a user can do to the next turn: pick the model, write the message, send or stop it.
 *
 * @param isStreaming disables the picker and turns the composer's send button into stop, since the
 *   model a request already went out with cannot be changed.
 * @param composerState the composed text, held by the screen rather than by `UiState`.
 */
@Composable
fun ChatBottomBar(
    model: ClaudeModel,
    isStreaming: Boolean,
    composerState: TextFieldState,
    onModelSelected: (ClaudeModel) -> Unit,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier =
                Modifier
                    .bottomBarSafeDrawingPadding()
                    .padding(
                        start = Spacing.s3,
                        end = Spacing.s3,
                        top = Spacing.s2,
                        bottom = Spacing.s3,
                    ),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            ModelPickerChip(
                model = model,
                enabled = !isStreaming,
                onModelSelected = onModelSelected,
            )
            Composer(
                state = composerState,
                isStreaming = isStreaming,
                onSend = onSend,
                onCancel = onCancel,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ChatBottomBarPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatBottomBar(
                model = ClaudeModel.Sonnet,
                isStreaming = false,
                composerState = rememberTextFieldState(),
                onModelSelected = {},
                onSend = {},
                onCancel = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ChatBottomBarStreamingPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatBottomBar(
                model = ClaudeModel.Sonnet,
                isStreaming = true,
                composerState = rememberTextFieldState(),
                onModelSelected = {},
                onSend = {},
                onCancel = {},
            )
        }
    }
}
