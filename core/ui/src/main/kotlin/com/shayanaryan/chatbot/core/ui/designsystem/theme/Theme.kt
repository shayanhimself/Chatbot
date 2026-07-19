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
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChatbotTypography,
            shapes = ChatbotM3Shapes,
            content = content,
        )
    }
}

/** Accessors for tokens which M3 has no slot for. Standard tokens come from [MaterialTheme]. */
object ChatbotTheme {
    val extendedColors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
