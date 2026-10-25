package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistStoreItemTest {

    private fun item(id: String, done: Boolean = false, position: Int = 0) = ChecklistItem(
        id = id,
        text = id,
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    private fun state(vararg items: ChecklistItem): AppState = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "List",
                createdAt = 1L,
                updatedAt = 1L,
                items = items.toList(),
            ),
        ),
    )

    @Test
    fun `add appends items with increasing positions`() {
        val first = ChecklistStore.addItem(state(), "c1", "i1", "  Milk  ", 5L)
        val second = ChecklistStore.addItem(first, "c1", "i2", "Bread", 6L)
        val items = second.checklists.single().items
        assertEquals(listOf("i1", "i2"), items.map { it.id })
        assertEquals("Milk", items[0].text)
        assertEquals(listOf(0, 1), items.map { it.position })
        assertEquals(6L, second.checklists.single().updatedAt)
    }

    @Test
    fun `blank item text is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChecklistStore.addItem(state(), "c1", "i1", "  ", 5L)
        }
    }

    @Test
    fun `add to a missing checklist is a no-op`() {
        val original = state()
        assertEquals(original, ChecklistStore.addItem(original, "missing", "i1", "Milk", 5L))
    }

    @Test
    fun `update trims text and updates timestamp`() {
        val next = ChecklistStore.updateItem(state(item("i1")), "i1", "  Oat milk  ", 8L)
        assertEquals("Oat milk", next.checklists.single().items.single().text)
        assertEquals(8L, next.checklists.single().updatedAt)
    }

    @Test
    fun `update missing item is a no-op`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.updateItem(original, "missing", "Text", 8L))
    }

    @Test
    fun `toggle flips only the target item and updates its list`() {
        val next = ChecklistStore.toggleItem(state(item("i1"), item("i2")), "i2", 10L)
        assertFalse(next.checklists.single().items.first { it.id == "i1" }.isDone)
        assertTrue(next.checklists.single().items.first { it.id == "i2" }.isDone)
        assertEquals(10L, next.checklists.single().updatedAt)
    }

    @Test
    fun `toggle is a no-op for a missing item`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.toggleItem(original, "missing", 10L))
    }

    @Test
    fun `toggling twice restores the original state`() {
        val original = state(item("i1"))
        val twice = ChecklistStore.toggleItem(ChecklistStore.toggleItem(original, "i1", 2L), "i1", 3L)
        assertFalse(twice.checklists.single().items.single().isDone)
    }

    @Test
    fun `delete removes only the target item`() {
        val next = ChecklistStore.deleteItem(state(item("i1"), item("i2", position = 1)), "i1", 4L)
        assertEquals(listOf("i2"), next.checklists.single().items.map { it.id })
        assertEquals(4L, next.checklists.single().updatedAt)
    }

    @Test
    fun `delete missing item is a no-op`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.deleteItem(original, "missing", 4L))
    }

    @Test
    fun `deleting a checklist cascades to its items`() {
        val original = state(item("i1"), item("i2", position = 1))
        val next = ChecklistStore.deleteChecklist(original, "c1")
        assertTrue(next.checklists.isEmpty())
    }

    @Test
    fun `item ids are found across different lists`() {
        val original = AppState(
            checklists = listOf(
                Checklist(
                    id = "c1",
                    title = "One",
                    createdAt = 1L,
                    updatedAt = 1L,
                    items = listOf(item("i1")),
                ),
                Checklist(
                    id = "c2",
                    title = "Two",
                    createdAt = 1L,
                    updatedAt = 1L,
                    items = listOf(item("i2")),
                ),
            ),
        )
        val next = ChecklistStore.toggleItem(original, "i2", 2L)
        assertFalse(next.checklists.first { it.id == "c1" }.items.single().isDone)
        assertTrue(next.checklists.first { it.id == "c2" }.items.single().isDone)
    }
}
