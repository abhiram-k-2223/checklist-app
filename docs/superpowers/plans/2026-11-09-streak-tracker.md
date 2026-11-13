# Streak Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multiple named streak trackers with up to four colored daily tasks, per-task streak history, a Streaks section in the app, and a dark current-month Streak widget with completion dots and checkable task rows.

**Architecture:** Extend the existing single `AppState` JSON document with `streakTrackers` and a separate `streakWidgetBindings` map. Keep all rules in pure `StreakStore` and `StreakCalendar` functions, expose them through the existing repository `mutate` path, and drive the UI through a new `StreakViewModel`. Add a separate Glance `StreakWidget` with its own picker/config activity and a WorkManager job that refreshes widgets at local midnight.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose, Material 3, Glance 1.2.0, kotlinx.serialization, DataStore Preferences, WorkManager 2.7.1, JUnit 4, kotlinx-coroutines-test, AGP 9.4.1, JDK 21.

**Spec:** `docs/superpowers/specs/2026-11-09-streak-tracker-design.md`

## Global Constraints

- Work from `/home/abhiram/projects/widgets`.
- Export the JDK before every Gradle command:
  ```bash
  export JAVA_HOME=/home/abhiram/.local/opt/jdk21
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- compileSdk 37, minSdk 26, targetSdk 37.
- Kotlin 2.4.21, AGP 9.4.1, Compose BOM 2026.09.00, Glance 1.2.0, WorkManager 2.7.1, JDK 21.
- Do not add dependencies, Room, KSP, Hilt, or a navigation library.
- Keep the app and both widgets always dark.
- Keep `AppState.version = 1`; both new fields must have defaults so old JSON loads.
- All app mutations go through `ChecklistRepository` and the existing atomic `DefaultChecklistRepository.mutate` path.
- Preserve the existing checklist feature, checklist widget, per-widget checklist binding, and all existing tests.
- The existing checklist FAB stays dedicated to creating checklists.
- Streak widget bindings are keyed by `appWidgetId.toString()` and are separate from checklist `widgetBindings`.
- A streak widget never guesses a tracker. Unbound or stale bindings render the unbound state.
- Commit after every task with the conventional message shown in that task.
- On-device phone serial when available: `GAKNWKGQJJJ7QWXW`.

## Review Focus

These are the five failure modes most likely to bite a user. Each one has a test in the owning task and an on-device check in Task 11.

1. **Deleting a tracker with bound widgets leaves stale bindings.** Store and repository tests must prove `deleteTracker` removes every `streakWidgetBindings` entry pointing at that tracker, and the widget must render unbound instead of crashing. Owned by Tasks 2, 4, 5, and the Task 11 on-device delete check.
2. **Streak calculation is wrong at the today/yesterday boundary, after a missed day, or across midnight/month/year boundaries.** Tests must pin today done, today not done but yesterday done, missed yesterday, today after a missed yesterday, unchecking today, empty history, multi-day runs, and month/year crossings. Owned by Task 3.
3. **An unbound or stale widget guesses a tracker or crashes.** `streakTrackerForWidget` must return `null` for no binding and for a binding whose tracker was deleted, and the widget must render the unbound state for both. Owned by Tasks 2, 5, and the Task 11 unbound check.
4. **Widget configuration cancel/empty state writes a binding, loses an existing binding, or binds a tracker that was deleted between render and tap.** The config helper must write nothing for `null`, for the empty state, and for a tracker ID that no longer exists. Owned by Task 6 and the Task 11 reconfigure/cancel checks.
5. **The minimum-size widget exceeds Glance/RemoteViews child limits or hides task rows.** The calendar must be nested as week rows and day cells, not flattened, and the four task rows must live in a scrollable area. Owned by Task 5 and the Task 11 resize/scroll check.

## File Structure

**New files**

- `app/src/main/kotlin/dev/abhiram/checklist/data/model/Streak.kt` — serializable tracker, task, color, and task-limit constant.
- `app/src/main/kotlin/dev/abhiram/checklist/data/StreakStore.kt` — pure tracker/task/binding rules.
- `app/src/main/kotlin/dev/abhiram/checklist/data/StreakCalendar.kt` — streak calculation and six-week calendar helper.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt` — streak labels and available-color helper.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakSummary.kt` — main-screen tracker summary and color list.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakViewModel.kt` — streak screen state and mutations.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakScreen.kt` — streak detail screen and task dialog.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayout.kt` — widget row/locale helpers.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidget.kt` — Glance Streak widget and toggle action.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetReceiver.kt` — Glance receiver.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRefresh.kt` — refresh every Streak widget.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetPicker.kt` — tracker picker and binding helper.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetConfigActivity.kt` — add/reconfigure picker activity.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRollover.kt` — midnight delay, worker, scheduler.
- `app/src/main/res/xml/streak_widget_info.xml` — provider metadata.
- Tests listed inside each task.

**Modified files**

- `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt` — add two streak fields.
- `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt` — add streak methods.
- `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt` — implement streak methods.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt` — Streaks section and new-streak dialog.
- `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt` — route between checklist, streak, and main.
- `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` — create `StreakViewModel`, handle streak-widget intent.
- `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt` — schedule rollover.
- `app/src/main/AndroidManifest.xml` — register receiver and config activity.
- `app/src/main/res/values/strings.xml` — Streak widget strings.
- `README.md` — feature, background-work, widget, architecture, limitation updates.
- `docs/superpowers/handoffs/2026-11-09-streak-tracker-handoff.md` — verification record.

---

### Task 1: Streak Data Model and JSON

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/model/Streak.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/model/StreakJsonTest.kt`

**Interfaces:**
- Consumes: existing `AppState` and `ChecklistJson` in `dev.abhiram.checklist.data.model`.
- Produces:
  - `data class StreakTracker(id: String, title: String, createdAt: Long, updatedAt: Long, tasks: List<StreakTask>)`
  - `data class StreakTask(id: String, text: String, color: StreakColor, position: Int, createdAt: Long, completions: Set<String>)`
  - `enum class StreakColor(val argb: Long)` with `BLUE`, `GREEN`, `ORANGE`, `PURPLE`
  - `const val MAX_STREAK_TASKS = 4`
  - `AppState.streakTrackers: List<StreakTracker>` and `AppState.streakWidgetBindings: Map<String, String>`

- [ ] **Step 1: Write the failing JSON test**

Create `app/src/test/kotlin/dev/abhiram/checklist/data/model/StreakJsonTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.model.StreakJsonTest'
```

Expected: FAIL to compile because `StreakTracker`, `StreakTask`, `StreakColor`, and the new `AppState` fields do not exist.

- [ ] **Step 3: Add the streak models**

Create `app/src/main/kotlin/dev/abhiram/checklist/data/model/Streak.kt`:

```kotlin
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
```

- [ ] **Step 4: Add the fields to `AppState`**

Replace the `AppState` data class in `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt` with:

```kotlin
@Serializable
data class AppState(
    val version: Int = 1,
    val checklists: List<Checklist> = emptyList(),
    val widgetBindings: Map<String, String> = emptyMap(),
    val streakTrackers: List<StreakTracker> = emptyList(),
    val streakWidgetBindings: Map<String, String> = emptyMap(),
)
```

Leave `Checklist`, `ChecklistItem`, and `ChecklistJson` unchanged.

- [ ] **Step 5: Run the test to verify it passes**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.model.StreakJsonTest'
```

Expected: PASS (4 tests).

- [ ] **Step 6: Run the existing JSON tests**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.model.AppStateJsonTest'
```

Expected: PASS. Old JSON compatibility and checklist round trips are unchanged.

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/model/Streak.kt \
  app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/model/StreakJsonTest.kt
git commit -m "feat: add streak tracker data model"
```

---

### Task 2: Streak Store Rules

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/StreakStore.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/StreakStoreTest.kt`

**Interfaces:**
- Consumes: `AppState`, `StreakTracker`, `StreakTask`, `StreakColor`, `MAX_STREAK_TASKS`, `ChecklistStore.requireText`.
- Produces:
  - `createTracker(state, id, title, now): AppState`
  - `renameTracker(state, id, title, now): AppState`
  - `deleteTracker(state, id): AppState`
  - `addTask(state, trackerId, taskId, text, color, now): AppState`
  - `updateTask(state, taskId, text, color, now): AppState`
  - `deleteTask(state, taskId, now): AppState`
  - `toggleTask(state, taskId, date: LocalDate, now): AppState`
  - `setStreakWidgetBinding(state, appWidgetId: Int, trackerId: String): AppState`
  - `removeStreakWidgetBinding(state, appWidgetId: Int): AppState`
  - `streakTrackerForWidget(state, appWidgetId: Int): StreakTracker?`

- [ ] **Step 1: Write the failing store test**

Create `app/src/test/kotlin/dev/abhiram/checklist/data/StreakStoreTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakStoreTest {

    private fun tracker(
        id: String = "t1",
        tasks: List<StreakTask> = emptyList(),
    ) = StreakTracker(
        id = id,
        title = id,
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks,
    )

    private fun task(
        id: String,
        color: StreakColor,
        position: Int = 0,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    @Test
    fun `create tracker trims the title`() {
        val next = StreakStore.createTracker(AppState(), "t1", "  Morning  ", 5L)
        val created = next.streakTrackers.single()

        assertEquals("Morning", created.title)
        assertEquals(5L, created.createdAt)
        assertEquals(5L, created.updatedAt)
        assertTrue(created.tasks.isEmpty())
    }

    @Test
    fun `create tracker rejects a blank title`() {
        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.createTracker(AppState(), "t1", "   ", 5L)
        }
    }

    @Test
    fun `rename tracker trims the title and updates the timestamp`() {
        val state = AppState(streakTrackers = listOf(tracker()))
        val next = StreakStore.renameTracker(state, "t1", "  Evening  ", 9L)

        assertEquals("Evening", next.streakTrackers.single().title)
        assertEquals(9L, next.streakTrackers.single().updatedAt)
    }

    @Test
    fun `rename tracker rejects a blank title`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.renameTracker(state, "t1", "", 9L)
        }
    }

    @Test
    fun `rename unknown tracker is a no-op`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertEquals(state, StreakStore.renameTracker(state, "missing", "New", 9L))
    }

    @Test
    fun `delete tracker removes its tasks and widget bindings`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1", listOf(task("a", StreakColor.BLUE)))),
            streakWidgetBindings = mapOf("1" to "t1", "2" to "other"),
        )

        val next = StreakStore.deleteTracker(state, "t1")

        assertTrue(next.streakTrackers.isEmpty())
        assertEquals(mapOf("2" to "other"), next.streakWidgetBindings)
    }

    @Test
    fun `add task trims text and appends the next position`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE, 0)))),
        )

        val next = StreakStore.addTask(state, "t1", "b", "  Walk  ", StreakColor.GREEN, 7L)
        val added = next.streakTrackers.single().tasks.last()

        assertEquals("Walk", added.text)
        assertEquals(StreakColor.GREEN, added.color)
        assertEquals(1, added.position)
        assertEquals(7L, added.createdAt)
        assertTrue(added.completions.isEmpty())
    }

    @Test
    fun `add task rejects a blank text`() {
        val state = AppState(streakTrackers = listOf(tracker()))

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(state, "t1", "a", "   ", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task rejects a fifth task`() {
        val full = AppState(
            streakTrackers = listOf(
                tracker(
                    tasks = StreakColor.entries.mapIndexed { index, color ->
                        task("task$index", color, index)
                    },
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(full, "t1", "task4", "Fifth", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task rejects a color already used in the tracker`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.addTask(state, "t1", "b", "Walk", StreakColor.BLUE, 7L)
        }
    }

    @Test
    fun `add task to an unknown tracker is a no-op`() {
        val state = AppState()

        assertEquals(state, StreakStore.addTask(state, "missing", "a", "Walk", StreakColor.BLUE, 7L))
    }

    @Test
    fun `update task allows its own color and rejects another task's color`() {
        val state = AppState(
            streakTrackers = listOf(
                tracker(tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN, 1))),
            ),
        )

        val next = StreakStore.updateTask(state, "a", "  New text  ", StreakColor.BLUE, 9L)
        assertEquals("New text", next.streakTrackers.single().tasks.first().text)

        assertThrows(IllegalArgumentException::class.java) {
            StreakStore.updateTask(state, "a", "New text", StreakColor.GREEN, 9L)
        }
    }

    @Test
    fun `delete task removes the task and its history`() {
        val state = AppState(
            streakTrackers = listOf(
                tracker(tasks = listOf(task("a", StreakColor.BLUE, completions = setOf("2026-11-09")))),
            ),
        )

        val next = StreakStore.deleteTask(state, "a", 9L)

        assertTrue(next.streakTrackers.single().tasks.isEmpty())
        assertEquals(9L, next.streakTrackers.single().updatedAt)
    }

    @Test
    fun `toggle task adds and removes the ISO date`() {
        val date = LocalDate.of(2026, 11, 9)
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        val added = StreakStore.toggleTask(state, "a", date, 2L)
        assertEquals(
            setOf("2026-11-09"),
            added.streakTrackers.single().tasks.single().completions,
        )

        val removed = StreakStore.toggleTask(added, "a", date, 3L)
        assertTrue(removed.streakTrackers.single().tasks.single().completions.isEmpty())
    }

    @Test
    fun `toggle unknown task is a no-op`() {
        val state = AppState(
            streakTrackers = listOf(tracker(tasks = listOf(task("a", StreakColor.BLUE)))),
        )

        assertEquals(state, StreakStore.toggleTask(state, "missing", LocalDate.of(2026, 11, 9), 2L))
    }

    @Test
    fun `bind only an existing tracker and replace the same widget binding`() {
        val state = AppState(streakTrackers = listOf(tracker("t1"), tracker("t2")))

        val bound = StreakStore.setStreakWidgetBinding(state, 5, "t2")
        assertEquals("t2", bound.streakWidgetBindings["5"])

        val replaced = StreakStore.setStreakWidgetBinding(bound, 5, "t1")
        assertEquals("t1", replaced.streakWidgetBindings["5"])

        val unknown = StreakStore.setStreakWidgetBinding(state, 6, "missing")
        assertTrue(unknown.streakWidgetBindings.isEmpty())
    }

    @Test
    fun `remove one widget binding leaves the others`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1"), tracker("t2")),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "t2"),
        )

        val next = StreakStore.removeStreakWidgetBinding(state, 5)

        assertEquals(mapOf("6" to "t2"), next.streakWidgetBindings)
    }

    @Test
    fun `resolve a tracker for a widget and return null for unbound or stale`() {
        val state = AppState(
            streakTrackers = listOf(tracker("t1")),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "gone"),
        )

        assertEquals("t1", StreakStore.streakTrackerForWidget(state, 5)?.id)
        assertNull(StreakStore.streakTrackerForWidget(state, 6))
        assertNull(StreakStore.streakTrackerForWidget(state, 7))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.StreakStoreTest'
