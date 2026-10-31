package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WidgetBindingsTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun stateOf(vararg checklists: Checklist) = AppState(checklists = checklists.toList())

    @Test
    fun `bound widget resolves its own checklist`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("todo", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `unbound widget falls back to the pinned checklist`() {
        val state = stateOf(list("groceries", pinned = true), list("todo"))

        assertEquals("groceries", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `bound id wins over a different pinned checklist`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("todo", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `binding a widget does not change the pin`() {
        val state = stateOf(list("groceries", pinned = true), list("todo"))

        val next = bindWidget(state, 7, "todo")

        assertEquals("groceries", next.checklists.first { it.isPinned }.id)
    }

    @Test
    fun `stale binding resolves to null without pin fallback`() {
        val state = stateOf(list("groceries", pinned = true)).copy(
            widgetBindings = mapOf("7" to "deleted"),
        )

        assertNull(checklistForWidget(state, 7))
    }

    @Test
    fun `deleting a bound checklist removes the binding and falls back to the pin`() {
        val bound = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        val deleted = ChecklistStore.deleteChecklist(bound, "todo")

        assertNull(deleted.widgetBindings["7"])
        assertEquals("groceries", checklistForWidget(deleted, 7)?.id)
    }

    @Test
    fun `no binding and no pin returns null`() {
        val state = stateOf(list("groceries"), list("todo"))

        assertNull(checklistForWidget(state, 7))
    }

    @Test
    fun `binding survives a checklist rename`() {
        val bound = bindWidget(stateOf(list("todo")), 7, "todo")

        val renamed = ChecklistStore.renameChecklist(bound, "todo", "Errands", 5L)

        assertEquals("Errands", checklistForWidget(renamed, 7)?.title)
    }

    @Test
    fun `deleting a checklist removes every binding that points at it`() {
        val bound = bindWidget(
            bindWidget(
                bindWidget(stateOf(list("groceries"), list("todo")), 1, "todo"),
                2,
                "todo",
            ),
            3,
            "groceries",
        )

        val deleted = ChecklistStore.deleteChecklist(bound, "todo")

        assertEquals(mapOf("3" to "groceries"), deleted.widgetBindings)
    }

    @Test
    fun `bind ignores an unknown checklist`() {
        val state = stateOf(list("groceries"))

        assertSame(state, bindWidget(state, 7, "missing"))
    }

    @Test
    fun `bind replaces a previous binding for the same widget`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "groceries"),
            7,
            "todo",
        )

        assertEquals(mapOf("7" to "todo"), state.widgetBindings)
    }

    @Test
    fun `unbind removes only the target widget`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "groceries"),
            8,
            "todo",
        )

        val next = unbindWidget(state, 7)

        assertEquals(mapOf("8" to "todo"), next.widgetBindings)
    }

    @Test
    fun `two widgets can show the same checklist independently`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "todo"),
            8,
            "todo",
        )

        assertEquals("todo", checklistForWidget(state, 7)?.id)
        assertEquals("todo", checklistForWidget(state, 8)?.id)

        val changed = bindWidget(state, 8, "groceries")

        assertEquals("todo", checklistForWidget(changed, 7)?.id)
        assertEquals("groceries", checklistForWidget(changed, 8)?.id)
    }

    @Test
    fun `binding one widget leaves another unbound and pinned`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("groceries", checklistForWidget(state, 8)?.id)
    }
}
