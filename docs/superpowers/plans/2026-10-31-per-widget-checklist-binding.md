# Per-Widget Checklist Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let every Checklist home-screen widget remember its own checklist, chosen from a picker that lists every checklist, while Pin becomes only the default/fallback for legacy or unconfigured widgets.

**Architecture:** Store `appWidgetId -> checklistId` bindings in the app-owned `AppState` JSON document. Pure helpers in `data/WidgetBindings.kt` bind, unbind, resolve, and plan migrations; `DefaultChecklistRepository` exposes suspend bind/unbind operations over the existing atomic `mutate` path. The Glance widget resolves its own checklist from the observed repository state, migrates a legacy unbound widget to the pinned checklist once, and removes its binding in `onDelete`. A new exported `ChecklistWidgetConfigActivity` provides the add/reconfigure picker through the standard `android:configure` mechanism and `android:widgetFeatures="reconfigurable"`.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3 1.4.0, Glance 1.2.0, Preferences DataStore 1.1.7, kotlinx.serialization 1.11.0, AGP 9.4.1, Gradle 9.8.1, JDK 21, compileSdk/targetSdk 37, minSdk 26.

**Spec:** docs/superpowers/specs/2026-10-31-per-widget-checklist-binding-design.md

## Global Constraints

- Work from `/home/abhiram/projects/widgets`. Every command block that runs Gradle must start with:
  ```bash
  export JAVA_HOME=/home/abhiram/.local/opt/jdk21
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- Local-only app: no `INTERNET` permission, no network calls, no analytics, no accounts, no cloud sync. Do not add permissions or components beyond the configuration activity described here.
- No new dependencies. Use the existing Compose, Material 3, Glance 1.2.0, Preferences DataStore, lifecycle, and kotlinx.serialization versions already in `gradle/libs.versions.toml`.
- `compileSdk = 37`, `minSdk = 26`, `targetSdk = 37`. Java/Kotlin target stays 17; build with JDK 21 at `/home/abhiram/.local/opt/jdk21`.
- Always-dark theme. The picker uses `dev.abhiram.checklist.ui.theme.ChecklistTheme` and `SystemBarStyle.dark(...)`, exactly like `MainActivity`.
- Any number of widgets can be placed. There is no app-level widget count limit; bindings are keyed by the launcher-assigned app-widget ID.
- The picker lists **every** checklist. The pinned checklist is first and carries a pin badge. Pin never filters the picker and never overrides a widget's own binding.
- Pin remains available in the app and means only "default/fallback checklist" for a legacy or unconfigured widget.
- The existing Groceries widget must keep showing Groceries after the upgrade. An unbound widget with a pinned checklist is migrated to a permanent binding on first render and before the picker can change the pin.
- Preserve existing widget behavior: the open intent (`EXTRA_FROM_WIDGET`, `EXTRA_OPEN_CHECKLIST`), scrollable `LazyColumn`, row toggles through `ToggleItemAction`, empty/unbound states, adaptive compact header below 160 dp, dark palette, 24 dp corners, and 44 dp rows.
- `AppState.version` stays `1`. `widgetBindings` is additive and defaults to `emptyMap()`, so old JSON decodes unchanged.
- Every data mutation goes through `ChecklistRepository`; no direct DataStore access from the UI, widget, or configuration activity.
- Commit after every task with the conventional message named in that task. Do not commit `local.properties`, `build/`, or `.gradle/`.
- The phone serial for on-device verification is `GAKNWKGQJJJ7QWXW`. `gh` is authenticated as `abhiram-k-2223`; the repository currently has no remote.

## Review Focus

These are the five failure modes most likely to slip through if only the happy path is tested. Each one has an owning-task test named below.

- **Bound checklist deleted** — deleting a checklist must remove every binding that points at it; the widget then falls back to the pin if one exists, or to the unbound state if none does. A surviving stale binding must resolve to `null` instead of falling back to the pin. Tests: `WidgetBindingsTest.stale binding resolves to null without pin fallback`, `WidgetBindingsTest.deleting a bound checklist removes the binding and falls back to the pin`, `WidgetBindingsTest.deleting a checklist removes every binding that points at it`, and `DefaultChecklistRepositoryTest.deleting a checklist removes its persisted widget bindings` (Tasks 1–2). On-device check in Task 7.
- **Stale binding / reused app-widget ID** — rebinding the same widget ID must replace the old checklist, unbinding must remove only that ID, and `onDelete` must remove the binding when the host reports deletion. Tests: `WidgetBindingsTest.bind replaces a previous binding for the same widget`, `WidgetBindingsTest.unbind removes only the target widget`, and `WidgetBindingMigrationTest.existing binding is never overwritten by the pin` (Tasks 1 and 3). The repository removal path used by `onDelete` is tested in Task 2.
- **Pin changes** — a bound widget must ignore a different pinned checklist; migration must write only when the widget is unbound; the picker must not filter by pin. Tests: `WidgetBindingsTest.bound id wins over a different pinned checklist`, `WidgetBindingsTest.migration target is the pin only while the widget is unbound`, `WidgetBindingMigrationTest.unbound widget is bound to the pinned checklist`, and `WidgetPickerTest.pinned checklist is first and every checklist is kept` (Tasks 1, 3, 4).
- **Configuration cancel / empty state** — pressing Back must leave the target widget untouched, and the empty state must write no binding. The bulk migration excludes the widget being configured, and the selection helper returns without writing for a null selection. Tests: `WidgetBindingsTest.migration plan skips bound widgets and the widget being configured` and `WidgetConfigBindingTest.cancel or empty state writes nothing` (Task 4). On-device cancel check in Task 7.
- **Multiple widgets with the same checklist** — two widget IDs may point at the same checklist and resolve independently; changing one binding must not change the other. Tests: `WidgetBindingsTest.two widgets can show the same checklist independently` and `WidgetBindingsTest.binding one widget leaves another unbound and pinned` (Task 1). On-device third-widget check in Task 7.

---

## File Structure

- `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt` — adds the additive `widgetBindings` field.
- `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt` — pure bind, unbind, resolve, migration-target, and migration-plan helpers.
- `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt` — deletion also removes bindings that point at the deleted checklist.
- `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt` and `DefaultChecklistRepository.kt` — suspend bind/unbind operations over the existing atomic `mutate` path.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt` — per-widget resolution, one-time legacy migration, and `onDelete` cleanup.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt` — suspend helpers for one-widget and bulk legacy migration.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetPicker.kt` — picker ordering and progress helpers.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetConfigBinding.kt` — cancel-safe selection helper.
- `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetConfigActivity.kt` — dark add/reconfigure picker, selection write, result handling.
- `app/src/main/res/values/strings.xml` — picker title, empty state, pin badge, and open-app copy.
- `app/src/main/res/xml/checklist_widget_info.xml` and `app/src/main/AndroidManifest.xml` — configure/reconfigure registration.
- `README.md` and `docs/superpowers/handoffs/2026-10-31-per-widget-checklist-binding-handoff.md` — user documentation and verification evidence.
- Tests mirror the `data` and `widget` packages under `app/src/test/kotlin/dev/abhiram/checklist/`.

---

### Task 1: Add the binding data model and pure helpers

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt`

