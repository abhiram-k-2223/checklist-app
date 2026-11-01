package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPickerTest {
    @Test
    fun `pinned checklist is first and every checklist is kept`() {
        val checklists = listOf(
            checklist("todo"),
            checklist("groceries", pinned = true),
            checklist("work"),
        )

        assertEquals(
            listOf("groceries", "todo", "work"),
            pickerChecklists(checklists).map { it.id },
        )
    }

    @Test
    fun `progress counts done items over all items`() {
        assertEquals("2/3", pickerProgress(checklist("todo", done = 2, total = 3)))
    }

    @Test
    fun `empty checklist shows zero progress`() {
        assertEquals("0/0", pickerProgress(checklist("empty")))
    }
}

private fun checklist(
    id: String,
    pinned: Boolean = false,
    done: Int = 0,
    total: Int = 0,
): Checklist = Checklist(
    id = id,
    title = id,
    isPinned = pinned,
    createdAt = 1L,
    updatedAt = 1L,
    items = (0 until total).map { index ->
        ChecklistItem(
            id = "$id-$index",
            text = "Item $index",
            isDone = index < done,
            createdAt = 1L,
            position = index,
        )
    },
)
