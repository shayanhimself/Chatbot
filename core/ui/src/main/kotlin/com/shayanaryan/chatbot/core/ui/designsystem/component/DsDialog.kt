package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import androidx.compose.material3.AlertDialog as M3AlertDialog

/**
 * Design-system alert dialog, wrapping M3 `AlertDialog`.
 *
 * @param onDismissRequest invoked on scrim tap / back press.
 * @param title dialog headline; caller-supplied copy.
 * @param confirmText primary action label; caller-supplied copy.
 * @param onConfirm invoked when the confirm button is tapped.
 * @param text optional supporting body; caller-supplied copy.
 * @param glyph optional leading [Glyphs] ligature.
 * @param dismissText optional secondary action label; when non-null shows the dismiss button.
 * @param onDismiss invoked on dismiss-button tap; defaults to [onDismissRequest].
 */
@Composable
fun DsDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    glyph: String? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    M3AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = ComponentShapes.dialog,
        icon =
            glyph?.let {
                {
                    DsIcon(
                        it,
                        contentDescription = null,
                        size = 24.dp,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = {
            DsButton(
                text = confirmText,
                onClick = onConfirm,
                variant = ButtonVariant.Text,
            )
        },
        dismissButton =
            if (dismissText != null) {
                {
                    DsButton(
                        text = dismissText,
                        onClick = onDismiss ?: onDismissRequest,
                        variant = ButtonVariant.Text,
                    )
                }
            } else {
                null
            },
    )
}
