package com.shayanaryan.chatbot.feature.conversation.conversationlist

import com.shayanaryan.chatbot.feature.conversation.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val FIXED_NOW_MILLIS = 1_000_000_000L

class RelativeTimeTest {
    private val now = Instant.fromEpochMilliseconds(FIXED_NOW_MILLIS)

    private fun ago(duration: kotlin.time.Duration) = (now - duration).relativeTo(now)

    @Test
    fun `under a minute reads as now`() {
        assertEquals(RelativeTime(R.string.conversation_time_now, 0), ago(30.seconds))
    }

    @Test
    fun `minutes are whole minutes`() {
        assertEquals(RelativeTime(R.string.conversation_time_minutes, 59), ago(59.minutes))
    }

    @Test
    fun `an hour rolls over to hours`() {
        assertEquals(RelativeTime(R.string.conversation_time_hours, 2), ago(2.hours))
    }

    @Test
    fun `a day rolls over to days`() {
        assertEquals(RelativeTime(R.string.conversation_time_days, 3), ago(3.days))
    }

    @Test
    fun `a week rolls over to weeks`() {
        assertEquals(RelativeTime(R.string.conversation_time_weeks, 1), ago(7.days))
    }

    @Test
    fun `a fortnight is two weeks, not fourteen days`() {
        assertEquals(RelativeTime(R.string.conversation_time_weeks, 2), ago(15.days))
    }

    /** A device clock that moved backwards must not print a negative age. */
    @Test
    fun `a future timestamp reads as now`() {
        assertEquals(RelativeTime(R.string.conversation_time_now, 0), ago((-5).hours))
    }
}
