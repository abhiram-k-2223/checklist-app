@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package dev.abhiram.checklist

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.DataStoreStatePersistence
import dev.abhiram.checklist.data.DefaultChecklistRepository
import dev.abhiram.checklist.widget.ChecklistWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

val Context.checklistDataStore by preferencesDataStore(name = "checklist_state")

class ChecklistApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: ChecklistRepository by lazy {
        DefaultChecklistRepository(
            persistence = DataStoreStatePersistence(checklistDataStore),
            scope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.state
                .debounce(150)
                .collect {
                    ChecklistWidget().updateAll(this@ChecklistApplication)
                }
        }
    }
}
