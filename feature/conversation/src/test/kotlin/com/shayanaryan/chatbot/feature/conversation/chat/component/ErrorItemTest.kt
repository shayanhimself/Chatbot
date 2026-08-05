package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.shared.chat.ChatError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val RETRY_AFTER_SECONDS = 30

@RunWith(AndroidJUnit4::class)
class ErrorItemTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val allErrors =
        listOf(
            ChatError.Authentication,
            ChatError.RateLimited(retryAfterSeconds = null),
            ChatError.RateLimited(retryAfterSeconds = RETRY_AFTER_SECONDS),
            ChatError.Overloaded,
            ChatError.InvalidRequest,
            ChatError.Server,
            ChatError.Network,
            ChatError.Timeout,
            ChatError.Unexpected,
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
                text = ChatError.RateLimited(RETRY_AFTER_SECONDS).text()
            }
        }

        assertTrue(text.contains(RETRY_AFTER_SECONDS.toString()))
    }

    @Test
    fun `retry reports a tap`() {
        var retried = false
        composeRule.setContent {
            ChatbotTheme {
                ErrorItem(error = ChatError.Network, onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    @Test
    fun `the network failure renders its sentence`() {
        composeRule.setContent {
            ChatbotTheme {
                ErrorItem(error = ChatError.Network, onRetry = {})
            }
        }

        composeRule
            .onNodeWithText("Couldn't reach the API. Check your connection and try again.")
            .assertIsDisplayed()
    }
}