**Interfaces:**
- Consumes: `AppState`, `Checklist`, `ChecklistStore`, `ChecklistJson`.
- Produces:
  - `AppState.widgetBindings: Map<String, String>` (key is `appWidgetId.toString()`, value is a checklist ID; default `emptyMap()`).
  - `fun bindWidget(state: AppState, appWidgetId: Int, checklistId: String): AppState`
  - `fun unbindWidget(state: AppState, appWidgetId: Int): AppState`
  - `fun checklistForWidget(state: AppState, appWidgetId: Int): Checklist?`
  - `ChecklistStore.deleteChecklist(state, id)` now also removes every binding whose value equals `id`.

- [ ] **Step 1: Write the failing pure-helper tests**

Create `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WidgetBindingsTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun stateOf(vararg checklists: Checklist) = AppState(checklists = checklists.toList())

    @Test
    fun `bound widget resolves its own checklist`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("todo", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `unbound widget falls back to the pinned checklist`() {
        val state = stateOf(list("groceries", pinned = true), list("todo"))

        assertEquals("groceries", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `bound id wins over a different pinned checklist`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("todo", checklistForWidget(state, 7)?.id)
    }

    @Test
    fun `binding a widget does not change the pin`() {
        val state = stateOf(list("groceries", pinned = true), list("todo"))

        val next = bindWidget(state, 7, "todo")

        assertEquals("groceries", next.checklists.first { it.isPinned }.id)
    }

    @Test
    fun `stale binding resolves to null without pin fallback`() {
        val state = stateOf(list("groceries", pinned = true)).copy(
            widgetBindings = mapOf("7" to "deleted"),
        )

        assertNull(checklistForWidget(state, 7))
    }

    @Test
    fun `deleting a bound checklist removes the binding and falls back to the pin`() {
        val bound = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        val deleted = ChecklistStore.deleteChecklist(bound, "todo")

        assertNull(deleted.widgetBindings["7"])
        assertEquals("groceries", checklistForWidget(deleted, 7)?.id)
    }

    @Test
    fun `no binding and no pin returns null`() {
        val state = stateOf(list("groceries"), list("todo"))

        assertNull(checklistForWidget(state, 7))
    }

    @Test
    fun `binding survives a checklist rename`() {
        val bound = bindWidget(stateOf(list("todo")), 7, "todo")

        val renamed = ChecklistStore.renameChecklist(bound, "todo", "Errands", 5L)

        assertEquals("Errands", checklistForWidget(renamed, 7)?.title)
    }

    @Test
    fun `deleting a checklist removes every binding that points at it`() {
        val bound = bindWidget(
            bindWidget(
                bindWidget(stateOf(list("groceries"), list("todo")), 1, "todo"),
                2,
                "todo",
            ),
            3,
            "groceries",
        )

        val deleted = ChecklistStore.deleteChecklist(bound, "todo")

        assertEquals(mapOf("3" to "groceries"), deleted.widgetBindings)
    }

    @Test
    fun `bind ignores an unknown checklist`() {
        val state = stateOf(list("groceries"))

        assertSame(state, bindWidget(state, 7, "missing"))
    }

    @Test
    fun `bind replaces a previous binding for the same widget`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "groceries"),
            7,
            "todo",
        )

        assertEquals(mapOf("7" to "todo"), state.widgetBindings)
    }

    @Test
    fun `unbind removes only the target widget`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "groceries"),
            8,
            "todo",
        )

        val next = unbindWidget(state, 7)

        assertEquals(mapOf("8" to "todo"), next.widgetBindings)
    }

    @Test
    fun `two widgets can show the same checklist independently`() {
        val state = bindWidget(
            bindWidget(stateOf(list("groceries"), list("todo")), 7, "todo"),
            8,
            "todo",
        )

        assertEquals("todo", checklistForWidget(state, 7)?.id)
        assertEquals("todo", checklistForWidget(state, 8)?.id)

        val changed = bindWidget(state, 8, "groceries")

        assertEquals("todo", checklistForWidget(changed, 7)?.id)
        assertEquals("groceries", checklistForWidget(changed, 8)?.id)
    }

    @Test
    fun `binding one widget leaves another unbound and pinned`() {
        val state = bindWidget(stateOf(list("groceries", pinned = true), list("todo")), 7, "todo")

        assertEquals("groceries", checklistForWidget(state, 8)?.id)
    }
}
```

Add these two tests to `app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt` (after `round trip preserves state`):

```kotlin
    @Test
    fun `widget bindings survive a json round trip`() {
        val state = AppState(widgetBindings = mapOf("7" to "c1"))

        assertEquals(state, ChecklistJson.decode(ChecklistJson.encode(state)))
    }

    @Test
    fun `old json without widget bindings decodes to an empty map`() {
        val decoded = ChecklistJson.tryDecode(
            """{"version":1,"checklists":[]}""",
        )

        assertNotNull(decoded)
        val state = decoded!!
        assertEquals(1, state.version)
        assertTrue(state.widgetBindings.isEmpty())
    }
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.data.model.AppStateJsonTest'
```

Expected: compilation fails with unresolved references to `widgetBindings`, `bindWidget`, `unbindWidget`, and `checklistForWidget`. That is the failing-test state for this task.

- [ ] **Step 3: Add the `widgetBindings` field**

Change `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt` to:

```kotlin
@Serializable
data class AppState(
    val version: Int = 1,
    val checklists: List<Checklist> = emptyList(),
    val widgetBindings: Map<String, String> = emptyMap(),
)
```

