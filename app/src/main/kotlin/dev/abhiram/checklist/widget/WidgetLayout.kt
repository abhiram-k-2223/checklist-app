package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.ChecklistItem

/**
 * Items in widget order: unchecked first, then completed, each in stored order.
 * The widget renders every item in a scrollable list, so nothing is truncated.
 */
fun sortedWidgetItems(items: List<ChecklistItem>): List<ChecklistItem> =
    items.sortedWith(
        compareBy<ChecklistItem> { it.isDone }.thenBy { it.position },
    )

/** Below this height the widget uses a single-line header and tighter padding. */
fun isCompactWidgetHeight(heightDp: Float): Boolean = heightDp < COMPACT_HEIGHT_DP

private const val COMPACT_HEIGHT_DP = 160f
