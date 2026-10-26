# Dark, scrollable widget — Session Handoff

**Date:** 2026-10-26
**Repo:** `/home/abhiram/projects/widgets`
**Branch:** `master`
**Code commit:** `4f4689b` (`Make theme always dark and widget scrollable`)
**Previous handoff commit:** `3583457`
**Remote:** none configured
**Phone serial (last seen):** `GAKNWKGQJJJ7QWXW` (Android 14 / SDK 34 GSI)

Read this after `docs/superpowers/handoffs/2026-10-26-checklist-app-handoff.md`. The older handoff covers the original build; this one covers the follow-up changes and what is still unverified.

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

## Verification performed

- `./gradlew :app:testDebugUnitTest --rerun-tasks`: **44 tests, 0 failures, 0 errors, 0 skipped**.
- `./gradlew :app:assembleDebug :app:assembleRelease`: successful.
- Release APK: `app/build/outputs/apk/release/app-release.apk`, 2.2 MB, package `dev.abhiram.checklist`, versionCode 1, versionName 1.0, minSdk 26, targetSdk 37.
- Signed with `CN=Android Debug` (SHA-256 `667afde3c5c214c5accb2904abfdf4407ab2c110d331581f0f0527b79d9e8764`).
- APK SHA-256: `3d0403aee400a5481fb2ed7f939421a12e1b30961ba4ab8c3588c180ffa49103`.

## Not verified / next steps

The phone was **not connected** during this session (`adb devices` empty, no USB device, no wireless adb). The new release APK was therefore **not installed**; the phone still has the previous build.

When the phone reconnects:

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
adb devices -l
adb -s GAKNWKGQJJJ7QWXW install -r app/build/outputs/apk/release/app-release.apk
```

Then verify on-device:

1. App is black even when the phone is set to light mode; status/navigation icons are visible.
2. Widget renders after the update (if it does not, remove and re-add it).
3. Resize to 2×2, wide, and tall; confirm the header switches to compact at short heights and rows fill the available space.
4. Scroll the widget list to items that used to be hidden behind `+N more`; tap a row after scrolling and confirm it toggles and the widget refreshes.
5. Empty checklist and unbound-widget states.
6. Reboot and process death (`adb shell am kill dev.abhiram.checklist`), then confirm the widget restores.
7. PSS / idle CPU, rapid taps, large font scale.

## Risk / fallback

In-widget scrolling depends on the launcher rendering collection widgets. The standard Android 14 launcher supports it. If the phone's launcher does not, the visible rows still render but cannot scroll to the rest; the fallback would be explicit previous/next controls in the widget.
