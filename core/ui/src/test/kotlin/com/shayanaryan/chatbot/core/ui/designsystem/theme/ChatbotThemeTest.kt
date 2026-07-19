package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        var monoSize = 0.sp
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                success = ChatbotTheme.extendedColors.success
                duration = ChatbotTheme.motion.durationMediumMillis
                monoSize = ChatbotTheme.typography.mono.fontSize
            }
        }
        assertEquals(ColorPrimitives.Orange50, primary)
        assertEquals(ColorPrimitives.Green50, success)
        assertEquals(250, duration)
        assertEquals(14.sp, monoSize)
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