```

Expected: FAIL to compile because `StreakStore` does not exist.

- [ ] **Step 3: Implement `StreakStore`**

Create `app/src/main/kotlin/dev/abhiram/checklist/data/StreakStore.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate

object StreakStore {

    fun createTracker(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = ChecklistStore.requireText(title, "Streak tracker title")
        val tracker = StreakTracker(
            id = id,
            title = clean,
            createdAt = now,
            updatedAt = now,
        )
        return state.copy(streakTrackers = state.streakTrackers + tracker)
    }

    fun renameTracker(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = ChecklistStore.requireText(title, "Streak tracker title")
        if (state.streakTrackers.none { it.id == id }) return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { tracker ->
                if (tracker.id == id) tracker.copy(title = clean, updatedAt = now) else tracker
            },
        )
    }

    fun deleteTracker(state: AppState, id: String): AppState {
        if (state.streakTrackers.none { it.id == id }) return state
        return state.copy(
            streakTrackers = state.streakTrackers.filterNot { it.id == id },
            streakWidgetBindings = state.streakWidgetBindings.filterValues { it != id },
        )
    }

    fun addTask(
        state: AppState,
        trackerId: String,
        taskId: String,
        text: String,
        color: StreakColor,
        now: Long,
    ): AppState {
        val clean = ChecklistStore.requireText(text, "Task text")
        val tracker = state.streakTrackers.firstOrNull { it.id == trackerId } ?: return state
        require(tracker.tasks.size < MAX_STREAK_TASKS) {
            "A tracker can have at most $MAX_STREAK_TASKS tasks"
        }
        require(tracker.tasks.none { it.color == color }) {
            "That color is already used in this tracker"
        }
        require(state.streakTrackers.none { candidate ->
            candidate.tasks.any { it.id == taskId }
        }) {
            "Task ID already exists"
        }
        val task = StreakTask(
            id = taskId,
            text = clean,
            color = color,
            position = (tracker.tasks.maxOfOrNull { it.position } ?: -1) + 1,
            createdAt = now,
        )
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id == trackerId) {
                    candidate.copy(tasks = candidate.tasks + task, updatedAt = now)
                } else {
                    candidate
                }
            },
        )
    }

    fun updateTask(
        state: AppState,
        taskId: String,
        text: String,
        color: StreakColor,
        now: Long,
    ): AppState {
        val clean = ChecklistStore.requireText(text, "Task text")
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        require(tracker.tasks.none { it.id != taskId && it.color == color }) {
            "That color is already used in this tracker"
        }
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.map { task ->
                            if (task.id == taskId) {
                                task.copy(text = clean, color = color)
                            } else {
                                task
                            }
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun deleteTask(state: AppState, taskId: String, now: Long): AppState {
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.filterNot { it.id == taskId },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun toggleTask(state: AppState, taskId: String, date: LocalDate, now: Long): AppState {
        val isoDate = date.toString()
        val tracker = state.streakTrackers.firstOrNull { candidate ->
            candidate.tasks.any { it.id == taskId }
        } ?: return state
        return state.copy(
            streakTrackers = state.streakTrackers.map { candidate ->
                if (candidate.id != tracker.id) {
                    candidate
                } else {
                    candidate.copy(
                        tasks = candidate.tasks.map { task ->
                            if (task.id != taskId) {
                                task
                            } else {
                                val completions = if (isoDate in task.completions) {
                                    task.completions - isoDate
                                } else {
                                    task.completions + isoDate
                                }
                                task.copy(completions = completions)
                            }
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun setStreakWidgetBinding(state: AppState, appWidgetId: Int, trackerId: String): AppState {
        if (state.streakTrackers.none { it.id == trackerId }) return state
        val key = appWidgetId.toString()
        if (state.streakWidgetBindings[key] == trackerId) return state
        return state.copy(
            streakWidgetBindings = state.streakWidgetBindings + (key to trackerId),
        )
    }

    fun removeStreakWidgetBinding(state: AppState, appWidgetId: Int): AppState {
        val key = appWidgetId.toString()
        if (!state.streakWidgetBindings.containsKey(key)) return state
        return state.copy(streakWidgetBindings = state.streakWidgetBindings - key)
    }

    fun streakTrackerForWidget(state: AppState, appWidgetId: Int): StreakTracker? {
        val trackerId = state.streakWidgetBindings[appWidgetId.toString()] ?: return null
        return state.streakTrackers.firstOrNull { it.id == trackerId }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.StreakStoreTest'
```

Expected: PASS (18 tests).

- [ ] **Step 5: Run the existing store tests**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.ChecklistStore*'
```

Expected: PASS. Checklist rules are unchanged.

- [ ] **Step 6: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/StreakStore.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/StreakStoreTest.kt
git commit -m "feat: add streak store rules"
```

---

### Task 3: Streak Calculation, Calendar, and Labels

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/StreakCalendar.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/StreakCalendarTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt`

**Interfaces:**
- Consumes: `StreakTask`, `StreakColor`, `MAX_STREAK_TASKS`.
- Produces:
  - `data class StreakCalendarDay(date: LocalDate, inCurrentMonth: Boolean, isToday: Boolean, dots: List<StreakColor>)`
  - `isDoneOn(task, date): Boolean`
  - `orderedTasks(tasks): List<StreakTask>`
  - `currentStreak(task, today): Int`
  - `streakCalendarMonth(month, tasks, today, firstDayOfWeek): List<StreakCalendarDay>` (always 42 cells)
  - `streakLabel(streak): String`
  - `compactStreakLabel(streak): String`
  - `availableStreakColors(tasks, editingTaskId = null): List<StreakColor>`

- [ ] **Step 1: Write the failing streak/calendar test**

Create `app/src/test/kotlin/dev/abhiram/checklist/data/StreakCalendarTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalendarTest {

    private fun task(
        id: String,
        color: StreakColor = StreakColor.BLUE,
        position: Int = 0,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    @Test
    fun `today done counts today and earlier consecutive days`() {
        val task = task(
            "a",
            completions = setOf("2026-11-09", "2026-11-08", "2026-11-07"),
        )

        assertEquals(3, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `today not done keeps yesterday's streak alive`() {
        val task = task("a", completions = setOf("2026-11-08", "2026-11-07"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `missed yesterday resets the streak to zero`() {
        val task = task("a", completions = setOf("2026-11-07"))

        assertEquals(0, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `today after a missed yesterday is one`() {
        val task = task("a", completions = setOf("2026-11-09"))

        assertEquals(1, currentStreak(task, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `unchecking today drops the visible streak`() {
        val before = task(
            "a",
            completions = setOf("2026-11-09", "2026-11-08", "2026-11-07"),
        )
        val after = before.copy(completions = before.completions - "2026-11-09")

        assertEquals(3, currentStreak(before, LocalDate.of(2026, 11, 9)))
        assertEquals(2, currentStreak(after, LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `empty history has no streak`() {
        assertEquals(0, currentStreak(task("a"), LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun `streak crosses a month boundary`() {
        val task = task("a", completions = setOf("2026-10-31", "2026-11-01"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `streak crosses a year boundary`() {
        val task = task("a", completions = setOf("2025-12-31", "2026-01-01"))

        assertEquals(2, currentStreak(task, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `calendar always returns forty two cells`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(42, days.size)
        assertEquals(LocalDate.of(2026, 11, 1), days.first().date)
        assertEquals(LocalDate.of(2026, 12, 12), days.last().date)
    }

    @Test
    fun `monday first week starts on the preceding monday`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(LocalDate.of(2026, 10, 26), days.first().date)
        assertEquals(LocalDate.of(2026, 12, 6), days.last().date)
    }

    @Test
    fun `current month and today flags are correct`() {
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = emptyList(),
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertFalse(days.first { it.date == LocalDate.of(2026, 10, 31) }.inCurrentMonth)
        assertTrue(days.first { it.date == LocalDate.of(2026, 11, 1) }.inCurrentMonth)
        assertTrue(days.first { it.date == LocalDate.of(2026, 11, 9) }.isToday)
        assertFalse(days.first { it.date == LocalDate.of(2026, 11, 10) }.isToday)
    }

    @Test
    fun `dots follow task position and include every completed task`() {
        val tasks = listOf(
            task("a", StreakColor.BLUE, 0, setOf("2026-11-09")),
            task("b", StreakColor.GREEN, 1, setOf("2026-11-09")),
        )
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.GREEN),
            days.first { it.date == LocalDate.of(2026, 11, 9) }.dots,
        )
    }

    @Test
    fun `a day can have zero to four dots`() {
        val tasks = StreakColor.entries.mapIndexed { index, color ->
            task("t$index", color, index, setOf("2026-11-09"))
        }
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(4, days.first { it.date == LocalDate.of(2026, 11, 9) }.dots.size)
        assertEquals(0, days.first { it.date == LocalDate.of(2026, 11, 10) }.dots.size)
    }

    @Test
    fun `dots show for adjacent month dates`() {
        val tasks = listOf(task("a", completions = setOf("2026-10-31")))
        val days = streakCalendarMonth(
            month = YearMonth.of(2026, 11),
            tasks = tasks,
            today = LocalDate.of(2026, 11, 9),
            firstDayOfWeek = DayOfWeek.MONDAY,
        )
        val day = days.first { it.date == LocalDate.of(2026, 10, 31) }

        assertFalse(day.inCurrentMonth)
        assertEquals(listOf(StreakColor.BLUE), day.dots)
    }
}
```

- [ ] **Step 2: Write the failing format test**

Create `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt`:

```kotlin
package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakFormatTest {

    private fun task(id: String, color: StreakColor) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = 0,
        createdAt = 1L,
    )

    @Test
    fun `streak label handles zero one and many`() {
        assertEquals("No streak yet", streakLabel(0))
        assertEquals("🔥 1 day", streakLabel(1))
        assertEquals("🔥 6 days", streakLabel(6))
    }

    @Test
    fun `compact label handles zero and many`() {
        assertEquals("—", compactStreakLabel(0))
        assertEquals("🔥 6", compactStreakLabel(6))
    }

    @Test
    fun `available colors exclude colors used by other tasks`() {
        val tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN))

        assertEquals(
            listOf(StreakColor.ORANGE, StreakColor.PURPLE),
            availableStreakColors(tasks),
        )
    }

    @Test
    fun `editing task keeps its own color available`() {
        val tasks = listOf(task("a", StreakColor.BLUE), task("b", StreakColor.GREEN))

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.ORANGE, StreakColor.PURPLE),
            availableStreakColors(tasks, editingTaskId = "a"),
        )
    }
}
```

- [ ] **Step 3: Run both tests to verify they fail**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest \
  --tests 'dev.abhiram.checklist.data.StreakCalendarTest' \
  --tests 'dev.abhiram.checklist.ui.StreakFormatTest'
```

Expected: FAIL to compile because the helpers do not exist.

- [ ] **Step 4: Implement `StreakCalendar.kt`**

Create `app/src/main/kotlin/dev/abhiram/checklist/data/StreakCalendar.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class StreakCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val dots: List<StreakColor>,
)

fun isDoneOn(task: StreakTask, date: LocalDate): Boolean =
    date.toString() in task.completions

fun orderedTasks(tasks: List<StreakTask>): List<StreakTask> =
    tasks.sortedBy { it.position }

fun currentStreak(task: StreakTask, today: LocalDate): Int {
    var cursor = if (isDoneOn(task, today)) today else today.minusDays(1)
    var streak = 0
    while (isDoneOn(task, cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun streakCalendarMonth(
    month: YearMonth,
    tasks: List<StreakTask>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<StreakCalendarDay> {
    val ordered = orderedTasks(tasks)
    val firstOfMonth = month.atDay(1)
    val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val start = firstOfMonth.minusDays(offset.toLong())

    return (0 until 42).map { index ->
        val date = start.plusDays(index.toLong())
        StreakCalendarDay(
            date = date,
            inCurrentMonth = YearMonth.from(date) == month,
            isToday = date == today,
            dots = ordered.filter { isDoneOn(it, date) }.map { it.color },
        )
    }
}
```

- [ ] **Step 5: Implement `StreakFormat.kt`**

Create `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt`:

```kotlin
package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask

fun streakLabel(streak: Int): String = when {
    streak <= 0 -> "No streak yet"
    streak == 1 -> "🔥 1 day"
    else -> "🔥 $streak days"
}

fun compactStreakLabel(streak: Int): String =
    if (streak <= 0) "—" else "🔥 $streak"

fun availableStreakColors(
    tasks: List<StreakTask>,
    editingTaskId: String? = null,
): List<StreakColor> {
    val used = tasks
        .filter { it.id != editingTaskId }
        .map { it.color }
        .toSet()
    return StreakColor.entries.filterNot { it in used }
}
```

- [ ] **Step 6: Run both tests to verify they pass**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest \
  --tests 'dev.abhiram.checklist.data.StreakCalendarTest' \
  --tests 'dev.abhiram.checklist.ui.StreakFormatTest'
```

Expected: PASS (14 calendar tests and 4 format tests).

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/StreakCalendar.kt \
  app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/StreakCalendarTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt
git commit -m "feat: add streak calculation and calendar helpers"
```

---

### Task 4: Repository Streak Methods

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt`
- Modify: `app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt` (add new methods to both fakes)
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/StreakRepositoryTest.kt`

**Interfaces:**
- Consumes: `StreakStore`, `ChecklistJson`, `StatePersistence`, existing `mutate`, `now`, `newId`.
- Produces on `ChecklistRepository`:
  - `suspend fun createStreakTracker(title: String): String`
  - `suspend fun renameStreakTracker(id: String, title: String)`
  - `suspend fun deleteStreakTracker(id: String)`
  - `suspend fun addStreakTask(trackerId: String, text: String, color: StreakColor): String`
  - `suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor)`
  - `suspend fun deleteStreakTask(taskId: String)`
  - `suspend fun toggleStreakTask(taskId: String, date: LocalDate)`
  - `suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String)`
  - `suspend fun removeStreakWidgetBinding(appWidgetId: Int)`

- [ ] **Step 1: Write the failing repository test**

Create `app/src/test/kotlin/dev/abhiram/checklist/data/StreakRepositoryTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.ChecklistJson
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakRepositoryTest {

    private class FakePersistence(initial: String? = null) : StatePersistence {
        val raw = MutableStateFlow(initial)
        val backups = mutableListOf<String>()

        override val rawState: Flow<String?> get() = raw

        override suspend fun update(transform: (String?) -> PersistenceUpdate) {
            val update = transform(raw.value)
            update.corruptBackup?.let(backups::add)
            raw.value = update.nextRaw
        }
    }

    private fun state(persistence: FakePersistence) =
        ChecklistJson.decode(persistence.raw.value)

    @Test
    fun `create tracker persists a trimmed title and returns the generated id`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "t1" },
        )

        val id = repository.createStreakTracker("  Morning  ")

        assertEquals("t1", id)
        assertEquals("Morning", state(persistence).streakTrackers.single().title)
    }

    @Test
    fun `add task persists color position and empty completions`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")

        val taskId = repository.addStreakTask(trackerId, "  Stretch  ", StreakColor.BLUE)
        val task = state(persistence).streakTrackers.single().tasks.single()

        assertEquals("generated", taskId)
        assertEquals("Stretch", task.text)
        assertEquals(StreakColor.BLUE, task.color)
        assertEquals(0, task.position)
        assertTrue(task.completions.isEmpty())
    }

    @Test
    fun `toggle task adds then removes the local date`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        val taskId = repository.addStreakTask(trackerId, "Stretch", StreakColor.BLUE)
        val date = LocalDate.of(2026, 11, 9)

        repository.toggleStreakTask(taskId, date)
        assertEquals(
            setOf("2026-11-09"),
            state(persistence).streakTrackers.single().tasks.single().completions,
        )

        repository.toggleStreakTask(taskId, date)
        assertTrue(state(persistence).streakTrackers.single().tasks.single().completions.isEmpty())
    }

    @Test
    fun `delete task removes its completion history`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        val taskId = repository.addStreakTask(trackerId, "Stretch", StreakColor.BLUE)
        repository.toggleStreakTask(taskId, LocalDate.of(2026, 11, 9))

        repository.deleteStreakTask(taskId)

        assertTrue(state(persistence).streakTrackers.single().tasks.isEmpty())
    }

    @Test
    fun `delete tracker removes its widget bindings`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )
        val trackerId = repository.createStreakTracker("Morning")
        repository.setStreakWidgetBinding(7, trackerId)

        repository.deleteStreakTracker(trackerId)

        assertTrue(state(persistence).streakTrackers.isEmpty())
        assertTrue(state(persistence).streakWidgetBindings.isEmpty())
    }

    @Test
    fun `binding only an existing tracker is persisted`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "generated" },
        )

        repository.setStreakWidgetBinding(7, "missing")

        assertTrue(state(persistence).streakWidgetBindings.isEmpty())
    }

    @Test
    fun `removing one binding leaves the other widget bound`() = runTest {
        val persistence = FakePersistence()
        var nextId = 0
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 5L },
            newId = { "t${nextId++}" },
        )
        val first = repository.createStreakTracker("First")
        val second = repository.createStreakTracker("Second")
        repository.setStreakWidgetBinding(7, first)
        repository.setStreakWidgetBinding(8, second)

        repository.removeStreakWidgetBinding(7)

        assertEquals(mapOf("8" to second), state(persistence).streakWidgetBindings)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.StreakRepositoryTest'
```

Expected: FAIL to compile because the repository methods do not exist.

- [ ] **Step 3: Add the methods to the repository interface**

In `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt`, add these imports:

```kotlin
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
```

Add these declarations before the closing brace of `ChecklistRepository`:

```kotlin
    suspend fun createStreakTracker(title: String): String
    suspend fun renameStreakTracker(id: String, title: String)
    suspend fun deleteStreakTracker(id: String)
    suspend fun addStreakTask(trackerId: String, text: String, color: StreakColor): String
    suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor)
    suspend fun deleteStreakTask(taskId: String)
    suspend fun toggleStreakTask(taskId: String, date: LocalDate)
    suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String)
    suspend fun removeStreakWidgetBinding(appWidgetId: Int)
```

- [ ] **Step 4: Implement the methods in `DefaultChecklistRepository`**

Add these imports to `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt`:

```kotlin
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
```

Add these overrides before the closing brace of `DefaultChecklistRepository`:

```kotlin
    override suspend fun createStreakTracker(title: String): String {
        val id = newId()
        mutate { state -> StreakStore.createTracker(state, id, title, now()) }
        return id
    }

    override suspend fun renameStreakTracker(id: String, title: String) {
        mutate { state -> StreakStore.renameTracker(state, id, title, now()) }
    }

    override suspend fun deleteStreakTracker(id: String) {
        mutate { state -> StreakStore.deleteTracker(state, id) }
    }

    override suspend fun addStreakTask(
        trackerId: String,
        text: String,
        color: StreakColor,
    ): String {
        val id = newId()
        mutate { state -> StreakStore.addTask(state, trackerId, id, text, color, now()) }
        return id
    }

    override suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor) {
        mutate { state -> StreakStore.updateTask(state, taskId, text, color, now()) }
    }

    override suspend fun deleteStreakTask(taskId: String) {
        mutate { state -> StreakStore.deleteTask(state, taskId, now()) }
    }

    override suspend fun toggleStreakTask(taskId: String, date: LocalDate) {
        mutate { state -> StreakStore.toggleTask(state, taskId, date, now()) }
    }

    override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) {
        mutate { state -> StreakStore.setStreakWidgetBinding(state, appWidgetId, trackerId) }
    }

    override suspend fun removeStreakWidgetBinding(appWidgetId: Int) {
        mutate { state -> StreakStore.removeStreakWidgetBinding(state, appWidgetId) }
    }
```

- [ ] **Step 5: Update the existing test fakes**

In `app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt`, add these imports:

```kotlin
import dev.abhiram.checklist.data.model.StreakColor
import java.time.LocalDate
```

Add these methods inside `ThrowingRepository`:

```kotlin
        override suspend fun createStreakTracker(title: String): String = throw IOException("fail")
        override suspend fun renameStreakTracker(id: String, title: String) = throw IOException("fail")
        override suspend fun deleteStreakTracker(id: String) = throw IOException("fail")
        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String = throw IOException("fail")
        override suspend fun updateStreakTask(
            taskId: String,
            text: String,
            color: StreakColor,
        ) = throw IOException("fail")
        override suspend fun deleteStreakTask(taskId: String) = throw IOException("fail")
        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) = throw IOException("fail")
        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) = throw IOException("fail")
        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) = throw IOException("fail")
```

Add these methods inside `NoOpRepository`:

```kotlin
        override suspend fun createStreakTracker(title: String): String = "id"
        override suspend fun renameStreakTracker(id: String, title: String) = Unit
        override suspend fun deleteStreakTracker(id: String) = Unit
        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String = "id"
        override suspend fun updateStreakTask(
            taskId: String,
            text: String,
            color: StreakColor,
        ) = Unit
        override suspend fun deleteStreakTask(taskId: String) = Unit
        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) = Unit
        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) = Unit
        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) = Unit
```

- [ ] **Step 6: Run the repository and ViewModel tests**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest \
  --tests 'dev.abhiram.checklist.data.StreakRepositoryTest' \
  --tests 'dev.abhiram.checklist.ui.ChecklistViewModelTest'
```

Expected: PASS. The new repository test has 7 tests; the existing ViewModel tests remain green.

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt \
  app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/StreakRepositoryTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt
git commit -m "feat: add streak repository methods"
```

### Task 5: Streak Widget Helpers, Renderer, Refresh, and Receiver

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayout.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidget.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetReceiver.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRefresh.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayoutTest.kt`

**Interfaces:**
- Consumes: `AppState`, `StreakTracker`, `StreakTask`, `currentStreak`, `isDoneOn`, `orderedTasks`, `streakCalendarMonth`, `compactStreakLabel`, `WidgetToggleRunner`, `ChecklistApplication.repository`.
- Produces:
  - `data class StreakWidgetTaskRow(task, streak, doneToday)`
  - `streakWidgetTracker(state, appWidgetId): StreakTracker?`
  - `streakWidgetRows(tracker, today): List<StreakWidgetTaskRow>`
  - `firstDayOfWeek(locale = Locale.getDefault()): DayOfWeek`
  - `orderedWeekdayLabels(firstDayOfWeek, locale = Locale.getDefault()): List<String>`
  - `class StreakWidget : GlanceAppWidget` with `EXTRA_FROM_WIDGET = "from_streak_widget"`
  - `class ToggleStreakTaskAction : ActionCallback`
  - `class StreakWidgetReceiver : GlanceAppWidgetReceiver`
  - `suspend fun updateStreakWidgets(context)`

- [ ] **Step 1: Write the failing widget-helper test**

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayoutTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakWidgetLayoutTest {

    private fun task(
        id: String,
        color: StreakColor,
        position: Int,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    private fun tracker(tasks: List<StreakTask>) = StreakTracker(
        id = "t1",
        title = "Morning",
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks,
    )

    @Test
    fun `rows are ordered by position with streak and today state`() {
        val today = LocalDate.of(2026, 11, 9)
        val tracker = tracker(
            listOf(
                task("second", StreakColor.GREEN, 1),
                task(
                    "first",
                    StreakColor.BLUE,
                    0,
                    setOf("2026-11-09", "2026-11-08"),
                ),
            ),
        )

        val rows = streakWidgetRows(tracker, today)

        assertEquals(listOf("first", "second"), rows.map { it.task.id })
        assertEquals(2, rows.first().streak)
        assertTrue(rows.first().doneToday)
        assertEquals(0, rows.last().streak)
        assertFalse(rows.last().doneToday)
    }

    @Test
    fun `tracker resolution returns null for unbound and stale widgets`() {
        val state = AppState(
            streakTrackers = listOf(tracker(emptyList())),
            streakWidgetBindings = mapOf("5" to "t1", "6" to "gone"),
        )

        assertEquals("t1", streakWidgetTracker(state, 5)?.id)
        assertNull(streakWidgetTracker(state, 6))
        assertNull(streakWidgetTracker(state, 7))
    }

    @Test
    fun `weekday labels follow the locale first day`() {
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals("Sun", orderedWeekdayLabels(DayOfWeek.SUNDAY, Locale.US).first())
    }

    @Test
    fun `monday first locale starts with monday`() {
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.UK))
        assertEquals("Mon", orderedWeekdayLabels(DayOfWeek.MONDAY, Locale.UK).first())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakWidgetLayoutTest'
```

Expected: FAIL to compile because the helpers do not exist.

- [ ] **Step 3: Implement the widget helpers**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayout.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.currentStreak
import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

data class StreakWidgetTaskRow(
    val task: StreakTask,
    val streak: Int,
    val doneToday: Boolean,
)

fun streakWidgetTracker(state: AppState, appWidgetId: Int): StreakTracker? {
    val trackerId = state.streakWidgetBindings[appWidgetId.toString()] ?: return null
    return state.streakTrackers.firstOrNull { it.id == trackerId }
}

fun streakWidgetRows(
    tracker: StreakTracker,
    today: LocalDate,
): List<StreakWidgetTaskRow> =
    orderedTasks(tracker.tasks).map { task ->
        StreakWidgetTaskRow(
            task = task,
            streak = currentStreak(task, today),
            doneToday = isDoneOn(task, today),
        )
    }

fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek =
    WeekFields.of(locale).firstDayOfWeek

fun orderedWeekdayLabels(
    firstDayOfWeek: DayOfWeek,
    locale: Locale = Locale.getDefault(),
): List<String> {
    val days = DayOfWeek.entries
    val start = days.indexOf(firstDayOfWeek)
    return (0 until 7).map { offset ->
        days[(start + offset) % 7].getDisplayName(TextStyle.SHORT, locale)
    }
}
```

- [ ] **Step 4: Implement the widget**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidget.kt`:

```kotlin
package dev.abhiram.checklist.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.StreakCalendarDay
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.streakCalendarMonth
import dev.abhiram.checklist.ui.compactStreakLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException

private val WidgetBackground = Color(0xFF000000)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)
private val WidgetOtherMonth = Color(0xFF48484A)
private val WidgetToday = Color(0xFF1C1C1E)

class StreakWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val application = context.applicationContext as ChecklistApplication
        val repository = application.repository
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initial = try {
            repository.snapshot()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to read streak state", error)
            AppState()
        }

        provideContent {
            val state by repository.state.collectAsState(initial = initial)
            val tracker = streakWidgetTracker(state, appWidgetId)
            val openIntent = remember {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    putExtra(EXTRA_FROM_WIDGET, true)
                }
            }
            StreakWidgetContent(
                tracker = tracker,
                hasTrackers = state.streakTrackers.isNotEmpty(),
                openIntent = openIntent,
            )
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            (context.applicationContext as ChecklistApplication)
                .repository
                .removeStreakWidgetBinding(appWidgetId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to remove streak widget binding", error)
        }
        super.onDelete(context, glanceId)
    }

    companion object {
        const val TAG = "StreakWidget"
        const val EXTRA_FROM_WIDGET = "from_streak_widget"
        internal val TASK_ID_KEY = ActionParameters.Key<String>("streak_task_id")
    }
}

class ToggleStreakTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[StreakWidget.TASK_ID_KEY] ?: return
        val repository = (context.applicationContext as ChecklistApplication).repository
        val today = LocalDate.now(ZoneId.systemDefault())
        var toggled = false
        WidgetToggleRunner(
            toggle = {
                repository.toggleStreakTask(taskId, today)
                toggled = true
            },
            refresh = {
                if (toggled) StreakWidget().update(context, glanceId)
            },
            onError = { stage, error ->
                Log.e(StreakWidget.TAG, "Widget $stage failed", error)
            },
        ).run(taskId)
    }
}

@Composable
private fun StreakWidgetContent(
    tracker: StreakTracker?,
    hasTrackers: Boolean,
    openIntent: Intent,
) {
    val outerPadding = if (LocalSize.current.height.value < 360f) 10.dp else 14.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(outerPadding),
    ) {
        if (tracker == null) {
            UnboundContent(hasTrackers = hasTrackers, openIntent = openIntent)
        } else {
            BoundContent(tracker = tracker, openIntent = openIntent)
        }
    }
}

@Composable
private fun UnboundContent(hasTrackers: Boolean, openIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(openIntent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (hasTrackers) "Choose a streak tracker" else "Create a streak tracker in the app",
            style = TextStyle(
                color = ColorProvider(WidgetText),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Open the app to choose one",
            style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 12.sp),
        )
    }
}

@Composable
private fun BoundContent(tracker: StreakTracker, openIntent: Intent) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val month = YearMonth.from(today)
    val locale = Locale.getDefault()
    val firstDay = firstDayOfWeek(locale)
    val days = streakCalendarMonth(month, tracker.tasks, today, firstDay)
    val rows = streakWidgetRows(tracker, today)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openIntent)),
        ) {
            Text(
                text = tracker.title,
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            WeekdayHeader(firstDay = firstDay, locale = locale)
            Spacer(GlanceModifier.height(4.dp))
            CalendarGrid(days = days)
        }
        Spacer(GlanceModifier.height(10.dp))
        StreakDivider()
        Spacer(GlanceModifier.height(6.dp))
        if (rows.isEmpty()) {
            Text(
                text = "Add daily tasks in the app",
                style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 12.sp),
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
            ) {
                items(rows, itemId = { it.task.id.hashCode().toLong() }) { row ->
                    StreakTaskRow(row = row)
                    StreakDivider()
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(firstDay: DayOfWeek, locale: Locale) {
    val labels = orderedWeekdayLabels(firstDay, locale)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        labels.forEach { label ->
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 9.sp),
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(days: List<StreakCalendarDay>) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        days.chunked(7).forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(day = day)
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: StreakCalendarDay) {
    val cellModifier = if (day.isToday) {
        GlanceModifier
            .defaultWeight()
            .height(30.dp)
            .background(ColorProvider(WidgetToday))
            .cornerRadius(8.dp)
    } else {
        GlanceModifier.defaultWeight().height(30.dp)
    }
    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = TextStyle(
                    color = ColorProvider(
                        if (day.inCurrentMonth) WidgetText else WidgetOtherMonth,
                    ),
                    fontSize = 10.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                ),
            )
            Spacer(GlanceModifier.height(1.dp))
            Row(horizontalAlignment = Alignment.CenterHorizontally) {
                day.dots.forEach { color ->
                    Box(
                        modifier = GlanceModifier
                            .size(4.dp)
                            .background(ColorProvider(Color(color.argb)))
                            .cornerRadius(2.dp),
                    )
                    Spacer(GlanceModifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
private fun StreakTaskRow(row: StreakWidgetTaskRow) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(
                actionRunCallback<ToggleStreakTaskAction>(
                    actionParametersOf(StreakWidget.TASK_ID_KEY to row.task.id),
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(7.dp)
                .background(ColorProvider(Color(row.task.color.argb)))
                .cornerRadius(4.dp),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.task.text,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(if (row.doneToday) WidgetMuted else WidgetText),
                fontSize = 13.sp,
            ),
            maxLines = 1,
        )
        Text(
            text = compactStreakLabel(row.streak),
            style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 11.sp),
        )
        Spacer(GlanceModifier.width(8.dp))
        Image(
            provider = ImageProvider(
                if (row.doneToday) R.drawable.ic_widget_check else R.drawable.ic_widget_circle,
            ),
            contentDescription = if (row.doneToday) "Done" else "Not done",
            modifier = GlanceModifier.size(20.dp),
        )
    }
}

@Composable
private fun StreakDivider() {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ColorProvider(WidgetSeparator)),
    )
}
```

- [ ] **Step 5: Add the receiver and refresh helper**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetReceiver.kt`:

```kotlin
package dev.abhiram.checklist.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
```

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRefresh.kt`:

```kotlin
package dev.abhiram.checklist.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

suspend fun updateStreakWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(StreakWidget::class.java).forEach { glanceId ->
        StreakWidget().update(context, glanceId)
    }
}
```

- [ ] **Step 6: Run the helper test and build the debug app**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakWidgetLayoutTest'
./gradlew :app:assembleDebug
```

Expected: helper test PASS (4 tests); debug build succeeds, proving the Glance imports and composables compile.

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayout.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidget.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetReceiver.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRefresh.kt \
  app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetLayoutTest.kt
git commit -m "feat: add streak widget"
```

---

### Task 6: Streak Widget Picker, Config Activity, and Registration

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetPicker.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetConfigActivity.kt`
- Create: `app/src/main/res/xml/streak_widget_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetPickerTest.kt`

**Interfaces:**
- Consumes: `StreakTracker`, `orderedTasks`, `MAX_STREAK_TASKS`, `StreakWidget`, `ChecklistApplication.repository`, existing `ChecklistTheme`.
- Produces:
  - `streakPickerSummary(tracker): String`
  - `streakPickerColors(tracker): List<StreakColor>`
  - `suspend fun bindSelectedStreakTracker(trackerId, trackers, setBinding, refresh): Boolean`
  - `@Composable fun StreakWidgetPicker(trackers, onSelect, modifier)`
  - `class StreakWidgetConfigActivity : ComponentActivity`
  - registered `StreakWidgetReceiver` and config activity.

- [ ] **Step 1: Write the failing picker test**

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetPickerTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakWidgetPickerTest {

    private fun task(id: String, color: StreakColor, position: Int) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
    )

    private fun tracker(id: String = "t1", tasks: List<StreakTask> = emptyList()) =
        StreakTracker(
            id = id,
            title = id,
            createdAt = 1L,
            updatedAt = 1L,
            tasks = tasks,
        )

    @Test
    fun `summary reports no tasks or the task count`() {
        assertEquals("No tasks", streakPickerSummary(tracker()))
        assertEquals(
            "2 tasks",
            streakPickerSummary(
                tracker(
                    tasks = listOf(
                        task("a", StreakColor.BLUE, 0),
                        task("b", StreakColor.GREEN, 1),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `colors follow task order and cap at four`() {
        val tracker = tracker(
            tasks = listOf(
                task("e", StreakColor.BLUE, 4),
                task("a", StreakColor.PURPLE, 0),
                task("b", StreakColor.GREEN, 1),
                task("c", StreakColor.ORANGE, 2),
                task("d", StreakColor.BLUE, 3),
            ),
        )

        assertEquals(
            listOf(
                StreakColor.PURPLE,
                StreakColor.GREEN,
                StreakColor.ORANGE,
                StreakColor.BLUE,
            ),
            streakPickerColors(tracker),
        )
    }

    @Test
    fun `null selection writes nothing`() = runTest {
        var writes = 0
        var refreshes = 0

        val bound = bindSelectedStreakTracker(
            trackerId = null,
            trackers = listOf(tracker()),
            setBinding = { writes += 1 },
            refresh = { refreshes += 1 },
        )

        assertFalse(bound)
        assertEquals(0, writes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `missing tracker writes nothing`() = runTest {
        var writes = 0
        var refreshes = 0

        val bound = bindSelectedStreakTracker(
            trackerId = "gone",
            trackers = listOf(tracker()),
            setBinding = { writes += 1 },
            refresh = { refreshes += 1 },
        )

        assertFalse(bound)
        assertEquals(0, writes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `existing tracker binds before refreshing`() = runTest {
        val calls = mutableListOf<String>()

        val bound = bindSelectedStreakTracker(
            trackerId = "t1",
            trackers = listOf(tracker()),
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertTrue(bound)
        assertEquals(listOf("bind:t1", "refresh"), calls)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakWidgetPickerTest'
```

Expected: FAIL to compile because the picker helpers do not exist.

- [ ] **Step 3: Implement the picker and binding helper**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetPicker.kt`:

```kotlin
package dev.abhiram.checklist.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks

fun streakPickerSummary(tracker: StreakTracker): String =
    if (tracker.tasks.isEmpty()) "No tasks" else "${tracker.tasks.size} tasks"

fun streakPickerColors(tracker: StreakTracker): List<StreakColor> =
    orderedTasks(tracker.tasks).map { it.color }.take(MAX_STREAK_TASKS)

suspend fun bindSelectedStreakTracker(
    trackerId: String?,
    trackers: List<StreakTracker>,
    setBinding: suspend (String) -> Unit,
    refresh: suspend () -> Unit,
): Boolean {
    if (trackerId == null || trackers.none { it.id == trackerId }) return false
    setBinding(trackerId)
    refresh()
    return true
}

@Composable
fun StreakWidgetPicker(
    trackers: List<StreakTracker>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(trackers, key = { it.id }) { tracker ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(tracker.id) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tracker.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = streakPickerSummary(tracker),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        streakPickerColors(tracker).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(color.argb), CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Implement the config activity**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetConfigActivity.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class StreakWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(intent)
        if (glanceId == null) {
            finish()
            return
        }

        val repository = (application as ChecklistApplication).repository
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by repository.state.collectAsStateWithLifecycle()
                    val scope = rememberCoroutineScope()
                    StreakWidgetConfigScreen(
                        trackers = state.streakTrackers,
                        onSelect = { trackerId ->
                            scope.launch {
                                try {
                                    val bound = bindSelectedStreakTracker(
                                        trackerId = trackerId,
                                        trackers = state.streakTrackers,
                                        setBinding = { id ->
                                            repository.setStreakWidgetBinding(appWidgetId, id)
                                        },
                                        refresh = {
                                            StreakWidget().update(
                                                this@StreakWidgetConfigActivity,
                                                glanceId,
                                            )
                                        },
                                    )
                                    if (bound) {
                                        setResult(
                                            RESULT_OK,
                                            Intent().putExtra(
                                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                appWidgetId,
                                            ),
                                        )
                                        finish()
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    Log.e("StreakWidgetConfig", "Failed to bind streak widget", error)
                                    finish()
                                }
                            }
                        },
                        onOpenApp = {
                            startActivity(
                                Intent(
                                    this@StreakWidgetConfigActivity,
                                    MainActivity::class.java,
                                ),
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakWidgetConfigScreen(
    trackers: List<StreakTracker>,
    onSelect: (String) -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.streak_widget_config_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (trackers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.streak_widget_config_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.streak_widget_config_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenApp) {
                    Text(stringResource(R.string.widget_config_open_app))
                }
            }
        } else {
            StreakWidgetPicker(
                trackers = trackers,
                onSelect = onSelect,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
```

- [ ] **Step 5: Add provider XML, strings, and manifest entries**

Create `app/src/main/res/xml/streak_widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:configure="dev.abhiram.checklist.widget.StreakWidgetConfigActivity"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="250dp"
    android:minHeight="300dp"
    android:minResizeWidth="250dp"
    android:minResizeHeight="300dp"
    android:targetCellWidth="4"
    android:targetCellHeight="5"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable" />
```

Add these strings inside `<resources>` in `app/src/main/res/values/strings.xml`:

```xml
    <string name="streak_widget_label">Streak</string>
    <string name="streak_widget_config_title">Choose a streak tracker</string>
    <string name="streak_widget_config_empty_title">No streak trackers yet</string>
    <string name="streak_widget_config_empty_body">Open the app to create a streak tracker.</string>
```

Add these entries inside `<application>` in `app/src/main/AndroidManifest.xml`, after the existing `ChecklistWidgetReceiver`:

```xml
        <activity
            android:name=".widget.StreakWidgetConfigActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:theme="@style/Theme.Checklist">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.StreakWidgetReceiver"
            android:exported="true"
            android:label="@string/streak_widget_label">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/streak_widget_info" />
        </receiver>
```

- [ ] **Step 6: Run the picker test and build**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakWidgetPickerTest'
./gradlew :app:assembleDebug
```

Expected: picker test PASS (5 tests); debug build succeeds.

- [ ] **Step 7: Verify the merged manifest and provider XML**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:processDebugMainManifest
grep -n 'StreakWidget' app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
cat app/src/main/res/xml/streak_widget_info.xml
```

Expected: the merged manifest contains `StreakWidgetConfigActivity` with `APPWIDGET_CONFIGURE` and `StreakWidgetReceiver` with `APPWIDGET_UPDATE`; the provider XML has min 250x300 dp and target cells 4x5.

- [ ] **Step 8: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetPicker.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/StreakWidgetConfigActivity.kt \
  app/src/main/res/xml/streak_widget_info.xml \
  app/src/main/res/values/strings.xml \
  app/src/main/AndroidManifest.xml \
  app/src/test/kotlin/dev/abhiram/checklist/widget/StreakWidgetPickerTest.kt
git commit -m "feat: add streak widget configuration"
```

---

### Task 7: Daily Rollover Worker

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRollover.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakRolloverTest.kt`

**Interfaces:**
- Consumes: `updateStreakWidgets`, existing WorkManager dependency and custom `Configuration.Provider`.
- Produces:
  - `const val STREAK_ROLLOVER_WORK = "streak-widget-rollover"`
  - `millisUntilNextMidnight(now: ZonedDateTime): Long`
  - `scheduleStreakRollover(context)`
  - `class StreakRolloverWorker : CoroutineWorker`

- [ ] **Step 1: Write the failing rollover test**

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/StreakRolloverTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakRolloverTest {

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
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakRolloverTest'
```

Expected: FAIL to compile because `millisUntilNextMidnight` does not exist.

- [ ] **Step 3: Implement the rollover worker and scheduler**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRollover.kt`:

```kotlin
package dev.abhiram.checklist.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

const val STREAK_ROLLOVER_WORK = "streak-widget-rollover"

fun millisUntilNextMidnight(now: ZonedDateTime): Long {
    val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, next).toMillis()
}

fun scheduleStreakRollover(context: Context) {
    val request = OneTimeWorkRequestBuilder<StreakRolloverWorker>()
        .setInitialDelay(
            millisUntilNextMidnight(ZonedDateTime.now()),
            TimeUnit.MILLISECONDS,
        )
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        STREAK_ROLLOVER_WORK,
        ExistingWorkPolicy.REPLACE,
        request,
    )
}

class StreakRolloverWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            updateStreakWidgets(applicationContext)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Failed to refresh streak widgets", error)
        } finally {
            try {
                scheduleStreakRollover(applicationContext)
            } catch (error: Exception) {
                Log.e(TAG, "Failed to schedule the next streak rollover", error)
            }
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "StreakRollover"
    }
}
```

- [ ] **Step 4: Schedule the worker from the application**

In `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt`, add this import:

```kotlin
import dev.abhiram.checklist.widget.scheduleStreakRollover
```

Add this override before the closing brace of `ChecklistApplication`:

```kotlin
    override fun onCreate() {
        super.onCreate()
        scheduleStreakRollover(this)
    }
```

- [ ] **Step 5: Run the rollover test and build**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.StreakRolloverTest'
./gradlew :app:assembleDebug
```

Expected: rollover test PASS (4 tests); debug build succeeds.

- [ ] **Step 6: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/widget/StreakRollover.kt \
  app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt \
  app/src/test/kotlin/dev/abhiram/checklist/widget/StreakRolloverTest.kt
git commit -m "feat: refresh streak widgets at midnight"
```

### Task 8: Streak ViewModel

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakViewModel.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakViewModelTest.kt`

**Interfaces:**
- Consumes: `ChecklistRepository.state: StateFlow<AppState>`; `createStreakTracker(title): String`; `renameStreakTracker(id, title)`; `deleteStreakTracker(id)`; `addStreakTask(trackerId, text, color): String`; `updateStreakTask(taskId, text, color)`; `deleteStreakTask(taskId)`; `toggleStreakTask(taskId, date: LocalDate)`; `StreakStore` from Task 2; `StreakColor`/`StreakTask`/`StreakTracker` from Task 1.
- Produces: `StreakUiState(trackers, selectedId, today)` with `selectedTracker`; `StreakViewModel(repository, savedStateHandle, onError, refreshWidgets, today)`; `uiState: StateFlow<StreakUiState>`; `openTracker(id)`, `closeTracker()`, `createTracker(title)`, `renameTracker(id, title)`, `deleteTracker(id)`, `addTask(trackerId, text, color)`, `updateTask(taskId, text, color)`, `deleteTask(taskId)`, `toggleToday(taskId)`; `StreakViewModel.factory(owner, repository, refreshWidgets)`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakViewModelTest.kt`:

```kotlin
package dev.abhiram.checklist.ui

import androidx.lifecycle.SavedStateHandle
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.StreakStore
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StreakViewModelTest {

    private val fixedToday = LocalDate.of(2026, 11, 9)

    private fun tracker(id: String, vararg tasks: StreakTask) = StreakTracker(
        id = id,
        title = id,
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks.toList(),
    )

    private fun task(id: String, color: StreakColor = StreakColor.BLUE) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = 0,
        createdAt = 1L,
    )

    private class FakeRepository(initial: AppState = AppState()) : ChecklistRepository {
        override val state: MutableStateFlow<AppState> = MutableStateFlow(initial)
        var failMutations = false
        var toggledTaskId: String? = null
        var toggledDate: LocalDate? = null
        private var nextTrackerId = "tracker-new"

        private fun requireSuccess() {
            if (failMutations) throw IOException("fail")
        }

        override suspend fun snapshot(): AppState = state.value

        override suspend fun createChecklist(title: String): String = "checklist"
        override suspend fun renameChecklist(id: String, title: String) = Unit
        override suspend fun deleteChecklist(id: String) = Unit
        override suspend fun setPinned(id: String?) = Unit
        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) = Unit
        override suspend fun setWidgetBindingIfUnbound(appWidgetId: Int, checklistId: String) = Unit
        override suspend fun removeWidgetBinding(appWidgetId: Int) = Unit
        override suspend fun addItem(checklistId: String, text: String): String = "item"
        override suspend fun updateItem(itemId: String, text: String) = Unit
        override suspend fun toggleItem(itemId: String) = Unit
        override suspend fun deleteItem(itemId: String) = Unit

        override suspend fun createStreakTracker(title: String): String {
            requireSuccess()
            val id = nextTrackerId
            state.value = StreakStore.createTracker(state.value, id, title, 1L)
            return id
        }

        override suspend fun renameStreakTracker(id: String, title: String) {
            requireSuccess()
            state.value = StreakStore.renameTracker(state.value, id, title, 2L)
        }

        override suspend fun deleteStreakTracker(id: String) {
            requireSuccess()
            state.value = StreakStore.deleteTracker(state.value, id)
        }

        override suspend fun addStreakTask(
            trackerId: String,
            text: String,
            color: StreakColor,
        ): String {
            requireSuccess()
            val id = "task-new"
            state.value = StreakStore.addTask(state.value, trackerId, id, text, color, 3L)
            return id
        }

        override suspend fun updateStreakTask(taskId: String, text: String, color: StreakColor) {
            requireSuccess()
            state.value = StreakStore.updateTask(state.value, taskId, text, color, 4L)
        }

        override suspend fun deleteStreakTask(taskId: String) {
            requireSuccess()
            state.value = StreakStore.deleteTask(state.value, taskId)
        }

        override suspend fun toggleStreakTask(taskId: String, date: LocalDate) {
            requireSuccess()
            toggledTaskId = taskId
            toggledDate = date
            state.value = StreakStore.toggleTask(state.value, taskId, date, 5L)
        }

        override suspend fun setStreakWidgetBinding(appWidgetId: Int, trackerId: String) {
            requireSuccess()
            state.value = StreakStore.setStreakWidgetBinding(state.value, appWidgetId, trackerId)
        }

        override suspend fun removeStreakWidgetBinding(appWidgetId: Int) {
            requireSuccess()
            state.value = StreakStore.removeStreakWidgetBinding(state.value, appWidgetId)
        }
    }

    @Test
    fun `create tracker opens the new tracker`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = {},
                today = { fixedToday },
            )
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.createTracker("Reading")
            advanceUntilIdle()

            assertEquals("Reading", viewModel.uiState.value.selectedTracker?.title)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `delete selected tracker closes the detail screen`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1"), tracker("t2"))),
            )
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = {},
                today = { fixedToday },
            )
            backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.openTracker("t1")
            advanceUntilIdle()
            viewModel.deleteTracker("t1")
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedId)
            assertEquals(listOf("t2"), viewModel.uiState.value.trackers.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `toggle today passes the local date and stores the completion`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1", task("task-1")))),
            )
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = {},
                today = { fixedToday },
            )

            viewModel.toggleToday("task-1")
            advanceUntilIdle()

            assertEquals("task-1", repository.toggledTaskId)
            assertEquals(fixedToday, repository.toggledDate)
            assertEquals(
                setOf("2026-11-09"),
                repository.state.value.streakTrackers.single().tasks.single().completions,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed mutation reports an error and skips the widget refresh`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val errors = mutableListOf<String>()
            var refreshes = 0
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1"))),
            ).apply { failMutations = true }
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { message, _ -> errors += message },
                refreshWidgets = { refreshes++ },
                today = { fixedToday },
            )

            viewModel.renameTracker("t1", "New title")
            advanceUntilIdle()

            assertEquals(listOf("Rename streak tracker failed"), errors)
            assertEquals(0, refreshes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `successful mutation refreshes widgets once`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var refreshes = 0
            val repository = FakeRepository(
                AppState(streakTrackers = listOf(tracker("t1"))),
            )
            val viewModel = StreakViewModel(
                repository = repository,
                savedStateHandle = SavedStateHandle(),
                onError = { _, _ -> },
                refreshWidgets = { refreshes++ },
                today = { fixedToday },
            )

            viewModel.renameTracker("t1", "New title")
            advanceUntilIdle()

            assertEquals(1, refreshes)
            assertEquals("New title", repository.state.value.streakTrackers.single().title)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakViewModelTest'
```

Expected: compilation fails with unresolved references to `StreakViewModel` and `StreakUiState`.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakViewModel.kt`:

```kotlin
package dev.abhiram.checklist.ui

import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class StreakUiState(
    val trackers: List<StreakTracker> = emptyList(),
    val selectedId: String? = null,
    val today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
) {
    val selectedTracker: StreakTracker?
        get() = trackers.firstOrNull { it.id == selectedId }
}

class StreakViewModel(
    private val repository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle,
    private val onError: (String, Throwable) -> Unit = { message, error ->
        Log.e("StreakViewModel", message, error)
    },
    private val refreshWidgets: suspend () -> Unit = {},
    private val today: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<StreakUiState> =
        combine(repository.state, selectedId) { state, requested ->
            val selected = requested?.takeIf { id ->
                state.streakTrackers.any { it.id == id }
            }
            StreakUiState(
                trackers = state.streakTrackers,
                selectedId = selected,
                today = today(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StreakUiState(today = today()),
        )

    fun openTracker(id: String) {
        savedStateHandle[KEY_SELECTED] = id
    }

    fun closeTracker() {
        savedStateHandle[KEY_SELECTED] = null
    }

    fun createTracker(title: String) {
        launchMutation("Create streak tracker") {
            openTracker(repository.createStreakTracker(title))
        }
    }

    fun renameTracker(id: String, title: String) {
        launchMutation("Rename streak tracker") {
            repository.renameStreakTracker(id, title)
        }
    }

    fun deleteTracker(id: String) {
        launchMutation("Delete streak tracker") {
            repository.deleteStreakTracker(id)
            if (selectedId.value == id) closeTracker()
        }
    }

    fun addTask(trackerId: String, text: String, color: StreakColor) {
        launchMutation("Add task") {
            repository.addStreakTask(trackerId, text, color)
        }
    }

    fun updateTask(taskId: String, text: String, color: StreakColor) {
        launchMutation("Update task") {
            repository.updateStreakTask(taskId, text, color)
        }
    }

    fun deleteTask(taskId: String) {
        launchMutation("Delete task") {
            repository.deleteStreakTask(taskId)
        }
    }

    fun toggleToday(taskId: String) {
        launchMutation("Toggle task") {
            repository.toggleStreakTask(taskId, today())
        }
    }

    private fun launchMutation(name: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError("$name failed", error)
                return@launch
            }

            try {
                refreshWidgets()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError("Widget refresh failed", error)
            }
        }
    }

    companion object {
        private const val KEY_SELECTED = "selected_streak_tracker_id"

        fun factory(
            owner: SavedStateRegistryOwner,
            repository: ChecklistRepository,
            refreshWidgets: suspend () -> Unit,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = StreakViewModel(
                    repository = repository,
                    savedStateHandle = handle,
                    refreshWidgets = refreshWidgets,
                ) as T
            }
    }
}
```

- [ ] **Step 4: Run the test and verify it passes**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakViewModelTest'
```

Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/ui/StreakViewModel.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/StreakViewModelTest.kt
git commit -m "feat: add streak view model"
```

### Task 9: Streak Detail Screen

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakScreen.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt`
- Modify: `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt`

**Interfaces:**
- Consumes: `StreakTracker`, `StreakTask`, `StreakColor`, `MAX_STREAK_TASKS`; `orderedTasks`, `isDoneOn`, `currentStreak` from Task 3; `streakLabel`, `availableStreakColors` from Task 3; `TextInputDialog`/`ConfirmDeleteDialog`.
- Produces: `streakColor(color: StreakColor): Color`; `StreakScreen(tracker, today, onBack, onRenameTracker, onDeleteTracker, onAddTask, onUpdateTask, onDeleteTask, onToggleToday)`.

- [ ] **Step 1: Write the failing color-helper test**

In `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt`, add this import:

```kotlin
import androidx.compose.ui.graphics.Color
```

Add this test method inside `StreakFormatTest`:

```kotlin
    @Test
    fun `streak colors map to compose colors`() {
        assertEquals(Color(0xFF0A84FF), streakColor(StreakColor.BLUE))
        assertEquals(Color(0xFF30D158), streakColor(StreakColor.GREEN))
        assertEquals(Color(0xFFFF9F0A), streakColor(StreakColor.ORANGE))
        assertEquals(Color(0xFFBF5AF2), streakColor(StreakColor.PURPLE))
    }
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakFormatTest'
```

Expected: compilation fails with an unresolved reference to `streakColor`.

- [ ] **Step 3: Add the color helper**

In `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt`, add this import:

```kotlin
import androidx.compose.ui.graphics.Color
```

Add this function:

```kotlin
fun streakColor(color: StreakColor): Color = Color(color.argb)
```

- [ ] **Step 4: Create the detail screen**

Create `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakScreen.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.currentStreak
import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog
import java.time.LocalDate

@Composable
fun StreakScreen(
    tracker: StreakTracker,
    today: LocalDate,
    onBack: () -> Unit,
    onRenameTracker: (String) -> Unit,
    onDeleteTracker: () -> Unit,
    onAddTask: (String, StreakColor) -> Unit,
    onUpdateTask: (String, String, StreakColor) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleToday: (String) -> Unit,
) {
    val ordered = remember(tracker.tasks) { orderedTasks(tracker.tasks) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StreakTask?>(null) }
    var deleteTarget by remember { mutableStateOf<StreakTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tracker.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Streak tracker actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename tracker") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete tracker", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { showAddTask = true },
                        enabled = ordered.size < MAX_STREAK_TASKS,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add task")
                    }
                    if (ordered.size >= MAX_STREAK_TASKS) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Maximum 4 tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (ordered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Add up to 4 daily tasks", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Each task gets its own color and streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(ordered, key = { it.id }) { task ->
                    StreakTaskRow(
                        task = task,
                        today = today,
                        onToggle = { onToggleToday(task.id) },
                        onEdit = { editingTask = task },
                        onRequestDelete = { deleteTarget = task },
                    )
                }
            }
        }
    }

    if (showAddTask) {
        StreakTaskDialog(
            title = "Add task",
            initialText = "",
            initialColor = null,
            tasks = ordered,
            editingTaskId = null,
            onDismiss = { showAddTask = false },
            onConfirm = { text, color ->
                showAddTask = false
                onAddTask(text, color)
            },
        )
    }

    editingTask?.let { task ->
        StreakTaskDialog(
            title = "Edit task",
            initialText = task.text,
            initialColor = task.color,
            tasks = ordered,
            editingTaskId = task.id,
            onDismiss = { editingTask = null },
            onConfirm = { text, color ->
                editingTask = null
                onUpdateTask(task.id, text, color)
            },
        )
    }

    deleteTarget?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete task?",
            message = "\"${task.text}\" and its past calendar dots and streak history will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteTask(task.id)
            },
        )
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename streak tracker",
            initialValue = tracker.title,
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { title ->
                showRename = false
                onRenameTracker(title)
            },
        )
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "Delete streak tracker?",
            message = "\"${tracker.title}\", all of its tasks and history will be removed. Bound widgets will become unbound.",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDeleteTracker()
            },
        )
    }
}

@Composable
private fun StreakTaskRow(
    task: StreakTask,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val done = isDoneOn(task, today)
    val streak = currentStreak(task, today)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(streakColor(task.color), CircleShape),
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = streakLabel(streak),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (done) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_circle_outline
                    },
                ),
                contentDescription = if (done) "Mark not done" else "Mark done",
                tint = if (done) {
                    streakColor(task.color)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Task actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onRequestDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun StreakTaskDialog(
    title: String,
    initialText: String,
    initialColor: StreakColor?,
    tasks: List<StreakTask>,
    editingTaskId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, StreakColor) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var color by remember { mutableStateOf(initialColor) }
    val available = remember(tasks, editingTaskId) {
        availableStreakColors(tasks, editingTaskId).toSet()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Task name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(16.dp))
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreakColor.entries.forEach { candidate ->
                        val enabled = candidate in available
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = streakColor(candidate).copy(
                                        alpha = if (enabled) 1f else 0.25f,
                                    ),
                                    shape = CircleShape,
                                )
                                .border(
                                    width = if (color == candidate) 3.dp else 0.dp,
                                    color = if (color == candidate) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape,
                                )
                                .clickable(enabled = enabled) { color = candidate },
                        )
                    }
                }
                if (available.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All colors are in use",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { color?.let { onConfirm(text.trim(), it) } },
                enabled = text.isNotBlank() && color != null,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
```

- [ ] **Step 5: Run the test and build the app**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakFormatTest'
./gradlew :app:assembleDebug
```

Expected: 5 format tests PASS; debug build succeeds.

- [ ] **Step 6: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/ui/StreakScreen.kt \
  app/src/main/kotlin/dev/abhiram/checklist/ui/StreakFormat.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/StreakFormatTest.kt
git commit -m "feat: add streak detail screen"
```

### Task 10: Main Screen, Navigation, and MainActivity

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakSummary.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakSummaryTest.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt`

**Interfaces:**
- Consumes: `StreakTracker`, `orderedTasks`, `isDoneOn`; `StreakViewModel` and `StreakUiState` from Task 8; `StreakScreen` from Task 9; `streakColor` from Task 9; `StreakWidget.EXTRA_FROM_WIDGET` from Task 5; `updateStreakWidgets` from Task 5.
- Produces: `streakTrackerSummary(tracker, today): String`; `streakTrackerColors(tracker): List<StreakColor>`; `ListsScreen(checklists, streakTrackers, today, onOpenChecklist, onCreateChecklist, onRenameChecklist, onDeleteChecklist, onSetPinned, onOpenStreakTracker, onCreateStreakTracker)`; `ChecklistApp(checklistViewModel, streakViewModel)`.

- [ ] **Step 1: Write the failing summary test**

Create `app/src/test/kotlin/dev/abhiram/checklist/ui/StreakSummaryTest.kt`:

```kotlin
package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTask
import dev.abhiram.checklist.data.model.StreakTracker
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakSummaryTest {

    private val today = LocalDate.of(2026, 11, 9)

    private fun task(
        id: String,
        position: Int = 0,
        color: StreakColor = StreakColor.BLUE,
        completions: Set<String> = emptySet(),
    ) = StreakTask(
        id = id,
        text = id,
        color = color,
        position = position,
        createdAt = 1L,
        completions = completions,
    )

    private fun tracker(vararg tasks: StreakTask) = StreakTracker(
        id = "t1",
        title = "Reading",
        createdAt = 1L,
        updatedAt = 1L,
        tasks = tasks.toList(),
    )

    @Test
    fun `summary reports no tasks`() {
        assertEquals("No tasks", streakTrackerSummary(tracker(), today))
    }

    @Test
    fun `summary uses singular for one task`() {
        val tasks = arrayOf(task("a", completions = setOf("2026-11-09")))

        assertEquals("1 task · 1 done today", streakTrackerSummary(tracker(*tasks), today))
    }

    @Test
    fun `summary counts completed tasks`() {
        val tasks = arrayOf(
            task("a", 0, StreakColor.BLUE, setOf("2026-11-09")),
            task("b", 1, StreakColor.GREEN, setOf("2026-11-09")),
            task("c", 2, StreakColor.ORANGE),
        )

        assertEquals("3 tasks · 2 done today", streakTrackerSummary(tracker(*tasks), today))
    }

    @Test
    fun `colors follow task position and stop at four`() {
        val tasks = arrayOf(
            task("d", 3, StreakColor.PURPLE),
            task("b", 1, StreakColor.GREEN),
            task("a", 0, StreakColor.BLUE),
            task("c", 2, StreakColor.ORANGE),
        )

        assertEquals(
            listOf(StreakColor.BLUE, StreakColor.GREEN, StreakColor.ORANGE, StreakColor.PURPLE),
            streakTrackerColors(tracker(*tasks)),
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakSummaryTest'
```

Expected: compilation fails with unresolved references to `streakTrackerSummary` and `streakTrackerColors`.

- [ ] **Step 3: Implement the summary helpers**

Create `app/src/main/kotlin/dev/abhiram/checklist/ui/StreakSummary.kt`:

```kotlin
package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.isDoneOn
import dev.abhiram.checklist.data.model.MAX_STREAK_TASKS
import dev.abhiram.checklist.data.model.StreakColor
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.data.orderedTasks
import java.time.LocalDate

fun streakTrackerSummary(tracker: StreakTracker, today: LocalDate): String {
    val tasks = orderedTasks(tracker.tasks)
    if (tasks.isEmpty()) return "No tasks"
    val done = tasks.count { isDoneOn(it, today) }
    val taskLabel = if (tasks.size == 1) "1 task" else "${tasks.size} tasks"
    return "$taskLabel · $done done today"
}

fun streakTrackerColors(tracker: StreakTracker): List<StreakColor> =
    orderedTasks(tracker.tasks).take(MAX_STREAK_TASKS).map { it.color }
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakSummaryTest'
```

Expected: 4 tests PASS.

- [ ] **Step 5: Add the Streaks section to the main screen**

Replace the entire contents of `app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt` with:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.StreakTracker
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog
import java.time.LocalDate

@Composable
fun ListsScreen(
    checklists: List<Checklist>,
    streakTrackers: List<StreakTracker>,
    today: LocalDate,
    onOpenChecklist: (String) -> Unit,
    onCreateChecklist: (String) -> Unit,
    onRenameChecklist: (String, String) -> Unit,
    onDeleteChecklist: (String) -> Unit,
    onSetPinned: (String?) -> Unit,
    onOpenStreakTracker: (String) -> Unit,
    onCreateStreakTracker: (String) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showCreateStreak by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Checklist?>(null) }
    var deleteTarget by remember { mutableStateOf<Checklist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Checklists") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "New checklist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (checklists.isEmpty()) {
                item(key = "checklist-empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No checklists yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap + to make your first one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(checklists, key = { it.id }) { checklist ->
                    ListCard(
                        checklist = checklist,
                        onOpen = { onOpenChecklist(checklist.id) },
                        onRename = { renameTarget = checklist },
                        onPinToggle = {
                            onSetPinned(if (checklist.isPinned) null else checklist.id)
                        },
                        onRequestDelete = { deleteTarget = checklist },
                    )
                }
            }

            item(key = "streaks-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Streaks",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { showCreateStreak = true }) {
                        Text("New streak")
                    }
                }
            }

            if (streakTrackers.isEmpty()) {
                item(key = "streaks-empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No streak trackers yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { showCreateStreak = true }) {
                            Text("New streak")
                        }
                    }
                }
            } else {
                items(streakTrackers, key = { "streak-${it.id}" }) { tracker ->
                    StreakTrackerCard(
                        tracker = tracker,
                        today = today,
                        onOpen = { onOpenStreakTracker(tracker.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New checklist",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { title ->
                showCreate = false
                onCreateChecklist(title)
            },
        )
    }

    if (showCreateStreak) {
        TextInputDialog(
            title = "New streak",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateStreak = false },
            onConfirm = { title ->
                showCreateStreak = false
                onCreateStreakTracker(title)
            },
        )
    }

    renameTarget?.let { checklist ->
        TextInputDialog(
            title = "Rename checklist",
            initialValue = checklist.title,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                renameTarget = null
                onRenameChecklist(checklist.id, title)
            },
        )
    }

    deleteTarget?.let { checklist ->
        ConfirmDeleteDialog(
            title = "Delete checklist?",
            message = "\"${checklist.title}\" and its items will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteChecklist(checklist.id)
            },
        )
    }
}

@Composable
private fun StreakTrackerCard(
    tracker: StreakTracker,
    today: LocalDate,
    onOpen: () -> Unit,
) {
    val colors = remember(tracker.tasks) { streakTrackerColors(tracker) }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tracker.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(streakColor(color), CircleShape),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (colors.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = streakTrackerSummary(tracker, today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ListCard(
    checklist: Checklist,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onPinToggle: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checklist.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = checklist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = progressLabel(checklist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "List actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (checklist.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            menuOpen = false
                            onPinToggle()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onRequestDelete()
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 6: Route between checklist, streak, and main screens**

Replace the entire contents of `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt` with:

```kotlin
package dev.abhiram.checklist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChecklistApp(
    checklistViewModel: ChecklistViewModel,
    streakViewModel: StreakViewModel,
) {
    val checklistState by checklistViewModel.uiState.collectAsStateWithLifecycle()
    val streakState by streakViewModel.uiState.collectAsStateWithLifecycle()
    val selectedChecklist = checklistState.selectedChecklist
    val selectedTracker = streakState.selectedTracker

    when {
        selectedChecklist != null -> {
            BackHandler { checklistViewModel.closeChecklist() }
            ChecklistScreen(
                checklist = selectedChecklist,
                onBack = checklistViewModel::closeChecklist,
                onAddItem = { text -> checklistViewModel.addItem(selectedChecklist.id, text) },
                onUpdateItem = checklistViewModel::updateItem,
                onToggleItem = checklistViewModel::toggleItem,
                onDeleteItem = checklistViewModel::deleteItem,
                onRenameChecklist = { title ->
                    checklistViewModel.renameChecklist(selectedChecklist.id, title)
                },
                onDeleteChecklist = { checklistViewModel.deleteChecklist(selectedChecklist.id) },
                onTogglePin = {
                    checklistViewModel.setPinned(
                        if (selectedChecklist.isPinned) null else selectedChecklist.id,
                    )
                },
            )
        }

        selectedTracker != null -> {
            BackHandler { streakViewModel.closeTracker() }
            StreakScreen(
                tracker = selectedTracker,
                today = streakState.today,
                onBack = streakViewModel::closeTracker,
                onRenameTracker = { title ->
                    streakViewModel.renameTracker(selectedTracker.id, title)
                },
                onDeleteTracker = { streakViewModel.deleteTracker(selectedTracker.id) },
                onAddTask = { text, color ->
                    streakViewModel.addTask(selectedTracker.id, text, color)
                },
                onUpdateTask = streakViewModel::updateTask,
                onDeleteTask = streakViewModel::deleteTask,
                onToggleToday = streakViewModel::toggleToday,
            )
        }

        else -> {
            ListsScreen(
                checklists = checklistState.checklists,
                streakTrackers = streakState.trackers,
                today = streakState.today,
                onOpenChecklist = { id ->
                    streakViewModel.closeTracker()
                    checklistViewModel.openChecklist(id)
                },
                onCreateChecklist = checklistViewModel::createChecklist,
                onRenameChecklist = checklistViewModel::renameChecklist,
                onDeleteChecklist = checklistViewModel::deleteChecklist,
                onSetPinned = checklistViewModel::setPinned,
                onOpenStreakTracker = { id ->
                    checklistViewModel.closeChecklist()
                    streakViewModel.openTracker(id)
                },
                onCreateStreakTracker = streakViewModel::createTracker,
            )
        }
    }
}
```

- [ ] **Step 7: Create the streak ViewModel and handle streak widget launches**

Replace the entire contents of `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` with:

```kotlin
package dev.abhiram.checklist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.abhiram.checklist.ui.ChecklistApp
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.StreakViewModel
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import dev.abhiram.checklist.widget.ChecklistWidget
import dev.abhiram.checklist.widget.StreakWidget
import dev.abhiram.checklist.widget.updateChecklistWidgets
import dev.abhiram.checklist.widget.updateStreakWidgets

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(
            owner = this,
            repository = app.repository,
            refreshWidgets = { updateChecklistWidgets(applicationContext) },
        )
    }

    private val streakViewModel: StreakViewModel by viewModels {
        val app = application as ChecklistApplication
        StreakViewModel.factory(
            owner = this,
            repository = app.repository,
            refreshWidgets = { updateStreakWidgets(applicationContext) },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (savedInstanceState == null) {
            openFromIntent(intent)
        }
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChecklistApp(viewModel, streakViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent?) {
        val source = intent ?: return

        if (source.getBooleanExtra(StreakWidget.EXTRA_FROM_WIDGET, false)) {
            viewModel.closeChecklist()
            streakViewModel.closeTracker()
            source.removeExtra(StreakWidget.EXTRA_FROM_WIDGET)
            return
        }

        val launch = widgetLaunch(
            fromWidget = source.getBooleanExtra(ChecklistWidget.EXTRA_FROM_WIDGET, false),
            checklistId = source.getStringExtra(ChecklistWidget.EXTRA_OPEN_CHECKLIST),
        ) ?: return

        streakViewModel.closeTracker()
        if (launch.checklistId == null) {
            viewModel.closeChecklist()
        } else {
            viewModel.openChecklist(launch.checklistId)
        }
        source.removeExtra(ChecklistWidget.EXTRA_FROM_WIDGET)
        source.removeExtra(ChecklistWidget.EXTRA_OPEN_CHECKLIST)
    }
}
```

- [ ] **Step 8: Run the tests and build the app**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.ui.StreakSummaryTest'
./gradlew :app:assembleDebug
```

Expected: 4 summary tests PASS; debug build succeeds.

- [ ] **Step 9: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/ui/StreakSummary.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/StreakSummaryTest.kt \
  app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt \
  app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt \
  app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt
git commit -m "feat: add streak trackers to the main screen"
```

### Task 11: Documentation, Verification, and Handoff

**Files:**
- Modify: `README.md`
- Create: `docs/superpowers/handoffs/2026-11-09-streak-tracker-handoff.md`

**Interfaces:**
- Consumes: the finished app, widget, tests, and build outputs from Tasks 1-10.
- Produces: updated user documentation and an evidence-based handoff.

- [ ] **Step 1: Update the README**

In `README.md`, replace the introduction with:

```markdown
A local-only Android app for checklists and daily streaks, with home-screen
widgets. Checklist widgets show the checklist you choose for them and let you
tick items off without opening the app. Streak widgets show a current-month
calendar and today's colored streak tasks.
```

In the **Features** list, add these bullets after the checklist widget bullets:

```markdown
- Multiple named streak trackers, each with up to four daily tasks
- Each streak task has a unique color and its own current streak
- Dark Streak widget with a current-month calendar, colored completion dots,
  today's highlight, and checkable task rows with compact streak counts
- Choose a tracker when adding a Streak widget; reconfigure it later on
  Android 12+
- Deleting a streak task removes its calendar dots and streak history;
  deleting a tracker unbinds its widgets
```

Replace the low-resource bullet that begins `- One Activity and one process;` with:

```markdown
- One Activity and one process; no services, alarms, or polling. One daily
  WorkManager job refreshes streak widgets just after local midnight; it never
  resets completion history.
```

Replace the WorkManager bullet that begins `- Glance bundles WorkManager` with:

```markdown
- Glance bundles WorkManager for async widget requests, but its startup
  initializer is disabled; WorkManager starts only when a widget request or the
  daily streak rollover needs it
```

Replace the widget-refresh bullet that begins `- The widget uses` with:

```markdown
- Both widgets use `updatePeriodMillis=0`; after a successful app mutation the
  ViewModel awaits an update of every widget of that type, and a widget tap
  updates its own widget before the action callback returns. The streak widget
  also refreshes once just after local midnight.
```

In the **Add the widget** section, rename the heading to `## Add the widgets` and add this subsection after the existing checklist steps:

```markdown
### Streak widget

1. Long-press the home screen and open **Widgets**.
2. Find **Streak** and drag it onto the home screen.
3. Pick any streak tracker in the picker. Every tracker is listed in creation
   order.
4. Repeat for more widgets; each one keeps its own tracker.
5. To change an existing widget on Android 12+, long-press it and choose
   **Reconfigure**. Back cancels without changing the binding.
```

In the **Tests** section, replace the sentence that begins `The suite has` with a sentence that starts `The suite has `, states the actual number printed by the test run in Step 2, and then uses this coverage text:

```markdown
tests covering the JSON codec, checklist and streak store rules, repository
persistence, corrupt state recovery, per-widget checklist binding and migration,
streak calculation and calendar layout, streak widget rows and picker, midnight
rollover timing, and ViewModel mutation success/error handling.
```

The finished sentence must read, for example, `The suite has 144 tests covering the JSON codec, ...` with the actual count in place of `144`.

In the **Architecture** section, replace the bullets with:

```markdown
- `data/model` — serializable `AppState`, `Checklist`, `ChecklistItem`,
  `StreakTracker`, `StreakTask`, and `StreakColor`
- `data/ChecklistStore` — pure checklist state transformations
- `data/StreakStore` — pure tracker, task, and streak-widget binding rules
- `data/StreakCalendar` — current-streak and six-week calendar helpers
- `data/WidgetBindings` — per-widget checklist binding helpers and migration
- `data/DefaultChecklistRepository` — atomic DataStore updates
- `ui` — Compose screens, `ChecklistViewModel`, and `StreakViewModel`
- `widget` — Glance checklist and streak renderers, picker configuration
  activities, legacy migration, row math, toggle actions, and the daily streak
  rollover
- `ChecklistApplication` — manual DI container, lazy WorkManager configuration,
  and streak rollover scheduling; ViewModels trigger widget refreshes after
  mutations
```

In the **Limitations** section, add these bullets:

```markdown
- Streak trackers show the current streak only; there is no best-streak metric
- The Streak widget shows the current month only and has no month navigation
- Streak trackers and tasks keep creation order; there is no drag reorder
- Deleting a streak task permanently removes its calendar dots and history
- The daily rollover may be delayed by the system; the widget still derives
  today's date whenever it renders
```

- [ ] **Step 2: Run the full unit suite and builds**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug :app:assembleRelease
```

Expected: all unit tests PASS; both builds succeed. Record the exact test count for the README.

- [ ] **Step 3: Record build and manifest evidence**

```bash
cd /home/abhiram/projects/widgets
sha256sum app/build/outputs/apk/release/app-release.apk
grep -R "StreakWidgetReceiver\|StreakWidgetConfigActivity" app/build/intermediates/merged_manifest* | head -20
cat app/src/main/res/xml/streak_widget_info.xml
git status --short
git log --oneline --decorate -12
```

Expected: the release APK prints a SHA-256; the merged manifest contains both streak components; the provider XML has 250×300 dp minimums, 4×5 target cells, `updatePeriodMillis="0"`, and `reconfigurable`; the working tree contains only the README and handoff changes.

- [ ] **Step 4: Run the on-device matrix if the phone is connected**

```bash
adb devices
```

If `GAKNWKGQJJJ7QWXW` is listed as `device`, install and verify:

```bash
adb -s GAKNWKGQJJJ7QWXW install -r app/build/outputs/apk/release/app-release.apk
adb -s GAKNWKGQJJJ7QWXW shell am start -n dev.abhiram.checklist/.MainActivity
adb -s GAKNWKGQJJJ7QWXW logcat -c
```

Run these checks and record each result:

1. Create a streak tracker with four colored tasks.
2. Add a Streak widget; confirm the picker lists every tracker; choose one.
3. Verify the current-month six-week calendar, today highlight, colored dots, and four task rows with streak labels.
4. Toggle tasks in the widget; confirm dots, streak counts, and app state update immediately.
5. Add a second tracker and a second widget; confirm they stay independent.
6. Reconfigure one widget; confirm only it changes. Cancel a reconfigure; confirm it is unchanged.
7. Add/edit/delete tasks in the app; confirm the four-task limit, unique colors, and the history-deletion warning.
8. Delete a bound tracker; confirm its widget becomes unbound and does not crash.
9. Seed multi-day history and confirm the app streak counters match the rule.
10. Verify process death, reboot, resize/scroll, dark theme in system-light mode, and `adb logcat` for crashes.
11. If practical, advance the device date past midnight, confirm the widget refreshes, then restore the date.

If the phone is not connected, do not claim an on-device pass. Record the matrix as blocked with the exact `adb devices` output.

- [ ] **Step 5: Write the handoff**

Create `docs/superpowers/handoffs/2026-11-09-streak-tracker-handoff.md` with this content, substituting the exact evidence gathered in Steps 2-4:

```markdown
# Streak Tracker Handoff

**Date:** 2026-11-09
**Repository:** `https://github.com/abhiram-k-2223/checklist-app`
**Branch:** `master`
**Implementation range:** the first and last streak commits shown by `git log --oneline`
**Phone:** `GAKNWKGQJJJ7QWXW`

## What changed

- Added `StreakTracker`, `StreakTask`, and `StreakColor` models plus
  `streakTrackers` and `streakWidgetBindings` in `AppState`.
- Added `StreakStore` rules for trackers, tasks, completion history, and
  per-widget tracker bindings.
- Added current-streak calculation and a locale-aware six-week calendar helper.
- Added repository methods for every streak mutation and binding.
- Added `StreakViewModel`, the Streaks section on the main screen, and the
  streak detail screen with task colors, checkboxes, and streak labels.
- Added the dark `StreakWidget` with calendar dots, task rows, configuration,
  reconfiguration, and unbound states.
- Added a daily WorkManager rollover that refreshes streak widgets after local
  midnight without resetting history.
- Updated the README and added this handoff.

## Verification performed

- Unit tests: the exact count from Step 2 passed with
  `./gradlew :app:testDebugUnitTest --rerun-tasks`.
- Debug build: `./gradlew :app:assembleDebug` succeeded.
- Release build: `./gradlew :app:assembleRelease` succeeded.
- Release APK SHA-256: the exact hash from Step 3.
- Merged manifest contains `StreakWidgetReceiver` and
  `StreakWidgetConfigActivity` with the expected exported flags and
  `APPWIDGET_UPDATE` / `APPWIDGET_CONFIGURE` filters.
- `streak_widget_info.xml` has 250×300 dp minimums, 4×5 target cells,
  `updatePeriodMillis="0"`, resize in both directions, and `reconfigurable`.
- Existing checklist tests and behavior remain green.

## On-device verification

| Check | Result |
|---|---|
| Create a tracker with four colored tasks | actual result |
| Add a Streak widget and choose a tracker | actual result |
| Calendar, today highlight, dots, and task rows | actual result |
| Toggle in the widget updates dots, streaks, and app | actual result |
| Two trackers and two widgets stay independent | actual result |
| Reconfigure one widget; cancel leaves it unchanged | actual result |
| Task add/edit/delete limits, colors, and history warning | actual result |
| Delete a bound tracker leaves an unbound widget without a crash | actual result |
| Seeded multi-day streak counters | actual result |
| Process death, reboot, resize/scroll, dark theme, logcat | actual result |
| Optional date-advance rollover and restore | actual result |

Record each `actual result` as `PASS`, `FAIL`, or `BLOCKED — phone not connected`.
If the phone is disconnected, every row is blocked and the section states that
on-device verification is pending.

## Review fixes

Record any fixes made during final review here, or write `None`.
```

Do not leave the words `actual result` in the finished handoff; replace each with the recorded outcome. If the phone is disconnected, every on-device row is `BLOCKED — phone not connected`.

- [ ] **Step 6: Commit and push**

```bash
cd /home/abhiram/projects/widgets
git add README.md docs/superpowers/handoffs/2026-11-09-streak-tracker-handoff.md
git commit -m "docs: document streak tracker release"
git push origin master
git status --short
git log --oneline --decorate -3
```

Expected: commit and push succeed; the working tree is clean.



