package org.volkov.tono.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.volkov.tono.BuildConfig
import org.volkov.tono.ui.TonoScreenKind
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType

/**
 * The mode line. Its left half is the screen switch — `WEEKS · MONTHS`, active word in ink,
 * inactive muted — so navigation costs no chrome and collides with none of the row gestures.
 */
@Composable
fun StatusStrip(
    screen: TonoScreenKind,
    onSelectScreen: (TonoScreenKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTonoColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            ScreenTab("WEEKS", TonoScreenKind.WEEKS, screen, onSelectScreen)
            Text(text = " · ", style = TonoType.meta, color = colors.muted)
            ScreenTab("MONTHS", TonoScreenKind.MONTHS, screen, onSelectScreen)
        }
        Text(
            text = "TONO v${BuildConfig.VERSION_NAME}",
            style = TonoType.meta,
            color = colors.muted,
        )
    }
}

@Composable
private fun ScreenTab(
    label: String,
    target: TonoScreenKind,
    current: TonoScreenKind,
    onSelect: (TonoScreenKind) -> Unit,
) {
    val colors = LocalTonoColors.current
    Text(
        text = label,
        style = TonoType.meta,
        color = if (target == current) colors.ink else colors.muted,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(target) },
            )
            // Widens the touch target to a comfortable tap without moving the text.
            .padding(vertical = 8.dp),
    )
}
