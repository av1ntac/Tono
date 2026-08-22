package org.volkov.tono.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.volkov.tono.data.BUCKET_MONTH
import org.volkov.tono.data.BUCKET_WEEK
import java.time.LocalDate
import java.time.YearMonth

class MonthWindowTest {

    // --- computeMonthWindow ---

    @Test
    fun `window is the current month plus the next three`() {
        val window = computeMonthWindow(LocalDate.of(2026, 8, 22))
        assertEquals(
            listOf("2026-08", "2026-09", "2026-10", "2026-11"),
            window.map { it.toString() },
        )
    }

    @Test
    fun `window starts on the current month regardless of day of month`() {
        val first = computeMonthWindow(LocalDate.of(2026, 8, 1))
        val last = computeMonthWindow(LocalDate.of(2026, 8, 31))
        assertEquals(first, last)
    }

    @Test
    fun `window rolls over the year boundary`() {
        val window = computeMonthWindow(LocalDate.of(2026, 11, 30))
        assertEquals(
            listOf("2026-11", "2026-12", "2027-01", "2027-02"),
            window.map { it.toString() },
        )
    }

    // --- bucketOf ---

    @Test
    fun `bucket is derived from the shape of the section key`() {
        assertEquals(BUCKET_WEEK, bucketOf("2026-08-22"))
        assertEquals(BUCKET_MONTH, bucketOf("2026-08"))
        assertEquals(BUCKET_MONTH, bucketOf(LATER_KEY))
    }

    // --- monthRolloverTarget ---

    @Test
    fun `a past month collapses onto the current month`() {
        assertEquals("2026-08", monthRolloverTarget("2026-05", YearMonth.of(2026, 8)))
        assertEquals("2026-01", monthRolloverTarget("2025-12", YearMonth.of(2026, 1)))
    }

    @Test
    fun `the current and future months are left alone`() {
        assertEquals("2026-08", monthRolloverTarget("2026-08", YearMonth.of(2026, 8)))
        assertEquals("2026-11", monthRolloverTarget("2026-11", YearMonth.of(2026, 8)))
    }

    // --- monthPushForwardTarget ---

    @Test
    fun `push steps a named month on by one`() {
        assertEquals("2026-09", monthPushForwardTarget("2026-08", YearMonth.of(2026, 8)))
        assertEquals("2026-11", monthPushForwardTarget("2026-10", YearMonth.of(2026, 8)))
    }

    @Test
    fun `push from the last named month spills into later`() {
        assertEquals(LATER_KEY, monthPushForwardTarget("2026-11", YearMonth.of(2026, 8)))
    }

    @Test
    fun `push from later has nowhere to go`() {
        assertNull(monthPushForwardTarget(LATER_KEY, YearMonth.of(2026, 8)))
    }

    @Test
    fun `push from a stale month collapses onto the current month`() {
        assertEquals("2026-08", monthPushForwardTarget("2026-03", YearMonth.of(2026, 8)))
    }

    @Test
    fun `push crosses the year boundary`() {
        assertEquals("2027-01", monthPushForwardTarget("2026-12", YearMonth.of(2026, 11)))
    }

    // --- labels ---

    @Test
    fun `month label is the lowercase english month name`() {
        assertEquals("august", monthLabel("2026-08"))
        assertEquals("january", monthLabel("2027-01"))
        assertEquals(LATER_KEY, monthLabel(LATER_KEY))
    }

    @Test
    fun `short month label stays short enough for the push affordance`() {
        assertEquals("sep", monthShortLabel("2026-09"))
        assertEquals("jan", monthShortLabel("2027-01"))
        assertEquals(LATER_KEY, monthShortLabel(LATER_KEY))
        listOf("2026-01", "2026-09", "2026-12").forEach {
            assert(monthShortLabel(it).length <= 3) { "\$it label too long" }
        }
    }

    @Test
    fun `month date label is the year, and later has no date`() {
        assertEquals("2026", monthDateLabel("2026-08"))
        assertEquals("—", monthDateLabel(LATER_KEY))
    }

    // --- key ordering, which the stale-month query relies on ---

    @Test
    fun `later sorts after every month key so it is never stale`() {
        listOf("2026-01", "2026-12", "2099-12").forEach { key ->
            assert(LATER_KEY > key) { "$LATER_KEY should sort after $key" }
        }
    }
}
