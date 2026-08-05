package com.shayanaryan.chatbot.feature.conversation.conversationlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.string
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

private const val FIRST_TITLE = "Weekend trip to Portland"
private const val FIRST_SNIPPET = "Powell's Books first."
private const val SECOND_TITLE = "Miso glaze recipe"

@RunWith(AndroidJUnit4::class)
class ConversationListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val items =
        listOf(
            ConversationListItemUiState(
                id = 1L,
                title = FIRST_TITLE,
                snippet = FIRST_SNIPPET,
                relativeTime = RelativeTime(R.string.conversation_time_hours, 2),
            ),
            ConversationListItemUiState(
                id = 2L,
                title = SECOND_TITLE,
                snippet = null,
                relativeTime = RelativeTime(R.string.conversation_time_days, 1),
            ),
        )

    private fun setScreen(
        uiState: ConversationListUiState,
        onConversationClick: (Long) -> Unit = {},
        onNewChat: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ConversationListScreen(
                    uiState = uiState,
                    selectedConversationId = null,
                    onConversationClick = onConversationClick,
                    onNewChat = onNewChat,
                )
            }
        }
    }

    @Test
    fun `shows an item per conversation with its snippet and age`() {
        setScreen(ConversationListUiState(isLoading = false, conversations = items))

        composeRule.onNodeWithText(FIRST_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_SNIPPET).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.conversation_time_hours, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.conversation_time_days, 1)).assertIsDisplayed()
    }

    @Test
    fun `reports the id of the item that was tapped`() {
        var clicked: Long? = null
        setScreen(
            ConversationListUiState(isLoading = false, conversations = items),
            onConversationClick = { clicked = it },
        )

        composeRule.onNodeWithText(SECOND_TITLE).performClick()

        assertEquals(2L, clicked)
    }

    @Test
    fun `shows the empty state when there is nothing stored`() {
        setScreen(ConversationListUiState(isLoading = false, conversations = emptyList()))

        composeRule
            .onNodeWithText(
                string(R.string.conversation_list_empty_title),
            ).assertIsDisplayed()
    }

    @Test
    fun `shows neither items nor the empty state while loading`() {
        setScreen(ConversationListUiState(isLoading = true, conversations = emptyList()))

        composeRule
            .onNodeWithText(
                string(R.string.conversation_list_empty_title),
            ).assertDoesNotExist()
    }

    @Test
    fun `the new chat button reports a tap`() {
        var tapped = false
        setScreen(
            ConversationListUiState(isLoading = false, conversations = items),
            onNewChat = { tapped = true },
        )

        composeRule.onNodeWithText(string(R.string.conversation_list_new_chat)).performClick()

        assertEquals(true, tapped)
    }
}
