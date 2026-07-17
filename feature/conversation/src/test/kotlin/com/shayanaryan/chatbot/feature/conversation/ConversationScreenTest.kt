package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsConversationPlaceholder() {
        composeRule.setContent {
            ConversationScreen()
        }
        composeRule.onNodeWithText("Conversation").assertIsDisplayed()
    }
}
