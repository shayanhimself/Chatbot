package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shayanaryan.chatbot.feature.chat.R
import com.shayanaryan.chatbot.shared.ApiError

/**
 * Maps ApiError to a localized string
 */
@Composable
fun ApiError.text(): String =
    when (this) {
        ApiError.Authentication -> {
            stringResource(R.string.chat_error_authentication)
        }

        is ApiError.RateLimited -> {
            // A property from another module cannot smart cast, so the hint is read once here.
            val seconds = retryAfterSeconds
            if (seconds == null) {
                stringResource(R.string.chat_error_rate_limited)
            } else {
                stringResource(R.string.chat_error_rate_limited_after, seconds)
            }
        }

        ApiError.Overloaded -> {
            stringResource(R.string.chat_error_overloaded)
        }

        ApiError.InvalidRequest -> {
            stringResource(R.string.chat_error_invalid_request)
        }

        ApiError.Server -> {
            stringResource(R.string.chat_error_server)
        }

        ApiError.Network -> {
            stringResource(R.string.chat_error_network)
        }

        ApiError.Timeout -> {
            stringResource(R.string.chat_error_timeout)
        }

        ApiError.Unexpected -> {
            stringResource(R.string.chat_error_unexpected)
        }
    }
