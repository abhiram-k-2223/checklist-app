package dev.abhiram.checklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateJsonTest {

    private val sample = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "Groceries",
                isPinned = true,
                createdAt = 1L,
                updatedAt = 2L,
                items = listOf(
                    ChecklistItem(
                        id = "i1",
                        text = "Milk",
                        isDone = true,
                        createdAt = 3L,
                        position = 0,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `round trip preserves state`() {
        assertEquals(sample, ChecklistJson.decode(ChecklistJson.encode(sample)))
    }

    @Test
    fun `unknown keys are ignored`() {
        val decoded = ChecklistJson.tryDecode(
            """{"version":1,"checklists":[],"future":true}""",
        )
        assertNotNull(decoded)
        assertTrue(decoded!!.checklists.isEmpty())
    }

    @Test
    fun `corrupt json falls back to empty state`() {
        assertEquals(AppState(), ChecklistJson.decode("{not json"))
    }

    @Test
    fun `null raw decodes to empty state`() {
        assertEquals(AppState(), ChecklistJson.decode(null))
    }
}
