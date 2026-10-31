# Per-Widget Checklist Binding — Design

**Date:** 2026-10-31
**Status:** Proposed
**Repo:** `/home/abhiram/projects/widgets`

## Problem

Every Checklist widget renders the one globally pinned checklist. The add-widget
flow has no configuration step, so adding a second widget for another checklist
is impossible: the new widget silently shows the pinned checklist again.

The user needs an arbitrary number of widgets, each tracking a different
checklist. Pin must not restrict which checklists a widget can show.

## Goals

- Any number of Checklist widgets can be placed, with no app-level limit.
- Adding a widget opens a picker containing **every** checklist.
- Each widget remembers its own checklist independently.
- An existing widget can be reconfigured later.
- The current Groceries widget keeps showing Groceries after the upgrade.
- The existing Pin control remains, but only as a default/fallback for legacy or
  unconfigured widgets. It never filters the picker and never overrides a
  widget's own binding.
- Widget taps and live refresh continue to work per widget.

## Non-goals

- Per-widget colors, sizes, or layouts beyond the existing adaptive sizing.
- Creating or editing checklists from the widget picker.
- Cloud sync or multi-device widget bindings.
- Removing the Pin feature from the app.

## Approach

Store the binding in the app-owned `AppState` rather than in Glance's
widget-scoped preferences.

Glance 1.2 has no `currentState` composable, and `update`/`updateAll` do not
restart `provideGlance` while a widget session is alive. The app already
observes `repository.state` inside the widget composition, so app-owned
bindings update reliably. Glance widget preferences would require relying on
internal store names or restarting sessions.

The standard Android app-widget configuration activity provides the picker.
The launcher launches it when a widget is added; `android:widgetFeatures=
"reconfigurable"` allows the same activity to edit an existing widget on
Android 12+.

## Data model

Add one additive field to `AppState`:

```kotlin
val widgetBindings: Map<String, String> = emptyMap()
```

- Key: `appWidgetId.toString()`.
- Value: checklist ID.
- Old JSON without the field decodes to an empty map.
- `version` remains `1`; the change is backward-compatible.
- `ChecklistJson` already ignores unknown keys and encodes defaults.

Pure helpers in `ChecklistStore` (or a small `WidgetBindings` helper):

```kotlin
fun bindWidget(state: AppState, appWidgetId: Int, checklistId: String): AppState
fun unbindWidget(state: AppState, appWidgetId: Int): AppState
fun checklistForWidget(state: AppState, appWidgetId: Int): Checklist?
```

`bindWidget` only stores an existing checklist ID. `checklistForWidget`
returns the bound checklist, otherwise the pinned checklist, otherwise null.
`deleteChecklist` removes every binding that points at the deleted checklist.

Repository additions:

```kotlin
suspend fun setWidgetBinding(appWidgetId: Int, checklistId: String)
suspend fun removeWidgetBinding(appWidgetId: Int)
```

## Widget rendering

`ChecklistWidget.provideGlance` obtains its `appWidgetId` through
`GlanceAppWidgetManager.getAppWidgetId(glanceId)`.

Before composing, if the widget has no binding and a pinned checklist exists,
persist a binding to the pinned checklist. This migrates the existing widget
once and prevents it from following later pin changes.

The composition observes `repository.state` and resolves the checklist through
`checklistForWidget(state, appWidgetId)`. Because the binding lives in observed
app state, reconfiguration recomposes the live Glance session correctly.

If the bound checklist no longer exists, show the normal unbound/empty state
rather than crashing. A stale binding may remain until the widget is
reconfigured or deleted.

Override `GlanceAppWidget.onDelete` to remove the widget's binding when Glance
reports deletion. Stale entries are harmless if a host fails to report deletion:
app-widget IDs are reused and the next configuration overwrites the entry.

## Configuration activity

Add `ChecklistWidgetConfigActivity`, a `ComponentActivity` using the existing
always-dark `ChecklistTheme`.

Flow:

1. Read `AppWidgetManager.EXTRA_APPWIDGET_ID`; finish if invalid.
2. Resolve the widget's `GlanceId` with
   `GlanceAppWidgetManager.getGlanceIdBy(intent)`.
3. Migrate any existing unbound widgets to the pinned checklist. This ensures
   the old Groceries widget is bound before the user can change the pin.
4. Observe all checklists from the repository and show a dark picker.
   - The pinned checklist is listed first with a small pin badge.
   - Every checklist is selectable; there is no pinned-only filter.
   - Each row shows the title and progress (`done/total`).
5. On selection, write the binding, call `ChecklistWidget().update(...)` for
   that widget, return `RESULT_OK` with `EXTRA_APPWIDGET_ID`, and finish.
6. Back cancels the add/reconfigure operation.

If there are no checklists, show a short empty state and a button that opens the
app. No binding is written.

Provider/manifest changes:

```xml
android:configure="dev.abhiram.checklist.widget.ChecklistWidgetConfigActivity"
android:widgetFeatures="reconfigurable"
```

The activity is exported, excluded from recents, and uses the existing theme.
No new dependency is needed.

## Pin behavior

- Pin remains available in the app UI; its meaning changes from "the checklist
  every widget shows" to "the default/fallback checklist."
- The picker shows the pinned checklist first and badges it.
- A widget with its own binding is never affected by pin changes.
- A legacy/unconfigured widget uses the pinned checklist as its fallback and is
  migrated to a permanent binding when possible.
- If no checklist is pinned, an unbound widget shows the existing
  "Pin a checklist / Open the app to choose one" state.

## Testing

Unit tests:

- Bound widget resolves its own checklist.
- Unbound widget falls back to the pinned checklist.
- Bound ID wins over a different pinned checklist.
- Missing/deleted bound checklist falls back safely.
- No binding and no pin returns null.
- Binding survives a checklist rename.
- Deleting a checklist removes its bindings.
- JSON round-trip preserves bindings; old JSON without the field decodes empty.
- Repository bind/unbind operations persist.

On-device:

- Add a Todo widget; the picker lists Groceries and Todo; choose Todo.
- Confirm Groceries widget still shows Groceries and Todo widget shows Todo.
- Add a third widget (same or other checklist); confirm all widgets are
  independent. This proves the design is not limited to two.
- Reconfigure one widget; only that widget changes.
- Toggle rows in each widget; each updates independently.
- Change the pinned checklist; bound widgets do not change.
- Delete a bound checklist; the widget shows a safe fallback and does not crash.
- Reboot and process death; bindings persist.
- Existing Groceries widget remains Groceries after installing the update.

## Acceptance criteria

- Adding a widget always presents every checklist.
- Any number of widgets can be placed.
- Each widget's checklist is independent of every other widget and of Pin.
- The existing Groceries widget keeps its checklist through the upgrade.
- Reconfiguration works without removing and re-adding the widget.
- All existing widget behavior (scroll, toggle, adaptive sizing, dark theme)
  still works.
