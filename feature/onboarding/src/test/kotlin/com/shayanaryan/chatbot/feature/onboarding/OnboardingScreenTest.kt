package com.shayanaryan.chatbot.feature.onboarding

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.testing.string
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.shared.chat.ChatError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// One character short of MIN_KEY_LENGTH, so the boundary is testable from both sides.
private const val TOO_SHORT_KEY = "sk-ant-api0"
private const val ONE_MORE_CHARACTER = "3"
private const val VALID_LENGTH_KEY = "sk-ant-api03"

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var submitted: String? = null
    private var edits = 0

    private fun setScreen(uiState: OnboardingUiState = OnboardingUiState.Idle) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                OnboardingScreen(
                    uiState = uiState,
                    onSubmit = { submitted = it },
                    onKeyEdited = { edits++ },
                    onConsoleClick = {},
                )
            }
        }
    }

    @Test
    fun `submit is disabled below the length threshold and enabled at it`() {
        setScreen()

        composeRule
            .onNodeWithText(string(R.string.onboarding_submit_empty))
            .assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput(TOO_SHORT_KEY)
        composeRule
            .onNodeWithText(string(R.string.onboarding_submit_empty))
            .assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextInput(ONE_MORE_CHARACTER)

        composeRule.onNodeWithText(string(R.string.onboarding_submit)).assertIsEnabled()
    }

    @Test
    fun `submitting reports the typed key`() {
        setScreen()
        composeRule.onNode(hasSetTextAction()).performTextInput(VALID_LENGTH_KEY)

        composeRule.onNodeWithText(string(R.string.onboarding_submit)).performClick()

        assertEquals(VALID_LENGTH_KEY, submitted)
    }

    @Test
    fun `the reveal toggle flips both ways`() {
        setScreen()
        composeRule.onNode(hasSetTextAction()).performTextInput(VALID_LENGTH_KEY)

        composeRule
            .onNodeWithContentDescription(
                string(R.string.onboarding_key_reveal),
            ).performClick()
        composeRule
            .onNodeWithContentDescription(
                string(R.string.onboarding_key_hide),
            ).assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription(
                string(R.string.onboarding_key_hide),
            ).performClick()
        composeRule
            .onNodeWithContentDescription(
                string(R.string.onboarding_key_reveal),
            ).assertIsDisplayed()
    }

    @Test
    fun `the field is disabled while validating`() {
        setScreen(OnboardingUiState.Validating)

        // A disabled text field carries no set-text action at all, so the absence of an editable
        // node is what disabled looks like from here. Asserting on the field's own node instead
        // would find nothing and never reach the assertion.
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        composeRule.onNodeWithText(string(R.string.onboarding_key_label)).assertIsNotEnabled()
        composeRule
            .onNodeWithText(string(R.string.onboarding_key_helper_validating))
            .assertIsDisplayed()
    }

    @Test
    fun `a rejected key shows its message and offers a retry`() {
        setScreen(OnboardingUiState.Failed(ChatError.Authentication))
        composeRule.onNode(hasSetTextAction()).performTextInput(VALID_LENGTH_KEY)

        composeRule
            .onNodeWithText(string(R.string.onboarding_error_authentication))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_submit_retry)).assertIsEnabled()
    }

    @Test
    fun `an offline failure shows the offline message and stays retryable`() {
        setScreen(OnboardingUiState.Failed(ChatError.Network))
        composeRule.onNode(hasSetTextAction()).performTextInput(VALID_LENGTH_KEY)

        composeRule.onNodeWithText(string(R.string.onboarding_error_network)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.onboarding_submit_retry)).performClick()

        assertEquals(VALID_LENGTH_KEY, submitted)
    }

    @Test
    fun `typing after a failure reports the edit`() {
        setScreen(OnboardingUiState.Failed(ChatError.Authentication))

        composeRule.onNode(hasSetTextAction()).performTextInput(VALID_LENGTH_KEY)

        assertTrue(edits > 0)
    }
}
