package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.ChecklistJson
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakRepositoryTest {

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

    private fun state(persistence: FakePersistence) =
        ChecklistJson.decode(persistence.raw.value)

    @Test
    fun `create tracker persists a trimmed title and returns the generated id`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "t1" },
        )

        val id = repository.createStreakTracker("  Morning  ")

        assertEquals("t1", id)
        assertEquals("Morning", state(persistence).streakTrackers.single().title)
    }

    @Test
    fun `add task persists color position and empty completions`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")

        val taskId = repository.addStreakTask(trackerId, "  Stretch  ", StreakColor.BLUE)
        val task = state(persistence).streakTrackers.single().tasks.single()

        assertEquals("generated", taskId)
        assertEquals("Stretch", task.text)
        assertEquals(StreakColor.BLUE, task.color)
        assertEquals(0, task.position)
        assertTrue(task.completions.isEmpty())
    }

    @Test
    fun `toggle task adds then removes the local date`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        val taskId = repository.addStreakTask(trackerId, "Stretch", StreakColor.BLUE)
        val date = LocalDate.of(2026, 11, 9)

        repository.toggleStreakTask(taskId, date)
        assertEquals(
            setOf("2026-11-09"),
            state(persistence).streakTrackers.single().tasks.single().completions,
        )

        repository.toggleStreakTask(taskId, date)
        assertTrue(state(persistence).streakTrackers.single().tasks.single().completions.isEmpty())
    }

    @Test
    fun `delete task removes its completion history`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        val taskId = repository.addStreakTask(trackerId, "Stretch", StreakColor.BLUE)
        repository.toggleStreakTask(taskId, LocalDate.of(2026, 11, 9))

        repository.deleteStreakTask(taskId)

        assertTrue(state(persistence).streakTrackers.single().tasks.isEmpty())
    }

    @Test
    fun `delete tracker removes its widget bindings`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        repository.setStreakWidgetBinding(7, trackerId)

        repository.deleteStreakTracker(trackerId)

        assertTrue(state(persistence).streakTrackers.isEmpty())
        assertTrue(state(persistence).streakWidgetBindings.isEmpty())
    }

    @Test
    fun `binding only an existing tracker is persisted`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )

        repository.setStreakWidgetBinding(7, "missing")

        assertTrue(state(persistence).streakWidgetBindings.isEmpty())
    }

    @Test
    fun `removing one binding leaves the other widget bound`() = runTest {
        val persistence = FakePersistence()
        var nextId = 0
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "t${nextId++}" },
        )
        val first = repository.createStreakTracker("First")
        val second = repository.createStreakTracker("Second")
        repository.setStreakWidgetBinding(7, first)
        repository.setStreakWidgetBinding(8, second)

        repository.removeStreakWidgetBinding(7)

        assertEquals(mapOf("8" to second), state(persistence).streakWidgetBindings)
    }
}
