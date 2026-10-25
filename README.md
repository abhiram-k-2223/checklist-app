# Checklist

A local-only Android checklist app with a home-screen widget. The widget shows
one pinned checklist and lets you tick items off without opening the app.

## Features

- Multiple checklists with create, rename, delete, and pin
- One pinned checklist at a time; pinning another unpins the previous one
- Add, edit, toggle, and delete items
- The widget shows unchecked items before completed items
- Resizable Glance widget (default 4×4, minimum 2×2, no artificial maximum)
- Flat iOS-style dark widget theme
- Widget rows toggle items in place; the widget header opens that checklist
- Local-only data in Preferences DataStore; no accounts, network, or analytics
- Built for low-end phones: no polling or background services, debounced
  single-path widget refresh, vector-only assets, and an R8 release build

## Build

Requires JDK 21 and the Android SDK with platform 37.

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
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
- The widget uses `updatePeriodMillis=0` and refreshes only after data changes
  or widget actions, through one 150 ms debounced observer
- Widget actions toggle only; they do not trigger a second refresh
- The widget reads a repository snapshot once per render, never in a loop
- Data is one JSON document in Preferences DataStore, written only on explicit
  user actions
- Vector drawables only; no bitmaps or decorative animations
- No Room, KSP, Hilt, or navigation library
- R8 minification and resource shrinking for the release build

To check footprint on the phone:

```bash
adb shell dumpsys meminfo dev.abhiram.checklist
adb shell top -b -n 1 | grep dev.abhiram.checklist
```

## Add the widget

1. Pin a checklist from the list menu in the app.
2. Long-press the home screen and open **Widgets**.
3. Find **Checklist** and drag it onto the home screen.
4. Resize it by long-pressing and dragging the handles.

## Tests

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
./gradlew test
```

Unit tests cover the JSON codec, store rules, repository persistence, corrupt
state recovery, and widget row layout.

## Architecture

- `data/model` — serializable `AppState`, `Checklist`, `ChecklistItem`
- `data/ChecklistStore` — pure state transformations
- `data/DefaultChecklistRepository` — atomic DataStore updates
- `ui` — Compose screens and `ChecklistViewModel`
- `widget` — Glance renderer, row math, and toggle action callback
- `ChecklistApplication` — manual DI container and the single debounced
  widget-refresh observer

There is no Room, KSP, Hilt, or navigation library; the app is intentionally
small and builds quickly on a low-memory machine.

## Limitations

- No cloud sync, reminders, due dates, tags, drag reorder, or images
- No widget text entry
- Widget taps toggle items only; all editing happens in the app
