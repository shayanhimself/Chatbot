package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object Motion {
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easingEmphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val easingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val easingAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val durationShortMillis: Int = 150
    val durationMediumMillis: Int = 250
    val durationLongMillis: Int = 400
    val pressScaleButton: Float = 0.97f
    val pressScaleIconButton: Float = 0.90f
    val stateLayerHover: Float = 0.08f
    val stateLayerFocus: Float = 0.10f
    val stateLayerPressed: Float = 0.12f
    val caretBlinkMillis: Int = 1000
}
