# Glance and widget entry points are referenced by name from the manifest and
# action callbacks; keep them when R8 shrinks the release build.
-keep class dev.abhiram.checklist.widget.** { *; }
