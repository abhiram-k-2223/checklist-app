package dev.abhiram.checklist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChecklistApp(viewModel: ChecklistViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = uiState.selectedChecklist

    if (selected == null) {
        ListsScreen(
            checklists = uiState.checklists,
            onOpenChecklist = viewModel::openChecklist,
            onCreateChecklist = viewModel::createChecklist,
            onRenameChecklist = viewModel::renameChecklist,
            onDeleteChecklist = viewModel::deleteChecklist,
            onSetPinned = viewModel::setPinned,
        )
    } else {
        BackHandler { viewModel.closeChecklist() }
        ChecklistScreen(
            checklist = selected,
            onBack = viewModel::closeChecklist,
            onAddItem = { text -> viewModel.addItem(selected.id, text) },
            onUpdateItem = viewModel::updateItem,
            onToggleItem = viewModel::toggleItem,
            onDeleteItem = viewModel::deleteItem,
            onRenameChecklist = { title -> viewModel.renameChecklist(selected.id, title) },
            onDeleteChecklist = { viewModel.deleteChecklist(selected.id) },
            onTogglePin = {
                viewModel.setPinned(if (selected.isPinned) null else selected.id)
            },
        )
    }
}
