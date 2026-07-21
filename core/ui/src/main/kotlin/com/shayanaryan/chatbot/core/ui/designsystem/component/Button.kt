package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Motion
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.CircularProgressIndicator as M3CircularProgressIndicator
import androidx.compose.material3.ElevatedButton as M3ElevatedButton
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.TextButton as M3TextButton

enum class ButtonVariant { Filled, Tonal, Outlined, Text, Elevated }

/**
 * Design-system button wrapping the M3 variants, pressed-scale animation, and a [loading] state.
 *
 * @param text button label.
 * @param onClick invoked on click; a no-op while [loading].
 * @param variant one of the five M3 button styles.
 * @param enabled `false` dims and disables the button.
 * @param loading `true` shows a trailing spinner and blocks the click while keeping full colour.
 * @param leadingGlyph optional [com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs] constant before the label.
 * @param trailingGlyph optional [com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs] constant after the label; hidden while [loading].
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.pressScaleButton else 1f,
        animationSpec = tween(Motion.durationShortMillis, easing = Motion.easingStandard),
        label = "button-press-scale",
    )
    // Loading is deliberately not folded into `enabled`: disabled dims the button, loading keeps
    // the full filled appearance and the label, and only swallows the click.
    val loadingDescription = stringResource(R.string.core_ui_loading)
    val pressModifier =
        modifier
            .graphicsLayer {
                // Lambda graphicsLayer only re-draws, has better performance than Modifier.scale()
                scaleX = scale
                scaleY = scale
            }.semantics { if (loading) stateDescription = loadingDescription }

    val action = if (loading) ({}) else onClick
    val shape = ChatbotShapes.button
    val content = buttonContent(text, loading, leadingGlyph, trailingGlyph)
    when (variant) {
        ButtonVariant.Filled -> {
            M3Button(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = content,
            )
        }

        ButtonVariant.Tonal -> {
            M3FilledTonalButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = content,
            )
        }

        ButtonVariant.Outlined -> {
            M3OutlinedButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = content,
            )
        }

        ButtonVariant.Text -> {
            M3TextButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = content,
            )
        }

        ButtonVariant.Elevated -> {
            M3ElevatedButton(
                onClick = action,
                modifier = pressModifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = content,
            )
        }
    }
}

/**
 * Builds the row content shared by every [ButtonVariant].
 */
private fun buttonContent(
    text: String,
    loading: Boolean,
    leadingGlyph: String?,
    trailingGlyph: String?,
): @Composable RowScope.() -> Unit =
    {
        if (leadingGlyph != null) {
            Icon(leadingGlyph, contentDescription = null, size = 18.dp)
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text)
        // The spinner occupies the trailing slot, replacing any trailing glyph while it spins.
        if (loading) {
            Spacer(Modifier.width(Spacing.xs))
            M3CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                trackColor = LocalContentColor.current.copy(alpha = 0.25f),
            )
        } else if (trailingGlyph != null) {
            Spacer(Modifier.width(Spacing.xs))
            Icon(trailingGlyph, contentDescription = null, size = 18.dp)
        }
    }

@Preview
@Composable
private fun ButtonPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                ButtonVariant.entries.forEach { variant ->
                    Button(
                        text = variant.name,
                        onClick = {},
                        variant = variant,
                        leadingGlyph = Glyphs.CLOSE,
                    )
                }
                Button(text = "Disabled", onClick = {}, enabled = false)
                Button(text = "Loading", onClick = {}, loading = true)
            }
        }
    }
}
