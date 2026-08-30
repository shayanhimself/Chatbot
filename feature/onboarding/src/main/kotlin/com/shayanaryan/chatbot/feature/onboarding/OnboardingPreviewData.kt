package com.shayanaryan.chatbot.feature.onboarding

import androidx.compose.runtime.Composable

// An obviously fake value, so no reader mistakes it for a working key.
internal const val PREVIEW_API_KEY = "sk-ant-api03-9f2b7XaQ1eRcT"

/**
 * [OnboardingScreen] with every event lambda stubbed out, so a preview or a golden only has to name
 * what it renders.
 *
 * @param key the field's contents, which is what separates the empty frame from the filled ones.
 * @param revealed whether the key is shown rather than masked.
 */
@Composable
internal fun PreviewOnboarding(
    status: OnboardingStatus,
    key: String = "",
    revealed: Boolean = false,
) {
    OnboardingScreen(
        uiState =
            OnboardingUiState(
                key = key,
                revealed = revealed,
                status = status,
            ),
        onKeyChange = {},
        onToggleReveal = {},
        onSubmit = {},
        onConsoleClick = {},
    )
}
