package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.Checklist

fun progressLabel(checklist: Checklist): String {
    val done = checklist.items.count { it.isDone }
    val total = checklist.items.size
    return if (total == 0) "No items" else "$done of $total done"
}
