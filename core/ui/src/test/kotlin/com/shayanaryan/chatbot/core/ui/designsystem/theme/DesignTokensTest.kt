package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignTokensTest {
    private val motion = Motion()

    @Test
    fun spacingFollowsFourDpGrid() {
        assertEquals(0.dp, Spacing.none)
        assertEquals(4.dp, Spacing.xxs)
        assertEquals(8.dp, Spacing.xs)
        assertEquals(12.dp, Spacing.sm)
        assertEquals(16.dp, Spacing.md)
        assertEquals(20.dp, Spacing.lg)
        assertEquals(24.dp, Spacing.xl)
        assertEquals(32.dp, Spacing.xxl)
        assertEquals(40.dp, Spacing.x3l)
        assertEquals(48.dp, Spacing.x4l)
        assertEquals(64.dp, Spacing.x5l)
        assertEquals(16.dp, Spacing.gutter)
    }

    @Test
    fun componentShapesMatchSpec() {
        assertEquals(CircleShape, ChatbotShapes.button)
        assertEquals(CircleShape, ChatbotShapes.chip)
        assertEquals(RoundedCornerShape(12.dp), ChatbotShapes.card)
        assertEquals(RoundedCornerShape(4.dp), ChatbotShapes.input)
        assertEquals(RoundedCornerShape(28.dp), ChatbotShapes.dialog)
        assertEquals(
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomEnd = 4.dp,
                bottomStart = 20.dp,
            ),
            ChatbotShapes.bubbleUser,
        )
        assertEquals(
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 4.dp,
            ),
            ChatbotShapes.bubbleAssistant,
        )
    }

    @Test
    fun motionDurationsScalesAndStateLayers() {
        assertEquals(150, motion.durationShortMillis)
        assertEquals(250, motion.durationMediumMillis)
        assertEquals(400, motion.durationLongMillis)
        assertEquals(0.97f, motion.pressScaleButton, 0f)
        assertEquals(0.90f, motion.pressScaleIconButton, 0f)
        assertEquals(0.08f, motion.stateLayerHover, 0f)
        assertEquals(0.10f, motion.stateLayerFocus, 0f)
        assertEquals(0.12f, motion.stateLayerPressed, 0f)
        assertEquals(1000, motion.caretBlinkMillis)
    }

    @Test
    fun elevationLevelsAreM3Dps() {
        assertEquals(1.dp, Elevation.level1)
        assertEquals(3.dp, Elevation.level2)
        assertEquals(6.dp, Elevation.level3)
        assertEquals(8.dp, Elevation.level4)
        assertEquals(12.dp, Elevation.level5)
    }

    @Test
    fun extendedColorsResolvePerScheme() {
        assertEquals(ColorPrimitives.Green50, DarkExtendedColors.success)
        assertEquals(ColorPrimitives.Green08, DarkExtendedColors.onSuccess)
        assertEquals(ColorPrimitives.Green20, DarkExtendedColors.successContainer)
        assertEquals(ColorPrimitives.Amber50, DarkExtendedColors.warning)
        assertEquals(ColorPrimitives.Orange57, DarkExtendedColors.primaryHover)
        assertEquals(ColorPrimitives.Orange40, DarkExtendedColors.primaryPressed)
        assertEquals(ColorPrimitives.Green44, LightExtendedColors.success)
        assertEquals(ColorPrimitives.White, LightExtendedColors.onSuccess)
        assertEquals(ColorPrimitives.Green88, LightExtendedColors.successContainer)
        assertEquals(ColorPrimitives.Amber35, LightExtendedColors.warning)
        assertEquals(ColorPrimitives.Orange30, LightExtendedColors.primaryHover)
        assertEquals(ColorPrimitives.Orange20, LightExtendedColors.primaryPressed)
    }
}
