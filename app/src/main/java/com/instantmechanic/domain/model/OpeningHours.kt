package com.instantmechanic.domain.model

import java.time.LocalDateTime

/**
 * Decides whether a weekly schedule is open at a given instant.
 *
 * Kept as a standalone pure function rather than a property on the model because it is the one
 * piece of genuinely tricky business logic in the app: a window such as 21:00 -> 05:00 is active
 * both late on its own day *and* in the early hours of the following day, so answering "is this
 * garage open now?" requires looking at yesterday's row as well as today's.
 *
 * Being pure and clock-injected makes it exhaustively unit-testable — see `OpeningHoursTest`.
 */
object OpeningHours {

    fun isOpenAt(weekly: List<DayHours>, dateTime: LocalDateTime): Boolean {
        val time = dateTime.toLocalTime()

        // 1. Today's own window.
        val today = weekly.firstOrNull { it.day == dateTime.dayOfWeek }
        if (today != null && !today.isClosed && today.opensAt != null && today.closesAt != null) {
            if (today.isOvernight) {
                // Runs to midnight and beyond; anything at or after opening is inside it.
                if (!time.isBefore(today.opensAt)) return true
            } else {
                if (!time.isBefore(today.opensAt) && time.isBefore(today.closesAt)) return true
            }
        }

        // 2. Yesterday's overnight window spilling into this morning.
        val yesterday = weekly.firstOrNull { it.day == dateTime.dayOfWeek.minus(1) }
        if (yesterday != null && yesterday.isOvernight && yesterday.closesAt != null) {
            if (time.isBefore(yesterday.closesAt)) return true
        }

        return false
    }
}
