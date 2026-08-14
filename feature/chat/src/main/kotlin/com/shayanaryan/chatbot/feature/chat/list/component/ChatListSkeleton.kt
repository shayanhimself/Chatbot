package com.shayanaryan.chatbot.feature.chat.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

private const val LAST_SKELETON_ITEM_ALPHA = 0.6f

@Composable
fun ChatListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        skeletonItems.forEachIndexed { index, widths ->
            SkeletonItem(
                widths = widths,
                modifier =
                    Modifier.alpha(
                        if (index == skeletonItems.lastIndex) LAST_SKELETON_ITEM_ALPHA else 1f,
                    ),
            )
        }
    }
}

@Composable
private fun SkeletonItem(
    widths: SkeletonWidths,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(widths.title)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.shapes.small,
                    ),
            )
            Box(
                Modifier
                    .fillMaxWidth(widths.snippet)
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.shapes.small,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatListSkeletonPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface { ChatListSkeleton() }
    }
}

/**
 * One entry per placeholder item, top to bottom. Widths differ because real titles and replies do;
 * identical items would read as a table.
 */
private val skeletonItems =
    listOf(
        SkeletonWidths(title = 0.62f, snippet = 0.88f),
        SkeletonWidths(title = 0.48f, snippet = 0.74f),
        SkeletonWidths(title = 0.70f, snippet = 0.56f),
        SkeletonWidths(title = 0.54f, snippet = 0.80f),
    )

/**
 * How wide a placeholder item's two bars are, each a fraction of the item's width. The bars stand
 * in for the two lines [ChatListItem] draws.
 *
 * @property title the top bar, where the title goes.
 * @property snippet the bottom bar, where the snippet goes.
 */
private data class SkeletonWidths(
    val title: Float,
    val snippet: Float,
)
