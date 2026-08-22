# Tono — Manual Smoke Test Checklist

A pass through every user-facing behavior after a build. Run on a device or
emulator with `./gradlew installDebug`. Check the box only when the observed
result matches the **Expected** note.

> Tip: several checks assume you can reach the current window. The visible window
> is **this week's Monday + 13 days** (current week + next week), so "today" is
> always somewhere in the first seven day sections. The months view shows the
> **current month + the next three**, plus a `later` bucket.

---

## 1 — Launch & layout

- [ ] **App launches** to the scrolling **WEEKS** editor screen (no splash, no nav bars).
- [ ] **Status strip** at the very top shows the screen switch on the left
      (`WEEKS · MONTHS`, uppercase — `WEEKS` in ink, `MONTHS` muted) and
      `TONO v<version>` on the right.
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

## 10 — Push a whole day forward (swipe the day heading)

- [ ] **Swipe the heading of a past day right**: the heading slides with a yellow
      wash and, past ~96dp, the date on the right flips to **`→ <WEEKDAY>`** with a
      light haptic tick.
- [ ] **Releasing past the threshold** moves **all** that day's tasks to **today**,
      appended in their original order; the source day empties.
- [ ] **Releasing short of the threshold** snaps back and changes nothing.
- [ ] Swiping **today's** heading right moves its tasks to **tomorrow**; swiping a
      **future** day's heading moves them one day on.
- [ ] After the move the source heading shows **`← UNDO · N → <WEEKDAY>`** for ~6.5s;
      **swiping that heading left** past the threshold restores every task to its
      original day *and* original order.
- [ ] Letting the undo window lapse **silently drops the affordance**; the date
      label returns.
- [ ] The gesture is **inert on an empty day** (no wash) and on the **last day of the
      window** (its target would fall outside the visible fortnight).
- [ ] **Completed ghosts are left behind** — only live tasks move.
- [ ] **Tapping** a day heading still starts a new entry, and **vertical drags**
      starting on a heading still scroll the list.

## 11 — Months screen

- [ ] **Tap `MONTHS`** in the status strip → the list is replaced by **five sections**:
      the current month, the next three, and a **`later`** bucket. `MONTHS` is now ink
      and `WEEKS` muted.
- [ ] Each month heading shows the **month name in green** on the left (`AUGUST`) and
      the **year** on the right (`2026`); `later` shows `—` on the right.
- [ ] The **current month** is bold, has a ` · THIS MONTH` suffix, a **3px yellow left
      border**, and a **blinking caret** on its first empty line.
- [ ] A centered **`— LATER —`** divider sits above the `later` section.
- [ ] **Tap `WEEKS`** → back to the 14-day view, with day tasks untouched.
- [ ] **System back** on MONTHS returns to WEEKS (it does not exit the app);
      system back on WEEKS exits as usual.

## 11a — Months screen: entries & gestures

Everything from sections 3–9 should behave identically here. Spot-check:

- [ ] **Tap an empty line under a month** → input opens; typing and waiting ~1s
      persists it. **Enter** chains a new entry on the same month.
- [ ] **Swipe a month task right** → completes to a ghost; **swipe the ghost left**
      within ~6.5s → restored.
- [ ] **Long-press and drag** a task from one month to another → it lands appended
      at the destination.
- [ ] **Swipe a month heading right** → all its tasks move to the **next month**,
      with the `← UNDO · N → <MONTH>` affordance for ~6.5s; **swipe left** restores
      order and month exactly.
- [ ] Swiping the **last named month's** heading right pushes into **`later`**.
- [ ] The gesture is **inert on the `later` heading** (nowhere further to push) and
      on any **empty** month.
- [ ] **Switch to WEEKS mid-edit** → the in-flight entry is saved, not lost, and the
      keyboard/editing session closes.
- [ ] Month tasks **do not appear** in the weeks view, and vice versa.

## 12 — Task rollover (carry old task forward)

> Requires changing the device clock, since it only triggers for tasks older than
> the visible window.

- [ ] Create a task, then **set the device date forward by a week or more** and
      relaunch → the task is **carried to the same weekday** in the new window
      (rather than vanishing).
- [ ] Create a task in the **current month**, then **set the device date forward by a
      month or more** and relaunch → it is **carried to the new current month**.
      Tasks in **`later` never move**.

## 13 — Persistence

- [ ] **Force-quit and reopen** the app → **tasks persist** in both views.
- [ ] **Upgrade over an older install** (`adb install -r` on top of v1.0, do *not*
      uninstall) → existing tasks survive the schema migration and all land in
      **WEEKS**; the months view starts empty.
- [ ] Transient state is **reset** on relaunch: any in-flight ghost/undo, swipe
      offsets, drag, the editing session, and the selected screen (always opens on
      **WEEKS**).

## 14 — Theme

- [ ] **Switch the system to dark mode** → the app follows automatically
      (warm dark paper, dimmer accents); there is **no in-app toggle**.
- [ ] Colors read correctly in **both light and dark** (green weekday labels,
      yellow today accent, muted chrome).
