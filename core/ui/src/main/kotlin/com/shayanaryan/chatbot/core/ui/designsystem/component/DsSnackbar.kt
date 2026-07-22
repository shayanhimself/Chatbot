package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.Snackbar as M3Snackbar

/**
 * Design-system snackbar. Drop it into an M3 `SnackbarHost`:
 */
@Composable
fun DsSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    M3Snackbar(snackbarData = snackbarData, modifier = modifier)
}

private val PreviewSnackbarData =
    object : SnackbarData {
        override val visuals =
            object : SnackbarVisuals {
                override val message = "Memory deleted"
                override val actionLabel = "Undo"
                override val withDismissAction = false
                override val duration = SnackbarDuration.Short
            }

        override fun performAction() {}

        override fun dismiss() {}
    }

@Preview
@Composable
private fun SnackbarPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            DsSnackbar(snackbarData = PreviewSnackbarData)
        }
    }
}
