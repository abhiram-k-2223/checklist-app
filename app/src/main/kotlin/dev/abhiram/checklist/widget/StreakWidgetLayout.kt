package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.currentStreak
import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

const val STREAK_WIDGET_TASK_ROW_HEIGHT_DP = 36

data class StreakWidgetTaskRow(
    val task: StreakTask,
    val streak: Int,
    val doneToday: Boolean,
)

data class StreakWidgetMetrics(
    val outerPaddingDp: Int,
    val cellHeightDp: Int,
    val taskRowHeightDp: Int,
    val fixedContentHeightDp: Int,
)

/**
 * Compact vertical metrics keep at least one full task row visible at the
 * declared 300dp minimum height.
 */
fun streakWidgetMetrics(heightDp: Float): StreakWidgetMetrics {
    val compact = heightDp < 360f
    val padding = if (compact) 10 else 14
    val cellHeight = if (compact) 26 else 30
    val fixedContent = FIXED_HEADER_HEIGHT_DP + CALENDAR_WEEKS * cellHeight
    return StreakWidgetMetrics(
        outerPaddingDp = padding,
        cellHeightDp = cellHeight,
        taskRowHeightDp = STREAK_WIDGET_TASK_ROW_HEIGHT_DP,
        fixedContentHeightDp = fixedContent,
    )
}

fun streakWidgetTaskAreaDp(heightDp: Float): Int {
    val metrics = streakWidgetMetrics(heightDp)
    return (heightDp - 2 * metrics.outerPaddingDp - metrics.fixedContentHeightDp)
        .toInt()
        .coerceAtLeast(0)
}

fun streakWidgetUnboundInstruction(hasTrackers: Boolean): String =
    if (hasTrackers) "Tap to choose a tracker" else "Tap to open the picker"

private const val FIXED_HEADER_HEIGHT_DP = 84
private const val CALENDAR_WEEKS = 6

fun streakWidgetTracker(state: AppState, appWidgetId: Int): StreakTracker? {
    val trackerId = state.streakWidgetBindings[appWidgetId.toString()] ?: return null
    return state.streakTrackers.firstOrNull { it.id == trackerId }
}

fun streakWidgetRows(
    tracker: StreakTracker,
    today: LocalDate,
): List<StreakWidgetTaskRow> =
    orderedTasks(tracker.tasks).map { task ->
        StreakWidgetTaskRow(
            task = task,
            streak = currentStreak(task, today),
            doneToday = isDoneOn(task, today),
        )
    }

fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
    WeekFields.of(locale).firstDayOfWeek

fun orderedWeekdayLabels(
    firstDayOfWeek: DayOfWeek,
    locale: Locale = Locale.getDefault(),
): List<String> {
    val days = DayOfWeek.entries
    val start = days.indexOf(firstDayOfWeek)
    return (0 until 7).map { offset ->
        days[(start + offset) % 7].getDisplayName(TextStyle.SHORT, locale)
    }
}
