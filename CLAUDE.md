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
│   │   ├── Task.kt                  Room entity (id, dayKey, position, text, bucket, createdAt)
│   │   ├── TaskHistory.kt           Room entity — when a piece of task text was first seen
│   │   ├── TaskDao.kt               Flow<List<Task>>, suspend insert/delete/update
│   │   └── TonoDatabase.kt          singleton Room DB ("tono.db")
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Color.kt             TonoColors data class + Light/Dark instances
│   │   │   ├── Type.kt              JetBrainsMono FontFamily + TonoType text styles
│   │   │   └── Theme.kt             TonoTheme composable + LocalTonoColors
│   │   ├── TonoViewModel.kt         all state + business logic (both screens)
│   │   ├── TonoScreen.kt            root LazyColumn screen + WEEKS/MONTHS switch
│   │   └── components/
│   │       ├── StatusStrip.kt       mode line: WEEKS · MONTHS switch + app version
│   │       ├── SectionDivider.kt    "— next week —" / "— later —" separator
│   │       ├── DaySection.kt        heading + hairline + task list (day or month)
│   │       ├── DayHeadingRow.kt     heading, swipe-right to push the section forward
│   │       ├── TaskRow.kt           live task, swipe-right to complete
│   │       ├── GhostRow.kt          completed task, swipe-left to undo
│   │       ├── EmptyRow.kt          invitation line with blinking cursor
│   │       └── EditingRow.kt        active BasicTextField input
│   └── util/
│       ├── DayWindow.kt             computeDayWindow() → 14 LocalDates
│       ├── MonthWindow.kt           computeMonthWindow() → 4 YearMonths + "later"
│       └── TaskSimilarity.kt        normalizeTaskText() / taskSimilarity() — near-duplicate matching
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
                        │        builds BOTH section lists on every emission
                        ▼
                  TonoScreen (collectAsState)
                        │  renders state.sections — days or months
                  ┌─────┴──────┐
              DaySection    DaySection  …
                  │
          TaskRow / GhostRow / EmptyRow / EditingRow
