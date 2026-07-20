package com.shayanaryan.chatbot.core.ui.designsystem.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

// OpenType variation-axis tags the Material Symbols variable font exposes.
// outline ↔ solid
private const val AXIS_FILL = "FILL"

// thin ↔ thick stroke
private const val AXIS_WEIGHT = "wght"

// fine weight nudge, independent of wght
private const val AXIS_GRADE = "GRAD"

// stroke tuning per rendered size
private const val AXIS_OPTICAL_SIZE = "opsz"

/**
 * Renders a Material Symbols Rounded glyph as a themed icon.
 *
 * The [glyph] is a ligature name from [Glyphs] — a wrong value fails silently (nothing draws), so
 * never pass a bare string. The ligature text is stripped from the accessibility tree: pass a
 * [contentDescription] for a meaningful label, or `null` for a purely decorative icon that TalkBack
 * skips. The four variable-font axes are set per call.
 *
 * @param glyph Material Symbols ligature name, always a [Glyphs] constant.
 * @param contentDescription label read by TalkBack, or `null` when the icon is decorative.
 * @param size icon size; also drives the `opsz` optical-size axis.
 * @param filled `FILL` axis — `false` outline (default), `true` solid.
 * @param weight `wght` axis, stroke thickness (default 400).
 * @param grade `GRAD` axis, fine weight nudge independent of [weight] (default 0).
 * @param tint glyph color; defaults to the ambient content color.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun Icon(
    glyph: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    weight: Int = 400,
    grade: Int = 0,
    tint: Color = LocalContentColor.current,
) {
    val fontFamily =
        remember(filled, weight, grade, size) {
            FontFamily(
                Font(
                    R.font.material_symbols_rounded,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.Setting(AXIS_FILL, if (filled) 1f else 0f),
                            FontVariation.Setting(AXIS_WEIGHT, weight.toFloat()),
                            FontVariation.Setting(AXIS_GRADE, grade.toFloat()),
                            FontVariation.Setting(AXIS_OPTICAL_SIZE, size.value),
                        ),
                ),
            )
        }
    // convert the Dp icon size into the sp Text needs, in a way that keeps the glyph a fixed dp regardless of the user's font scale.
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Text(
        text = glyph,
        modifier =
            modifier.clearAndSetSemantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            },
        color = tint,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}

@Preview(showBackground = true)
@Composable
private fun IconPreview() {
    ChatbotTheme {
        Icon(
            glyph = Glyphs.BRAND,
            contentDescription = null,
            filled = true,
            size = 36.dp,
        )
    }
}
