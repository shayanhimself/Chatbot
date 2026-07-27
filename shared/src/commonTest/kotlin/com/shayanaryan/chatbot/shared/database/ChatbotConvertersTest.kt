package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertEquals(ClaudeModel.Default, converters.toModel("Sonnet4"))
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
        assertFailsWith<IllegalArgumentException> { converters.toStatus("Pending") }
    }

    @Test
    fun `round trips a multi block content list`() {
        val content = listOf<ContentBlock>(ContentBlock.Text("one"), ContentBlock.Text("two"))

        assertEquals(content, converters.toContent(converters.fromContent(content)))
    }

    @Test
    fun `stores the status name so a query can match it as a literal`() {
        assertEquals("Complete", converters.fromStatus(MessageStatus.Complete))
    }
}