```

### UI state shape

```kotlin
data class TonoUiState(
    val screen: TonoScreenKind,   // WEEKS or MONTHS
    val days: List<DayUiState>,   // 14 days, always present from VM init
    val months: List<DayUiState>, // 4 months + "later", always present from VM init
    val editing: EditingState?,   // which section is receiving keyboard input
    val swipe: Map<String, SwipeState>,   // per-task swipe offset (transient)
    val drag: DragState?,         // drag-in-progress (transient)
) {
    val sections: List<DayUiState>   // whichever list the active screen renders
}
```

`DayUiState` is one heading-plus-rows **section** — a calendar day in the weeks view,
a month or the `later` bucket in the months view. Both screens render it through the
same `DaySection`, so every gesture is written once. Its `isCurrent`/`currentLabel`
fields carry the "you are here" treatment (`· TODAY`, `· THIS MONTH`).

Only `tasks` and `task_history` are persisted (Room). `recents` (ghosts), `swipe`, `drag`,
`editing`, and the active `screen` are in-memory and reset on app restart.

### The two screens

The months view is the same document at a coarser scale: **current month + the next
three + a `later` bucket**, each holding tasks with no committed day yet.

| | Weeks view | Months view |
|---|---|---|
| Sections | 14 days (this week + next) | 4 months + `later` |
| Section key (`dayKey`) | `2026-08-24` | `2026-08`, or `later` |
| `bucket` column | `week` | `month` |
| Heading | `пн · TODAY` / `24 AUG` | `AUGUST · THIS MONTH` / `2026` |
| Divider | `— NEXT WEEK —` above next Monday | `— LATER —` above the later bucket |
| Push-forward target | next day (`pushForwardTarget`) | next month, then `later` (`monthPushForwardTarget`) |
| Rollover on launch | stale day → same weekday this window | stale month → current month |

**Switching:** the status strip is a mode line — `WEEKS · MONTHS`, active word in `ink`,
inactive in `muted`, tap to switch. Nothing else was spent on navigation, and it collides
with no row gesture (horizontal swipes are already taken by complete/undo/push). System
back on MONTHS returns to WEEKS. Switching screens finalizes any in-flight edit.

**Section keys are self-describing.** `bucketOf(key)` in `util/MonthWindow.kt` derives the
bucket from the key's shape (`yyyy-MM-dd` = day, `yyyy-MM` and `later` = month), which is
why every gesture callback stays bucket-agnostic — `DaySection`, `TaskRow`, and
`DayHeadingRow` never learn which screen they are on. The `bucket` column exists so the
two rollover queries can filter cheaply and readably; it is always derived, never entered
by hand.

> Tasks cannot be dragged between the two screens — drag resolves a drop target from
> on-screen section bounds. To move a month task onto a day, retype it (or push it forward).

### Task age

A task that has been carried for more than a week says so: `remember the milk (15)`, the
number in the `age` token (warm rust) against `ink` body text. Under the threshold nothing is
drawn — the marker is meant to be an occasional flag, not a column of numbers.

- `Task.createdAt` holds the **epoch day** the task entered the list. Every move preserves it,
  because rollover, whole-section push, and drag are all `dao.update(task.copy(...))` on the
  same row — never delete + re-insert. `undoComplete()` restores it from the ghost.
- `TonoViewModel.ageBadge()` turns it into `TaskItem.ageDays`, which is **null** at or below
  `AGE_BADGE_MIN_DAYS` (7) so the row renders unmarked. Age is computed against the launch
  date, like every other date in the app.
- Rows migrated from a pre-v3 database start their clock on the day of the upgrade — an age
  nobody can verify is worse than none.

**Near-duplicates.** A row does not survive every way a task travels: retyping a month task
onto a day, deleting and re-adding a line, or rewording it all produce a *new* row that would
otherwise restart at zero. The `task_history` table records, per normalized text, the day it
was first seen; `startDayFor()` looks up the closest match above `SIMILARITY_THRESHOLD` and
the new task inherits its `firstSeen`. Because autosave fires after a 600 ms pause, a row can
first be saved under a half-typed fragment, so `reconcileStartDay()` resolves again once the
text is complete — but only for rows created today; rewording an established task changes its
spelling, never its clock.

`util/TaskSimilarity.kt` scores two lines 0..1 by taking the stronger of two measures — a
character-level Levenshtein ratio (typo fixes, small rewordings) and an order-insensitive Dice
overlap of fuzzily-paired tokens (added or dropped filler). Character-level matching is
ignored below 8 characters, where a single edit (`milk` / `silk`) stops being evidence.
It is a pure function, and the threshold behavior is what `TaskSimilarityTest` pins down.

A history entry is dropped when a task is **completed for good** — at ghost expiry, and only
if no live row still carries that text. Finishing something ends its clock, so writing the
same line next month is a genuinely new task; merely deleting or rewording is not. Entries
untouched for `HISTORY_TTL_DAYS` (180) are pruned at launch.

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

### Section-scale gestures (DayHeadingRow)

Row gestures are per-task; the section *heading* carries the whole-section equivalents,
using the same 96dp threshold and easing:

| Gesture on the heading | Result |
|---|---|
| Swipe right (section has live tasks) | Push every live task one step forward |
| Swipe left (undo window open) | Restore the pushed tasks to their original section + positions |
| Tap | Start a new entry, same as tapping the section's empty space |
| Vertical drag | Release capture → scroll |

The ViewModel's `pushTargetKey()` picks the destination by bucket:

- **Days** — `pushForwardTarget(dayKey, today)` (`util/DayWindow.kt`) is `max(today, day + 1)`:
  past days collapse onto **today**, today defers to **tomorrow**, a future day steps on by
  one. Suppressed when the target falls outside the 14-day window.
- **Months** — `monthPushForwardTarget(monthKey, currentMonth)` (`util/MonthWindow.kt`) steps
  on by one month; a past month collapses onto the **current** month, the last named month
  spills into **`later`**, and `later` returns `null` — it has nowhere further to go, so the
  gesture is suppressed there.

The affordance names its destination with a **short** label (`→ SEP`, `→ ВТ`, `→ LATER`) —
`pushLabel()` in the ViewModel — because it shares one non-wrapping heading line with the
section's own label; `AUGUST · THIS MONTH` plus a spelled-out `SEPTEMBER` does not fit a
phone-width heading.

`Modifier.pointerInput` sits *outside* the heading's top padding so the touch strip covers
the section's leading whitespace.

### Whole-section push (undo) lifecycle

Mirrors the ghost lifecycle at section scale:

1. `pushDayForward()` folds any in-flight editing session into the move, reads the
   section's rows, stores them verbatim in `pushRecords[dayKey]`, then rewrites each
   row's `dayKey`/`position`/`bucket`. The Room flow emission is what repaints both days.
2. A `viewModelScope` coroutine runs `delay(PUSH_UNDO_MS)` (6 500 ms, matching the
   ghost TTL) then drops the record.
3. `undoPush()` cancels that job and `dao.update()`s the stored pre-move rows,
   restoring section *and* order exactly.

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

1. Add the field to `Task.kt` and increment `TonoDatabase.version` (currently **3**).
2. Write a Room `Migration` and register it in `TonoDatabase.addMigrations()`
   (see `MIGRATION_1_2`, which added the `bucket` column, and `MIGRATION_2_3`, which
   added `createdAt` plus the `task_history` table).
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

### Bump the app version

`versionName`/`versionCode` live in `app/build.gradle.kts` → `defaultConfig`.
`StatusStrip` reads `versionName` at build time via `BuildConfig.VERSION_NAME`
(requires `buildFeatures.buildConfig = true`, already enabled) — no need to
touch the UI when the version changes.

- **`versionName`** (shown in the header, e.g. `TONO v1.0`): bump for any
  user-visible change worth distinguishing between installs — a new feature,
  a behavior change, a notable bug fix. Use `MAJOR.MINOR`
  (`1.0` → `1.1` for incremental changes, `2.0` for a substantial redesign or
  breaking data change). Skip it for pure refactors, test-only changes, or
  typo fixes with no user-visible effect.
- **`versionCode`**: increment by 1 on every release you intend to install
  over a previous build (Android requires a strictly increasing code to
  allow the upgrade). Bump it alongside `versionName` in the same commit.

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
| `util/MonthWindowTest.kt` | `computeMonthWindow()` (4 months, year boundary), `bucketOf()` key-shape dispatch, `monthRolloverTarget()`, `monthPushForwardTarget()` (incl. spill into `later` and the `later` dead end), `monthLabel()`/`monthDateLabel()`, `later`-sorts-last (which the stale-month query relies on) |
| `util/PastedTextTest.kt` | `splitPastedLines()` — multi-line paste → entries + live remainder |
| `util/TaskSimilarityTest.kt` | `normalizeTaskText()`, `levenshtein()`/`levenshteinRatio()`, and `taskSimilarity()`/`isSameTask()` — what counts as the same task (filler words, reordering, typos) and what does not (same verb, different object; short words; length mismatch) |

**Testability strategy:** the genuinely bug-prone logic is kept as pure functions
in `util/` (no `Application`, no Room, no coroutines) so it can be unit-tested
without the Android runtime. When adding logic, prefer extracting the decision
into a pure helper and testing that, rather than reaching for an emulator.

**Not yet unit-tested** (needs the Android runtime — an emulator/device via
`connectedDebugAndroidTest`, or Robolectric under `src/test/`): `TonoViewModel`
ghost TTL + undo timing, `pushDayForward()`/`undoPush()` round-tripping, the
WEEKS/MONTHS switch, `createdAt` inheritance through `startDayFor()`, and `TaskDao` against
real SQLite (including `MIGRATION_1_2` and `MIGRATION_2_3`). The ViewModel is
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
- History of older weeks or past months
- Dragging a task between the weeks and months screens
- Tags, priorities, sub-items
- Dark-mode toggle (design tokens exist; `TonoTheme` reads `isSystemInDarkTheme()` automatically)
