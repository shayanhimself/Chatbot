package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.shared.chat.ChatError

/**
 * Maps ChatError to a localized string
 */
@Composable
fun ChatError.text(): String =
    when (this) {
        ChatError.Authentication -> {
            stringResource(R.string.conversation_error_authentication)
        }

        is ChatError.RateLimited -> {
            // A property from another module cannot smart cast, so the hint is read once here.
            val seconds = retryAfterSeconds
            if (seconds == null) {
                stringResource(R.string.conversation_error_rate_limited)
            } else {
                stringResource(R.string.conversation_error_rate_limited_after, seconds)
            }
        }

        ChatError.Overloaded -> {
            stringResource(R.string.conversation_error_overloaded)
        }

        ChatError.InvalidRequest -> {
            stringResource(R.string.conversation_error_invalid_request)
        }

        ChatError.Server -> {
            stringResource(R.string.conversation_error_server)
        }

        ChatError.Network -> {
            stringResource(R.string.conversation_error_network)
        }

        ChatError.Timeout -> {
            stringResource(R.string.conversation_error_timeout)
        }

        ChatError.Unexpected -> {
            stringResource(R.string.conversation_error_unexpected)
        }
    }
