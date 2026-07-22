package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tier 1 — primitives. The raw corner-radius ramp, mirrored 1:1 from the upstream DS tokens
 * (numeric scale, N = px/4 = dp). Every shape below is built from it.
 * This ramp is public, so a component that can't be composed using M3 shapes or ComponentShapes
 * (e.g. the 20dp logo tiles), can build its own `RoundedCornerShape` from the matching primitive.
 */
object RadiusPrimitives {
    val radius1: Dp = 4.dp
    val radius2: Dp = 8.dp
    val radius3: Dp = 12.dp
    val radius4: Dp = 16.dp
    val radius5: Dp = 20.dp
    val radius7: Dp = 28.dp
}

/**
 * Tier 2 — the M3 [Shapes] ramp installed into `MaterialTheme`.
 * It pins the five public M3 slots to our DS radii, so every built-in M3 component that reads
 * its default corner from the theme (`Card`, `TextField`, `AlertDialog`, …) draws our values
 * rather than the library defaults. Read via `MaterialTheme.shapes.*`.
 */
internal val ChatbotM3Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(RadiusPrimitives.radius1),
        small = RoundedCornerShape(RadiusPrimitives.radius2),
        medium = RoundedCornerShape(RadiusPrimitives.radius3),
        large = RoundedCornerShape(RadiusPrimitives.radius4),
        extraLarge = RoundedCornerShape(RadiusPrimitives.radius7),
    )

/**
 * Named component shapes, read directly at call sites.
 * Exists for the shapes [ChatbotM3Shapes] can't carry: `button` and the chat `bubble` corners have
 * no public M3 slot, so they can only live here.
 * The remaining roles map to a primitive radius, that way each component's slot assignment is pinned,
 * so it can't drift from what the built-in M3 component would otherwise pick.
 */
object ComponentShapes {
    val button: Shape = CircleShape
    val input: Shape = RoundedCornerShape(RadiusPrimitives.radius1)
    val chip: Shape = RoundedCornerShape(RadiusPrimitives.radius2)
    val card: Shape = RoundedCornerShape(RadiusPrimitives.radius3)
    val dialog: Shape = RoundedCornerShape(RadiusPrimitives.radius7)
    val bubbleUser: Shape =
        RoundedCornerShape(
            topStart = RadiusPrimitives.radius5,
            topEnd = RadiusPrimitives.radius5,
            bottomEnd = RadiusPrimitives.radius1,
            bottomStart = RadiusPrimitives.radius5,
        )
    val bubbleAssistant: Shape =
        RoundedCornerShape(
            topStart = RadiusPrimitives.radius5,
            topEnd = RadiusPrimitives.radius5,
            bottomEnd = RadiusPrimitives.radius5,
            bottomStart = RadiusPrimitives.radius1,
        )
}
