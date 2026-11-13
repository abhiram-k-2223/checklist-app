package dev.abhiram.checklist.ui

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class StreakUiState(
    val trackers: List<StreakTracker> = emptyList(),
    val selectedId: String? = null,
    val today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
) {
    val selectedTracker: StreakTracker?
        get() = trackers.firstOrNull { it.id == selectedId }
}

class StreakViewModel(
    private val repository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle,
    private val onError: (String, Throwable) -> Unit = { message, error ->
        Log.e("StreakViewModel", message, error)
    },
    private val refreshWidgets: suspend () -> Unit = {},
    private val today: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<StreakUiState> =
        combine(repository.state, selectedId) { state, requested ->
            val selected = requested?.takeIf { id ->
                state.streakTrackers.any { it.id == id }
            }
            StreakUiState(
                trackers = state.streakTrackers,
                selectedId = selected,
                today = today(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakUiState(today = today()),
        )

    fun openTracker(id: String) {
        savedStateHandle[KEY_SELECTED] = id
    }

    fun closeTracker() {
        savedStateHandle[KEY_SELECTED] = null
    }

    fun createTracker(title: String) {
        launchMutation("Create streak tracker") {
            openTracker(repository.createStreakTracker(title))
        }
    }

    fun renameTracker(id: String, title: String) {
        launchMutation("Rename streak tracker") {
            repository.renameStreakTracker(id, title)
        }
    }

    fun deleteTracker(id: String) {
        launchMutation("Delete streak tracker") {
            repository.deleteStreakTracker(id)
            if (selectedId.value == id) closeTracker()
        }
    }

    fun addTask(trackerId: String, text: String, color: StreakColor) {
        launchMutation("Add task") {
            repository.addStreakTask(trackerId, text, color)
        }
    }

    fun updateTask(taskId: String, text: String, color: StreakColor) {
        launchMutation("Update task") {
            repository.updateStreakTask(taskId, text, color)
        }
    }

    fun deleteTask(taskId: String) {
        launchMutation("Delete task") {
            repository.deleteStreakTask(taskId)
        }
    }

    fun toggleToday(taskId: String) {
        launchMutation("Toggle task") {
            repository.toggleStreakTask(taskId, today())
        }
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

    companion object {
        private const val KEY_SELECTED = "selected_streak_tracker_id"

        fun factory(
            owner: SavedStateRegistryOwner,
            repository: ChecklistRepository,
            refreshWidgets: suspend () -> Unit,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = StreakViewModel(
                    repository = repository,
                    savedStateHandle = handle,
                    refreshWidgets = refreshWidgets,
                ) as T
            }
    }
}
