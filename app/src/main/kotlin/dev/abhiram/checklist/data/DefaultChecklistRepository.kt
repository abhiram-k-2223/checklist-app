package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.ChecklistJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class DefaultChecklistRepository(
    private val persistence: StatePersistence,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ChecklistRepository {

    override val state: StateFlow<AppState> = persistence.rawState
        .map { ChecklistJson.decode(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(AppState())
        }
        .stateIn(scope, SharingStarted.Eagerly, AppState())

    override suspend fun snapshot(): AppState = try {
        ChecklistJson.decode(persistence.rawState.first())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        AppState()
    }

    private suspend fun mutate(transform: (AppState) -> AppState) {
        persistence.update { raw ->
            val decoded = raw?.let(ChecklistJson::tryDecode)
            PersistenceUpdate(
                nextRaw = ChecklistJson.encode(transform(decoded ?: AppState())),
                corruptBackup = if (raw != null && decoded == null) raw else null,
            )
        }
    }

    override suspend fun createChecklist(title: String): String {
        val id = newId()
        mutate { ChecklistStore.createChecklist(it, id, title, now()) }
        return id
    }

    override suspend fun renameChecklist(id: String, title: String) {
        mutate { ChecklistStore.renameChecklist(it, id, title, now()) }
    }

    override suspend fun deleteChecklist(id: String) {
        mutate { ChecklistStore.deleteChecklist(it, id) }
    }

    override suspend fun setPinned(id: String?) {
        mutate { ChecklistStore.setPinned(it, id, now()) }
    }

    override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) {
        mutate { bindWidget(it, appWidgetId, checklistId) }
    }

    override suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String) {
        mutate { state ->
            if (state.widgetBindings.containsKey(appWidgetId.toString())) {
                state
            } else {
                bindWidget(state, appWidgetId, checklistId)
            }
        }
    }

    override suspend fun removeWidgetBinding(appWidgetId: Int) {
        mutate { unbindWidget(it, appWidgetId) }
    }

    override suspend fun addItem(checklistId: String, text: String): String {
        val id = newId()
        mutate { ChecklistStore.addItem(it, checklistId, id, text, now()) }
        return id
    }

    override suspend fun updateItem(itemId: String, text: String) {
        mutate { ChecklistStore.updateItem(it, itemId, text, now()) }
    }

    override suspend fun toggleItem(itemId: String) {
        mutate { ChecklistStore.toggleItem(it, itemId, now()) }
    }

    override suspend fun deleteItem(itemId: String) {
        mutate { ChecklistStore.deleteItem(it, itemId, now()) }
    }
}
