package dev.abhiram.checklist.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppState(
    val version: Int = 1,
    val checklists: List<Checklist> = emptyList(),
    val widgetBindings: Map<String, String> = emptyMap(),
)

@Serializable
data class Checklist(
    val id: String,
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<ChecklistItem> = emptyList(),
)

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long,
    val position: Int,
)

object ChecklistJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: AppState): String =
        json.encodeToString(AppState.serializer(), state)

    fun tryDecode(raw: String): AppState? =
        runCatching { json.decodeFromString(AppState.serializer(), raw) }.getOrNull()

    fun decode(raw: String?): AppState = raw?.let(::tryDecode) ?: AppState()
}
