package com.instantmechanic.data.mapper

import com.instantmechanic.data.remote.dto.WorkingHoursDto
import com.instantmechanic.domain.model.DayHours
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns the wire form of a weekly schedule into domain [DayHours].
 *
 * Shared by [MechanicMapper] and the mock backend so both agree on what "open" means — the
 * `openNow` filter applied server-side and the Open/Closed pill drawn in the UI are computed
 * from exactly the same parse.
 */
object WorkingHoursParser {

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun parse(dtos: List<WorkingHoursDto>): List<DayHours> = dtos.mapNotNull(::parseDay)

    private fun parseDay(dto: WorkingHoursDto): DayHours? {
        val day = parseDayOfWeek(dto.day) ?: return null
        val opens = parseTime(dto.opensAt)
        val closes = parseTime(dto.closesAt)
        val closed = dto.closed || opens == null || closes == null
        return DayHours(
            day = day,
            opensAt = if (closed) null else opens,
            closesAt = if (closed) null else closes,
            isClosed = closed,
        )
    }

    private fun parseDayOfWeek(raw: String): DayOfWeek? = runCatching {
        DayOfWeek.valueOf(raw.trim().uppercase(Locale.US))
    }.getOrNull()

    private fun parseTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalTime.parse(raw.trim(), TIME_FORMAT) }.getOrNull()
    }
}
