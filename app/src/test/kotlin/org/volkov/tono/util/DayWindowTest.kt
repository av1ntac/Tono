package org.volkov.tono.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure-JVM tests for the date-window math. No Android framework, no emulator —
 * these run under `./gradlew testDebugUnitTest` in milliseconds.
 */
class DayWindowTest {

    // --- computeDayWindow -------------------------------------------------

    @Test
    fun `window is always 14 consecutive days`() {
        val window = computeDayWindow(LocalDate.of(2026, 8, 16)) // a Sunday
        assertEquals(14, window.size)
        window.zipWithNext { a, b ->
            assertEquals("dates must be consecutive", a.plusDays(1), b)
        }
    }

    @Test
    fun `window always starts on a Monday`() {
        // Sweep every day of a fortnight; the first cell must be a Monday in all cases.
        var day = LocalDate.of(2026, 8, 10) // Monday
        repeat(14) {
            val first = computeDayWindow(day).first()
            assertEquals("start of window for $day", DayOfWeek.MONDAY, first.dayOfWeek)
            day = day.plusDays(1)
        }
    }

    @Test
    fun `today always falls inside the window`() {
        var day = LocalDate.of(2026, 1, 1)
        repeat(400) { // more than a year, crossing month + year boundaries
            assertTrue("$day should be inside its own window", day in computeDayWindow(day))
            day = day.plusDays(1)
        }
    }

    @Test
    fun `Monday input starts the window on that same Monday`() {
        val monday = LocalDate.of(2026, 8, 10)
        assertEquals(monday, computeDayWindow(monday).first())
    }

    @Test
    fun `Sunday input keeps that Sunday as the last day of week one`() {
        val sunday = LocalDate.of(2026, 8, 16)
        val window = computeDayWindow(sunday)
        assertEquals(LocalDate.of(2026, 8, 10), window.first()) // preceding Monday
        assertEquals(sunday, window[6])                          // Sunday is index 6
    }

    @Test
    fun `Friday input resolves to the surrounding Monday-to-Monday fortnight`() {
        val friday = LocalDate.of(2026, 8, 14)
        val window = computeDayWindow(friday)
        assertEquals(LocalDate.of(2026, 8, 10), window.first())
        assertEquals(friday, window[4])
        assertEquals("second week starts one week later", window.first().plusWeeks(1), window[7])
    }

    @Test
    fun `window spanning a month boundary stays consecutive`() {
        val window = computeDayWindow(LocalDate.of(2026, 1, 29)) // Jan/Feb boundary
        assertEquals(14, window.size)
        assertTrue(window.contains(LocalDate.of(2026, 2, 1)))
        window.zipWithNext { a, b -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun `window spanning a year boundary stays consecutive and Monday-aligned`() {
        val window = computeDayWindow(LocalDate.of(2025, 12, 31)) // Wednesday
        assertEquals(DayOfWeek.MONDAY, window.first().dayOfWeek)
        assertTrue(window.contains(LocalDate.of(2026, 1, 1)))
        window.zipWithNext { a, b -> assertEquals(a.plusDays(1), b) }
    }

    // --- rolloverTarget ---------------------------------------------------

    @Test
    fun `rollover preserves the weekday within the current window`() {
        val windowStart = LocalDate.of(2026, 8, 10) // Monday
        // A task stranded on last week's Wednesday lands on this window's Wednesday.
        assertEquals(
            LocalDate.of(2026, 8, 12),
            rolloverTarget("2026-08-05", windowStart),
        )
    }

    @Test
    fun `rollover maps Monday to the window start and Sunday to start plus six`() {
        val windowStart = LocalDate.of(2026, 8, 10)
        assertEquals(windowStart, rolloverTarget("2026-07-27", windowStart))          // Monday
        assertEquals(windowStart.plusDays(6), rolloverTarget("2026-07-26", windowStart)) // Sunday
    }

    @Test
    fun `rollover ignores how many weeks old the task is`() {
        val windowStart = LocalDate.of(2026, 8, 10)
        // Both are Thursdays, three weeks apart — both land on this window's Thursday.
        val recentThu = rolloverTarget("2026-08-06", windowStart)
        val ancientThu = rolloverTarget("2026-07-16", windowStart)
        assertEquals(LocalDate.of(2026, 8, 13), recentThu)
        assertEquals(recentThu, ancientThu)
    }

    // --- labels -----------------------------------------------------------

    @Test
    fun `dayLabel returns the Russian abbreviation for every weekday`() {
        val monday = LocalDate.of(2026, 8, 10)
        val expected = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
        expected.forEachIndexed { i, label ->
            assertEquals(label, monday.plusDays(i.toLong()).dayLabel())
        }
    }

    @Test
    fun `dateLabel is day-of-month plus lowercase English month`() {
        assertEquals("16 aug", LocalDate.of(2026, 8, 16).dateLabel())
        assertEquals("1 jan", LocalDate.of(2026, 1, 1).dateLabel())
        assertEquals("31 dec", LocalDate.of(2025, 12, 31).dateLabel())
    }
}
