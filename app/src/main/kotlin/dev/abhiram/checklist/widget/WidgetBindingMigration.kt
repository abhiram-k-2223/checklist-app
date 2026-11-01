package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.bindingMigrationTarget
import dev.abhiram.checklist.data.model.AppState

/**
 * Persists a legacy unconfigured widget's fallback checklist as a real binding.
 * The returned state is used as the first composition value so the first frame
 * already resolves the binding instead of briefly following the pin.
 */
suspend fun migrateWidgetBinding(
    state: AppState,
    appWidgetId: Int,
    setBinding: suspend (Int, String) -> Unit,
): AppState {
    val checklistId = bindingMigrationTarget(state, appWidgetId) ?: return state
    setBinding(appWidgetId, checklistId)
    return state.copy(
        widgetBindings = state.widgetBindings + (appWidgetId.toString() to checklistId),
    )
}
