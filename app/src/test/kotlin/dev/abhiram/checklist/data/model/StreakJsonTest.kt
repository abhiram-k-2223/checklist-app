package dev.abhiram.checklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakJsonTest {

    private val tracker = StreakTracker(
        id = "t1",
        title = "Morning",
        createdAt = 1L,
        updatedAt = 2L,
        tasks = listOf(
            StreakTask(
                id = "task1",
                text = "Stretch",
                color = StreakColor.BLUE,
                position = 0,
                createdAt = 3L,
                completions = setOf("2026-11-08", "2026-11-09"),
            ),
        ),
    )

    @Test
    fun `round trip preserves streak trackers and widget bindings`() {
        val state = AppState(
            streakTrackers = listOf(tracker),
            streakWidgetBindings = mapOf("42" to "t1"),
        )

        assertEquals(state, ChecklistJson.decode(ChecklistJson.encode(state)))
    }

    @Test
    fun `old json without streak fields decodes to empty values`() {
        val state = ChecklistJson.decode(
            """{"version":1,"checklists":[],"widgetBindings":{}}""",
        )

        assertTrue(state.streakTrackers.isEmpty())
        assertTrue(state.streakWidgetBindings.isEmpty())
    }

    @Test
    fun `existing checklist json still decodes`() {
        val state = ChecklistJson.decode(
            """{"version":1,"checklists":[{"id":"c1","title":"Groceries","createdAt":1,"updatedAt":2,"items":[]}],"widgetBindings":{"7":"c1"}}""",
        )

        assertEquals("c1", state.checklists.single().id)
        assertEquals("c1", state.widgetBindings["7"])
        assertTrue(state.streakTrackers.isEmpty())
    }

    @Test
    fun `streak colors keep their palette values`() {
        assertEquals(0xFF0A84FFL, StreakColor.BLUE.argb)
        assertEquals(0xFF30D158L, StreakColor.GREEN.argb)
        assertEquals(0xFFFF9F0AL, StreakColor.ORANGE.argb)
        assertEquals(0xFFBF5AF2L, StreakColor.PURPLE.argb)
    }
}
