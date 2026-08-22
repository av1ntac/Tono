package org.volkov.tono.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

@Composable
fun TaskRow(
    text: String,
    taskId: String,
    /** Days carried, appended as `(15)` in the age color; null on tasks too young to mark. */
    ageDays: Int?,
    isDragging: Boolean,
    onComplete: () -> Unit,
    onTap: () -> Unit,
    onDragStart: (rootX: Float, rootY: Float) -> Unit,
    onDragMove: (rootX: Float, rootY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isPastThreshold by remember { mutableStateOf(false) }
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val swipeThresholdPx = with(density) { 96.dp.toPx() }
    val moveLockPx = with(density) { 8.dp.toPx() }
    val exitPx = with(density) { 480.dp.toPx() }
    val longPressMs = 380L

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .alpha(if (isDragging) 0.3f else 1f)
            .onGloballyPositioned { rowCoordinates = it }
            .pointerInput(taskId) {
                var startX = 0f
                var startY = 0f
                var gestureMode = GestureMode.IDLE
                var longPressJob: kotlinx.coroutines.Job? = null

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                // Consume so the day column's tap-to-create-entry click
                                // never fires for gestures that land on an existing task.
                                change.consume()
                                startX = change.position.x
                                startY = change.position.y
                                gestureMode = GestureMode.IDLE

                                longPressJob = scope.launch {
                                    kotlinx.coroutines.delay(longPressMs)
                                    if (gestureMode == GestureMode.IDLE) {
                                        gestureMode = GestureMode.DRAG
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val root = rowCoordinates?.localToRoot(Offset(startX, startY))
                                            ?: Offset(startX, startY)
                                        onDragStart(root.x, root.y)
                                    }
                                }
                            }

                            PointerEventType.Move -> {
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY

                                if (gestureMode == GestureMode.IDLE) {
                                    if (abs(dx) > moveLockPx && abs(dx) > abs(dy) && dx > 0) {
                                        gestureMode = GestureMode.SWIPE
                                        longPressJob?.cancel()
                                    } else if (abs(dy) > moveLockPx && abs(dy) > abs(dx)) {
                                        gestureMode = GestureMode.SCROLL
                                        longPressJob?.cancel()
                                    }
                                }

                                if (gestureMode == GestureMode.SWIPE) {
                                    change.consume()
                                    val clampedDx = dx.coerceAtLeast(0f)
                                    isPastThreshold = clampedDx >= swipeThresholdPx
                                    scope.launch { offsetX.snapTo(clampedDx) }
                                } else if (gestureMode == GestureMode.DRAG) {
                                    change.consume()
                                    val root = rowCoordinates?.localToRoot(change.position) ?: change.position
                                    onDragMove(root.x, root.y)
                                }
                            }

                            PointerEventType.Release -> {
                                longPressJob?.cancel()
                                when (gestureMode) {
                                    GestureMode.SWIPE -> {
                                        if (offsetX.value >= swipeThresholdPx) {
                                            scope.launch {
                                                offsetX.animateTo(
                                                    exitPx,
                                                    animationSpec = tween(240, easing = SwipeEasing),
                                                )
                                                onComplete()
                                                offsetX.snapTo(0f)
                                                isPastThreshold = false
                                            }
                                        } else {
                                            scope.launch {
                                                offsetX.animateTo(
                                                    0f,
                                                    animationSpec = tween(240, easing = SwipeEasing),
                                                )
                                                isPastThreshold = false
                                            }
                                        }
                                    }
                                    GestureMode.DRAG -> onDragEnd()
                                    GestureMode.IDLE -> onTap()
                                    else -> {}
                                }
                                gestureMode = GestureMode.IDLE
                            }

                            else -> {}
                        }
                    }
                }
            },
    ) {
        val washAlpha = (offsetX.value / swipeThresholdPx).coerceIn(0f, 1f)
        if (washAlpha > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    color = colors.today.copy(alpha = washAlpha),
                    topLeft = Offset.Zero,
                    size = Size(offsetX.value, size.height),
                )
            }
        }

        val line = buildAnnotatedString {
            append(text)
            if (ageDays != null) {
                append(" ")
                withStyle(SpanStyle(color = colors.age)) { append("($ageDays)") }
            }
        }

        Text(
            text = line,
            style = TonoType.body.copy(
                textDecoration = if (isPastThreshold) TextDecoration.LineThrough else null,
            ),
            color = colors.ink,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        )
    }
}

private enum class GestureMode { IDLE, SWIPE, SCROLL, DRAG }
