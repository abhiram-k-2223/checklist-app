package dev.abhiram.checklist.widget

import androidx.work.ExistingWorkPolicy
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakRolloverTest {

    @Test
    fun `app start keeps the pending rollover and the worker replaces it`() {
        assertEquals(ExistingWorkPolicy.KEEP, streakRolloverPolicy(fromWorker = false))
        assertEquals(ExistingWorkPolicy.REPLACE, streakRolloverPolicy(fromWorker = true))
    }

    @Test
    fun `before midnight waits until the next midnight`() {
        val now = ZonedDateTime.of(2026, 11, 9, 23, 59, 0, 0, ZoneId.of("UTC"))

        assertEquals(60_000L, millisUntilNextMidnight(now))
    }

    @Test
    fun `exactly midnight waits a full day`() {
        val now = ZonedDateTime.of(2026, 11, 9, 0, 0, 0, 0, ZoneId.of("UTC"))

        assertEquals(86_400_000L, millisUntilNextMidnight(now))
    }

    @Test
    fun `just after midnight waits almost a full day`() {
        val now = ZonedDateTime.of(2026, 11, 9, 0, 0, 0, 1_000_000, ZoneId.of("UTC"))

        assertEquals(86_399_999L, millisUntilNextMidnight(now))
    }

    @Test
    fun `spring forward day is twenty three hours`() {
        val now = ZonedDateTime.of(
            2026,
            3,
            8,
            0,
            0,
            0,
            0,
            ZoneId.of("America/New_York"),
        )

        assertEquals(82_800_000L, millisUntilNextMidnight(now))
    }
}
