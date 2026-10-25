package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

data class WidgetRowSet(
    val visible: List<ChecklistItem>,
    val hiddenCount: Int,
)

fun widgetRows(checklist: Checklist, maxRows: Int): WidgetRowSet {
    val sorted = checklist.items.sortedWith(
        compareBy<ChecklistItem> { it.isDone }.thenBy { it.position },
    )
    val cap = maxRows.coerceAtLeast(1)
    return WidgetRowSet(
        visible = sorted.take(cap),
        hiddenCount = (sorted.size - cap).coerceAtLeast(0),
    )
}

fun maxWidgetRows(heightDp: Float): Int {
    val availableForRows = (heightDp - 72f).coerceAtLeast(0f)
    return (availableForRows / 41f).toInt().coerceIn(1, 24)
}
