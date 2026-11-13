package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakWidgetLayoutTest {

    private fun task(
        id: String,
        color: StreakColor,
        position: Int,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    private fun tracker(tasks: List<StreakTask>) = StreakTracker(
        id = "t1",
        title = "Morning",
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks,
    )

    @Test
    fun `rows are ordered by position with streak and today state`() {
        val today = LocalDate.of(2026, 11, 9)
        val tracker = tracker(
            listOf(
                task("second", StreakColor.GREEN, 1),
                task(
                    "first",
                    StreakColor.BLUE,
                    0,
                    setOf("2026-11-09", "2026-11-08"),
                ),
            ),
        )

        val rows = streakWidgetRows(tracker, today)

        assertEquals(listOf("first", "second"), rows.map { it.task.id })
        assertEquals(2, rows.first().streak)
        assertTrue(rows.first().doneToday)
        assertEquals(0, rows.last().streak)
        assertFalse(rows.last().doneToday)
    }

    @Test
    fun `tracker resolution returns null for unbound and stale widgets`() {
        val state = AppState(
            streakTrackers = listOf(tracker(emptyList())),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "gone"),
        )

        assertEquals("t1", streakWidgetTracker(state, 5)?.id)
        assertNull(streakWidgetTracker(state, 6))
        assertNull(streakWidgetTracker(state, 7))
    }

    @Test
    fun `weekday labels follow the locale first day`() {
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals("Sun", orderedWeekdayLabels(DayOfWeek.SUNDAY, Locale.US).first())
    }

    @Test
    fun `monday first locale starts with monday`() {
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.UK))
        assertEquals("Mon", orderedWeekdayLabels(DayOfWeek.MONDAY, Locale.UK).first())
    }
}
