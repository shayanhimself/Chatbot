package com.shayanaryan.chatbot.feature.onboarding.preview

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.testing.preview.ThemePreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.onboarding.OnboardingUiState
import com.shayanaryan.chatbot.feature.onboarding.PREVIEW_API_KEY
import com.shayanaryan.chatbot.feature.onboarding.PreviewOnboarding
import com.shayanaryan.chatbot.shared.ApiError

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingEmptyPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingObscuredPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingRevealedPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
            revealed = true,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingValidatingPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Validating,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingRejectedPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Authentication),
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingOfflinePreview() {
    ChatbotTheme {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Network),
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun OnboardingFormFactorPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@FontScalePreviews
@Composable
private fun OnboardingFontScalePreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}
