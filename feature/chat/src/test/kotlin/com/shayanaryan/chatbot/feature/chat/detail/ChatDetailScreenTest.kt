package com.shayanaryan.chatbot.feature.chat.detail

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.R
import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import com.shayanaryan.chatbot.core.ui.R as CoreUiR

private const val CHAT_TITLE = "Weekend trip to Portland"
private const val USER_MESSAGE = "help me plan a weekend in portland"
private const val ASSISTANT_MESSAGE = "Powell's Books first."
private const val COMPOSED_MESSAGE = "a packing list please"
private const val TYPED_TEXT = "hello"
private const val STREAMED_TEXT = "Powell"
private const val FIRST_MESSAGE = "what should I pack for the coast"
private const val FILLER_MESSAGE =
    "Layers, mostly. Mornings on the coast are cold enough for a fleece, afternoons are warm " +
        "enough for a t-shirt, and it rains without warning in between, so a shell you can " +
        "stuff in a daypack earns its space more than a heavy coat does."
private const val LAST_MESSAGE = "That is the whole list."
private const val LATE_REPLY = "One more thing: bring a rain shell."

// Enough messages that the last one is off the bottom of the window until the list scrolls to it.
private const val FILLER_COUNT = 4

@RunWith(AndroidJUnit4::class)
class ChatDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a chat with no first message shows the new-chat copy`() {
        setScreen(ChatDetailUiState())

        composeRule.onNodeWithText(string(R.string.chat_new_chat_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                string(R.string.chat_new_chat_greeting),
            ).assertIsDisplayed()
    }

    @Test
    fun `send is disabled until there is non-blank text`() {
        setScreen(openChat)

        composeRule
            .onNodeWithContentDescription(
                string(R.string.chat_send),
            ).assertIsNotEnabled()
        composeRule
            .onNodeWithText(
                string(R.string.chat_composer_placeholder),
            ).performTextInput(TYPED_TEXT)
        composeRule
            .onNodeWithContentDescription(
                string(R.string.chat_send),
            ).assertIsEnabled()
    }

    @Test
    fun `send reports the composed text and clears the field`() {
        var sent: String? = null
        setScreen(openChat, onSend = { sent = it })

        composeRule
            .onNodeWithText(
                string(R.string.chat_composer_placeholder),
            ).performTextInput(COMPOSED_MESSAGE)
        composeRule.onNodeWithContentDescription(string(R.string.chat_send)).performClick()

        assertEquals(COMPOSED_MESSAGE, sent)
        composeRule
            .onNodeWithText(
                string(R.string.chat_composer_placeholder),
            ).assertIsDisplayed()
    }

    @Test
    fun `while streaming the trailing button stops the turn`() {
        composeRule.mainClock.autoAdvance = false
        var cancelled = false
        val streaming =
            openChat.copy(
                items = openChat.items + ChatDetailItem.Streaming(STREAMED_TEXT),
                isStreaming = true,
            )
        setScreen(streaming, onCancel = { cancelled = true })

        composeRule.onNodeWithContentDescription(string(R.string.chat_stop)).performClick()

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

        composeRule.onNodeWithContentDescription(string(R.string.chat_more)).performClick()
        composeRule.onNodeWithText(string(R.string.chat_delete)).performClick()

        assertTrue(requested)
    }

    @Test
    fun `the overflow button is hidden on a chat with nothing to delete`() {
        setScreen(ChatDetailUiState())

        composeRule
            .onNodeWithContentDescription(
                string(R.string.chat_more),
            ).assertDoesNotExist()
    }

    @Test
    fun `confirming the delete dialog reports it`() {
        var confirmed = false
        val deleting = openChat.copy(deleteDialogVisible = true)
        setScreen(deleting, onDeleteConfirmed = { confirmed = true })

        composeRule.onNodeWithText(string(R.string.chat_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.chat_delete_confirm)).performClick()

        assertTrue(confirmed)
    }

    @Test
    fun `retry on the error item reports it`() {
        var retried = false
        val failed = openChat.copy(items = openChat.items + ChatDetailItem.Error(ApiError.Network))
        setScreen(failed, onRetry = { retried = true })

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_retry)).performClick()

        assertTrue(retried)
    }

    @Test
    fun `opening a chat longer than the window shows its last message`() {
        setScreen(longChat)

        composeRule.onNodeWithText(LAST_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `a reply arriving keeps the tail in view`() {
        val state = mutableStateOf(longChat)
        setScreen(state)

        composeRule.runOnIdle { state.value = longChat.plusReply(LATE_REPLY) }

        composeRule.onNodeWithText(LATE_REPLY).assertIsDisplayed()
    }

    @Test
    fun `scrolling up stops the list following the tail`() {
        val state = mutableStateOf(longChat)
        setScreen(state)

        messageList().performTouchInput { swipeDown() }
        assertFalse(isAtTail())
        composeRule.runOnIdle { state.value = longChat.plusReply(LATE_REPLY) }

        assertFalse(isAtTail())
    }

    @Test
    fun `the back arrow is absent when the caller gives no back action`() {
        setScreen(openChat, onBack = null)

        composeRule
            .onNodeWithContentDescription(
                string(R.string.chat_back),
            ).assertDoesNotExist()
    }

    private fun persisted(
        id: Long,
        role: Role,
        text: String,
    ) = ChatDetailItem.Persisted(
        Message(
            id = id,
            chatId = 1L,
            role = role,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = Instant.fromEpochMilliseconds(id),
        ),
    )

    /** The message list, the screen's only lazy list. */
    private fun messageList() =
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))

    /** Whether the message list is scrolled to the end of its content. */
    private fun isAtTail(): Boolean {
        val scroll =
            messageList().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        return scroll.value() == scroll.maxValue()
    }

    private fun ChatDetailUiState.plusReply(text: String) =
        copy(items = items + persisted((items.size + 1).toLong(), Role.Assistant, text))

    private val openChat =
        ChatDetailUiState(
            chatId = 1L,
            title = CHAT_TITLE,
            model = ClaudeModel.Sonnet,
            items =
                listOf(
                    persisted(1L, Role.User, USER_MESSAGE),
                    persisted(2L, Role.Assistant, ASSISTANT_MESSAGE),
                ),
        )

    private val longChat =
        openChat.copy(
            items =
                (
                    // First message
                    listOf(Role.User to FIRST_MESSAGE) +
                        // A few replies from Assistant
                        List(FILLER_COUNT) { Role.Assistant to FILLER_MESSAGE } +
                        // Last message
                        (Role.Assistant to LAST_MESSAGE)
                ).mapIndexed { index, (role, text) ->
                    persisted(
                        id = index.toLong(),
                        role = role,
                        text = text,
                    )
                },
        )

    private fun setScreen(state: MutableState<ChatDetailUiState>) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ChatDetailScreen(
                    uiState = state.value,
                    onBack = {},
                    onSend = {},
                    onCancel = {},
                    onRetry = {},
                    onModelSelected = {},
                    onDeleteRequested = {},
                    onDeleteDismissed = {},
                    onDeleteConfirmed = {},
                )
            }
        }
    }

    private fun setScreen(
        uiState: ChatDetailUiState,
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
                ChatDetailScreen(
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
