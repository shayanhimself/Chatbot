package com.shayanaryan.chatbot.feature.chat.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.chat.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

private const val FIRST_TITLE = "Weekend trip to Portland"
private const val FIRST_SNIPPET = "Powell's Books first."
private const val SECOND_TITLE = "Miso glaze recipe"

@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val items =
        listOf(
            ChatListItemUiState(
                id = 1L,
                title = FIRST_TITLE,
                snippet = FIRST_SNIPPET,
                relativeTime = RelativeTime(R.string.chat_time_hours, 2),
            ),
            ChatListItemUiState(
                id = 2L,
                title = SECOND_TITLE,
                snippet = null,
                relativeTime = RelativeTime(R.string.chat_time_days, 1),
            ),
        )

    @Test
    fun `shows an item per chat with its snippet and age`() {
        setScreen(ChatListUiState(isLoading = false, chats = items))

        composeRule.onNodeWithText(FIRST_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(FIRST_SNIPPET).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.chat_time_hours, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.chat_time_days, 1)).assertIsDisplayed()
    }

    @Test
    fun `reports the id of the item that was tapped`() {
        var clicked: Long? = null
        setScreen(
            ChatListUiState(isLoading = false, chats = items),
            onChatClick = { clicked = it },
        )

        composeRule.onNodeWithText(SECOND_TITLE).performClick()

        assertEquals(2L, clicked)
    }

    @Test
    fun `shows the empty state when there is nothing stored`() {
        setScreen(ChatListUiState(isLoading = false, chats = emptyList()))

        composeRule
            .onNodeWithText(
                string(R.string.chat_list_empty_title),
            ).assertIsDisplayed()
    }

    @Test
    fun `shows neither items nor the empty state while loading`() {
        setScreen(ChatListUiState(isLoading = true, chats = emptyList()))

        composeRule
            .onNodeWithText(
                string(R.string.chat_list_empty_title),
            ).assertDoesNotExist()
    }

    @Test
    fun `the new chat button reports a tap`() {
        var tapped = false
        setScreen(
            ChatListUiState(isLoading = false, chats = items),
            onNewChat = { tapped = true },
        )

        composeRule.onNodeWithText(string(R.string.chat_list_new_chat)).performClick()

        assertEquals(true, tapped)
    }

    private fun setScreen(
        uiState: ChatListUiState,
        onChatClick: (Long) -> Unit = {},
        onNewChat: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ChatListScreen(
                    uiState = uiState,
                    selectedChatId = null,
                    onChatClick = onChatClick,
                    onNewChat = onNewChat,
                )
            }
        }
    }
}
