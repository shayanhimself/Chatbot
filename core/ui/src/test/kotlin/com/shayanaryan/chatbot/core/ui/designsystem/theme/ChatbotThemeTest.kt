package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatbotThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkIsDefaultAndInstallsAllTokens() {
        var primary = Color.Unspecified
        var success = Color.Unspecified
        var duration = 0
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                success = ChatbotTheme.extendedColors.success
                duration = ChatbotTheme.motion.durationMediumMillis
            }
        }
        assertEquals(ColorPrimitives.Orange50, primary)
        assertEquals(ColorPrimitives.Green50, success)
        assertEquals(250, duration)
    }

    @Test
    fun lightThemeSwitchesSchemesAndExtendedColors() {
        var primary = Color.Unspecified
        var success = Color.Unspecified
        composeRule.setContent {
            ChatbotTheme(darkTheme = false) {
                primary = MaterialTheme.colorScheme.primary
                success = ChatbotTheme.extendedColors.success
            }
        }
        assertEquals(ColorPrimitives.Orange40, primary)
        assertEquals(ColorPrimitives.Green44, success)
    }
}
