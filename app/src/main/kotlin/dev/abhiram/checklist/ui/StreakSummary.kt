package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import java.time.LocalDate

fun streakTrackerSummary(tracker: StreakTracker, today: LocalDate): String {
    val tasks = orderedTasks(tracker.tasks)
    if (tasks.isEmpty()) return "No tasks"
    val done = tasks.count { isDoneOn(it, today) }
    val taskLabel = if (tasks.size == 1) "1 task" else "${tasks.size} tasks"
    return "$taskLabel · $done done today"
}

fun streakTrackerColors(tracker: StreakTracker): List<StreakColor> =
    orderedTasks(tracker.tasks).take(MAX_STREAK_TASKS).map { it.color }
