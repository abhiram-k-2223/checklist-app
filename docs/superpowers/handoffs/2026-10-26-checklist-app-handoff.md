# Checklist App — Session Handoff

**Date:** 2026-10-26
**Repo:** `/home/abhiram/projects/widgets`
**Branch:** `feat/checklist-app`
**App HEAD at handoff:** `cb8fb45` (`fix: use iOS blue for the add button`)
**Base:** `master` at `91ccbab`
**Working tree:** clean before this handoff doc
**Remote:** none configured

Read this file first in the new session. Then run `git status`, `git log --oneline -5`, and the full test command before changing anything. Do not redo the completed tasks.

---

## What this project is

A local-only Android checklist app with a home-screen widget.

- App name: **Checklist**
- Package: `dev.abhiram.checklist`
- Stack: Kotlin + Jetpack Compose + Glance widget + one JSON document in Preferences DataStore
- No Room, KSP, Hilt, or navigation library in app code
- One Activity, one process
- Release build: R8 minified + resource shrunk, signed with the local debug key for sideloading
- `compileSdk = 37`, `minSdk = 26`, `targetSdk = 37`
- One pinned checklist is shown in the widget
- Widget default 4×4, minimum 2×2, freely resizable
- Flat iOS-dark widget palette; no liquid glass

The approved design is in:

- `docs/superpowers/specs/2026-10-25-android-checklist-widget-design.md`
- `docs/superpowers/plans/2026-10-25-android-checklist-widget.md`

`README.md` contains build/install instructions and user-facing limitations.

---

## Implemented features

- Multiple checklists: create, rename, delete, pin
- Exactly one pinned checklist; pinning another unpins the previous one
- Items: add, edit, toggle, delete
- App preserves stored item order; only the widget sorts unchecked before completed
- Widget rows toggle items directly
- Widget header opens the pinned checklist
- Empty widget state opens Lists
- Widget title/progress, 44 dp rows, 24 dp corners, iOS-dark palette
- Data persists across app restarts and process restarts
- Malformed JSON falls back safely; corrupt raw JSON is backed up on the next mutation
- DataStore IO failures fall back to empty state instead of crashing
- ViewModel mutation failures are logged instead of crashing
- App-originated edits refresh widgets with an awaited update
- Widget taps update their own widget inside the action callback

---

## Current phone state

- Device: `GAKNWKGQJJJ7QWXW`
- Model reported by ADB: `TrebleDroid_with_GApps` (Android 14 / SDK 34 GSI)
- Screen: 720×1600, density 320, fontScale 0.85
- USB debugging is enabled and authorized
- Final release APK is installed
- App data exists: checklist **Groceries**, five items, **3 of 5 done**
- The home-screen widget is **not currently bound** because reinstalling the APK removed it
- The widget itself was verified working before that reinstall

To add it again manually:

1. Long-press the home screen.
2. Open **Widgets**.
3. Find **Checklist**.
4. Drag the 4×4 preview onto the home screen.

The launcher's widget picker is flaky on this GSI when driven by `adb` gestures; manual dragging worked.

The phone intermittently renders the system/app UI duplicated side-by-side in screenshots. This is a GSI display/framebuffer issue, not an app layout bug; logical coordinates remain 720 wide and taps land correctly.

---

## What was verified on-device

- App launches to the Lists screen.
- Created **Groceries**; app opened it automatically.
- Added **Milk, Eggs, Bread, Apples, Coffee**.
- Pinned Groceries; blue pin and progress shown.
- Data persisted across activity/process restarts.
- Release widget bound after the R8 fixes.
- Widget rendered all five items, completed Milk at the bottom with blue check and strikethrough.
- Widget showed `1/5`; tapping rows changed it to `3/5`.
- Opening the app confirmed `3 of 5 done`, proving persistence.
- Add button is now the correct iOS blue (`#0A84FF`), not Material purple.

Not yet verified on-device:

- Widget resize to 2×2 and very tall/wide sizes
- PSS memory
- Idle CPU
- Reboot persistence
- Process-death recovery
- Rapid/stale widget taps
- Large font scale

Use `adb shell am kill dev.abhiram.checklist` or swipe-away for process-death testing. On Android 15+, `am force-stop` disables widgets and is not a valid widget test; this phone is Android 14, but `am kill` is still the better process-death check.

---

## Verification commands

Always point Gradle at JDK 21:

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
./gradlew test
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Latest verified state:

- **45 tests, 0 failures, 0 errors, 0 skipped**
- `:app:assembleDebug` and `:app:assembleRelease` both succeed
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — about **14 MB**
- Release APK: `app/build/outputs/apk/release/app-release.apk` — about **2.2 MB**

