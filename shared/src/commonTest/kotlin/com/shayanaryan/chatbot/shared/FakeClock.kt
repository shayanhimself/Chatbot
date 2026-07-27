package com.shayanaryan.chatbot.shared

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A clock a test moves by hand, so timestamp assertions are exact.
 *
 * @property instant what [now] reports, readable and settable directly for a test that wants to
 *   place the clock somewhere specific rather than step it with [advanceBy].
 * @property autoAdvanceBy how far [now] moves the clock after each reading. Zero — the default —
 *   leaves the clock entirely under [instant] and [advanceBy], which is what an exact assertion
 *   wants. A non-zero step makes successive writes distinguishable without a test having to say
 *   so, which is what anything asserting on ordering needs.
 */
class FakeClock(
    var instant: Instant = Instant.fromEpochMilliseconds(0),
    val autoAdvanceBy: Duration = Duration.ZERO,
) : Clock {
    override fun now(): Instant = instant.also { instant += autoAdvanceBy }

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}
