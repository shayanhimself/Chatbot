package com.shayanaryan.chatbot.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ClaudeModelTest {
    @Test
    fun defaultModelIsSonnet() {
        assertEquals("claude-sonnet-5", ClaudeModel.Default.id)
    }

    @Test
    fun allModelsHaveDistinctIds() {
        val ids = ClaudeModel.entries.map { it.id }
        assertEquals(ids.distinct(), ids)
    }
}
