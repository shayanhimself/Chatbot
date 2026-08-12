package com.shayanaryan.chatbot

import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
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

private const val API_KEY = "sk-ant-api03-not-a-real-key"

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the gate is undecided until the store answers`() =
        runTest {
            val viewModel = MainViewModel(FakeApiKeyRepository())

            assertEquals(MainUiState.Undecided, viewModel.uiState.value)
        }

    @Test
    fun `an empty store decides the gate closed`() =
        runTest {
            val viewModel = MainViewModel(FakeApiKeyRepository())

            advanceUntilIdle()

            assertEquals(MainUiState.Decided(hasApiKey = false), viewModel.uiState.value)
        }

    @Test
    fun `a stored key decides the gate open`() =
        runTest {
            val viewModel = MainViewModel(FakeApiKeyRepository(initialKey = API_KEY))

            advanceUntilIdle()

            assertEquals(MainUiState.Decided(hasApiKey = true), viewModel.uiState.value)
        }

    @Test
    fun `storing a key later flips the decision`() =
        runTest {
            val repository = FakeApiKeyRepository()
            val viewModel = MainViewModel(repository)
            advanceUntilIdle()

            repository.save(API_KEY)
            advanceUntilIdle()

            assertEquals(MainUiState.Decided(hasApiKey = true), viewModel.uiState.value)
        }
}
