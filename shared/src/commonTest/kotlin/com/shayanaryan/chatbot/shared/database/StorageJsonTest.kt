package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageJsonTest {
    @Test
    fun `encodes a content list with a type discriminator`() {
        val encoded = storageJson.encodeToString(listOf<ContentBlock>(ContentBlock.Text("hi")))

        assertEquals("""[{"type":"text","text":"hi"}]""", encoded)
    }

    @Test
    fun `round trips a multi block content list`() {
        val content = listOf<ContentBlock>(ContentBlock.Text("one"), ContentBlock.Text("two"))

        val decoded =
            storageJson.decodeFromString<List<ContentBlock>>(
                storageJson.encodeToString(content),
            )

        assertEquals(content, decoded)
    }

    @Test
    fun `decodes a stored block that carries an unknown field`() {
        val stored = """[{"type":"text","text":"hi","tokens":3}]"""

        val decoded = storageJson.decodeFromString<List<ContentBlock>>(stored)

        assertEquals(listOf<ContentBlock>(ContentBlock.Text("hi")), decoded)
    }
}
