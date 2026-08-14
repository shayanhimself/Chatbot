package com.shayanaryan.chatbot.feature.onboarding.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsTextField
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.onboarding.PREVIEW_API_KEY
import com.shayanaryan.chatbot.feature.onboarding.R
import com.shayanaryan.chatbot.shared.ApiError

/**
 * The API key input field: a masked, monospaced field whose trailing slot doubles as the reveal
 * toggle and, once a check has failed, as the error indicator that replaces it.
 *
 * @param revealed shows the key as typed rather than masked.
 * @param validating disables the field while a check is in flight.
 * @param failure the last failed check, or null when there is none. Puts the field in its error
 *   state and takes the trailing slot over from the reveal toggle, so the key cannot be revealed
 *   again until an edit clears the failure.
 * @param onSubmit the keyboard's action key was pressed, which submits the key without the button.
 */
@Composable
internal fun KeyField(
    key: String,
    revealed: Boolean,
    validating: Boolean,
    failure: ApiError?,
    onKeyChange: (String) -> Unit,
    onToggleReveal: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleable = failure == null && !validating
    DsTextField(
        value = key,
        onValueChange = onKeyChange,
        modifier = modifier.fillMaxWidth(),
        label = stringResource(R.string.onboarding_key_label),
        placeholder = stringResource(R.string.onboarding_key_placeholder),
        leadingGlyph = Glyphs.KEY,
        trailingGlyph =
            when {
                failure != null -> failure.trailingGlyph()
                revealed -> Glyphs.VISIBILITY_OFF
                else -> Glyphs.VISIBILITY
            },
        onTrailingClick = if (toggleable) onToggleReveal else null,
        trailingContentDescription =
            when {
                failure != null -> null
                revealed -> stringResource(R.string.onboarding_key_hide)
                else -> stringResource(R.string.onboarding_key_reveal)
            },
        supportingText =
            when {
                failure != null -> failure.supportingText()
                validating -> stringResource(R.string.onboarding_key_helper_validating)
                else -> stringResource(R.string.onboarding_key_helper)
            },
        isError = failure != null,
        enabled = !validating,
        mono = true,
        visualTransformation =
            if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Go,
            ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
    )
}

@Composable
private fun PreviewKeyField(
    key: String = "",
    revealed: Boolean = false,
    validating: Boolean = false,
    failure: ApiError? = null,
) {
    ChatbotTheme(darkTheme = true) {
        Surface {
            KeyField(
                key = key,
                revealed = revealed,
                validating = validating,
                failure = failure,
                onKeyChange = {},
                onToggleReveal = {},
                onSubmit = {},
                modifier = Modifier.padding(Spacing.gutter),
            )
        }
    }
}

@Preview
@Composable
private fun KeyFieldEmptyPreview() {
    PreviewKeyField()
}

@Preview
@Composable
private fun KeyFieldObscuredPreview() {
    PreviewKeyField(
        key = PREVIEW_API_KEY,
    )
}

@Preview
@Composable
private fun KeyFieldRevealedPreview() {
    PreviewKeyField(
        key = PREVIEW_API_KEY,
        revealed = true,
    )
}

@Preview
@Composable
private fun KeyFieldValidatingPreview() {
    PreviewKeyField(
        key = PREVIEW_API_KEY,
        validating = true,
    )
}

@Preview
@Composable
private fun KeyFieldRejectedPreview() {
    PreviewKeyField(
        key = PREVIEW_API_KEY,
        failure = ApiError.Authentication,
    )
}

@Preview
@Composable
private fun KeyFieldOfflinePreview() {
    PreviewKeyField(
        key = PREVIEW_API_KEY,
        failure = ApiError.Network,
    )
}
