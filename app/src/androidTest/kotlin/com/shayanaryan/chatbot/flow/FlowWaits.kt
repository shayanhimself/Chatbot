package com.shayanaryan.chatbot.flow

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick

private const val APPEARANCE_TIMEOUT_MILLIS = 10_000L
private const val SETTLE_TIMEOUT_MILLIS = 5_000L
private const val STILL_MILLIS = 300L

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

/**
 * Clicks the node [matcher] finds, once it has stayed still in the same place for [STILL_MILLIS].
 *
 * Focusing a field opens the soft keyboard, and the inset animation that follows moves the layout
 * under the test for several frames. Nothing reports that motion, since the composition is idle
 * throughout, so a tap sent during it is injected where the target was when it was aimed and lands
 * beside it, leaving the typed text in the field and nothing sent.
 *
 * Waiting for the node to stop moving ends when the animation does rather than after a guessed
 * number of milliseconds, and both the position within the window and the window's own position
 * count, because the keyboard can either resize the layout or slide the whole window.
 */
internal fun ComposeTestRule.clickWhenStill(matcher: SemanticsMatcher) {
    val node = onNode(matcher)
    var previous: Pair<Any, Any>? = null
    var unchangedSince = 0L
    waitUntil(SETTLE_TIMEOUT_MILLIS) {
        val semantics = node.fetchSemanticsNode()
        val place = semantics.boundsInRoot to semantics.positionOnScreen
        val now = SystemClock.uptimeMillis()
        if (place != previous) {
            previous = place
            unchangedSince = now
            false
        } else {
            now - unchangedSince >= STILL_MILLIS
        }
    }
    node.performClick()
}
