# Streak Tracker Design

**Date:** 2026-11-09
**Status:** Approved design; implementation plan pending
**Repo:** `/home/abhiram/projects/widgets`
**Related:** `docs/superpowers/specs/2026-10-31-per-widget-checklist-binding-design.md`

## 1. Purpose

Add a daily streak subsystem to the checklist app. A streak tracker is a named list of up to four daily tasks. Each task has its own color, a completion history, and a current streak. A new home-screen Streak widget shows a dark current-month calendar with colored completion dots and the tracker's checkable task list. The app shows the same tasks with their streak counters.

The existing checklist feature and checklist widget must continue to work unchanged.

## 2. Goals

- Support multiple named streak trackers.
- Allow up to four daily tasks per tracker, each with a unique color.
- Record completion history by local calendar date.
- Calculate and display a per-task current streak.
- Provide a dark Streak widget with a current-month calendar, colored dots, task toggles, and compact streak counts.
- Let each Streak widget bind to one tracker, with add-time selection and later reconfiguration.
- Preserve existing checklist data, behavior, and tests.

## 3. Non-goals

- No weekly or monthly goals, badges, reminders, or notifications.
- No best-streak metric in v1.
- No reordering of trackers or tasks.
- No calendar month navigation in the widget; it always shows the current month.
- No calendar in the app detail screen.
- No archived tasks. Deleting a task deletes its history.
- No cloud sync, accounts, or remote storage.
- No new dependencies, Room, KSP, Hilt, or navigation library.

## 4. Data model

Add to `AppState`:

```kotlin
val streakTrackers: List<StreakTracker> = emptyList(),
val streakWidgetBindings: Map<String, String> = emptyMap(),
```

`streakWidgetBindings` is keyed by `appWidgetId.toString()` and valued by tracker ID. It is separate from the existing checklist `widgetBindings` map.

New serializable models:

```kotlin
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
enum class StreakColor {
    BLUE,
    GREEN,
    ORANGE,
    PURPLE,
}
```

`completions` stores local ISO dates (`YYYY-MM-DD`), for example `"2026-11-09"`. Storing strings keeps the JSON independent of the device timezone and avoids a custom date serializer.

The four colors map to the existing iOS-dark palette:

| Color | Hex |
|---|---|
| BLUE | `#0A84FF` |
| GREEN | `#30D158` |
| ORANGE | `#FF9F0A` |
| PURPLE | `#BF5AF2` |

`AppState.version` stays `1`. Both new fields have defaults, so old JSON decodes with no streak trackers and no streak widget bindings. Existing checklist JSON is unchanged.

## 5. Store rules

All rules live in pure functions in a new `StreakStore` object, tested without Android or DataStore.

### Trackers

- `createStreakTracker(state, id, title, now)`: trims the title, rejects a blank title, and appends a tracker.
- `renameStreakTracker(state, id, title, now)`: trims, rejects blank, updates the title and timestamp; unknown ID is a no-op.
- `deleteStreakTracker(state, id)`: removes the tracker, all of its tasks, and every `streakWidgetBindings` entry whose value is that tracker ID.

### Tasks

- `addStreakTask(state, trackerId, taskId, text, color, now)`: trims text, rejects blank, rejects a tracker with four tasks, rejects a color already used by another task in that tracker, and appends with the next position.
- `updateStreakTask(state, taskId, text, color, now)`: trims text, rejects blank, rejects a color used by another task in the same tracker, and updates text/color/timestamp. The task's own current color is allowed.
- `deleteStreakTask(state, taskId)`: removes the task and its completion history. This intentionally removes its calendar dots.
- `toggleStreakTask(state, taskId, date, now)`: if the ISO date is present in `completions`, removes it; otherwise adds it. Multiple toggles on one date cannot create duplicate history. Unknown task ID is a no-op.

### Widget bindings

