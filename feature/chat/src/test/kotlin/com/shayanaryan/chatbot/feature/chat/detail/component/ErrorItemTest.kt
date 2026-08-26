package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.quantityString
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.R
import com.shayanaryan.chatbot.shared.ApiError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import com.shayanaryan.chatbot.core.ui.R as CoreUiR

private const val RETRY_AFTER_SECONDS = 30
private const val ONE_SECOND = 1

@RunWith(AndroidJUnit4::class)
class ErrorItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val allErrors =
        listOf(
            ApiError.Authentication,
            ApiError.RateLimited(retryAfterSeconds = null),
            ApiError.RateLimited(retryAfterSeconds = RETRY_AFTER_SECONDS),
            ApiError.Overloaded,
            ApiError.InvalidRequest,
            ApiError.Server,
            ApiError.Network,
            ApiError.Timeout,
            ApiError.Unexpected,
        )

    @Test
    fun `no two failures resolve to the same string, and none to an empty one`() {
        val resolved = mutableListOf<String>()
        composeRule.setContent {
            ChatbotTheme {
                allErrors.forEach { resolved += it.text() }
            }
        }

        assertEquals(allErrors.size, resolved.distinct().size)
        assertTrue(resolved.none { it.isBlank() })
    }

    @Test
    fun `a rate limit with a retry hint names the wait`() {
        var text = ""
        composeRule.setContent {
            ChatbotTheme {
                text = ApiError.RateLimited(RETRY_AFTER_SECONDS).text()
            }
        }

        assertTrue(text.contains(RETRY_AFTER_SECONDS.toString()))
    }

    @Test
    fun `a one second wait reads as one second, not as one seconds`() {
        var text = ""
        composeRule.setContent {
            ChatbotTheme {
                text = ApiError.RateLimited(ONE_SECOND).text()
            }
        }

        assertEquals(
            quantityString(
                id = R.plurals.chat_error_rate_limited_after,
                quantity = ONE_SECOND,
                ONE_SECOND,
            ),
            text,
        )
        assertNotEquals(
            quantityString(
                id = R.plurals.chat_error_rate_limited_after,
                quantity = RETRY_AFTER_SECONDS,
                ONE_SECOND,
            ),
            text,
        )
    }

    @Test
    fun `retry reports a tap`() {
        var retried = false
        composeRule.setContent {
            ChatbotTheme {
                ErrorItem(error = ApiError.Network, onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertEquals(true, retried)
    }

    @Test
    fun `the network failure renders its sentence`() {
        composeRule.setContent {
            ChatbotTheme {
                ErrorItem(error = ApiError.Network, onRetry = {})
            }
        }

        composeRule
            .onNodeWithText(string(R.string.chat_error_network))
            .assertIsDisplayed()
    }
}
