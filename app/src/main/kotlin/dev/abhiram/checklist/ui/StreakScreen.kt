@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.currentStreak
import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog
import java.time.LocalDate

@Composable
fun StreakScreen(
    tracker: StreakTracker,
    today: LocalDate,
    onBack: () -> Unit,
    onRenameTracker: (String) -> Unit,
    onDeleteTracker: () -> Unit,
    onAddTask: (String, StreakColor) -> Unit,
    onUpdateTask: (String, String, StreakColor) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleToday: (String) -> Unit,
) {
    val ordered = remember(tracker.tasks) { orderedTasks(tracker.tasks) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StreakTask?>(null) }
    var deleteTarget by remember { mutableStateOf<StreakTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tracker.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Streak tracker actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename tracker") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete tracker", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { showAddTask = true },
                        enabled = ordered.size < MAX_STREAK_TASKS,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add task")
                    }
                    if (ordered.size >= MAX_STREAK_TASKS) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Maximum 4 tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (ordered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Add up to 4 daily tasks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Each task gets its own color and streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(ordered, key = { it.id }) { task ->
                    StreakTaskRow(
                        task = task,
                        today = today,
                        onToggle = { onToggleToday(task.id) },
                        onEdit = { editingTask = task },
                        onRequestDelete = { deleteTarget = task },
                    )
                }
            }
        }
    }

    if (showAddTask) {
        StreakTaskDialog(
            title = "Add task",
            initialText = "",
            initialColor = null,
            tasks = ordered,
            editingTaskId = null,
            onDismiss = { showAddTask = false },
            onConfirm = { text, color ->
                showAddTask = false
                onAddTask(text, color)
            },
        )
    }

    editingTask?.let { task ->
        StreakTaskDialog(
            title = "Edit task",
            initialText = task.text,
            initialColor = task.color,
            tasks = ordered,
            editingTaskId = task.id,
            onDismiss = { editingTask = null },
            onConfirm = { text, color ->
                editingTask = null
                onUpdateTask(task.id, text, color)
            },
        )
    }

    deleteTarget?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete task?",
            message = "\"${task.text}\" and its past calendar dots and streak history will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteTask(task.id)
            },
        )
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename streak tracker",
            initialValue = tracker.title,
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { title ->
                showRename = false
                onRenameTracker(title)
            },
        )
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "Delete streak tracker?",
            message = "\"${tracker.title}\", all of its tasks and history will be removed. Bound widgets will become unbound.",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDeleteTracker()
            },
        )
    }
}

@Composable
private fun StreakTaskRow(
    task: StreakTask,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val done = isDoneOn(task, today)
    val streak = currentStreak(task, today)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(streakColor(task.color), CircleShape),
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = streakLabel(streak),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (done) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_circle_outline
                    },
                ),
                contentDescription = if (done) "Mark not done" else "Mark done",
                tint = if (done) {
                    streakColor(task.color)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Task actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuOpen = false
                        onEdit()
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

@Composable
private fun StreakTaskDialog(
    title: String,
    initialText: String,
    initialColor: StreakColor?,
    tasks: List<StreakTask>,
    editingTaskId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, StreakColor) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var color by remember { mutableStateOf(initialColor) }
    val available = remember(tasks, editingTaskId) {
        availableStreakColors(tasks, editingTaskId).toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Task name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StreakColor.entries.forEach { candidate ->
                        val enabled = candidate in available
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .selectable(
                                    selected = color == candidate,
                                    enabled = enabled,
                                    role = Role.RadioButton,
                                    onClick = { color = candidate },
                                )
                                .semantics {
                                    contentDescription = streakColorName(candidate)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = streakColor(candidate).copy(
                                            alpha = if (enabled) 1f else 0.25f,
                                        ),
                                        shape = CircleShape,
                                    )
                                    .border(
                                        width = if (color == candidate) 3.dp else 0.dp,
                                        color = if (color == candidate) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
                if (available.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All colors are in use",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { color?.let { onConfirm(text.trim(), it) } },
                enabled = text.isNotBlank() && color != null,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
