# Tono — Developer Reference

Tono is an Android to-do app that looks and feels like a plain-text editor.
See [`README.md`](README.md) for the full product spec and design system.

---

## Environment Setup

### 1 — JDK 17

```bash
# Ubuntu / Debian / WSL
sudo apt update && sudo apt install openjdk-17-jdk

# Verify
java -version   # must print openjdk 17.*
```

Set `JAVA_HOME` if it is not picked up automatically:

```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### 2 — Android SDK (command-line tools)

```bash
# Download the latest command-line tools zip from:
# https://developer.android.com/studio#command-line-tools-only
# Example (version may differ):
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
unzip ~/Downloads/commandlinetools-linux-*.zip
mv cmdline-tools latest     # sdkmanager requires this exact path

# Accept licenses and install required packages
~/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/android-sdk/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"
```

Add to `~/.bashrc` (or `~/.zshrc`):

```bash
export ANDROID_HOME=~/android-sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

> **WSL note:** The Android emulator does not run inside WSL. Build the APK here,
> then install it on a physical device or an emulator running on your Windows host.

### 3 — Android Studio (alternative, easier)

Download Android Studio for Windows/Mac from <https://developer.android.com/studio>,
open the project root (`Tono/`), and let the IDE handle SDK installation.
Use the IDE's built-in run button to launch on an emulator or physical device.

---

## Build

All commands run from the repository root (`/home/cpu/code/Tono`).

```bash
# Debug APK (fastest, no signing required)
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (unsigned)
./gradlew assembleRelease

# Build + run unit tests
./gradlew test

# Full check (build + lint + tests)
./gradlew check
```

### First-run Gradle bootstrap

The Gradle wrapper (`gradlew`) downloads Gradle 8.9 automatically on first run.
This requires an internet connection and may take a minute.

---

## Run on Device

### Physical device (USB)

```bash
# Enable USB debugging on the phone:
# Settings → About → tap Build Number 7× → Developer Options → USB Debugging ON

adb devices          # verify device is listed
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Emulator from Windows host (WSL workflow)

1. Create an emulator in Android Studio (AVD Manager), API 35, x86_64.
2. Start the emulator.
3. In WSL, point ADB at the Windows host emulator:
   ```bash
   export ANDROID_ADB_SERVER_ADDRESS=host.docker.internal   # or the host IP
   adb connect 127.0.0.1:5554
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### One-shot install+launch (connected device)

```bash
./gradlew installDebug
adb shell am start -n org.volkov.tono/.MainActivity
```

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── kotlin/org/volkov/tono/
│   ├── MainActivity.kt              entry point, edge-to-edge
│   ├── data/
│   │   ├── Task.kt                  Room entity (id, dayKey, position, text)
│   │   ├── TaskDao.kt               Flow<List<Task>>, suspend insert/delete/update
│   │   └── TonoDatabase.kt          singleton Room DB ("tono.db")
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt             TonoColors data class + Light/Dark instances
│   │   │   ├── Type.kt              JetBrainsMono FontFamily + TonoType text styles
│   │   │   └── Theme.kt             TonoTheme composable + LocalTonoColors
│   │   ├── TonoViewModel.kt         all state + business logic
│   │   ├── TonoScreen.kt            root LazyColumn screen
│   │   └── components/
│   │       ├── StatusStrip.kt       top date-range / app-name strip
│   │       ├── WeekDivider.kt       "— next week —" separator
│   │       ├── DaySection.kt        day heading + hairline + task list
│   │       ├── TaskRow.kt           live task, swipe-right to complete
│   │       ├── GhostRow.kt          completed task, swipe-left to undo
│   │       ├── EmptyRow.kt          invitation line with blinking cursor
│   │       └── EditingRow.kt        active BasicTextField input
│   └── util/
│       └── DayWindow.kt             computeDayWindow() → 14 LocalDates
└── res/
    ├── font/                        JetBrains Mono TTF (regular/medium/bold)
    ├── values/strings.xml
    ├── values/themes.xml            minimal Material window theme
    └── mipmap-anydpi{,-v26}/        adaptive launcher icon
