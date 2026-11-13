package dev.abhiram.checklist.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

suspend fun updateStreakWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(StreakWidget::class.java).forEach { glanceId ->
        StreakWidget().update(context, glanceId)
    }
}
