package dev.abhiram.checklist.data

import kotlinx.coroutines.flow.Flow

data class PersistenceUpdate(
    val nextRaw: String,
    val corruptBackup: String? = null,
)

interface StatePersistence {
    val rawState: Flow<String?>
    suspend fun update(transform: (String?) -> PersistenceUpdate)
}
