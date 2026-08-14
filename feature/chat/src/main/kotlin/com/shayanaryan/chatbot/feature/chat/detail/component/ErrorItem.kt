package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_PREVIEW_WIDTH_DP
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.core.ui.R as CoreUiR

private const val ERROR_MAX_WIDTH_FRACTION = 0.86f

/**
 * A turn that failed, rendered inline where the reply would have been. Losing connectivity gets no
 * special treatment: it is `ApiError.Network` and lands here like any other failure.
 */
@Composable
fun ErrorItem(
    error: ApiError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(ERROR_MAX_WIDTH_FRACTION),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = ComponentShapes.bubbleAssistant,
                    ).padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            DsIcon(
                glyph = Glyphs.ERROR,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = error.text(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        DsButton(
            text = stringResource(CoreUiR.string.core_ui_retry),
            onClick = onRetry,
            variant = ButtonVariant.Tonal,
            leadingGlyph = Glyphs.REFRESH,
        )
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ErrorItemRateLimitedPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ErrorItem(
                error = ApiError.RateLimited(retryAfterSeconds = null),
                onRetry = {},
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ErrorItemNetworkPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ErrorItem(
                error = ApiError.Network,
                onRetry = {},
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}
