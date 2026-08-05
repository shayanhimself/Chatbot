package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R

private const val DOT_COUNT = 3
private const val DOT_CYCLE_MILLIS = 1200
private const val DOT_STAGGER_MILLIS = 200
private const val DOT_MIN_ALPHA = 0.25f

// Each dot fades up and back down within one cycle, so a fade takes half of it.
private const val DOT_HALF_CYCLE_DIVISOR = 2

/** The turn has started but no token has arrived yet. */
@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.conversation_thinking)
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier =
            modifier
                .semantics { contentDescription = description }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = ComponentShapes.bubbleAssistant,
                ).padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = DOT_MIN_ALPHA,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(DOT_CYCLE_MILLIS / DOT_HALF_CYCLE_DIVISOR),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * DOT_STAGGER_MILLIS),
                    ),
                label = "thinking-dot-$index",
            )
            Box(
                Modifier
                    .alpha(alpha)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ThinkingIndicatorPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ThinkingIndicator(modifier = Modifier.padding(Spacing.gutter))
        }
    }
}
