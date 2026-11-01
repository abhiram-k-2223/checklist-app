package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import kotlinx.coroutines.flow.StateFlow

interface ChecklistRepository {
    val state: StateFlow<AppState>
    suspend fun snapshot(): AppState
    suspend fun createChecklist(title: String): String
    suspend fun renameChecklist(id: String, title: String)
    suspend fun deleteChecklist(id: String)
    suspend fun setPinned(id: String?)
    suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String)
    suspend fun removeWidgetBinding(appWidgetId: Int)
    suspend fun addItem(checklistId: String, text: String): String
    suspend fun updateItem(itemId: String, text: String)
    suspend fun toggleItem(itemId: String)
    suspend fun deleteItem(itemId: String)
}
