package com.shayanaryan.chatbot.flow

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText

private const val APPEARANCE_TIMEOUT_MILLIS = 10_000L

/**
 * Blocks until [text] is on screen, and fails the test once the timeout passes without it.
 *
 * A flow waits on work no idling resource covers: the reply crosses a real socket and the key
 * store writes on its own dispatcher, so a composition that has gone idle says nothing about
 * whether either has landed.
 */
internal fun ComposeTestRule.awaitText(text: String) {
    waitUntil(APPEARANCE_TIMEOUT_MILLIS) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}
