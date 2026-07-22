package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale on the 4dp grid, mirroring the upstream `--space-N` tokens 1:1.
 * The suffix is the grid-step count: `sN` resolves to `(N * 4).dp` (`s0_5` = 2dp).
 */
object Spacing {
    val s0: Dp = 0.dp
    val s0_5: Dp = 2.dp
    val s1: Dp = 4.dp
    val s2: Dp = 8.dp
    val s3: Dp = 12.dp
    val s4: Dp = 16.dp
    val s5: Dp = 20.dp
    val s6: Dp = 24.dp
    val s8: Dp = 32.dp
    val s10: Dp = 40.dp
    val s12: Dp = 48.dp
    val s16: Dp = 64.dp
    val gutter: Dp = s4
}
