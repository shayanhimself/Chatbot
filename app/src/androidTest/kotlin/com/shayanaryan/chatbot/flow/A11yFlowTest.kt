package com.shayanaryan.chatbot.flow

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
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
import com.shayanaryan.chatbot.feature.onboarding.R as OnboardingR

private const val SEEDED_KEY = "sk-ant-api03-accessibility-flow"
private const val TYPED_KEY = "sk-ant-api03-typed-on-the-screen"
private const val SENT_MESSAGE = "where should I go in march"
private const val REPLY_TEXT = "Somewhere with fewer people than you expect."
private const val EXPECTED_MESSAGE_COUNT = 2

private const val KEY_ACCEPTED = 200
private const val KEY_REJECTED = 401

private const val STREAMED_REPLY_TEXT =
    "Start on the coast, where march is still quiet enough that the beaches belong to whoever " +
        "shows up, and work inland once the passes open."
private const val STREAMED_REPLY_FIRST_CHUNK = "Start on "
private const val STREAMED_CHUNK_LENGTH = 8
private const val STREAMED_BYTES_PER_PERIOD = 128L
private const val STREAMED_PERIOD_MILLIS = 400L
private const val STREAMED_TIMEOUT_MILLIS = 60_000L

// The checks read the node tree through a platform API that arrived in Android 14, below which
// the framework has nothing to hand them.
private const val ACCESSIBILITY_CHECKS_MIN_SDK = 34

/**
 * Whether every state the app puts on screen survives the Accessibility Test Framework's checks: a
 * label on everything a service can reach, text that meets contrast, tappable targets at the
 * minimum size, and a traversal order that reads the screen the way it looks.
 *
 * The checks run over the accessibility node tree the platform builds, which is what TalkBack is
 * handed, rather than over the semantics a Compose assertion asks for. That is the difference from
 * the per-component accessibility tests.
 */
@HiltAndroidTest
@UninstallModules(ApiKeyModule::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = ACCESSIBILITY_CHECKS_MIN_SDK)
class A11yFlowTest {
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
            clearChats()
            // Enabling the checks here also runs them ahead of every tap and every keystroke the
            // tests below make.
            composeRule.enableAccessibilityChecks(
                AccessibilityValidator()
                    .setRunChecksFromRootView(true)
                    .setThrowExceptionFor(AccessibilityCheckResultType.ERROR),
            )
        }

    @After
    fun tearDown() =
        runTest {
            launcher.close()
            clearChats()
        }

    @Test
    fun `the onboarding screen passes accessibility checks as a key is entered`() =
        runTest {
            launcher.launch()
            composeRule.awaitText(string(OnboardingR.string.onboarding_key_label))
            assertScreenIsAccessible()

            // A key in the field is what enables the submit button, and the enabled button is the
            // one whose colors a person is expected to read.
            composeRule.onNode(hasSetTextAction()).performTextInput(TYPED_KEY)
            composeRule.awaitText(string(OnboardingR.string.onboarding_submit))
            assertScreenIsAccessible()

            anthropic.enqueueStatus(KEY_REJECTED)
            composeRule.clickWhenStill(hasText(string(OnboardingR.string.onboarding_submit)))
            composeRule.awaitText(string(OnboardingR.string.onboarding_error_authentication))
            assertScreenIsAccessible()

            anthropic.enqueueStatus(KEY_ACCEPTED)
            composeRule.clickWhenStill(hasText(string(OnboardingR.string.onboarding_submit_retry)))
            composeRule.awaitText(string(ChatR.string.chat_new_chat_greeting))
            assertScreenIsAccessible()
        }

    @Test
    fun `the chat list passes accessibility checks empty and with a chat in it`() =
        runTest {
            apiKeyRepository.save(SEEDED_KEY)

            launcher.launch()
            composeRule.awaitText(string(ChatR.string.chat_list_empty_title))
            assertScreenIsAccessible()

            seedChat()
            composeRule.awaitText(SENT_MESSAGE)
            assertScreenIsAccessible()
        }

    @Test
    fun `a chat passes accessibility checks while a reply streams and once it settles`() =
        runTest {
            apiKeyRepository.save(SEEDED_KEY)

            launcher.launch()
            openNewChat()
            assertScreenIsAccessible()

            composeMessage(SENT_MESSAGE)
            assertScreenIsAccessible()

            anthropic.enqueueSlowReply(
                text = STREAMED_REPLY_TEXT,
                chunkLength = STREAMED_CHUNK_LENGTH,
                bytesPerPeriod = STREAMED_BYTES_PER_PERIOD,
                periodMillis = STREAMED_PERIOD_MILLIS,
            )
            sendMessage()

            // A half arrived reply is a state of its own: the text is a live region, and the
            // composer's trailing action is the stop button rather than the send button.
            composeRule.awaitPartialText(STREAMED_REPLY_FIRST_CHUNK, STREAMED_REPLY_TEXT)
            assertScreenIsAccessible()

            composeRule.awaitText(STREAMED_REPLY_TEXT, STREAMED_TIMEOUT_MILLIS)
            composeRule.awaitContentDescription(
                string(ChatR.string.chat_send),
                STREAMED_TIMEOUT_MILLIS,
            )
            assertScreenIsAccessible()
        }

    @Test
    fun `a failed turn passes accessibility checks`() =
        runTest {
            apiKeyRepository.save(SEEDED_KEY)

            launcher.launch()
            openNewChat()
            composeMessage(SENT_MESSAGE)

            anthropic.enqueueStatus(KEY_REJECTED)
            sendMessage()

            composeRule.awaitText(string(ChatR.string.chat_error_authentication))
            assertScreenIsAccessible()
        }

    @Test
    fun `the delete confirmation passes accessibility checks`() =
        runTest {
            apiKeyRepository.save(SEEDED_KEY)
            seedChat()

            launcher.launch()
            composeRule.awaitText(SENT_MESSAGE)
            composeRule.clickWhenStill(hasText(SENT_MESSAGE))
            composeRule.awaitText(string(ChatR.string.chat_composer_placeholder))

            composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_more)))
            composeRule.clickWhenStill(hasText(string(ChatR.string.chat_delete)))
            composeRule.awaitText(string(ChatR.string.chat_delete_title))
            assertScreenIsAccessible()

            composeRule.clickWhenStill(hasText(string(ChatR.string.chat_delete_cancel)))
        }

    /** Runs the checks over everything on screen, and fails the test with what they report. */
    private fun assertScreenIsAccessible() {
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private suspend fun clearChats() {
        chatRepository.getChatsFlow().first().forEach { chatRepository.delete(it.id) }
    }

    private suspend fun seedChat() {
        anthropic.enqueueReply(REPLY_TEXT)
        val chatId = chatRepository.send(chatId = null, text = SENT_MESSAGE)
        chatRepository.getMessagesFlow(chatId).first { it.size == EXPECTED_MESSAGE_COUNT }
    }

    private fun openNewChat() {
        composeRule.awaitText(string(ChatR.string.chat_list_new_chat))
        composeRule.clickWhenStill(hasText(string(ChatR.string.chat_list_new_chat)))
        composeRule.awaitText(string(ChatR.string.chat_new_chat_greeting))
    }

    private fun composeMessage(text: String) {
        composeRule
            .onNodeWithText(string(ChatR.string.chat_composer_placeholder))
            .performTextInput(text)
    }

    private fun sendMessage() {
        composeRule.clickWhenStill(hasContentDescription(string(ChatR.string.chat_send)))
    }
}
