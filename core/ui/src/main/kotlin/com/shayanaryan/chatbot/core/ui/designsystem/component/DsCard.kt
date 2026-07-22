package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.ElevatedCard as M3ElevatedCard
import androidx.compose.material3.OutlinedCard as M3OutlinedCard

enum class CardVariant { Filled, Outlined, Elevated }

/**
 * Design-system card wrapping the three M3 card variants.
 *
 * @param variant one of filled (default) / outlined / elevated.
 * @param onClick invoked on click; non-null turns the card into a clickable surface.
 * @param content card body, laid out in a [ColumnScope].
 */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Filled,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = ComponentShapes.card
    when (variant) {
        CardVariant.Filled -> {
            if (onClick != null) {
                M3Card(onClick = onClick, modifier = modifier, shape = shape, content = content)
            } else {
                M3Card(modifier = modifier, shape = shape, content = content)
            }
        }

        CardVariant.Outlined -> {
            if (onClick != null) {
                M3OutlinedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content,
                )
            } else {
                M3OutlinedCard(modifier = modifier, shape = shape, content = content)
            }
        }

        CardVariant.Elevated -> {
            if (onClick != null) {
                M3ElevatedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    content = content,
                )
            } else {
                M3ElevatedCard(modifier = modifier, shape = shape, content = content)
            }
        }
    }
}

@Preview
@Composable
private fun CardPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                CardVariant.entries.forEach { variant ->
                    DsCard(variant = variant) {
                        Text(variant.name, Modifier.padding(Spacing.s4))
                    }
                }
            }
        }
    }
}
