package com.shayanaryan.chatbot.feature.conversation.chat

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.conversation.TurnState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

private const val USER_MESSAGE = "hi"
private const val ASSISTANT_MESSAGE = "hello"
private const val PARTIAL_TEXT = "hel"
private const val CANCELLED_TEXT = "half a th"
private const val FAILED_TEXT = "half"

// A turn that has started but produced no token, and a reply that finished with nothing to show,
// are both an empty string to the mapper and two different cases to a reader.
private const val NO_TOKEN_YET = ""
private const val NO_TEXT = ""

class ChatItemMapperTest {
    private var nextId = 1L

    private fun message(
        role: Role,
        text: String,
        status: MessageStatus = MessageStatus.Complete,
    ) = Message(
        id = nextId++,
        conversationId = 1L,
        role = role,
        content = listOf(ContentBlock.Text(text)),
        status = status,
        createdAt = Instant.fromEpochMilliseconds(nextId),
    )

    private fun user(text: String) = message(Role.User, text)

    private fun assistant(
        text: String,
        status: MessageStatus = MessageStatus.Complete,
    ) = message(Role.Assistant, text, status)

    @Test
    fun `an idle conversation is only its persisted messages`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(ASSISTANT_MESSAGE),
            ).toChatItems(TurnState.Idle)

        assertEquals(2, items.size)
        assertTrue(items.all { it is ChatItem.Persisted })
    }

    @Test
    fun `an empty streaming turn after a user message is the thinking item`() {
        val items = listOf(user(USER_MESSAGE)).toChatItems(TurnState.Streaming(NO_TOKEN_YET))

        assertEquals(ChatItem.Thinking, items.last())
    }

    @Test
    fun `a streaming turn with text is the streaming item`() {
        val items = listOf(user(USER_MESSAGE)).toChatItems(TurnState.Streaming(PARTIAL_TEXT))

        assertEquals(ChatItem.Streaming(PARTIAL_TEXT), items.last())
    }

    /**
     * The repository stores the assistant row *before* the turn returns to Idle, so there is a
     * window where the database has already emitted the finished message while the turn still
     * reads Streaming. Rendering both would double the bubble for a frame.
     */
    @Test
    fun `a stale streaming turn behind a persisted reply adds no item`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(ASSISTANT_MESSAGE),
            ).toChatItems(TurnState.Streaming(ASSISTANT_MESSAGE))

        assertEquals(2, items.size)
        assertTrue(items.all { it is ChatItem.Persisted })
    }

    /**
     * A failed turn writes a Failed assistant row and keeps the turn entry until the next send,
     * retry or delete, so the error renders *after* that row rather than instead of it.
     */
    @Test
    fun `a failed turn keeps its persisted item and adds the error`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(FAILED_TEXT, MessageStatus.Failed),
            ).toChatItems(TurnState.Failed(ChatError.Network))

        assertEquals(3, items.size)
        assertIs<ChatItem.Persisted>(items[1])
        assertEquals(ChatItem.Error(ChatError.Network), items[2])
    }

    @Test
    fun `a failed turn that produced no text still shows the error`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(NO_TEXT, MessageStatus.Failed),
            ).toChatItems(TurnState.Failed(ChatError.Overloaded))

        assertEquals(2, items.size)
        assertEquals(ChatItem.Error(ChatError.Overloaded), items.last())
    }

    @Test
    fun `a blank message is not an item`() {
        val items = listOf(user(USER_MESSAGE), assistant(NO_TEXT)).toChatItems(TurnState.Idle)

        assertEquals(1, items.size)
    }

    @Test
    fun `a cancelled reply keeps its partial text as an ordinary message`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(CANCELLED_TEXT, MessageStatus.Cancelled),
            ).toChatItems(TurnState.Idle)

        assertEquals(2, items.size)
        val persisted = assertIs<ChatItem.Persisted>(items.last())
        assertEquals(MessageStatus.Cancelled, persisted.message.status)
    }

    @Test
    fun `an empty conversation with an idle turn has no items at all`() {
        assertEquals(emptyList(), emptyList<Message>().toChatItems(TurnState.Idle))
    }
}
