package com.shayanaryan.chatbot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * The app's root composable. It owns the back stack, seeds it from the gate, and hands every
 * mutation to [ChatbotNavigator].
 *
 * @param hasApiKey whether a key is stored. Resolved before this composes, so the stack starts on
 *   the right destination instead of being rewritten onto it.
 * @param deepLinkChatId the chat an intent asked to open, null on a normal launch.
 * @param onDeepLinkHandled called once that id has been consumed, so the same intent cannot pull
 *   the user back to the chat after they navigate away. It fires whether the navigator
 *   acted on the id or dropped it for want of a key.
 */
@Composable
fun ChatbotApp(
    hasApiKey: Boolean,
    deepLinkChatId: Long?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keys are serializable, which is what carries this across process death.
    val backStack = rememberNavBackStack(ChatbotNavigator.startKeyFor(hasApiKey))

    // Remembered rather than saved: a restore rebuilds it against the flag as it stands, which is
    // what makes the restore report no transition and keep the user where they were.
    val navigator =
        remember(backStack) {
            ChatbotNavigator(
                backStack = backStack,
                hasApiKeyAtStart = hasApiKey,
            )
        }

    LaunchedEffect(hasApiKey) { navigator.resetForApiKeyState(hasApiKey) }

    LaunchedEffect(deepLinkChatId) {
        val id = deepLinkChatId ?: return@LaunchedEffect
        navigator.openChatFromDeepLink(id)
        onDeepLinkHandled()
    }

    ChatbotNavDisplay(
        backStack = backStack,
        navigator = navigator,
        modifier = modifier,
    )
}