```

---

## Architecture

### Data flow

```
Room DB ──Flow──► TonoViewModel (StateFlow<TonoUiState>)
                        │
                        ▼
                  TonoScreen (collectAsState)
                        │
                  ┌─────┴──────┐
              DaySection    DaySection  …
                  │
          TaskRow / GhostRow / EmptyRow / EditingRow
```

### UI state shape

```kotlin
data class TonoUiState(
    val days: List<DayUiState>,   // 14 days, always present from VM init
    val editing: EditingState?,   // which day is receiving keyboard input
    val swipe: Map<String, SwipeState>,   // per-task swipe offset (transient)
    val drag: DragState?,         // drag-in-progress (transient)
)
```

Only `tasks` is persisted (Room). `recents` (ghosts), `swipe`, `drag`, and `editing`
are in-memory and reset on app restart.

### Gesture system (TaskRow / GhostRow)

Both use `Modifier.pointerInput` with a manual `awaitPointerEventScope` loop.
Disambiguation on first 8dp of movement:

| Movement | Result |
|---|---|
| `\|dx\| > 8dp && \|dx\| > \|dy\| && dx > 0` (live row) | Enter SWIPE mode |
| `\|dx\| > 8dp && \|dx\| > \|dy\| && dx < 0` (ghost row) | Enter SWIPE mode |
| `\|dy\| > 8dp && \|dy\| > \|dx\|` | Release capture → scroll |
| Finger still for 380 ms | Enter DRAG mode, fire haptic |

Once a mode is entered it does not switch.
Swipe threshold: **96dp**. Animation: **240ms `CubicBezierEasing(.2,.7,.3,1)`**.

### Ghost (undo) lifecycle

1. `completeTask()` removes from DB, adds `GhostItem` to in-memory `ghostsByDay`.
2. A `viewModelScope` coroutine runs `delay(6_500)` then silently removes the ghost.
3. If `undoComplete()` fires first, the coroutine `Job` is cancelled and the task
   is re-inserted into the DB.

### Theme access

Use `LocalTonoColors.current` (not `MaterialTheme.colorScheme`) to get Tono's
design tokens directly:

```kotlin
val colors = LocalTonoColors.current
Text(color = colors.muted, ...)
```

---

## Common Tasks

### Add a new task property (e.g., `priority`)

1. Add the field to `Task.kt` and increment `TonoDatabase.version`.
2. Write a Room `Migration` and register it in `TonoDatabase`.
3. Expose the field in `TaskItem` (ViewModel) and update `buildDayList`.
4. Render it in `TaskRow`.

### Modify swipe threshold or animation timing

Constants live in `TaskRow.kt` and `GhostRow.kt` as local `val`s (named after
the design-spec tokens: `swipeThresholdPx`, `longPressMs`, etc.).
The spec values are in [`README.md`](README.md) → *Design Tokens — quick reference*.

### Add a new color token

1. Add the light and dark `Color` values to `Color.kt`.
2. Add the field to `TonoColors` data class and both `Light`/`Dark` instances.
3. If the color maps to a Material role, wire it in `Theme.kt`'s `lightScheme`/`darkScheme`.

---

## Testing

### Unit tests

```bash
./gradlew test
# Reports: app/build/reports/tests/testDebugUnitTest/index.html
```

Key things worth unit-testing:
- `computeDayWindow()` edge cases (Friday, Monday, week boundary)
- `TonoViewModel` ghost TTL and undo logic (use `TestCoroutineScheduler`)

### Manual smoke test checklist

- [ ] App launches showing two full weeks; today has yellow left border
- [ ] Weekend headings are dimmed (~55 % opacity)
- [ ] Tap empty line → keyboard opens, typing works, Enter chains to next line
- [ ] Escape / tap outside → cancels without saving
- [ ] Swipe right on a task: yellow wash fills proportionally, text strikes through at 96dp, row exits on release
- [ ] Ghost row appears with "← UNDO" hint; swipe left to restore
- [ ] Ghost disappears automatically after ~6.5 s
- [ ] Force-quit and reopen: tasks persist

---

## Out of Scope (v1)

Not yet implemented — do not add without a product decision:

- Edit an existing task (tap written line)
- Reorder tasks within a day
- "Show completed today" view
- History of older weeks
- Tags, priorities, sub-items
- Dark-mode toggle (design tokens exist; `TonoTheme` reads `isSystemInDarkTheme()` automatically)
