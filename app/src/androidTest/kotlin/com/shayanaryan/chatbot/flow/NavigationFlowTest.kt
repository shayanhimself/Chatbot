package com.shayanaryan.chatbot.flow

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.MainActivity
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.di.ApiKeyModule
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.TestApiKeyStore
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import com.shayanaryan.chatbot.wire.LocalAnthropic
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.shayanaryan.chatbot.feature.chat.R as ChatR

private const val SEEDED_KEY = "sk-ant-api03-navigation-flow"
private const val FIRST_MESSAGE = "what should I pack"
private const val REPLY_TEXT = "Layers and a rain shell."
private const val EXPECTED_MESSAGE_COUNT = 2

/**
 * Whether the destination the app shows is the one the back stack was asked for: after a tap, after
 * process restoration, and after an intent from outside the app.
 */
@HiltAndroidTest
@UninstallModules(ApiKeyModule::class)
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val anthropic = LocalAnthropic()

    @get:Rule(order = 2)
    val keyStore = TestApiKeyStore()

    // The Activity must not launch until the key store is seeded, and createAndroidComposeRule
    // starts it while the rule evaluates, which is before @Before runs.
    @get:Rule(order = 3)
    val composeRule = createEmptyComposeRule()

    private val launcher = AppLauncher()

    /** The store the app reads its key from, replacing its own so each test gets a fresh file. */
    @BindValue
    @JvmField
    val apiKeyRepository: ApiKeyRepository = keyStore.repository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Before
    fun setUp() =
        runTest {
            hiltRule.inject()
            apiKeyRepository.save(SEEDED_KEY)
            clearChats()
        }

    @After
    fun tearDown() =
        runTest {
            launcher.close()
            clearChats()
        }

    private suspend fun clearChats() {
        chatRepository.getChatsFlow().first().forEach { chatRepository.delete(it.id) }
    }

    /**
     * Seeding through the repository rather than the composer keeps this about navigation.
     *
     * `send` returns once the user message is written, so reading the rows without waiting can
     * observe one message rather than two.
     *
     * @return the id of the chat the message created.
     */
    private suspend fun seedChat(): Long {
        anthropic.enqueueReply(REPLY_TEXT)
        val chatId = chatRepository.send(chatId = null, text = FIRST_MESSAGE)
        chatRepository.getMessagesFlow(chatId).first { it.size == EXPECTED_MESSAGE_COUNT }
        return chatId
    }

    /**
     * Waits until the open chat is on top, which the composer identifies.
     *
     * A chat's last reply is also its row's snippet on the list, so waiting on the reply text
     * would be satisfied by the screen the chat was opened from.
     */
    private fun awaitOpenChat() {
        composeRule.awaitText(string(ChatR.string.chat_composer_placeholder))
    }

    private fun intentFor(chatId: Long): Intent =
        Intent(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_CHAT_ID, chatId)

    @Test
    fun `tapping a chat opens that chat and not another`() =
        runTest {
            seedChat()

            launcher.launch()
            composeRule.awaitText(FIRST_MESSAGE)
            composeRule.clickWhenStill(hasText(FIRST_MESSAGE))

            awaitOpenChat()
            composeRule.onNodeWithText(REPLY_TEXT).assertIsDisplayed()
        }

    @Test
    fun `an open chat survives restoration`() =
        runTest {
            seedChat()

            val scenario = launcher.launch()
            composeRule.awaitText(FIRST_MESSAGE)
            composeRule.clickWhenStill(hasText(FIRST_MESSAGE))
            awaitOpenChat()

            scenario.recreate()

            awaitOpenChat()
            composeRule.onNodeWithText(REPLY_TEXT).assertIsDisplayed()
        }

    @Test
    fun `an intent naming a chat opens it and back returns to the list`() =
        runTest {
            val chatId = seedChat()

            launcher.launch(intentFor(chatId))
            awaitOpenChat()
            composeRule.onNodeWithText(REPLY_TEXT).assertIsDisplayed()

            composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_back)))

            val chatList = string(ChatR.string.chat_list_title)
            composeRule.awaitText(chatList)
            composeRule.onNodeWithText(chatList).assertIsDisplayed()
        }

    @Test
    fun `an intent naming a chat that no longer exists opens a new chat`() =
        runTest {
            val chatId = seedChat()
            chatRepository.delete(chatId)

            launcher.launch(intentFor(chatId))

            // The greeting rather than the title, which the list's own new-chat button repeats.
            val greeting = string(ChatR.string.chat_new_chat_greeting)
            composeRule.awaitText(greeting)
            composeRule.onNodeWithText(greeting).assertIsDisplayed()
        }
}
