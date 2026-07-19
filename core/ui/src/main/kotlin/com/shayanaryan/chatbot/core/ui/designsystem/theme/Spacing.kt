package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp-grid spacing scale. `gutter` is the default screen gutter; `minTouchTarget` the minimum touch size. */
@Immutable
class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val x3l: Dp = 40.dp,
    val x4l: Dp = 48.dp,
    val x5l: Dp = 64.dp,
    val gutter: Dp = 16.dp,
    val minTouchTarget: Dp = 48.dp,
)

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }
