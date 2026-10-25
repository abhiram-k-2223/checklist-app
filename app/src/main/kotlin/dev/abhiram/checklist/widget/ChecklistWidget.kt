package dev.abhiram.checklist.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

private val WidgetBackground = Color(0xFF1C1C1E)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)

private val ITEM_ID_KEY = ActionParameters.Key<String>("item_id")

class ChecklistWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        val checklist = repository.snapshot().checklists.firstOrNull { it.isPinned }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            checklist?.let { putExtra(EXTRA_OPEN_CHECKLIST, it.id) }
        }
        provideContent {
            WidgetContent(checklist = checklist, openIntent = openIntent)
        }
    }

    companion object {
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
        app.repository.toggleItem(itemId)
        // The application-scope observer performs the single debounced refresh.
    }
}

@Composable
private fun WidgetContent(checklist: Checklist?, openIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProviderCompat(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(16.dp),
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
                        color = ColorProviderCompat(WidgetText),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Open the app to choose one",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            val done = checklist.items.count { it.isDone }
            val total = checklist.items.size
            val size = LocalSize.current
            val rowSet = widgetRows(checklist, maxWidgetRows(size.height.value))

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openIntent)),
            ) {
                Text(
                    text = checklist.title,
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetText),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (total == 0) "No items" else "$done/$total",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Spacer(
                GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProviderCompat(WidgetSeparator)),
            )

            if (rowSet.visible.isEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "Nothing to do yet",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                )
            } else {
                rowSet.visible.forEach { item ->
                    WidgetItemRow(item)
                    Spacer(
                        GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProviderCompat(WidgetSeparator)),
                    )
                }
                if (rowSet.hiddenCount > 0) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = "+${rowSet.hiddenCount} more",
                        style = TextStyle(
                            color = ColorProviderCompat(WidgetMuted),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
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
            .height(40.dp),
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
                color = ColorProviderCompat(if (item.isDone) WidgetMuted else WidgetText),
                fontSize = 15.sp,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            ),
            maxLines = 2,
        )
    }
}

private fun ColorProviderCompat(color: Color) = androidx.glance.unit.ColorProvider(color)
