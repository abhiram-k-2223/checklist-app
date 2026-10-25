package dev.abhiram.checklist.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Updates every Checklist widget directly, without enqueueing WorkManager work.
 *
 * The update is awaited by the caller (app mutation coroutine or widget action),
 * so a change cannot be lost if the process loses priority after the callback
 * returns. Each update is idempotent.
 */
suspend fun updateChecklistWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(ChecklistWidget::class.java).forEach { glanceId ->
        ChecklistWidget().update(context, glanceId)
    }
}
