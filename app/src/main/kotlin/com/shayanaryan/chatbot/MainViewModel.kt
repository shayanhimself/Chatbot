package com.shayanaryan.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The app-wide gate. It reads only whether a key exists, never the key itself, so collecting it
 * for the process's whole life costs no decryption.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        repository: ApiKeyRepository,
    ) : ViewModel() {
        val uiState: StateFlow<MainUiState> =
            repository
                .hasKeyFlow()
                .map<Boolean, MainUiState> { MainUiState.Decided(hasApiKey = it) }
                .stateIn(
                    scope = viewModelScope,
                    // Eagerly, so the store is already being read while the activity is still
                    // setting up and the gate is more likely to be decided by the first frame.
                    // WhileSubscribed would also resolve, one composition later, since the splash
                    // is held over the Undecided value it reads before anything subscribes.
                    started = SharingStarted.Eagerly,
                    initialValue = MainUiState.Undecided,
                )
    }
