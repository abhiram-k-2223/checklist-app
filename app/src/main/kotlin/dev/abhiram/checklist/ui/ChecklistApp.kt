package dev.abhiram.checklist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChecklistApp(
    checklistViewModel: ChecklistViewModel,
    streakViewModel: StreakViewModel,
) {
    val checklistState by checklistViewModel.uiState.collectAsStateWithLifecycle()
    val streakState by streakViewModel.uiState.collectAsStateWithLifecycle()
    val selectedChecklist = checklistState.selectedChecklist
    val selectedTracker = streakState.selectedTracker

    when {
        selectedChecklist != null -> {
            BackHandler { checklistViewModel.closeChecklist() }
            ChecklistScreen(
                checklist = selectedChecklist,
                onBack = checklistViewModel::closeChecklist,
                onAddItem = { text -> checklistViewModel.addItem(selectedChecklist.id, text) },
                onUpdateItem = checklistViewModel::updateItem,
                onToggleItem = checklistViewModel::toggleItem,
                onDeleteItem = checklistViewModel::deleteItem,
                onRenameChecklist = { title ->
                    checklistViewModel.renameChecklist(selectedChecklist.id, title)
                },
                onDeleteChecklist = { checklistViewModel.deleteChecklist(selectedChecklist.id) },
                onTogglePin = {
                    checklistViewModel.setPinned(
                        if (selectedChecklist.isPinned) null else selectedChecklist.id,
                    )
                },
            )
        }

        selectedTracker != null -> {
            BackHandler { streakViewModel.closeTracker() }
            StreakScreen(
                tracker = selectedTracker,
                today = streakState.today,
                onBack = streakViewModel::closeTracker,
                onRenameTracker = { title ->
                    streakViewModel.renameTracker(selectedTracker.id, title)
                },
                onDeleteTracker = { streakViewModel.deleteTracker(selectedTracker.id) },
                onAddTask = { text, color ->
                    streakViewModel.addTask(selectedTracker.id, text, color)
                },
                onUpdateTask = streakViewModel::updateTask,
                onDeleteTask = streakViewModel::deleteTask,
                onToggleToday = streakViewModel::toggleToday,
            )
        }

        else -> {
            ListsScreen(
                checklists = checklistState.checklists,
                streakTrackers = streakState.trackers,
                today = streakState.today,
                onOpenChecklist = { id ->
                    streakViewModel.closeTracker()
                    checklistViewModel.openChecklist(id)
                },
                onCreateChecklist = checklistViewModel::createChecklist,
                onRenameChecklist = checklistViewModel::renameChecklist,
                onDeleteChecklist = checklistViewModel::deleteChecklist,
                onSetPinned = checklistViewModel::setPinned,
                onOpenStreakTracker = { id ->
                    checklistViewModel.closeChecklist()
                    streakViewModel.openTracker(id)
                },
                onCreateStreakTracker = streakViewModel::createTracker,
            )
        }
    }
}
