package dev.abhiram.checklist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreStatePersistence(
    private val dataStore: DataStore<Preferences>,
) : StatePersistence {

    override val rawState: Flow<String?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { it[STATE_JSON] }

    override suspend fun update(transform: (String?) -> PersistenceUpdate) {
        dataStore.edit { preferences ->
            val update = transform(preferences[STATE_JSON])
            update.corruptBackup?.let { preferences[STATE_JSON_CORRUPT] = it }
            preferences[STATE_JSON] = update.nextRaw
        }
    }

    companion object {
        val STATE_JSON = stringPreferencesKey("state_json")
        val STATE_JSON_CORRUPT = stringPreferencesKey("state_json_corrupt")
    }
}
