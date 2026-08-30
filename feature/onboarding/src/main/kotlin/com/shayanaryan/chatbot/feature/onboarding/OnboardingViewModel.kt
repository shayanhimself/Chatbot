package com.shayanaryan.chatbot.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.claude.ApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.KeyValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val validator: ApiKeyValidator,
        private val repository: ApiKeyRepository,
    ) : ViewModel() {
        private val state = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

        /**
         * Validates the typed key and stores it.
         *
         * Does nothing below [MIN_KEY_LENGTH] or while a check is already in flight, so neither the
         * keyboard's action key nor a double tap can spend a second round trip.
         */
        fun onSubmit() {
            val submitted = state.value
            if (!submitted.submittable) return
            if (submitted.status == OnboardingStatus.Validating) return
            state.update { it.copy(status = OnboardingStatus.Validating) }
            viewModelScope.launch {
                when (val result = validator.validate(submitted.key)) {
                    KeyValidationResult.Valid -> {
                        repository.save(submitted.key)
                    }

                    is KeyValidationResult.Failed -> {
                        state.update { it.copy(status = OnboardingStatus.Failed(result.error)) }
                    }
                }
            }
        }

        /**
         * Takes an edited key.
         *
         * Editing also clears a failure, so a rejected field stops being red as soon as the user
         * starts correcting it rather than only on the next attempt.
         */
        fun onKeyChange(key: String) {
            state.update {
                val newStatus =
                    if (it.status is OnboardingStatus.Failed) OnboardingStatus.Idle else it.status
                it.copy(key = key, status = newStatus)
            }
        }

        /** Shows the key as typed rather than masked, or masks it again. */
        fun onToggleReveal() {
            state.update { it.copy(revealed = !it.revealed) }
        }
    }
