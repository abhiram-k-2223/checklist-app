package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist

/**
 * Binds one app widget to an existing checklist. Unknown checklist IDs are a
 * no-op, and a new binding replaces any previous binding for the same widget.
 */
fun bindWidget(state: AppState, appWidgetId: Int, checklistId: String): AppState {
    if (state.checklists.none { it.id == checklistId }) return state
    val key = appWidgetId.toString()
    if (state.widgetBindings[key] == checklistId) return state
    return state.copy(widgetBindings = state.widgetBindings + (key to checklistId))
}

/** Removes the binding for one app widget; other widgets are untouched. */
fun unbindWidget(state: AppState, appWidgetId: Int): AppState {
    val key = appWidgetId.toString()
    if (!state.widgetBindings.containsKey(key)) return state
    return state.copy(widgetBindings = state.widgetBindings - key)
}

/**
 * Resolves the checklist one widget should show. A binding always wins, even
 * when it points at a deleted checklist (then the result is null and the widget
 * shows its unbound state). Only unbound widgets fall back to the pin.
 */
fun checklistForWidget(state: AppState, appWidgetId: Int): Checklist? {
    val key = appWidgetId.toString()
    if (state.widgetBindings.containsKey(key)) {
        val boundId = state.widgetBindings[key]
        return state.checklists.firstOrNull { it.id == boundId }
    }
    return state.checklists.firstOrNull { it.isPinned }
}

/**
 * Returns the pinned checklist ID when this widget has no binding yet, or null
 * when it is already bound or nothing is pinned. Used for one-time legacy
 * migration; it never overrides an existing binding.
 */
fun bindingMigrationTarget(state: AppState, appWidgetId: Int): String? {
    if (state.widgetBindings.containsKey(appWidgetId.toString())) return null
    return state.checklists.firstOrNull { it.isPinned }?.id
}
