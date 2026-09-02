package com.instantmechanic.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Open/closed is the one piece of genuinely fiddly logic in the app, and the overnight case is
 * where a naive implementation goes wrong: for a 21:00 -> 05:00 window, "is 02:00 inside it?"
 * depends on which *day* you are asking about. These tests pin both sides of every boundary.
 */
class OpeningHoursTest {

    @Test
    fun `inside a normal window is open`() {
        assertTrue(isOpen(dayTime(DayOfWeek.MONDAY, 14, 30)))
    }

    @Test
    fun `exactly at opening time is open`() {
        // Inclusive lower bound: a garage that says it opens at 09:00 is open at 09:00.
        assertTrue(isOpen(dayTime(DayOfWeek.MONDAY, 9, 0)))
    }

    @Test
    fun `one minute before opening is closed`() {
        assertFalse(isOpen(dayTime(DayOfWeek.MONDAY, 8, 59)))
    }

    @Test
    fun `exactly at closing time is closed`() {
        // Exclusive upper bound: turning up at 20:00 sharp for a garage that shuts at 20:00 is
        // too late, and claiming otherwise sends the user on a wasted trip.
        assertFalse(isOpen(dayTime(DayOfWeek.MONDAY, 20, 0)))
    }

    @Test
    fun `one minute before closing is open`() {
        assertTrue(isOpen(dayTime(DayOfWeek.MONDAY, 19, 59)))
    }

    @Test
    fun `a day marked closed is closed all day`() {
        assertFalse(isOpen(dayTime(DayOfWeek.SUNDAY, 14, 30)))
    }

    @Test
    fun `a missing day is treated as closed`() {
        val partialWeek = listOf(open(DayOfWeek.MONDAY, 9, 0, 20, 0))

        assertFalse(OpeningHours.isOpenAt(partialWeek, dayTime(DayOfWeek.TUESDAY, 14, 30)))
    }

    @Test
    fun `an empty schedule is closed`() {
        assertFalse(OpeningHours.isOpenAt(emptyList(), dayTime(DayOfWeek.MONDAY, 14, 30)))
    }

    // ------------------------------------------------------------ overnight windows

    @Test
    fun `late evening inside an overnight window is open`() {
        assertTrue(OpeningHours.isOpenAt(overnightWeek(), dayTime(DayOfWeek.MONDAY, 23, 30)))
    }

    @Test
    fun `early morning is open thanks to the previous day's overnight window`() {
        // 02:00 Tuesday is covered by Monday's 21:00 -> 05:00 shift, not by Tuesday's own.
        assertTrue(OpeningHours.isOpenAt(overnightWeek(), dayTime(DayOfWeek.TUESDAY, 2, 0)))
    }

    @Test
    fun `after the overnight window closes it is closed`() {
        assertFalse(OpeningHours.isOpenAt(overnightWeek(), dayTime(DayOfWeek.TUESDAY, 5, 0)))
        assertFalse(OpeningHours.isOpenAt(overnightWeek(), dayTime(DayOfWeek.TUESDAY, 11, 0)))
    }

    @Test
    fun `early morning is closed when the previous day did not run overnight`() {
        // The regression this guards: reading "02:00 < 05:00" off *today's* wrapping window would
        // wrongly report open even when yesterday was shut.
        val onlyTuesdayOvernight = listOf(
            closed(DayOfWeek.MONDAY),
            DayHours(DayOfWeek.TUESDAY, LocalTime.of(21, 0), LocalTime.of(5, 0), isClosed = false),
        )

        assertFalse(OpeningHours.isOpenAt(onlyTuesdayOvernight, dayTime(DayOfWeek.TUESDAY, 2, 0)))
    }

    @Test
    fun `an overnight window wraps correctly from Sunday into Monday`() {
        // DayOfWeek arithmetic must wrap, not throw or underflow.
        val sundayOnly = listOf(
            DayHours(DayOfWeek.SUNDAY, LocalTime.of(22, 0), LocalTime.of(6, 0), isClosed = false),
            closed(DayOfWeek.MONDAY),
        )

        assertTrue(OpeningHours.isOpenAt(sundayOnly, dayTime(DayOfWeek.MONDAY, 3, 0)))
    }

    @Test
    fun `a round-the-clock garage is always open`() {
        val allDay = DayOfWeek.entries.map {
            DayHours(it, LocalTime.of(0, 0), LocalTime.of(23, 59), isClosed = false)
        }

        assertTrue(OpeningHours.isOpenAt(allDay, dayTime(DayOfWeek.WEDNESDAY, 0, 0)))
        assertTrue(OpeningHours.isOpenAt(allDay, dayTime(DayOfWeek.WEDNESDAY, 12, 0)))
        assertTrue(OpeningHours.isOpenAt(allDay, dayTime(DayOfWeek.WEDNESDAY, 23, 58)))
    }

    // ------------------------------------------------------------ helpers

    /** Mon-Sat 09:00-20:00, closed Sunday. */
    private fun standardWeek(): List<DayHours> = DayOfWeek.entries.map { day ->
        if (day == DayOfWeek.SUNDAY) closed(day) else open(day, 9, 0, 20, 0)
    }

    /** Every night 21:00 -> 05:00. */
    private fun overnightWeek(): List<DayHours> = DayOfWeek.entries.map { day ->
        DayHours(day, LocalTime.of(21, 0), LocalTime.of(5, 0), isClosed = false)
    }

    private fun isOpen(at: LocalDateTime) = OpeningHours.isOpenAt(standardWeek(), at)

    private fun open(day: DayOfWeek, oh: Int, om: Int, ch: Int, cm: Int) =
        DayHours(day, LocalTime.of(oh, om), LocalTime.of(ch, cm), isClosed = false)

    private fun closed(day: DayOfWeek) = DayHours(day, null, null, isClosed = true)

    /** A [LocalDateTime] in the first week of 2024, where 1 Jan was a Monday. */
    private fun dayTime(day: DayOfWeek, hour: Int, minute: Int): LocalDateTime =
        LocalDateTime.of(2024, 1, day.value, hour, minute)
}
