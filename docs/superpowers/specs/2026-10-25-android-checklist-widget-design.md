# Android Checklist App with Home-Screen Widget — Design

**Date:** 2026-10-25
**Status:** Approved in conversation (design sections 1–3)
**Working directory:** `/home/abhiram/projects/widgets`

## 1. Overview

A local-only Android checklist app and a companion home-screen widget. The user
creates named checklists, adds plain-text items, and pins exactly one checklist
to the widget. The widget shows the pinned checklist and lets the user toggle
items directly from the home screen without opening the app.

The widget is intentionally styled like an iOS dark-mode card: near-black
surfaces, hairline separators, rounded corners, and iOS blue accents — flat,
with no translucency, blur, gradients, or liquid-glass effects.

## 2. Goals

- Create, rename, and delete multiple checklists.
- Add, edit, toggle, and delete checklist items.
- Pin one checklist at a time; pinning another unpins the previous one.
- Widget shows the pinned checklist's title, progress, and items.
- Widget checkboxes toggle items in place, including when the app process is dead.
- Widget defaults to 4×4, resizes horizontally and vertically down to 2×2 and up
  to the launcher's maximum with no artificial cap.
- All data stays on the device and survives process death and reboot.
- Stay responsive and lightweight on a 4 GB RAM MediaTek phone: no polling,
  no background services, near-zero idle CPU, and a small release APK.
- Produce installable debug and release APKs and verify the release build on a
  physical phone over USB.

## 2.1 Low-end device budget

The target phone is a 4 GB RAM MediaTek device, so the app must stay usable
under memory pressure and avoid unnecessary wakeups.

- One Activity, one process, one app-scope coroutine collector. No services,
  alarms, scheduled WorkManager jobs, or polling. Glance's transitively bundled
  WorkManager is lazy-initialized only if an async widget request needs it; its
  startup initializer is disabled.
- Widget `updatePeriodMillis=0`. Refresh only after data changes or widget
  actions, debounced briefly and coalesced into one `updateAll`.
- No duplicate refresh from the widget action callback; the application
  observer owns refresh.
- Widget reads a repository snapshot inside `provideGlance`: one small
  DataStore read per update, never a loop.
- One JSON document. Writes happen only on explicit user actions, never per
  keystroke.
- Vector drawables only. No bitmaps, large images, or decorative animations.
- No Room, KSP, Hilt, or navigation library.
- Ship an R8-minified, resource-shrunk release APK for daily use. It is signed
  with the local debug key for sideloading, not for Play Store distribution.
- Verification: record PSS after opening both screens and confirm idle CPU is
  near 0%. Investigate if PSS exceeds about 150 MB or idle CPU stays above
  about 0.5%.

## 3. Non-goals (v1)

- Reminders/notifications, due dates, priorities, tags, folders.
- Drag-to-reorder items or lists.
- Rich text, images, attachments, audio.
- Multiple pinned checklists or multiple widget variants.
- Adding or editing item text from the widget.
- Backup/export/import, cloud sync, accounts.
- Release signing for the Play Store and Play Store packaging. The local
  R8 release APK is signed with the debug key only for sideloading.
- Tablet-specific layouts.
- App localization beyond English.

## 4. User experience

### 4.1 Lists screen

- App bar title: "Checklists".
- Each row shows the checklist title, progress (`done/total done`), and a small
  pin indicator on the pinned list.
- Tap a row to open that checklist.
- Overflow menu per list: Pin/Unpin, Rename, Delete.
- Floating action button creates a new checklist. A dialog asks for the title;
  blank titles are rejected.
- Empty state explains how to create the first list.
- Delete asks for confirmation and removes all of that list's items.
- Rename dialog starts with the current title; blank titles are rejected.

### 4.2 Checklist screen

- Top bar shows the checklist title and an overflow menu with Pin/Unpin, Rename,
  and Delete.
- Progress summary under the title (e.g. "3 of 7 done").
- Items render in stored order. Each row: circular check control and item text.
  Tapping the check toggles done; done rows are struck through and muted.
- Tapping the text opens an edit dialog. Long text wraps to a maximum of two
  lines in the list.
