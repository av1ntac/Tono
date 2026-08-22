package org.volkov.tono.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.volkov.tono.ui.DayUiState
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/**
 * The `пн · 4 aug` / `august · 2026` line, doubling as the section-scale gesture surface:
 *
 * - swipe right → push every live task in this section forward one step (see `pushDayForward`):
 *                 a day in the weeks view, a month (and finally `later`) in the months view
 * - swipe left  → undo that push, while its window is open
 * - tap         → same as tapping the section's empty space: start a new entry
 *
 * Thresholds and easing mirror [TaskRow] so the day-scale gesture feels like the row-scale one.
 */
@Composable
fun DayHeadingRow(
    day: DayUiState,
    onTap: () -> Unit,
    onPushForward: () -> Unit,
    onUndoPush: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isPastThreshold by remember { mutableStateOf(false) }

    val swipeThresholdPx = with(density) { 96.dp.toPx() }
    val moveLockPx = with(density) { 8.dp.toPx() }

    val pendingPush = day.pendingPush
    val canPush = day.pushTargetLabel != null && day.tasks.isNotEmpty()
    val canUndo = pendingPush != null
    val headingAlpha = if (day.isWeekend && !day.isCurrent) 0.55f else 1f
    val headingStyle = if (day.isCurrent) TonoType.headingToday else TonoType.headingNormal

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(day.dayKey, canPush, canUndo) {
                var startX = 0f
                var startY = 0f
                var gestureMode = HeadingGestureMode.IDLE

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                // Consume so the day column's tap-to-create-entry click never
                                // fires mid-swipe; a plain tap is re-dispatched on release.
                                change.consume()
                                startX = change.position.x
                                startY = change.position.y
                                gestureMode = HeadingGestureMode.IDLE
                            }

                            PointerEventType.Move -> {
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY

                                if (gestureMode == HeadingGestureMode.IDLE) {
                                    val horizontal = abs(dx) > moveLockPx && abs(dx) > abs(dy)
                                    if (horizontal && ((dx > 0 && canPush) || (dx < 0 && canUndo))) {
                                        gestureMode = HeadingGestureMode.SWIPE
                                    } else if (abs(dy) > moveLockPx && abs(dy) > abs(dx)) {
                                        gestureMode = HeadingGestureMode.SCROLL
                                    }
                                }

                                if (gestureMode == HeadingGestureMode.SWIPE) {
                                    change.consume()
                                    val clampedDx = if (canUndo && dx < 0) {
                                        dx.coerceAtLeast(-swipeThresholdPx * 1.4f)
                                    } else {
                                        dx.coerceIn(0f, swipeThresholdPx * 1.4f)
                                    }
                                    val crossed = abs(clampedDx) >= swipeThresholdPx
                                    if (crossed != isPastThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    isPastThreshold = crossed
                                    scope.launch { offsetX.snapTo(clampedDx) }
                                }
                            }

                            PointerEventType.Release -> {
                                when (gestureMode) {
                                    HeadingGestureMode.SWIPE -> {
                                        val dx = offsetX.value
                                        if (abs(dx) >= swipeThresholdPx) {
                                            if (dx > 0) onPushForward() else onUndoPush()
                                        }
                                        scope.launch {
                                            offsetX.animateTo(
                                                0f,
                                                animationSpec = tween(240, easing = SwipeEasing),
                                            )
                                            isPastThreshold = false
                                        }
                                    }
                                    HeadingGestureMode.IDLE -> onTap()
                                    else -> {}
                                }
                                gestureMode = HeadingGestureMode.IDLE
                            }

                            else -> {}
                        }
                    }
                }
            }
            .padding(top = 18.dp),
    ) {
        if (offsetX.value != 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = abs(offsetX.value)
                drawRect(
                    color = colors.drop,
                    topLeft = if (offsetX.value > 0f) Offset.Zero else Offset(size.width - width, 0f),
                    size = Size(width, size.height),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 19.dp, end = 16.dp)
                .alpha(headingAlpha)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        ) {
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.label,
                    style = headingStyle,
                    color = colors.dayOfWeek,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (day.currentLabel != null) {
                    Text(
                        text = " · ${day.currentLabel}",
                        style = headingStyle,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val trailing = when {
                isPastThreshold && offsetX.value > 0f ->
                    "→ ${day.pushTargetLabel.orEmpty().uppercase()}"
                isPastThreshold && offsetX.value < 0f -> "← UNDO"
                pendingPush != null ->
                    "← UNDO · ${pendingPush.count} → ${pendingPush.toLabel.uppercase()}"
                else -> day.dateLabel.uppercase()
            }
            Text(
                text = trailing,
                style = TonoType.headingNormal,
                color = if (isPastThreshold || pendingPush != null) colors.ink else colors.muted,
                maxLines = 1,
            )
        }
    }
}

private enum class HeadingGestureMode { IDLE, SWIPE, SCROLL }
