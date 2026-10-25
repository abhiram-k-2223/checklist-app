package dev.abhiram.checklist

data class WidgetLaunch(val checklistId: String?)

fun widgetLaunch(fromWidget: Boolean, checklistId: String?): WidgetLaunch? =
    if (fromWidget) WidgetLaunch(checklistId) else null
