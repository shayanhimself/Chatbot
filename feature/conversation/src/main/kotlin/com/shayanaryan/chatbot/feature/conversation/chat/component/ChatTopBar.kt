package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsIconButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.chat.CHAT_PREVIEW_WIDTH_DP

/**
 * The conversation's title and the only route to deleting it.
 *
 * @param title null for a chat with no first message yet, which reads as the new-chat copy.
 * @param deletable false for a chat with no first message, which has nothing to delete and so
 *   carries no overflow menu.
 * @param onBack null on a wide window, where the chat sits beside the list and there is nothing to
 *   go back to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String?,
    deletable: Boolean,
    onBack: (() -> Unit)?,
    onDeleteRequested: () -> Unit,
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            if (onBack != null) {
                DsIconButton(
                    glyph = Glyphs.ARROW_BACK,
                    contentDescription = stringResource(R.string.conversation_back),
                    onClick = onBack,
                )
            }
        },
        title = {
            Text(
                text = title ?: stringResource(R.string.conversation_new_chat_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            if (deletable) {
                DsIconButton(
                    glyph = Glyphs.MORE_VERT,
                    contentDescription = stringResource(R.string.conversation_more),
                    onClick = { overflowExpanded = true },
                )
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.conversation_delete),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onDeleteRequested()
                        },
                        leadingIcon = {
                            DsIcon(
                                glyph = Glyphs.DELETE,
                                contentDescription = null,
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ChatTopBarPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatTopBar(
                title = "Weekend trip to Portland",
                deletable = true,
                onBack = {},
                onDeleteRequested = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ChatTopBarNewChatPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatTopBar(
                title = null,
                deletable = false,
                onBack = {},
                onDeleteRequested = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = CHAT_PREVIEW_WIDTH_DP)
@Composable
private fun ChatTopBarDetailPanePreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ChatTopBar(
                title = "Weekend trip to Portland",
                deletable = true,
                onBack = null,
                onDeleteRequested = {},
            )
        }
    }
}
