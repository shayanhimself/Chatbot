package com.shayanaryan.chatbot.feature.chat.detail

import com.shayanaryan.chatbot.shared.ApiError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val STREAMED_TEXT = "Powell"

class ChatDetailUiStateTest {
    @Test
    fun `a chat with no items has no tail`() {
        assertNull(ChatDetailUiState().tailIndex)
    }

    @Test
    fun `the tail is the last item`() {
        val uiState =
            ChatDetailUiState(
                items = listOf(ChatDetailItem.Thinking, ChatDetailItem.Error(ApiError.Network)),
            )

        assertEquals(1, uiState.tailIndex)
    }

    @Test
    fun `the tail follows a copy that adds an item`() {
        val uiState = ChatDetailUiState(items = listOf(ChatDetailItem.Thinking))

        val streaming =
            uiState.copy(
                items = uiState.items + ChatDetailItem.Streaming(STREAMED_TEXT),
            )

        assertEquals(1, streaming.tailIndex)
    }
}
