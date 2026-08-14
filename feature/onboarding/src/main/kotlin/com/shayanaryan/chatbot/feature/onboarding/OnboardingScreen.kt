package com.shayanaryan.chatbot.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.onboarding.component.BrandMark
import com.shayanaryan.chatbot.feature.onboarding.component.KeyField
import com.shayanaryan.chatbot.shared.ApiError

/** The shortest input worth spending a network round trip on, per the design. */
internal const val MIN_KEY_LENGTH = 12

/** Caps the content column so the field does not stretch across a tablet. */
internal val contentMaxWidth = 480.dp

/**
 * First-launch key entry screen.
 *
 * @param uiState what the ViewModel decided. Everything else the screen shows, it derives from the
 *   field it holds.
 * @param onSubmit carries the typed key. Fired only at or above [MIN_KEY_LENGTH], and never while
 *   a check is already in flight.
 * @param onKeyEdited fired on every edit, so a failure stops being red as soon as the user starts
 *   correcting it.
 * @param onConsoleClick the console footer was tapped.
 * @param keyState the state of the api key. A plain [remember] rather than a saveable one:
 *   saved state is handed to the system, which keeps a copy outside this process that the app can
 *   neither scope nor clear. Hoisted so a preview or a test can supply a filled field.
 * @param revealedState whether the key is shown rather than masked, hoisted for the same reason.
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onSubmit: (String) -> Unit,
    onKeyEdited: () -> Unit,
    onConsoleClick: () -> Unit,
    modifier: Modifier = Modifier,
    keyState: MutableState<String> = remember { mutableStateOf("") },
    revealedState: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    var key by keyState
    var revealed by revealedState

    val validating = uiState == OnboardingUiState.Validating
    val failure = (uiState as? OnboardingUiState.Failed)?.error
    val submittable = key.length >= MIN_KEY_LENGTH
    val submit = { if (submittable && !validating) onSubmit(key) }

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = contentMaxWidth)
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(
                            start = Spacing.s6,
                            end = Spacing.s6,
                            top = Spacing.s8,
                            bottom = Spacing.s6,
                        ),
            ) {
                Column(
                    // This column takes whatever space the button and footer leave, and scrolls
                    // inside it. The keyboard's inset takes a big portion of the height and the key
                    // field is the last item, so it would be the first thing clipped without the
                    // scroller.
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    BrandMark()
                    Spacer(Modifier.height(Spacing.s6))
                    Text(
                        text = stringResource(R.string.onboarding_headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.s4))
                    Text(
                        text = stringResource(R.string.onboarding_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.s8))
                    KeyField(
                        key = key,
                        revealed = revealed,
                        validating = validating,
                        failure = failure,
                        onKeyChange = {
                            key = it
                            onKeyEdited()
                        },
                        onToggleReveal = { revealed = !revealed },
                        onSubmit = submit,
                    )
                }
                DsButton(
                    text = stringResource(submitLabel(validating, failure, submittable)),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = submittable,
                    loading = validating,
                    trailingGlyph = if (failure != null) Glyphs.REFRESH else Glyphs.ARROW_FORWARD,
                )
                Spacer(Modifier.height(Spacing.s4))
                // The webpage to get the API key from
                Text(
                    text = stringResource(R.string.onboarding_console),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Spacing.touchTargetMin)
                            .clickable(onClick = onConsoleClick)
                            .wrapContentHeight(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** One button, four labels: the label says what the next tap will do. */
private fun submitLabel(
    validating: Boolean,
    failure: ApiError?,
    submittable: Boolean,
): Int =
    when {
        validating -> R.string.onboarding_submit_validating
        failure != null -> R.string.onboarding_submit_retry
        submittable -> R.string.onboarding_submit
        else -> R.string.onboarding_submit_empty
    }

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
        )
    }
}

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingObscuredPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRevealedPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
            revealed = true,
        )
    }
}

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingValidatingPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Validating,
            key = PREVIEW_API_KEY,
        )
    }
}

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRejectedPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Authentication),
            key = PREVIEW_API_KEY,
        )
    }
}

@Preview(showBackground = true, heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingOfflinePreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Network),
            key = PREVIEW_API_KEY,
        )
    }
}
