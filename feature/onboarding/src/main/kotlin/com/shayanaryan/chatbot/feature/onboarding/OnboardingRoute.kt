package com.shayanaryan.chatbot.feature.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of onboarding.
 */
@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    OnboardingScreen(
        uiState = uiState,
        onKeyChange = viewModel::onKeyChange,
        onToggleReveal = viewModel::onToggleReveal,
        onSubmit = viewModel::onSubmit,
        onConsoleClick = { context.openConsole() },
        modifier = modifier,
    )
}

private fun Context.openConsole() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, CONSOLE_URL.toUri()))
    } catch (_: ActivityNotFoundException) {
        // A device with no browser at all. There is nothing useful the screen can say about it,
        // and the user still has every other way of getting a key.
    }
}
