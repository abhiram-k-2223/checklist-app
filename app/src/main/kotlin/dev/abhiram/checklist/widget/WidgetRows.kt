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
    val cap = maxRows.coerceAtLeast(0)
    val visible = sorted.take(cap)
    return WidgetRowSet(
        visible = visible,
        hiddenCount = sorted.size - visible.size,
    )
}

fun maxWidgetRows(heightDp: Float): Int {
    // 32 dp outer padding + ~49 dp header + 20 dp overflow line = 101 dp fixed.
    // Each row is 44 dp plus a 1 dp separator.
    // Glance RemoteViews allows at most 10 children per container: header +
    // eight item containers + the overflow line. Cap at eight visible rows.
    val availableForRows = (heightDp - 101f).coerceAtLeast(0f)
    return (availableForRows / 45f).toInt().coerceIn(0, 8)
}
