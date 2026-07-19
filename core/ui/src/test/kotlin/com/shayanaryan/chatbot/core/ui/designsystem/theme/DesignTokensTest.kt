package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignTokensTest {
    private val spacing = Spacing()
    private val shapes = ChatbotShapes()
    private val motion = Motion()

    @Test
    fun spacingFollowsFourDpGrid() {
        assertEquals(0.dp, spacing.none)
        assertEquals(4.dp, spacing.xxs)
        assertEquals(8.dp, spacing.xs)
        assertEquals(12.dp, spacing.sm)
        assertEquals(16.dp, spacing.md)
        assertEquals(20.dp, spacing.lg)
        assertEquals(24.dp, spacing.xl)
        assertEquals(32.dp, spacing.xxl)
        assertEquals(40.dp, spacing.x3l)
        assertEquals(48.dp, spacing.x4l)
        assertEquals(64.dp, spacing.x5l)
        assertEquals(16.dp, spacing.gutter)
        assertEquals(48.dp, spacing.minTouchTarget)
    }

    @Test
    fun componentShapesMatchSpec() {
        assertEquals(CircleShape, shapes.button)
        assertEquals(CircleShape, shapes.chip)
        assertEquals(RoundedCornerShape(12.dp), shapes.card)
        assertEquals(RoundedCornerShape(4.dp), shapes.input)
        assertEquals(RoundedCornerShape(28.dp), shapes.dialog)
        assertEquals(
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomEnd = 4.dp,
                bottomStart = 20.dp,
            ),
            shapes.bubbleUser,
        )
        assertEquals(
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 4.dp,
            ),
            shapes.bubbleAssistant,
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
        val elevation = Elevation()
        assertEquals(1.dp, elevation.level1)
        assertEquals(3.dp, elevation.level2)
        assertEquals(6.dp, elevation.level3)
        assertEquals(8.dp, elevation.level4)
        assertEquals(12.dp, elevation.level5)
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
