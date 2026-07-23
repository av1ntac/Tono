package org.volkov.tono.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.volkov.tono.ui.DayUiState
import org.volkov.tono.ui.EditingState
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType

@Composable
fun DaySection(
    day: DayUiState,
    editing: EditingState?,
    draggingTaskId: String?,
    isDropTarget: Boolean,
    onEmptyClick: (String) -> Unit,
    onEditValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: (String) -> Unit,
    onComplete: (taskId: String, dayKey: String) -> Unit,
    onUndo: (ghostId: String, dayKey: String) -> Unit,
    onEditTask: (taskId: String, dayKey: String, text: String) -> Unit,
    onDragStart: (taskId: String, dayKey: String, rootX: Float, rootY: Float) -> Unit,
    onDragMove: (rootX: Float, rootY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val editingHere = editing?.takeIf { it.dayKey == day.dayKey }
    val isEditing = editingHere != null
    val headingAlpha = if (day.isWeekend && !day.isToday) 0.55f else 1f
    val leftBorderColor = if (day.isToday) colors.today else Color.Transparent
    val headingStyle = if (day.isToday) TonoType.headingToday else TonoType.headingNormal
    val dropBackground = if (isDropTarget) colors.drop else Color.Transparent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(dropBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (!isEditing) onEmptyClick(day.dayKey) }
            )
            .padding(top = 18.dp)
            .drawBehind {
                drawRect(
                    color = leftBorderColor,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 19.dp, end = 16.dp)
                .alpha(headingAlpha),
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.label,
                    style = headingStyle,
                    color = colors.dayOfWeek,
                )
                if (day.isToday) {
                    Text(
                        text = " · TODAY",
                        style = headingStyle,
                        color = colors.ink,
                    )
                }
            }
            Text(
                text = day.dateLabel.uppercase(),
                style = TonoType.headingNormal,
                color = colors.muted,
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colors.hair,
            modifier = Modifier.padding(top = 6.dp, start = 19.dp, end = 16.dp),
        )

        Column(modifier = Modifier.padding(top = 6.dp, start = 19.dp, end = 16.dp)) {
            day.ghosts.forEach { ghost ->
                GhostRow(
                    text = ghost.text,
                    ghostId = ghost.id,
                    onUndo = { onUndo(ghost.id, day.dayKey) },
                )
            }

            day.tasks.forEach { task ->
                if (editingHere != null && !editingHere.isNew && editingHere.taskId == task.id) {
                    EditingRow(
                        value = editingHere.value,
                        onValueChange = onEditValueChange,
                        onCommit = onCommit,
                        onCancel = { onCancel(day.dayKey) },
                    )
                } else if (editingHere != null && editingHere.isNew && editingHere.taskId == task.id) {
                    // Already autosaved from the trailing new-entry row below; avoid a duplicate row.
                } else {
                    TaskRow(
                        text = task.text,
                        taskId = task.id,
                        isDragging = draggingTaskId == task.id,
                        onComplete = { onComplete(task.id, day.dayKey) },
                        onTap = { onEditTask(task.id, day.dayKey, task.text) },
                        onDragStart = { x, y -> onDragStart(task.id, day.dayKey, x, y) },
                        onDragMove = onDragMove,
                        onDragEnd = onDragEnd,
                    )
                }
            }

            when {
                editingHere != null && editingHere.isNew -> {
                    EditingRow(
                        value = editingHere.value,
                        onValueChange = onEditValueChange,
                        onCommit = onCommit,
                        onCancel = { onCancel(day.dayKey) },
                    )
                }
                editingHere == null -> {
                    EmptyRow(
                        showCursor = day.isToday && editing == null,
                        onClick = { onEmptyClick(day.dayKey) },
                    )
                }
            }
        }
    }
}
