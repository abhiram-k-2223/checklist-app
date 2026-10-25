package dev.abhiram.checklist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class DataStoreStatePersistenceTest {

    private class ThrowingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk read failed") }

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = error("not used")
    }

    @Test
    fun `io failure becomes empty state`() = runTest {
        val persistence = DataStoreStatePersistence(ThrowingDataStore())

        assertNull(persistence.rawState.first())
    }
}