Install/relaunch:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n dev.abhiram.checklist/.MainActivity
```

Memory/CPU:

```bash
adb shell dumpsys meminfo dev.abhiram.checklist
adb shell top -b -n 1 | grep dev.abhiram.checklist
```

Useful screenshots from testing:

`/tmp/opencode/checklist-test/01-lists.png` through `23-fab-fixed.png`

---

## Important fixes already made

### Release widget / R8

The release widget initially failed with:

```text
Failed to create an instance of androidx.work.impl.WorkDatabase
```

R8 had removed Room classes used by Glance's transitive WorkManager. `app/proguard-rules.pro` now keeps:

- `androidx.room.**`
- `androidx.work.**`
- subclasses of `androidx.room.RoomDatabase`
- subclasses of `androidx.work.ListenableWorker`
- `-dontwarn androidx.room.**`

This is why the release APK grew from 1.6 MB to about 2.2 MB.

### Glance 10-child limit

Glance RemoteViews rejects a Column with more than 10 direct children:

```text
Column container cannot have more than 10 elements
```

Each item row plus its 1 dp divider is wrapped in its own Column container; visible rows are capped at 8, with `+N more`.

### Widget row math

`app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetRows.kt`:

```kotlin
fun maxWidgetRows(heightDp: Float): Int {
    // 32 dp padding + ~49 dp header + 20 dp overflow line = 101 dp fixed.
    // Row = 44 dp + 1 dp separator.
    val available = (heightDp - 101f).coerceAtLeast(0f)
    return (available / 45f).toInt().coerceIn(0, 8)
}
```

- 44 dp rows
- 8-row cap
- 0 rows on a very short widget shows a tappable “Open app to see/add items” prompt
- Widgets shorter than about 146 dp do not attempt to render rows

### Widget refresh

There is no background observer now.

- App mutations: `ChecklistViewModel` calls an injected `refreshWidgets` callback after a successful mutation; `WidgetRefresh.kt` updates every Checklist Glance ID and awaits it.
- Widget taps: `ToggleItemAction` uses `WidgetToggleRunner` to toggle and then update its own widget before returning.
- `updatePeriodMillis=0`; no polling, services, alarms, or scheduled work.
- Glance's transitive WorkManager is lazy-initialized; its startup initializer is removed in the manifest. The app schedules no WorkManager jobs.

### Widget intents

- Widget intents include `from_widget=true`.
- `MainActivity` consumes the launch intent only when `savedInstanceState == null`.
- `onNewIntent` handles widget intents while the app is alive.
- An empty-state intent with no checklist id calls `closeChecklist()`.
- Extras are cleared after handling.

### Data/error handling

- `DataStoreStatePersistence` catches `IOException` and emits an empty state.
- Preferences DataStore has `ReplaceFileCorruptionHandler { emptyPreferences() }`.
- Repository `state` catches non-cancellation failures and emits `AppState()`.
- `snapshot()` rethrows `CancellationException`; other failures return `AppState()`.
- Widget rendering catches snapshot failures and shows the empty state.
- ViewModel mutations rethrow cancellation, log other failures, and do not crash.
- Widget refresh failures are logged and do not crash.

---

## Key files

```text
app/src/main/kotlin/dev/abhiram/checklist/
  MainActivity.kt
  ChecklistApplication.kt
  WidgetLaunch.kt
  data/
    ChecklistRepository.kt
    DefaultChecklistRepository.kt
    DataStoreStatePersistence.kt
    StatePersistence.kt
    ChecklistStore.kt
    model/AppState.kt
  ui/
    ChecklistApp.kt
    ListsScreen.kt
    ChecklistScreen.kt
    ChecklistViewModel.kt
    UiFormat.kt
    components/Dialogs.kt
    theme/Theme.kt
  widget/
    ChecklistWidget.kt
    ChecklistWidgetReceiver.kt
    WidgetRows.kt
    WidgetRefresh.kt
    WidgetToggleRunner.kt
```

Tests:

```text
app/src/test/kotlin/dev/abhiram/checklist/
  data/AppStateJsonTest.kt
  data/ChecklistStoreChecklistTest.kt
  data/ChecklistStoreItemTest.kt
  data/DefaultChecklistRepositoryTest.kt
  data/DataStoreStatePersistenceTest.kt
  ui/ChecklistViewModelTest.kt
  widget/WidgetRowsTest.kt
  widget/WidgetToggleRunnerTest.kt
  WidgetLaunchTest.kt
```

---

## Final review status

A fresh-context review found four Important issues; all four were fixed:

1. Widget row math clipped at 2×2 and dropped overflow — fixed with 44 dp rows, reserved header/padding/overflow space, 0-row compact prompt, 8-row cap.
2. Sticky widget intents / empty-state tap no-op — fixed with `from_widget`, saved-state guard, intent clearing, and `closeChecklist()`.
3. DataStore IO/corruption failures could crash — fixed with corruption handler, `.catch`, repository/widget/ViewModel error isolation.
4. Widget action refresh could outlive the receiver — fixed with in-callback awaited targeted update plus direct app-mutation refresh.

Minor items deferred:

- Corrupt JSON is not logged; backup is written only on the next mutation.
- Some Material 3 theme roles are still default values (the FAB was fixed to iOS blue; dialogs/menus may still use defaults).
- Release manifest still carries transitive WorkManager/Room permissions and components; no `INTERNET` permission.
- `allowBackup` remains true; README qualifies what “local-only” means.
- Repository tests use a fake persistence for most cases; real DataStore atomicity/concurrent-writer behavior is not tested.
- Dialog state uses `remember`, so rotation can dismiss an open dialog and lose typed text.
- Widget text caps at two lines and clips; Glance 1.2 has no ellipsis API.
- Widget picker gestures are flaky on this GSI when driven by adb.

No known data-loss or security blocker.

---

## Open decisions / next steps

1. **Merge or keep the branch.** The user has not chosen. Base is `master`.
   - Merge locally after review, or
   - keep `feat/checklist-app` as-is.
   - There is no git remote, so a PR would require creating one first.
2. **Re-add the widget manually** on the phone and complete the unverified matrix if desired:
   - 2×2 and tall/wide resize
   - PSS and idle CPU
   - reboot
   - `am kill` process death
   - rapid/stale widget taps
   - large font scale
3. Optionally address deferred minors, especially theme roles and corrupt-JSON logging.

---

## Quick start for the new session

Suggested opening prompt:

> Read `docs/superpowers/handoffs/2026-10-26-checklist-app-handoff.md` and continue from the current state. Do not redo completed tasks. Check `git status`, run the full test suite, and confirm the installed release APK. The next decision is whether to merge `feat/checklist-app` into `master`; on-device widget resize/PSS/reboot/process-death checks are still unverified.
