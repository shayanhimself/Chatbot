package com.shayanaryan.chatbot.shared

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock a test moves by hand, so timestamp assertions are exact. */
class FakeClock(
    var instant: Instant = Instant.fromEpochMilliseconds(0),
) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}
