package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakStoreTest {

    private fun tracker(
        id: String = "t1",
        tasks: List<StreakTask> = emptyList(),
    ) = StreakTracker(
        id = id,
        title = id,
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks,
    )

    private fun task(
        id: String,
        color: StreakColor,
        position: Int = 0,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    @Test
    fun `create tracker trims the title`() {
        val next = StreakStore.createTracker(AppState(), "t1", "  Morning  ", 5L)
        val created = next.streakTrackers.single()

        assertEquals("Morning", created.title)
        assertEquals(5L, created.createdAt)
        assertEquals(5L, created.updatedAt)
        assertTrue(created.tasks.isEmpty())
    }

    @Test
    fun `create tracker rejects a blank title`() {
        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.createTracker(AppState(), "t1", "   ", 5L)
        }
    }

    @Test
    fun `rename tracker trims the title and updates the timestamp`() {
        val state = AppState(streakTrackers = listOf(tracker()))
        val next = StreakStore.renameTracker(state, "t1", "  Evening  ", 9L)

        assertEquals("Evening", next.streakTrackers.single().title)
        assertEquals(9L, next.streakTrackers.single().updatedAt)
    }

    @Test
    fun `rename tracker rejects a blank title`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.renameTracker(state, "t1", "", 9L)
        }
    }

    @Test
    fun `rename unknown tracker is a no-op`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertEquals(state, StreakStore.renameTracker(state, "missing", "New", 9L))
    }

    @Test
    fun `delete tracker removes its tasks and widget bindings`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1", listOf(task("a", StreakColor.BLUE)))),
            streakWidgetBindings = mapOf("1" to "t1", "2" to "other"),
        )

        val next = StreakStore.deleteTracker(state, "t1")

        assertTrue(next.streakTrackers.isEmpty())
        assertEquals(mapOf("2" to "other"), next.streakWidgetBindings)
    }

    @Test
    fun `add task trims text and appends the next position`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE, 0)))),
        )

        val next = StreakStore.addTask(state, "t1", "b", "  Walk  ", StreakColor.GREEN, 7L)
        val added = next.streakTrackers.single().tasks.last()

        assertEquals("Walk", added.text)
        assertEquals(StreakColor.GREEN, added.color)
        assertEquals(1, added.position)
        assertEquals(7L, added.createdAt)
        assertTrue(added.completions.isEmpty())
    }

    @Test
    fun `add task rejects a blank text`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(state, "t1", "a", "   ", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task rejects a fifth task`() {
        val full = AppState(
            streakTrackers = listOf(
                tracker(
                    tasks = StreakColor.entries.mapIndexed { index, color ->
                        task("task$index", color, index)
                    },
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(full, "t1", "task4", "Fifth", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task rejects a color already used in the tracker`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(state, "t1", "b", "Walk", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task to an unknown tracker is a no-op`() {
        val state = AppState()

        assertEquals(state, StreakStore.addTask(state, "missing", "a", "Walk", StreakColor.BLUE, 7L))
    }

    @Test
    fun `update task allows its own color and rejects another task's color`() {
        val state = AppState(
            streakTrackers = listOf(
                tracker(tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN, 1))),
            ),
        )

        val next = StreakStore.updateTask(state, "a", "  New text  ", StreakColor.BLUE, 9L)
        assertEquals("New text", next.streakTrackers.single().tasks.first().text)

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.updateTask(state, "a", "New text", StreakColor.GREEN, 9L)
        }
    }

    @Test
    fun `delete task removes the task and its history`() {
        val state = AppState(
            streakTrackers = listOf(
                tracker(tasks = listOf(task("a", StreakColor.BLUE, completions = setOf("2026-11-09")))),
            ),
        )

        val next = StreakStore.deleteTask(state, "a", 9L)

        assertTrue(next.streakTrackers.single().tasks.isEmpty())
        assertEquals(9L, next.streakTrackers.single().updatedAt)
    }

    @Test
    fun `toggle task adds and removes the ISO date`() {
        val date = LocalDate.of(2026, 11, 9)
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        val added = StreakStore.toggleTask(state, "a", date, 2L)
        assertEquals(
            setOf("2026-11-09"),
            added.streakTrackers.single().tasks.single().completions,
        )

        val removed = StreakStore.toggleTask(added, "a", date, 3L)
        assertTrue(removed.streakTrackers.single().tasks.single().completions.isEmpty())
    }

    @Test
    fun `toggle unknown task is a no-op`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        assertEquals(state, StreakStore.toggleTask(state, "missing", LocalDate.of(2026, 11, 9), 2L))
    }

    @Test
    fun `bind only an existing tracker and replace the same widget binding`() {
        val state = AppState(streakTrackers = listOf(tracker("t1"), tracker("t2")))

        val bound = StreakStore.setStreakWidgetBinding(state, 5, "t2")
        assertEquals("t2", bound.streakWidgetBindings["5"])

        val replaced = StreakStore.setStreakWidgetBinding(bound, 5, "t1")
        assertEquals("t1", replaced.streakWidgetBindings["5"])

        val unknown = StreakStore.setStreakWidgetBinding(state, 6, "missing")
        assertTrue(unknown.streakWidgetBindings.isEmpty())
    }

    @Test
    fun `remove one widget binding leaves the others`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1"), tracker("t2")),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "t2"),
        )

        val next = StreakStore.removeStreakWidgetBinding(state, 5)

        assertEquals(mapOf("6" to "t2"), next.streakWidgetBindings)
    }

    @Test
    fun `resolve a tracker for a widget and return null for unbound or stale`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1")),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "gone"),
        )

        assertEquals("t1", StreakStore.streakTrackerForWidget(state, 5)?.id)
        assertNull(StreakStore.streakTrackerForWidget(state, 6))
        assertNull(StreakStore.streakTrackerForWidget(state, 7))
    }
}