- `setStreakWidgetBinding(state, appWidgetId, trackerId)`: binds only if the tracker exists; replaces an existing binding for that widget.
- `removeStreakWidgetBinding(state, appWidgetId)`: removes only that widget's binding.
- `streakTrackerForWidget(state, appWidgetId)`: returns the bound tracker if it still exists; otherwise `null`. There is no pin or arbitrary fallback. An unbound or stale widget shows the unbound state.

## 6. Streak calculation

The current streak is per task and uses local calendar dates.

```kotlin
fun currentStreak(task: StreakTask, today: LocalDate): Int
```

Rules:

1. If today is completed, count backwards starting at today.
2. If today is not completed, count backwards starting at yesterday.
3. If the starting date is not completed, return `0`.
4. Count consecutive completed dates backwards until a date is missing.
5. Return the count.

Consequences:

- Today done, yesterday done, day before done -> `3`.
- Today not done, yesterday done, day before done -> `2`; the visible streak stays alive until midnight.
- Today done, yesterday missed -> `1`.
- Today not done, yesterday missed -> `0`.
- Unchecking today removes today and recalculates immediately.
- Multiple completions on one day count once because the history is a set.

The UI passes `LocalDate.now(ZoneId.systemDefault())`; pure tests pass fixed dates. No background data reset is needed: "today" is derived from the current local date, and old dates remain in history.

## 7. Calendar

A pure helper builds the widget calendar:

```kotlin
data class StreakCalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val dots: List<StreakColor>,
)

fun streakCalendarMonth(
    month: YearMonth,
    tasks: List<StreakTask>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<StreakCalendarDay>
```

- Always return exactly 42 cells (six weeks).
- Start at the first day of the week on or before the first of the month.
- `firstDayOfWeek` comes from `WeekFields.of(Locale.getDefault()).firstDayOfWeek`.
- For each cell, collect the color of every task whose `completions` contains that ISO date.
- Preserve task order (by `position`) when building dots.
- A day can have zero to four dots.
- `inCurrentMonth` is false for leading/trailing days; `isToday` is true only for the device-local today.

## 8. App UI

### Main screen

The existing Checklists screen gains a separate **Streaks** section below the checklist cards.

- Section header: `Streaks` and a `New streak` text button.
- Tracker card: title, up to four task color dots, and a summary such as `3 tasks · 2 done today`.
- Tapping a card opens the streak detail screen.
- `New streak` opens a title dialog. Saving creates the tracker, closes the dialog, and opens the new tracker's detail screen. Cancel writes nothing.
- The existing checklist FAB remains dedicated to creating checklists.
- Empty state: `No streak trackers yet` with the `New streak` action.
- Trackers display in creation order. No reordering in v1.

### Streak detail screen

- Dark screen with a back action and the tracker title.
- Overflow menu: `Rename tracker`, `Delete tracker`.
- Up to four task rows. Each row contains:
  - color dot;
  - task text;
  - today's checkbox;
  - current-streak label: `🔥 6 days`, `🔥 1 day`, or `No streak yet`.
- Tapping the checkbox toggles only today's completion.
- Each row has edit and delete actions.
- `Add task` is disabled at four tasks with a short `Maximum 4 tasks` explanation.
- Empty state: `Add up to 4 daily tasks`.

### Add/edit task dialog

- Text field with trimmed, non-blank validation.
- Four color swatches. Colors already used by another task in the tracker are disabled; when editing, the task's own color remains available.
- Save and Cancel actions.
- No color is selected by default when adding; the user must choose one.

### Delete confirmations

- Delete task: warns that its past calendar dots and streak history will be removed.
- Delete tracker: warns that all tasks and history will be removed, and that bound widgets will become unbound.

### Navigation

No bottom navigation is added. `ChecklistApp` coordinates three states:

1. checklist detail;
2. streak detail;
3. combined main screen.

Opening a checklist closes any open streak tracker, and opening a streak tracker closes any open checklist. Back returns to the main screen.

