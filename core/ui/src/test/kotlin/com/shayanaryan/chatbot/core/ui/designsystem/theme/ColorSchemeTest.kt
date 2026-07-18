package com.shayanaryan.chatbot.core.ui.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSchemeTest {
    @Test
    fun darkSchemePrimaryRolesResolveToOrangePrimitives() {
        assertEquals(ColorPrimitives.Orange50, DarkColorScheme.primary)
        assertEquals(ColorPrimitives.Orange12, DarkColorScheme.onPrimary)
        assertEquals(ColorPrimitives.Orange20, DarkColorScheme.primaryContainer)
        assertEquals(ColorPrimitives.Orange90, DarkColorScheme.onPrimaryContainer)
    }

    @Test
    fun darkSchemeSurfaceStackIsNavy() {
        assertEquals(ColorPrimitives.Navy10, DarkColorScheme.background)
        assertEquals(ColorPrimitives.Navy10, DarkColorScheme.surface)
        assertEquals(ColorPrimitives.Navy06, DarkColorScheme.surfaceContainerLowest)
        assertEquals(ColorPrimitives.Navy14, DarkColorScheme.surfaceContainerLow)
        assertEquals(ColorPrimitives.Navy17, DarkColorScheme.surfaceContainer)
        assertEquals(ColorPrimitives.Navy22, DarkColorScheme.surfaceContainerHigh)
        assertEquals(ColorPrimitives.Navy26, DarkColorScheme.surfaceContainerHighest)
        assertEquals(ColorPrimitives.Navy95, DarkColorScheme.onSurface)
        assertEquals(ColorPrimitives.Navy80, DarkColorScheme.onSurfaceVariant)
        assertEquals(ColorPrimitives.Navy40, DarkColorScheme.outline)
        assertEquals(ColorPrimitives.Navy22, DarkColorScheme.outlineVariant)
    }

    @Test
    fun darkSchemeErrorInverseAndScrim() {
        assertEquals(ColorPrimitives.Red50, DarkColorScheme.error)
        assertEquals(ColorPrimitives.Red12, DarkColorScheme.onError)
        assertEquals(ColorPrimitives.Red20, DarkColorScheme.errorContainer)
        assertEquals(ColorPrimitives.Red70, DarkColorScheme.onErrorContainer)
        assertEquals(ColorPrimitives.ScrimDark, DarkColorScheme.scrim)
        assertEquals(ColorPrimitives.Navy95, DarkColorScheme.inverseSurface)
        assertEquals(ColorPrimitives.Navy14, DarkColorScheme.inverseOnSurface)
        assertEquals(ColorPrimitives.Warm85, DarkColorScheme.secondary)
        assertEquals(ColorPrimitives.Yellow90, DarkColorScheme.tertiary)
    }

    @Test
    fun lightSchemeDarkensHuesForLegibility() {
        assertEquals(ColorPrimitives.Orange40, LightColorScheme.primary)
        assertEquals(ColorPrimitives.White, LightColorScheme.onPrimary)
        assertEquals(ColorPrimitives.Orange90, LightColorScheme.primaryContainer)
        assertEquals(ColorPrimitives.Orange10, LightColorScheme.onPrimaryContainer)
        assertEquals(ColorPrimitives.Yellow35, LightColorScheme.tertiary)
        assertEquals(ColorPrimitives.Red44, LightColorScheme.error)
        assertEquals(ColorPrimitives.Sand98, LightColorScheme.background)
        assertEquals(ColorPrimitives.Sand11, LightColorScheme.onSurface)
        assertEquals(ColorPrimitives.Sand52, LightColorScheme.outline)
        assertEquals(ColorPrimitives.ScrimLight, LightColorScheme.scrim)
        assertEquals(ColorPrimitives.Sand20, LightColorScheme.inverseSurface)
    }
}
