package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object ColorPrimitives {
    val Orange90 = Color(0xFFFFDCC2)
    val Orange57 = Color(0xFFFFA257)
    val Orange50 = Color(0xFFFF9239)
    val Orange40 = Color(0xFFED6900)
    val Orange30 = Color(0xFFB84E00)
    val Orange20 = Color(0xFF7A3300)
    val Orange12 = Color(0xFF431C00)
    val Orange10 = Color(0xFF401A00)

    val Yellow90 = Color(0xFFFFE494)
    val Yellow35 = Color(0xFF8A6600)
    val Yellow20 = Color(0xFF4A3600)
    val Yellow15 = Color(0xFF2A1E00)

    val Warm95 = Color(0xFFFFDCC2)
    val Warm85 = Color(0xFFE4C0A4)
    val Warm50 = Color(0xFF7A5733)
    val Warm30 = Color(0xFF5A4029)
    val Warm22 = Color(0xFF422A15)
    val Warm18 = Color(0xFF2A1800)

    val Navy06 = Color(0xFF12161F)
    val Navy10 = Color(0xFF1A2130)
    val Navy14 = Color(0xFF212A3B)
    val Navy17 = Color(0xFF263043)
    val Navy22 = Color(0xFF2E394D)
    val Navy26 = Color(0xFF354158)
    val Navy40 = Color(0xFF55617A)
    val Navy60 = Color(0xFF8791A6)
    val Navy80 = Color(0xFFC3CBD9)
    val Navy95 = Color(0xFFE7EFFE)

    val Sand100 = Color(0xFFFFFFFF)
    val Sand98 = Color(0xFFFBF9F7)
    val Sand96 = Color(0xFFF5F2EE)
    val Sand94 = Color(0xFFEFECE7)
    val Sand92 = Color(0xFFE9E5DF)
    val Sand90 = Color(0xFFE3DFD8)
    val Sand70 = Color(0xFFD5C8BA)
    val Sand52 = Color(0xFF837567)
    val Sand32 = Color(0xFF4F4539)
    val Sand20 = Color(0xFF313029)
    val Sand11 = Color(0xFF1C1B19)

    val Green50 = Color(0xFF4ECB7B)
    val Green44 = Color(0xFF1F7A44)
    val Green88 = Color(0xFFB7F0C8)
    val Green20 = Color(0xFF0F4D2A)
    val Green08 = Color(0xFF00210F)

    val Red70 = Color(0xFFFFB3B3)
    val Red50 = Color(0xFFFF6B6B)
    val Red44 = Color(0xFFBA1A1A)
    val Red90 = Color(0xFFFFDAD6)
    val Red20 = Color(0xFF5C1A1A)
    val Red12 = Color(0xFF430E0E)
    val Red05 = Color(0xFF410002)

    val Amber50 = Color(0xFFFFCE54)
    val Amber35 = Color(0xFF8A5A00)

    val White = Color(0xFFFFFFFF)
    val ScrimDark = Color(0xB806090E)
    val ScrimLight = Color(0x661C1B19)
}

internal val DarkColorScheme =
    darkColorScheme(
        primary = ColorPrimitives.Orange50,
        onPrimary = ColorPrimitives.Orange12,
        primaryContainer = ColorPrimitives.Orange20,
        onPrimaryContainer = ColorPrimitives.Orange90,
        secondary = ColorPrimitives.Warm85,
        onSecondary = ColorPrimitives.Warm22,
        secondaryContainer = ColorPrimitives.Warm30,
        onSecondaryContainer = ColorPrimitives.Warm95,
        tertiary = ColorPrimitives.Yellow90,
        onTertiary = ColorPrimitives.Yellow20,
        tertiaryContainer = ColorPrimitives.Yellow20,
        onTertiaryContainer = ColorPrimitives.Yellow90,
        background = ColorPrimitives.Navy10,
        onBackground = ColorPrimitives.Navy95,
        surface = ColorPrimitives.Navy10,
        onSurface = ColorPrimitives.Navy95,
        surfaceContainerLowest = ColorPrimitives.Navy06,
        surfaceContainerLow = ColorPrimitives.Navy14,
        surfaceContainer = ColorPrimitives.Navy17,
        surfaceContainerHigh = ColorPrimitives.Navy22,
        surfaceContainerHighest = ColorPrimitives.Navy26,
        onSurfaceVariant = ColorPrimitives.Navy80,
        outline = ColorPrimitives.Navy40,
        outlineVariant = ColorPrimitives.Navy22,
        error = ColorPrimitives.Red50,
        onError = ColorPrimitives.Red12,
        errorContainer = ColorPrimitives.Red20,
        onErrorContainer = ColorPrimitives.Red70,
        scrim = ColorPrimitives.ScrimDark,
        inverseSurface = ColorPrimitives.Navy95,
        inverseOnSurface = ColorPrimitives.Navy14,
    )

internal val LightColorScheme =
    lightColorScheme(
        primary = ColorPrimitives.Orange40,
        onPrimary = ColorPrimitives.White,
        primaryContainer = ColorPrimitives.Orange90,
        onPrimaryContainer = ColorPrimitives.Orange10,
        secondary = ColorPrimitives.Warm50,
        onSecondary = ColorPrimitives.White,
        secondaryContainer = ColorPrimitives.Warm95,
        onSecondaryContainer = ColorPrimitives.Warm18,
        tertiary = ColorPrimitives.Yellow35,
        onTertiary = ColorPrimitives.White,
        tertiaryContainer = ColorPrimitives.Yellow90,
        onTertiaryContainer = ColorPrimitives.Yellow15,
        background = ColorPrimitives.Sand98,
        onBackground = ColorPrimitives.Sand11,
        surface = ColorPrimitives.Sand98,
        onSurface = ColorPrimitives.Sand11,
        surfaceContainerLowest = ColorPrimitives.Sand100,
        surfaceContainerLow = ColorPrimitives.Sand96,
        surfaceContainer = ColorPrimitives.Sand94,
        surfaceContainerHigh = ColorPrimitives.Sand92,
        surfaceContainerHighest = ColorPrimitives.Sand90,
        onSurfaceVariant = ColorPrimitives.Sand32,
        outline = ColorPrimitives.Sand52,
        outlineVariant = ColorPrimitives.Sand70,
        error = ColorPrimitives.Red44,
        onError = ColorPrimitives.White,
        errorContainer = ColorPrimitives.Red90,
        onErrorContainer = ColorPrimitives.Red05,
        scrim = ColorPrimitives.ScrimLight,
        inverseSurface = ColorPrimitives.Sand20,
        inverseOnSurface = ColorPrimitives.Sand96,
    )
