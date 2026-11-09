package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import dev.abhiram.checklist.data.model.ChecklistJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultChecklistRepositoryTest {

    private class FakePersistence(initial: String? = null) : StatePersistence {
        val raw = MutableStateFlow(initial)
        val backups = mutableListOf<String>()

        override val rawState: Flow<String?> get() = raw

        override suspend fun update(transform: (String?) -> PersistenceUpdate) {
            val update = transform(raw.value)
            update.corruptBackup?.let(backups::add)
            raw.value = update.nextRaw
        }
    }

    private fun initialState() = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "List",
                createdAt = 1L,
                updatedAt = 1L,
                items = listOf(
                    ChecklistItem(
                        id = "i1",
                        text = "Milk",
                        createdAt = 1L,
                        position = 0,
                    ),
                ),
            ),
            Checklist(
                id = "c2",
                title = "Other",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        ),
    )

    @Test
    fun `create checklist persists and returns id`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 100L },
            newId = { "id-1" },
        )

        val id = repository.createChecklist(" Groceries ")

        assertEquals("id-1", id)
        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals("Groceries", state.checklists.single().title)
        assertEquals(100L, state.checklists.single().createdAt)
    }

    @Test
    fun `toggle item works when no state was cached in memory`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.toggleItem("i1")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertTrue(state.checklists.first { it.id == "c1" }.items.single().isDone)
    }

    @Test
    fun `pin replaces the previous pin`() = runTest {
        val initial = initialState().copy(
            checklists = initialState().checklists.map { it.copy(isPinned = it.id == "c1") },
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setPinned("c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertFalse(state.checklists.first { it.id == "c1" }.isPinned)
        assertTrue(state.checklists.first { it.id == "c2" }.isPinned)
    }

    @Test
    fun `widget binding is persisted`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setWidgetBinding(42, "c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("42" to "c2"), state.widgetBindings)
    }

    @Test
    fun `binding if unbound preserves an existing binding`() = runTest {
        val initial = initialState().copy(widgetBindings = mapOf("42" to "c2"))
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setWidgetBindingIfUnbound(42, "c1")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("42" to "c2"), state.widgetBindings)
    }

    @Test
    fun `binding if unbound writes when the widget has no binding`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setWidgetBindingIfUnbound(42, "c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("42" to "c2"), state.widgetBindings)
    }

    @Test
    fun `widget binding removal is persisted`() = runTest {
        val initial = initialState().copy(
            widgetBindings = mapOf("42" to "c2", "43" to "c1"),
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.removeWidgetBinding(42)

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("43" to "c1"), state.widgetBindings)
    }

    @Test
    fun `deleting a checklist removes its persisted widget bindings`() = runTest {
        val initial = initialState().copy(
            widgetBindings = mapOf("42" to "c2", "43" to "c1"),
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.deleteChecklist("c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("43" to "c1"), state.widgetBindings)
    }

    @Test
    fun `corrupt raw state is backed up before replacement`() = runTest {
        val persistence = FakePersistence("{broken")
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 1L },
            newId = { "id-1" },
        )

        repository.createChecklist("Recovered")

        assertEquals(listOf("{broken"), persistence.backups)
        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals("Recovered", state.checklists.single().title)
    }

    @Test
    fun `snapshot reads the current persisted state`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        val snapshot = repository.snapshot()

        assertEquals(listOf("c1", "c2"), snapshot.checklists.map { it.id })
    }

    @Test
    fun `snapshot propagates cancellation instead of returning empty state`() = runTest {
        val persistence = object : StatePersistence {
            override val rawState: Flow<String?> = flow {
                throw CancellationException("cancelled")
            }

            override suspend fun update(transform: (String?) -> PersistenceUpdate): Unit =
                error("not used")
        }
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        var cancelled = false
        try {
            repository.snapshot()
        } catch (expected: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }
}
