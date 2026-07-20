package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.MonoTextStyle
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.TextField as M3TextField

enum class TextFieldVariant { Outlined, Filled }

/**
 * Design-system text field wrapping the M3 outlined / filled variants.
 *
 * The floating label and the 2dp accent focus border come from the M3 defaults — not re-implemented
 * here. Set [mono] for API keys, model ids, or code, which renders the value in [MonoTextStyle].
 * All copy ([label], [placeholder], [supportingText]) is caller-supplied; the field holds no literal.
 *
 * @param label floating label; drawn inside the field at rest, animating up on focus/content.
 * @param variant outlined (default) or filled.
 * @param leadingGlyph / trailingGlyph [com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs] ligatures for the leading/trailing slots.
 * @param onTrailingClick when non-null, wraps the trailing glyph in a clickable icon button.
 * @param supportingText helper/error text below the field.
 * @param mono renders the value in [MonoTextStyle] for keys / ids / code.
 * @param visualTransformation e.g. password masking for an API key.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    variant: TextFieldVariant = TextFieldVariant.Outlined,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val textStyle = if (mono) MonoTextStyle else LocalTextStyle.current
    val labelComposable: (@Composable () -> Unit)? = label?.let { { Text(it) } }
    val placeholderComposable: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingComposable: (@Composable () -> Unit)? = supportingText?.let { { Text(it) } }
    val leadingComposable: (@Composable () -> Unit)? =
        leadingGlyph?.let {
            { Icon(it, contentDescription = null, size = 20.dp) }
        }
    val trailingComposable: (@Composable () -> Unit)? =
        trailingGlyph?.let { glyph ->
            {
                if (onTrailingClick != null) {
                    M3IconButton(
                        onClick = onTrailingClick,
                    ) { Icon(glyph, contentDescription = null, size = 20.dp) }
                } else {
                    Icon(glyph, contentDescription = null, size = 20.dp)
                }
            }
        }
    when (variant) {
        TextFieldVariant.Outlined -> {
            M3OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                textStyle = textStyle,
                label = labelComposable,
                placeholder = placeholderComposable,
                leadingIcon = leadingComposable,
                trailingIcon = trailingComposable,
                supportingText = supportingComposable,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = singleLine,
                minLines = minLines,
                shape = ChatbotShapes.input,
            )
        }

        TextFieldVariant.Filled -> {
            M3TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                textStyle = textStyle,
                label = labelComposable,
                placeholder = placeholderComposable,
                leadingIcon = leadingComposable,
                trailingIcon = trailingComposable,
                supportingText = supportingComposable,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = singleLine,
                minLines = minLines,
            )
        }
    }
}

@Preview
@Composable
private fun TextFieldPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                TextField(value = "", onValueChange = {}, label = "Label")
                TextField(
                    value = "sk-ant-api03-xxxx",
                    onValueChange = {},
                    label = "API key",
                    mono = true,
                    variant = TextFieldVariant.Filled,
                )
                TextField(
                    value = "bad",
                    onValueChange = {},
                    label = "Key",
                    isError = true,
                    supportingText = "Invalid key",
                )
                TextField(
                    value = "clearable",
                    onValueChange = {},
                    label = "Search",
                    // No preview shows the placeholder, it's painted just when focused+empty.
                    placeholder = "Type to search",
                    leadingGlyph = Glyphs.ARROW_FORWARD,
                    trailingGlyph = Glyphs.CLOSE,
                    onTrailingClick = {},
                )
                TextField(
                    value = "read only",
                    onValueChange = {},
                    label = "Status",
                    trailingGlyph = Glyphs.ERROR,
                )
            }
        }
    }
}
