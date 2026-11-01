package dev.abhiram.checklist.widget

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigBindingTest {
    @Test
    fun `selection writes the binding before refreshing`() = runTest {
        val calls = mutableListOf<String>()

        val result = bindSelectedChecklist(
            checklistId = "todo",
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertTrue(result)
        assertEquals(listOf("bind:todo", "refresh"), calls)
    }

    @Test
    fun `cancel or empty state writes nothing`() = runTest {
        val calls = mutableListOf<String>()

        val result = bindSelectedChecklist(
            checklistId = null,
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertFalse(result)
        assertTrue(calls.isEmpty())
    }
}
