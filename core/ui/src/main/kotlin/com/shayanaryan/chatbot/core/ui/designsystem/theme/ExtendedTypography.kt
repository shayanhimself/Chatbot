package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Text styles outside the M3 scale. `mono` is for API keys, model ids, and code. */
@Immutable
class ExtendedTypography(
    val mono: TextStyle,
)

internal val DefaultExtendedTypography =
    ExtendedTypography(
        mono =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
    )

internal val LocalExtendedTypography = staticCompositionLocalOf { DefaultExtendedTypography }
