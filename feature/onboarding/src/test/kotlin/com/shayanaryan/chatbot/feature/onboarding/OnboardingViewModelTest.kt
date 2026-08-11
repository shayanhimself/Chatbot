package com.shayanaryan.chatbot.feature.onboarding

import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.FakeApiKeyValidator
import com.shayanaryan.chatbot.shared.chat.KeyValidationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val API_KEY = "sk-ant-api03-not-a-real-key"
private const val OTHER_API_KEY = "sk-ant-api03-also-not-real"
private const val RETRY_AFTER_SECONDS = 30

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val validator = FakeApiKeyValidator()
    private val repository = FakeApiKeyRepository()

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OnboardingViewModel(validator, repository)

    @Test
    fun `starts idle`() =
        runTest {
            assertEquals(OnboardingUiState.Idle, viewModel().uiState.value)
        }

    @Test
    fun `submitting reports the validating state while the check is in flight`() =
        runTest {
            validator.gate = CompletableDeferred()
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()

            assertEquals(OnboardingUiState.Validating, model.uiState.value)
        }

    @Test
    fun `a rejected key reports the authentication failure`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ChatError.Authentication)
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()

            assertEquals(OnboardingUiState.Failed(ChatError.Authentication), model.uiState.value)
            assertNull(repository.apiKey())
        }

    @Test
    fun `an offline attempt reports the network failure without storing anything`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ChatError.Network)
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()

            assertEquals(OnboardingUiState.Failed(ChatError.Network), model.uiState.value)
            assertNull(repository.apiKey())
        }

    @Test
    fun `a rate limited attempt reports the rate limit`() =
        runTest {
            validator.result =
                KeyValidationResult.Failed(ChatError.RateLimited(RETRY_AFTER_SECONDS))
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()

            assertEquals(
                OnboardingUiState.Failed(ChatError.RateLimited(RETRY_AFTER_SECONDS)),
                model.uiState.value,
            )
        }

    @Test
    fun `editing after a failure returns to idle`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ChatError.Authentication)
            val model = viewModel()
            model.onSubmit(API_KEY)
            advanceUntilIdle()

            model.onKeyEdited()

            assertEquals(OnboardingUiState.Idle, model.uiState.value)
        }

    @Test
    fun `editing while idle changes nothing`() =
        runTest {
            val model = viewModel()

            model.onKeyEdited()

            assertEquals(OnboardingUiState.Idle, model.uiState.value)
        }

    @Test
    fun `a valid key reaches the repository and the screen keeps spinning`() =
        runTest {
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()

            assertEquals(API_KEY, repository.apiKey())
            assertEquals(OnboardingUiState.Validating, model.uiState.value)
        }

    @Test
    fun `a second submit while validating is ignored`() =
        runTest {
            validator.gate = CompletableDeferred()
            val model = viewModel()

            model.onSubmit(API_KEY)
            advanceUntilIdle()
            model.onSubmit(OTHER_API_KEY)
            advanceUntilIdle()

            assertEquals(listOf(API_KEY), validator.validated)
        }
}
