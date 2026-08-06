package com.shayanaryan.chatbot.feature.conversation.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import com.shayanaryan.chatbot.core.ui.R as CoreUiR

private const val CHAT_TITLE = "Weekend trip to Portland"
private const val USER_MESSAGE = "help me plan a weekend in portland"
private const val ASSISTANT_MESSAGE = "Powell's Books first."
private const val COMPOSED_MESSAGE = "a packing list please"
private const val TYPED_TEXT = "hello"
private const val STREAMED_TEXT = "Powell"

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders the title and both bubbles`() {
        setScreen(openChat)

        composeRule.onNodeWithText(CHAT_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(USER_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText(ASSISTANT_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `a chat with no first message shows the new-chat copy`() {
        setScreen(ChatUiState())

        composeRule.onNodeWithText(string(R.string.conversation_new_chat_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                string(R.string.conversation_new_chat_greeting),
            ).assertIsDisplayed()
    }

    @Test
    fun `send is disabled until there is non-blank text`() {
        setScreen(openChat)

        composeRule
            .onNodeWithContentDescription(
                string(R.string.conversation_send),
            ).assertIsNotEnabled()
        composeRule
            .onNodeWithText(
                string(R.string.conversation_composer_placeholder),
            ).performTextInput(TYPED_TEXT)
        composeRule
            .onNodeWithContentDescription(
                string(R.string.conversation_send),
            ).assertIsEnabled()
    }

    @Test
    fun `send reports the composed text and clears the field`() {
        var sent: String? = null
        setScreen(openChat, onSend = { sent = it })

        composeRule
            .onNodeWithText(
                string(R.string.conversation_composer_placeholder),
            ).performTextInput(COMPOSED_MESSAGE)
        composeRule.onNodeWithContentDescription(string(R.string.conversation_send)).performClick()

        assertEquals(COMPOSED_MESSAGE, sent)
        composeRule
            .onNodeWithText(
                string(R.string.conversation_composer_placeholder),
            ).assertIsDisplayed()
    }

    @Test
    fun `while streaming the trailing button stops the turn`() {
        composeRule.mainClock.autoAdvance = false
        var cancelled = false
        val streaming =
            openChat.copy(
                items = openChat.items + ChatItem.Streaming(STREAMED_TEXT),
                isStreaming = true,
            )
        setScreen(streaming, onCancel = { cancelled = true })

        composeRule.onNodeWithContentDescription(string(R.string.conversation_stop)).performClick()

        assertTrue(cancelled)
    }

    @Test
    fun `the model picker checkmarks the current model and reports a change`() {
        var picked: ClaudeModel? = null
        setScreen(openChat, onModelSelected = { picked = it })

        composeRule.onNodeWithText(ClaudeModel.Sonnet.displayName).performClick()
        composeRule.onNodeWithText(ClaudeModel.Haiku.displayName).performClick()

        assertEquals(ClaudeModel.Haiku, picked)
    }

    @Test
    fun `the model picker is disabled during a turn`() {
        composeRule.mainClock.autoAdvance = false
        setScreen(openChat.copy(isStreaming = true))

        composeRule.onNodeWithText(ClaudeModel.Sonnet.displayName).assertIsNotEnabled()
    }

    @Test
    fun `the overflow menu offers delete and reports it`() {
        var requested = false
        setScreen(openChat, onDeleteRequested = { requested = true })

        composeRule.onNodeWithContentDescription(string(R.string.conversation_more)).performClick()
        composeRule.onNodeWithText(string(R.string.conversation_delete)).performClick()

        assertTrue(requested)
    }

    @Test
    fun `the overflow button is hidden on a chat with nothing to delete`() {
        setScreen(ChatUiState())

        composeRule
            .onNodeWithContentDescription(
                string(R.string.conversation_more),
            ).assertDoesNotExist()
    }

    @Test
    fun `confirming the delete dialog reports it`() {
        var confirmed = false
        val deleting = openChat.copy(deleteDialogVisible = true)
        setScreen(deleting, onDeleteConfirmed = { confirmed = true })

        composeRule.onNodeWithText(string(R.string.conversation_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.conversation_delete_confirm)).performClick()

        assertTrue(confirmed)
    }

    @Test
    fun `retry on the error item reports it`() {
        var retried = false
        val failed = openChat.copy(items = openChat.items + ChatItem.Error(ChatError.Network))
        setScreen(failed, onRetry = { retried = true })

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertTrue(retried)
    }

    @Test
    fun `the back arrow is absent when the caller gives no back action`() {
        setScreen(openChat, onBack = null)

        composeRule
            .onNodeWithContentDescription(
                string(R.string.conversation_back),
            ).assertDoesNotExist()
    }

    private fun persisted(
        id: Long,
        role: Role,
        text: String,
    ) = ChatItem.Persisted(
        Message(
            id = id,
            conversationId = 1L,
            role = role,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = Instant.fromEpochMilliseconds(id),
        ),
    )

    private val openChat =
        ChatUiState(
            conversationId = 1L,
            title = CHAT_TITLE,
            model = ClaudeModel.Sonnet,
            items =
                listOf(
                    persisted(1L, Role.User, USER_MESSAGE),
                    persisted(2L, Role.Assistant, ASSISTANT_MESSAGE),
                ),
        )

    private fun setScreen(
        uiState: ChatUiState,
        onBack: (() -> Unit)? = {},
        onSend: (String) -> Unit = {},
        onCancel: () -> Unit = {},
        onRetry: () -> Unit = {},
        onModelSelected: (ClaudeModel) -> Unit = {},
        onDeleteRequested: () -> Unit = {},
        onDeleteDismissed: () -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ChatScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onSend = onSend,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    onModelSelected = onModelSelected,
                    onDeleteRequested = onDeleteRequested,
                    onDeleteDismissed = onDeleteDismissed,
                    onDeleteConfirmed = onDeleteConfirmed,
                )
            }
        }
    }
}
