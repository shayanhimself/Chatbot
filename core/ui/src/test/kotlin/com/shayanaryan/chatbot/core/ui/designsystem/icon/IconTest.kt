package com.shayanaryan.chatbot.core.ui.designsystem.icon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun iconExposesContentDescriptionNotLigatureText() {
        composeRule.setContent {
            ChatbotTheme { Icon(glyph = Glyphs.CLOSE, contentDescription = "Close") }
        }
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        // Ligature text must not leak into semantics — TalkBack would read the raw glyph name.
        composeRule.onNodeWithText(Glyphs.CLOSE).assertDoesNotExist()
    }
}
