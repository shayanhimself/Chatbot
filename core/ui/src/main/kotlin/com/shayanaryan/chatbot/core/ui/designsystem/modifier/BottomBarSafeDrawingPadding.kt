package com.shayanaryan.chatbot.core.ui.designsystem.modifier

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps a bar pinned to the bottom of the window clear of everything the system draws over that
 * edge: the navigation bar, the keyboard and the camera hole in landscape.
 *
 * `safeDrawing` is the union of those three. Chaining `navigationBarsPadding()` and
 * `imePadding()` arrives at the same number for those two, but leaves out the cutout and spells
 * out by hand a union the framework already publishes.
 */
@Composable
fun Modifier.bottomBarSafeDrawingPadding(): Modifier =
    this.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    )
