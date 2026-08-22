package org.volkov.tono.ui.theme

import androidx.compose.ui.graphics.Color

// Light tokens
val PaperLight = Color(0xFFF5F1E8)
val InkLight = Color(0xFF1A1814)
val MutedLight = Color(0xFF9A948A)
val HairLight = Color(0xFFD8D2C4)
val TodayLight = Color(0xFFF7E07A)
val DropLight = Color(0x4CF7E07A)
val DayOfWeekLight = Color(0xFF6FAF3D)
val AgeLight = Color(0xFFB4622F)

// Dark tokens
val PaperDark = Color(0xFF15130F)
val InkDark = Color(0xFFE8E3D6)
val MutedDark = Color(0xFF6B675E)
val HairDark = Color(0xFF2A2722)
val TodayDark = Color(0xFFC9A02D)
val DropDark = Color(0x2EC9A02D)
val DayOfWeekDark = Color(0xFF8FCB5C)
val AgeDark = Color(0xFFD08C55)

data class TonoColors(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val hair: Color,
    val today: Color,
    val drop: Color,
    val dayOfWeek: Color,
    /** Age marker on a task that has been carried for more than a week. */
    val age: Color,
) {
    companion object {
        val Light = TonoColors(
            paper = PaperLight,
            ink = InkLight,
            muted = MutedLight,
            hair = HairLight,
            today = TodayLight,
            drop = DropLight,
            dayOfWeek = DayOfWeekLight,
            age = AgeLight,
        )
        val Dark = TonoColors(
            paper = PaperDark,
            ink = InkDark,
            muted = MutedDark,
            hair = HairDark,
            today = TodayDark,
            drop = DropDark,
            dayOfWeek = DayOfWeekDark,
            age = AgeDark,
        )
    }
}
