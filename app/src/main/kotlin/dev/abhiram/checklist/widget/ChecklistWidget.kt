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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import kotlinx.coroutines.CancellationException

private val WidgetBackground = Color(0xFF000000)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)

private val ITEM_ID_KEY = ActionParameters.Key<String>("item_id")

class ChecklistWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        val initialState = try {
            repository.snapshot()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("ChecklistWidget", "Could not read widget state", error)
            AppState()
        }
        provideContent {
            // Glance does not restart provideGlance while a widget session is alive,
            // so the repository must be observed inside the composition. Otherwise
            // updates recompose against the list captured on the first render.
            val state by repository.state.collectAsState(initial = initialState)
            val checklist = state.checklists.firstOrNull { it.isPinned }
            val openIntent = remember(checklist?.id) {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    putExtra(EXTRA_FROM_WIDGET, true)
                    checklist?.let { putExtra(EXTRA_OPEN_CHECKLIST, it.id) }
                }
            }
            WidgetContent(checklist = checklist, openIntent = openIntent)
        }
    }

    companion object {
        const val EXTRA_FROM_WIDGET = "from_widget"
        const val EXTRA_OPEN_CHECKLIST = "open_checklist_id"
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[ITEM_ID_KEY] ?: return
        val app = context.applicationContext as ChecklistApplication
        WidgetToggleRunner(
            toggle = { id -> app.repository.toggleItem(id) },
            refresh = { ChecklistWidget().update(context, glanceId) },
            onError = { stage, error ->
                Log.e("ChecklistWidget", "Widget $stage failed", error)
            },
        ).run(itemId)
    }
}

@Composable
private fun WidgetContent(checklist: Checklist?, openIntent: Intent) {
    val compact = checklist != null && isCompactWidgetHeight(LocalSize.current.height.value)
    val outerPadding = if (compact) 12.dp else 16.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(outerPadding),
    ) {
        if (checklist == null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openIntent)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pin a checklist",
                    style = TextStyle(
                        color = ColorProvider(WidgetText),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Open the app to choose one",
                    style = TextStyle(
                        color = ColorProvider(WidgetMuted),
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            val items = sortedWidgetItems(checklist.items)

            WidgetHeader(checklist = checklist, compact = compact, openIntent = openIntent)
            Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
            WidgetDivider()

            if (items.isEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "Open app to add items",
                    modifier = GlanceModifier.clickable(actionStartActivity(openIntent)),
                    style = TextStyle(
                        color = ColorProvider(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            } else {
                // A collection-backed LazyColumn can scroll in the widget and is
                // not subject to the ten-direct-children limit of a plain Column.
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                ) {
                    items(
                        items = items,
                        itemId = { item -> item.id.hashCode().toLong() },
                    ) { item ->
                        Column {
                            WidgetItemRow(item)
                            WidgetDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(checklist: Checklist, compact: Boolean, openIntent: Intent) {
    val done = checklist.items.count { it.isDone }
    val total = checklist.items.size
    val progress = if (total == 0) "No items" else "$done/$total"

    if (compact) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = checklist.title,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = progress,
                style = TextStyle(
                    color = ColorProvider(WidgetMuted),
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    } else {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openIntent)),
        ) {
            Text(
                text = checklist.title,
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = progress,
                style = TextStyle(
                    color = ColorProvider(WidgetMuted),
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WidgetDivider() {
    Spacer(
        GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(WidgetSeparator)),
    )
}

@Composable
private fun WidgetItemRow(item: ChecklistItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionRunCallback<ToggleItemAction>(
                    actionParametersOf(ITEM_ID_KEY to item.id),
                ),
            )
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(
                if (item.isDone) R.drawable.ic_widget_check else R.drawable.ic_widget_circle,
            ),
            contentDescription = if (item.isDone) "Done" else "Not done",
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = item.text,
            style = TextStyle(
                color = ColorProvider(if (item.isDone) WidgetMuted else WidgetText),
                fontSize = 15.sp,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            ),
            maxLines = 2,
        )
    }
}
