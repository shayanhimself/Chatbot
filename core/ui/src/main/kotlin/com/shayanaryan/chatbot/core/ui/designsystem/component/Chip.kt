package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.AssistChip as M3AssistChip
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.InputChip as M3InputChip
import androidx.compose.material3.SuggestionChip as M3SuggestionChip

/** The four Material 3 chip types; each differs in behavior and appearance. */
enum class ChipVariant {
    /** A contextual action; stateless. */
    Assist,

    /** A toggleable filter reflecting [Chip]'s `selected` state. */
    Filter,

    /** A piece of user-entered content; dismissible via `onDismiss`. */
    Input,

    /** A dynamically offered suggestion; stateless, leading slot is an `icon`. */
    Suggestion,
}

/**
 * Design-system chip wrapping the four M3 chip variants on the pill [ChatbotShapes.chip].
 *
 * [selected] applies to the toggleable variants (filter / input). [onDismiss], when non-null on an
 * input chip, renders a trailing close button labelled from the generic `core_ui_dismiss` string.
 *
 * @param label chip text; caller-supplied copy.
 * @param variant assist (default) / filter / input / suggestion.
 * @param selected selection state for filter/input chips.
 * @param leadingGlyph optional [Glyphs] ligature for the leading slot.
 * @param onDismiss when non-null on an input chip, shows a trailing dismiss button.
 */
@Composable
fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Assist,
    selected: Boolean = false,
    leadingGlyph: String? = null,
    onDismiss: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val shape = ChatbotShapes.chip
    val labelComposable: @Composable () -> Unit = { Text(label) }
    val leadingComposable: (@Composable () -> Unit)? =
        leadingGlyph?.let {
            { Icon(it, contentDescription = null, size = 18.dp) }
        }
    when (variant) {
        ChipVariant.Assist -> {
            M3AssistChip(
                onClick = onClick,
                label = labelComposable,
                modifier = modifier,
                enabled = enabled,
                leadingIcon = leadingComposable,
                shape = shape,
            )
        }

        ChipVariant.Filter -> {
            M3FilterChip(
                selected = selected,
                onClick = onClick,
                label = labelComposable,
                modifier = modifier,
                enabled = enabled,
                leadingIcon = leadingComposable,
                shape = shape,
            )
        }

        ChipVariant.Input -> {
            M3InputChip(
                selected = selected,
                onClick = onClick,
                label = labelComposable,
                modifier = modifier,
                enabled = enabled,
                leadingIcon = leadingComposable,
                shape = shape,
                trailingIcon =
                    onDismiss?.let { dismiss ->
                        { DismissButton(label = label, onDismiss = dismiss) }
                    },
            )
        }

        ChipVariant.Suggestion -> {
            M3SuggestionChip(
                onClick = onClick,
                label = labelComposable,
                modifier = modifier,
                enabled = enabled,
                icon = leadingComposable,
                shape = shape,
            )
        }
    }
}

/**
 * Trailing dismiss affordance for an input chip. A bare clickable [Icon] rather than an M3
 * `IconButton`: the button's 48dp minimum interactive size does not fit the 32dp chip and would
 * inflate the trailing slot. The click target is therefore the glyph itself; the content
 * description keeps the affordance accessible.
 */
@Composable
private fun DismissButton(
    label: String,
    onDismiss: () -> Unit,
) {
    val description = stringResource(R.string.core_ui_dismiss, label)
    Box(
        modifier =
            Modifier
                .clickable(onClick = onDismiss)
                .semantics {
                    this.contentDescription = description
                    this.role = Role.Button
                },
    ) {
        Icon(Glyphs.CLOSE, contentDescription = null, size = 18.dp)
    }
}

@Preview
@Composable
private fun ChipPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Row(
                modifier = Modifier.padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                ChipVariant.entries.forEach { variant ->
                    Chip(
                        label = variant.name,
                        onClick = {},
                        variant = variant,
                        selected = variant == ChipVariant.Filter,
                        onDismiss =
                            if (variant == ChipVariant.Input) {
                                fun() {}
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}
