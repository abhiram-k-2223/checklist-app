# Checklist

A local-only Android checklist app with home-screen widgets. Each widget shows
the checklist you choose for it and lets you tick items off without opening the
app. Pin marks the default/fallback checklist for legacy or unconfigured
widgets.

## Features

- Multiple checklists with create, rename, delete, and pin
- Any number of widgets, each bound to the checklist you choose for it
- Widget picker lists every checklist, with the pinned one first and badged
- Pin is only the default/fallback; it never filters the picker or overrides a
  widget's own binding
- Reconfigure any widget on Android 12+ without removing it
- One pinned checklist at a time; pinning another unpins the previous one
- Add, edit, toggle, and delete items
- The widget shows unchecked items before completed items
- Resizable Glance widget (default 4×4, minimum 2×2, no artificial maximum)
- Always-dark night theme: the app ignores the system light setting
- Flat iOS-style dark widget theme
- Scrollable widget list; visible rows adapt to the resized height
- Widget rows toggle items in place; the widget header opens that checklist
- Local-only data in Preferences DataStore; no accounts, network, or analytics
- Built for low-end phones: no polling or background services, an awaited
  widget refresh after app mutations plus an in-callback update for widget
  taps, vector-only assets, and an R8 release build

## Build

Requires JDK 21 and the Android SDK with platform 37.

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. For daily use,
install the R8 release build at
`app/build/outputs/apk/release/app-release.apk`; it is signed with the local
debug key for sideloading, not for Play Store distribution.

## Install on a phone

Enable USB debugging, connect the phone, then:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n dev.abhiram.checklist/.MainActivity
```

## Low-resource design

- One Activity and one process; no services, alarms, scheduled background
  jobs, or polling
- Glance bundles WorkManager for async widget requests, but its startup
  initializer is disabled; WorkManager starts only if a widget request needs
  it, and this app schedules no background work
- The widget uses `updatePeriodMillis=0`; after a successful app mutation the
  ViewModel awaits an update of every widget, and a widget tap updates its own
  widget before the action callback returns. Nothing observes or polls in the
  background.
- The widget reads one initial snapshot per session, then observes the
  repository's `StateFlow` inside the Glance composition; nothing polls in the
  background
- Data is one JSON document in Preferences DataStore, written only on explicit
  user actions
- Vector drawables only; no bitmaps or decorative animations
- No Room, KSP, Hilt, or navigation library in the app; Glance's transitive
  WorkManager bundles Room for its own database, which is kept for release
- R8 minification and resource shrinking for the release build, with keep rules
  for the widget entry points and Glance's WorkManager/Room reflection targets

To check footprint on the phone:

```bash
adb shell dumpsys meminfo dev.abhiram.checklist
adb shell top -b -n 1 | grep dev.abhiram.checklist
```

## Add the widget

1. Long-press the home screen and open **Widgets**.
2. Find **Checklist** and drag it onto the home screen.
3. Pick any checklist in the picker. The pinned checklist is listed first with
   a pin badge, but every checklist is selectable.
4. Repeat for more widgets; each one keeps its own checklist.
5. To change an existing widget on Android 12+, long-press it and choose
   **Reconfigure**.

Pinning is no longer required to add a widget. Existing widgets created before
per-widget selection bind to the pinned checklist on first render, so a
Groceries widget keeps showing Groceries.

## Tests

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test
```

The suite has 74 tests covering the JSON codec, store rules, repository
persistence, corrupt state recovery, per-widget binding and migration, picker
ordering and progress, widget row layout, widget toggle refresh, and ViewModel
mutation success/error handling.

## Architecture

- `data/model` — serializable `AppState`, `Checklist`, `ChecklistItem`
- `data/ChecklistStore` — pure state transformations
- `data/WidgetBindings` — per-widget binding helpers and migration target
- `data/DefaultChecklistRepository` — atomic DataStore updates
- `ui` — Compose screens and `ChecklistViewModel`
- `widget` — Glance renderer, picker configuration activity, legacy migration,
  row math, and toggle action callback
- `ChecklistApplication` — manual DI container and lazy WorkManager
  configuration; the ViewModel triggers widget refreshes after mutations

There is no Room, KSP, Hilt, or navigation library; the app is intentionally
small and builds quickly on a low-memory machine.

## Limitations

- No cloud sync, reminders, due dates, tags, drag reorder, or images
- No widget text entry
- Widget taps toggle items only; all editing happens in the app
- Widget text caps at two lines and clips overflow because Glance 1.2 has no
  ellipsis API
- In-widget scrolling depends on launcher support; unsupported launchers still
  show the visible rows but cannot scroll to the rest
- Widget bindings use launcher-assigned app-widget IDs; clearing app data or
  switching launchers resets them
- Reconfiguration requires Android 12+; older launchers may need the widget
  removed and re-added
- “Local-only” means the app has no network permission or sync; Android’s OS
  backup setting may still include app data if enabled on the device
