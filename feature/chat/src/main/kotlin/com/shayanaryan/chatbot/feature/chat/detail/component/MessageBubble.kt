package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Motion
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.detail.CHAT_PREVIEW_WIDTH_DP
import com.shayanaryan.chatbot.shared.Role

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.82f

// The caret blinks on and off within one period, so each direction takes half of it.
private const val CARET_HALF_PERIOD_DIVISOR = 2

/**
 * One chat turn.
 *
 * @param role user turns sit right in the primary container, assistant turns left on the surface.
 * @param streaming appends a blinking caret, so a reply arriving one token at a time reads as
 *   still in progress rather than as a short answer.
 */
@Composable
fun MessageBubble(
    text: String,
    role: Role,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
) {
    val isUser = role == Role.User
    // The first row is to position the bubble horizontally
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(BUBBLE_MAX_WIDTH_FRACTION)
                    .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
                    .background(
                        color =
                            if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        shape =
                            if (isUser) {
                                ComponentShapes.bubbleUser
                            } else {
                                ComponentShapes.bubbleAssistant
                            },
                    ).padding(horizontal = Spacing.s4, vertical = Spacing.s3)
                    .semantics(mergeDescendants = true) {
                        // Merged so the announced node carries the reply text: a live region with
                        // no text of its own gives a screen reader nothing to speak, and the turn
                        // reads as one focus stop rather than text and caret separately.
                        //
                        // Tokens arrive with no user action behind them, so nothing else would
                        // prompt a screen reader to speak the reply. Polite waits for the current
                        // utterance rather than interrupting on every delta.
                        if (streaming) liveRegion = LiveRegionMode.Polite
                    },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.weight(1f, fill = false),
            )
            if (streaming) {
                StreamingCaret(Modifier.padding(start = Spacing.s1))
            }
        }
    }
}

@Composable
private fun StreamingCaret(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "streaming-caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = Motion.caretBlinkMillis / CARET_HALF_PERIOD_DIVISOR,
                        easing = Motion.easingStandard,
                    ),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "streaming-caret-alpha",
    )
    Box(
        modifier
            .alpha(alpha)
            .width(8.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
    )
}

@Preview(widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun MessageBubbleUserPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            MessageBubble(
                text = "help me plan a weekend in portland",
                role = Role.User,
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}

@Preview(widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun MessageBubbleAssistantPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            MessageBubble(
                text = "Love it. Two nights? I'd do Powell's Books + a food-cart lunch Saturday.",
                role = Role.Assistant,
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}

@Preview(widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun MessageBubbleStreamingPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            MessageBubble(
                text = "Love it. Two nights? I'd do Powell's Books + a food-cart lunch",
                role = Role.Assistant,
                streaming = true,
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}
