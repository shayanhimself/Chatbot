package com.shayanaryan.chatbot.feature.onboarding.preview

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.testing.preview.FontScalePreviews
import com.shayanaryan.chatbot.core.testing.preview.FormFactorPreviews
import com.shayanaryan.chatbot.core.testing.preview.ThemePreviews
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.onboarding.OnboardingStatus
import com.shayanaryan.chatbot.feature.onboarding.PREVIEW_API_KEY
import com.shayanaryan.chatbot.feature.onboarding.PreviewOnboarding
import com.shayanaryan.chatbot.shared.ApiError

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingEmptyPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            status = OnboardingStatus.Idle,
        )
    }
}

@PreviewTest
@ThemePreviews
@Composable
private fun OnboardingObscuredPreview() {
    ChatbotTheme {
        PreviewOnboarding(
            status = OnboardingStatus.Idle,
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
            status = OnboardingStatus.Idle,
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
            status = OnboardingStatus.Validating,
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
            status = OnboardingStatus.Failed(ApiError.Authentication),
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
            status = OnboardingStatus.Failed(ApiError.Network),
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
            status = OnboardingStatus.Idle,
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
            status = OnboardingStatus.Idle,
            key = PREVIEW_API_KEY,
        )
    }
}
