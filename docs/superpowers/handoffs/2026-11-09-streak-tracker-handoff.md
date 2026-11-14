# Streak Tracker Handoff

**Date:** 2026-11-14
**Repository:** `https://github.com/abhiram-k-2223/checklist-app`
**Branch:** `master` (merged from `feat/streak-tracker`)
**Implementation range:** `97500a3..cf2405c`
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

- Unit tests: 150 passed with
  `./gradlew :app:testDebugUnitTest --rerun-tasks`.
- Debug build: `./gradlew :app:assembleDebug` succeeded.
- Release build: `./gradlew :app:assembleRelease` succeeded.
- Published release APK SHA-256:
  `bee42f49a70eeeba7134fcc658285f1fd20b45c5f93a7dd0545f9fdbd0bdc911`.
- Merged release manifest contains `StreakWidgetReceiver` and
  `StreakWidgetConfigActivity` with exported flags and the
  `APPWIDGET_UPDATE` / `APPWIDGET_CONFIGURE` filters.
- `streak_widget_info.xml` has 250×300 dp minimums, 4×5 target cells,
  `updatePeriodMillis="0"`, resize in both directions, and `reconfigurable`.
- Existing checklist tests and behavior remain green.

## On-device verification

`adb devices` returned only `List of devices attached`, so phone
`GAKNWKGQJJJ7QWXW` was not connected. On-device verification is pending.

| Check | Result |
|---|---|
| Create a tracker with four colored tasks | BLOCKED — phone not connected |
| Add a Streak widget and choose a tracker | BLOCKED — phone not connected |
| Calendar, today highlight, dots, and task rows | BLOCKED — phone not connected |
| Toggle in the widget updates dots, streaks, and app | BLOCKED — phone not connected |
| Two trackers and two widgets stay independent | BLOCKED — phone not connected |
| Reconfigure one widget; cancel leaves it unchanged | BLOCKED — phone not connected |
| Task add/edit/delete limits, colors, and history warning | BLOCKED — phone not connected |
| Delete a bound tracker leaves an unbound widget without a crash | BLOCKED — phone not connected |
| Seeded multi-day streak counters | BLOCKED — phone not connected |
| Process death, reboot, resize/scroll, dark theme, logcat | BLOCKED — phone not connected |
| Optional date-advance rollover and restore | BLOCKED — phone not connected |

## Review fixes

The final whole-branch review found two critical and three important issues.
All five were addressed in `944121c`:

- **Rollover self-cancellation:** app startup now uses
  `ExistingWorkPolicy.KEEP`, while the worker's next-midnight reschedule uses
  `REPLACE`. A regression test pins both policy choices.
- **Minimum-height task rows:** below 360dp the widget uses 10dp padding and
  26dp calendar cells, leaving 40dp for the 36dp task rows at the declared
  300dp minimum. A layout-budget test asserts a full row fits.
- **Unbound widget dead end:** an unbound widget now opens
  `StreakWidgetConfigActivity` with its app-widget ID, and the copy tells the
  user to tap to choose or open the picker. The config activity's existing
  empty state still offers `Open app`.
- **Color picker accessibility:** each color is now a 48dp radio-style
  selectable target with a per-color content description and a visible selected
  ring. The color-name helper is unit-tested.
- **Review Focus #5:** the minimum-height layout-budget test was added. The
  on-device matrix remains blocked because the phone is disconnected.

After the fixes, 150 unit tests pass, both builds succeed, and the published
release APK SHA-256 is
`bee42f49a70eeeba7134fcc658285f1fd20b45c5f93a7dd0545f9fdbd0bdc911`.
