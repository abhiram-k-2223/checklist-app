package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

object ChecklistStore {

    fun createChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        val created = Checklist(
            id = id,
            title = clean,
            createdAt = now,
            updatedAt = now,
        )
        return state.copy(checklists = state.checklists + created)
    }

    fun renameChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.id == id) {
                    checklist.copy(title = clean, updatedAt = now)
                } else {
                    checklist
                }
            },
        )
    }

    fun deleteChecklist(state: AppState, id: String): AppState =
        state.copy(checklists = state.checklists.filterNot { it.id == id })

    fun setPinned(state: AppState, id: String?, now: Long): AppState {
        val targetExists = id != null && state.checklists.any { it.id == id }
        return state.copy(
            checklists = state.checklists.map { checklist ->
                val shouldPin = targetExists && checklist.id == id
                if (checklist.isPinned == shouldPin) {
                    checklist
                } else {
                    checklist.copy(isPinned = shouldPin, updatedAt = now)
                }
            },
        )
    }

    fun addItem(
        state: AppState,
        checklistId: String,
        itemId: String,
        text: String,
        now: Long,
    ): AppState {
        val clean = requireText(text, "Item text")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.id != checklistId) {
                    checklist
                } else {
                    val position = (checklist.items.maxOfOrNull { it.position } ?: -1) + 1
                    checklist.copy(
                        items = checklist.items + ChecklistItem(
                            id = itemId,
                            text = clean,
                            createdAt = now,
                            position = position,
                        ),
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun updateItem(state: AppState, itemId: String, text: String, now: Long): AppState {
        val clean = requireText(text, "Item text")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.map { item ->
                            if (item.id == itemId) item.copy(text = clean) else item
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun toggleItem(state: AppState, itemId: String, now: Long): AppState =
        state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.map { item ->
                            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                        },
                        updatedAt = now,
                    )
                }
            },
        )

    fun deleteItem(state: AppState, itemId: String, now: Long): AppState =
        state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.filterNot { it.id == itemId },
                        updatedAt = now,
                    )
                }
            },
        )

    internal fun requireText(value: String, label: String): String {
        val clean = value.trim()
        require(clean.isNotEmpty()) { "$label must not be blank" }
        return clean
    }
}
