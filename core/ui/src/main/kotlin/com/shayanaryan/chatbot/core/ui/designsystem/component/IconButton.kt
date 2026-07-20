package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Motion
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.FilledIconButton as M3FilledIconButton
import androidx.compose.material3.FilledTonalIconButton as M3FilledTonalIconButton
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedIconButton as M3OutlinedIconButton

enum class IconButtonVariant { Standard, Filled, Tonal, Outlined }

/**
 * Design-system icon button wrapping the M3 variants with a pressed-scale animation.
 *
 * @param glyph a [com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs] constant.
 * @param contentDescription label read by TalkBack; required since the button carries no text.
 * @param onClick invoked on click.
 * @param variant one of the four M3 icon-button styles.
 * @param enabled `false` dims and disables the button.
 * @param selected renders the glyph filled (and, for the standard variant, tinted primary).
 */
@Composable
fun IconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.Standard,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.pressScaleIconButton else 1f,
        animationSpec = tween(Motion.durationShortMillis, easing = Motion.easingStandard),
        label = "icon-button-press-scale",
    )
    val pressModifier =
        modifier.graphicsLayer {
            // Lambda graphicsLayer only re-draws, has better performance than Modifier.scale()
            scaleX = scale
            scaleY = scale
        }
    when (variant) {
        IconButtonVariant.Standard -> {
            M3IconButton(
                onClick = onClick,
                modifier = pressModifier,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                // selected: glyph fills and tints primary.
                val tint =
                    if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                Icon(
                    glyph,
                    contentDescription = contentDescription,
                    filled = selected,
                    tint = tint,
                )
            }
        }

        IconButtonVariant.Filled -> {
            M3FilledIconButton(
                onClick = onClick,
                modifier = pressModifier,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
        }

        IconButtonVariant.Tonal -> {
            M3FilledTonalIconButton(
                onClick = onClick,
                modifier = pressModifier,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
        }

        IconButtonVariant.Outlined -> {
            M3OutlinedIconButton(
                onClick = onClick,
                modifier = pressModifier,
                enabled = enabled,
                interactionSource = interactionSource,
            ) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
        }
    }
}

@Preview
@Composable
private fun IconButtonPreview() {
    ChatbotTheme {
        Surface {
            Row(Modifier.padding(Spacing.md)) {
                IconButton(
                    glyph = Glyphs.CLOSE,
                    contentDescription = "Selected",
                    onClick = {},
                    selected = true,
                )
                IconButtonVariant.entries.forEach { variant ->
                    IconButton(
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
