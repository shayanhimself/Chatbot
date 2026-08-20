package com.shayanaryan.chatbot.feature.chat.detail.component

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/** The message list's scroll position, and whether the list is following its tail. */
@Stable
internal class TailFollower(
    val listState: LazyListState,
) {
    /**
     * Whether content arriving scrolls the list to its end.
     */
    private var isFollowing by mutableStateOf(true)

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Reached by the user's own scrolls alone,
                // since a programmatic scroll dispatches nothing.
                isFollowing = !listState.canScrollForward
                return Offset.Zero
            }
        }

    /** Follows again from the next change on. */
    fun follow() {
        isFollowing = true
    }

    /**
     * Scrolls so the end of the item at [tailIndex] sits at the end of the viewport.
     */
    suspend fun scrollIfFollowing(tailIndex: Int) {
        if (!isFollowing) return
        listState.scrollToItem(tailIndex)
        val tail = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return
        // A scroll to an item aligns that item's start, which is the same place only while the item
        // fits the window: a reply longer than that would land with its newest text below the fold.
        val below = tail.offset + tail.size - listState.layoutInfo.viewportEndOffset
        if (below > 0) listState.scrollBy(below.toFloat())
    }
}

@Composable
internal fun rememberTailFollower(): TailFollower {
    val listState = rememberLazyListState()
    return remember(listState) { TailFollower(listState) }
}
