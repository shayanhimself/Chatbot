package com.shayanaryan.chatbot.feature.chat.detail

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private const val STREAMED_TEXT = "Powell"
private const val PARTIAL_TEXT = "Pow"
private const val CHAT_ID = 1L
private const val FIRST_MESSAGE_ID = 1L
private const val SECOND_MESSAGE_ID = 2L
private const val CREATED_AT_MILLIS = 1_000L

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

    @Test
    fun `two persisted messages key apart`() {
        assertNotEquals(persisted(FIRST_MESSAGE_ID).key, persisted(SECOND_MESSAGE_ID).key)
    }

    @Test
    fun `a streaming item keeps its key as tokens arrive`() {
        assertEquals(
            ChatDetailItem.Streaming(PARTIAL_TEXT).key,
            ChatDetailItem.Streaming(STREAMED_TEXT).key,
        )
    }

    @Test
    fun `every kind of item keys apart from the others`() {
        val keys =
            listOf(
                persisted(FIRST_MESSAGE_ID),
                ChatDetailItem.Thinking,
                ChatDetailItem.Streaming(STREAMED_TEXT),
                ChatDetailItem.Error(ApiError.Network),
            ).map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    private fun persisted(id: Long) =
        ChatDetailItem.Persisted(
            Message(
                id = id,
                chatId = CHAT_ID,
                role = Role.Assistant,
                content = listOf(ContentBlock.Text(STREAMED_TEXT)),
                status = MessageStatus.Complete,
                createdAt = Instant.fromEpochMilliseconds(CREATED_AT_MILLIS),
            ),
        )
}