- Items can be deleted from a per-row overflow menu, with confirmation.
- A persistent inline input at the bottom adds a new item on submit; blank input
  is ignored. Keyboard stays available for rapid entry.
- Empty state explains how to add the first item.
- Back returns to the Lists screen.

### 4.3 Navigation

Single activity. Screen state is either `Lists` or `Checklist(id)`, saved across
configuration changes and process death. System back from `Checklist` returns to
`Lists`. No navigation library.

## 5. Visual design

### 5.1 App

- Material 3, following system light/dark mode.
- Fixed palette (no dynamic color) for a consistent look:
  - Accent: iOS blue `#0A84FF`
  - Dark surfaces: `#1C1C1E` cards on `#000000` background
  - Light surfaces: white cards on `#F2F2F7` background
  - Muted text: `#8E8E93`
- Default Android sans-serif (Roboto). Do not bundle SF Pro; the iOS feel comes
  from palette, spacing, rounded shapes, and restrained chrome.

### 5.2 Widget (iOS-dark, flat)

- Background: `#1C1C1E`, corner radius 24 dp, padding 16 dp.
- Header:
  - Checklist title, 17 sp, bold, white `#FFFFFF`, single line with ellipsis.
  - Progress `done/total`, 13 sp, `#8E8E93`.
  - 1 dp hairline divider `#2C2C2E` below the header.
- Rows:
  - Minimum height 40 dp; 22 dp circular icon; 15 sp item text.
  - Unchecked: outlined circle `#636366`, white text.
  - Checked: filled blue circle with white check `#0A84FF`, muted strikethrough
    text `#8E8E93`.
  - Items sorted unchecked first (stored order), then completed (stored order).
  - Text wraps to at most two lines, ellipsized.
  - Entire row is tappable to toggle.
- Empty states: pinned list with no items, and no pinned list at all. Both show
  a short muted message prompting the user to open the app.
- No stock Glance checkbox; custom icon + text rows so the iOS look is exact.

## 6. Data model

```kotlin
@Serializable
data class AppState(
    val version: Int = 1,
    val checklists: List<Checklist> = emptyList(),
)

@Serializable
data class Checklist(
    val id: String,                 // UUID
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long,            // epoch millis
    val updatedAt: Long,            // epoch millis
    val items: List<ChecklistItem> = emptyList(),
)

@Serializable
data class ChecklistItem(
    val id: String,                 // UUID
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long,            // epoch millis
    val position: Int,              // display order within the checklist
)
```

Invariants:

- At most one checklist has `isPinned = true`.
- Checklist titles are trimmed and non-blank.
- Item text is trimmed and non-blank.
- Item ids are unique across the whole app so widget actions can toggle by id.
- New items get `position = (max position + 1)`, appended.
- Deleting a checklist deletes its items.
- `updatedAt` changes on any list or item mutation.

## 7. Storage

- Preferences DataStore with a single key `state_json` holding the serialized
  `AppState`.
- `kotlinx.serialization` JSON with `ignoreUnknownKeys = true` and
  `encodeDefaults = true`.
- Writes use `DataStore.updateData` (atomic read-modify-write) so rapid toggles
  cannot interleave.
- Read path maps the DataStore flow to a decoded `AppState`, drops consecutive
  duplicates, and exposes it as `StateFlow<AppState>`.
- Undecodable JSON: log, fall back to an empty state, and preserve the raw
  value under `state_json_corrupt` for potential recovery. Never crash.
- Store layer is a pure transformation (`AppState` → `AppState`) so it is unit
  testable; the repository only handles persistence.
- Widget and app run in the same process by default; DataStore is not used
  cross-process.

## 8. Repository API

```kotlin
interface ChecklistRepository {
    val state: StateFlow<AppState>

    suspend fun createChecklist(title: String): String
    suspend fun renameChecklist(id: String, title: String)
    suspend fun deleteChecklist(id: String)
    suspend fun setPinned(id: String?)
    suspend fun addItem(checklistId: String, text: String): String
    suspend fun updateItem(itemId: String, text: String)
    suspend fun toggleItem(itemId: String)
    suspend fun deleteItem(itemId: String)
}
```

