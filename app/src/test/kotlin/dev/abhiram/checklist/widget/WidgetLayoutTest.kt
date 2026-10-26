package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {

    private fun item(id: String, done: Boolean, position: Int) = ChecklistItem(
        id = id,
        text = id,
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    @Test
    fun `unchecked items are listed before completed items`() {
        val items = listOf(
            item("a", done = true, position = 0),
            item("b", done = false, position = 1),
            item("c", done = true, position = 2),
            item("d", done = false, position = 3),
        )

        val sorted = sortedWidgetItems(items)

        assertEquals(listOf("b", "d", "a", "c"), sorted.map { it.id })
    }

    @Test
    fun `positions order items within each group`() {
        val items = listOf(
            item("late", done = false, position = 2),
            item("early", done = false, position = 0),
            item("middle", done = false, position = 1),
        )

        val sorted = sortedWidgetItems(items)

        assertEquals(listOf("early", "middle", "late"), sorted.map { it.id })
    }

    @Test
    fun `every item is returned with no truncation`() {
        val items = (0 until 12).map { position ->
            item("item$position", done = position % 3 == 0, position = position)
        }

        val sorted = sortedWidgetItems(items)

        assertEquals(12, sorted.size)
        assertEquals(items.map { it.id }.toSet(), sorted.map { it.id }.toSet())
    }

    @Test
    fun `empty item list returns empty`() {
        assertTrue(sortedWidgetItems(emptyList()).isEmpty())
    }

    @Test
    fun `short widget uses compact layout`() {
        assertTrue(isCompactWidgetHeight(110f))
        assertTrue(isCompactWidgetHeight(159.9f))
        assertFalse(isCompactWidgetHeight(160f))
        assertFalse(isCompactWidgetHeight(600f))
    }
}
