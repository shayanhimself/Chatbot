package com.shayanaryan.chatbot.feature.chat.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.chat.R
import com.shayanaryan.chatbot.feature.chat.list.RelativeTime

/**
 * One chat in the list: title and age on the first line, the last complete reply on the
 * second.
 *
 * @param snippet null for a chat whose first turn has not finished, which leaves the
 *   second line empty rather than reserving space for nothing.
 * @param selected the open chat on a wide window. A narrow window never shows the list
 *   beside a chat, so it always passes false.
 */
@Composable
fun ChatListItem(
    title: String,
    snippet: String?,
    relativeTime: RelativeTime,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    val background =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(background)
                .clickable(onClick = onClick)
                .padding(Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(relativeTime.unitRes, relativeTime.value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (snippet != null) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatListItemPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatListItem(
                title = "Weekend trip to Portland",
                snippet = "Booked — I'll remind you to check in Friday.",
                relativeTime = RelativeTime(R.string.chat_time_hours, 2),
                selected = false,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatListItemSelectedPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatListItem(
                title = "Miso glaze recipe",
                snippet = null,
                relativeTime = RelativeTime(R.string.chat_time_days, 1),
                selected = true,
                onClick = {},
            )
        }
    }
}
