package com.shayanaryan.chatbot.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.di.ApiKeyModule
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.TestApiKeyStore
import com.shayanaryan.chatbot.shared.chat.Chat
import com.shayanaryan.chatbot.shared.chat.ChatRepository
import com.shayanaryan.chatbot.shared.textContent
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.shayanaryan.chatbot.feature.chat.R as ChatR

private const val SEEDED_KEY = "sk-ant-api03-chat-flow"
private const val SENT_MESSAGE = "help me plan a weekend in portland"
private const val REPLY_TEXT = "Powell's Books first."
private const val MESSAGES_PATH = "/v1/messages"
private const val EXPECTED_MESSAGE_COUNT = 2

private const val LONG_REPLY_SENT_MESSAGE = "what should I pack for the coast"
private const val LONG_REPLY_TEXT =
    "Layers, mostly. Mornings on the coast are cold enough for a fleece, afternoons are warm " +
        "enough for a t-shirt, and it rains without warning in between, so a shell you can " +
        "stuff in a daypack earns its space more than a heavy coat does. Bring shoes you do " +
        "not mind soaking, because the sand at the tide line is wet for most of the day, and " +
        "a second pair to change into for the drive home."

private const val LONG_REPLY_CHUNK_LENGTH = 8
private const val LONG_REPLY_BYTES_PER_PERIOD = 128L
private const val LONG_REPLY_PERIOD_MILLIS = 400L
private const val LONG_REPLY_TIMEOUT_MILLIS = 90_000L
private const val LONG_REPLY_FIRST_CHUNK = "Layers, "

/**
 * Whether a message typed into the composer travels the whole stack and comes back as rendered,
 * stored text, and whether the chat it created survives the trip out to the list and back.
 */
@HiltAndroidTest
@UninstallModules(ApiKeyModule::class)
@RunWith(AndroidJUnit4::class)
class ChatFlowTest {
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

    private fun sendFirstMessage() {
        anthropic.enqueueReply(REPLY_TEXT)
        launcher.launch()
        composeRule.awaitText(string(ChatR.string.chat_list_new_chat))
        composeRule.clickWhenStill(hasText(string(ChatR.string.chat_list_new_chat)))
        composeRule
            .onNodeWithText(string(ChatR.string.chat_composer_placeholder))
            .performTextInput(SENT_MESSAGE)
        composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_send)))
        composeRule.awaitText(REPLY_TEXT)
    }

    @Test
    fun `a composed message reaches the API and its reply reaches the screen`() {
        sendFirstMessage()

        val request = anthropic.lastRequest()

        assertEquals(MESSAGES_PATH, request.target.substringBefore('?'))
        assertTrue(
            request.body
                ?.utf8()
                .orEmpty()
                .contains(SENT_MESSAGE),
        )
        // The top bar names the chat after its first message, so that text is on screen twice and
        // the bubble is the lower of the two.
        composeRule.onAllNodesWithText(SENT_MESSAGE).onLast().assertIsDisplayed()
        composeRule.onNodeWithText(REPLY_TEXT).assertIsDisplayed()
    }

    @Test
    fun `the turn is stored so leaving and returning shows it again`() =
        runTest {
            sendFirstMessage()

            composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_back)))
            composeRule.awaitText(string(ChatR.string.chat_list_title))
            composeRule.onNodeWithText(SENT_MESSAGE).assertIsDisplayed()

            composeRule.clickWhenStill(hasText(SENT_MESSAGE))
            composeRule.awaitText(REPLY_TEXT)
            composeRule.onNodeWithText(REPLY_TEXT).assertIsDisplayed()

            val chat = chatRepository.getChatsFlow().first().single()
            assertEquals(
                EXPECTED_MESSAGE_COUNT,
                chatRepository.getMessagesFlow(chat.id).first().size,
            )
        }

    @Test
    fun `a long reply renders as it streams and lands as one stored message`() =
        runTest {
            anthropic.enqueueSlowReply(
                text = LONG_REPLY_TEXT,
                chunkLength = LONG_REPLY_CHUNK_LENGTH,
                bytesPerPeriod = LONG_REPLY_BYTES_PER_PERIOD,
                periodMillis = LONG_REPLY_PERIOD_MILLIS,
            )
            launcher.launch()
            composeRule.awaitText(string(ChatR.string.chat_list_new_chat))
            composeRule.clickWhenStill(hasText(string(ChatR.string.chat_list_new_chat)))
            composeRule
                .onNodeWithText(string(ChatR.string.chat_composer_placeholder))
                .performTextInput(LONG_REPLY_SENT_MESSAGE)
            composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_send)))

            composeRule.awaitPartialText(LONG_REPLY_FIRST_CHUNK, LONG_REPLY_TEXT)
            composeRule.awaitText(LONG_REPLY_TEXT, LONG_REPLY_TIMEOUT_MILLIS)
            composeRule.awaitContentDescription(
                string(ChatR.string.chat_send),
                LONG_REPLY_TIMEOUT_MILLIS,
            )

            // One row for the whole reply, however many deltas carried it.
            val chat = chatRepository.getChatsFlow().first().single()
            val messages = chatRepository.getMessagesFlow(chat.id).first()
            assertEquals(EXPECTED_MESSAGE_COUNT, messages.size)
            assertEquals(LONG_REPLY_TEXT, messages.last().content.textContent())
        }

    @Test
    fun `deleting from the detail screen returns to the list without the chat`() =
        runTest {
            sendFirstMessage()

            composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_more)))
            composeRule.clickWhenStill(hasText(string(ChatR.string.chat_delete)))
            composeRule.clickWhenStill(hasText(string(ChatR.string.chat_delete_confirm)))

            composeRule.awaitText(string(ChatR.string.chat_list_title))
            composeRule.onNodeWithText(string(ChatR.string.chat_list_title)).assertIsDisplayed()
            assertEquals(emptyList<Chat>(), chatRepository.getChatsFlow().first())
        }
}
