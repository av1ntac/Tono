package org.volkov.tono.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTonoColors = staticCompositionLocalOf { TonoColors.Light }

private val lightScheme = lightColorScheme(
    background = PaperLight,
    surface = PaperLight,
    onBackground = InkLight,
    onSurface = InkLight,
    primary = TodayLight,
    onPrimary = InkLight,
    outline = HairLight,
)

private val darkScheme = darkColorScheme(
    background = PaperDark,
    surface = PaperDark,
    onBackground = InkDark,
    onSurface = InkDark,
    primary = TodayDark,
    onPrimary = InkDark,
    outline = HairDark,
)

@Composable
fun TonoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tonoColors = if (darkTheme) TonoColors.Dark else TonoColors.Light
    val colorScheme = if (darkTheme) darkScheme else lightScheme

    CompositionLocalProvider(LocalTonoColors provides tonoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
