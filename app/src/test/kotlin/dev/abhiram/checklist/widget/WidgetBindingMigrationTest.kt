package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBindingMigrationTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `unbound widget is bound to the pinned checklist`() = runTest {
        val state = AppState(checklists = listOf(list("groceries", pinned = true), list("todo")))
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertEquals(listOf(7 to "groceries"), writes)
        assertEquals("groceries", migrated.widgetBindings["7"])
    }

    @Test
    fun `existing binding is never overwritten by the pin`() = runTest {
        val state = AppState(
            checklists = listOf(list("groceries", pinned = true), list("todo")),
            widgetBindings = mapOf("7" to "todo"),
        )
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertTrue(writes.isEmpty())
        assertEquals(mapOf("7" to "todo"), migrated.widgetBindings)
    }

    @Test
    fun `unbound widget without a pin is left unbound`() = runTest {
        val state = AppState(checklists = listOf(list("groceries"), list("todo")))
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertTrue(writes.isEmpty())
        assertNull(migrated.widgetBindings["7"])
    }

    @Test
    fun `migration targets only the widget being rendered`() = runTest {
        val state = AppState(checklists = listOf(list("groceries", pinned = true)))
        val writes = mutableListOf<Int>()

        migrateWidgetBinding(state, 7) { widgetId, _ -> writes += widgetId }

        assertEquals(listOf(7), writes)
    }
}
