package com.instantmechanic.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

/**
 * Opening hours for a single weekday.
 *
 * [opensAt]/[closesAt] are null when [isClosed] is true. A garage that closes past midnight
 * (e.g. a towing desk running 21:00 -> 05:00) is represented with [closesAt] <= [opensAt];
 * deciding whether such a window is currently active needs to know about the *previous* day
 * too, so that logic lives in [OpeningHours] rather than on this type.
 */
data class DayHours(
    val day: DayOfWeek,
    val opensAt: LocalTime?,
    val closesAt: LocalTime?,
    val isClosed: Boolean,
) {
    /** True when this day's window runs past midnight into the next day. */
    val isOvernight: Boolean
        get() = !isClosed && opensAt != null && closesAt != null && closesAt <= opensAt

    /** Human label such as "9:00 AM - 8:00 PM" or "Closed". */
    val displayRange: String
        get() = if (isClosed || opensAt == null || closesAt == null) {
            "Closed"
        } else {
            "${opensAt.toDisplay()} - ${closesAt.toDisplay()}"
        }

    /** Short weekday label such as "Mon". */
    val dayLabel: String
        get() = day.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() }.take(3)

    private fun LocalTime.toDisplay(): String {
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val suffix = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(Locale.US, hour12, minute, suffix)
    }
}
