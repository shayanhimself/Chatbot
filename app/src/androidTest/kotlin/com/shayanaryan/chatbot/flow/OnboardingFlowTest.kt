package com.shayanaryan.chatbot.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.di.ApiKeyModule
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.TestApiKeyStore
import com.shayanaryan.chatbot.wire.LocalAnthropic
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.shayanaryan.chatbot.feature.chat.R as ChatR
import com.shayanaryan.chatbot.feature.onboarding.R as OnboardingR

private const val VALID_KEY = "sk-ant-api03-onboarding-flow"
private const val API_KEY_HEADER = "x-api-key"
private const val MODELS_OK = 200

/**
 * Whether a key typed on the onboarding screen reaches the encrypted store and moves the app off
 * onboarding.
 *
 * Every layer between the field and the store is the real one, including the validator's HTTP call,
 * so this fails when the seams between them are wrong rather than when one of them is.
 */
@HiltAndroidTest
@UninstallModules(ApiKeyModule::class)
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
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
    val repository: ApiKeyRepository = keyStore.repository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        launcher.close()
    }

    @Test
    fun `a validated key stores itself and opens a chat`() =
        runTest {
            anthropic.enqueueStatus(MODELS_OK)
            launcher.launch()

            composeRule.onNode(hasSetTextAction()).performTextInput(VALID_KEY)
            composeRule.clickWhenStill(hasText(string(OnboardingR.string.onboarding_submit)))

            // Storing a key lands on a new chat, with the list underneath it.
            val greeting = string(ChatR.string.chat_new_chat_greeting)
            composeRule.awaitText(greeting)
            composeRule.onNodeWithText(greeting).assertIsDisplayed()
            assertEquals(VALID_KEY, repository.apiKey())
            assertEquals(VALID_KEY, anthropic.lastRequest().headers[API_KEY_HEADER])
        }

    @Test
    fun `a stored key skips onboarding on the next launch`() =
        runTest {
            repository.save(VALID_KEY)

            launcher.launch()

            val chatList = string(ChatR.string.chat_list_title)
            composeRule.awaitText(chatList)
            composeRule.onNodeWithText(chatList).assertIsDisplayed()
        }
}
