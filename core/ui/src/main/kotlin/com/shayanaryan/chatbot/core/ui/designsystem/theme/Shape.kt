package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

internal val ChatbotM3Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/**
 * Named component shapes. Bubble tail (4dp squared corner) sits bottom-end for user, bottom-start
 * for assistant.
 */
object ChatbotShapes {
    val button: Shape = CircleShape
    val chip: Shape = CircleShape
    val card: Shape = ChatbotM3Shapes.medium
    val input: Shape = ChatbotM3Shapes.extraSmall
    val dialog: Shape = ChatbotM3Shapes.extraLarge
    val bubbleUser: Shape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
    val bubbleAssistant: Shape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
}
