package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.RadiusPrimitives
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_PREVIEW_WIDTH_DP

/** A chat with no first message yet. */
@Composable
fun NewChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Spacing.s6),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DsIcon(
            glyph = Glyphs.BRAND,
            contentDescription = null,
            size = 30.dp,
            filled = true,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(RadiusPrimitives.radius5),
                    ),
        )
        Text(
            text = stringResource(R.string.conversation_new_chat_greeting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP, heightDp = 400)
@Composable
private fun NewChatEmptyStatePreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            NewChatEmptyState()
        }
    }
}
