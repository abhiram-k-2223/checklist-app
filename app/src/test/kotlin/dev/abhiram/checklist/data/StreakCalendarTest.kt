package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalendarTest {

    private fun task(
        id: String,
        color: StreakColor = StreakColor.BLUE,
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
    fun `today done counts today and earlier consecutive days`() {
        val task = task(
            "a",
            completions = setOf("2026-11-09", "2026-11-08", "2026-11-07"),
        )

        assertEquals(3, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `today not done keeps yesterday's streak alive`() {
        val task = task("a", completions = setOf("2026-11-08", "2026-11-07"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `missed yesterday resets the streak to zero`() {
        val task = task("a", completions = setOf("2026-11-07"))

        assertEquals(0, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `today after a missed yesterday is one`() {
        val task = task("a", completions = setOf("2026-11-09"))

        assertEquals(1, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `unchecking today drops the visible streak`() {
        val before = task(
            "a",
            completions = setOf("2026-11-09", "2026-11-08", "2026-11-07"),
        )
        val after = before.copy(completions = before.completions - "2026-11-09")

        assertEquals(3, currentStreak(before, LocalDate.of(2026, 11, 9)))
        assertEquals(2, currentStreak(after, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, currentStreak(task("a"), LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `streak crosses a month boundary`() {
        val task = task("a", completions = setOf("2026-10-31", "2026-11-01"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `streak crosses a year boundary`() {
        val task = task("a", completions = setOf("2025-12-31", "2026-01-01"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `calendar always returns forty two cells`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(42, days.size)
        assertEquals(LocalDate.of(2026, 11, 1), days.first().date)
        assertEquals(LocalDate.of(2026, 12, 12), days.last().date)
    }

    @Test
    fun `monday first week starts on the preceding monday`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(LocalDate.of(2026, 10, 26), days.first().date)
        assertEquals(LocalDate.of(2026, 12, 6), days.last().date)
    }

    @Test
    fun `current month and today flags are correct`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertFalse(days.first { it.date == LocalDate.of(2026, 10, 31) }.inCurrentMonth)
        assertTrue(days.first { it.date == LocalDate.of(2026, 11, 1) }.inCurrentMonth)
        assertTrue(days.first { it.date == LocalDate.of(2026, 11, 9) }.isToday)
        assertFalse(days.first { it.date == LocalDate.of(2026, 11, 10) }.isToday)
    }

    @Test
    fun `dots follow task position and include every completed task`() {
        val tasks = listOf(
            task("a", StreakColor.BLUE, 0, setOf("2026-11-09")),
            task("b", StreakColor.GREEN, 1, setOf("2026-11-09")),
        )
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.GREEN),
            days.first { it.date == LocalDate.of(2026, 11, 9) }.dots,
        )
    }

    @Test
    fun `a day can have zero to four dots`() {
        val tasks = StreakColor.entries.mapIndexed { index, color ->
            task("t$index", color, index, setOf("2026-11-09"))
        }
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(4, days.first { it.date == LocalDate.of(2026, 11, 9) }.dots.size)
        assertEquals(0, days.first { it.date == LocalDate.of(2026, 11, 10) }.dots.size)
    }

    @Test
    fun `dots show for adjacent month dates`() {
        val tasks = listOf(task("a", completions = setOf("2026-10-31")))
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )
        val day = days.first { it.date == LocalDate.of(2026, 10, 31) }

        assertFalse(day.inCurrentMonth)
        assertEquals(listOf(StreakColor.BLUE), day.dots)
    }
}
