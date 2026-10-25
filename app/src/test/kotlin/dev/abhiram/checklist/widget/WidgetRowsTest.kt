package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRowsTest {

    private fun item(id: String, done: Boolean, position: Int) = ChecklistItem(
        id = id,
        text = id,
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    private fun checklistOf(vararg items: ChecklistItem) = Checklist(
        id = "c1",
        title = "List",
        createdAt = 1L,
        updatedAt = 1L,
        items = items.toList(),
    )

    @Test
    fun `unchecked items are listed before completed items`() {
        val checklist = checklistOf(
            item("a", done = true, position = 0),
            item("b", done = false, position = 1),
            item("c", done = true, position = 2),
            item("d", done = false, position = 3),
        )

        val rows = widgetRows(checklist, maxRows = 10)

        assertEquals(listOf("b", "d", "a", "c"), rows.visible.map { it.id })
        assertEquals(0, rows.hiddenCount)
    }

    @Test
    fun `positions order items within each group`() {
        val checklist = checklistOf(
            item("late", done = false, position = 2),
            item("early", done = false, position = 0),
            item("middle", done = false, position = 1),
        )

        val rows = widgetRows(checklist, maxRows = 10)

        assertEquals(listOf("early", "middle", "late"), rows.visible.map { it.id })
    }

    @Test
    fun `extra rows are counted`() {
        val checklist = checklistOf(
            item("a", done = false, position = 0),
            item("b", done = false, position = 1),
            item("c", done = false, position = 2),
            item("d", done = false, position = 3),
            item("e", done = false, position = 4),
        )

        val rows = widgetRows(checklist, maxRows = 3)

        assertEquals(listOf("a", "b", "c"), rows.visible.map { it.id })
        assertEquals(2, rows.hiddenCount)
    }

    @Test
    fun `zero max rows shows no rows`() {
        val rows = widgetRows(checklistOf(item("a", false, 0)), maxRows = 0)

        assertEquals(0, rows.visible.size)
        assertEquals(1, rows.hiddenCount)
    }

    @Test
    fun `empty checklist has no rows`() {
        val rows = widgetRows(checklistOf(), maxRows = 5)

        assertEquals(emptyList<ChecklistItem>(), rows.visible)
        assertEquals(0, rows.hiddenCount)
    }

    @Test
    fun `row capacity grows with the widget height`() {
        assertEquals(0, maxWidgetRows(110f))
        assertEquals(0, maxWidgetRows(120f))
        assertEquals(0, maxWidgetRows(130f))
        assertEquals(2, maxWidgetRows(200f))
        assertEquals(3, maxWidgetRows(250f))
        assertEquals(4, maxWidgetRows(300f))
        assertEquals(6, maxWidgetRows(400f))
        assertEquals(11, maxWidgetRows(600f))
    }
}
