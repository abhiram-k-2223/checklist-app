package dev.abhiram.checklist.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks

fun streakPickerSummary(tracker: StreakTracker): String =
    if (tracker.tasks.isEmpty()) "No tasks" else "${tracker.tasks.size} tasks"

fun streakPickerColors(tracker: StreakTracker): List<StreakColor> =
    orderedTasks(tracker.tasks).map { it.color }.take(MAX_STREAK_TASKS)

suspend fun bindSelectedStreakTracker(
    trackerId: String?,
    trackers: List<StreakTracker>,
    setBinding: suspend (String) -> Unit,
    refresh: suspend () -> Unit,
): Boolean {
    if (trackerId == null || trackers.none { it.id == trackerId }) return false
    setBinding(trackerId)
    refresh()
    return true
}

@Composable
fun StreakWidgetPicker(
    trackers: List<StreakTracker>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(trackers, key = { it.id }) { tracker ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(tracker.id) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tracker.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = streakPickerSummary(tracker),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        streakPickerColors(tracker).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(color.argb), CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}