## 9. Streak widget

### Provider

- New `StreakWidget` Glance provider and `StreakWidgetReceiver`.
- New provider XML: `streak_widget_info.xml`.
- Initial layout: `@layout/glance_default_loading_layout`.
- Widget label: `Streak`.
- Default size **4x5**, resizable horizontally and vertically.
- Minimum width **250 dp**, minimum height **300 dp**.
- `minResizeWidth=250dp`, `minResizeHeight=300dp`.
- `targetCellWidth=4`, `targetCellHeight=5`.
- `updatePeriodMillis=0`; daily refresh is handled by WorkManager.
- `android:configure="dev.abhiram.checklist.widget.StreakWidgetConfigActivity"`.
- `android:widgetFeatures="reconfigurable"`.
- `widgetCategory="home_screen"`.
- Uses `SizeMode.Exact` and `LocalSize` to adapt to the widget's actual bounds.

### Visual design

- True-black background, 24 dp rounded corners, white task text, muted gray secondary text.
- 14 dp outer padding.
- Top row: tracker title, 15 sp semibold.
- Calendar header: current month and year, e.g. `November 2026`.
- Weekday labels in the device locale's order.
- Six-week month grid with seven columns.
- Day cell: date number and a fixed-height dot row.
- Today: subtle `#1C1C1E` rounded background and bright day number.
- Current-month days: white/light gray. Other-month days: muted `#48484A`.
- Completion dots: up to four 4 dp circles, one per completed task, in that task's color, with 2 dp spacing.
- Divider: `#2C2C2E`.
- Task list below the calendar: up to four rows.
- Task row: color dot, task text (one line, ellipsized), compact streak label, checkbox.
- Compact streak label: `🔥 6` when the streak is greater than zero; `—` when zero.
- Tapping a row toggles today's completion.
- The task list scrolls if the widget is shorter than its natural height, so all four tasks remain reachable.

Glance/RemoteViews child limits must be respected. Build the calendar as week rows inside a wrapping Column, and wrap header/calendar/task areas in their own Columns rather than putting every cell directly under the root.

### Unbound and empty states

- Bound tracker exists: render calendar and tasks.
- Bound tracker has no tasks: render the calendar with no dots and `Add daily tasks in the app`.
- Unbound widget or stale binding: `Choose a streak tracker` and a short instruction to reconfigure the widget.
- No trackers exist: `Create a streak tracker in the app`.
- The widget never guesses a tracker.

## 10. Widget configuration

New `StreakWidgetConfigActivity`:

1. Set `RESULT_CANCELED` immediately so Back cancels.
2. Read `AppWidgetManager.EXTRA_APPWIDGET_ID`; finish if invalid.
3. Resolve the `GlanceId` with `GlanceAppWidgetManager.getGlanceIdBy(intent)`; finish if unresolved.
4. Observe `repository.state` and list every streak tracker. No pinned concept and no filtering.
5. Each row shows the tracker title, its task color dots, and `N tasks`.
6. On selection, verify the tracker still exists, write `streakWidgetBindings`, refresh that widget, return `RESULT_OK` with `EXTRA_APPWIDGET_ID`, and finish.
7. Empty state: `No streak trackers yet`, `Open app`, and no binding write.
8. Back cancels without changing the binding.

Registration:

- Declare `StreakWidgetReceiver` as exported with `APPWIDGET_UPDATE` and the provider metadata.
- Declare `StreakWidgetConfigActivity` as exported, `excludeFromRecents="true"`, themed with `@style/Theme.Checklist`, and filtered for `android.appwidget.action.APPWIDGET_CONFIGURE`.

## 11. Daily rollover

Add `StreakRolloverWorker : CoroutineWorker`.

