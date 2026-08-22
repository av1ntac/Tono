package org.volkov.tono.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * When a piece of task text was first seen, kept independently of the rows that carry it.
 *
 * A [Task] row does not survive every way a task can travel — retyping a month task onto a day,
 * deleting and re-adding a line, rewording it — so the age clock would restart each time. This
 * table remembers the text itself, and a newly created task inherits [firstSeen] from the closest
 * matching entry (see `util/TaskSimilarity.kt`).
 *
 * An entry is dropped when the task is actually *completed*: finishing something ends its clock,
 * so writing the same line again next month starts a genuinely new task.
 *
 * @param normalized `normalizeTaskText()` of the line — the identity used for lookups.
 * @param text the last spelling seen, kept for readability when inspecting the table.
 * @param firstSeen epoch day the text (or a near-match of it) first appeared.
 * @param lastSeen epoch day it was last written, used to prune entries nobody revisits.
 */
@Entity(tableName = "task_history")
data class TaskHistory(
    @PrimaryKey val normalized: String,
    val text: String,
    val firstSeen: Long,
    val lastSeen: Long
)
