package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate

object StreakStore {

    fun createTracker(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = ChecklistStore.requireText(title, "Streak tracker title")
        val tracker = StreakTracker(
            id = id,
            title = clean,
            createdAt = now,
            updatedAt = now,
        )
        return state.copy(streakTrackers = state.streakTrackers + tracker)
    }

    fun renameTracker(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = ChecklistStore.requireText(title, "Streak tracker title")
        if (state.streakTrackers.none { it.id == id }) return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { tracker ->
                if (tracker.id == id) tracker.copy(title = clean, updatedAt = now) else tracker
            },
        )
    }

    fun deleteTracker(state: AppState, id: String): AppState {
        if (state.streakTrackers.none { it.id == id }) return state
        return state.copy(
            streakTrackers = state.streakTrackers.filterNot { it.id == id },
            streakWidgetBindings = state.streakWidgetBindings.filterValues { it != id },
        )
    }

    fun addTask(
        state: AppState,
        trackerId: String,
        taskId: String,
        text: String,
        color: StreakColor,
        now: Long,
    ): AppState {
        val clean = ChecklistStore.requireText(text, "Task text")
        val tracker = state.streakTrackers.firstOrNull { it.id == trackerId } ?: return state
        require(tracker.tasks.size < MAX_STREAK_TASKS) {
            "A tracker can have at most $MAX_STREAK_TASKS tasks"
        }
        require(tracker.tasks.none { it.color == color }) {
            "That color is already used in this tracker"
        }
        require(state.streakTrackers.none { candidate ->
            candidate.tasks.any { it.id == taskId }
        }) {
            "Task ID already exists"
        }
        val task = StreakTask(
            id = taskId,
            text = clean,
            color = color,
            position = (tracker.tasks.maxOfOrNull { it.position } ?: -1) + 1,
            createdAt = now,
        )
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id == trackerId) {
                    candidate.copy(tasks = candidate.tasks + task, updatedAt = now)
                } else {
                    candidate
                }
            },
        )
    }

    fun updateTask(
        state: AppState,
        taskId: String,
        text: String,
        color: StreakColor,
        now: Long,
    ): AppState {
        val clean = ChecklistStore.requireText(text, "Task text")
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        require(tracker.tasks.none { it.id != taskId && it.color == color }) {
            "That color is already used in this tracker"
        }
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.map { task ->
                            if (task.id == taskId) {
                                task.copy(text = clean, color = color)
                            } else {
                                task
                            }
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun deleteTask(state: AppState, taskId: String, now: Long): AppState {
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.filterNot { it.id == taskId },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun toggleTask(state: AppState, taskId: String, date: LocalDate, now: Long): AppState {
        val isoDate = date.toString()
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.map { task ->
                            if (task.id != taskId) {
                                task
                            } else {
                                val completions = if (isoDate in task.completions) {
                                    task.completions - isoDate
                                } else {
                                    task.completions + isoDate
                                }
                                task.copy(completions = completions)
                            }
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun setStreakWidgetBinding(state: AppState, appWidgetId: Int, trackerId: String): AppState {
        if (state.streakTrackers.none { it.id == trackerId }) return state
        val key = appWidgetId.toString()
        if (state.streakWidgetBindings[key] == trackerId) return state
        return state.copy(
            streakWidgetBindings = state.streakWidgetBindings + (key to trackerId),
        )
    }

    fun removeStreakWidgetBinding(state: AppState, appWidgetId: Int): AppState {
        val key = appWidgetId.toString()
        if (!state.streakWidgetBindings.containsKey(key)) return state
        return state.copy(streakWidgetBindings = state.streakWidgetBindings - key)
    }

    fun streakTrackerForWidget(state: AppState, appWidgetId: Int): StreakTracker? {
        val trackerId = state.streakWidgetBindings[appWidgetId.toString()] ?: return null
        return state.streakTrackers.firstOrNull { it.id == trackerId }
    }
}
