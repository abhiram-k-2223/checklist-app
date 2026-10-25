package dev.abhiram.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetLaunchTest {

    @Test
    fun `non-widget launches are ignored`() {
        assertNull(widgetLaunch(fromWidget = false, checklistId = "c1"))
    }

    @Test
    fun `widget launch opens its checklist`() {
        val launch = widgetLaunch(fromWidget = true, checklistId = "c1")
        assertEquals("c1", launch?.checklistId)
    }

    @Test
    fun `widget empty state asks for the lists screen`() {
        val launch = widgetLaunch(fromWidget = true, checklistId = null)
        assertEquals(WidgetLaunch(checklistId = null), launch)
    }
}
