package dev.abhiram.checklist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IosBlue = Color(0xFF0A84FF)
val IosGray = Color(0xFF8E8E93)

private val DarkColors = darkColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = IosGray,
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
)

@Composable
fun ChecklistTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
