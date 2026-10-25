package dev.abhiram.checklist.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.Checklist
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

    fun createChecklist(title: String) {
        viewModelScope.launch {
            openChecklist(repository.createChecklist(title))
        }
    }

    fun renameChecklist(id: String, title: String) {
        viewModelScope.launch { repository.renameChecklist(id, title) }
    }

    fun deleteChecklist(id: String) {
        viewModelScope.launch {
            repository.deleteChecklist(id)
            if (selectedId.value == id) {
                closeChecklist()
            }
        }
    }

    fun setPinned(id: String?) {
        viewModelScope.launch { repository.setPinned(id) }
    }

    fun addItem(checklistId: String, text: String) {
        viewModelScope.launch { repository.addItem(checklistId, text) }
    }

    fun updateItem(itemId: String, text: String) {
        viewModelScope.launch { repository.updateItem(itemId, text) }
    }

    fun toggleItem(itemId: String) {
        viewModelScope.launch { repository.toggleItem(itemId) }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch { repository.deleteItem(itemId) }
    }

    companion object {
        private const val KEY_SELECTED = "selected_checklist_id"

        fun factory(
            owner: SavedStateRegistryOwner,
            repository: ChecklistRepository,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, null) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = ChecklistViewModel(repository, handle) as T
            }
    }
}
