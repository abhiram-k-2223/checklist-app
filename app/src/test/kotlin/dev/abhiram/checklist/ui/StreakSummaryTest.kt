package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakSummaryTest {

    private val today = LocalDate.of(2026, 11, 9)

    private fun task(
        id: String,
        position: Int = 0,
        color: StreakColor = StreakColor.BLUE,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    private fun tracker(vararg tasks: StreakTask) = StreakTracker(
        id = "t1",
        title = "Reading",
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks.toList(),
    )

    @Test
    fun `summary reports no tasks`() {
        assertEquals("No tasks", streakTrackerSummary(tracker(), today))
    }

    @Test
    fun `summary uses singular for one task`() {
        val tasks = arrayOf(task("a", completions = setOf("2026-11-09")))

        assertEquals("1 task · 1 done today", streakTrackerSummary(tracker(*tasks), today))
    }

    @Test
    fun `summary counts completed tasks`() {
        val tasks = arrayOf(
            task("a", 0, StreakColor.BLUE, setOf("2026-11-09")),
            task("b", 1, StreakColor.GREEN, setOf("2026-11-09")),
            task("c", 2, StreakColor.ORANGE),
        )

        assertEquals("3 tasks · 2 done today", streakTrackerSummary(tracker(*tasks), today))
    }

    @Test
    fun `colors follow task position and stop at four`() {
        val tasks = arrayOf(
            task("d", 3, StreakColor.PURPLE),
            task("b", 1, StreakColor.GREEN),
            task("a", 0, StreakColor.BLUE),
            task("c", 2, StreakColor.ORANGE),
        )

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.GREEN, StreakColor.ORANGE, StreakColor.PURPLE),
            streakTrackerColors(tracker(*tasks)),
        )
    }
}
