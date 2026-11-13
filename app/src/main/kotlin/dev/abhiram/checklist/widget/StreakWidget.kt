package dev.abhiram.checklist.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.StreakCalendarDay
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.streakCalendarMonth
import dev.abhiram.checklist.ui.compactStreakLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException

private val WidgetBackground = Color(0xFF000000)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)
private val WidgetOtherMonth = Color(0xFF48484A)
private val WidgetToday = Color(0xFF1C1C1E)

class StreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val application = context.applicationContext as ChecklistApplication
        val repository = application.repository
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = try {
            repository.snapshot()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to read streak state", error)
            AppState()
        }

        provideContent {
            val state by repository.state.collectAsState(initial = initial)
            val tracker = streakWidgetTracker(state, appWidgetId)
            val openIntent = remember {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    putExtra(EXTRA_FROM_WIDGET, true)
                }
            }
            StreakWidgetContent(
                tracker = tracker,
                hasTrackers = state.streakTrackers.isNotEmpty(),
                openIntent = openIntent,
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            (context.applicationContext as ChecklistApplication)
                .repository
                .removeStreakWidgetBinding(appWidgetId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to remove streak widget binding", error)
        }
        super.onDelete(context, glanceId)
    }

    companion object {
        const val TAG = "StreakWidget"
        const val EXTRA_FROM_WIDGET = "from_streak_widget"
        internal val TASK_ID_KEY = ActionParameters.Key<String>("streak_task_id")
    }
}

class ToggleStreakTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[StreakWidget.TASK_ID_KEY] ?: return
        val repository = (context.applicationContext as ChecklistApplication).repository
        val today = LocalDate.now(ZoneId.systemDefault())
        var toggled = false
        WidgetToggleRunner(
            toggle = {
                repository.toggleStreakTask(taskId, today)
                toggled = true
            },
            refresh = {
                if (toggled) StreakWidget().update(context, glanceId)
            },
            onError = { stage, error ->
                Log.e(StreakWidget.TAG, "Widget $stage failed", error)
            },
        ).run(taskId)
    }
}

@Composable
private fun StreakWidgetContent(
    tracker: StreakTracker?,
    hasTrackers: Boolean,
    openIntent: Intent,
) {
    val outerPadding = if (LocalSize.current.height.value < 360f) 10.dp else 14.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(outerPadding),
    ) {
        if (tracker == null) {
            UnboundContent(hasTrackers = hasTrackers, openIntent = openIntent)
        } else {
            BoundContent(tracker = tracker, openIntent = openIntent)
        }
    }
}

@Composable
private fun UnboundContent(hasTrackers: Boolean, openIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(openIntent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (hasTrackers) "Choose a streak tracker" else "Create a streak tracker in the app",
            style = TextStyle(
                color = ColorProvider(WidgetText),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Open the app to choose one",
            style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 12.sp),
        )
    }
}

@Composable
private fun BoundContent(tracker: StreakTracker, openIntent: Intent) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val month = YearMonth.from(today)
    val locale = Locale.getDefault()
    val firstDay = firstDayOfWeek(locale)
    val days = streakCalendarMonth(month, tracker.tasks, today, firstDay)
    val rows = streakWidgetRows(tracker, today)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openIntent)),
        ) {
            Text(
                text = tracker.title,
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            WeekdayHeader(firstDay = firstDay, locale = locale)
            Spacer(GlanceModifier.height(4.dp))
            CalendarGrid(days = days)
        }
        Spacer(GlanceModifier.height(10.dp))
        StreakDivider()
        Spacer(GlanceModifier.height(6.dp))
        if (rows.isEmpty()) {
            Text(
                text = "Add daily tasks in the app",
                style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 12.sp),
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
            ) {
                items(rows, itemId = { it.task.id.hashCode().toLong() }) { row ->
                    StreakTaskRow(row = row)
                    StreakDivider()
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(firstDay: DayOfWeek, locale: Locale) {
    val labels = orderedWeekdayLabels(firstDay, locale)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        labels.forEach { label ->
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 9.sp),
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(days: List<StreakCalendarDay>) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        days.chunked(7).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: StreakCalendarDay, modifier: GlanceModifier) {
    val cellModifier = if (day.isToday) {
        modifier
            .height(30.dp)
            .background(ColorProvider(WidgetToday))
            .cornerRadius(8.dp)
    } else {
        modifier.height(30.dp)
    }
    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = TextStyle(
                    color = ColorProvider(
                        if (day.inCurrentMonth) WidgetText else WidgetOtherMonth,
                    ),
                    fontSize = 10.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                ),
            )
            Spacer(GlanceModifier.height(1.dp))
            Row(horizontalAlignment = Alignment.CenterHorizontally) {
                day.dots.forEach { color ->
                    Box(
                        modifier = GlanceModifier
                            .size(4.dp)
                            .background(ColorProvider(Color(color.argb)))
                            .cornerRadius(2.dp),
                    ) { }
                    Spacer(GlanceModifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
private fun StreakTaskRow(row: StreakWidgetTaskRow) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(
                actionRunCallback<ToggleStreakTaskAction>(
                    actionParametersOf(StreakWidget.TASK_ID_KEY to row.task.id),
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(7.dp)
                .background(ColorProvider(Color(row.task.color.argb)))
                .cornerRadius(4.dp),
        ) { }
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.task.text,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(if (row.doneToday) WidgetMuted else WidgetText),
                fontSize = 13.sp,
            ),
            maxLines = 1,
        )
        Text(
            text = compactStreakLabel(row.streak),
            style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 11.sp),
        )
        Spacer(GlanceModifier.width(8.dp))
        Image(
            provider = ImageProvider(
                if (row.doneToday) R.drawable.ic_widget_check else R.drawable.ic_widget_circle,
            ),
            contentDescription = if (row.doneToday) "Done" else "Not done",
            modifier = GlanceModifier.size(20.dp),
        )
    }
}

@Composable
private fun StreakDivider() {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(WidgetSeparator)),
    )
}
