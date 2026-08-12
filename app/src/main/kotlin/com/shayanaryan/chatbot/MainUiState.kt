package com.shayanaryan.chatbot

/**
 * Whether the app knows yet what it is allowed to show.
 *
 * [Undecided] is what the splash screen is held over: it lasts only until the encrypted store
 * reports, and composing anything during it would show a frame of the wrong destination.
 */
sealed interface MainUiState {
    data object Undecided : MainUiState

    data class Decided(
        val hasApiKey: Boolean,
    ) : MainUiState
}
