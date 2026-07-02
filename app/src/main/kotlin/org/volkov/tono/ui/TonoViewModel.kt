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
import org.volkov.tono.data.Task
import org.volkov.tono.data.TonoDatabase
import org.volkov.tono.util.computeDayWindow
import org.volkov.tono.util.dateLabel
import org.volkov.tono.util.dayLabel
import java.time.LocalDate
import java.util.UUID

data class TaskItem(val id: String, val text: String)
data class GhostItem(val id: String, val text: String)
data class EditingState(val dayKey: String, val value: String)
data class SwipeState(val dx: Float, val snapping: Boolean)
data class DragState(
    val taskId: String,
    val fromDay: String,
    val x: Float,
    val y: Float,
    val overDay: String,
)

data class DayUiState(
    val dayKey: String,
    val label: String,
    val dateLabel: String,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val tasks: List<TaskItem>,
    val ghosts: List<GhostItem>,
)

data class TonoUiState(
    val days: List<DayUiState> = emptyList(),
    val editing: EditingState? = null,
    val swipe: Map<String, SwipeState> = emptyMap(),
    val drag: DragState? = null,
)

class TonoViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = TonoDatabase.getInstance(app).taskDao()

    private val _uiState = MutableStateFlow(TonoUiState())
    val uiState: StateFlow<TonoUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now()
    private val dayWindow = computeDayWindow(today)

    private val ghostMap = mutableMapOf<String, GhostItem>()
    private val ghostJobs = mutableMapOf<String, Job>()
    private val ghostsByDay = mutableMapOf<String, MutableList<GhostItem>>()

    init {
        _uiState.value = TonoUiState(days = buildDayList(emptyMap(), ghostsByDay))
        viewModelScope.launch {
            dao.observeAll().collect { tasks ->
                val tasksByDay = tasks.groupBy { it.dayKey }
                    .mapValues { (_, v) -> v.map { TaskItem(it.id, it.text) } }
                _uiState.update { state ->
                    state.copy(days = buildDayList(tasksByDay, ghostsByDay))
                }
            }
        }
    }

    private fun buildDayList(
        tasksByDay: Map<String, List<TaskItem>>,
        ghostsByDay: Map<String, List<GhostItem>>,
    ): List<DayUiState> = dayWindow.map { date ->
        val key = date.toString()
        val dow = date.dayOfWeek
        DayUiState(
            dayKey = key,
            label = date.dayLabel(),
            dateLabel = date.dateLabel(),
            isToday = date == today,
            isWeekend = dow.value >= 6,
            tasks = tasksByDay[key] ?: emptyList(),
            ghosts = ghostsByDay[key] ?: emptyList(),
        )
    }

    fun startEditing(dayKey: String) {
        _uiState.update { it.copy(editing = EditingState(dayKey, "")) }
    }

    fun updateEditing(value: String) {
        _uiState.update { it.copy(editing = it.editing?.copy(value = value)) }
    }

    fun commitEdit() {
        val editing = _uiState.value.editing ?: return
        if (editing.value.isBlank()) {
            _uiState.update { it.copy(editing = null) }
            return
        }
        viewModelScope.launch {
            val pos = (dao.maxPosition(editing.dayKey) ?: -1) + 1
            dao.insert(Task(UUID.randomUUID().toString(), editing.dayKey, pos, editing.value.trim()))
            _uiState.update { it.copy(editing = EditingState(editing.dayKey, "")) }
        }
    }

    fun cancelEdit(dayKey: String) {
        _uiState.update { state ->
            if (state.editing?.dayKey == dayKey) {
                state.copy(editing = null)
            } else {
                state
            }
        }
    }

    fun completeTask(taskId: String, dayKey: String) {
        val task = _uiState.value.days
            .find { it.dayKey == dayKey }
            ?.tasks?.find { it.id == taskId } ?: return

        viewModelScope.launch {
            dao.deleteById(dayKey, taskId)
        }

        val ghost = GhostItem(taskId, task.text)
        ghostMap[taskId] = ghost
        ghostsByDay.getOrPut(dayKey) { mutableListOf() }.add(ghost)
        refreshGhosts()

        ghostJobs[taskId] = viewModelScope.launch {
            delay(6_500)
            removeGhost(taskId, dayKey)
        }
    }

    fun undoComplete(ghostId: String, dayKey: String) {
        ghostJobs.remove(ghostId)?.cancel()
        val ghost = ghostMap.remove(ghostId) ?: return
        ghostsByDay[dayKey]?.remove(ghost)
        refreshGhosts()

        viewModelScope.launch {
            val pos = (dao.maxPosition(dayKey) ?: -1) + 1
            dao.insert(Task(ghost.id, dayKey, pos, ghost.text))
        }
    }

    private fun removeGhost(ghostId: String, dayKey: String) {
        ghostJobs.remove(ghostId)
        val ghost = ghostMap.remove(ghostId) ?: return
        ghostsByDay[dayKey]?.remove(ghost)
        refreshGhosts()
    }

    private fun refreshGhosts() {
        val ghostIds = ghostMap.keys
        val tasksByDay = _uiState.value.days.associate { d ->
            d.dayKey to d.tasks.filter { it.id !in ghostIds }
        }
        _uiState.update { state ->
            state.copy(days = buildDayList(tasksByDay, ghostsByDay))
        }
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
            val task = _uiState.value.days
                .find { it.dayKey == drag.fromDay }
                ?.tasks?.find { it.id == drag.taskId }

            if (task != null) {
                viewModelScope.launch {
                    dao.deleteById(drag.fromDay, drag.taskId)
                    val pos = (dao.maxPosition(drag.overDay) ?: -1) + 1
                    dao.insert(Task(drag.taskId, drag.overDay, pos, task.text))
                }
            }
        }
        _uiState.update { it.copy(drag = null) }
    }
}
