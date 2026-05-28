# Handoff: Tono — text-editor to-do app

## Overview

Tono is an Android to-do app that **looks and feels like a plain-text editor**, not a list app. Each day of the week is a markdown-style subheading; tasks are just lines of text under their day. The user can see the current week + next week in one scroll. They tap an empty line to type a task, swipe right to complete it (which makes the line disappear), and long-press to drag a task between days.

The design intentionally avoids every typical to-do trope: no checkboxes, no icons, no chips, no priority flags, no due-time pickers, no separate "add task" button. The empty line under each day is the only affordance — same metaphor as adding a line in a text file.

## About the Design Files

The files in `/prototype` and `/wireframes` are **design references built in HTML/React**. They demonstrate intended look, layout, and behavior — they are NOT production code to copy directly.

Your task is to **recreate these designs in the target codebase's existing environment** (Jetpack Compose / Kotlin if shipping native Android, React Native / Expo if cross-platform, etc.) using its established patterns. If no codebase exists yet, choose the framework most appropriate for an Android-first app.

The HTML prototype uses:
- React 18 + inline Babel transpile (for prototyping speed only)
- Pointer-event-based gesture handling
- Vanilla CSS — no UI framework
- localStorage for persistence (replace with proper local DB in production)

## Fidelity

**High-fidelity.** Colors, typography, spacing, and interaction timings are intentional. Recreate pixel-for-pixel using the target codebase's idioms.

## The Design System

### Color tokens (light)
| Token | Value | Usage |
|---|---|---|
| `paper` | `#f5f1e8` | Background — warm off-white, the "page" |
| `ink` | `#1a1814` | Body text |
| `muted` | `#9a948a` | Day-date right-side, status strip, placeholder cursor |
| `hair` | `#d8d2c4` | Thin hairline under each day heading |
| `today` | `#f7e07a` | The ONLY accent — today's left border + swipe wash + drop-target tint |
| `drop` | `rgba(247,224,122,0.30)` | Day section background when a drag is hovering it |

### Color tokens (dark)
| Token | Value |
|---|---|
| `paper` | `#15130f` |
| `ink` | `#e8e3d6` |
| `muted` | `#6b675e` |
| `hair` | `#2a2722` |
| `today` | `#c9a02d` |
| `drop` | `rgba(201,160,45,0.18)` |

### Typography
- **Body / tasks / day headings:** JetBrains Mono, 14px, line-height 26px
- **Day name + date subheading:** JetBrains Mono, 11px, letter-spacing 2px, uppercase, weight 500 (700 for today)
- **Status row at top + "next week" divider:** JetBrains Mono, 10px, letter-spacing 2–3px, uppercase, muted
- **Undo hint:** JetBrains Mono, 9px, letter-spacing 1.5px, uppercase, 55% opacity

Alternate fonts exposed in the prototype's Tweaks panel (`Newsreader` serif, `Inter` sans) are for exploration only — ship with monospace as default.

