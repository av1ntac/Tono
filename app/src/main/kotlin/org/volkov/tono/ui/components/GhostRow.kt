package org.volkov.tono.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

@Composable
fun GhostRow(
    text: String,
    ghostId: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val swipeThresholdPx = with(density) { 96.dp.toPx() }
    val moveLockPx = with(density) { 8.dp.toPx() }
    val exitPx = with(density) { -480.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .alpha(0.55f)
            .pointerInput(ghostId) {
                var startX = 0f
                var startY = 0f
                var gestureMode = GhostGestureMode.IDLE

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                startX = change.position.x
                                startY = change.position.y
                                gestureMode = GhostGestureMode.IDLE
                            }

                            PointerEventType.Move -> {
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY

                                if (gestureMode == GhostGestureMode.IDLE) {
                                    if (abs(dx) > moveLockPx && abs(dx) > abs(dy) && dx < 0) {
                                        gestureMode = GhostGestureMode.SWIPE
                                    } else if (abs(dy) > moveLockPx && abs(dy) > abs(dx)) {
                                        gestureMode = GhostGestureMode.SCROLL
                                    }
                                }

                                if (gestureMode == GhostGestureMode.SWIPE) {
                                    change.consume()
                                    val clampedDx = dx.coerceAtMost(0f)
                                    scope.launch { offsetX.snapTo(clampedDx) }
                                }
                            }

                            PointerEventType.Release -> {
                                if (gestureMode == GhostGestureMode.SWIPE) {
                                    if (offsetX.value <= -swipeThresholdPx) {
                                        scope.launch {
                                            offsetX.animateTo(
                                                exitPx,
                                                animationSpec = tween(240, easing = SwipeEasing),
                                            )
                                            onUndo()
                                        }
                                    } else {
                                        scope.launch {
                                            offsetX.animateTo(
                                                0f,
                                                animationSpec = tween(240, easing = SwipeEasing),
                                            )
                                        }
                                    }
                                }
                                gestureMode = GhostGestureMode.IDLE
                            }

                            else -> {}
                        }
                    }
                }
            },
    ) {
        Text(
            text = text,
            style = TonoType.body.copy(textDecoration = TextDecoration.LineThrough),
            color = colors.muted,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        )
        Text(
            text = "← UNDO",
            style = TonoType.undoHint,
            color = colors.muted.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        )
    }
}

private enum class GhostGestureMode { IDLE, SWIPE, SCROLL }
