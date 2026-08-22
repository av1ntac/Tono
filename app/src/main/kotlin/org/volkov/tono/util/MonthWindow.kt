package org.volkov.tono.util

import org.volkov.tono.data.BUCKET_MONTH
import org.volkov.tono.data.BUCKET_WEEK
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Section key for the open-ended bucket that sits after the last named month. */
const val LATER_KEY = "later"

/** Number of named months shown: the current one plus the next three. */
const val MONTH_WINDOW_SIZE = 4

/**
 * The named months of the months view: the current month plus the next three.
 * The `later` bucket is not a month and is appended separately by the caller.
 */
fun computeMonthWindow(today: java.time.LocalDate = java.time.LocalDate.now()): List<YearMonth> {
    val current = YearMonth.from(today)
    return (0 until MONTH_WINDOW_SIZE).map { current.plusMonths(it.toLong()) }
}

/**
 * Which bucket a section key belongs to, derived from its shape: `yyyy-MM-dd` is a
 * weeks-view day, `yyyy-MM` and [LATER_KEY] are months-view sections. Keeping this a
 * pure function of the key means every gesture callback can stay bucket-agnostic.
 */
fun bucketOf(sectionKey: String): String =
    if (sectionKey == LATER_KEY || sectionKey.length == 7) BUCKET_MONTH else BUCKET_WEEK

/**
 * Where a stale month's tasks land: any month before [currentMonth] collapses onto it,
 * mirroring how [rolloverTarget] carries stale days into the current window.
 */
fun monthRolloverTarget(oldMonthKey: String, currentMonth: YearMonth): String =
    if (YearMonth.parse(oldMonthKey).isBefore(currentMonth)) currentMonth.toString() else oldMonthKey

/**
 * Where a whole-month push forward lands, mirroring [pushForwardTarget] at month scale.
 * A past month collapses onto the current one, the last named month spills into [LATER_KEY],
 * and [LATER_KEY] itself has nowhere further to go (null suppresses the gesture).
 */
fun monthPushForwardTarget(monthKey: String, currentMonth: YearMonth): String? {
    if (monthKey == LATER_KEY) return null
    val next = YearMonth.parse(monthKey).plusMonths(1)
    val lastNamed = currentMonth.plusMonths((MONTH_WINDOW_SIZE - 1).toLong())
    return when {
        next.isBefore(currentMonth) -> currentMonth.toString()
        next.isAfter(lastNamed) -> LATER_KEY
        else -> next.toString()
    }
}

/** Heading label for a months-view section key, e.g. `august` or `later`. */
fun monthLabel(sectionKey: String): String =
    if (sectionKey == LATER_KEY) {
        LATER_KEY
    } else {
        YearMonth.parse(sectionKey).month
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            .lowercase()
    }

/**
 * Short label used in the push-forward affordance (`→ SEP`, `← UNDO · 3 → SEP`), where the
 * full month name would overflow the heading alongside the section's own label.
 */
fun monthShortLabel(sectionKey: String): String =
    if (sectionKey == LATER_KEY) {
        LATER_KEY
    } else {
        YearMonth.parse(sectionKey).month
            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            .lowercase()
    }

/**
 * Right-hand heading text for a months-view section: the year for a named month,
 * and an em dash for [LATER_KEY], which has no date to show.
 */
fun monthDateLabel(sectionKey: String): String =
    if (sectionKey == LATER_KEY) "—" else YearMonth.parse(sectionKey).year.toString()
