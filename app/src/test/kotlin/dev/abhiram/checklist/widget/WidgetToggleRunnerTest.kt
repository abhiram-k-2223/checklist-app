package dev.abhiram.checklist.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetToggleRunnerTest {

    @Test
    fun `toggle completes before the widget refresh`() = runTest {
        val calls = mutableListOf<String>()
        val runner = WidgetToggleRunner(
            toggle = { calls += "toggle:$it" },
            refresh = { calls += "refresh:$it" },
        )

        runner.run("item-1")

        assertEquals(listOf("toggle:item-1", "refresh:item-1"), calls)
    }

    @Test
    fun `toggle failure is reported and refresh still runs`() = runTest {
        val errors = mutableListOf<String>()
        var refreshed = false
        val runner = WidgetToggleRunner(
            toggle = { throw IllegalStateException("write failed") },
            refresh = { refreshed = true },
            onError = { stage, _ -> errors += stage },
        )

        runner.run("item-1")

        assertEquals(listOf("toggle"), errors)
        assertTrue(refreshed)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        val runner = WidgetToggleRunner(
            toggle = { throw CancellationException("cancelled") },
            refresh = {},
        )

        try {
            runner.run("item-1")
        } catch (expected: CancellationException) {
            return@runTest
        }
        throw AssertionError("Expected CancellationException")
    }
}
