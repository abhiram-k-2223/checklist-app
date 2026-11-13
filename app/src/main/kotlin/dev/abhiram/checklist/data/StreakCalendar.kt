package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class StreakCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val dots: List<StreakColor>,
)

fun isDoneOn(task: StreakTask, date: LocalDate): Boolean =
    date.toString() in task.completions

fun orderedTasks(tasks: List<StreakTask>): List<StreakTask> =
    tasks.sortedBy { it.position }

fun currentStreak(task: StreakTask, today: LocalDate): Int {
    var cursor = if (isDoneOn(task, today)) today else today.minusDays(1)
    var streak = 0
    while (isDoneOn(task, cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun streakCalendarMonth(
    month: YearMonth,
    tasks: List<StreakTask>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<StreakCalendarDay> {
    val ordered = orderedTasks(tasks)
    val firstOfMonth = month.atDay(1)
    val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val start = firstOfMonth.minusDays(offset.toLong())

    return (0 until 42).map { index ->
        val date = start.plusDays(index.toLong())
        StreakCalendarDay(
            date = date,
            inCurrentMonth = YearMonth.from(date) == month,
            isToday = date == today,
            dots = ordered.filter { isDoneOn(it, date) }.map { it.color },
        )
    }
}
