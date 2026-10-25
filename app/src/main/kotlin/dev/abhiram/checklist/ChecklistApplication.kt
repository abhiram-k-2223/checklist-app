@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package dev.abhiram.checklist

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import androidx.work.Configuration
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

class ChecklistApplication : Application(), Configuration.Provider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Glance needs WorkManager for async widget requests. The manifest removes
    // WorkManager's startup initializer so it is created only when a widget
    // request actually needs it, keeping cold start light.
    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.ERROR)
            .build()

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
