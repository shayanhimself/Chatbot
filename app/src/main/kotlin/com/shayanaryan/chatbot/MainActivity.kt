package com.shayanaryan.chatbot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.navigation.ChatbotApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * The conversation a launch intent asked for, cleared once the back stack has been seeded.
     * Snapshot state rather than a plain field because `onNewIntent` arrives from outside the
     * composition and has to recompose it.
     */
    private var deepLinkConversationId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch seeds. A recreated activity restores its own back stack, and its
        // intent still carries the extra it launched with, so reading that extra again would drag
        // the user back into the notification's conversation on every rotation.
        if (savedInstanceState == null) {
            deepLinkConversationId = intent.conversationIdExtra()
        }
        setContent {
            ChatbotTheme {
                ChatbotApp(
                    // Replaced by the gate in the next task.
                    hasApiKey = true,
                    deepLinkConversationId = deepLinkConversationId,
                    onDeepLinkHandled = { deepLinkConversationId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Without this, getIntent() keeps returning the intent the activity launched with.
        setIntent(intent)
        deepLinkConversationId = intent.conversationIdExtra()
    }

    companion object {
        /** Set by a reminder notification to reopen the conversation that scheduled it. */
        const val EXTRA_CONVERSATION_ID: String = "com.shayanaryan.chatbot.extra.CONVERSATION_ID"
    }
}

/**
 * The id is only ever a database key, and a conversation that does not exist resolves to a new chat
 * rather than an error, so an intent from outside the app cannot do anything worse than open an
 * empty screen.
 */
private fun Intent.conversationIdExtra(): Long? =
    getLongExtra(MainActivity.EXTRA_CONVERSATION_ID, NO_CONVERSATION_ID).takeIf { it > 0 }

private const val NO_CONVERSATION_ID: Long = -1L
