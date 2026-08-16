package org.volkov.tono.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

fun computeDayWindow(today: LocalDate = LocalDate.now()): List<LocalDate> {
    val monday = today.with(DayOfWeek.MONDAY).let {
        if (it.isAfter(today)) it.minusWeeks(1) else it
    }
    return (0..13).map { monday.plusDays(it.toLong()) }
}

/**
 * Maps a stale task's day to the same weekday in the current window's first week.
 * A task left on last Wednesday rolls forward to this window's Wednesday, preserving weekday.
 *
 * @param oldDayKey the task's stored ISO date string (e.g. "2026-08-12")
 * @param windowStart the Monday returned first by [computeDayWindow]
 */
fun rolloverTarget(oldDayKey: String, windowStart: LocalDate): LocalDate {
    val oldDate = LocalDate.parse(oldDayKey)
    return windowStart.plusDays(
        (oldDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
    )
}

fun LocalDate.dayLabel(): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "пн"
    DayOfWeek.TUESDAY -> "вт"
    DayOfWeek.WEDNESDAY -> "ср"
    DayOfWeek.THURSDAY -> "чт"
    DayOfWeek.FRIDAY -> "пт"
    DayOfWeek.SATURDAY -> "сб"
    DayOfWeek.SUNDAY -> "вс"
}

fun LocalDate.dateLabel(): String {
    val month = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()
    return "$dayOfMonth $month"
}
