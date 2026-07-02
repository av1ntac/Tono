package org.volkov.tono.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.volkov.tono.ui.components.DaySection
import org.volkov.tono.ui.components.StatusStrip
import org.volkov.tono.ui.components.WeekDivider
import org.volkov.tono.ui.theme.LocalTonoColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
fun TonoScreen(vm: TonoViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    val colors = LocalTonoColors.current

    val today = LocalDate.now()
    val nextMonday = today.with(java.time.DayOfWeek.MONDAY).let {
        if (it.isAfter(today)) it else it.plusWeeks(1)
    }

    val windowStart = state.days.firstOrNull()?.let {
        LocalDate.parse(it.dayKey)
    } ?: today
    val windowEnd = state.days.lastOrNull()?.let {
        LocalDate.parse(it.dayKey)
    } ?: today.plusDays(13)

    val dateRange = "${windowStart.format(DateFmt).lowercase()} — ${windowEnd.format(DateFmt).lowercase()}"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        item {
            StatusStrip(dateRange = dateRange)
        }

        items(state.days, key = { it.dayKey }) { day ->
            val dayDate = LocalDate.parse(day.dayKey)
            if (dayDate == nextMonday) {
                WeekDivider()
            }

            DaySection(
                day = day,
                editing = state.editing,
                onEmptyClick = { vm.startEditing(it) },
                onEditValueChange = { vm.updateEditing(it) },
                onCommit = { vm.commitEdit() },
                onCancel = { vm.cancelEdit(it) },
                onComplete = { taskId, dayKey -> vm.completeTask(taskId, dayKey) },
                onUndo = { ghostId, dayKey -> vm.undoComplete(ghostId, dayKey) },
                onDragStart = { taskId, dayKey -> vm.dragStart(taskId, dayKey, 0f, 0f) },
            )
        }
    }
}
