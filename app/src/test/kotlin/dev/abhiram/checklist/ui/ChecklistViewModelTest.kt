package dev.abhiram.checklist.ui

import androidx.lifecycle.SavedStateHandle
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.AppState
import java.io.IOException
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

        override suspend fun addItem(checklistId: String, text: String): String = throw IOException("fail")

        override suspend fun updateItem(itemId: String, text: String) = throw IOException("fail")

        override suspend fun toggleItem(itemId: String) = throw IOException("fail")

        override suspend fun deleteItem(itemId: String) = throw IOException("fail")
    }

    @Test
    fun `mutation failure is reported and does not escape`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val errors = mutableListOf<String>()
            val viewModel = ChecklistViewModel(
                repository = ThrowingRepository(),
                savedStateHandle = SavedStateHandle(),
                onError = { message, _ -> errors += message },
            )

            viewModel.renameChecklist("c1", "Name")
            advanceUntilIdle()

            assertEquals(listOf("Rename checklist failed"), errors)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
