@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog

@Composable
fun ChecklistScreen(
    checklist: Checklist,
    onBack: () -> Unit,
    onAddItem: (String) -> Unit,
    onUpdateItem: (String, String) -> Unit,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRenameChecklist: (String) -> Unit,
    onDeleteChecklist: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val orderedItems = remember(checklist.items) {
        checklist.items.sortedBy { it.position }
    }
    val done = remember(checklist.items) { checklist.items.count { it.isDone } }
    val total = checklist.items.size

    var addText by rememberSaveable { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ChecklistItem?>(null) }
    var deleteTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var checklistMenuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val submit: () -> Unit = {
        val clean = addText.trim()
        if (clean.isNotEmpty()) {
            onAddItem(clean)
            addText = ""
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = checklist.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (total > 0) {
                            Text(
                                text = "$done of $total done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
                        IconButton(onClick = { checklistMenuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Checklist actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = checklistMenuOpen,
                            onDismissRequest = { checklistMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (checklist.isPinned) "Unpin" else "Pin") },
                                onClick = {
                                    checklistMenuOpen = false
                                    onTogglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    checklistMenuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    checklistMenuOpen = false
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = addText,
                        onValueChange = { addText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Add an item") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                    Spacer(Modifier.size(10.dp))
                    FilledIconButton(
                        onClick = submit,
                        enabled = addText.isNotBlank(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add item",
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (orderedItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Add the first item below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(orderedItems, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        onToggle = { onToggleItem(item.id) },
                        onEdit = { editingItem = item },
                        onRequestDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }

    editingItem?.let { item ->
        TextInputDialog(
            title = "Edit item",
            initialValue = item.text,
            confirmLabel = "Save",
            onDismiss = { editingItem = null },
            onConfirm = { text ->
                editingItem = null
                onUpdateItem(item.id, text)
            },
        )
    }

    deleteTarget?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete item?",
            message = "\"${item.text}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteItem(item.id)
            },
        )
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename checklist",
            initialValue = checklist.title,
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { title ->
                showRename = false
                onRenameChecklist(title)
            },
        )
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "Delete checklist?",
            message = "\"${checklist.title}\" and all of its items will be removed.",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDeleteChecklist()
            },
        )
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (item.isDone) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_circle_outline
                    },
                ),
                contentDescription = if (item.isDone) "Mark not done" else "Mark done",
                tint = if (item.isDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = item.text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
            color = if (item.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Item actions",
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
