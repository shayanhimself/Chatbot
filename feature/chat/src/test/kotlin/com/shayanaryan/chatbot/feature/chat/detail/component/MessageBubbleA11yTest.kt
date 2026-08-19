package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.shared.Role
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

private const val STREAMING_REPLY = "Two nights? I would start with Powell's Books"
private const val SETTLED_REPLY = "Portland it is."

@RunWith(AndroidJUnit4::class)
class MessageBubbleA11yTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a streaming reply is announced politely`() {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                MessageBubble(text = STREAMING_REPLY, role = Role.Assistant, streaming = true)
            }
        }

        val node = composeRule.onNodeWithText(STREAMING_REPLY).fetchSemanticsNode()

        assertEquals(
            LiveRegionMode.Polite,
            node.config.getOrNull(SemanticsProperties.LiveRegion),
        )
    }

    @Test
    fun `a settled reply is not announced`() {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                MessageBubble(text = SETTLED_REPLY, role = Role.Assistant)
            }
        }

        val node = composeRule.onNodeWithText(SETTLED_REPLY).fetchSemanticsNode()

        assertEquals(null, node.config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun `a user turn is never announced`() {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                Column {
                    MessageBubble(text = SETTLED_REPLY, role = Role.User)
                }
            }
        }

        val node = composeRule.onNodeWithText(SETTLED_REPLY).fetchSemanticsNode()

        assertEquals(null, node.config.getOrNull(SemanticsProperties.LiveRegion))
    }
}
