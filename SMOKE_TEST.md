# Tono — Manual Smoke Test Checklist

A pass through every user-facing behavior after a build. Run on a device or
emulator with `./gradlew installDebug`. Check the box only when the observed
result matches the **Expected** note.

> Tip: several checks assume you can reach the current window. The visible window
> is **this week's Monday + 13 days** (current week + next week), so "today" is
> always somewhere in the first seven day sections.

---

## 1 — Launch & layout

- [ ] **App launches** to a single scrolling editor screen (no splash, no nav bars).
- [ ] **Status strip** at the very top shows the date range on the left
      (e.g. `31 JUL — 13 AUG`, uppercase, muted) and `TONO` on the right.
- [ ] **Fourteen day sections** render — the current week then next week.
- [ ] A centered **`— NEXT WEEK —`** divider sits above next Monday's section.
- [ ] Scrolling is smooth vertically through all 14 days.

## 2 — Day headings & chrome

- [ ] Each day shows a **Russian weekday abbreviation in green** (`пн вт ср чт пт сб вс`)
      and an **English date on the right** (e.g. `31 JUL`, muted, uppercase).
- [ ] A **1px hairline** sits under each heading, above that day's tasks.
- [ ] **Today** is bold, has a ` · TODAY` suffix, and a **3px yellow left border**.
- [ ] **Weekend headings** (Sat/Sun) render dimmed (~55% opacity) — unless the
      weekend day is today. Task text itself is not dimmed.
- [ ] **Today's first empty line** shows a **blinking caret** (~1s cycle) while
      nothing else is being edited.

## 3 — Adding a task (tap → type)

- [ ] **Tap an empty line** (or anywhere in a day section) → keyboard opens and
      the line becomes an input, autofocused with the caret at the end.
- [ ] The **text cursor is green**.
- [ ] Typing a few characters and **waiting ~1s persists it automatically**
      (autosave) — even without pressing anything.
- [ ] **Enter** commits the line and immediately **opens a fresh empty input on
      the same day** (chained entry, like a text editor).
- [ ] **Tapping outside / dismissing focus** commits the current text and closes
      the editor.
- [ ] Starting to type on a **new day while another line is being edited** saves
      the first line before switching.
- [ ] An input left **completely blank** creates no task.

## 4 — Editing an existing task (tap-to-edit)

- [ ] **Tapping a written task line** turns it into an editable field preloaded
      with its text, caret at the end.
- [ ] Changing the text and pressing **Enter** updates the line and closes the editor.
- [ ] **Clearing all the text** and committing (Enter or tap-away) **deletes** the task.

## 5 — Deleting while editing

- [ ] **Backspace on an empty field** of an existing task **deletes** it and closes.
- [ ] **Backspace on an empty field** of a brand-new (unsaved) entry just dismisses
      the editor (nothing to delete).
- [ ] **Swiping right on the editing row**: a yellow wash fills proportionally, text
      strikes through past ~96dp; releasing past threshold **deletes** the entry.

## 6 — Paste multiple lines

- [ ] **Paste multi-line text** into an editing field → each complete line becomes
      its own task on that day; the **trailing remainder stays live** in the field.

## 7 — Swipe right → complete

- [ ] **Swipe right** on a live task: a **yellow wash fills** the row left-to-right
      proportional to the drag.
- [ ] Past **96dp** the text picks up a **strikethrough**.
- [ ] **Release past threshold** → the row **animates off the right edge** (~240ms)
      and the task is removed from the live list.
- [ ] **Release before threshold** → the row **snaps back** with no change.
- [ ] A **mostly-vertical drag scrolls** the list instead of swiping.

## 8 — Ghost row & undo

- [ ] After a completion, a **ghost row** takes its place: muted, strikethrough,
      ~55% opacity, with a **`← UNDO`** hint at the right edge.
- [ ] **Swiping the ghost left** past ~96dp **restores** the task to that day's live list.
- [ ] A **right-swipe on the ghost is ignored** (direction-gated).
- [ ] Left untouched, the ghost **disappears on its own after ~6.5s**.

## 9 — Drag a task between days

- [ ] **Long-press a task** (~380ms, finger held still) fires a **haptic buzz**; the
      source row fades to ~30% opacity and a **floating chip** (paper background,
      hairline border) follows the finger.
- [ ] Dragging over a **different day tints that section yellow** (drop target).
- [ ] **Releasing over a different day** moves the task there (appended to its list).
- [ ] **Releasing over the original day or outside any day** leaves everything unchanged.

## 10 — Task rollover (carry old task forward)

> Requires changing the device clock, since it only triggers for tasks older than
> the visible window.

- [ ] Create a task, then **set the device date forward by a week or more** and
      relaunch → the task is **carried to the same weekday** in the new window
      (rather than vanishing).

## 11 — Persistence

- [ ] **Force-quit and reopen** the app → **tasks persist**.
- [ ] Transient state is **reset** on relaunch: any in-flight ghost/undo, swipe
      offsets, drag, and the editing session are gone.

## 12 — Theme

- [ ] **Switch the system to dark mode** → the app follows automatically
      (warm dark paper, dimmer accents); there is **no in-app toggle**.
- [ ] Colors read correctly in **both light and dark** (green weekday labels,
      yellow today accent, muted chrome).
