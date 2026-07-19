package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun ChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalExtendedTypography provides DefaultExtendedTypography,
        LocalChatbotShapes provides ChatbotShapes(),
        LocalElevation provides Elevation(),
        LocalMotion provides Motion(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChatbotTypography,
            shapes = ChatbotM3Shapes,
            content = content,
        )
    }
}

/** Accessors for tokens M3 has no slot for. Standard tokens come from [MaterialTheme]. */
object ChatbotTheme {
    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
    val typography: ExtendedTypography
        @Composable @ReadOnlyComposable
        get() = LocalExtendedTypography.current
    val shapes: ChatbotShapes
        @Composable @ReadOnlyComposable
        get() = LocalChatbotShapes.current
    val elevation: Elevation
        @Composable @ReadOnlyComposable
        get() = LocalElevation.current
    val motion: Motion
        @Composable @ReadOnlyComposable
        get() = LocalMotion.current
}
