package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors M3 has no slot for. */
@Immutable
class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
)

internal val DarkExtendedColors =
    ExtendedColors(
        success = ColorPrimitives.Green50,
        onSuccess = ColorPrimitives.Green08,
        successContainer = ColorPrimitives.Green20,
        warning = ColorPrimitives.Amber50,
        primaryHover = ColorPrimitives.Orange57,
        primaryPressed = ColorPrimitives.Orange40,
    )

internal val LightExtendedColors =
    ExtendedColors(
        success = ColorPrimitives.Green44,
        onSuccess = ColorPrimitives.White,
        successContainer = ColorPrimitives.Green88,
        warning = ColorPrimitives.Amber35,
        primaryHover = ColorPrimitives.Orange30,
        primaryPressed = ColorPrimitives.Orange20,
    )

internal val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
