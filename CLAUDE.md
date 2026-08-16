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

> **WSL note:** You do **not** need an emulator to develop here. The unit-test
> suite (see [Testing](#testing)) is pure JVM and runs entirely in WSL. When you
> do need a running app or instrumented tests, modern WSL2 (WSLg + `/dev/kvm`) can
> run the emulator headless — check with `ls /dev/kvm` and add yourself to the
> `kvm` group (`sudo usermod -aG kvm $USER`, then `wsl --shutdown` and reopen).
> The simpler alternative is still to build the APK here and install it on a
> physical device or an emulator running on your Windows host.

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

#### Physical device over USB from WSL

WSL2 doesn't see USB devices natively, so the phone has to be forwarded from
Windows using `usbipd-win`.

**One-time setup:**

```powershell
# On Windows, in an Administrator PowerShell:
winget install usbipd-win
```

```bash
# In WSL:
sudo apt install linux-tools-generic hwdata usbutils
sudo update-alternatives --install /usr/local/bin/usbip usbip /usr/lib/linux-tools/*/usbip 20
```

**Each time you plug in the phone:**

```powershell
# On Windows (Administrator PowerShell):
usbipd list                            # find the BUSID for the phone
usbipd bind --busid <BUSID>            # one-time per device, persists across reboots
usbipd attach --wsl --busid <BUSID>    # run this every time you plug in / reconnect
```

```bash
# In WSL:
lsusb          # confirm the device shows up
adb devices    # confirm it's listed
```

If `adb devices` shows the device as `unauthorized`, check the phone screen for
the "Allow USB debugging" prompt and tap Allow.

If `adb devices` shows `no permissions`, WSL's `udevd` likely isn't applying
device permissions to your user. The quickest fix is running the adb server as
root (note: `sudo` doesn't inherit your `PATH`, so pass it through or use the
full binary path):

```bash
sudo env "PATH=$PATH" adb kill-server
sudo env "PATH=$PATH" adb start-server
adb devices    # run as your normal user — should now show the device
```

For a permanent fix instead of running the server as root each time, add a
udev rule for your device's USB vendor ID (find it via `lsusb`, e.g. `18d1` for
Google, `04e8` for Samsung) and add yourself to the `plugdev` group:

```bash
sudo tee /etc/udev/rules.d/51-android.rules <<'EOF'
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
EOF
sudo usermod -aG plugdev $USER
sudo udevadm control --reload-rules && sudo udevadm trigger
```

Then restart the WSL session (`wsl --shutdown` from Windows, then reopen) for
the group change to apply. Note this only works if `udevd`/systemd is actually
running in your WSL instance (`ps aux | grep udev`) — otherwise the `sudo adb`
workaround above is the simpler path.

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
│   │       ├── DayHeadingRow.kt     day heading, swipe-right to push the day forward
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

### Day-scale gestures (DayHeadingRow)

Row gestures are per-task; the day *heading* carries the whole-day equivalents,
using the same 96dp threshold and easing:

| Gesture on the heading | Result |
|---|---|
| Swipe right (day has live tasks) | Push every live task to `pushForwardTarget()` |
| Swipe left (undo window open) | Restore the pushed tasks to their original day + positions |
| Tap | Start a new entry, same as tapping the day's empty space |
| Vertical drag | Release capture → scroll |

`pushForwardTarget(dayKey, today)` (in `util/DayWindow.kt`) is `max(today, day + 1)`:
past days collapse onto **today**, today defers to **tomorrow**, a future day steps
on by one. The gesture is suppressed when the target would fall outside the 14-day
window. `Modifier.pointerInput` sits *outside* the heading's top padding so the
touch strip covers the section's leading whitespace.

### Whole-day push (undo) lifecycle

Mirrors the ghost lifecycle at day scale:

1. `pushDayForward()` folds any in-flight editing session into the move, reads the
   day's rows, stores them verbatim in `pushRecords[dayKey]`, then rewrites each
   row's `dayKey`/`position`. The Room flow emission is what repaints both days.
2. A `viewModelScope` coroutine runs `delay(PUSH_UNDO_MS)` (6 500 ms, matching the
   ghost TTL) then drops the record.
3. `undoPush()` cancels that job and `dao.update()`s the stored pre-move rows,
   restoring day *and* order exactly.

Ghosts are never pushed — they are already completed.

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

Pure-JVM tests — **no emulator, no device, no Android SDK runtime**. They run in
seconds anywhere WSL included, and are the first thing to run after a change.

```bash
./gradlew testDebugUnitTest          # or `./gradlew test` for debug + release
# Reports: app/build/reports/tests/testDebugUnitTest/index.html
```

Tests live in `app/src/test/kotlin/org/volkov/tono/` (mirrors the `main` layout).
Current coverage:

| File | What it covers |
|---|---|
| `util/DayWindowTest.kt` | `computeDayWindow()` (14-day length, Monday alignment, today-in-window, month/year boundaries), `rolloverTarget()` weekday-preserving carry-over, `pushForwardTarget()` whole-day push destination, `dayLabel()`/`dateLabel()` |
| `util/PastedTextTest.kt` | `splitPastedLines()` — multi-line paste → entries + live remainder |

**Testability strategy:** the genuinely bug-prone logic is kept as pure functions
in `util/` (no `Application`, no Room, no coroutines) so it can be unit-tested
without the Android runtime. When adding logic, prefer extracting the decision
into a pure helper and testing that, rather than reaching for an emulator.

**Not yet unit-tested** (needs the Android runtime — an emulator/device via
`connectedDebugAndroidTest`, or Robolectric under `src/test/`): `TonoViewModel`
ghost TTL + undo timing, `pushDayForward()`/`undoPush()` round-tripping, and
`TaskDao` against real SQLite. The ViewModel is
currently coupled to `AndroidViewModel(app)`, the Room singleton, and
`viewModelScope`; unit-testing it cleanly would first want the DAO, clock, and
dispatcher injected.

### Manual smoke test checklist

The full, up-to-date checklist lives in [`SMOKE_TEST.md`](SMOKE_TEST.md) — it
covers launch/layout, add & tap-to-edit, delete-while-editing, multi-line paste,
swipe-to-complete + undo, drag between days, task rollover, persistence, and theme.

---

## Out of Scope (v1)

Not yet implemented — do not add without a product decision:

- Reorder tasks within a day
- "Show completed today" view
- History of older weeks
- Tags, priorities, sub-items
- Dark-mode toggle (design tokens exist; `TonoTheme` reads `isSystemInDarkTheme()` automatically)
