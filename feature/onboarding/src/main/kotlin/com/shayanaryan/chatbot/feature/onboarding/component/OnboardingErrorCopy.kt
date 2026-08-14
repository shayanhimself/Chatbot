package com.shayanaryan.chatbot.feature.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.feature.onboarding.R
import com.shayanaryan.chatbot.shared.ApiError

/** The sentence shown under the field for a failed check. */
@Composable
internal fun ApiError.supportingText(): String =
    when (this) {
        ApiError.Authentication -> {
            stringResource(R.string.onboarding_error_authentication)
        }

        ApiError.Network -> {
            stringResource(R.string.onboarding_error_network)
        }

        ApiError.Timeout -> {
            stringResource(R.string.onboarding_error_timeout)
        }

        is ApiError.RateLimited -> {
            retryAfterSeconds?.let {
                stringResource(R.string.onboarding_error_rate_limited_after, it)
            } ?: stringResource(R.string.onboarding_error_rate_limited)
        }

        ApiError.Overloaded -> {
            stringResource(R.string.onboarding_error_overloaded)
        }

        ApiError.InvalidRequest -> {
            stringResource(R.string.onboarding_error_invalid_request)
        }

        ApiError.Server -> {
            stringResource(R.string.onboarding_error_server)
        }

        ApiError.Unexpected -> {
            stringResource(R.string.onboarding_error_unexpected)
        }
    }

/**
 * The glyph the field's trailing slot shows for a failed check. Reaching Anthropic and being
 * turned away are different problems, and the icon is what says which one this was before the
 * sentence is read.
 */
internal fun ApiError.trailingGlyph(): String =
    when (this) {
        ApiError.Network, ApiError.Timeout -> Glyphs.CLOUD_OFF
        else -> Glyphs.ERROR
    }
