package org.volkov.tono.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onEmptyClick: (String) -> Unit,
    onEditValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    onComplete: (taskId: String, dayKey: String) -> Unit,
    onUndo: (ghostId: String, dayKey: String) -> Unit,
    onDragStart: (taskId: String, dayKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val isEditing = editing?.dayKey == day.dayKey
    val headingAlpha = if (day.isWeekend && !day.isToday) 0.55f else 1f
    val leftBorderColor = if (day.isToday) colors.today else Color.Transparent
    val headingStyle = if (day.isToday) TonoType.headingToday else TonoType.headingNormal

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                .padding(start = 19.dp)
                .alpha(headingAlpha),
        ) {
            val dayLabel = if (day.isToday) "${day.label} · TODAY" else day.label
            Text(
                text = dayLabel.uppercase(),
                style = headingStyle,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = day.dateLabel.uppercase(),
                style = TonoType.headingNormal,
                color = colors.muted,
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colors.hair,
            modifier = Modifier.padding(top = 6.dp, start = 19.dp),
        )

        Column(modifier = Modifier.padding(top = 6.dp, start = 19.dp, end = 0.dp)) {
            day.ghosts.forEach { ghost ->
                GhostRow(
                    text = ghost.text,
                    ghostId = ghost.id,
                    onUndo = { onUndo(ghost.id, day.dayKey) },
                )
            }

            day.tasks.forEach { task ->
                TaskRow(
                    text = task.text,
                    taskId = task.id,
                    onComplete = { onComplete(task.id, day.dayKey) },
                    onDragStart = { onDragStart(task.id, day.dayKey) },
                )
            }

            if (isEditing && editing != null) {
                EditingRow(
                    value = editing.value,
                    onValueChange = onEditValueChange,
                    onCommit = onCommit,
                    onCancel = onCancel,
                )
            } else {
                EmptyRow(
                    showCursor = day.isToday,
                    onClick = { onEmptyClick(day.dayKey) },
                )
            }
        }
    }
}
