package com.shayanaryan.chatbot.feature.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.feature.onboarding.R
import com.shayanaryan.chatbot.shared.chat.ChatError

/** The sentence shown under the field for a failed check. */
@Composable
internal fun ChatError.supportingText(): String =
    when (this) {
        ChatError.Authentication -> {
            stringResource(R.string.onboarding_error_authentication)
        }

        ChatError.Network -> {
            stringResource(R.string.onboarding_error_network)
        }

        ChatError.Timeout -> {
            stringResource(R.string.onboarding_error_timeout)
        }

        is ChatError.RateLimited -> {
            retryAfterSeconds?.let {
                stringResource(R.string.onboarding_error_rate_limited_after, it)
            } ?: stringResource(R.string.onboarding_error_rate_limited)
        }

        ChatError.Overloaded -> {
            stringResource(R.string.onboarding_error_overloaded)
        }

        ChatError.InvalidRequest -> {
            stringResource(R.string.onboarding_error_invalid_request)
        }

        ChatError.Server -> {
            stringResource(R.string.onboarding_error_server)
        }

        ChatError.Unexpected -> {
            stringResource(R.string.onboarding_error_unexpected)
        }
    }

/**
 * The glyph the field's trailing slot shows for a failed check. Reaching Anthropic and being
 * turned away are different problems, and the icon is what says which one this was before the
 * sentence is read.
 */
internal fun ChatError.trailingGlyph(): String =
    when (this) {
        ChatError.Network, ChatError.Timeout -> Glyphs.CLOUD_OFF
        else -> Glyphs.ERROR
    }
