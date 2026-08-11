package com.shayanaryan.chatbot.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.chat.ApiKeyValidator
import com.shayanaryan.chatbot.shared.chat.KeyValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    private val validator: ApiKeyValidator,
    private val repository: ApiKeyRepository,
) : ViewModel() {
    private val state = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    /**
     * Validates a submitted key and stores it.
     *
     * The key is a parameter and never a field: a field would keep it reachable for this object's
     * whole life and so present in any heap dump taken meanwhile, and there is no reason to hold it
     * past the call that saves it. It narrows the exposure window rather than closing it, since a
     * `String` cannot be zeroed.
     */
    fun onSubmit(key: String) {
        if (state.value == OnboardingUiState.Validating) return
        state.value = OnboardingUiState.Validating
        viewModelScope.launch {
            when (val result = validator.validate(key)) {
                KeyValidationResult.Valid -> {
                    repository.save(key)
                }

                is KeyValidationResult.Failed -> {
                    state.value = OnboardingUiState.Failed(result.error)
                }
            }
        }
    }

    /**
     * Clears a failure the moment the user starts correcting the key, so a rejected field
     * stops being red before the next attempt rather than after it.
     */
    fun onKeyEdited() {
        if (state.value is OnboardingUiState.Failed) {
            state.value = OnboardingUiState.Idle
        }
    }
}
