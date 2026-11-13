package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakWidgetPickerTest {

    private fun task(id: String, color: StreakColor, position: Int) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
    )

    private fun tracker(id: String = "t1", tasks: List<StreakTask> = emptyList()) =
        StreakTracker(
            id = id,
            title = id,
            createdAt = 1L,
            updatedAt = 1L,
            tasks = tasks,
        )

    @Test
    fun `summary reports no tasks or the task count`() {
        assertEquals("No tasks", streakPickerSummary(tracker()))
        assertEquals(
            "2 tasks",
            streakPickerSummary(
                tracker(
                    tasks = listOf(
                        task("a", StreakColor.BLUE, 0),
                        task("b", StreakColor.GREEN, 1),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `colors follow task order and cap at four`() {
        val tracker = tracker(
            tasks = listOf(
                task("e", StreakColor.BLUE, 4),
                task("a", StreakColor.PURPLE, 0),
                task("b", StreakColor.GREEN, 1),
                task("c", StreakColor.ORANGE, 2),
                task("d", StreakColor.BLUE, 3),
            ),
        )

        assertEquals(
            listOf(
                StreakColor.PURPLE,
                StreakColor.GREEN,
                StreakColor.ORANGE,
                StreakColor.BLUE,
            ),
            streakPickerColors(tracker),
        )
    }

    @Test
    fun `null selection writes nothing`() = runTest {
        var writes = 0
        var refreshes = 0

        val bound = bindSelectedStreakTracker(
            trackerId = null,
            trackers = listOf(tracker()),
            setBinding = { writes += 1 },
            refresh = { refreshes += 1 },
        )

        assertFalse(bound)
        assertEquals(0, writes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `missing tracker writes nothing`() = runTest {
        var writes = 0
        var refreshes = 0

        val bound = bindSelectedStreakTracker(
            trackerId = "gone",
            trackers = listOf(tracker()),
            setBinding = { writes += 1 },
            refresh = { refreshes += 1 },
        )

        assertFalse(bound)
        assertEquals(0, writes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `existing tracker binds before refreshing`() = runTest {
        val calls = mutableListOf<String>()

        val bound = bindSelectedStreakTracker(
            trackerId = "t1",
            trackers = listOf(tracker()),
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertTrue(bound)
        assertEquals(listOf("bind:t1", "refresh"), calls)
    }
}
