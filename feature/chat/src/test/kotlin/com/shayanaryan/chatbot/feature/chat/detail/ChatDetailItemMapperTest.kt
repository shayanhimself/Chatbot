package com.shayanaryan.chatbot.feature.chat.detail

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.chat.TurnState
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

class ChatDetailItemMapperTest {
    private var nextId = 1L

    private fun message(
        role: Role,
        text: String,
        status: MessageStatus = MessageStatus.Complete,
        error: ApiError? = null,
    ) = Message(
        id = nextId++,
        chatId = 1L,
        role = role,
        content = listOf(ContentBlock.Text(text)),
        status = status,
        error = error,
        createdAt = Instant.fromEpochMilliseconds(nextId),
    )

    private fun user(text: String) = message(Role.User, text)

    private fun assistant(
        text: String,
        status: MessageStatus = MessageStatus.Complete,
    ) = message(Role.Assistant, text, status)

    private fun failed(
        text: String,
        error: ApiError,
    ) = message(Role.Assistant, text, MessageStatus.Failed, error)

    @Test
    fun `an idle chat is only its persisted messages`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(ASSISTANT_MESSAGE),
            ).toChatDetailItems(TurnState.Idle)

        assertEquals(2, items.size)
        assertTrue(items.all { it is ChatDetailItem.Persisted })
    }

    @Test
    fun `an empty streaming turn after a user message is the thinking item`() {
        val items = listOf(user(USER_MESSAGE)).toChatDetailItems(TurnState.Streaming(NO_TOKEN_YET))

        assertEquals(ChatDetailItem.Thinking, items.last())
    }

    @Test
    fun `a streaming turn with text is the streaming item`() {
        val items = listOf(user(USER_MESSAGE)).toChatDetailItems(TurnState.Streaming(PARTIAL_TEXT))

        assertEquals(ChatDetailItem.Streaming(PARTIAL_TEXT), items.last())
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
            ).toChatDetailItems(TurnState.Streaming(ASSISTANT_MESSAGE))

        assertEquals(2, items.size)
        assertTrue(items.all { it is ChatDetailItem.Persisted })
    }

    /**
     * The error renders *after* the partial reply rather than instead of it.
     */
    @Test
    fun `a failed reply keeps its persisted item and adds the error`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                failed(FAILED_TEXT, ApiError.Network),
            ).toChatDetailItems(TurnState.Idle)

        assertEquals(3, items.size)
        assertIs<ChatDetailItem.Persisted>(items[1])
        assertEquals(ChatDetailItem.Error(ApiError.Network), items[2])
    }

    @Test
    fun `a failed reply that produced no text still shows the error`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                failed(NO_TEXT, ApiError.Overloaded),
            ).toChatDetailItems(TurnState.Idle)

        assertEquals(2, items.size)
        assertEquals(ChatDetailItem.Error(ApiError.Overloaded), items.last())
    }

    /**
     * The turn is gone after a restart, so the error has to come from the stored reply for the
     * retry it sits above to be reachable at all.
     */
    @Test
    fun `a chat reopened on a failed reply still shows the error`() {
        val restored =
            listOf(
                user(USER_MESSAGE),
                failed(NO_TEXT, ApiError.Network),
            )

        val items = restored.toChatDetailItems(TurnState.Idle)

        assertEquals(ChatDetailItem.Error(ApiError.Network), items.last())
    }

    /**
     * Only the trailing reply's failure is live. An earlier one was already answered by the retry
     * that followed it.
     */
    @Test
    fun `an earlier failure inside the history adds no error`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                failed(FAILED_TEXT, ApiError.Network),
                assistant(ASSISTANT_MESSAGE),
            ).toChatDetailItems(TurnState.Idle)

        assertEquals(3, items.size)
        assertTrue(items.all { it is ChatDetailItem.Persisted })
    }

    @Test
    fun `a blank message is not an item`() {
        val items = listOf(user(USER_MESSAGE), assistant(NO_TEXT)).toChatDetailItems(TurnState.Idle)

        assertEquals(1, items.size)
    }

    @Test
    fun `a cancelled reply keeps its partial text as an ordinary message`() {
        val items =
            listOf(
                user(USER_MESSAGE),
                assistant(CANCELLED_TEXT, MessageStatus.Cancelled),
            ).toChatDetailItems(TurnState.Idle)

        assertEquals(2, items.size)
        val persisted = assertIs<ChatDetailItem.Persisted>(items.last())
        assertEquals(MessageStatus.Cancelled, persisted.message.status)
    }

    @Test
    fun `an empty chat with an idle turn has no items at all`() {
        assertEquals(emptyList(), emptyList<Message>().toChatDetailItems(TurnState.Idle))
    }
}
