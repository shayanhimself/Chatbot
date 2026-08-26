package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val RETIRED_MODEL_NAME = "Sonnet4"
private const val UNWRITTEN_STATUS_NAME = "Pending"
private const val FIRST_BLOCK_TEXT = "one"
private const val SECOND_BLOCK_TEXT = "two"
private const val RETRY_AFTER_SECONDS = 30

// An error kind written by a build that had one this one does not.
private const val RETIRED_ERROR = """{"type":"quota_exhausted"}"""

// Every error the app can store, the one carrying a payload included.
private val storableErrors =
    listOf(
        ApiError.Authentication,
        ApiError.RateLimited(retryAfterSeconds = null),
        ApiError.RateLimited(RETRY_AFTER_SECONDS),
        ApiError.Overloaded,
        ApiError.InvalidRequest,
        ApiError.Server,
        ApiError.Network,
        ApiError.Timeout,
        ApiError.Unexpected,
    )

class ChatbotConvertersTest {
    private val converters = ChatbotConverters()

    @Test
    fun `round trips every model`() {
        ClaudeModel.entries.forEach { model ->
            assertEquals(model, converters.toModel(converters.fromModel(model)))
        }
    }

    @Test
    fun `falls back to the default model for a name that no longer exists`() {
        assertEquals(ClaudeModel.Default, converters.toModel(RETIRED_MODEL_NAME))
    }

    @Test
    fun `round trips every role`() {
        Role.entries.forEach { role ->
            assertEquals(role, converters.toRole(converters.fromRole(role)))
        }
    }

    @Test
    fun `round trips every status`() {
        MessageStatus.entries.forEach { status ->
            assertEquals(status, converters.toStatus(converters.fromStatus(status)))
        }
    }

    @Test
    fun `rejects a status the app never wrote`() {
        assertFailsWith<IllegalArgumentException> { converters.toStatus(UNWRITTEN_STATUS_NAME) }
    }

    @Test
    fun `round trips a multi block content list`() {
        val content =
            listOf<ContentBlock>(
                ContentBlock.Text(FIRST_BLOCK_TEXT),
                ContentBlock.Text(SECOND_BLOCK_TEXT),
            )

        assertEquals(content, converters.toContent(converters.fromContent(content)))
    }

    @Test
    fun `round trips every error`() {
        storableErrors.forEach { error ->
            assertEquals(error, converters.toError(converters.fromError(error)))
        }
    }

    @Test
    fun `stores no error for a message that did not fail`() {
        assertNull(converters.fromError(null))
        assertNull(converters.toError(null))
    }

    @Test
    fun `falls back to the unexpected error for a kind that no longer exists`() {
        assertEquals(ApiError.Unexpected, converters.toError(RETIRED_ERROR))
    }

    @Test
    fun `stores the status name so a query can match it as a literal`() {
        assertEquals("Complete", converters.fromStatus(MessageStatus.Complete))
    }
}
