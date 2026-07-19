package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun displayAndHeadlineAreRegularWeight() {
        assertEquals(57.sp, ChatbotTypography.displayLarge.fontSize)
        assertEquals(64.sp, ChatbotTypography.displayLarge.lineHeight)
        assertEquals(FontWeight.Normal, ChatbotTypography.displayLarge.fontWeight)
        assertEquals(32.sp, ChatbotTypography.headlineLarge.fontSize)
        assertEquals(40.sp, ChatbotTypography.headlineLarge.lineHeight)
        assertEquals(FontWeight.Normal, ChatbotTypography.headlineLarge.fontWeight)
    }

    @Test
    fun titleAndLabelAreMediumWeight() {
        assertEquals(22.sp, ChatbotTypography.titleLarge.fontSize)
        assertEquals(FontWeight.Medium, ChatbotTypography.titleLarge.fontWeight)
        assertEquals(16.sp, ChatbotTypography.titleMedium.fontSize)
        assertEquals(0.15.sp, ChatbotTypography.titleMedium.letterSpacing)
        assertEquals(14.sp, ChatbotTypography.labelLarge.fontSize)
        assertEquals(0.1.sp, ChatbotTypography.labelLarge.letterSpacing)
        assertEquals(FontWeight.Medium, ChatbotTypography.labelSmall.fontWeight)
        assertEquals(11.sp, ChatbotTypography.labelSmall.fontSize)
    }

    @Test
    fun bodyMetricsMatchSpec() {
        assertEquals(16.sp, ChatbotTypography.bodyLarge.fontSize)
        assertEquals(24.sp, ChatbotTypography.bodyLarge.lineHeight)
        assertEquals(0.5.sp, ChatbotTypography.bodyLarge.letterSpacing)
        assertEquals(14.sp, ChatbotTypography.bodyMedium.fontSize)
        assertEquals(0.25.sp, ChatbotTypography.bodyMedium.letterSpacing)
        assertEquals(12.sp, ChatbotTypography.bodySmall.fontSize)
        assertEquals(0.4.sp, ChatbotTypography.bodySmall.letterSpacing)
    }

    @Test
    fun monoStyleIsMonospaceFourteenSp() {
        assertEquals(FontFamily.Monospace, MonoTextStyle.fontFamily)
        assertEquals(14.sp, MonoTextStyle.fontSize)
        assertEquals(20.sp, MonoTextStyle.lineHeight)
    }
}
