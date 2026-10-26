# Dark, scrollable widget — Session Handoff

**Date:** 2026-10-26
**Repo:** `/home/abhiram/projects/widgets`
**Branch:** `master`
**Code commits:** `4f4689b` (dark theme + scrollable widget), `10cf624` (live widget refresh)
**Previous handoff commit:** `3583457`
**Remote:** none configured
**Phone serial:** `GAKNWKGQJJJ7QWXW` (Android 14 / SDK 34 GSI)

Read this after `docs/superpowers/handoffs/2026-10-26-checklist-app-handoff.md`. The older handoff covers the original build; this one covers the follow-up changes and what remains unverified.

## What changed in `4f4689b`

- The app is now always dark. `ChecklistTheme` always uses `DarkColors`; the light palette and `isSystemInDarkTheme()` were removed.
- `MainActivity` forces light system-bar icons with `SystemBarStyle.dark(TRANSPARENT)`, so the status/navigation bars remain readable when the phone itself is in light mode.
- The Android XML theme uses the dark parent and a black window background.
- The widget background is true black (`#000000`).
- The widget no longer caps at eight rows or shows `+N more`. It has a fixed header and a Glance `LazyColumn` that fills the remaining height, so every item is reachable by scrolling.
- Widget rows use stable item ids (`item.id.hashCode()`) so scrolling position can survive widget updates on Android 12+.
- Below 160 dp the widget uses a compact one-line header and 12 dp outer padding; above that it uses the original two-line header and 16 dp padding.
- `WidgetRows.kt`/`WidgetRowsTest.kt` became `WidgetLayout.kt`/`WidgetLayoutTest.kt`. Tests now cover sorting, no truncation, empty input, and the compact breakpoint.
- `README.md` documents the always-dark theme, scrollable widget, and launcher-support caveat.

## Live widget refresh fix in `10cf624`

The first on-device pass found that some row taps changed the data but left the
home widget stale. Diagnostic logging showed the action ran and the refresh was
called, but `provideGlance` was not always re-run:

- Glance `1.2.0` docs: "`update` and `updateAll` do not restart `provideGlance`
  if it is already running." Updates recompose the existing session.
- The widget read `repository.snapshot()` once before calling `provideContent`,
  so while the session worker stayed alive, recomposition reused the list
  captured on the first render.
- Once the session expired, the next update started a new session and worked,
  which made the bug intermittent.

`ChecklistWidget.provideGlance` now uses the snapshot only as the initial value
and observes `repository.state` with `collectAsState` inside `provideContent`.
A rapid two-item toggle test then updated the widget on every tap.

## Verification performed

- `./gradlew :app:testDebugUnitTest --rerun-tasks`: **44 tests, 0 failures, 0 errors, 0 skipped**.
- `./gradlew :app:assembleRelease`: successful.
- Release APK installed on the phone with `adb -s GAKNWKGQJJJ7QWXW install -r app/build/outputs/apk/release/app-release.apk` (12:31).
- APK: 2.27 MB, package `dev.abhiram.checklist`, versionCode 1, versionName 1.0, minSdk 26, targetSdk 37.
- Signed with `CN=Android Debug` (SHA-256 `667afde3c5c214c5accb2904abfdf4407ab2c110d331581f0f0527b79d9e8764`).
- APK SHA-256: `b9d7152954b6038a7a8cc8a169364fc7e7043019e9fdaa2db3413f9a50d9e126`.
- App is black in phone light mode **and** night mode; status/navigation icons stay visible.
- Widget added, pinned to "Groceries", and rendered at 4×4.
- Resized to 2×2 (`[32,378][344,838]`, about 156×230 dp). The header stayed fixed, the list showed roughly three rows, and a vertical swipe scrolled to the remaining items. No `+N more` footer.
- Row taps update immediately after the refresh fix, including two taps one second apart. The release build (R8) was verified after installing over the debug build.
- System night mode was restored to `yes`; the app and widget stayed black.

## Not verified / next steps

1. Wide and tall layouts (e.g. 4×2 and 2×4).
2. The compact header at heights below 160 dp. The Pixel launcher's 2×2 size was
   about 230 dp, so this path was only covered by unit tests.
3. Empty-checklist and unbound-widget states after the final install.
4. Reboot and process death (`adb shell am kill dev.abhiram.checklist`), then
   confirm the widget restores.
5. PSS / idle CPU, large font scale, and accessibility checks.
6. Widget scroll position after an update; stable ids are in place but this was
   not measured.

## Risk / fallback

In-widget scrolling depends on the launcher rendering collection widgets. The
standard Android 14 launcher supports it. If another launcher does not, the
visible rows still render but cannot scroll to the rest; the fallback would be
explicit previous/next controls in the widget.
