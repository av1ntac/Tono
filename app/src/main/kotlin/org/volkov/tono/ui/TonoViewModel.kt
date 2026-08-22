package org.volkov.tono.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.volkov.tono.data.BUCKET_MONTH
import org.volkov.tono.data.Task
import org.volkov.tono.data.TonoDatabase
import org.volkov.tono.util.LATER_KEY
import org.volkov.tono.util.bucketOf
import org.volkov.tono.util.computeDayWindow
import org.volkov.tono.util.computeMonthWindow
import org.volkov.tono.util.dateLabel
import org.volkov.tono.util.dayLabel
import org.volkov.tono.util.monthDateLabel
import org.volkov.tono.util.monthLabel
import org.volkov.tono.util.monthPushForwardTarget
import org.volkov.tono.util.monthShortLabel
import org.volkov.tono.util.monthRolloverTarget
import org.volkov.tono.util.pushForwardTarget
import org.volkov.tono.util.rolloverTarget
import org.volkov.tono.util.splitPastedLines
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** The two views of the same task list: the 14-day window, and the month buckets. */
enum class TonoScreenKind { WEEKS, MONTHS }

data class TaskItem(val id: String, val text: String)
data class GhostItem(val id: String, val text: String)
data class EditingState(
    val dayKey: String,
    val value: String,
    val taskId: String? = null,
    val isNew: Boolean = true,
)
data class SwipeState(val dx: Float, val snapping: Boolean)
data class DragState(
    val taskId: String,
    val fromDay: String,
    val x: Float,
    val y: Float,
    val overDay: String,
)

/** A whole-section push that has happened but is still inside its undo window. */
data class PendingPush(val toDayKey: String, val toLabel: String, val count: Int)

/**
 * One heading-plus-rows section. The weeks view fills these with calendar days, the months
 * view with months and the `later` bucket; both render through the same `DaySection`.
 */
data class DayUiState(
    val dayKey: String,
    val label: String,
    val dateLabel: String,
    /** Marks the "you are here" section — today, or the current month — for the accent border and cursor. */
    val isCurrent: Boolean,
    /** Suffix shown after the heading label on the current section, e.g. `TODAY`. */
    val currentLabel: String?,
    val isWeekend: Boolean,
    val tasks: List<TaskItem>,
    val ghosts: List<GhostItem>,
    /** Label this section's tasks would move to, or null when that target is outside the window. */
    val pushTargetLabel: String?,
    val pendingPush: PendingPush?,
    /** Separator rendered above this section, e.g. `NEXT WEEK`. */
    val dividerLabel: String? = null,
)

data class TonoUiState(
    val screen: TonoScreenKind = TonoScreenKind.WEEKS,
    val days: List<DayUiState> = emptyList(),
    val months: List<DayUiState> = emptyList(),
    val editing: EditingState? = null,
    val swipe: Map<String, SwipeState> = emptyMap(),
    val drag: DragState? = null,
) {
    /** The section list the active screen renders. */
    val sections: List<DayUiState>
        get() = if (screen == TonoScreenKind.WEEKS) days else months
}

private const val AUTOSAVE_DELAY_MS = 600L

/** Undo window for a whole-section push — matches the per-task ghost TTL. */
private const val PUSH_UNDO_MS = 6_500L

class TonoViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = TonoDatabase.getInstance(app).taskDao()

    private val _uiState = MutableStateFlow(TonoUiState())
    val uiState: StateFlow<TonoUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()
    private val dayWindow = computeDayWindow(today)
    private val currentMonth = YearMonth.from(today)
    private val monthWindow = computeMonthWindow(today)

    /** The Monday the second week of [dayWindow] starts on — where the week divider goes. */
    private val nextMonday = dayWindow.first().plusWeeks(1)

    private val ghostMap = mutableMapOf<String, GhostItem>()
    private val ghostJobs = mutableMapOf<String, Job>()
    private val ghostsByDay = mutableMapOf<String, MutableList<GhostItem>>()

    /** Pre-move rows of a pushed section, kept so [undoPush] can restore dayKey and position exactly. */
    private class PushRecord(val toDayKey: String, val toLabel: String, val original: List<Task>)

    private val pushRecords = mutableMapOf<String, PushRecord>()
    private val pushJobs = mutableMapOf<String, Job>()

    private var autosaveJob: Job? = null

    /** Latest DB snapshot, kept so ghost and push changes can rebuild both section lists. */
    private var latestTasks: List<Task> = emptyList()

    init {
        rebuildSections()
        viewModelScope.launch {
            rolloverStaleTasks()
            dao.observeAll().collect { tasks ->
                latestTasks = tasks
                rebuildSections()
            }
        }
    }

    /**
     * Carries tasks that scrolled out of their window forward: days out of [dayWindow] land on the
     * same weekday in the current window, months before [currentMonth] collapse onto it.
     */
    private suspend fun rolloverStaleTasks() {
        val windowStart = dayWindow.first()
        dao.getTasksBefore(windowStart.toString()).forEach { task ->
            val newDayKey = rolloverTarget(task.dayKey, windowStart).toString()
            val pos = (dao.maxPosition(newDayKey) ?: -1) + 1
            dao.update(task.copy(dayKey = newDayKey, position = pos))
        }
        dao.getMonthTasksBefore(currentMonth.toString()).forEach { task ->
            val newMonthKey = monthRolloverTarget(task.dayKey, currentMonth)
            if (newMonthKey == task.dayKey) return@forEach
            val pos = (dao.maxPosition(newMonthKey) ?: -1) + 1
            dao.update(task.copy(dayKey = newMonthKey, position = pos))
        }
    }

    private fun rebuildSections() {
        val ghostIds = ghostMap.keys
        val tasksByKey = latestTasks
            .filter { it.id !in ghostIds }
            .groupBy { it.dayKey }
            .mapValues { (_, rows) -> rows.map { TaskItem(it.id, it.text) } }

        _uiState.update { state ->
            state.copy(
                days = buildDayList(tasksByKey),
                months = buildMonthList(tasksByKey),
            )
        }
    }

    private fun buildDayList(tasksByKey: Map<String, List<TaskItem>>): List<DayUiState> =
        dayWindow.map { date ->
            val key = date.toString()
            section(
                key = key,
                label = date.dayLabel(),
                dateLabel = date.dateLabel(),
                isCurrent = date == today,
                currentLabel = "TODAY",
                isWeekend = date.dayOfWeek.value >= DayOfWeek.SATURDAY.value,
                tasksByKey = tasksByKey,
                dividerLabel = "NEXT WEEK".takeIf { date == nextMonday },
            )
        }

    private fun buildMonthList(tasksByKey: Map<String, List<TaskItem>>): List<DayUiState> =
        (monthWindow.map { it.toString() } + LATER_KEY).map { key ->
            section(
                key = key,
                label = monthLabel(key),
                dateLabel = monthDateLabel(key),
                isCurrent = key == currentMonth.toString(),
                currentLabel = "THIS MONTH",
                isWeekend = false,
                tasksByKey = tasksByKey,
                dividerLabel = "LATER".takeIf { key == LATER_KEY },
            )
        }

    private fun section(
        key: String,
        label: String,
        dateLabel: String,
        isCurrent: Boolean,
        currentLabel: String,
        isWeekend: Boolean,
        tasksByKey: Map<String, List<TaskItem>>,
        dividerLabel: String?,
    ): DayUiState {
        val record = pushRecords[key]
        return DayUiState(
            dayKey = key,
            label = label,
            dateLabel = dateLabel,
            isCurrent = isCurrent,
            currentLabel = currentLabel.takeIf { isCurrent },
            isWeekend = isWeekend,
            tasks = tasksByKey[key] ?: emptyList(),
            ghosts = ghostsByDay[key] ?: emptyList(),
            pushTargetLabel = pushTargetKey(key)?.let { pushLabel(it) },
            pendingPush = record?.let { PendingPush(it.toDayKey, it.toLabel, it.original.size) },
            dividerLabel = dividerLabel,
        )
    }

    /** Where a whole-section push from [sectionKey] lands, or null when it would leave the window. */
    private fun pushTargetKey(sectionKey: String): String? =
        if (bucketOf(sectionKey) == BUCKET_MONTH) {
            monthPushForwardTarget(sectionKey, currentMonth)
        } else {
            pushForwardTarget(sectionKey, today)
                .takeIf { !it.isAfter(dayWindow.last()) }
                ?.toString()
        }

    /**
     * The short label a push affordance names its destination by (`→ SEP`, `→ ВТ`). Kept short
     * deliberately: it shares one heading line with the section's own label.
     */
    private fun pushLabel(sectionKey: String): String =
        if (bucketOf(sectionKey) == BUCKET_MONTH) monthShortLabel(sectionKey)
        else LocalDate.parse(sectionKey).dayLabel()

    fun switchScreen(screen: TonoScreenKind) {
        if (_uiState.value.screen == screen) return
        finalizeEditing()
        _uiState.update { it.copy(screen = screen, editing = null, drag = null) }
    }

    fun startEditing(dayKey: String) {
        finalizeEditing()
        _uiState.update { it.copy(editing = EditingState(dayKey, "", taskId = null, isNew = true)) }
    }

    fun startEditingTask(dayKey: String, taskId: String, text: String) {
        finalizeEditing()
        _uiState.update { it.copy(editing = EditingState(dayKey, text, taskId = taskId, isNew = false)) }
    }

    /** Persists whatever is currently being edited before switching to a new editing target. Blank existing entries are deleted by [saveEntry]. */
    private fun finalizeEditing() {
        val editing = _uiState.value.editing ?: return
        autosaveJob?.cancel()
        viewModelScope.launch {
            saveEntry(editing.dayKey, editing.taskId, editing.value)
        }
    }

    /** Explicitly removes the entry currently being edited (swipe-to-delete or backspace-on-empty). */
    fun deleteEditingTask() {
        val editing = _uiState.value.editing ?: return
        autosaveJob?.cancel()
        val taskId = editing.taskId
        if (taskId != null) {
            viewModelScope.launch {
                dao.deleteById(editing.dayKey, taskId)
            }
        }
        _uiState.update { it.copy(editing = null) }
    }

    fun updateEditing(value: String) {
        val editing = _uiState.value.editing ?: return

        if (!value.contains("\n")) {
            _uiState.update { it.copy(editing = it.editing?.copy(value = value)) }
            if (editing.isNew) scheduleAutosave()
            return
        }

        // Pasted multi-line text: every complete line becomes its own entry,
        // the trailing remainder stays live in the field being edited.
        autosaveJob?.cancel()
        val (toCreate, remainder) = splitPastedLines(value)

        viewModelScope.launch {
            toCreate.forEach { line ->
                val pos = (dao.maxPosition(editing.dayKey) ?: -1) + 1
                dao.insert(newTask(editing.dayKey, pos, line))
            }
            _uiState.update { state ->
                if (state.editing?.dayKey == editing.dayKey && state.editing.taskId == editing.taskId) {
                    state.copy(editing = state.editing.copy(value = remainder))
                } else {
                    state
                }
            }
            if (remainder.isNotBlank() && editing.isNew) scheduleAutosave()
        }
    }

    private fun newTask(sectionKey: String, position: Int, text: String, id: String = UUID.randomUUID().toString()) =
        Task(id, sectionKey, position, text, bucketOf(sectionKey))

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            val editing = _uiState.value.editing ?: return@launch
            val savedId = saveEntry(editing.dayKey, editing.taskId, editing.value)
            _uiState.update { state ->
                val current = state.editing
                if (current != null &&
                    current.dayKey == editing.dayKey &&
                    current.taskId == editing.taskId &&
                    current.value == editing.value
                ) {
                    state.copy(editing = current.copy(taskId = savedId))
                } else {
                    state
                }
            }
        }
    }

    /** Inserts, updates, or deletes the backing task for an editing session. Returns the resulting task id, or null if nothing exists. */
    private suspend fun saveEntry(dayKey: String, taskId: String?, rawText: String): String? {
        val text = rawText.trim()
        return when {
            taskId != null && text.isBlank() -> {
                dao.deleteById(dayKey, taskId)
                null
            }
            taskId != null -> {
                dao.updateText(taskId, text)
                taskId
            }
            text.isBlank() -> null
            else -> {
                val pos = (dao.maxPosition(dayKey) ?: -1) + 1
                val task = newTask(dayKey, pos, text)
                dao.insert(task)
                task.id
            }
        }
    }

    fun commitEdit() {
        val editing = _uiState.value.editing ?: return
        autosaveJob?.cancel()

        if (editing.value.isBlank() && editing.taskId == null) {
            _uiState.update { it.copy(editing = null) }
            return
        }

        viewModelScope.launch {
            saveEntry(editing.dayKey, editing.taskId, editing.value)
            _uiState.update { state ->
                state.copy(
                    editing = if (editing.isNew) {
                        EditingState(editing.dayKey, "", taskId = null, isNew = true)
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun cancelEdit(dayKey: String) {
        val editing = _uiState.value.editing ?: return
        if (editing.dayKey != dayKey) return
        autosaveJob?.cancel()

        viewModelScope.launch {
            saveEntry(editing.dayKey, editing.taskId, editing.value)
        }
        _uiState.update { it.copy(editing = null) }
    }

    fun completeTask(taskId: String, dayKey: String) {
        val task = _uiState.value.sections
            .find { it.dayKey == dayKey }
            ?.tasks?.find { it.id == taskId } ?: return

        viewModelScope.launch {
            dao.deleteById(dayKey, taskId)
        }

        val ghost = GhostItem(taskId, task.text)
        ghostMap[taskId] = ghost
        ghostsByDay.getOrPut(dayKey) { mutableListOf() }.add(ghost)
        rebuildSections()

        ghostJobs[taskId] = viewModelScope.launch {
            delay(6_500)
            removeGhost(taskId, dayKey)
        }
    }

    fun undoComplete(ghostId: String, dayKey: String) {
        ghostJobs.remove(ghostId)?.cancel()
        val ghost = ghostMap.remove(ghostId) ?: return
        ghostsByDay[dayKey]?.remove(ghost)
        rebuildSections()

        viewModelScope.launch {
            val pos = (dao.maxPosition(dayKey) ?: -1) + 1
            dao.insert(newTask(dayKey, pos, ghost.text, id = ghost.id))
        }
    }

    private fun removeGhost(ghostId: String, dayKey: String) {
        ghostJobs.remove(ghostId)
        val ghost = ghostMap.remove(ghostId) ?: return
        ghostsByDay[dayKey]?.remove(ghost)
        rebuildSections()
    }

    /**
     * Moves every live task in [dayKey] to the section [pushTargetKey] resolves to, appended in
     * their existing order. Ghosts are left behind — they are already completed. The move stays
     * undoable for [PUSH_UNDO_MS]; the Room flow emission is what repaints both sections.
     */
    fun pushDayForward(dayKey: String) {
        val targetKey = pushTargetKey(dayKey) ?: return

        pushJobs.remove(dayKey)?.cancel()

        // Fold an in-flight entry on this section into the move rather than racing it.
        val editing = _uiState.value.editing?.takeIf { it.dayKey == dayKey }
        if (editing != null) {
            autosaveJob?.cancel()
            _uiState.update { it.copy(editing = null) }
        }

        viewModelScope.launch {
            if (editing != null) saveEntry(editing.dayKey, editing.taskId, editing.value)

            val moved = dao.getTasksForDay(dayKey)
            if (moved.isEmpty()) {
                // Nothing to move; the expiry job was already cancelled above, so retire
                // any superseded record rather than leaving its undo affordance stranded.
                expirePush(dayKey)
                return@launch
            }

            val base = (dao.maxPosition(targetKey) ?: -1) + 1
            pushRecords[dayKey] = PushRecord(targetKey, pushLabel(targetKey), moved)
            moved.forEachIndexed { i, task ->
                dao.update(task.copy(dayKey = targetKey, position = base + i, bucket = bucketOf(targetKey)))
            }

            pushJobs[dayKey] = viewModelScope.launch {
                delay(PUSH_UNDO_MS)
                expirePush(dayKey)
            }
        }
    }

    /** Restores a pushed section's tasks to their original section and positions. */
    fun undoPush(dayKey: String) {
        pushJobs.remove(dayKey)?.cancel()
        val record = pushRecords.remove(dayKey) ?: return

        viewModelScope.launch {
            record.original.forEach { dao.update(it) }
        }
    }

    private fun expirePush(dayKey: String) {
        pushJobs.remove(dayKey)
        if (pushRecords.remove(dayKey) == null) return
        rebuildSections() // drops the now-expired undo affordance
    }

    fun swipeUpdate(taskId: String, dx: Float) {
        _uiState.update { state ->
            state.copy(swipe = state.swipe + (taskId to SwipeState(dx, snapping = false)))
        }
    }

    fun swipeRelease(taskId: String) {
        _uiState.update { state ->
            state.copy(swipe = state.swipe - taskId)
        }
    }

    fun dragStart(taskId: String, fromDay: String, x: Float, y: Float) {
        _uiState.update { it.copy(drag = DragState(taskId, fromDay, x, y, fromDay)) }
    }

    fun dragMove(x: Float, y: Float, overDay: String) {
        _uiState.update { state ->
            state.copy(drag = state.drag?.copy(x = x, y = y, overDay = overDay))
        }
    }

    fun dragRelease() {
        val drag = _uiState.value.drag ?: return
        if (drag.overDay != drag.fromDay) {
            val task = _uiState.value.sections
                .find { it.dayKey == drag.fromDay }
                ?.tasks?.find { it.id == drag.taskId }

            if (task != null) {
                viewModelScope.launch {
                    dao.deleteById(drag.fromDay, drag.taskId)
                    val pos = (dao.maxPosition(drag.overDay) ?: -1) + 1
                    dao.insert(newTask(drag.overDay, pos, task.text, id = drag.taskId))
                }
            }
        }
        _uiState.update { it.copy(drag = null) }
    }
}
