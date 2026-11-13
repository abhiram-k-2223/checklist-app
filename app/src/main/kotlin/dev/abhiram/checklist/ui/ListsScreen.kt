@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog
import java.time.LocalDate

@Composable
fun ListsScreen(
    checklists: List<Checklist>,
    streakTrackers: List<StreakTracker>,
    today: LocalDate,
    onOpenChecklist: (String) -> Unit,
    onCreateChecklist: (String) -> Unit,
    onRenameChecklist: (String, String) -> Unit,
    onDeleteChecklist: (String) -> Unit,
    onSetPinned: (String?) -> Unit,
    onOpenStreakTracker: (String) -> Unit,
    onCreateStreakTracker: (String) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showCreateStreak by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Checklist?>(null) }
    var deleteTarget by remember { mutableStateOf<Checklist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Checklists") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "New checklist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (checklists.isEmpty()) {
                item(key = "checklist-empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No checklists yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap + to make your first one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(checklists, key = { it.id }) { checklist ->
                    ListCard(
                        checklist = checklist,
                        onOpen = { onOpenChecklist(checklist.id) },
                        onRename = { renameTarget = checklist },
                        onPinToggle = {
                            onSetPinned(if (checklist.isPinned) null else checklist.id)
                        },
                        onRequestDelete = { deleteTarget = checklist },
                    )
                }
            }

            item(key = "streaks-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Streaks",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { showCreateStreak = true }) {
                        Text("New streak")
                    }
                }
            }

            if (streakTrackers.isEmpty()) {
                item(key = "streaks-empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No streak trackers yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { showCreateStreak = true }) {
                            Text("New streak")
                        }
                    }
                }
            } else {
                items(streakTrackers, key = { "streak-${it.id}" }) { tracker ->
                    StreakTrackerCard(
                        tracker = tracker,
                        today = today,
                        onOpen = { onOpenStreakTracker(tracker.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New checklist",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { title ->
                showCreate = false
                onCreateChecklist(title)
            },
        )
    }

    if (showCreateStreak) {
        TextInputDialog(
            title = "New streak",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateStreak = false },
            onConfirm = { title ->
                showCreateStreak = false
                onCreateStreakTracker(title)
            },
        )
    }

    renameTarget?.let { checklist ->
        TextInputDialog(
            title = "Rename checklist",
            initialValue = checklist.title,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                renameTarget = null
                onRenameChecklist(checklist.id, title)
            },
        )
    }

    deleteTarget?.let { checklist ->
        ConfirmDeleteDialog(
            title = "Delete checklist?",
            message = "\"${checklist.title}\" and its items will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteChecklist(checklist.id)
            },
        )
    }
}

@Composable
private fun StreakTrackerCard(
    tracker: StreakTracker,
    today: LocalDate,
    onOpen: () -> Unit,
) {
    val colors = remember(tracker.tasks) { streakTrackerColors(tracker) }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tracker.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(streakColor(color), CircleShape),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (colors.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = streakTrackerSummary(tracker, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ListCard(
    checklist: Checklist,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onPinToggle: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checklist.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = checklist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = progressLabel(checklist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "List actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (checklist.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            menuOpen = false
                            onPinToggle()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onRequestDelete()
                        },
                    )
                }
            }
        }
    }
}
