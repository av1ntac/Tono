package org.volkov.tono.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.volkov.tono.ui.components.DaySection
import org.volkov.tono.ui.components.SectionDivider
import org.volkov.tono.ui.components.StatusStrip
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType

@Composable
fun TonoScreen(vm: TonoViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    val colors = LocalTonoColors.current

    // Back on the months view returns to the weeks view rather than leaving the app.
    BackHandler(enabled = state.screen == TonoScreenKind.MONTHS) {
        vm.switchScreen(TonoScreenKind.WEEKS)
    }

    // Root-coordinate top..bottom bounds of each visible section, used to resolve which
    // section a dragged task is currently hovering over. Cleared when the screen changes,
    // since the sections underneath it are replaced wholesale.
    val dayBounds = remember(state.screen) { mutableStateMapOf<String, ClosedFloatingPointRange<Float>>() }
    val drag = state.drag
    val sections = state.sections

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper),
        ) {
            item {
                StatusStrip(
                    screen = state.screen,
                    onSelectScreen = { vm.switchScreen(it) },
                )
            }

            items(sections, key = { it.dayKey }) { day ->
                day.dividerLabel?.let { SectionDivider(label = it) }

                DaySection(
                    day = day,
                    editing = state.editing,
                    draggingTaskId = drag?.taskId,
                    isDropTarget = drag != null && drag.overDay == day.dayKey && drag.fromDay != day.dayKey,
                    onEmptyClick = { vm.startEditing(it) },
                    onEditValueChange = { vm.updateEditing(it) },
                    onCommit = { vm.commitEdit() },
                    onCancel = { vm.cancelEdit(it) },
                    onDeleteEditing = { vm.deleteEditingTask() },
                    onComplete = { taskId, dayKey -> vm.completeTask(taskId, dayKey) },
                    onUndo = { ghostId, dayKey -> vm.undoComplete(ghostId, dayKey) },
                    onEditTask = { taskId, dayKey, text -> vm.startEditingTask(dayKey, taskId, text) },
                    onDragStart = { taskId, dayKey, x, y -> vm.dragStart(taskId, dayKey, x, y) },
                    onDragMove = { x, y ->
                        val overDay = dayBounds.entries.firstOrNull { y in it.value }?.key
                        if (overDay != null) vm.dragMove(x, y, overDay)
                    },
                    onDragEnd = { vm.dragRelease() },
                    onPushForward = { vm.pushDayForward(it) },
                    onUndoPush = { vm.undoPush(it) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val top = coords.positionInRoot().y
                        dayBounds[day.dayKey] = top..(top + coords.size.height)
                    },
                )
            }
        }

        if (drag != null) {
            val draggedText = sections
                .find { it.dayKey == drag.fromDay }
                ?.tasks?.find { it.id == drag.taskId }
                ?.text
                ?: ""

            Box(
                modifier = Modifier
                    .offset { IntOffset(drag.x.toInt() + 12, drag.y.toInt() - 32) }
                    .background(colors.paper)
                    .border(1.dp, colors.hair)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(text = draggedText, style = TonoType.body, color = colors.ink)
            }
        }
    }
}
