# Android Checklist App with Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local-only Android checklist app with a resizable, iOS-dark home-screen widget whose checkboxes toggle items in place.

**Architecture:** Single `app` module. Pure `ChecklistStore` transformations sit behind a `ChecklistRepository` backed by Preferences DataStore containing one JSON document. Compose Material 3 renders the lists and checklist screens; a Glance widget reads a repository snapshot and dispatches checkbox toggles through an `ActionCallback`. Manual application-scoped dependency container, no Hilt/KSP.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3 1.4.0, Glance 1.2.0, Preferences DataStore 1.1.7, kotlinx.serialization 1.11.0, AGP 9.4.1, Gradle 9.8.1, JDK 21, compileSdk/targetSdk 37, minSdk 26.

**Spec:** `docs/superpowers/specs/2026-10-25-android-checklist-widget-design.md`

## Global Constraints

- Package/applicationId: `dev.abhiram.checklist`. App label: `Checklist`. Source root: `app/src/main/kotlin`. Test root: `app/src/test/kotlin`.
- AGP 9 built-in Kotlin: apply only `com.android.application`, `org.jetbrains.kotlin.plugin.compose`, and `org.jetbrains.kotlin.plugin.serialization`. Never apply `org.jetbrains.kotlin.android`.
- `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26`, Java/Kotlin target 17, JDK 21.
- Gradle/AGP versions are pinned: Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Glance 1.2.0, DataStore 1.1.7.
- All commands run from `/home/abhiram/projects/widgets` with:
  ```bash
  export JAVA_HOME=/home/abhiram/.local/opt/jdk21
  ```
- Local-only: no `INTERNET` permission, no network calls, no analytics, no accounts.
- Exactly one checklist may be pinned. Pinning another unpins the previous. Item text and checklist titles are trimmed and non-blank.
- Widget: default 4×4, minimum 2×2, resizable horizontally and vertically with no artificial maximum, `updatePeriodMillis=0`, iOS-dark palette below, unchecked items first then completed, entire row toggles the item, header opens that checklist in the app, empty state opens the app.
- Widget palette: background `#1C1C1E`, separator `#2C2C2E`, muted `#8E8E93`, unchecked ring `#636366`, blue `#0A84FF`, white `#FFFFFF`, 24 dp corners, 14–16 dp padding.
- No Glance `CheckBox`: use custom vector circle/check icons with clickable rows.
- Every data mutation goes through `ChecklistRepository`; no direct DataStore access from UI or widget.
- Commit after every task. Do not commit `local.properties`, `build/`, or `.gradle/`.

## Review Focus

- **Widget toggle with the app process dead** — tapping a widget row after `adb shell am force-stop dev.abhiram.checklist` must start the process, flip the item, and refresh the widget. Verified manually in Task 9.
- **Corrupt persisted JSON** — must never crash or wipe silently; the raw value is backed up under `state_json_corrupt` and the app starts from an empty state. Unit-tested in Tasks 2 and 5.
- **Resize extremes** — 2×2 and very tall/wide widgets must not crash, clip badly, or render more rows than fit. Row math is unit-tested in Task 8 and resized manually in Tasks 8 and 10.
- **Rapid/stale widget taps** — toggling an item that was deleted, or tapping repeatedly, must be a no-op or a consistent toggle, never a crash. Unit-tested in Task 4; manually checked in Task 9.
- **Long text and many items** — long titles/items ellipsize, item text wraps to two lines, and hidden rows collapse into a `+N more` line. Unit-tested in Task 8 and manually checked in Tasks 7 and 10.

---

### Task 1: Project scaffold and verified build

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `local.properties` (machine-local, never committed)
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher.xml`

**Interfaces:**
- Consumes: nothing.
- Produces: a Gradle project where `./gradlew :app:assembleDebug` succeeds, the `ChecklistTheme` composable, and the `MainActivity` entry point.

- [ ] **Step 1: Create the Gradle configuration files**

`.gitignore`:

```gitignore
.gradle/
build/
local.properties
.idea/
*.iml
.kotlin/
captures/
.DS_Store
```

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "checklist"
include(":app")
```

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.workers.max=4
org.gradle.java.home=/home/abhiram/.local/opt/jdk21
kotlin.code.style=official
android.useAndroidX=true
```

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
coreKtx = "1.19.1"
coroutines = "1.11.0"
serialization = "1.11.0"
datastore = "1.1.7"
glance = "1.2.0"
junit4 = "4.13.2"

[libraries]
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-savedstate = { module = "androidx.lifecycle:lifecycle-viewmodel-savedstate", version.ref = "lifecycle" }
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
androidx-glance-appwidget = { module = "androidx.glance:glance-appwidget", version.ref = "glance" }
junit4 = { module = "junit:junit", version.ref = "junit4" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`local.properties` (create it, do not commit):

```properties
sdk.dir=/home/abhiram/Android/Sdk
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.abhiram.checklist"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.abhiram.checklist"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

- [ ] **Step 2: Create the manifest and minimal resources**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Checklist">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Checklist</string>
</resources>
```

`app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="app_window_background">#101012</color>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Checklist" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/app_window_background</item>
    </style>
</resources>
```

`app/src/main/res/drawable/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path
        android:fillColor="#0A84FF"
        android:pathData="M8,4 L40,4 Q44,4 44,8 L44,40 Q44,44 40,44 L8,44 Q4,44 4,40 L4,8 Q4,4 8,4 Z" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M20.5,32.5 L14,26 L16.8,23.2 L20.5,26.9 L30.2,17.2 L33,20 Z" />
</vector>
```

- [ ] **Step 3: Create the minimal Compose entry point and theme**

`app/src/main/kotlin/dev/abhiram/checklist/ui/theme/Theme.kt`:

```kotlin
package dev.abhiram.checklist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IosBlue = Color(0xFF0A84FF)
val IosGray = Color(0xFF8E8E93)

private val DarkColors = darkColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = IosGray,
    outline = Color(0xFF38383A),
    error = Color(0xFFFF453A),
)

private val LightColors = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    outline = Color(0xFFC7C7CC),
    error = Color(0xFFFF3B30),
)

@Composable
fun ChecklistTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
```

`app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt`:

```kotlin
package dev.abhiram.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.abhiram.checklist.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChecklistTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("Checklist")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Generate the Gradle wrapper and build**

Run:

