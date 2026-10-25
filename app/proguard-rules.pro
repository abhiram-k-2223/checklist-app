# Glance and widget entry points are referenced by name from the manifest and
# action callbacks; keep them when R8 shrinks the release build.
-keep class dev.abhiram.checklist.widget.** { *; }

# Glance uses WorkManager for async widget requests, and WorkManager builds its
# Room database by reflection. Keep the reflection targets; otherwise the
# minified release fails with "Failed to create an instance of WorkDatabase".
-keep class androidx.room.** { *; }
-keep class androidx.work.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-dontwarn androidx.room.**
