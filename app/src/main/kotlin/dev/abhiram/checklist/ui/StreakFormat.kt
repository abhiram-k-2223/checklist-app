package dev.abhiram.checklist.ui

import androidx.compose.ui.graphics.Color
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask

fun streakLabel(streak: Int): String = when {
    streak <= 0 -> "No streak yet"
    streak == 1 -> "🔥 1 day"
    else -> "🔥 $streak days"
}

fun compactStreakLabel(streak: Int): String =
    if (streak <= 0) "—" else "🔥 $streak"

fun streakColor(color: StreakColor): Color = Color(color.argb)

fun streakColorName(color: StreakColor): String = when (color) {
    StreakColor.BLUE -> "Blue"
    StreakColor.GREEN -> "Green"
    StreakColor.ORANGE -> "Orange"
    StreakColor.PURPLE -> "Purple"
}

fun availableStreakColors(
    tasks: List<StreakTask>,
    editingTaskId: String? = null,
): List<StreakColor> {
    val used = tasks
        .filter { it.id != editingTaskId }
        .map { it.color }
        .toSet()
    return StreakColor.entries.filterNot { it in used }
}
