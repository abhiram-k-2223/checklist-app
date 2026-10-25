package dev.abhiram.checklist

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.DataStoreStatePersistence
import dev.abhiram.checklist.data.DefaultChecklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val Context.checklistDataStore by preferencesDataStore(name = "checklist_state")

class ChecklistApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: ChecklistRepository by lazy {
        DefaultChecklistRepository(
            persistence = DataStoreStatePersistence(checklistDataStore),
            scope = appScope,
        )
    }
}
