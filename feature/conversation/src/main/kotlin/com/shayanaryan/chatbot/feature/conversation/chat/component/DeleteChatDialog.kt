package com.shayanaryan.chatbot.feature.conversation.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsDialog
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.R

/** Destructive and local-only, so it asks first and says why it matters. */
@Composable
fun DeleteChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DsDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.conversation_delete_title),
        confirmText = stringResource(R.string.conversation_delete_confirm),
        onConfirm = onConfirm,
        text = stringResource(R.string.conversation_delete_body),
        glyph = Glyphs.DELETE,
        dismissText = stringResource(R.string.conversation_delete_cancel),
        onDismiss = onDismiss,
    )
}
