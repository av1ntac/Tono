package org.volkov.tono.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A task lives in the weeks view (a calendar day) or the months view (a month or "later"). */
const val BUCKET_WEEK = "week"
const val BUCKET_MONTH = "month"

/**
 * @param dayKey the section this task sits in: an ISO date (`2026-08-24`) when [bucket] is
 *   [BUCKET_WEEK], an ISO year-month (`2026-08`) or `later` when it is [BUCKET_MONTH].
 * @param createdAt the epoch day this task first entered the list. Survives every move, and is
 *   inherited by a retyped near-duplicate via [TaskHistory], so age reflects the *task*, not the row.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val dayKey: String,
    val position: Int,
    val text: String,
    val bucket: String = BUCKET_WEEK,
    val createdAt: Long = 0L
)
