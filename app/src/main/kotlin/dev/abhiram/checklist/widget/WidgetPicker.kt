package dev.abhiram.checklist.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist

/** Lists every checklist, with the pinned checklist first. */
fun pickerChecklists(checklists: List<Checklist>): List<Checklist> =
    checklists.sortedByDescending { it.isPinned }

/** Compact progress label used in the widget picker. */
fun pickerProgress(checklist: Checklist): String {
    val done = checklist.items.count { it.isDone }
    return "$done/${checklist.items.size}"
}

@Composable
fun WidgetPicker(
    checklists: List<Checklist>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(checklists, key = { it.id }) { checklist ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(checklist.id) },
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
                            text = checklist.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = pickerProgress(checklist),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (checklist.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = stringResource(R.string.widget_config_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
