package dev.abhiram.checklist.ui

import androidx.compose.ui.graphics.Color
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakFormatTest {

    private fun task(id: String, color: StreakColor) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = 0,
        createdAt = 1L,
    )

    @Test
    fun `streak label handles zero one and many`() {
        assertEquals("No streak yet", streakLabel(0))
        assertEquals("🔥 1 day", streakLabel(1))
        assertEquals("🔥 6 days", streakLabel(6))
    }

    @Test
    fun `compact label handles zero and many`() {
        assertEquals("—", compactStreakLabel(0))
        assertEquals("🔥 6", compactStreakLabel(6))
    }

    @Test
    fun `available colors exclude colors used by other tasks`() {
        val tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN))

        assertEquals(
            listOf(StreakColor.ORANGE, StreakColor.PURPLE),
            availableStreakColors(tasks),
        )
    }

    @Test
    fun `editing task keeps its own color available`() {
        val tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN))

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.ORANGE, StreakColor.PURPLE),
            availableStreakColors(tasks, editingTaskId = "a"),
        )
    }

    @Test
    fun `streak colors map to compose colors`() {
        assertEquals(Color(0xFF0A84FF), streakColor(StreakColor.BLUE))
        assertEquals(Color(0xFF30D158), streakColor(StreakColor.GREEN))
        assertEquals(Color(0xFFFF9F0A), streakColor(StreakColor.ORANGE))
        assertEquals(Color(0xFFBF5AF2), streakColor(StreakColor.PURPLE))
    }
}
