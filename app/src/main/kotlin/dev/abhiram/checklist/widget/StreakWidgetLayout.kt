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

data class StreakWidgetTaskRow(
    val task: StreakTask,
    val streak: Int,
    val doneToday: Boolean,
)

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
