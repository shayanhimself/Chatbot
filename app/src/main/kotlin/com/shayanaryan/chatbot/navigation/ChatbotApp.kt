package com.shayanaryan.chatbot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * The app's root composable. It owns the back stack, starts it on the conversation list, and hands
 * rendering to [ChatbotNavDisplay].
 *
 * @param deepLinkConversationId the conversation an intent asked to open, null on a normal launch.
 * @param onDeepLinkHandled called once that id has been applied, so the same intent cannot pull the
 *   user back to the conversation after they navigate away.
 */
@Composable
fun ChatbotApp(
    deepLinkConversationId: Long?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keys are serializable, which is what carries this across process death.
    val backStack = rememberNavBackStack(ConversationListKey)

    LaunchedEffect(deepLinkConversationId) {
        val id = deepLinkConversationId ?: return@LaunchedEffect
        backStack.clear()
        backStack.add(ConversationListKey)
        backStack.add(ChatKey(conversationId = id))
        onDeepLinkHandled()
    }

    ChatbotNavDisplay(backStack = backStack, modifier = modifier)
}
