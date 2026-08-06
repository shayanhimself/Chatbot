package com.shayanaryan.chatbot.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.shayanaryan.chatbot.feature.conversation.chat.ChatRoute
import com.shayanaryan.chatbot.feature.conversation.chat.component.NewChatEmptyState
import com.shayanaryan.chatbot.feature.conversation.conversationlist.ConversationListRoute

/**
 * Maps every key to its route and lets the adaptive scene arrange them.
 *
 * @param backStack owned by the caller, since a launch intent has to be able to replace it
 *   wholesale.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatbotNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    // The open conversation, reported up by the chat route because the key deliberately never
    // learns it. The ViewModel's SavedStateHandle is the durable store,
    // and the lambda fires again on the first composition after a restore.
    var selectedConversationId by remember { mutableStateOf<Long?>(null) }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive =
        remember(windowAdaptiveInfo) {
            // Override the default so there is no horizontal gap between the panes.
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val twoPane = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val openChat: (ChatKey) -> Unit = { key ->
        // Replace rather than stack: the list is always the entry below a chat, and a different
        // chat should get a different ViewModel.
        if (backStack.lastOrNull() is ChatKey) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    val popChat: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // The ViewModel store decorator is what scopes a ViewModel to its entry, so a different
                // ChatKey gets a different ChatViewModel.
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<ConversationListKey>(
                    metadata =
                        ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Surface(Modifier.fillMaxSize()) { NewChatEmptyState() }
                            },
                        ),
                ) {
                    ConversationListRoute(
                        // A narrow window never shows the list beside a chat, so nothing is selected.
                        selectedConversationId = if (twoPane) selectedConversationId else null,
                        onConversationClick = { id -> openChat(ChatKey(id)) },
                        onNewChat = { openChat(ChatKey()) },
                    )
                }
                entry<ChatKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ChatRoute(
                        conversationId = key.conversationId,
                        onBack = if (twoPane) null else popChat,
                        // One path for both windows: popping the chat leaves the list, which on a
                        // wide window means the detail pane falls back to the new-chat state.
                        onDeleted = popChat,
                        onConversationIdChanged = { selectedConversationId = it },
                    )
                }
            },
    )
}
