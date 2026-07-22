package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import androidx.compose.material3.Switch as M3Switch

/**
 * Design-system toggle switch — a thin wrapper over the M3 [M3Switch] with our theme colors.
 *
 * @param checked whether the switch is on.
 * @param onCheckedChange invoked with the new state on toggle.
 */
@Composable
fun DsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    M3Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Preview
@Composable
private fun SwitchPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            Row(
                modifier = Modifier.padding(Spacing.s4),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                DsSwitch(
                    checked = true,
                    onCheckedChange = {},
                )
                DsSwitch(checked = false, onCheckedChange = {})
            }
        }
    }
}
