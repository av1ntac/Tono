package org.volkov.tono.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
fun EditingRow(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasBeenFocused by remember { mutableStateOf(false) }

    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val offsetX = remember { Animatable(0f) }
    var isPastThreshold by remember { mutableStateOf(false) }
    val swipeThresholdPx = with(density) { 96.dp.toPx() }
    val moveLockPx = with(density) { 8.dp.toPx() }
    val exitPx = with(density) { 480.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                var startX = 0f
                var startY = 0f
                var gestureMode = SwipeGestureMode.IDLE

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue

                        when (event.type) {
                            PointerEventType.Press -> {
                                startX = change.position.x
                                startY = change.position.y
                                gestureMode = SwipeGestureMode.IDLE
                            }

                            PointerEventType.Move -> {
                                val dx = change.position.x - startX
                                val dy = change.position.y - startY

                                if (gestureMode == SwipeGestureMode.IDLE) {
                                    if (abs(dx) > moveLockPx && abs(dx) > abs(dy) && dx > 0) {
                                        gestureMode = SwipeGestureMode.SWIPE
                                    } else if (abs(dx) > moveLockPx || abs(dy) > moveLockPx) {
                                        gestureMode = SwipeGestureMode.OTHER
                                    }
                                }

                                if (gestureMode == SwipeGestureMode.SWIPE) {
                                    change.consume()
                                    val clampedDx = dx.coerceAtLeast(0f)
                                    isPastThreshold = clampedDx >= swipeThresholdPx
                                    scope.launch { offsetX.snapTo(clampedDx) }
                                }
                            }

                            PointerEventType.Release -> {
                                if (gestureMode == SwipeGestureMode.SWIPE) {
                                    if (offsetX.value >= swipeThresholdPx) {
                                        scope.launch {
                                            offsetX.animateTo(
                                                exitPx,
                                                animationSpec = tween(240, easing = SwipeEasing),
                                            )
                                            onDelete()
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
                                gestureMode = SwipeGestureMode.IDLE
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

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onValueChange(newValue.text)
            },
            textStyle = TonoType.body.copy(
                color = colors.ink,
                textDecoration = if (isPastThreshold) TextDecoration.LineThrough else null,
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.dayOfWeek),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (hasBeenFocused && !it.isFocused) {
                        onCancel()
                    }
                    if (it.isFocused) {
                        hasBeenFocused = true
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.Backspace &&
                        fieldValue.text.isEmpty()
                    ) {
                        onDelete()
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                }
            },
        )
    }
}

private enum class SwipeGestureMode { IDLE, SWIPE, OTHER }
