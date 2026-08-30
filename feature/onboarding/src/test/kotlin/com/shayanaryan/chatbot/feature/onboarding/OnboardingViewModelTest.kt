package com.shayanaryan.chatbot.feature.onboarding

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import com.shayanaryan.chatbot.shared.claude.FakeApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.KeyValidationResult
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val API_KEY = "sk-ant-api03-not-a-real-key"
private const val OTHER_API_KEY = "sk-ant-api03-also-not-real"

// One character short of MIN_KEY_LENGTH, so the boundary is testable from both sides.
private const val TOO_SHORT_KEY = "sk-ant-api0"
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
    fun `starts empty and idle`() =
        runTest {
            assertEquals(OnboardingUiState(), viewModel().uiState.value)
        }

    @Test
    fun `typing reports the key back`() =
        runTest {
            val model = viewModel()

            model.onKeyChange(API_KEY)

            assertEquals(API_KEY, model.uiState.value.key)
        }

    @Test
    fun `a key reaching the minimum length becomes submittable`() =
        runTest {
            val model = viewModel()

            model.onKeyChange(TOO_SHORT_KEY)
            assertFalse(model.uiState.value.submittable)

            model.onKeyChange(API_KEY)

            assertTrue(model.uiState.value.submittable)
        }

    @Test
    fun `the reveal toggle flips both ways`() =
        runTest {
            val model = viewModel()

            model.onToggleReveal()
            assertTrue(model.uiState.value.revealed)

            model.onToggleReveal()

            assertFalse(model.uiState.value.revealed)
        }

    @Test
    fun `a key too short to be worth a round trip is not validated`() =
        runTest {
            val model = viewModel()
            model.onKeyChange(TOO_SHORT_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(emptyList(), validator.validated)
            assertEquals(OnboardingStatus.Idle, model.uiState.value.status)
        }

    @Test
    fun `submitting reports the validating state while the check is in flight`() =
        runTest {
            validator.gate = CompletableDeferred()
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(OnboardingStatus.Validating, model.uiState.value.status)
        }

    @Test
    fun `a rejected key reports the authentication failure`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ApiError.Authentication)
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(
                OnboardingStatus.Failed(ApiError.Authentication),
                model.uiState.value.status,
            )
            assertNull(repository.apiKey())
        }

    @Test
    fun `a rejected key stays in the field so it can be corrected`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ApiError.Authentication)
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(API_KEY, model.uiState.value.key)
        }

    @Test
    fun `an offline attempt reports the network failure without storing anything`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ApiError.Network)
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(OnboardingStatus.Failed(ApiError.Network), model.uiState.value.status)
            assertNull(repository.apiKey())
        }

    @Test
    fun `a rate limited attempt reports the rate limit`() =
        runTest {
            validator.result =
                KeyValidationResult.Failed(ApiError.RateLimited(RETRY_AFTER_SECONDS))
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(
                OnboardingStatus.Failed(ApiError.RateLimited(RETRY_AFTER_SECONDS)),
                model.uiState.value.status,
            )
        }

    @Test
    fun `editing after a failure returns to idle`() =
        runTest {
            validator.result = KeyValidationResult.Failed(ApiError.Authentication)
            val model = viewModel()
            model.onKeyChange(API_KEY)
            model.onSubmit()
            advanceUntilIdle()

            model.onKeyChange(OTHER_API_KEY)

            assertEquals(OnboardingStatus.Idle, model.uiState.value.status)
        }

    @Test
    fun `a valid key reaches the repository and the screen keeps spinning`() =
        runTest {
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()

            assertEquals(API_KEY, repository.apiKey())
            assertEquals(OnboardingStatus.Validating, model.uiState.value.status)
        }

    @Test
    fun `a second submit while validating is ignored`() =
        runTest {
            validator.gate = CompletableDeferred()
            val model = viewModel()
            model.onKeyChange(API_KEY)

            model.onSubmit()
            advanceUntilIdle()
            model.onKeyChange(OTHER_API_KEY)
            model.onSubmit()
            advanceUntilIdle()

            assertEquals(listOf(API_KEY), validator.validated)
        }

    @Test
    fun `the state does not print the key`() =
        runTest {
            val model = viewModel()
            model.onKeyChange(API_KEY)

            assertFalse(
                model.uiState.value
                    .toString()
                    .contains(API_KEY),
            )
        }
}
