package com.shayanaryan.chatbot.feature.conversation.chat

import com.shayanaryan.chatbot.shared.chat.ChatError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val STREAMED_TEXT = "Powell"

class ChatUiStateTest {
    @Test
    fun `a chat with no items has no tail`() {
        assertNull(ChatUiState().tailIndex)
    }

    @Test
    fun `the tail is the last item`() {
        val uiState =
            ChatUiState(
                items = listOf(ChatItem.Thinking, ChatItem.Error(ChatError.Network)),
            )

        assertEquals(1, uiState.tailIndex)
    }

    @Test
    fun `the tail follows a copy that adds an item`() {
        val uiState = ChatUiState(items = listOf(ChatItem.Thinking))

        val streaming = uiState.copy(items = uiState.items + ChatItem.Streaming(STREAMED_TEXT))

        assertEquals(1, streaming.tailIndex)
    }
}