- On run: update all streak widgets, then schedule the next run.
- `scheduleStreakRollover(context)` computes the delay to the next local midnight (strictly after now) and enqueues unique work named `streak-widget-rollover` with `ExistingWorkPolicy.REPLACE`.
- Schedule from `ChecklistApplication.onCreate` and after each worker run.
- WorkManager may be delayed by the system; the widget still derives today's date on every render, so a late rollover only delays the automatic redraw, not the underlying history.
- No data reset occurs. Yesterday's completions remain in history.

## 12. Persistence and error handling

- All streak writes use the existing atomic `DefaultChecklistRepository.mutate` path and `ChecklistJson` encoding.
- Old JSON without streak fields loads with empty streak data.
- Deleting a tracker removes its widget bindings in the same atomic update.
- Repository failures surface through the app's existing error path.
- Widget toggle failures are logged, do not crash the launcher, and refresh only after a successful toggle.
- Config selection catches write/refresh failures, logs them, and finishes without reporting a false success.
- A missing tracker or task resolves to the unbound/empty state rather than crashing.

## 13. Testing

### Unit tests

- JSON round-trip with streak trackers and streak widget bindings.
- Old JSON without `streakTrackers`/`streakWidgetBindings` decodes to empty values.
- Existing checklist JSON and tests remain green.
- Tracker create/rename/delete rules, including blank titles and unknown IDs.
- Task add/update/delete rules: maximum four, unique colors, trimmed text, delete removes history.
- Toggle adds and removes a date; repeated toggles do not duplicate history.
- Deleting a tracker removes its bindings; unbinding one widget leaves others.
- Binding only an existing tracker; stale binding resolves to `null`.
- Streak calculation: today done; today not done/yesterday done; missed yesterday; today after missed yesterday = 1; uncheck today; zero; multi-day; month/year boundary.
- Calendar: exactly 42 cells; locale first day; current-month flags; today flag; dots per task; zero to four dots; months with different starting weekdays.
- Repository persistence and deletion cleanup.
- ViewModel: open/close, create/rename/delete, add/edit/delete task, toggle today, error path, widget refresh after mutations.
- Rollover delay calculation for before midnight, exactly midnight, and after midnight.
- Widget helpers: ordered task rows, compact streak label, tracker resolution, unbound/stale states.

### On-device checks

- Create a tracker with four colored tasks.
- Add a Streak widget; confirm the picker lists every tracker and choose one.
- Verify current-month calendar, today highlight, colored dots, and four task rows with streak labels.
- Toggle tasks in the widget; confirm dots, streak counts, and app state update.
- Add a second tracker and second widget; confirm independence.
- Reconfigure one widget; confirm only it changes. Cancel a reconfigure; confirm no change.
- Add/edit/delete tasks; confirm four-task limit, unique colors, and history deletion.
- Delete a bound tracker; confirm the widget becomes unbound and does not crash.
- Verify app streak counters against seeded multi-day history.
- Verify process death, reboot, resize/scroll, dark theme in system-light mode, and logcat.
- Rollover is unit-tested; if practical, verify on-device by advancing the device date and restoring it afterward.

## 14. Acceptance criteria

- Multiple named streak trackers exist, each with at most four uniquely colored daily tasks.
- Per-task current streak follows the approved rule and is visible in both the app and the widget.
- The Streak widget shows the current month, colored completion dots, and today's checkable task list.
- Add-time tracker selection, reconfiguration, deletion cleanup, and unbound states work.
- Existing checklist features and all existing tests remain green.
- Full unit suite, debug build, release build, and the on-device matrix pass before completion.

## 15. Constraints

- Kotlin + Jetpack Compose + Glance, compileSdk 37, minSdk 26, targetSdk 37.
- Existing versions: Kotlin 2.4.21, AGP 9.4.1, Compose BOM 2026.09.00, Glance 1.2.0, JDK 21.
- Always-dark app and widget theme.
- No new dependencies.
- No Room, KSP, Hilt, or navigation library.
- Preserve the existing checklist widget, checklist data, and per-widget checklist binding behavior.