Do not change `version`. Do not add a custom serializer; the default-value field is enough for old JSON.

- [ ] **Step 4: Add the pure helpers**

Create `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist

/**
 * Binds one app widget to an existing checklist. Unknown checklist IDs are a
 * no-op, and a new binding replaces any previous binding for the same widget.
 */
fun bindWidget(state: AppState, appWidgetId: Int, checklistId: String): AppState {
    if (state.checklists.none { it.id == checklistId }) return state
    val key = appWidgetId.toString()
    if (state.widgetBindings[key] == checklistId) return state
    return state.copy(widgetBindings = state.widgetBindings + (key to checklistId))
}

/** Removes the binding for one app widget; other widgets are untouched. */
fun unbindWidget(state: AppState, appWidgetId: Int): AppState {
    val key = appWidgetId.toString()
    if (!state.widgetBindings.containsKey(key)) return state
    return state.copy(widgetBindings = state.widgetBindings - key)
}

/**
 * Resolves the checklist one widget should show. A binding always wins, even
 * when it points at a deleted checklist (then the result is null and the widget
 * shows its unbound state). Only unbound widgets fall back to the pin.
 */
fun checklistForWidget(state: AppState, appWidgetId: Int): Checklist? {
    val key = appWidgetId.toString()
    if (state.widgetBindings.containsKey(key)) {
        val boundId = state.widgetBindings[key]
        return state.checklists.firstOrNull { it.id == boundId }
    }
    return state.checklists.firstOrNull { it.isPinned }
}
```

- [ ] **Step 5: Remove bindings when a checklist is deleted**

Change `deleteChecklist` in `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt` to:

```kotlin
    fun deleteChecklist(state: AppState, id: String): AppState =
        state.copy(
            checklists = state.checklists.filterNot { it.id == id },
            widgetBindings = state.widgetBindings.filterValues { it != id },
        )
```

- [ ] **Step 6: Run the tests and confirm they pass**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.data.model.AppStateJsonTest'
```

Expected: all tests in both classes pass. Then run the full suite once to catch regressions from the new field:

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest
```

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt \
  app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt \
  app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt
git commit -m "feat(data): add per-widget checklist bindings"
```

---

### Task 2: Expose binding operations on the repository

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt`

**Interfaces:**
- Consumes: `bindWidget`, `unbindWidget`, `ChecklistStore.deleteChecklist`, `StatePersistence.update`.
- Produces:
  - `suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String)` on `ChecklistRepository`.
  - `suspend fun removeWidgetBinding(appWidgetId: Int)` on `ChecklistRepository`.
  - `DefaultChecklistRepository` implementations that use the existing `mutate` path.

- [ ] **Step 1: Write the failing repository tests**

Add these tests to `app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt` (after `pin replaces the previous pin`):

```kotlin
    @Test
    fun `widget binding is persisted`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setWidgetBinding(42, "c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("42" to "c2"), state.widgetBindings)
    }

    @Test
    fun `widget binding removal is persisted`() = runTest {
        val initial = initialState().copy(
            widgetBindings = mapOf("42" to "c2", "43" to "c1"),
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.removeWidgetBinding(42)

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("43" to "c1"), state.widgetBindings)
    }

    @Test
    fun `deleting a checklist removes its persisted widget bindings`() = runTest {
        val initial = initialState().copy(
            widgetBindings = mapOf("42" to "c2", "43" to "c1"),
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.deleteChecklist("c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals(mapOf("43" to "c1"), state.widgetBindings)
    }
```

Also add the new overrides to both fakes in `app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt`. In `ThrowingRepository`, after `setPinned`:

```kotlin
        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) =
            throw IOException("fail")

        override suspend fun removeWidgetBinding(appWidgetId: Int) =
            throw IOException("fail")
```

In `NoOpRepository`, after `setPinned`:

```kotlin
        override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) = Unit

        override suspend fun removeWidgetBinding(appWidgetId: Int) = Unit
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.DefaultChecklistRepositoryTest' --tests 'dev.abhiram.checklist.ui.ChecklistViewModelTest'
```

Expected: compilation fails because `setWidgetBinding` and `removeWidgetBinding` are not members of `ChecklistRepository`.

- [ ] **Step 3: Add the interface methods**

Change `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt` to:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import kotlinx.coroutines.flow.StateFlow

interface ChecklistRepository {
    val state: StateFlow<AppState>
    suspend fun snapshot(): AppState
    suspend fun createChecklist(title: String): String
    suspend fun renameChecklist(id: String, title: String)
    suspend fun deleteChecklist(id: String)
    suspend fun setPinned(id: String?)
    suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String)
    suspend fun removeWidgetBinding(appWidgetId: Int)
    suspend fun addItem(checklistId: String, text: String): String
    suspend fun updateItem(itemId: String, text: String)
    suspend fun toggleItem(itemId: String)
    suspend fun deleteItem(itemId: String)
}
```

- [ ] **Step 4: Implement the repository operations**

Add to `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt` after `setPinned`:

```kotlin
    override suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String) {
        mutate { bindWidget(it, appWidgetId, checklistId) }
    }

    override suspend fun removeWidgetBinding(appWidgetId: Int) {
        mutate { unbindWidget(it, appWidgetId) }
    }
```

Both helpers are in the same `dev.abhiram.checklist.data` package, so no new imports are needed. `deleteChecklist` already calls `ChecklistStore.deleteChecklist`, which now removes bindings from Task 1.

- [ ] **Step 5: Run the tests and confirm they pass**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.DefaultChecklistRepositoryTest' --tests 'dev.abhiram.checklist.ui.ChecklistViewModelTest'
```

Expected: both classes pass, including the existing ViewModel success/error tests with the updated fakes.

- [ ] **Step 6: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt \
  app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt \
  app/src/test/kotlin/dev/abhiram/checklist/ui/ChecklistViewModelTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt
git commit -m "feat(data): expose widget binding repository operations"
```

---

### Task 3: Render and migrate each widget independently

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigrationTest.kt`

**Interfaces:**
- Consumes: `ChecklistRepository.snapshot()`, `ChecklistRepository.setWidgetBinding`, `ChecklistRepository.removeWidgetBinding`, `checklistForWidget`, `AppState`.
- Produces:
  - `fun bindingMigrationTarget(state: AppState, appWidgetId: Int): String?`
  - `suspend fun migrateWidgetBinding(state: AppState, appWidgetId: Int, setBinding: suspend (Int, String) -> Unit): AppState`
  - `ChecklistWidget.onDelete(context: Context, glanceId: GlanceId)` removes the widget's binding.
  - `ChecklistWidget.provideGlance` resolves its own `appWidgetId` and checklist.