`setPinned(null)` unpins everything. `toggleItem` locates the item by unique id,
so the widget does not need to pass the checklist id.

## 9. Architecture

Single Gradle module `app`, package `dev.abhiram.checklist`, app name
"Checklist".

```
app/src/main/java/dev/abhiram/checklist/
  ChecklistApplication.kt        // manual dependency container + widget refresh observer
  MainActivity.kt
  data/
    model/AppState.kt            // models + JSON serializer config
    ChecklistStore.kt            // pure state transformations
    ChecklistRepository.kt       // interface
    DataStoreChecklistRepository.kt
  ui/
    ChecklistViewModel.kt        // exposes UI state + actions
    ListsScreen.kt
    ChecklistScreen.kt
    components/                  // shared dialogs/rows
    theme/                       // Material 3 theme
  widget/
    ChecklistWidget.kt           // Glance app widget
    ChecklistWidgetReceiver.kt
    ToggleItemAction.kt          // ActionCallback
    WidgetRefresh.kt             // updateAll helper
```

- Manual dependency container in `ChecklistApplication`; no Hilt, no KSP.
- `ChecklistViewModel` collects repository state and exposes a screen-oriented
  UI state. Every mutation goes through the repository.
- A widget-refresh observer in the application scope watches the repository
  state and calls `ChecklistWidget.updateAll()` after a 150 ms debounce. It is
  the single refresh path and also runs when a widget action starts the app
  process.
- The widget action callback toggles through the repository only; it does not
  call `updateAll` itself.
- Widget reads a repository snapshot inside `provideGlance`: one small
  DataStore read per update, never a loop.

## 10. Widget technical design

- Glance `GlanceAppWidget` with `SizeMode.Exact`; derives available size from
  `LocalSize` and computes rows:
  `rows = ((height - header - padding) / rowHeight).toInt().coerceIn(1, 24)`.
- Provider XML:
  - `minWidth` / `minHeight`: 250 dp (defaults to 4×4 on older launchers)
  - `minResizeWidth` / `minResizeHeight`: 110 dp (2×2 minimum)
  - `targetCellWidth` / `targetCellHeight`: 4
  - `resizeMode`: horizontal|vertical
  - no `maxResizeWidth` / `maxResizeHeight` — launcher grid is the only cap
  - `updatePeriodMillis = 0` (no polling)
  - `widgetCategory = home_screen`
- Row order: unchecked items first, then completed, each in stored order.
  Overflow beyond the computed row count is indicated by a muted "+N more" line
  if space allows.
- Tap title/header opens the app directly to the pinned checklist.
- Tap empty state opens the Lists screen.
- `ToggleItemAction` receives the item id via `ActionParameters`; Glance invokes
  its suspending `onAction`, which toggles through the repository. The
  application observer performs the single debounced refresh; the callback does
  not call `updateAll` itself. No `BroadcastReceiver.goAsync()` is involved.
- Multiple widget instances are supported and all show the same pinned list.
- Unpinning or deleting the pinned list makes widgets show the empty state.
- Widget updates on app launch, on any data change while the process is alive,
  and after widget actions.

## 11. Error handling and edge cases

- Blank list/item titles rejected in dialogs and add field.
- Deleting the pinned checklist leaves no pinned list; widgets show the empty
  state.
- Toggling an item that was deleted concurrently is a no-op.
- Duplicate rapid checkbox taps are serialized by DataStore updates.
- Rotations and process death preserve the open screen via saved state.
- Long titles/items ellipsize; item text wraps to two lines.
- If DataStore is unreadable, show empty app state and keep a corrupt backup key.
- No network permissions or internet dependency at runtime.

## 12. Testing

### Unit tests (JVM)

- Create/rename/delete checklist; blank titles rejected.
- Single-pin invariant: pinning a second list unpins the first; unpin works.
- Add item appends with increasing position; blank text rejected.
- Edit item updates text and `updatedAt`.
- Toggle item by globally unique id.
- Delete item removes only that item.
- Delete checklist cascades to its items.
- JSON round-trip preserves state; unknown keys ignored.
- Decoding corrupt JSON falls back safely (repository-level test).

