package dev.abhiram.checklist.ui

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.Checklist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val checklists: List<Checklist> = emptyList(),
    val selectedId: String? = null,
) {
    val selectedChecklist: Checklist?
        get() = checklists.firstOrNull { it.id == selectedId }
}

class ChecklistViewModel(
    private val repository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle,
    private val onError: (String, Throwable) -> Unit = { message, error ->
        Log.e("ChecklistViewModel", message, error)
    },
    private val refreshWidgets: suspend () -> Unit = {},
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<ChecklistUiState> = combine(repository.state, selectedId) { state, requested ->
        val selected = requested?.takeIf { id -> state.checklists.any { it.id == id } }
        ChecklistUiState(checklists = state.checklists, selectedId = selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState(),
    )

    fun openChecklist(id: String) {
        savedStateHandle[KEY_SELECTED] = id
    }

    fun closeChecklist() {
        savedStateHandle[KEY_SELECTED] = null
    }

    private fun launchMutation(name: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError("$name failed", error)
                return@launch
            }

            try {
                refreshWidgets()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError("Widget refresh failed", error)
            }
        }
    }

    fun createChecklist(title: String) {
        launchMutation("Create checklist") {
            openChecklist(repository.createChecklist(title))
        }
    }

    fun renameChecklist(id: String, title: String) {
        launchMutation("Rename checklist") { repository.renameChecklist(id, title) }
    }

    fun deleteChecklist(id: String) {
        launchMutation("Delete checklist") {
            repository.deleteChecklist(id)
            if (selectedId.value == id) {
                closeChecklist()
            }
        }
    }

    fun setPinned(id: String?) {
        launchMutation("Pin checklist") { repository.setPinned(id) }
    }

    fun addItem(checklistId: String, text: String) {
        launchMutation("Add item") { repository.addItem(checklistId, text) }
    }

    fun updateItem(itemId: String, text: String) {
        launchMutation("Update item") { repository.updateItem(itemId, text) }
    }

    fun toggleItem(itemId: String) {
        launchMutation("Toggle item") { repository.toggleItem(itemId) }
    }

    fun deleteItem(itemId: String) {
        launchMutation("Delete item") { repository.deleteItem(itemId) }
    }

    companion object {
        private const val KEY_SELECTED = "selected_checklist_id"

        fun factory(
            owner: SavedStateRegistryOwner,
            repository: ChecklistRepository,
            refreshWidgets: suspend () -> Unit,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, null) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = ChecklistViewModel(repository, handle, refreshWidgets = refreshWidgets) as T
            }
    }
}
