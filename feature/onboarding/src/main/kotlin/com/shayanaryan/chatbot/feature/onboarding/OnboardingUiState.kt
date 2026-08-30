package com.shayanaryan.chatbot.feature.onboarding

import com.shayanaryan.chatbot.shared.ApiError

/** The shortest input worth spending a network round trip on, per the design. */
internal const val MIN_KEY_LENGTH = 12

/** Stands in for the key in a printed state, so the value itself is never rendered. */
private const val REDACTED_KEY = "redacted"

/** Stands in for a key that has not been typed yet, which is worth telling apart from one that has. */
private const val EMPTY_KEY = "empty"

/**
 * Everything onboarding screen needs.
 *
 * @property key the API key being typed.
 * @property revealed shows the key as typed rather than masked.
 * @property status drives the submit button's label, its spinner, and the field's error state.
 */
data class OnboardingUiState(
    val key: String = "",
    val revealed: Boolean = false,
    val status: OnboardingStatus = OnboardingStatus.Idle,
) {
    /** Whether the key is long enough to be worth checking. */
    val submittable: Boolean get() = key.length >= MIN_KEY_LENGTH

    /**
     * Overridden for security reasons:
     * The generated `toString` would print the key, and a UiState is what reaches a log line, a
     * crash breadcrumb, or a recomposition trace.
     */
    override fun toString(): String =
        "OnboardingUiState(" +
            "key=${if (key.isEmpty()) EMPTY_KEY else REDACTED_KEY}, " +
            "revealed=$revealed, " +
            "status=$status" +
            ")"
}

/** Where an attempt to store the key has got to. */
sealed interface OnboardingStatus {
    data object Idle : OnboardingStatus

    data object Validating : OnboardingStatus

    /**
     * @property error why the attempt failed. Only [ApiError.Authentication] says the key itself
     *   is wrong; every other case leaves its validity unknown and stays retryable.
     */
    data class Failed(
        val error: ApiError,
    ) : OnboardingStatus
}
