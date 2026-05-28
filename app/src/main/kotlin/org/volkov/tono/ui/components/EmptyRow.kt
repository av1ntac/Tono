package org.volkov.tono.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.volkov.tono.ui.theme.LocalTonoColors

@Composable
fun EmptyRow(
    showCursor: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    val cursorAlpha = remember { Animatable(1f) }

    if (showCursor) {
        LaunchedEffect(Unit) {
            while (true) {
                cursorAlpha.snapTo(1f)
                delay(550)
                cursorAlpha.snapTo(0f)
                delay(550)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clickable(onClick = onClick),
    ) {
        if (showCursor) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    color = colors.muted.copy(alpha = 0.4f * cursorAlpha.value),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    size = Size(1.5.dp.toPx(), 18.dp.toPx()),
                )
            }
        }
    }
}
