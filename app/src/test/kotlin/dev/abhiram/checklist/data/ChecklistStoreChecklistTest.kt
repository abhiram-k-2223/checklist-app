package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistStoreChecklistTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `create appends a trimmed checklist`() {
        val next = ChecklistStore.createChecklist(AppState(), "c1", "  Groceries  ", 5L)
        val created = next.checklists.single()
        assertEquals("Groceries", created.title)
        assertEquals("c1", created.id)
        assertEquals(5L, created.createdAt)
        assertEquals(5L, created.updatedAt)
        assertFalse(created.isPinned)
        assertTrue(created.items.isEmpty())
    }

    @Test
    fun `blank title is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChecklistStore.createChecklist(AppState(), "c1", "   ", 5L)
        }
    }

    @Test
    fun `rename trims and updates timestamp`() {
        val state = AppState(checklists = listOf(list("c1")))
        val next = ChecklistStore.renameChecklist(state, "c1", "  Renamed  ", 9L)
        assertEquals("Renamed", next.checklists.single().title)
        assertEquals(9L, next.checklists.single().updatedAt)
    }

    @Test
    fun `rename unknown list is a no-op`() {
        val state = AppState(checklists = listOf(list("c1")))
        assertEquals(state, ChecklistStore.renameChecklist(state, "missing", "Name", 9L))
    }

    @Test
    fun `delete removes only the target list`() {
        val state = AppState(checklists = listOf(list("c1"), list("c2")))
        val next = ChecklistStore.deleteChecklist(state, "c1")
        assertEquals(listOf("c2"), next.checklists.map { it.id })
    }

    @Test
    fun `pinning a second list unpins the first`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true), list("c2")))
        val next = ChecklistStore.setPinned(state, "c2", 7L)
        assertFalse(next.checklists.first { it.id == "c1" }.isPinned)
        assertTrue(next.checklists.first { it.id == "c2" }.isPinned)
        assertEquals(7L, next.checklists.first { it.id == "c2" }.updatedAt)
    }

    @Test
    fun `pinning an unknown id unpins everything`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true)))
        val next = ChecklistStore.setPinned(state, "missing", 7L)
        assertFalse(next.checklists.single().isPinned)
    }

    @Test
    fun `unpin null clears the pin`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true)))
        val next = ChecklistStore.setPinned(state, null, 7L)
        assertFalse(next.checklists.single().isPinned)
    }
}
