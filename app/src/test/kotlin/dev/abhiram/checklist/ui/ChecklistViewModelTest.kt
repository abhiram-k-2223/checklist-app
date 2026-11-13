package dev.abhiram.checklist.ui

import androidx.lifecycle.SavedStateHandle
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModelTest {

    private class ThrowingRepository : ChecklistRepository {
        override val state: StateFlow<AppState> = MutableStateFlow(AppState())

        override suspend fun snapshot(): AppState = AppState()

        override suspend fun createChecklist(title: String): String = throw IOException("fail")

        override suspend fun renameChecklist(id: String, title: String) = throw IOException("fail")

        override suspend fun deleteChecklist(id: String) = throw IOException("fail")

        override suspend fun setPinned(id: String?) = throw IOException("fail")

        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) =
            throw IOException("fail")

        override suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String) =
            throw IOException("fail")

        override suspend fun removeWidgetBinding(appWidgetId: Int) =
            throw IOException("fail")

        override suspend fun addItem(checklistId: String, text: String): String = throw IOException("fail")

        override suspend fun updateItem(itemId: String, text: String) = throw IOException("fail")

        override suspend fun toggleItem(itemId: String) = throw IOException("fail")

        override suspend fun deleteItem(itemId: String) = throw IOException("fail")

        override suspend fun createStreakTracker(title: String): String = throw IOException("fail")

        override suspend fun renameStreakTracker(id: String, title: String) =
            throw IOException("fail")

        override suspend fun deleteStreakTracker(id: String) = throw IOException("fail")

        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String = throw IOException("fail")

        override suspend fun updateStreakTask(
            taskId: String,
            text: String,
            color: StreakColor,
        ) = throw IOException("fail")

        override suspend fun deleteStreakTask(taskId: String) = throw IOException("fail")

        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) =
            throw IOException("fail")

        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) =
            throw IOException("fail")

        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) =
            throw IOException("fail")
    }

    private class NoOpRepository : ChecklistRepository {
        override val state: StateFlow<AppState> = MutableStateFlow(AppState())

        override suspend fun snapshot(): AppState = AppState()

        override suspend fun createChecklist(title: String): String = "id"

        override suspend fun renameChecklist(id: String, title: String) = Unit

        override suspend fun deleteChecklist(id: String) = Unit

        override suspend fun setPinned(id: String?) = Unit

        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) = Unit

        override suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String) = Unit

        override suspend fun removeWidgetBinding(appWidgetId: Int) = Unit

        override suspend fun addItem(checklistId: String, text: String): String = "id"

        override suspend fun updateItem(itemId: String, text: String) = Unit

        override suspend fun toggleItem(itemId: String) = Unit

        override suspend fun deleteItem(itemId: String) = Unit

        override suspend fun createStreakTracker(title: String): String = "id"

        override suspend fun renameStreakTracker(id: String, title: String) = Unit

        override suspend fun deleteStreakTracker(id: String) = Unit

        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String = "id"

        override suspend fun updateStreakTask(
            taskId: String,
            text: String,
            color: StreakColor,
        ) = Unit

        override suspend fun deleteStreakTask(taskId: String) = Unit

        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) = Unit

        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) = Unit

        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) = Unit
    }

    @Test
    fun `mutation failure is reported and does not escape`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val errors = mutableListOf<String>()
            var refreshes = 0
            val viewModel = ChecklistViewModel(
                repository = ThrowingRepository(),
                savedStateHandle = SavedStateHandle(),
                onError = { message, _ -> errors += message },
                refreshWidgets = { refreshes++ },
            )

            viewModel.renameChecklist("c1", "Name")
            advanceUntilIdle()

            assertEquals(listOf("Rename checklist failed"), errors)
            assertEquals(0, refreshes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `successful mutation refreshes the widget`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var refreshes = 0
            val viewModel = ChecklistViewModel(
                repository = NoOpRepository(),
                savedStateHandle = SavedStateHandle(),
                refreshWidgets = { refreshes++ },
            )

            viewModel.renameChecklist("c1", "Name")
            advanceUntilIdle()

            assertEquals(1, refreshes)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
