package com.shayanaryan.chatbot.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import com.shayanaryan.chatbot.shared.claude.ApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.ClaudeEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Every binding the app resolves at startup, injected on a real device.
 *
 * The JVM tests build their subjects by hand, so a module that fails to provide something is not
 * caught until launch. Injecting here is what proves the graph the app actually runs on.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DependencyGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var apiKeyRepository: ApiKeyRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var claudeEngine: ClaudeEngine

    @Inject
    lateinit var apiKeyValidator: ApiKeyValidator

    @Test
    fun `every startup binding resolves`() {
        hiltRule.inject()

        assertNotNull(apiKeyRepository)
        assertNotNull(chatRepository)
        assertNotNull(claudeEngine)
        assertNotNull(apiKeyValidator)
    }

    // This uses the device's real Keystore, since proving the Hilt graph is the point of this class.
    // A failure on a local emulator that already ran the app usually means leftover device state,
    // not a broken test: uninstall the app and rerun.
    @Test
    fun `the key store reports no key on a clean install`() =
        runTest {
            hiltRule.inject()

            assertFalse(apiKeyRepository.hasKeyFlow().first())
        }
}
