package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.ContentBlock
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BLOCK_TEXT = "hi"
private const val FIRST_BLOCK_TEXT = "one"
private const val SECOND_BLOCK_TEXT = "two"
private const val ENCODED_BLOCK = """[{"type":"text","text":"hi"}]"""

// A row written by a later version of the app, which the current schema has no field for.
private const val STORED_BLOCK_WITH_UNKNOWN_FIELD = """[{"type":"text","text":"hi","tokens":3}]"""

class StorageJsonTest {
    @Test
    fun `encodes a content list with a type discriminator`() {
        val encoded =
            storageJson.encodeToString(
                listOf<ContentBlock>(ContentBlock.Text(BLOCK_TEXT)),
            )

        assertEquals(ENCODED_BLOCK, encoded)
    }

    @Test
    fun `round trips a multi block content list`() {
        val content =
            listOf<ContentBlock>(
                ContentBlock.Text(FIRST_BLOCK_TEXT),
                ContentBlock.Text(SECOND_BLOCK_TEXT),
            )

        val decoded =
            storageJson.decodeFromString<List<ContentBlock>>(
                storageJson.encodeToString(content),
            )

        assertEquals(content, decoded)
    }

    @Test
    fun `decodes a stored block that carries an unknown field`() {
        val stored = STORED_BLOCK_WITH_UNKNOWN_FIELD

        val decoded = storageJson.decodeFromString<List<ContentBlock>>(stored)

        assertEquals(listOf<ContentBlock>(ContentBlock.Text(BLOCK_TEXT)), decoded)
    }
}
