package dev.abhiram.checklist.ui

import androidx.lifecycle.SavedStateHandle
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.StreakStore
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StreakViewModelTest {

    private val fixedToday = LocalDate.of(2026, 11, 9)

    private fun tracker(id: String, vararg tasks: StreakTask) = StreakTracker(
        id = id,
        title = id,
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks.toList(),
    )

    private fun task(id: String, color: StreakColor = StreakColor.BLUE) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = 0,
        createdAt = 1L,
    )

    private class FakeRepository(initial: AppState = AppState()) : ChecklistRepository {
        override val state: MutableStateFlow<AppState> = MutableStateFlow(initial)
        var failMutations = false
        var toggledTaskId: String? = null
        var toggledDate: LocalDate? = null
        private var nextTrackerId = "tracker-new"

        private fun requireSuccess() {
            if (failMutations) throw IOException("fail")
        }

        override suspend fun snapshot(): AppState = state.value

        override suspend fun createChecklist(title: String): String = "checklist"
        override suspend fun renameChecklist(id: String, title: String) = Unit
        override suspend fun deleteChecklist(id: String) = Unit
        override suspend fun setPinned(id: String?) = Unit
        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) = Unit
        override suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String) = Unit
        override suspend fun removeWidgetBinding(appWidgetId: Int) = Unit
        override suspend fun addItem(checklistId: String, text: String): String = "item"
        override suspend fun updateItem(itemId: String, text: String) = Unit
        override suspend fun toggleItem(itemId: String) = Unit
        override suspend fun deleteItem(itemId: String) = Unit

        override suspend fun createStreakTracker(title: String): String {
            requireSuccess()
            val id = nextTrackerId
            state.value = StreakStore.createTracker(state.value, id, title, 1L)
            return id
        }

        override suspend fun renameStreakTracker(id: String, title: String) {
            requireSuccess()
            state.value = StreakStore.renameTracker(state.value, id, title, 2L)
        }

        override suspend fun deleteStreakTracker(id: String) {
            requireSuccess()
            state.value = StreakStore.deleteTracker(state.value, id)
        }

        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String {
            requireSuccess()
            val id = "task-new"
            state.value = StreakStore.addTask(state.value, trackerId, id, text, color, 3L)
            return id
        }

        override suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor) {
            requireSuccess()
            state.value = StreakStore.updateTask(state.value, taskId, text, color, 4L)
        }

        override suspend fun deleteStreakTask(taskId: String) {
            requireSuccess()
            state.value = StreakStore.deleteTask(state.value, taskId, 4L)
        }

        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) {
            requireSuccess()
            toggledTaskId = taskId
            toggledDate = date
            state.value = StreakStore.toggleTask(state.value, taskId, date, 5L)
        }

        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) {
            requireSuccess()
            state.value = StreakStore.setStreakWidgetBinding(state.value, appWidgetId, trackerId)
        }

        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) {
            requireSuccess()
            state.value = StreakStore.removeStreakWidgetBinding(state.value, appWidgetId)
        }
    }

    @Test
    fun `create tracker opens the new tracker`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeRepository()
        val viewModel = StreakViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(),
            onError = { _, _ -> },
            refreshWidgets = {},
            today = { fixedToday },
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        try {
            viewModel.createTracker("Reading")
            advanceUntilIdle()

            assertEquals("Reading", viewModel.uiState.value.selectedTracker?.title)
        } finally {
            collection.cancelAndJoin()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `delete selected tracker closes the detail screen`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeRepository(
            AppState(streakTrackers = listOf(tracker("t1"), tracker("t2"))),
        )
        val viewModel = StreakViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(),
            onError = { _, _ -> },
            refreshWidgets = {},
            today = { fixedToday },
        )
        val collection = backgroundScope.launch { viewModel.uiState.collect { } }
        try {
            viewModel.openTracker("t1")
            advanceUntilIdle()
            viewModel.deleteTracker("t1")
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedId)
            assertEquals(listOf("t2"), viewModel.uiState.value.trackers.map { it.id })
        } finally {
            collection.cancelAndJoin()
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `toggle today passes the local date and stores the completion`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1", task("task-1")))),
            )
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = {},
                today = { fixedToday },
            )

            viewModel.toggleToday("task-1")
            advanceUntilIdle()

            assertEquals("task-1", repository.toggledTaskId)
            assertEquals(fixedToday, repository.toggledDate)
            assertEquals(
                setOf("2026-11-09"),
                repository.state.value.streakTrackers.single().tasks.single().completions,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed mutation reports an error and skips the widget refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val errors = mutableListOf<String>()
            var refreshes = 0
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1"))),
            ).apply { failMutations = true }
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { message, _ -> errors += message },
                refreshWidgets = { refreshes++ },
                today = { fixedToday },
            )

            viewModel.renameTracker("t1", "New title")
            advanceUntilIdle()

            assertEquals(listOf("Rename streak tracker failed"), errors)
            assertEquals(0, refreshes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `successful mutation refreshes widgets once`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var refreshes = 0
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1"))),
            )
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = { refreshes++ },
                today = { fixedToday },
            )

            viewModel.renameTracker("t1", "New title")
            advanceUntilIdle()

            assertEquals(1, refreshes)
            assertEquals("New title", repository.state.value.streakTrackers.single().title)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
