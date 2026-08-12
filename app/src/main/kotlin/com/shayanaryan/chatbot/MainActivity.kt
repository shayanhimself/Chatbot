package com.shayanaryan.chatbot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installed before super.onCreate and before setContent: the keep-on-screen condition has
        // to exist before the first frame, and holding the splash over Undecided is what stops a
        // blank frame on a cold start.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Undecided }
        enableEdgeToEdge()
        // Only a fresh launch seeds. A recreated activity restores its own back stack, and its
        // intent still carries the extra it launched with, so reading that extra again would drag
        // the user back into the notification's conversation on every rotation.
        if (savedInstanceState == null) {
            deepLinkConversationId = intent.conversationIdExtra()
        }
        setContent {
            ChatbotTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                // Nothing composes until the gate resolves, so the back stack is seeded correctly
                // on first composition rather than seeded wrong and rewritten.
                (state as? MainUiState.Decided)?.let { decided ->
                    ChatbotApp(
                        hasApiKey = decided.hasApiKey,
                        deepLinkConversationId = deepLinkConversationId,
                        onDeepLinkHandled = { deepLinkConversationId = null },
                    )
                }
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
