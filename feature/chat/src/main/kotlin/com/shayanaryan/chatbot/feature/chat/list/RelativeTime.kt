package com.shayanaryan.chatbot.feature.chat.list

import androidx.annotation.StringRes
import com.shayanaryan.chatbot.feature.chat.R
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * A timestamp reduced to the coarsest unit that still describes it.
 *
 * Split in two so the ViewModel never touches `Resources`: it picks the unit, the composable
 * resolves the string.
 *
 * @property unitRes a feature string taking one integer, except the "now" string, which takes none
 *   and ignores the extra argument.
 * @property value how many of that unit.
 */
data class RelativeTime(
    @param:StringRes val unitRes: Int,
    val value: Int,
)

/**
 * Truncates towards the past:
 * 45s  -> now    90m -> 1h    9d  -> 1w
 * 100s -> 1m     26h -> 1d    15d -> 2w
 *
 * @param now the reading of the injected clock this label is relative to. A timestamp in the
 *   future (a device clock that moved backwards) reads as "now" rather than as a negative age.
 */
fun Instant.relativeTo(now: Instant): RelativeTime {
    val elapsed = now - this
    return when {
        elapsed < 1.minutes -> {
            RelativeTime(R.string.chat_time_now, 0)
        }

        elapsed < 1.hours -> {
            RelativeTime(
                R.string.chat_time_minutes,
                elapsed.inWholeMinutes.toInt(),
            )
        }

        elapsed < 1.days -> {
            RelativeTime(
                R.string.chat_time_hours,
                elapsed.inWholeHours.toInt(),
            )
        }

        elapsed < 7.days -> {
            RelativeTime(
                R.string.chat_time_days,
                elapsed.inWholeDays.toInt(),
            )
        }

        else -> {
            RelativeTime(R.string.chat_time_weeks, (elapsed.inWholeDays / 7).toInt())
        }
    }
}
