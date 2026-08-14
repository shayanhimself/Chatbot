package com.shayanaryan.chatbot.feature.onboarding

import com.shayanaryan.chatbot.shared.ApiError

/**
 * Everything the OnboardingViewModel decides about key entry.
 *
 * Nothing here describes the key text: the ViewModel does not see it until it is submitted, so it
 * cannot produce anything derived from it. Whether submission is enabled, and whether the key is
 * revealed, are the screen's to derive from the field it holds.
 */
sealed interface OnboardingUiState {
    data object Idle : OnboardingUiState

    data object Validating : OnboardingUiState

    /**
     * @property error why the attempt failed. Only [ApiError.Authentication] says the key itself
     *   is wrong; every other case leaves its validity unknown and stays retryable.
     */
    data class Failed(
        val error: ApiError,
    ) : OnboardingUiState
}