### Manual on-device tests

- Fresh install: empty state, create list, add items, edit, toggle, delete.
- Pin a list; widget appears with correct content and progress.
- Toggle from widget with the app open, backgrounded, and force-stopped.
- Resize widget to 2×2, 4×4, tall, and wide; verify row/empty-state behavior.
- Pin a different list; widget switches. Unpin; widget shows empty state.
- Delete pinned list; widget shows empty state.
- Kill app, reboot phone; data persists.
- Rotate app on both screens; state survives.
- Verify dark and light system modes in the app; widget stays iOS-dark.
- Install the R8 release APK and run the matrix against it.
- After opening both screens, run `adb shell dumpsys meminfo
  dev.abhiram.checklist` and record PSS.
- Leave the app open and idle for 10 seconds, then run `adb shell top -b -n 1`
  and confirm the app's CPU is near 0%.
- Toggle widget rows rapidly; confirm refresh is coalesced and does not jank.

### Build verification

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 13. Toolchain

- Gradle 9.8.1 (cached), AGP 9.4.1 (cached), JDK 21
  (`/home/abhiram/.local/opt/jdk21`).
- Kotlin 2.4.21, Compose 1.12.1, Material 3 1.4.0, Activity Compose 1.13.0,
  Lifecycle 2.11.0, DataStore Preferences 1.1.7,
  kotlinx-serialization-json 1.11.0, Glance 1.2.0.
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`.
- Platform 37 and build-tools 36.0.0 are installed. Glance 1.2.0 requires
  compileSdk ≥ 35 and AGP ≥ 8.6; Compose 1.12.1 requires compileSdk 37 and
  AGP ≥ 9.1, so the cached toolchain fits.
- Gradle memory settings tuned for 8 GB RAM: `-Xmx2g`, max 4 workers.
- Release build: R8 minification, resource shrinking, and the local debug
  signing key for sideloading.
- Physical test device over USB via `adb`; no emulator installed.

## 14. Build milestones

1. **Scaffold** (~45 min): Gradle wrapper, manifest, theme, minimal Compose
   activity; `assembleDebug` succeeds; git initialized.
2. **Data layer** (~45 min): models, pure store, repository, serialization,
   unit tests green.
3. **App UI** (~2 h): Lists and Checklist screens, dialogs, ViewModel,
   navigation; installable and usable without the widget.
4. **Widget** (~2 h): provider XML, Glance UI, resize math, row toggles,
   cold-process action, refresh wiring; iOS-dark theme.
5. **Verification/polish** (~1 h): device test matrix, PSS/idle-CPU check,
   fixes, README, final `test` + `assembleDebug` + `assembleRelease`, release
   APK handed over.

## 15. Risks and fallbacks

- **Bleeding-edge toolchain:** AGP 9.4.1 + Compose 1.12.1 are newer than the
  most battle-tested combination. If the build fails or is unstable, fall back
  to an AGP ≥ 9.1 / Compose 1.12-compatible setup, or install SDK 36 and use
  Compose 1.9/1.10 with Glance 1.2. Time-boxed to ~30 minutes.
- **Low RAM:** first build and Kotlin/Compose compilation may be slow. Cap
  Gradle workers and avoid clean builds; use incremental builds.
- **Glance cold-start actions:** explicitly test with `adb shell am
  force-stop dev.abhiram.checklist`. If the action fails to run, verify the
  receiver/intent configuration before changing approach.
- **Launcher differences:** rounded corners and resize limits are
  launcher-controlled; the widget draws its own rounded background and accepts
  launcher clipping.
- **No emulator:** all UI verification depends on the phone being connected;
  otherwise a build-only deliverable is still produced.

## 16. Deliverables

- Android Studio/Gradle project at `/home/abhiram/projects/widgets`.
- Debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- R8 release APK at `app/build/outputs/apk/release/app-release.apk`, signed with
  the debug key for local sideloading.
- Unit tests and passing `./gradlew test`.
- `README.md` with build, install, and usage instructions.
- Git history with the spec, implementation, and tests.