```bash
cd /home/abhiram/projects/widgets
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
GRADLE_BIN=$(echo /home/abhiram/.gradle/wrapper/dists/gradle-9.8.1-bin/*/gradle-9.8.1/bin/gradle)
"$GRADLE_BIN" wrapper --gradle-version 9.8.1 --distribution-type bin
./gradlew --version
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 5: Commit the scaffold**

```bash
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper gradlew gradlew.bat app/build.gradle.kts app/src
git commit -m "build: scaffold Android Compose project"
```

---

### Task 2: Persisted models and JSON codec

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `AppState(version: Int = 1, checklists: List<Checklist> = emptyList())`
  - `Checklist(id, title, isPinned = false, createdAt, updatedAt, items = emptyList())`
  - `ChecklistItem(id, text, isDone = false, createdAt, position)`
  - `ChecklistJson.encode(state: AppState): String`
  - `ChecklistJson.tryDecode(raw: String): AppState?`
  - `ChecklistJson.decode(raw: String?): AppState`

- [ ] **Step 1: Write the failing JSON tests**

`app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt`:

```kotlin
package dev.abhiram.checklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateJsonTest {

    private val sample = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "Groceries",
                isPinned = true,
                createdAt = 1L,
                updatedAt = 2L,
                items = listOf(
                    ChecklistItem(
                        id = "i1",
                        text = "Milk",
                        isDone = true,
                        createdAt = 3L,
                        position = 0,
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `round trip preserves state`() {
        assertEquals(sample, ChecklistJson.decode(ChecklistJson.encode(sample)))
    }

    @Test
    fun `unknown keys are ignored`() {
        val decoded = ChecklistJson.tryDecode(
            """{"version":1,"checklists":[],"future":true}""",
        )
        assertNotNull(decoded)
        assertTrue(decoded!!.checklists.isEmpty())
    }

    @Test
    fun `corrupt json falls back to empty state`() {
        assertEquals(AppState(), ChecklistJson.decode("{not json"))
    }

    @Test
    fun `null raw decodes to empty state`() {
        assertEquals(AppState(), ChecklistJson.decode(null))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.model.AppStateJsonTest'
```

Expected: compilation failure because `AppState`, `Checklist`, `ChecklistItem`, and `ChecklistJson` do not exist yet.

- [ ] **Step 3: Implement the models and codec**

`app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt`:

```kotlin
package dev.abhiram.checklist.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppState(
    val version: Int = 1,
    val checklists: List<Checklist> = emptyList(),
)

@Serializable
data class Checklist(
    val id: String,
    val title: String,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<ChecklistItem> = emptyList(),
)

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false,
    val createdAt: Long,
    val position: Int,
)

object ChecklistJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: AppState): String =
        json.encodeToString(AppState.serializer(), state)

    fun tryDecode(raw: String): AppState? =
        runCatching { json.decodeFromString(AppState.serializer(), raw) }.getOrNull()

    fun decode(raw: String?): AppState = raw?.let(::tryDecode) ?: AppState()
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.model.AppStateJsonTest'
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/data/model/AppState.kt app/src/test/kotlin/dev/abhiram/checklist/data/model/AppStateJsonTest.kt
git commit -m "feat: add checklist models and JSON codec"
```

---

### Task 3: Checklist create, rename, delete, and pin rules

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreChecklistTest.kt`

**Interfaces:**
- Consumes: `AppState`, `Checklist` from Task 2.
- Produces:
  - `ChecklistStore.createChecklist(state, id, title, now): AppState`
  - `ChecklistStore.renameChecklist(state, id, title, now): AppState`
  - `ChecklistStore.deleteChecklist(state, id): AppState`
  - `ChecklistStore.setPinned(state, id: String?, now): AppState`
  - `ChecklistStore.requireText(value, label): String` (internal helper, also used by Task 4)

- [ ] **Step 1: Write the failing checklist-store tests**

`app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreChecklistTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistStoreChecklistTest {

    private fun list(id: String, pinned: Boolean = false) = Checklist(
        id = id,
        title = id,
        isPinned = pinned,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `create appends a trimmed checklist`() {
        val next = ChecklistStore.createChecklist(AppState(), "c1", "  Groceries  ", 5L)
        val created = next.checklists.single()
        assertEquals("Groceries", created.title)
        assertEquals("c1", created.id)
        assertEquals(5L, created.createdAt)
        assertEquals(5L, created.updatedAt)
        assertFalse(created.isPinned)
        assertTrue(created.items.isEmpty())
    }

    @Test
    fun `blank title is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChecklistStore.createChecklist(AppState(), "c1", "   ", 5L)
        }
    }

    @Test
    fun `rename trims and updates timestamp`() {
        val state = AppState(checklists = listOf(list("c1")))
        val next = ChecklistStore.renameChecklist(state, "c1", "  Renamed  ", 9L)
        assertEquals("Renamed", next.checklists.single().title)
        assertEquals(9L, next.checklists.single().updatedAt)
    }

    @Test
    fun `rename unknown list is a no-op`() {
        val state = AppState(checklists = listOf(list("c1")))
        assertEquals(state, ChecklistStore.renameChecklist(state, "missing", "Name", 9L))
    }

    @Test
    fun `delete removes only the target list`() {
        val state = AppState(checklists = listOf(list("c1"), list("c2")))
        val next = ChecklistStore.deleteChecklist(state, "c1")
        assertEquals(listOf("c2"), next.checklists.map { it.id })
    }

    @Test
    fun `pinning a second list unpins the first`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true), list("c2")))
        val next = ChecklistStore.setPinned(state, "c2", 7L)
        assertFalse(next.checklists.first { it.id == "c1" }.isPinned)
        assertTrue(next.checklists.first { it.id == "c2" }.isPinned)
        assertEquals(7L, next.checklists.first { it.id == "c2" }.updatedAt)
    }

    @Test
    fun `pinning an unknown id unpins everything`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true)))
        val next = ChecklistStore.setPinned(state, "missing", 7L)
        assertFalse(next.checklists.single().isPinned)
    }

    @Test
    fun `unpin null clears the pin`() {
        val state = AppState(checklists = listOf(list("c1", pinned = true)))
        val next = ChecklistStore.setPinned(state, null, 7L)
        assertFalse(next.checklists.single().isPinned)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.ChecklistStoreChecklistTest'
```

Expected: compilation failure because `ChecklistStore` does not exist.

- [ ] **Step 3: Implement the checklist operations**

`app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist

object ChecklistStore {

    fun createChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        val created = Checklist(
            id = id,
            title = clean,
            createdAt = now,
            updatedAt = now,
        )
        return state.copy(checklists = state.checklists + created)
    }

    fun renameChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.id == id) {
                    checklist.copy(title = clean, updatedAt = now)
                } else {
                    checklist
                }
            },
        )
    }

    fun deleteChecklist(state: AppState, id: String): AppState =
        state.copy(checklists = state.checklists.filterNot { it.id == id })

    fun setPinned(state: AppState, id: String?, now: Long): AppState {
        val targetExists = id != null && state.checklists.any { it.id == id }
        return state.copy(
            checklists = state.checklists.map { checklist ->
                val shouldPin = targetExists && checklist.id == id
                if (checklist.isPinned == shouldPin) {
                    checklist
                } else {
                    checklist.copy(isPinned = shouldPin, updatedAt = now)
                }
            },
        )
    }

    internal fun requireText(value: String, label: String): String {
        val clean = value.trim()
        require(clean.isNotEmpty()) { "$label must not be blank" }
        return clean
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.ChecklistStoreChecklistTest'
```

Expected: `BUILD SUCCESSFUL`, 8 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreChecklistTest.kt
git commit -m "feat: add checklist CRUD and single-pin rules"
```

---

### Task 4: Item add, edit, toggle, delete, and cascade rules

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt`
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreItemTest.kt`

**Interfaces:**
- Consumes: `ChecklistStore` and models from Tasks 2–3.
- Produces:
  - `ChecklistStore.addItem(state, checklistId, itemId, text, now): AppState`
  - `ChecklistStore.updateItem(state, itemId, text, now): AppState`
  - `ChecklistStore.toggleItem(state, itemId, now): AppState`
  - `ChecklistStore.deleteItem(state, itemId, now): AppState`

- [ ] **Step 1: Write the failing item-store tests**

`app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreItemTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistStoreItemTest {

    private fun item(id: String, done: Boolean = false, position: Int = 0) = ChecklistItem(
        id = id,
        text = id,
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    private fun state(vararg items: ChecklistItem): AppState = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "List",
                createdAt = 1L,
                updatedAt = 1L,
                items = items.toList(),
            ),
        ),
    )

    @Test
    fun `add appends items with increasing positions`() {
        val first = ChecklistStore.addItem(state(), "c1", "i1", "  Milk  ", 5L)
        val second = ChecklistStore.addItem(first, "c1", "i2", "Bread", 6L)
        val items = second.checklists.single().items
        assertEquals(listOf("i1", "i2"), items.map { it.id })
        assertEquals("Milk", items[0].text)
        assertEquals(listOf(0, 1), items.map { it.position })
        assertEquals(6L, second.checklists.single().updatedAt)
    }

    @Test
    fun `blank item text is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChecklistStore.addItem(state(), "c1", "i1", "  ", 5L)
        }
    }

    @Test
    fun `add to a missing checklist is a no-op`() {
        val original = state()
        assertEquals(original, ChecklistStore.addItem(original, "missing", "i1", "Milk", 5L))
    }

    @Test
    fun `update trims text and updates timestamp`() {
        val next = ChecklistStore.updateItem(state(item("i1")), "i1", "  Oat milk  ", 8L)
        assertEquals("Oat milk", next.checklists.single().items.single().text)
        assertEquals(8L, next.checklists.single().updatedAt)
    }

    @Test
    fun `update missing item is a no-op`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.updateItem(original, "missing", "Text", 8L))
    }

    @Test
    fun `toggle flips only the target item and updates its list`() {
        val next = ChecklistStore.toggleItem(state(item("i1"), item("i2")), "i2", 10L)
        assertFalse(next.checklists.single().items.first { it.id == "i1" }.isDone)
        assertTrue(next.checklists.single().items.first { it.id == "i2" }.isDone)
        assertEquals(10L, next.checklists.single().updatedAt)
    }

    @Test
    fun `toggle is a no-op for a missing item`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.toggleItem(original, "missing", 10L))
    }

    @Test
    fun `toggling twice restores the original state`() {
        val original = state(item("i1"))
        val twice = ChecklistStore.toggleItem(ChecklistStore.toggleItem(original, "i1", 2L), "i1", 3L)
        assertFalse(twice.checklists.single().items.single().isDone)
    }

    @Test
    fun `delete removes only the target item`() {
        val next = ChecklistStore.deleteItem(state(item("i1"), item("i2", position = 1)), "i1", 4L)
        assertEquals(listOf("i2"), next.checklists.single().items.map { it.id })
        assertEquals(4L, next.checklists.single().updatedAt)
    }

    @Test
    fun `delete missing item is a no-op`() {
        val original = state(item("i1"))
        assertEquals(original, ChecklistStore.deleteItem(original, "missing", 4L))
    }

    @Test
    fun `deleting a checklist cascades to its items`() {
        val original = state(item("i1"), item("i2", position = 1))
        val next = ChecklistStore.deleteChecklist(original, "c1")
        assertTrue(next.checklists.isEmpty())
    }

    @Test
    fun `item ids are found across different lists`() {
        val original = AppState(
            checklists = listOf(
                Checklist(
                    id = "c1",
                    title = "One",
                    createdAt = 1L,
                    updatedAt = 1L,
                    items = listOf(item("i1")),
                ),
                Checklist(
                    id = "c2",
                    title = "Two",
                    createdAt = 1L,
                    updatedAt = 1L,
                    items = listOf(item("i2")),
                ),
            ),
        )
        val next = ChecklistStore.toggleItem(original, "i2", 2L)
        assertFalse(next.checklists.first { it.id == "c1" }.items.single().isDone)
        assertTrue(next.checklists.first { it.id == "c2" }.items.single().isDone)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.ChecklistStoreItemTest'
```

Expected: compilation failure because the item functions do not exist.

- [ ] **Step 3: Implement the item operations**

Add the import and functions to `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt`. The final file must be:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

object ChecklistStore {

    fun createChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        val created = Checklist(
            id = id,
            title = clean,
            createdAt = now,
            updatedAt = now,
        )
        return state.copy(checklists = state.checklists + created)
    }

    fun renameChecklist(state: AppState, id: String, title: String, now: Long): AppState {
        val clean = requireText(title, "Checklist title")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.id == id) {
                    checklist.copy(title = clean, updatedAt = now)
                } else {
                    checklist
                }
            },
        )
    }

    fun deleteChecklist(state: AppState, id: String): AppState =
        state.copy(checklists = state.checklists.filterNot { it.id == id })

    fun setPinned(state: AppState, id: String?, now: Long): AppState {
        val targetExists = id != null && state.checklists.any { it.id == id }
        return state.copy(
            checklists = state.checklists.map { checklist ->
                val shouldPin = targetExists && checklist.id == id
                if (checklist.isPinned == shouldPin) {
                    checklist
                } else {
                    checklist.copy(isPinned = shouldPin, updatedAt = now)
                }
            },
        )
    }

    fun addItem(
        state: AppState,
        checklistId: String,
        itemId: String,
        text: String,
        now: Long,
    ): AppState {
        val clean = requireText(text, "Item text")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.id != checklistId) {
                    checklist
                } else {
                    val position = (checklist.items.maxOfOrNull { it.position } ?: -1) + 1
                    checklist.copy(
                        items = checklist.items + ChecklistItem(
                            id = itemId,
                            text = clean,
                            createdAt = now,
                            position = position,
                        ),
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun updateItem(state: AppState, itemId: String, text: String, now: Long): AppState {
        val clean = requireText(text, "Item text")
        return state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.map { item ->
                            if (item.id == itemId) item.copy(text = clean) else item
                        },
                        updatedAt = now,
                    )
                }
            },
        )
    }

    fun toggleItem(state: AppState, itemId: String, now: Long): AppState =
        state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.map { item ->
                            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                        },
                        updatedAt = now,
                    )
                }
            },
        )

    fun deleteItem(state: AppState, itemId: String, now: Long): AppState =
        state.copy(
            checklists = state.checklists.map { checklist ->
                if (checklist.items.none { it.id == itemId }) {
                    checklist
                } else {
                    checklist.copy(
                        items = checklist.items.filterNot { it.id == itemId },
                        updatedAt = now,
                    )
                }
            },
        )

    internal fun requireText(value: String, label: String): String {
        val clean = value.trim()
        require(clean.isNotEmpty()) { "$label must not be blank" }
        return clean
    }
}
```

- [ ] **Step 4: Run the store tests to verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.ChecklistStore*'
```

Expected: `BUILD SUCCESSFUL`, 20 tests passed total (8 checklist + 12 item).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistStore.kt app/src/test/kotlin/dev/abhiram/checklist/data/ChecklistStoreItemTest.kt
git commit -m "feat: add checklist item operations"
```

---

### Task 5: DataStore persistence, repository, and application container

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/StatePersistence.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/DataStoreStatePersistence.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml` (add `android:name=".ChecklistApplication"`)
- Test: `app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt`

**Interfaces:**
- Consumes: `ChecklistStore`, `ChecklistJson`, models.
- Produces:
  - `StatePersistence` with `rawState: Flow<String?>` and `suspend fun update(transform: (String?) -> PersistenceUpdate)`
  - `PersistenceUpdate(nextRaw: String, corruptBackup: String? = null)`
  - `ChecklistRepository` with `state`, `snapshot()`, CRUD methods
  - `DefaultChecklistRepository(persistence, scope, now, newId)`
  - `ChecklistApplication.repository`
  - `Context.checklistDataStore`

- [ ] **Step 1: Write the failing repository tests**

`app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import dev.abhiram.checklist.data.model.ChecklistJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultChecklistRepositoryTest {

    private class FakePersistence(initial: String? = null) : StatePersistence {
        val raw = MutableStateFlow(initial)
        val backups = mutableListOf<String>()

        override val rawState: Flow<String?> get() = raw

        override suspend fun update(transform: (String?) -> PersistenceUpdate) {
            val update = transform(raw.value)
            update.corruptBackup?.let(backups::add)
            raw.value = update.nextRaw
        }
    }

    private fun initialState() = AppState(
        checklists = listOf(
            Checklist(
                id = "c1",
                title = "List",
                createdAt = 1L,
                updatedAt = 1L,
                items = listOf(
                    ChecklistItem(
                        id = "i1",
                        text = "Milk",
                        createdAt = 1L,
                        position = 0,
                    ),
                ),
            ),
            Checklist(
                id = "c2",
                title = "Other",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        ),
    )

    @Test
    fun `create checklist persists and returns id`() = runTest {
        val persistence = FakePersistence()
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 100L },
            newId = { "id-1" },
        )

        val id = repository.createChecklist(" Groceries ")

        assertEquals("id-1", id)
        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals("Groceries", state.checklists.single().title)
        assertEquals(100L, state.checklists.single().createdAt)
    }

    @Test
    fun `toggle item works when no state was cached in memory`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.toggleItem("i1")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertTrue(state.checklists.first { it.id == "c1" }.items.single().isDone)
    }

    @Test
    fun `pin replaces the previous pin`() = runTest {
        val initial = initialState().copy(
            checklists = initialState().checklists.map { it.copy(isPinned = it.id == "c1") },
        )
        val persistence = FakePersistence(ChecklistJson.encode(initial))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        repository.setPinned("c2")

        val state = ChecklistJson.decode(persistence.raw.value)
        assertFalse(state.checklists.first { it.id == "c1" }.isPinned)
        assertTrue(state.checklists.first { it.id == "c2" }.isPinned)
    }

    @Test
    fun `corrupt raw state is backed up before replacement`() = runTest {
        val persistence = FakePersistence("{broken")
        val repository = DefaultChecklistRepository(
            persistence = persistence,
            scope = backgroundScope,
            now = { 1L },
            newId = { "id-1" },
        )

        repository.createChecklist("Recovered")

        assertEquals(listOf("{broken"), persistence.backups)
        val state = ChecklistJson.decode(persistence.raw.value)
        assertEquals("Recovered", state.checklists.single().title)
    }

    @Test
    fun `snapshot reads the current persisted state`() = runTest {
        val persistence = FakePersistence(ChecklistJson.encode(initialState()))
        val repository = DefaultChecklistRepository(persistence, backgroundScope)

        val snapshot = repository.snapshot()

        assertEquals(listOf("c1", "c2"), snapshot.checklists.map { it.id })
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.DefaultChecklistRepositoryTest'
```

Expected: compilation failure because `StatePersistence`, `PersistenceUpdate`, `DefaultChecklistRepository`, and `ChecklistRepository` do not exist.

- [ ] **Step 3: Implement persistence and repository**

`app/src/main/kotlin/dev/abhiram/checklist/data/StatePersistence.kt`:

```kotlin
package dev.abhiram.checklist.data

import kotlinx.coroutines.flow.Flow

data class PersistenceUpdate(
    val nextRaw: String,
    val corruptBackup: String? = null,
)

interface StatePersistence {
    val rawState: Flow<String?>
    suspend fun update(transform: (String?) -> PersistenceUpdate)
}
```

`app/src/main/kotlin/dev/abhiram/checklist/data/DataStoreStatePersistence.kt`:

```kotlin
package dev.abhiram.checklist.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreStatePersistence(
    private val dataStore: DataStore<Preferences>,
) : StatePersistence {

    override val rawState: Flow<String?> = dataStore.data.map { it[STATE_JSON] }

    override suspend fun update(transform: (String?) -> PersistenceUpdate) {
        dataStore.edit { preferences ->
            val update = transform(preferences[STATE_JSON])
            update.corruptBackup?.let { preferences[STATE_JSON_CORRUPT] = it }
            preferences[STATE_JSON] = update.nextRaw
        }
    }

    companion object {
        val STATE_JSON = stringPreferencesKey("state_json")
        val STATE_JSON_CORRUPT = stringPreferencesKey("state_json_corrupt")
    }
}
```

`app/src/main/kotlin/dev/abhiram/checklist/data/ChecklistRepository.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import kotlinx.coroutines.flow.StateFlow

interface ChecklistRepository {
    val state: StateFlow<AppState>
    suspend fun snapshot(): AppState
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

`app/src/main/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepository.kt`:

```kotlin
package dev.abhiram.checklist.data

import dev.abhiram.checklist.data.model.AppState
import dev.abhiram.checklist.data.model.ChecklistJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

class DefaultChecklistRepository(
    private val persistence: StatePersistence,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ChecklistRepository {

    override val state: StateFlow<AppState> = persistence.rawState
        .map { ChecklistJson.decode(it) }
        .stateIn(scope, SharingStarted.Eagerly, AppState())

    override suspend fun snapshot(): AppState =
        ChecklistJson.decode(persistence.rawState.first())

    private suspend fun mutate(transform: (AppState) -> AppState) {
        persistence.update { raw ->
            val decoded = raw?.let(ChecklistJson::tryDecode)
            PersistenceUpdate(
                nextRaw = ChecklistJson.encode(transform(decoded ?: AppState())),
                corruptBackup = if (raw != null && decoded == null) raw else null,
            )
        }
    }

    override suspend fun createChecklist(title: String): String {
        val id = newId()
        mutate { ChecklistStore.createChecklist(it, id, title, now()) }
        return id
    }

    override suspend fun renameChecklist(id: String, title: String) {
        mutate { ChecklistStore.renameChecklist(it, id, title, now()) }
    }

    override suspend fun deleteChecklist(id: String) {
        mutate { ChecklistStore.deleteChecklist(it, id) }
    }

    override suspend fun setPinned(id: String?) {
        mutate { ChecklistStore.setPinned(it, id, now()) }
    }

    override suspend fun addItem(checklistId: String, text: String): String {
        val id = newId()
        mutate { ChecklistStore.addItem(it, checklistId, id, text, now()) }
        return id
    }

    override suspend fun updateItem(itemId: String, text: String) {
        mutate { ChecklistStore.updateItem(it, itemId, text, now()) }
    }

    override suspend fun toggleItem(itemId: String) {
        mutate { ChecklistStore.toggleItem(it, itemId, now()) }
    }

    override suspend fun deleteItem(itemId: String) {
        mutate { ChecklistStore.deleteItem(it, itemId, now()) }
    }
}
```

`app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt`:

```kotlin
package dev.abhiram.checklist

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.DataStoreStatePersistence
import dev.abhiram.checklist.data.DefaultChecklistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val Context.checklistDataStore by preferencesDataStore(name = "checklist_state")

class ChecklistApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: ChecklistRepository by lazy {
        DefaultChecklistRepository(
            persistence = DataStoreStatePersistence(checklistDataStore),
            scope = appScope,
        )
    }
}
```

- [ ] **Step 4: Register the application class**

Change the `<application ...>` tag in `app/src/main/AndroidManifest.xml` to include:

```xml
android:name=".ChecklistApplication"
```

The full opening tag must be:

```xml
<application
    android:name=".ChecklistApplication"
    android:allowBackup="true"
    android:icon="@drawable/ic_launcher"
    android:label="@string/app_name"
    android:supportsRtl="true"
    android:theme="@style/Theme.Checklist">
```

- [ ] **Step 5: Run the tests to verify they pass**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.data.DefaultChecklistRepositoryTest'
./gradlew :app:assembleDebug
```

Expected: 5 repository tests pass and the debug APK builds.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/data app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt app/src/main/AndroidManifest.xml app/src/test/kotlin/dev/abhiram/checklist/data/DefaultChecklistRepositoryTest.kt
git commit -m "feat: add DataStore repository and application container"
```

---

### Task 6: ViewModel and the Lists screen

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistViewModel.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/UiFormat.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/components/Dialogs.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt`
- Create: `app/src/main/res/drawable/ic_add.xml`
- Create: `app/src/main/res/drawable/ic_more_vert.xml`
- Create: `app/src/main/res/drawable/ic_pin.xml`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` (temporary selected-list branch)
- Test: manual list CRUD on the phone

**Interfaces:**
- Consumes: `ChecklistRepository`, `Checklist`, `ChecklistItem` from Tasks 2–5.
- Produces:
  - `ChecklistUiState(checklists, selectedId)` with `selectedChecklist`
  - `ChecklistViewModel` with `uiState`, `openChecklist`, `closeChecklist`, `createChecklist`, `renameChecklist`, `deleteChecklist`, `setPinned`, `addItem`, `updateItem`, `toggleItem`, `deleteItem`
  - `ChecklistViewModel.factory(owner, repository): ViewModelProvider.Factory`
  - `TextInputDialog`, `ConfirmDeleteDialog`, `progressLabel`
  - `ListsScreen(checklists, onOpenChecklist, onCreateChecklist, onRenameChecklist, onDeleteChecklist, onSetPinned)`

- [ ] **Step 1: Add the ViewModel**

`app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistViewModel.kt`:

```kotlin
package dev.abhiram.checklist.ui

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.model.Checklist
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val checklists: List<Checklist> = emptyList(),
    val selectedId: String? = null,
) {
    val selectedChecklist: Checklist?
        get() = checklists.firstOrNull { it.id == selectedId }
}

class ChecklistViewModel(
    private val repository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<ChecklistUiState> = combine(repository.state, selectedId) { state, requested ->
        val selected = requested?.takeIf { id -> state.checklists.any { it.id == id } }
        ChecklistUiState(checklists = state.checklists, selectedId = selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChecklistUiState(),
    )

    fun openChecklist(id: String) {
        savedStateHandle[KEY_SELECTED] = id
    }

    fun closeChecklist() {
        savedStateHandle[KEY_SELECTED] = null
    }

    fun createChecklist(title: String) {
        viewModelScope.launch {
            openChecklist(repository.createChecklist(title))
        }
    }

    fun renameChecklist(id: String, title: String) {
        viewModelScope.launch { repository.renameChecklist(id, title) }
    }

    fun deleteChecklist(id: String) {
        viewModelScope.launch {
            repository.deleteChecklist(id)
            if (selectedId.value == id) {
                closeChecklist()
            }
        }
    }

    fun setPinned(id: String?) {
        viewModelScope.launch { repository.setPinned(id) }
    }

    fun addItem(checklistId: String, text: String) {
        viewModelScope.launch { repository.addItem(checklistId, text) }
    }

    fun updateItem(itemId: String, text: String) {
        viewModelScope.launch { repository.updateItem(itemId, text) }
    }

    fun toggleItem(itemId: String) {
        viewModelScope.launch { repository.toggleItem(itemId) }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch { repository.deleteItem(itemId) }
    }

    companion object {
        private const val KEY_SELECTED = "selected_checklist_id"

        fun factory(
            owner: SavedStateRegistryOwner,
            repository: ChecklistRepository,
        ): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, null) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle,
                ): T = ChecklistViewModel(repository, handle) as T
            }
    }
}
```

- [ ] **Step 2: Add the formatting helper and dialogs**

`app/src/main/kotlin/dev/abhiram/checklist/ui/UiFormat.kt`:

```kotlin
package dev.abhiram.checklist.ui

import dev.abhiram.checklist.data.model.Checklist

fun progressLabel(checklist: Checklist): String {
    val done = checklist.items.count { it.isDone }
    val total = checklist.items.size
    return if (total == 0) "No items" else "$done of $total done"
}
```

`app/src/main/kotlin/dev/abhiram/checklist/ui/components/Dialogs.kt`:

```kotlin
package dev.abhiram.checklist.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (value.isNotBlank()) onConfirm(value.trim()) },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
```

- [ ] **Step 3: Add the Lists screen**

`app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog

@Composable
fun ListsScreen(
    checklists: List<Checklist>,
    onOpenChecklist: (String) -> Unit,
    onCreateChecklist: (String) -> Unit,
    onRenameChecklist: (String, String) -> Unit,
    onDeleteChecklist: (String) -> Unit,
    onSetPinned: (String?) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Checklist?>(null) }
    var deleteTarget by remember { mutableStateOf<Checklist?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Checklists") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "New checklist",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(checklists, key = { it.id }) { checklist ->
                    ListCard(
                        checklist = checklist,
                        onOpen = { onOpenChecklist(checklist.id) },
                        onRename = { renameTarget = checklist },
                        onPinToggle = {
                            onSetPinned(if (checklist.isPinned) null else checklist.id)
                        },
                        onRequestDelete = { deleteTarget = checklist },
                    )
                }
            }

            if (checklists.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No checklists yet",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap + to make your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New checklist",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { title ->
                showCreate = false
                onCreateChecklist(title)
            },
        )
    }

    renameTarget?.let { checklist ->
        TextInputDialog(
            title = "Rename checklist",
            initialValue = checklist.title,
            confirmLabel = "Save",
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                renameTarget = null
                onRenameChecklist(checklist.id, title)
            },
        )
    }

    deleteTarget?.let { checklist ->
        ConfirmDeleteDialog(
            title = "Delete checklist?",
            message = "\"${checklist.title}\" and its items will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteChecklist(checklist.id)
            },
        )
    }
}

@Composable
private fun ListCard(
    checklist: Checklist,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onPinToggle: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checklist.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = checklist.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = progressLabel(checklist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "List actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (checklist.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            menuOpen = false
                            onPinToggle()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onRequestDelete()
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Add the icon resources**

`app/src/main/res/drawable/ic_add.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
</vector>
```

`app/src/main/res/drawable/ic_more_vert.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z" />
</vector>
```

`app/src/main/res/drawable/ic_pin.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M16,9V4l1,0c0.55,0 1,-0.45 1,-1v0c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3v0c0,0.55 0.45,1 1,1l1,0v5c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1 1,-1v-7H19v-2c-1.66,0 -3,-1.34 -3,-3z" />
</vector>
```

- [ ] **Step 5: Wire the temporary host activity**

Replace `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` with:

```kotlin
package dev.abhiram.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.ListsScreen
import dev.abhiram.checklist.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val selected = uiState.selectedChecklist
                    if (selected == null) {
                        ListsScreen(
                            checklists = uiState.checklists,
                            onOpenChecklist = viewModel::openChecklist,
                            onCreateChecklist = viewModel::createChecklist,
                            onRenameChecklist = viewModel::renameChecklist,
                            onDeleteChecklist = viewModel::deleteChecklist,
                            onSetPinned = viewModel::setPinned,
                        )
                    } else {
                        BackHandler { viewModel.closeChecklist() }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = selected.title,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

This temporary branch is replaced in Task 7. It exists so list CRUD can be tested on the phone before the item screen is written.

- [ ] **Step 6: Build and install**

Run:

```bash
./gradlew :app:assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.abhiram.checklist/.MainActivity
```

Expected: build succeeds, install succeeds, the Lists screen is shown.

- [ ] **Step 7: Verify list behavior on the phone**

1. Tap **+**, type `Groceries`, tap **Create**; the list appears with `No items`.
2. Create a second list `Work`.
3. Rename `Groceries` to `Shopping`; the title updates.
4. Pin `Shopping`; the pin appears. Pin `Work`; the pin moves and `Shopping` loses it.
5. Unpin `Work`; no list is pinned.
6. Tap `Shopping`; a centered title appears. Press system back; lists return.
7. Delete `Work` through the menu and confirm; only `Shopping` remains.

If no phone is connected, record that only the build was verified and continue.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistViewModel.kt app/src/main/kotlin/dev/abhiram/checklist/ui/UiFormat.kt app/src/main/kotlin/dev/abhiram/checklist/ui/components/Dialogs.kt app/src/main/kotlin/dev/abhiram/checklist/ui/ListsScreen.kt app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt app/src/main/res/drawable/ic_add.xml app/src/main/res/drawable/ic_more_vert.xml app/src/main/res/drawable/ic_pin.xml
git commit -m "feat: add lists screen and view model"
```

---

### Task 7: Checklist screen and navigation

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistScreen.kt`
- Create: `app/src/main/res/drawable/ic_arrow_back.xml`
- Create: `app/src/main/res/drawable/ic_circle_outline.xml`
- Create: `app/src/main/res/drawable/ic_check_circle_filled.xml`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` (use `ChecklistApp`)
- Test: manual item CRUD on the phone

**Interfaces:**
- Consumes: `ChecklistViewModel`, `Checklist`, `ChecklistItem`, `ListsScreen`, dialogs.
- Produces:
  - `ChecklistApp(viewModel)`
  - `ChecklistScreen(checklist, onBack, onAddItem, onUpdateItem, onToggleItem, onDeleteItem, onRenameChecklist, onDeleteChecklist, onTogglePin)`
  - `ChecklistItemRow(item, onToggle, onEdit, onRequestDelete)`

- [ ] **Step 1: Add the root composable**

`app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt`:

```kotlin
package dev.abhiram.checklist.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChecklistApp(viewModel: ChecklistViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = uiState.selectedChecklist

    if (selected == null) {
        ListsScreen(
            checklists = uiState.checklists,
            onOpenChecklist = viewModel::openChecklist,
            onCreateChecklist = viewModel::createChecklist,
            onRenameChecklist = viewModel::renameChecklist,
            onDeleteChecklist = viewModel::deleteChecklist,
            onSetPinned = viewModel::setPinned,
        )
    } else {
        BackHandler { viewModel.closeChecklist() }
        ChecklistScreen(
            checklist = selected,
            onBack = viewModel::closeChecklist,
            onAddItem = { text -> viewModel.addItem(selected.id, text) },
            onUpdateItem = viewModel::updateItem,
            onToggleItem = viewModel::toggleItem,
            onDeleteItem = viewModel::deleteItem,
            onRenameChecklist = { title -> viewModel.renameChecklist(selected.id, title) },
            onDeleteChecklist = { viewModel.deleteChecklist(selected.id) },
            onTogglePin = {
                viewModel.setPinned(if (selected.isPinned) null else selected.id)
            },
        )
    }
}
```

- [ ] **Step 2: Add the Checklist screen**

`app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistScreen.kt`:

```kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.abhiram.checklist.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import dev.abhiram.checklist.ui.components.ConfirmDeleteDialog
import dev.abhiram.checklist.ui.components.TextInputDialog

@Composable
fun ChecklistScreen(
    checklist: Checklist,
    onBack: () -> Unit,
    onAddItem: (String) -> Unit,
    onUpdateItem: (String, String) -> Unit,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRenameChecklist: (String) -> Unit,
    onDeleteChecklist: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val orderedItems = remember(checklist.items) {
        checklist.items.sortedBy { it.position }
    }
    val done = remember(checklist.items) { checklist.items.count { it.isDone } }
    val total = checklist.items.size

    var addText by rememberSaveable { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ChecklistItem?>(null) }
    var deleteTarget by remember { mutableStateOf<ChecklistItem?>(null) }
    var checklistMenuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val submit: () -> Unit = {
        val clean = addText.trim()
        if (clean.isNotEmpty()) {
            onAddItem(clean)
            addText = ""
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = checklist.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (total > 0) {
                            Text(
                                text = "$done of $total done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { checklistMenuOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Checklist actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = checklistMenuOpen,
                            onDismissRequest = { checklistMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (checklist.isPinned) "Unpin" else "Pin") },
                                onClick = {
                                    checklistMenuOpen = false
                                    onTogglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    checklistMenuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    checklistMenuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = addText,
                        onValueChange = { addText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Add an item") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                    Spacer(Modifier.size(10.dp))
                    FilledIconButton(
                        onClick = submit,
                        enabled = addText.isNotBlank(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add item",
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (orderedItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Add the first item below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
            ) {
                items(orderedItems, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        onToggle = { onToggleItem(item.id) },
                        onEdit = { editingItem = item },
                        onRequestDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }

    editingItem?.let { item ->
        TextInputDialog(
            title = "Edit item",
            initialValue = item.text,
            confirmLabel = "Save",
            onDismiss = { editingItem = null },
            onConfirm = { text ->
                editingItem = null
                onUpdateItem(item.id, text)
            },
        )
    }

    deleteTarget?.let { item ->
        ConfirmDeleteDialog(
            title = "Delete item?",
            message = "\"${item.text}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteItem(item.id)
            },
        )
    }

    if (showRename) {
        TextInputDialog(
            title = "Rename checklist",
            initialValue = checklist.title,
            confirmLabel = "Save",
            onDismiss = { showRename = false },
            onConfirm = { title ->
                showRename = false
                onRenameChecklist(title)
            },
        )
    }

    if (showDelete) {
        ConfirmDeleteDialog(
            title = "Delete checklist?",
            message = "\"${checklist.title}\" and all of its items will be removed.",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDeleteChecklist()
            },
        )
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle) {
            Image(
                painter = painterResource(
                    if (item.isDone) {
                        R.drawable.ic_check_circle_filled
                    } else {
                        R.drawable.ic_circle_outline
                    },
                ),
                contentDescription = if (item.isDone) "Mark not done" else "Mark done",
            )
        }
        Text(
            text = item.text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium,
            color = if (item.isDone) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Item actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onRequestDelete()
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Add the item icons**

`app/src/main/res/drawable/ic_arrow_back.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
</vector>
```

`app/src/main/res/drawable/ic_circle_outline.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z" />
</vector>
```

`app/src/main/res/drawable/ic_check_circle_filled.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z" />
</vector>
```

- [ ] **Step 4: Replace the temporary activity branch**

Replace `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` with:

```kotlin
package dev.abhiram.checklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.abhiram.checklist.ui.ChecklistApp
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChecklistApp(viewModel)
                }
            }
        }
    }
}
```

- [ ] **Step 5: Build and install**

Run:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.abhiram.checklist/.MainActivity
```

Expected: build succeeds and the app launches.

- [ ] **Step 6: Verify item behavior on the phone**

1. Open a checklist; the empty state appears.
2. Add `Milk`, `Bread`, `Eggs`; each appears at the bottom.
3. Toggle `Bread` done; it stays in its original position, gains a filled check, and is struck through. (Only the home-screen widget moves completed items to the bottom.)
4. Un-toggle `Bread`; the check and strikethrough clear.
5. Tap `Milk`; edit it to `Oat milk`; the text updates.
6. Open the row menu for `Eggs`, choose **Delete**, confirm; it disappears.
7. Open the top-bar menu; check **Pin**/**Unpin**, **Rename**, and **Delete** all work. Delete returns to the lists screen.
8. Press system back; the lists screen returns with the correct progress count.
9. Rotate the phone on the items screen; the list remains and the typed add-bar text survives.

If no phone is connected, record that only the build was verified and continue.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistApp.kt app/src/main/kotlin/dev/abhiram/checklist/ui/ChecklistScreen.kt app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt app/src/main/res/drawable/ic_arrow_back.xml app/src/main/res/drawable/ic_circle_outline.xml app/src/main/res/drawable/ic_check_circle_filled.xml
git commit -m "feat: add checklist screen and navigation"
```

---

### Task 8: Static Glance widget and resize-safe row math

**Files:**
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetRows.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt`
- Create: `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetReceiver.kt`
- Create: `app/src/main/res/xml/checklist_widget_info.xml`
- Create: `app/src/main/res/drawable/ic_widget_circle.xml`
- Create: `app/src/main/res/drawable/ic_widget_check.xml`
- Modify: `app/src/main/res/values/strings.xml` (add widget label)
- Modify: `app/src/main/AndroidManifest.xml` (register the receiver)
- Test: `app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetRowsTest.kt`

**Interfaces:**
- Consumes: `Checklist`, `ChecklistItem`, `ChecklistApplication.repository`.
- Produces:
  - `WidgetRowSet(visible, hiddenCount)`
  - `widgetRows(checklist, maxRows): WidgetRowSet`
  - `maxWidgetRows(heightDp: Float): Int`
  - `ChecklistWidget`, `ChecklistWidgetReceiver`

- [ ] **Step 1: Write the failing row-math tests**

`app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetRowsTest.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRowsTest {

    private fun item(id: String, done: Boolean, position: Int) = ChecklistItem(
        id = id,
        text = id,
        isDone = done,
        createdAt = 1L,
        position = position,
    )

    private fun checklistOf(vararg items: ChecklistItem) = Checklist(
        id = "c1",
        title = "List",
        createdAt = 1L,
        updatedAt = 1L,
        items = items.toList(),
    )

    @Test
    fun `unchecked items are listed before completed items`() {
        val checklist = checklistOf(
            item("a", done = true, position = 0),
            item("b", done = false, position = 1),
            item("c", done = true, position = 2),
            item("d", done = false, position = 3),
        )

        val rows = widgetRows(checklist, maxRows = 10)

        assertEquals(listOf("b", "d", "a", "c"), rows.visible.map { it.id })
        assertEquals(0, rows.hiddenCount)
    }

    @Test
    fun `positions order items within each group`() {
        val checklist = checklistOf(
            item("late", done = false, position = 2),
            item("early", done = false, position = 0),
            item("middle", done = false, position = 1),
        )

        val rows = widgetRows(checklist, maxRows = 10)

        assertEquals(listOf("early", "middle", "late"), rows.visible.map { it.id })
    }

    @Test
    fun `extra rows are counted`() {
        val checklist = checklistOf(
            item("a", done = false, position = 0),
            item("b", done = false, position = 1),
            item("c", done = false, position = 2),
            item("d", done = false, position = 3),
            item("e", done = false, position = 4),
        )

        val rows = widgetRows(checklist, maxRows = 3)

        assertEquals(listOf("a", "b", "c"), rows.visible.map { it.id })
        assertEquals(2, rows.hiddenCount)
    }

    @Test
    fun `zero max rows still shows one row`() {
        val rows = widgetRows(checklistOf(item("a", false, 0)), maxRows = 0)

        assertEquals(1, rows.visible.size)
        assertEquals(0, rows.hiddenCount)
    }

    @Test
    fun `empty checklist has no rows`() {
        val rows = widgetRows(checklistOf(), maxRows = 5)

        assertEquals(emptyList<ChecklistItem>(), rows.visible)
        assertEquals(0, rows.hiddenCount)
    }

    @Test
    fun `row capacity grows with the widget height`() {
        assertEquals(1, maxWidgetRows(120f))
        assertEquals(3, maxWidgetRows(200f))
        assertEquals(5, maxWidgetRows(300f))
        assertEquals(8, maxWidgetRows(400f))
        assertEquals(12, maxWidgetRows(600f))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.WidgetRowsTest'
```

Expected: compilation failure because `widgetRows`, `WidgetRowSet`, and `maxWidgetRows` do not exist.

- [ ] **Step 3: Implement the row math**

`app/src/main/kotlin/dev/abhiram/checklist/widget/WidgetRows.kt`:

```kotlin
package dev.abhiram.checklist.widget

import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

data class WidgetRowSet(
    val visible: List<ChecklistItem>,
    val hiddenCount: Int,
)

fun widgetRows(checklist: Checklist, maxRows: Int): WidgetRowSet {
    val sorted = checklist.items.sortedWith(
        compareBy<ChecklistItem> { it.isDone }.thenBy { it.position },
    )
    val cap = maxRows.coerceAtLeast(1)
    return WidgetRowSet(
        visible = sorted.take(cap),
        hiddenCount = (sorted.size - cap).coerceAtLeast(0),
    )
}

fun maxWidgetRows(heightDp: Float): Int {
    val availableForRows = (heightDp - 72f).coerceAtLeast(0f)
    return (availableForRows / 41f).toInt().coerceIn(1, 24)
}
```

- [ ] **Step 4: Add the widget drawables**

`app/src/main/res/drawable/ic_widget_circle.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="22dp"
    android:height="22dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF636366"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z" />
</vector>
```

`app/src/main/res/drawable/ic_widget_check.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="22dp"
    android:height="22dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF0A84FF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2z" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z" />
</vector>
```

- [ ] **Step 5: Implement the static widget renderer**

`app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt`:

```kotlin
package dev.abhiram.checklist.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

private val WidgetBackground = Color(0xFF1C1C1E)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)

class ChecklistWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        val checklist = repository.snapshot().checklists.firstOrNull { it.isPinned }
        provideContent {
            WidgetContent(checklist = checklist)
        }
    }
}

@Composable
private fun WidgetContent(checklist: Checklist?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProviderCompat(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(16.dp),
    ) {
        if (checklist == null) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pin a checklist",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetText),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Open the app to choose one",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            val done = checklist.items.count { it.isDone }
            val total = checklist.items.size
            val size = LocalSize.current
            val rowSet = widgetRows(checklist, maxWidgetRows(size.height.value))

            Text(
                text = checklist.title,
                style = TextStyle(
                    color = ColorProviderCompat(WidgetText),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (total == 0) "No items" else "$done/$total",
                style = TextStyle(
                    color = ColorProviderCompat(WidgetMuted),
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(10.dp))
            Spacer(
                GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProviderCompat(WidgetSeparator)),
            )

            if (rowSet.visible.isEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "Nothing to do yet",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                )
            } else {
                rowSet.visible.forEach { item ->
                    WidgetItemRow(item)
                    Spacer(
                        GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProviderCompat(WidgetSeparator)),
                    )
                }
                if (rowSet.hiddenCount > 0) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = "+${rowSet.hiddenCount} more",
                        style = TextStyle(
                            color = ColorProviderCompat(WidgetMuted),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetItemRow(item: ChecklistItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(
                if (item.isDone) R.drawable.ic_widget_check else R.drawable.ic_widget_circle,
            ),
            contentDescription = if (item.isDone) "Done" else "Not done",
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = item.text,
            style = TextStyle(
                color = ColorProviderCompat(if (item.isDone) WidgetMuted else WidgetText),
                fontSize = 15.sp,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            ),
            maxLines = 2,
        )
    }
}

private fun ColorProviderCompat(color: Color) = androidx.glance.unit.ColorProvider(color)
```

The local `ColorProviderCompat` wrapper avoids a name clash with any Compose import and keeps the widget colors in one place.

- [ ] **Step 6: Add the receiver and app-widget metadata**

`app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidgetReceiver.kt`:

```kotlin
package dev.abhiram.checklist.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistWidget()
}
```

`app/src/main/res/xml/checklist_widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="250dp"
    android:minHeight="250dp"
    android:minResizeWidth="110dp"
    android:minResizeHeight="110dp"
    android:targetCellWidth="4"
    android:targetCellHeight="4"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

Add to `app/src/main/res/values/strings.xml`:

```xml
<string name="widget_label">Checklist</string>
```

Add this receiver inside `<application>` in `app/src/main/AndroidManifest.xml`, after the activity:

```xml
<receiver
    android:name=".widget.ChecklistWidgetReceiver"
    android:exported="true"
    android:label="@string/widget_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/checklist_widget_info" />
</receiver>
```

- [ ] **Step 7: Run the row tests and build**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests 'dev.abhiram.checklist.widget.WidgetRowsTest'
./gradlew :app:assembleDebug
```

Expected: 6 widget tests pass and the debug APK builds.

- [ ] **Step 8: Add the widget and verify the static render**

Run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On the phone:

1. Pin a checklist with several unchecked and completed items in the app.
2. Long-press the home screen, open **Widgets**, find **Checklist**, and add it.
3. Confirm the default size is 4×4, the background is flat `#1C1C1E`, corners are rounded, and the title and progress are visible.
4. Resize down to 2×2; confirm it does not crash and shows a reduced set of rows.
5. Resize very tall and very wide; confirm rows stop at the computed cap and `+N more` appears when items are hidden.
6. Unpin the checklist in the app; the widget shows **Pin a checklist**. Re-pin it.

Taps are still inert in this task; Task 9 adds them. If no phone is connected, record that only the build was verified.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/widget app/src/main/res/xml/checklist_widget_info.xml app/src/main/res/drawable/ic_widget_circle.xml app/src/main/res/drawable/ic_widget_check.xml app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml app/src/test/kotlin/dev/abhiram/checklist/widget/WidgetRowsTest.kt
git commit -m "feat: add resizable Glance checklist widget"
```

---

### Task 9: Widget taps, deep link, and live refresh

**Files:**
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt`
- Modify: `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt`
- Test: manual widget interaction and force-stop recovery

**Interfaces:**
- Consumes: `ChecklistRepository`, `ChecklistWidget`.
- Produces:
  - `ChecklistWidget.EXTRA_OPEN_CHECKLIST`
  - `ToggleItemAction` callback
  - app-scoped widget refresh observer

- [ ] **Step 1: Add tap actions and the toggle callback**

Replace `app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt` with:

```kotlin
package dev.abhiram.checklist.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import dev.abhiram.checklist.ChecklistApplication
import dev.abhiram.checklist.MainActivity
import dev.abhiram.checklist.R
import dev.abhiram.checklist.data.model.Checklist
import dev.abhiram.checklist.data.model.ChecklistItem

private val WidgetBackground = Color(0xFF1C1C1E)
private val WidgetSeparator = Color(0xFF2C2C2E)
private val WidgetMuted = Color(0xFF8E8E93)
private val WidgetText = Color(0xFFFFFFFF)

private val ITEM_ID_KEY = ActionParameters.Key<String>("item_id")

class ChecklistWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as ChecklistApplication).repository
        val checklist = repository.snapshot().checklists.firstOrNull { it.isPinned }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            checklist?.let { putExtra(EXTRA_OPEN_CHECKLIST, it.id) }
        }
        provideContent {
            WidgetContent(checklist = checklist, openIntent = openIntent)
        }
    }

    companion object {
        const val EXTRA_OPEN_CHECKLIST = "open_checklist_id"
    }
}

class ToggleItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[ITEM_ID_KEY] ?: return
        val app = context.applicationContext as ChecklistApplication
        app.repository.toggleItem(itemId)
        ChecklistWidget().updateAll(context)
    }
}

@Composable
private fun WidgetContent(checklist: Checklist?, openIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProviderCompat(WidgetBackground))
            .cornerRadius(24.dp)
            .padding(16.dp),
    ) {
        if (checklist == null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(openIntent)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pin a checklist",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetText),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "Open the app to choose one",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            val done = checklist.items.count { it.isDone }
            val total = checklist.items.size
            val size = LocalSize.current
            val rowSet = widgetRows(checklist, maxWidgetRows(size.height.value))

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openIntent)),
            ) {
                Text(
                    text = checklist.title,
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetText),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (total == 0) "No items" else "$done/$total",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Spacer(
                GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProviderCompat(WidgetSeparator)),
            )

            if (rowSet.visible.isEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    text = "Nothing to do yet",
                    style = TextStyle(
                        color = ColorProviderCompat(WidgetMuted),
                        fontSize = 13.sp,
                    ),
                )
            } else {
                rowSet.visible.forEach { item ->
                    WidgetItemRow(item)
                    Spacer(
                        GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ColorProviderCompat(WidgetSeparator)),
                    )
                }
                if (rowSet.hiddenCount > 0) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = "+${rowSet.hiddenCount} more",
                        style = TextStyle(
                            color = ColorProviderCompat(WidgetMuted),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetItemRow(item: ChecklistItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionRunCallback<ToggleItemAction>(
                    actionParametersOf(ITEM_ID_KEY to item.id),
                ),
            )
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(
                if (item.isDone) R.drawable.ic_widget_check else R.drawable.ic_widget_circle,
            ),
            contentDescription = if (item.isDone) "Done" else "Not done",
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = item.text,
            style = TextStyle(
                color = ColorProviderCompat(if (item.isDone) WidgetMuted else WidgetText),
                fontSize = 15.sp,
                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
            ),
            maxLines = 2,
        )
    }
}

private fun ColorProviderCompat(color: Color) = androidx.glance.unit.ColorProvider(color)
```

- [ ] **Step 2: Refresh widgets whenever app data changes**

Replace `app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt` with:

```kotlin
package dev.abhiram.checklist

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import dev.abhiram.checklist.data.ChecklistRepository
import dev.abhiram.checklist.data.DataStoreStatePersistence
import dev.abhiram.checklist.data.DefaultChecklistRepository
import dev.abhiram.checklist.widget.ChecklistWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val Context.checklistDataStore by preferencesDataStore(name = "checklist_state")

class ChecklistApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: ChecklistRepository by lazy {
        DefaultChecklistRepository(
            persistence = DataStoreStatePersistence(checklistDataStore),
            scope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.state.collect {
                ChecklistWidget().updateAll(this@ChecklistApplication)
            }
        }
    }
}
```

- [ ] **Step 3: Open the pinned checklist from the header**

Replace `app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt` with:

```kotlin
package dev.abhiram.checklist

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.abhiram.checklist.ui.ChecklistApp
import dev.abhiram.checklist.ui.ChecklistViewModel
import dev.abhiram.checklist.ui.theme.ChecklistTheme
import dev.abhiram.checklist.widget.ChecklistWidget

class MainActivity : ComponentActivity() {

    private val viewModel: ChecklistViewModel by viewModels {
        val app = application as ChecklistApplication
        ChecklistViewModel.factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openFromIntent(intent)
        setContent {
            ChecklistTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ChecklistApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    private fun openFromIntent(intent: Intent?) {
        intent?.getStringExtra(ChecklistWidget.EXTRA_OPEN_CHECKLIST)
            ?.let(viewModel::openChecklist)
    }
}
```

- [ ] **Step 4: Build and install**

Run:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: build succeeds and the app installs over the previous version without losing data.

- [ ] **Step 5: Verify widget interaction on the phone**

1. Add the widget if it is not already there.
2. Tap an unchecked row; it becomes checked, moves below the unchecked rows, and the progress count increases.
3. Tap a completed row; it becomes unchecked and returns above the completed rows.
4. Tap the widget header; the app opens on that checklist.
5. Press back; the lists screen appears. Open the checklist in the app; the widget state matches.
6. Unpin the pinned list in the app; the widget shows **Pin a checklist**. Tap the empty state; the app opens.
7. Run `adb shell am force-stop dev.abhiram.checklist`, then tap a widget row. The process starts, the item toggles, and the widget refreshes.
8. Tap the same row repeatedly; it toggles consistently and never crashes.
9. Change the checklist in the app; the widget updates without reopening it.

If no phone is connected, record which steps could not be verified and continue.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/dev/abhiram/checklist/widget/ChecklistWidget.kt app/src/main/kotlin/dev/abhiram/checklist/ChecklistApplication.kt app/src/main/kotlin/dev/abhiram/checklist/MainActivity.kt
git commit -m "feat: toggle items from the widget and refresh live"
```

---

### Task 10: Documentation, full verification, and final commit

**Files:**
- Create: `README.md`
- Modify: any file needed for defects found during verification
- Test: full JVM test suite, debug build, manual phone matrix

**Interfaces:**
- Consumes: the complete app.
- Produces: a documented, verified, committed project.

- [ ] **Step 1: Write the README**

`README.md`:

````markdown
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

## Build

Requires JDK 21 and the Android SDK with platform 37.

```bash
export JAVA_HOME=/home/abhiram/.local/opt/jdk21
./gradlew :app:assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone

Enable USB debugging, connect the phone, then:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.abhiram.checklist/.MainActivity
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
- `widget` — Glance renderer, row math, and action callback

There is no Room, KSP, Hilt, or navigation library; the app is intentionally
small and builds quickly on a low-memory machine.

## Limitations

- No cloud sync, reminders, due dates, tags, drag reorder, or images
- No widget text entry
- Widget taps toggle items only; all editing happens in the app
````

- [ ] **Step 2: Run the complete automated verification**

Run:

```bash
./gradlew test
./gradlew :app:assembleDebug
```

Expected: every unit test passes and the debug APK builds with no errors.

- [ ] **Step 3: Run the full manual matrix on the phone**

Verify each line and record the result:

| Area | Check |
| --- | --- |
| Lists | Create, rename, delete, pin, unpin; only one pin at a time |
| Items | Add, edit, toggle, delete; stored order in the app; widget shows unchecked above completed |
| Rotation | Lists and items screens survive rotation; add-bar text survives |
| Themes | Toggle system dark/light; the app follows, the widget stays iOS-dark |
| Reboot | Reboot the phone; checklists, items, and the widget content persist |
| Widget default | Adds at 4×4 with dark flat surfaces and rounded corners |
| Widget 2×2 | No crash; title, progress, and a reduced row set render |
| Widget tall/wide | No crash; `+N more` appears instead of unbounded rows |
| Widget tap | Row toggles and moves; progress updates; works with the app backgrounded |
| Widget header | Opens the pinned checklist directly |
| Widget empty | Unpin or delete the pinned list; the widget shows the empty state and opens the app |
| Process death | Force-stop the app, then tap a widget row; it toggles |
| Long text | Long titles ellipsize; long item text wraps to two lines in the app |
| Many items | Widget caps rows and shows `+N more` |

- [ ] **Step 4: Fix any defect found and rerun the affected checks**

For each failure, make the smallest code change, rerun the focused test or manual step, then rerun Step 2 before continuing.

- [ ] **Step 5: Confirm the repository is clean**

Run:

```bash
git status --short
git log --oneline -5
```

Expected: `git status` lists only `README.md` (and any deliberate fixes); recent commits are one per task.

- [ ] **Step 6: Commit the documentation and final fixes**

```bash
git add README.md
git commit -m "docs: add README and verification notes"
```

If Step 4 changed code, include those paths in `git add` and use a message that describes the fix instead.

---

## Plan complete

All ten tasks produce a working, testable increment. Tasks 1–5 build and test the data layer without a phone; Tasks 6–7 add the app UI; Tasks 8–9 add and connect the widget; Task 10 verifies the whole thing.


