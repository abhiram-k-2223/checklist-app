package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

interface ChecklistRepository {
    val state: StateFlow<AppState>
    suspend fun snapshot(): AppState
    suspend fun createChecklist(title: String): String
    suspend fun renameChecklist(id: String, title: String)
    suspend fun deleteChecklist(id: String)
    suspend fun setPinned(id: String?)
    suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String)
    suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String)
    suspend fun removeWidgetBinding(appWidgetId: Int)
    suspend fun addItem(checklistId: String, text: String): String
    suspend fun updateItem(itemId: String, text: String)
    suspend fun toggleItem(itemId: String)
    suspend fun deleteItem(itemId: String)
    suspend fun createStreakTracker(title: String): String
    suspend fun renameStreakTracker(id: String, title: String)
    suspend fun deleteStreakTracker(id: String)
    suspend fun addStreakTask(trackerId: String, text: String, color: StreakColor): String
    suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor)
    suspend fun deleteStreakTask(taskId: String)
    suspend fun toggleStreakTask(taskId: String, date: LocalDate)
    suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String)
    suspend fun removeStreakWidgetBinding(appWidgetId: Int)
}
