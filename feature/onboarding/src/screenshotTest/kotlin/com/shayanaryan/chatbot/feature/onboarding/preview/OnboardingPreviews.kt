package com.shayanaryan.chatbot.feature.onboarding.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.onboarding.ONBOARDING_PREVIEW_HEIGHT_DP
import com.shayanaryan.chatbot.feature.onboarding.OnboardingUiState
import com.shayanaryan.chatbot.feature.onboarding.PREVIEW_API_KEY
import com.shayanaryan.chatbot.feature.onboarding.PreviewOnboarding
import com.shayanaryan.chatbot.shared.ApiError

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingEmptyDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingEmptyLightPreview() {
    ChatbotTheme(darkTheme = false) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingObscuredDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingObscuredLightPreview() {
    ChatbotTheme(darkTheme = false) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRevealedDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
            revealed = true,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRevealedLightPreview() {
    ChatbotTheme(darkTheme = false) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Idle,
            key = PREVIEW_API_KEY,
            revealed = true,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingValidatingDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Validating,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingValidatingLightPreview() {
    ChatbotTheme(darkTheme = false) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Validating,
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRejectedDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Authentication),
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingRejectedLightPreview() {
    ChatbotTheme(darkTheme = false) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Authentication),
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingOfflineDarkPreview() {
    ChatbotTheme(darkTheme = true) {
        PreviewOnboarding(
            uiState = OnboardingUiState.Failed(ApiError.Network),
            key = PREVIEW_API_KEY,
        )
    }
}

@PreviewTest
@Preview(heightDp = ONBOARDING_PREVIEW_HEIGHT_DP)
@Composable
private fun OnboardingOfflineLightPreview() {
    ChatbotTheme(darkTheme = false) {
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
