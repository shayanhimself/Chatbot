package com.shayanaryan.chatbot.shared.apikey

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val API_KEY = "sk-ant-api03-not-a-real-key"

class FakeApiKeyRepositoryTest {
    @Test
    fun `an empty fake reports no key`() =
        runTest {
            val repository = FakeApiKeyRepository()

            assertFalse(repository.hasKeyFlow().first())
            assertNull(repository.apiKey())
        }

    @Test
    fun `a seeded fake reports the key it was given`() =
        runTest {
            val repository = FakeApiKeyRepository(initialKey = API_KEY)

            assertTrue(repository.hasKeyFlow().first())
            assertEquals(API_KEY, repository.apiKey())
        }

    @Test
    fun `saving then clearing moves the flow both ways and counts the write`() =
        runTest {
            val repository = FakeApiKeyRepository()

            repository.save(API_KEY)
            assertTrue(repository.hasKeyFlow().first())
            assertEquals(1, repository.saveCount)

            repository.clear()
            assertFalse(repository.hasKeyFlow().first())
            assertNull(repository.apiKey())
        }
}
