package dev.abhiram.checklist.data.model

import kotlinx.serialization.Serializable

const val MAX_STREAK_TASKS = 4

@Serializable
data class StreakTracker(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tasks: List<StreakTask> = emptyList(),
)

@Serializable
data class StreakTask(
    val id: String,
    val text: String,
    val color: StreakColor,
    val position: Int,
    val createdAt: Long,
    val completions: Set<String> = emptySet(),
)

@Serializable
enum class StreakColor(val argb: Long) {
    BLUE(0xFF0A84FF),
    GREEN(0xFF30D158),
    ORANGE(0xFFFF9F0A),
    PURPLE(0xFFBF5AF2),
}
