package dev.abhiram.checklist.widget

import kotlinx.coroutines.CancellationException

/**
 * Runs a widget toggle and then refreshes the widget, in that order.
 *
 * The refresh stays inside the action callback's lifetime, so the process cannot be
 * reclaimed between the DataStore write and the widget render.
 */
class WidgetToggleRunner(
    private val toggle: suspend (String) -> Unit,
    private val refresh: suspend (String) -> Unit,
    private val onError: (String, Throwable) -> Unit = { _, _ -> },
) {
    suspend fun run(itemId: String) {
        try {
            toggle(itemId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onError("toggle", error)
        }

        try {
            refresh(itemId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onError("refresh", error)
        }
    }
}
