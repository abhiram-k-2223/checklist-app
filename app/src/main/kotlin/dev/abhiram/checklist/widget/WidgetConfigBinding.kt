package dev.abhiram.checklist.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.unboundWidgetIds

/**
 * Writes the picked checklist and refreshes the widget. A null selection (Back
 * or the empty state) writes nothing and reports that the picker was cancelled.
 */
suspend fun bindSelectedChecklist(
    checklistId: String?,
    setBinding: suspend (String) -> Unit,
    refresh: suspend () -> Unit,
): Boolean {
    if (checklistId == null) return false
    setBinding(checklistId)
    refresh()
    return true
}

/**
 * Gives every existing unbound widget the pinned checklist, except the widget
 * currently being configured. This keeps the old widget on its checklist before
 * the user can change the pin.
 */
suspend fun migrateUnboundWidgets(
    context: Context,
    repository: ChecklistRepository,
    excludingAppWidgetId: Int? = null,
) {
    val state = repository.snapshot()
    val pinned = state.checklists.firstOrNull { it.isPinned } ?: return
    val manager = GlanceAppWidgetManager(context)
    val appWidgetIds = manager.getGlanceIds(ChecklistWidget::class.java)
        .map { manager.getAppWidgetId(it) }
    unboundWidgetIds(
        bindings = state.widgetBindings,
        appWidgetIds = appWidgetIds,
        excludingAppWidgetId = excludingAppWidgetId,
    ).forEach { appWidgetId ->
        repository.setWidgetBindingIfUnbound(appWidgetId, pinned.id)
    }
}
