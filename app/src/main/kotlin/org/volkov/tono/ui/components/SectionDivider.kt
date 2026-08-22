package org.volkov.tono.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.volkov.tono.ui.theme.LocalTonoColors
import org.volkov.tono.ui.theme.TonoType

/** Centred separator above a section that starts a new stretch — `NEXT WEEK`, `LATER`. */
@Composable
fun SectionDivider(label: String, modifier: Modifier = Modifier) {
    val colors = LocalTonoColors.current
    Text(
        text = "— ${label.uppercase()} —",
        style = TonoType.meta,
        color = colors.muted,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 4.dp),
    )
}
