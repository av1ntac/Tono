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
