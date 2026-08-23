package com.shayanaryan.chatbot.flow

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
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
internal fun ComposeTestRule.awaitText(
    text: String,
    timeoutMillis: Long = APPEARANCE_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Blocks until a node described as [description] is on screen.
 *
 * The composer's trailing action is what reports a turn ending: it is the stop action while a
 * reply streams and the send action once the turn is over, which is after the reply is stored.
 * The reply's own text appears earlier, as soon as the last delta lands.
 */
internal fun ComposeTestRule.awaitContentDescription(
    description: String,
    timeoutMillis: Long = APPEARANCE_TIMEOUT_MILLIS,
) {
    waitUntil(timeoutMillis) {
        onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Blocks until a reply is on screen that starts with [prefix] and is not yet [whole].
 *
 * That pair is what a partly arrived reply looks like, and the only evidence from outside the app
 * that its text was rendered as it accumulated rather than in one piece.
 */
internal fun ComposeTestRule.awaitPartialText(
    prefix: String,
    whole: String,
) {
    waitUntil(APPEARANCE_TIMEOUT_MILLIS) {
        onAllNodesWithText(prefix, substring = true).fetchSemanticsNodes().isNotEmpty() &&
            onAllNodesWithText(whole).fetchSemanticsNodes().isEmpty()
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