- [ ] **Step 1: Write the failing migration tests**

Add this test to `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt` (after `binding one widget leaves another unbound and pinned`):

```kotlin
    @Test
    fun `migration target is the pin only while the widget is unbound`() {
        val initial = stateOf(list("groceries", pinned = true), list("todo"))

        assertEquals("groceries", bindingMigrationTarget(initial, 7))
        assertNull(bindingMigrationTarget(bindWidget(initial, 7, "todo"), 7))
        assertNull(bindingMigrationTarget(stateOf(list("groceries")), 7))
    }
```

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigrationTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBindingMigrationTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `unbound widget is bound to the pinned checklist`() = runTest {
        val state = AppState(checklists = listOf(list("groceries", pinned = true), list("todo")))
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertEquals(listOf(7 to "groceries"), writes)
        assertEquals("groceries", migrated.widgetBindings["7"])
    }

    @Test
    fun `existing binding is never overwritten by the pin`() = runTest {
        val state = AppState(
            checklists = listOf(list("groceries", pinned = true), list("todo")),
            widgetBindings = mapOf("7" to "todo"),
        )
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertTrue(writes.isEmpty())
        assertEquals(mapOf("7" to "todo"), migrated.widgetBindings)
    }

    @Test
    fun `unbound widget without a pin is left unbound`() = runTest {
        val state = AppState(checklists = listOf(list("groceries"), list("todo")))
        val writes = mutableListOf<Pair<Int, String>>()

        val migrated = migrateWidgetBinding(state, 7) { widgetId, checklistId ->
            writes += widgetId to checklistId
        }

        assertTrue(writes.isEmpty())
        assertNull(migrated.widgetBindings["7"])
    }

    @Test
    fun `migration targets only the widget being rendered`() = runTest {
        val state = AppState(checklists = listOf(list("groceries", pinned = true)))
        val writes = mutableListOf<Int>()

        migrateWidgetBinding(state, 7) { widgetId, _ -> writes += widgetId }

        assertEquals(listOf(7), writes)
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.widget.WidgetBindingMigrationTest'
```

Expected: compilation fails because `bindingMigrationTarget` and `migrateWidgetBinding` do not exist.

- [ ] **Step 3: Add the pure migration target helper**

Append to `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`:

```kotlin
/**
 * Returns the pinned checklist ID when this widget has no binding yet, or null
 * when it is already bound or nothing is pinned. Used for one-time legacy
 * migration; it never overrides an existing binding.
 */
fun bindingMigrationTarget(state: AppState, appWidgetId: Int): String? {
    if (state.widgetBindings.containsKey(appWidgetId.toString())) return null
    return state.checklists.firstOrNull { it.isPinned }?.id
}
```

- [ ] **Step 4: Add the suspend migration helper**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.bindingMigrationTarget
import dev.abhiram.checklist.data.model.AppState

/**
 * Persists a legacy unconfigured widget's fallback checklist as a real binding.
 * The returned state is used as the first composition value so the first frame
 * already resolves the binding instead of briefly following the pin.
 */
suspend fun migrateWidgetBinding(
    state: AppState,
    appWidgetId: Int,
    setBinding: suspend (Int, String) -> Unit,
): AppState {
    val checklistId = bindingMigrationTarget(state, appWidgetId) ?: return state
    setBinding(appWidgetId, checklistId)
    return state.copy(
        widgetBindings = state.widgetBindings + (appWidgetId.toString() to checklistId),
    )
}
```

- [ ] **Step 5: Resolve the widget's own checklist and clean up on delete**

In `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt`, add these imports:

```kotlin
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.abhiram.checklist.data.checklistForWidget
```

Replace `provideGlance` with:

```kotlin
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialState = try {
            repository.snapshot()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("ChecklistWidget", "Could not read widget state", error)
            AppState()
        }
        val initial = try {
            migrateWidgetBinding(
                state = initialState,
                appWidgetId = appWidgetId,
                setBinding = { widgetId, checklistId ->
                    repository.setWidgetBinding(widgetId, checklistId)
                },
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("ChecklistWidget", "Could not migrate widget binding", error)
            initialState
        }
        provideContent {
            // Glance does not restart provideGlance while a widget session is alive,
            // so the repository must be observed inside the composition. Otherwise
            // updates recompose against the list captured on the first render.
            val state by repository.state.collectAsState(initial = initial)
            val checklist = checklistForWidget(state, appWidgetId)
            val openIntent = remember(checklist?.id) {
                Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    putExtra(EXTRA_FROM_WIDGET, true)
                    checklist?.let { putExtra(EXTRA_OPEN_CHECKLIST, it.id) }
                }
            }
            WidgetContent(checklist = checklist, openIntent = openIntent)
        }
    }

    override suspend fun onDelete(context: Context, glanceId: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        try {
            val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
            repository.removeWidgetBinding(appWidgetId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("ChecklistWidget", "Could not remove widget binding", error)
        }
        super.onDelete(context, glanceId)
    }
```

Do not change `WidgetContent`, `WidgetHeader`, `WidgetDivider`, `WidgetItemRow`, `ToggleItemAction`, or the companion-object extras. They already provide the open intent, scrollable list, row toggles, adaptive compact header, and dark palette. The only behavioral change in the composition is that `checklist` comes from `checklistForWidget(state, appWidgetId)` instead of the first pinned checklist.

If a host fails to report deletion, the stale entry is harmless: app-widget IDs are reused, and the next configuration overwrites the entry.

- [ ] **Step 6: Run the tests and confirm they pass**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.widget.WidgetBindingMigrationTest'
```

Expected: both classes pass. Then compile the app to catch Glance API or import mistakes:

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
```

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt \
  app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigrationTest.kt
git commit -m "feat(widget): bind and migrate each widget independently"
```

---

### Task 4: Add the configuration activity and picker

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetPicker.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetConfigBinding.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetConfigActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetPickerTest.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetConfigBindingTest.kt`

**Interfaces:**
- Consumes: `ChecklistRepository`, `ChecklistWidget`, `Checklist`, `ChecklistTheme`, `R.string.*`.
- Produces:
  - `fun unboundWidgetIds(bindings: Map<String, String>, appWidgetIds: List<Int>, excludingAppWidgetId: Int? = null): List<Int>`
  - `suspend fun migrateUnboundWidgets(context: Context, repository: ChecklistRepository, excludingAppWidgetId: Int? = null)`
  - `fun pickerChecklists(checklists: List<Checklist>): List<Checklist>`
  - `fun pickerProgress(checklist: Checklist): String`
  - `suspend fun bindSelectedChecklist(checklistId: String?, setBinding: suspend (String) -> Unit, refresh: suspend () -> Unit): Boolean`
  - `class ChecklistWidgetConfigActivity : ComponentActivity`

- [ ] **Step 1: Write the failing picker, migration-plan, and cancel-safety tests**

Add this test to `app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt`:

```kotlin
    @Test
    fun `migration plan skips bound widgets and the widget being configured`() {
        val bindings = mapOf("7" to "groceries")

        val targets = unboundWidgetIds(
            bindings = bindings,
            appWidgetIds = listOf(7, 8, 9),
            excludingAppWidgetId = 9,
        )

        assertEquals(listOf(8), targets)
    }
```

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetPickerTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPickerTest {

    private fun list(
        id: String,
        pinned: Boolean = false,
        items: List<ChecklistItem> = emptyList(),
    ) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
        items = items,
    )

    private fun item(done: Boolean, position: Int) = ChecklistItem(
        id = "i$position",
        text = "Item $position",
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    @Test
    fun `pinned checklist is first and every checklist is kept`() {
        val ordered = pickerChecklists(
            listOf(list("todo"), list("groceries", pinned = true), list("work")),
        )

        assertEquals(listOf("groceries", "todo", "work"), ordered.map { it.id })
    }

    @Test
    fun `progress counts done items over all items`() {
        val checklist = list(
            "groceries",
            items = listOf(
                item(done = true, position = 0),
                item(done = false, position = 1),
                item(done = true, position = 2),
            ),
        )

        assertEquals("2/3", pickerProgress(checklist))
    }

    @Test
    fun `empty checklist shows zero progress`() {
        assertEquals("0/0", pickerProgress(list("groceries")))
    }
}
```

Create `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetConfigBindingTest.kt`:

```kotlin
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

        val bound = bindSelectedChecklist(
            checklistId = "todo",
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertTrue(bound)
        assertEquals(listOf("bind:todo", "refresh"), calls)
    }

    @Test
    fun `cancel or empty state writes nothing`() = runTest {
        val calls = mutableListOf<String>()

        val bound = bindSelectedChecklist(
            checklistId = null,
            setBinding = { calls += "bind:$it" },
            refresh = { calls += "refresh" },
        )

        assertFalse(bound)
        assertTrue(calls.isEmpty())
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.widget.WidgetPickerTest' --tests 'dev.abhiram.checklist.widget.WidgetConfigBindingTest'
```

Expected: compilation fails because `unboundWidgetIds`, `pickerChecklists`, `pickerProgress`, and `bindSelectedChecklist` do not exist.

- [ ] **Step 3: Add the pure migration-plan helper**

Append to `app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt`:

```kotlin
/**
 * Returns the currently placed widget IDs that have no binding yet. The widget
 * being configured is excluded so cancelling the picker leaves it untouched.
 */
fun unboundWidgetIds(
    bindings: Map<String, String>,
    appWidgetIds: List<Int>,
    excludingAppWidgetId: Int? = null,
): List<Int> = appWidgetIds.filter { appWidgetId ->
    appWidgetId != excludingAppWidgetId &&
        !bindings.containsKey(appWidgetId.toString())
}
```

- [ ] **Step 4: Add the picker ordering and progress helpers**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetPicker.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist

/** Pinned checklist first, every other checklist in its existing order. */
fun pickerChecklists(checklists: List<Checklist>): List<Checklist> =
    checklists.sortedByDescending { it.isPinned }

/** Compact progress label used by the widget picker, for example "2/5". */
fun pickerProgress(checklist: Checklist): String {
    val done = checklist.items.count { it.isDone }
    return "$done/${checklist.items.size}"
}
```

- [ ] **Step 5: Add the cancel-safe selection helper**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetConfigBinding.kt`:

```kotlin
package dev.abhiram.checklist.widget

/**
 * Persists the picker selection and refreshes only the configured widget.
 * A null selection (back/cancel or the empty state) writes nothing.
 */
suspend fun bindSelectedChecklist(
    checklistId: String?,
    setBinding: suspend (String) -> Unit,
    refresh: suspend () -> Unit,
): Boolean {
    if (checklistId == null) return false
    setBinding(checklistId)
    refresh()
    return true
}
```

- [ ] **Step 6: Add bulk legacy migration for existing widgets**

Append to `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt`:

```kotlin
/**
 * Binds every existing unconfigured widget to the pinned checklist before the
 * picker can change the pin. The widget currently being configured is skipped
 * so cancelling the picker leaves it untouched.
 */
suspend fun migrateUnboundWidgets(
    context: Context,
    repository: ChecklistRepository,
    excludingAppWidgetId: Int? = null,
) {
    val state = repository.snapshot()
    val pinnedId = state.checklists.firstOrNull { it.isPinned }?.id ?: return
    val manager = GlanceAppWidgetManager(context)
    val appWidgetIds = manager.getGlanceIds(ChecklistWidget::class.java)
        .map { manager.getAppWidgetId(it) }
    unboundWidgetIds(
        bindings = state.widgetBindings,
        appWidgetIds = appWidgetIds,
        excludingAppWidgetId = excludingAppWidgetId,
    ).forEach { appWidgetId ->
        repository.setWidgetBinding(appWidgetId, pinnedId)
    }
}
```

Update the imports at the top of that file to:

```kotlin
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.bindingMigrationTarget
import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.unboundWidgetIds
```

- [ ] **Step 7: Add the picker strings**

Change `app/src/main/res/values/strings.xml` to:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Checklist</string>
    <string name="widget_label">Checklist</string>
    <string name="widget_config_title">Choose a checklist</string>
    <string name="widget_config_pinned">Pinned checklist</string>
    <string name="widget_config_empty_title">No checklists yet</string>
    <string name="widget_config_empty_body">Open the app to create a checklist.</string>
    <string name="widget_config_open_app">Open app</string>
</resources>
```

- [ ] **Step 8: Add the configuration activity**

Create `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetConfigActivity.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ChecklistWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val configureIntent = intent ?: run {
            finish()
            return
        }
        appWidgetId = configureIntent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val repository = (application as ChecklistApplication).repository
        val glanceId = try {
            GlanceAppWidgetManager(this).getGlanceIdBy(configureIntent)
        } catch (error: Exception) {
            Log.e(TAG, "Could not resolve the widget", error)
            finish()
            return
        }
        if (glanceId == null) {
            Log.e(TAG, "Could not resolve the widget")
            finish()
            return
        }

        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var migrationDone by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        try {
                            migrateUnboundWidgets(
                                context = this@ChecklistWidgetConfigActivity,
                                repository = repository,
                                excludingAppWidgetId = appWidgetId,
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.e(TAG, "Could not migrate legacy widget bindings", error)
                        }
                        migrationDone = true
                    }

                    if (migrationDone) {
                        val state by repository.state.collectAsStateWithLifecycle()
                        WidgetPicker(
                            checklists = state.checklists,
                            onSelect = { checklist ->
                                lifecycleScope.launch {
                                    try {
                                        val bound = bindSelectedChecklist(
                                            checklistId = checklist.id,
                                            setBinding = { id ->
                                                repository.setWidgetBinding(appWidgetId, id)
                                            },
                                            refresh = {
                                                ChecklistWidget().update(
                                                    this@ChecklistWidgetConfigActivity,
                                                    glanceId,
                                                )
                                            },
                                        )
                                        if (bound) finishWithResult()
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        Log.e(TAG, "Could not bind widget to checklist", error)
                                    }
                                }
                            },
                            onOpenApp = {
                                startActivity(
                                    Intent(
                                        this@ChecklistWidgetConfigActivity,
                                        MainActivity::class.java,
                                    ),
                                )
                                finish()
                            },
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    private fun finishWithResult() {
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }

    private companion object {
        const val TAG = "ChecklistWidgetConfig"
    }
}

@Composable
private fun WidgetPicker(
    checklists: List<Checklist>,
    onSelect: (Checklist) -> Unit,
    onOpenApp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (checklists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.widget_config_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.widget_config_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onOpenApp) {
                        Text(stringResource(R.string.widget_config_open_app))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(pickerChecklists(checklists), key = { it.id }) { checklist ->
                        PickerRow(checklist = checklist, onClick = { onSelect(checklist) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(checklist: Checklist, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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
                            contentDescription = stringResource(R.string.widget_config_pinned),
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
                    text = pickerProgress(checklist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

Behavioral notes for the implementation:
- `setResult(RESULT_CANCELED)` at the start makes Back cancel both add and reconfigure.
- The target widget is excluded from `migrateUnboundWidgets`, so cancelling does not bind it.
- The empty state has no selection and calls no binding write; its button opens the app and finishes the canceled configuration.
- `getGlanceIdBy(configureIntent)` is the Glance 1.2 overload that reads `EXTRA_APPWIDGET_ID`; `getAppWidgetId` is not used here.
- The activity uses the existing `ChecklistTheme`; no light-theme branch is introduced.

- [ ] **Step 9: Run the tests and confirm they pass**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.WidgetBindingsTest' --tests 'dev.abhiram.checklist.widget.WidgetPickerTest' --tests 'dev.abhiram.checklist.widget.WidgetConfigBindingTest'
```

Expected: all three classes pass. Then compile the app and check that the activity builds with the existing Compose/lifecycle dependencies:

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
```

- [ ] **Step 10: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/kotlin/dev/abhiram/checklist/data/WidgetBindings.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetBindingMigration.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetPicker.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetConfigBinding.kt \
  app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetConfigActivity.kt \
  app/src/main/res/values/strings.xml \
  app/src/test/kotlin/dev/abhiram/checklist/data/WidgetBindingsTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetPickerTest.kt \
  app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetConfigBindingTest.kt
git commit -m "feat(widget): add checklist picker configuration activity"
```

---

### Task 5: Register the provider configuration and reconfiguration entry point

**Files:**
- Modify: `app/src/main/res/xml/checklist_widget_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `dev.abhiram.checklist.widget.ChecklistWidgetConfigActivity` from Task 4.
- Produces: the launcher launches the picker when a widget is added, and offers reconfiguration on Android 12+.

- [ ] **Step 1: Add the configure and reconfigurable attributes**

Change `app/src/main/res/xml/checklist_widget_info.xml` to:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:configure="dev.abhiram.checklist.widget.ChecklistWidgetConfigActivity"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="250dp"
    android:minHeight="250dp"
    android:minResizeWidth="110dp"
    android:minResizeHeight="110dp"
    android:targetCellWidth="4"
    android:targetCellHeight="4"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable" />
```

- [ ] **Step 2: Declare the activity in the manifest**

Add this activity to `app/src/main/AndroidManifest.xml` after `MainActivity` and before the receiver:

```xml
        <activity
            android:name=".widget.ChecklistWidgetConfigActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:theme="@style/Theme.Checklist">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>
```

The full activity block should read:

```xml
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".widget.ChecklistWidgetConfigActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:theme="@style/Theme.Checklist">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".widget.ChecklistWidgetReceiver"
            android:exported="true"
            android:label="@string/widget_label">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/checklist_widget_info" />
        </receiver>
```

`android:exported="true"` is required because the launcher starts the configuration activity. The `APPWIDGET_CONFIGURE` filter is the standard action the launcher uses to resolve a configuration activity; the provider's `android:configure` attribute supplies the explicit component. `android:excludeFromRecents="true"` keeps the picker out of the recent-apps list. The existing app theme is already dark; the explicit theme keeps the activity consistent even if the application theme changes later.

- [ ] **Step 3: Build and inspect the merged manifest**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
find app/build/intermediates -name AndroidManifest.xml -exec grep -l 'ChecklistWidgetConfigActivity' {} \;
grep -n 'android:configure\|android:widgetFeatures' app/src/main/res/xml/checklist_widget_info.xml
```

Expected: the build succeeds; `find` prints at least one merged manifest containing `ChecklistWidgetConfigActivity`; `grep` prints both the `android:configure` and `android:widgetFeatures` lines.

- [ ] **Step 4: Commit**

```bash
cd /home/abhiram/projects/widgets
git add app/src/main/res/xml/checklist_widget_info.xml app/src/main/AndroidManifest.xml
git commit -m "feat(widget): register reconfigurable widget provider"
```

---

### Task 6: Update the README for per-widget selection

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the behavior implemented in Tasks 1–5.
- Produces: user-facing documentation for the picker, arbitrary widget count, Pin as default/fallback, reconfiguration, and legacy migration.

- [ ] **Step 1: Replace the introduction and feature bullets**

Change the opening paragraph of `README.md` to:

```markdown
A local-only Android checklist app with home-screen widgets. Each widget shows
the checklist you choose for it and lets you tick items off without opening the
app. Pin marks the default/fallback checklist for legacy or unconfigured widgets.
```

Replace the first two feature bullets with:

```markdown
- Multiple checklists with create, rename, delete, and pin
- Any number of home-screen widgets, each bound to its own checklist
- Adding a widget opens a picker with every checklist; the pinned checklist is
  listed first with a pin badge
- Pin is only a default/fallback for unconfigured widgets; it never filters the
  picker and never overrides a widget's binding
- Reconfigure an existing widget on Android 12+ without removing it
- One pinned checklist at a time; pinning another unpins the previous one
```

Keep the remaining feature bullets about items, widget ordering, resizing, dark
theme, scrolling, row toggles, local-only storage, and low-resource design.

- [ ] **Step 2: Replace the “Add the widget” section**

Replace the current four-step section with:

```markdown
## Add the widget

1. Long-press the home screen and open **Widgets**.
2. Find **Checklist** and drag it onto the home screen.
3. In **Choose a checklist**, pick any checklist. The pinned checklist appears
   first with a pin badge.
4. Repeat for as many widgets as you want; each keeps its own checklist.
5. To change one later on Android 12+, long-press the widget and choose
   **Edit** or **Reconfigure**.

You no longer need to pin a checklist before adding a widget. A widget that
predates per-widget binding is bound to the pinned checklist on first render,
so the existing Groceries widget keeps showing Groceries after the upgrade.
```

- [ ] **Step 3: Update the build/test commands and test description**

In the **Build** and **Tests** sections, add the PATH export after the JDK
export:

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
```

Replace the test-count sentence with:

```markdown
The suite covers the JSON codec, store rules, repository persistence, corrupt
state recovery, per-widget binding and migration, picker ordering and progress,
widget row layout, widget toggle refresh, and ViewModel mutation success/error
handling.
```

- [ ] **Step 4: Update the architecture list**

Replace the architecture bullets with:

```markdown
- `data/model` — serializable `AppState`, `Checklist`, `ChecklistItem`
- `data/ChecklistStore` — pure state transformations
- `data/WidgetBindings` — pure per-widget binding, resolution, and migration planning
- `data/DefaultChecklistRepository` — atomic DataStore updates
- `ui` — Compose screens and `ChecklistViewModel`
- `widget` — Glance renderer, picker configuration activity, migration, row math,
  and toggle action callback
- `ChecklistApplication` — manual DI container and lazy WorkManager
  configuration; the ViewModel triggers widget refreshes after mutations
```

- [ ] **Step 5: Add the new limitations**

Add these bullets to the **Limitations** section:

```markdown
- Widget bindings use launcher-assigned app-widget IDs; clearing app data or
  restoring data to a different launcher setup resets them
- Reconfiguration requires Android 12+; on older launchers, remove and re-add
  the widget to choose a different checklist
```

Keep the existing limitations about no cloud sync, no widget text entry,
two-line widget text, launcher-dependent scrolling, and the local-only backup
qualification.

- [ ] **Step 6: Review the README for contradictions**

Search for stale statements that every widget shows the pinned checklist:

```bash
cd /home/abhiram/projects/widgets
grep -n -i 'pinned checklist\|one pinned\|widget shows' README.md
```

Expected: the remaining matches describe Pin as the default/fallback or describe
the picker order; no match says every widget always shows the pinned checklist.
Update any stale sentence you find.

- [ ] **Step 7: Commit**

```bash
cd /home/abhiram/projects/widgets
git add README.md
git commit -m "docs: describe per-widget checklist selection"
```

---

### Task 7: Verify, document, and publish the repository

**Files:**
- Create: `docs/superpowers/handoffs/2026-10-31-per-widget-checklist-binding-handoff.md`
- No source changes in this task.

**Interfaces:**
- Consumes: all implementation and documentation from Tasks 1–6.
- Produces: a verified release build on `GAKNWKGQJJJ7QWXW`, a handoff with real evidence, and a private GitHub repository containing every commit.

- [ ] **Step 1: Run the full unit suite from a clean test task**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:testDebugUnitTest --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`, zero failures, zero errors. Record the total test
count from the generated reports:

```bash
grep -h '<testsuite ' app/build/test-results/testDebugUnitTest/*.xml
```

- [ ] **Step 2: Build both variants**

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug :app:assembleRelease
```

Expected: both builds succeed. Record the release APK size and SHA-256:

```bash
ls -l app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

- [ ] **Step 3: Install the release build on the phone**

```bash
cd /home/abhiram/projects/widgets
adb -s GAKNWKGQJJJ7QWXW install -r app/build/outputs/apk/release/app-release.apk
adb -s GAKNWKGQJJJ7QWXW shell am start -n dev.abhiram.checklist/.MainActivity
```

Expected: `Success`, and the app opens to the Lists screen. If the launcher
removes widgets on reinstall, re-add them as described in the next step; the
legacy-migration check below assumes the existing Groceries widget survives the
update, which is the normal in-place install path.

- [ ] **Step 4: Verify the legacy Groceries migration**

Before installing this build, the phone has an existing widget showing
**Groceries** with **Groceries** pinned. After the in-place install, reboot once
so every Glance widget session restarts and runs the `provideGlance` migration:

```bash
adb -s GAKNWKGQJJJ7QWXW reboot
adb -s GAKNWKGQJJJ7QWXW wait-for-device
```

Unlock the phone and wait for the widgets to render, then:

1. Confirm the existing widget still shows **Groceries**.
2. In the app, pin **Todo** (create Todo first if it does not exist).
3. Return to the home screen and confirm the existing widget still shows
   **Groceries**, not Todo.

The second check proves the widget was migrated to a permanent binding rather
than following the new pin. This reboot also verifies that bindings and data
survive process death and device restart. If the launcher removed the widget
during install, add it back, choose Groceries, then repeat the pin change.

- [ ] **Step 5: Verify the picker, arbitrary widget count, and reconfiguration**

The launcher's widget picker is flaky when driven by adb gestures on this GSI;
perform these steps manually on the phone.

1. Add a **Todo** checklist in the app with at least two items.
2. Add a second Checklist widget. Confirm **Choose a checklist** lists every
   checklist, with the pinned checklist first and carrying the pin badge.
3. Choose **Todo**. Confirm the first widget still shows **Groceries** and the
   second widget shows **Todo**.
4. Add a third widget and choose either checklist. Confirm all three widgets
   render independently; this proves the feature is not limited to two widgets.
5. Long-press one widget and choose **Edit** or **Reconfigure**. Choose the
   other checklist. Confirm only that widget changes.
6. Add a fourth widget, then press Back in the picker. Confirm no widget is
   added. Reconfigure an existing widget and press Back; confirm its checklist
   is unchanged.

- [ ] **Step 6: Verify toggles, pin independence, deletion, and process death**

1. Toggle rows in each widget. Confirm the app reflects each change. Widgets
   bound to different checklists must not affect each other; two widgets bound
   to the same checklist intentionally share that checklist's item state.
2. Change the pin again in the app (for example back to **Groceries**). Confirm
   no bound widget changes its checklist.
3. Create a temporary checklist, bind a widget to it, then delete that checklist
   in the app. Confirm the widget falls back to the pinned checklist when one is
   pinned, or to the existing “Pin a checklist / Open the app to choose one”
   state when nothing is pinned. The app must not crash.
4. Return to the home screen so the app is backgrounded, kill the app process,
   then tap a widget row to start the process again and confirm the widgets
   still render and toggle. Reboot persistence was already checked in Step 4;
   this is the process-death check:
   ```bash
   adb -s GAKNWKGQJJJ7QWXW shell am kill dev.abhiram.checklist
   ```
5. Resize one widget to 2×2 and to a tall size. Confirm the list still scrolls
   and rows still toggle. If the launcher allows a height below 160 dp, confirm
   the compact one-line header; otherwise record the actual height and confirm
   the regular header. The widget must stay black in both system light and dark
   modes.

- [ ] **Step 7: Check logcat for crashes**

```bash
adb -s GAKNWKGQJJJ7QWXW logcat -d -s AndroidRuntime:E ChecklistWidget:E ChecklistWidgetConfig:E
```

Expected: no `FATAL EXCEPTION`, no uncaught widget or picker errors. Warnings
about a failed refresh are acceptable only if the corresponding action still
persisted and the widget recovered on the next update; record any occurrence.

- [ ] **Step 8: Create the private GitHub repository and push the implementation commits**

Confirm authentication and that no remote is configured:

```bash
gh auth status
git remote -v
```

Create the private repository and push:

```bash
cd /home/abhiram/projects/widgets
gh repo create checklist-app --private --source=. --remote=origin --push
```

Expected: `gh` creates `checklist-app` under `abhiram-k-2223`, adds `origin`,
and pushes the current branch. If the repository name already exists, do not
force-push; inspect it first:

```bash
gh repo view abhiram-k-2223/checklist-app --json name,visibility,url
```

If `origin` is missing, add it and push:

```bash
git remote add origin https://github.com/abhiram-k-2223/checklist-app.git
git push -u origin HEAD
```

If `origin` already exists but points elsewhere, update it and push:

```bash
git remote set-url origin https://github.com/abhiram-k-2223/checklist-app.git
git push -u origin HEAD
```

- [ ] **Step 9: Write the handoff with the real evidence and repository URL**

Create `docs/superpowers/handoffs/2026-10-31-per-widget-checklist-binding-handoff.md`
with these sections, using the actual values printed by the commands above:

- Header: date `2026-10-31`, repo `/home/abhiram/projects/widgets`, branch,
  implementation commit range, remote state, phone serial `GAKNWKGQJJJ7QWXW`.
- What changed: data model and helpers, repository operations, widget migration
  and `onDelete`, configuration activity, provider/manifest changes, README.
- Verification performed: the exact unit-test count, debug/release build
  results, release APK size and SHA-256, install result, and the outcome of
  every on-device check in Steps 4–7.
- Not verified / remaining risks: any check that could not be completed, plus
  the launcher-dependent reconfiguration and scrolling caveats.
- Follow-up: anything deferred, and the GitHub repository URL printed by
  `gh repo create` or `gh repo view`.

Do not write speculative pass/fail values. Record what the commands and the
phone actually showed.

- [ ] **Step 10: Commit and push the handoff**

```bash
cd /home/abhiram/projects/widgets
git add docs/superpowers/handoffs/2026-10-31-per-widget-checklist-binding-handoff.md
git commit -m "docs: record per-widget binding verification"
git push
```

- [ ] **Step 11: Confirm the published state**

```bash
cd /home/abhiram/projects/widgets
git remote -v
git status --short
git log --oneline --decorate -12
gh repo view abhiram-k-2223/checklist-app --json name,visibility,url
```

Expected: `origin` points at the private `checklist-app` repository, the working
tree is clean, all implementation and documentation commits are present, and
the remote branch matches local `HEAD`. Do not leave uncommitted changes.

---

## Completion Checklist

- [ ] `AppState.widgetBindings` exists with a default empty map; old JSON decodes.
- [ ] `bindWidget`, `unbindWidget`, and `checklistForWidget` implement the resolver semantics from the spec.
- [ ] `deleteChecklist` removes every binding that points at the deleted checklist.
- [ ] Repository bind/unbind operations persist through `mutate`; both test fakes implement them.
- [ ] `ChecklistWidget.provideGlance` uses its own app-widget ID, migrates legacy widgets once, and observes `repository.state`.
- [ ] `ChecklistWidget.onDelete` removes the widget's binding.
- [ ] `ChecklistWidgetConfigActivity` lists every checklist with the pinned one first and badged, writes only on selection, and cancels cleanly.
- [ ] Provider XML declares `android:configure` and `android:widgetFeatures="reconfigurable"`; the manifest declares the exported, recents-excluded activity.
- [ ] README and handoff describe the final behavior and contain real verification evidence.
- [ ] Full unit suite, debug build, release build, and on-device checks pass.
- [ ] Private `checklist-app` repository exists and contains every commit.
