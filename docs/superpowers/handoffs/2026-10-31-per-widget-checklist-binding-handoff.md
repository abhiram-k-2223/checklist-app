# Per-Widget Checklist Binding Handoff

**Date:** 2026-11-09
**Repository:** https://github.com/abhiram-k-2223/checklist-app (private)
**Branch:** `master`
**Implementation range:** `126c2ef..4b8a8bc`
**Phone:** `GAKNWKGQJJJ7QWXW` (Android 14 / API 34, 720×1600, density 320)

## What changed

Each home-screen widget now remembers the checklist it shows instead of always
showing the globally pinned checklist.

- `AppState.widgetBindings: Map<String, String>` maps an app-widget ID to a
  checklist ID. The field defaults to an empty map, so existing JSON loads
  without migration.
- `data/WidgetBindings.kt` contains the pure binding helpers:
  `bindWidget`, `unbindWidget`, `checklistForWidget`, `bindingMigrationTarget`,
  and `unboundWidgetIds`. A stale binding resolves to `null` rather than
  silently falling back to a different checklist.
- `ChecklistStore.deleteChecklist` removes every binding that points at the
  deleted checklist.
- `ChecklistRepository` exposes `setWidgetBinding(appWidgetId, checklistId)`,
  `setWidgetBindingIfUnbound(appWidgetId, checklistId)`, and
  `removeWidgetBinding(appWidgetId)`. `DefaultChecklistRepository` persists them
  through the existing JSON/DataStore mutation path. Migration uses the
  if-unbound variant so a pin can never overwrite an explicit selection made
  concurrently.
- `ChecklistWidget` resolves its own app-widget ID, migrates an unbound legacy
  widget to the currently pinned checklist once, observes repository state, and
  renders through `checklistForWidget`. `onDelete` removes the binding.
- `ChecklistWidgetConfigActivity` is a dark configuration screen. It reads
  `EXTRA_APPWIDGET_ID`, resolves the Glance ID, migrates other unbound widgets,
  lists every checklist with the pinned one first and badged, and writes only
  the selected binding. Back cancels without writing. The empty state opens the
  app and writes nothing.
- `checklist_widget_info.xml` declares `android:configure` and
  `android:widgetFeatures="reconfigurable"`. The manifest declares the exported,
  recents-excluded configuration activity with the `APPWIDGET_CONFIGURE` filter.
- `README.md` documents per-widget selection, arbitrary widget count,
  reconfiguration, and Pin as the default/fallback only.

Pin is still available in the app. It is the default/fallback for unconfigured
or legacy widgets and never filters the picker or overrides an existing binding.

## Verification performed

- **Unit tests:** `./gradlew :app:testDebugUnitTest --rerun-tasks` —
  **BUILD SUCCESSFUL**, 13 XML result files, **76 tests, 0 failures, 0 errors,
  0 skipped**.
- **Builds:** `./gradlew :app:assembleDebug :app:assembleRelease` —
  **BUILD SUCCESSFUL** in 2m 3s.
- **Release APK:** `app/build/outputs/apk/release/app-release.apk`, **2.2 MB**,
  SHA-256
  `0f2bc226642af37894929357e9516f1b9a4cae3900e863813fb95a52b484d5f2`.
- **Merged manifest:** `ChecklistWidgetConfigActivity` appears in the merged
  debug manifests under
  `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/`,
  `.../packaged_manifests/debug/processDebugManifestForPackage/`, and
  `.../merged_manifests/debug/processDebugManifest/`.
- **Provider XML:** both `android:configure` and
  `android:widgetFeatures="reconfigurable"` are present.
- **Git:** private repository created and `master` pushed. `origin/master`
  points at `4b8a8bc` before the handoff commit.

## Final review fixes

A fresh whole-branch review found two Important issues. Both were fixed with
TDD and are included in `4b8a8bc`:

- The picker now renders through `pickerChecklists`, so the pinned checklist is
  actually first. The previous helper was tested but unused by the composable.
- Migration now uses `setWidgetBindingIfUnbound`, which checks the current
  DataStore state inside the mutation, so a concurrent explicit selection cannot
  be overwritten by the pin.

Minor findings deferred: config selection coroutine error handling; late
`onDelete` after app-widget ID reuse; additional activity/manifest test coverage.

## On-device verification: pending

The phone was not connected during this session. `adb devices -l` returned an
empty device list and `lsusb` showed no Android device, so the release APK was
**not installed** and none of the on-device checks below were run against this
build.

Reconnect the phone and run:

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
adb -s GAKNWKGQJJJ7QWXW install -r app/build/outputs/apk/release/app-release.apk
adb -s GAKNWKGQJJJ7QWXW shell am start -W -n dev.abhiram.checklist/.MainActivity
```

Checks still needed:

1. Existing Groceries widget still shows Groceries after the upgrade; pin Todo
   and confirm the Groceries widget does not change (legacy migration).
2. Add a second widget: the picker lists every checklist, pinned first and
   badged; choose Todo; the first widget stays Groceries and the second shows
   Todo.
3. Add a third widget and confirm all three are independent; reconfigure one
   and confirm only that widget changes; Back from add/reconfigure leaves the
   widget unchanged.
4. Toggle rows in each widget and confirm the app reflects the same data; change
   the pin and confirm bound widgets are unaffected.
5. Delete a bound checklist and confirm the widget falls back to the pin or the
   unbound state without crashing.
6. Kill the app process (`adb shell am kill dev.abhiram.checklist`) and tap a
   widget row; confirm it still renders and toggles.
7. Resize one widget to 2×2 and tall; confirm scrolling and toggles work. If the
   widget height is below 160 dp, confirm the compact one-line header; otherwise
   record the actual height and confirm the regular header.
8. Confirm the widget stays black in both system-light and system-dark modes.
9. Check `adb logcat -d -s AndroidRuntime:E ChecklistWidget:E
   ChecklistWidgetConfig:E` for uncaught errors.

## Risks and notes

- In-widget scrolling and reconfiguration depend on launcher support. The
  current phone uses the Pixel Launcher; other launchers may differ.
- Bindings use launcher-assigned app-widget IDs. Clearing app data or switching
  launchers resets them; widgets then fall back to the pin and migrate again.
- A stale binding for a deleted checklist resolves to `null` and shows the
  unbound/empty state; it does not silently switch to the pin.
- The release APK is signed with the local debug key and is not Play-ready.
- The existing widget was verified in an earlier session against the previous
  build; that does not prove this build's per-widget behavior.

## Follow-up

After the on-device matrix passes, update this handoff with the actual results
and any launcher-specific caveats. The private repository is
https://github.com/abhiram-k-2223/checklist-app.