### Spacing
- Page horizontal padding: **16px** left and right
- Status strip top: padding `14px 20px 4px`
- Between days: `marginTop: 18px`
- Day heading → first task: `paddingTop: 6px` (after the hairline rule)
- Hairline rule: `1px` solid `hair`, `marginTop: 6px` below heading
- Today's left border: `3px solid today`, paddingLeft `16px`
- Other days: `3px solid transparent` (so text doesn't shift)
- Between current and next week: a centered "— next week —" label, `margin: 32px 16px 4px`

### Row metrics
- Task row height: 26px line-height, no vertical padding
- Empty input row: 26px height (matches a real text-editor line)
- Cursor blink animation: `1.1s` steps(2) infinite, on muted color at 40% opacity

## Screens

There is only one screen — the editor. It scrolls vertically through 14 days (current week + next week).

### Layout (top to bottom)

```
┌────────────────────────────────────────┐
│ 27 may — 9 jun           tono          │  ← status strip (muted, uppercase)
├────────────────────────────────────────┤
│                                        │
│  MON                          27 may   │  ← day heading
│  ────────────────────────────────────  │  ← hairline rule
│  stand-up @ 10                         │  ← task row
│  review PR #482                        │
│  tono onboarding sketch                │
│   ▍                                    │  ← blinking cursor on empty line
│                                        │
│ │ TUE · TODAY                  28 may  │  ← today: bold + yellow left bar (3px)
│ │─────────────────────────────────────│
│ │ gym                                  │
│ │ call mom                             │
│ │                                      │
│                                        │
│  WED                          29 may   │
│  ...                                   │
│                                        │
│           — next week —                │  ← week divider, centered, muted
│                                        │
│  MON                           3 jun   │
│  ...                                   │
└────────────────────────────────────────┘
```

### Weekend treatment
Saturday and Sunday day headings render at `opacity: 0.55` (unless they're today) so the work week reads as primary. Tasks themselves are not dimmed.

## Interactions

All gestures use a unified pointer-event state machine. See `prototype/tono-app.jsx` → `taskHandlers` for the reference implementation.

### 1. Tap an empty line → add a task
- The empty line under each day's tasks is an invitation, not just whitespace.
- `onClick` enters edit mode: the empty line becomes an `<input>` autofocused with the OS keyboard.
- The day section scrolls toward the top of the viewport so it stays visible above the keyboard.
- **Enter** commits the task and immediately opens a new empty input on the same day (chained typing — like a real editor).
- **Escape** or blur cancels.
- Today's first empty line shows a blinking 1.5px caret as a hint, even when not yet in edit mode.

### 2. Swipe right → complete
- Threshold: `96px` of positive horizontal movement.
- During swipe: a yellow wash fills the row from left to right proportional to `dx`. The text picks up a strikethrough once past the threshold.
- On release **past threshold**: the row animates off the right edge (`translateX(480px)`, `cubic-bezier(.2,.7,.3,1)`, 240ms) and is replaced by a **ghost row** (see undo below).
- On release **before threshold**: row animates back to `translateX(0)` over 240ms (snap-back), then the swipe state entry is cleared.

### 3. Undo via swipe-left on the ghost row
- A completed task lives for **6500ms** as a "ghost row" in its original day:
  - Color: `muted`
  - Text decoration: `line-through` (color = muted)
  - Opacity: 0.55
  - A small `← UNDO` hint sits at the right edge (9px, letter-spacing 1.5px, 55% opacity)
- Swiping LEFT on the ghost past the same threshold (-96px) animates it off the LEFT edge (240ms) and restores the task to the live list of that day.
- If 6500ms elapses without an undo gesture, the ghost is silently removed.
- Right-swipe on a ghost is ignored (direction-gated).

### 4. Long-press → drag between days
- Long-press threshold: `380ms` with the finger holding still (< `8px` movement).
- On trigger: `navigator.vibrate(8)` haptic, the original row fades to `opacity: 0.25`, a "ghost" element follows the finger (`position: fixed`, 200px wide, paper background, shadow `0 8px 24px rgba(0,0,0,0.25)`).
- As the finger moves, the day section under it gets a `drop` background tint.
- On release over a different day, the task is removed from its original day and appended to the destination day's list.
- On release over the original day or outside any day, nothing happens.

### 5. Gesture disambiguation
The state machine resolves ambiguous starts:
- `idle → swipe`: horizontal movement > 8px and > vertical movement, AND direction matches variant (live = right-only, ghost = left-only).
- `idle → drag`: long-press timer fires while still `idle` (no movement).
- `idle → scroll`: vertical movement > 8px and > horizontal movement; pointer capture released so the scroll container handles it.
- Once a mode is entered, it commits — no mode switching mid-gesture.

## State Management

```ts
type DayKey = string;  // YYYY-MM-DD
type Task = { id: string; text: string };
type Ghost = { id: string; text: string; completedAt: number };

interface AppState {
  tasks:    Record<DayKey, Task[]>;     // live tasks per day
  recents:  Record<DayKey, Ghost[]>;    // recently-completed (undo window)
  editing:  { dayKey: DayKey; value: string } | null;
  swipe:    Record<TaskId, { dx: number; snapping: boolean }>;
  drag:     { taskId: string; fromDay: DayKey; x: number; y: number; overDay: DayKey } | null;
}
```

- `tasks` is the only durable state — persist to local DB (Room on Android, etc.).
- `recents` is in-memory only with TTL timers per entry (6500ms).
- `swipe`, `drag`, `editing` are transient UI state.

### Persistence
The prototype uses `localStorage` under key `tono.tasks.v1`. For production:
- Android: Room database with a single `tasks` table keyed by `(day_key, position)`
- The 14-day window is derived from "today" at app launch — tasks before that window stay in the DB but are not shown (history view is out of scope for v1).

### Day-window derivation
The visible window is **today's most recent Monday** + 13 days. So a user opening the app on Wednesday sees Mon–Sun of this week + Mon–Sun of next week, with today highlighted on Wednesday.

## Design Tokens — quick reference

```
// Colors (light)
PAPER  = #f5f1e8
INK    = #1a1814
MUTED  = #9a948a
HAIR   = #d8d2c4
TODAY  = #f7e07a
DROP   = rgba(247,224,122,0.30)

// Type
FONT   = JetBrains Mono (Regular 400 / Medium 500 / Bold 700)
BODY   = 14 / 26
HEAD   = 11 / letter-spacing 2 / uppercase
META   = 10 / letter-spacing 2-3 / uppercase

// Motion
SWIPE_THRESHOLD = 96px
LONGPRESS_MS    = 380
MOVE_LOCK       = 8px
REMOVE_ANIM     = 240ms (cubic-bezier(.2,.7,.3,1))
SNAP_BACK       = 240ms (cubic-bezier(.2,.7,.3,1))
RECENTS_TTL     = 6500ms

// Layout
PAGE_PAD_X      = 16
DAY_GAP         = 18
TODAY_BAR       = 3px
HAIRLINE        = 1px
```

## Out of scope for v1 (but worth knowing)

The prototype shows these as Tweaks but they are NOT product requirements:
- Theme toggle (dark mode) — implementation included in case you want it as a setting
- Font alternates (serif, sans) — exploration only; ship mono
- "Phone bezel" toggle — prototype-only

These ARE planned next iterations the design did not address:
- Edit existing task (tap a written line to edit it)
- Task reorder within a day
- "Show completed today" view
- History view (older weeks)
- Tags or sub-items — the brief explicitly excluded these

## Files

```
prototype/
  Tono.html              ← entry point, loads everything
  tono-app.jsx           ← all interaction logic + visual components
  android-frame.jsx      ← prototype-only phone bezel (not for production)
  tweaks-panel.jsx       ← prototype-only Tweaks panel (not for production)

wireframes/
  Tono Wireframes.html   ← three early directions explored before hi-fi
  variants.jsx           ← variant A (markdown), B (iA Writer), C (date-led)
  design-canvas.jsx      ← canvas shell for side-by-side variants
  android-frame.jsx
  tweaks-panel.jsx
```

The shipped design is **variant B** from the wireframe set. Variants A and C are preserved in `/wireframes` for context — they show alternatives the user rejected. Don't implement them.

## Open questions for the developer

1. **What's "today" if the user opens the app at 11:55pm?** The prototype uses local-time day rollover. Confirm with PM whether to delay rollover until ~4am like some calendar apps.
2. **Drag-to-yesterday?** The 14-day window starts on Monday. If today is Friday, the user can still drag a task to Mon–Thu of the current week. Confirm this is desired (it allows "back-fill" but may also create confusion).
3. **Keyboard adjust:** the prototype scrolls the editing day to top on focus. Native Android `windowSoftInputMode="adjustResize"` may do this automatically — pick whichever feels smoother.
