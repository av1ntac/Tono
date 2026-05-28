// tono-app.jsx — hi-fi interactive prototype, variant B (iA-Writer hairline)
// Gestures: tap empty line → edit · swipe right → complete (hides) · long-press → drag between days.
// Persists tasks to localStorage so refresh keeps state.

const { useState, useEffect, useRef, useCallback, useMemo, useLayoutEffect } = React;

// ── tokens ──────────────────────────────────────────────────────────
const TOKENS = {
  light: {
    paper: '#f5f1e8',
    ink:   '#1a1814',
    muted: '#9a948a',
    hair:  '#d8d2c4',
    today: '#f7e07a',
    drop:  'rgba(247,224,122,0.30)',
  },
  dark: {
    paper: '#15130f',
    ink:   '#e8e3d6',
    muted: '#6b675e',
    hair:  '#2a2722',
    today: '#c9a02d',
    drop:  'rgba(201,160,45,0.18)',
  },
};

const FONTS = {
  mono:   "'JetBrains Mono','IBM Plex Mono',ui-monospace,monospace",
  serif:  "'Newsreader','Iowan Old Style',Georgia,serif",
  sans:   "'Inter',-apple-system,system-ui,sans-serif",
};

const DAY_NAMES = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];

const SWIPE_THRESHOLD = 96;
const LONGPRESS_MS = 380;
const MOVE_LOCK = 8;
const STORAGE_KEY = 'tono.tasks.v1';

// ── date helpers ────────────────────────────────────────────────────
function buildDays() {
  // Anchor today to a Monday so demo always starts on Mon.
  // Use real "now" but snap to most recent Monday for predictable layout.
  const now = new Date();
  const dow = (now.getDay() + 6) % 7; // 0=Mon
  const monday = new Date(now);
  monday.setHours(0,0,0,0);
  monday.setDate(now.getDate() - dow);
  const out = [];
  for (let i = 0; i < 14; i++) {
    const d = new Date(monday);
    d.setDate(monday.getDate() + i);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2,'0');
    const dd = String(d.getDate()).padStart(2,'0');
    out.push({
      key: `${y}-${m}-${dd}`,
      day: DAY_NAMES[i % 7],
      date: d.getDate(),
      month: d.toLocaleString('en', { month: 'short' }).toLowerCase(),
      isToday: i === dow,
      isWeekend: (i % 7) >= 5,
      weekIdx: i < 7 ? 0 : 1,
    });
  }
  return out;
}

function seedTasks(days) {
  // Demo seed — only used if no saved state.
  const seedByOffset = {
    0: ['stand-up @ 10','review PR #482','tono onboarding sketch'],
    1: ['gym','call mom','pick up package'],
    2: ['design crit 2pm','ship v0.4'],
    3: ['1:1 with sam'],
    4: ['week retro','pay rent'],
    5: ['groceries'],
    6: [],
    7: ['flight to berlin 06:40'],
    8: ['conference day 1'],
    10: ['fly home'],
  };
  const todayIdx = days.findIndex(d => d.isToday);
  const out = {};
  days.forEach((d, i) => {
    const relative = i - todayIdx;
    // map demo seed indexed from today
    out[d.key] = (seedByOffset[relative] || []).map((t, j) => ({
      id: `${d.key}-${j}-${Math.random().toString(36).slice(2,7)}`,
      text: t,
    }));
  });
  return out;
}

// ── persistence ─────────────────────────────────────────────────────
function loadTasks(days) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedTasks(days);
    const parsed = JSON.parse(raw);
    // ensure every day key exists
    days.forEach(d => { if (!parsed[d.key]) parsed[d.key] = []; });
    return parsed;
  } catch {
    return seedTasks(days);
  }
}
function saveTasks(tasks) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks)); } catch {}
}

// ────────────────────────────────────────────────────────────────────
// TaskRow — handles all gestures
// Supports BOTH directions:
//   right swipe (dx > 0) → complete
//   left  swipe (dx < 0) → only meaningful if `allowUndo` (used by ghost rows)
// swipeState shape: { dx, snapping } — when snapping=true, transitions are on.
// ────────────────────────────────────────────────────────────────────
function TaskRow({
  task, dayKey, theme, fontFamily,
  isDragging, swipeState, onPointerHandlers,
  variant = 'live', // 'live' | 'ghost'
}) {
  const C = TOKENS[theme];
  const dx = swipeState?.dx || 0;
  const snapping = !!swipeState?.snapping;
  const isGhost = variant === 'ghost';
  const completing = !isGhost && dx > SWIPE_THRESHOLD;
  const restoring  = isGhost && dx < -SWIPE_THRESHOLD;

  // For ghost rows the highlight comes from the LEFT side (positive |dx| of a
  // negative swipe), drawn from the right edge inward.
  let bg = 'transparent';
  if (!isGhost && dx > 0) {
    const pct = Math.min(100, dx);
    bg = `linear-gradient(90deg, ${C.today} 0%, ${C.today} ${pct}%, transparent ${pct}%)`;
  } else if (isGhost && dx < 0) {
    const pct = Math.min(100, -dx);
    bg = `linear-gradient(270deg, ${C.today} 0%, ${C.today} ${pct}%, transparent ${pct}%)`;
  }

  return (
    <div
      data-task-id={task.id}
      data-day-key={dayKey}
      {...onPointerHandlers}
      style={{
        position: 'relative',
        fontFamily, fontSize: 14, lineHeight: '26px',
        color: isGhost ? C.muted : C.ink,
        padding: '0 16px 0 0',
        cursor: isGhost ? 'pointer' : 'grab',
        touchAction: 'pan-y',
        userSelect: 'none',
        opacity: isDragging ? 0.25 : (isGhost ? 0.55 : 1),
        transform: `translateX(${dx}px)`,
        transition: snapping ? 'transform 0.22s cubic-bezier(.2,.7,.3,1), background 0.22s ease' : 'none',
        background: bg,
        textDecoration: isGhost || completing ? 'line-through' : 'none',
        textDecorationColor: isGhost ? C.muted : C.ink,
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
      }}
    >
      {task.text}
      {isGhost && Math.abs(dx) < 4 && (
        <span style={{
          position: 'absolute', right: 16, top: 0,
          fontSize: 9, letterSpacing: 1.5, color: C.muted,
          opacity: 0.55, textTransform: 'uppercase',
          textDecoration: 'none',
        }}>← undo</span>
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// EmptyRow — invitation to type
// ────────────────────────────────────────────────────────────────────
function EmptyRow({ theme, fontFamily, onTap, isFirstEmpty }) {
  const C = TOKENS[theme];
  return (
    <div
      onClick={onTap}
      style={{
        height: 26, fontFamily, fontSize: 14, lineHeight: '26px',
        color: C.muted,
        cursor: 'text',
        position: 'relative',
      }}
    >
      {isFirstEmpty && (
        <span style={{
          position: 'absolute', left: 0, top: 5,
          width: 1.5, height: 16, background: C.muted, opacity: 0.4,
          animation: 'tono-blink 1.1s steps(2) infinite',
        }} />
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// EditingRow — active input
// ────────────────────────────────────────────────────────────────────
function EditingRow({ theme, fontFamily, value, onChange, onCommit, onCancel }) {
  const C = TOKENS[theme];
  const ref = useRef(null);
  useEffect(() => {
    ref.current?.focus();
  }, []);
  return (
    <input
      ref={ref}
      value={value}
      onChange={e => onChange(e.target.value)}
      onKeyDown={e => {
        if (e.key === 'Enter') { e.preventDefault(); onCommit(); }
        else if (e.key === 'Escape') { e.preventDefault(); onCancel(); }
      }}
      onBlur={onCommit}
      style={{
        width: '100%', boxSizing: 'border-box',
        height: 26, padding: 0, paddingRight: 16,
        fontFamily, fontSize: 14, lineHeight: '26px',
        color: C.ink, background: 'transparent',
        border: 'none', outline: 'none', caretColor: C.ink,
      }}
    />
  );
}

// ────────────────────────────────────────────────────────────────────
// DaySection — heading + tasks + empty input lines
// ────────────────────────────────────────────────────────────────────
function DaySection({
  day, tasks, ghosts, theme, fontFamily, dayRef,
  isDropTarget, editingValue, onStartEdit, onEditChange, onCommit, onCancel,
  draggingId, swipeMap, taskHandlers,
  isFirstEmptyInList,
}) {
  const C = TOKENS[theme];
  return (
    <div
      ref={dayRef}
      data-day-section={day.key}
      style={{
        position: 'relative',
        marginTop: 18,
        paddingLeft: 16,
        borderLeft: day.isToday ? `3px solid ${C.today}` : '3px solid transparent',
        background: isDropTarget ? C.drop : 'transparent',
        transition: 'background 0.15s ease',
      }}
    >
      {/* heading row */}
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
        fontFamily, fontSize: 11, letterSpacing: 2,
        color: C.ink, paddingRight: 16,
        textTransform: 'uppercase',
        fontWeight: day.isToday ? 700 : 500,
        opacity: day.isWeekend && !day.isToday ? 0.55 : 1,
      }}>
        <span>{day.day}{day.isToday ? ' · today' : ''}</span>
        <span style={{ color: C.muted, fontWeight: 400 }}>{day.date} {day.month}</span>
      </div>
      <div style={{ height: 1, background: C.hair, marginTop: 6, marginRight: 16 }} />

      {/* task list */}
      <div style={{ paddingTop: 6, paddingBottom: 2 }}>
        {tasks.map(t => (
          <TaskRow
            key={t.id}
            task={t}
            dayKey={day.key}
            theme={theme}
            fontFamily={fontFamily}
            isDragging={draggingId === t.id}
            swipeState={swipeMap[t.id]}
            onPointerHandlers={taskHandlers(t.id, day.key, 'live')}
            variant="live"
          />
        ))}

        {/* recently-completed ghost rows — swipe LEFT on one to undo */}
        {(ghosts || []).map(g => (
          <TaskRow
            key={`ghost-${g.id}`}
            task={g}
            dayKey={day.key}
            theme={theme}
            fontFamily={fontFamily}
            swipeState={swipeMap[g.id]}
            onPointerHandlers={taskHandlers(g.id, day.key, 'ghost')}
            variant="ghost"
          />
        ))}

        {/* editing input replaces / appends the first empty line */}
        {editingValue !== null && editingValue !== undefined ? (
          <EditingRow
            theme={theme} fontFamily={fontFamily}
            value={editingValue}
            onChange={onEditChange}
            onCommit={onCommit}
            onCancel={onCancel}
          />
        ) : (
          <EmptyRow theme={theme} fontFamily={fontFamily}
                    onTap={onStartEdit}
                    isFirstEmpty={isFirstEmptyInList} />
        )}
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────
// Main App
// ────────────────────────────────────────────────────────────────────
function TonoApp({ theme, fontFamily, onEditingChange }) {
  const C = TOKENS[theme];
  const days = useMemo(() => buildDays(), []);
  const [tasks, setTasks] = useState(() => loadTasks(days));
  const [recents, setRecents] = useState({});   // { dayKey: [{id,text,completedAt}] } — ghosts available for undo
  const [editing, setEditing] = useState(null); // { dayKey, value }
  const [swipe, setSwipe] = useState({});       // { taskId: { dx, snapping } }
  const [drag, setDrag] = useState(null);       // { taskId, fromDay, x, y, overDay }
  const dayRefs = useRef({});
  const scrollerRef = useRef(null);

  // refs mirror latest state so gesture handlers can stay stable
  const tasksRef = useRef(tasks);   useEffect(() => { tasksRef.current = tasks; }, [tasks]);
  const recentsRef = useRef(recents); useEffect(() => { recentsRef.current = recents; }, [recents]);
  const swipeRef = useRef(swipe);   useEffect(() => { swipeRef.current = swipe; }, [swipe]);
  const recentsTimers = useRef({}); // { taskId: timeoutId }

  const RECENTS_TTL = 6500;
  const REMOVE_ANIM = 240;

  // Persist whenever tasks change
  useEffect(() => { saveTasks(tasks); }, [tasks]);

  // Notify parent (so the phone frame can pop the keyboard)
  useEffect(() => { onEditingChange?.(!!editing); }, [editing, onEditingChange]);

  // When entering edit mode, scroll the editing day toward the top so it
  // stays visible above the keyboard.
  useEffect(() => {
    if (!editing) return;
    const el = dayRefs.current[editing.dayKey];
    if (el && scrollerRef.current) {
      const top = el.offsetTop - 12;
      scrollerRef.current.scrollTo({ top, behavior: 'smooth' });
    }
  }, [editing?.dayKey]);

  // Scroll today into view on mount
  useLayoutEffect(() => {
    const todayKey = days.find(d => d.isToday)?.key;
    const el = dayRefs.current[todayKey];
    if (el && scrollerRef.current) {
      const top = el.offsetTop - 12;
      scrollerRef.current.scrollTop = top;
    }
  }, []);

  // ── Complete / restore helpers ────────────────────────────────────
  const completeTask = useCallback((taskId, dayKey, text) => {
    setTasks(prev => {
      const next = { ...prev };
      next[dayKey] = (next[dayKey] || []).filter(t => t.id !== taskId);
      return next;
    });
    setRecents(prev => {
      const next = { ...prev };
      next[dayKey] = [...(next[dayKey] || []), { id: taskId, text, completedAt: Date.now() }];
      return next;
    });
    if (recentsTimers.current[taskId]) clearTimeout(recentsTimers.current[taskId]);
    recentsTimers.current[taskId] = setTimeout(() => {
      setRecents(prev => {
        const next = { ...prev };
        next[dayKey] = (next[dayKey] || []).filter(g => g.id !== taskId);
        return next;
      });
      delete recentsTimers.current[taskId];
    }, RECENTS_TTL);
  }, []);

  const restoreTask = useCallback((taskId, dayKey, text) => {
    if (recentsTimers.current[taskId]) {
      clearTimeout(recentsTimers.current[taskId]);
      delete recentsTimers.current[taskId];
    }
    setRecents(prev => {
      const next = { ...prev };
      next[dayKey] = (next[dayKey] || []).filter(g => g.id !== taskId);
      return next;
    });
    setTasks(prev => {
      const next = { ...prev };
      next[dayKey] = [...(next[dayKey] || []), { id: taskId, text }];
      return next;
    });
  }, []);

  // ── Gesture state machine (refs to avoid stale closures) ─────────
  const gesture = useRef(null);

  const taskHandlers = useCallback((taskId, dayKey, variant = 'live') => ({
    onPointerDown: (e) => {
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      const startX = e.clientX, startY = e.clientY;
      e.currentTarget.setPointerCapture?.(e.pointerId);
      gesture.current = {
        taskId, dayKey, variant, startX, startY,
        mode: 'idle', // idle | swipe | drag | scroll
        // ghosts don't long-press into drag; only live tasks do.
        timer: variant === 'live' ? setTimeout(() => {
          if (gesture.current && gesture.current.mode === 'idle') {
            gesture.current.mode = 'drag';
            setDrag({ taskId, fromDay: dayKey,
                     x: gesture.current.lastX || startX,
                     y: gesture.current.lastY || startY, overDay: dayKey });
            if (navigator.vibrate) navigator.vibrate(8);
          }
        }, LONGPRESS_MS) : null,
      };
    },
    onPointerMove: (e) => {
      const g = gesture.current;
      if (!g || g.taskId !== taskId) return;
      const dx = e.clientX - g.startX;
      const dy = e.clientY - g.startY;
      g.lastX = e.clientX; g.lastY = e.clientY;

      if (g.mode === 'idle') {
        if (Math.abs(dy) > MOVE_LOCK && Math.abs(dy) > Math.abs(dx)) {
          if (g.timer) clearTimeout(g.timer);
          g.mode = 'scroll';
          e.currentTarget.releasePointerCapture?.(e.pointerId);
          return;
        }
        if (Math.abs(dx) > MOVE_LOCK) {
          // direction-gate: live only accepts right (dx > 0); ghost only accepts left.
          if (variant === 'live' && dx < 0) { /* stay idle, no swipe-left on live */ return; }
          if (variant === 'ghost' && dx > 0) { /* stay idle, no swipe-right on ghost */ return; }
          if (g.timer) clearTimeout(g.timer);
          g.mode = 'swipe';
        } else {
          return;
        }
      }

      if (g.mode === 'swipe') {
        const clamped = variant === 'live' ? Math.max(0, dx) : Math.min(0, dx);
        setSwipe(s => ({ ...s, [taskId]: { dx: clamped, snapping: false } }));
      } else if (g.mode === 'drag') {
        let over = dayKey;
        for (const [k, el] of Object.entries(dayRefs.current)) {
          if (!el) continue;
          const r = el.getBoundingClientRect();
          if (e.clientY >= r.top && e.clientY <= r.bottom) { over = k; break; }
        }
        setDrag({ taskId, fromDay: dayKey, x: e.clientX, y: e.clientY, overDay: over });
      }
    },
    onPointerUp: (e) => {
      const g = gesture.current;
      if (!g || g.taskId !== taskId) return;
      if (g.timer) clearTimeout(g.timer);

      if (g.mode === 'swipe') {
        const dxLast = swipeRef.current[taskId]?.dx ?? 0;
        const crossed = variant === 'live'
          ? dxLast > SWIPE_THRESHOLD
          : dxLast < -SWIPE_THRESHOLD;

        if (crossed) {
          // capture text now — state may shift before timeout fires
          const text = variant === 'live'
            ? tasksRef.current[dayKey]?.find(t => t.id === taskId)?.text
            : recentsRef.current[dayKey]?.find(gh => gh.id === taskId)?.text;
          const target = variant === 'live' ? 480 : -480;
          // animate off-screen first
          setSwipe(s => ({ ...s, [taskId]: { dx: target, snapping: true } }));
          setTimeout(() => {
            if (text) {
              if (variant === 'live') completeTask(taskId, dayKey, text);
              else restoreTask(taskId, dayKey, text);
            }
            setSwipe(s => { const n = { ...s }; delete n[taskId]; return n; });
          }, REMOVE_ANIM);
        } else {
          // snap back — animate dx → 0, then clear
          setSwipe(s => ({ ...s, [taskId]: { dx: 0, snapping: true } }));
          setTimeout(() => {
            setSwipe(s => { const n = { ...s }; delete n[taskId]; return n; });
          }, 240);
        }
      } else if (g.mode === 'drag') {
        setDrag(d => {
          const dest = d?.overDay;
          if (dest && dest !== dayKey) {
            setTasks(prev => {
              const next = { ...prev };
              const moved = (next[dayKey] || []).find(t => t.id === taskId);
              if (!moved) return prev;
              next[dayKey] = next[dayKey].filter(t => t.id !== taskId);
              next[dest] = [...(next[dest] || []), moved];
              return next;
            });
          }
          return null;
        });
      }
      gesture.current = null;
    },
    onPointerCancel: () => {
      const g = gesture.current;
      if (g && g.timer) clearTimeout(g.timer);
      // gentle snap back if mid-swipe
      setSwipe(s => {
        if (s[taskId]) {
          return { ...s, [taskId]: { dx: 0, snapping: true } };
        }
        return s;
      });
      setTimeout(() => {
        setSwipe(s => { const n = { ...s }; delete n[taskId]; return n; });
      }, 240);
      setDrag(null);
      gesture.current = null;
    },
  }), [completeTask, restoreTask]);

  const startEdit = (dayKey) => setEditing({ dayKey, value: '' });
  const commitEdit = () => {
    if (!editing) return;
    const txt = editing.value.trim();
    if (txt) {
      setTasks(prev => {
        const next = { ...prev };
        next[editing.dayKey] = [
          ...(next[editing.dayKey] || []),
          { id: `${editing.dayKey}-${Date.now()}-${Math.random().toString(36).slice(2,5)}`, text: txt },
        ];
        return next;
      });
      // keep editing the same day to allow chaining (like a real editor)
      setEditing({ dayKey: editing.dayKey, value: '' });
    } else {
      setEditing(null);
    }
  };
  const cancelEdit = () => setEditing(null);

  // Resolve the task being dragged so we can render a ghost
  const dragTask = useMemo(() => {
    if (!drag) return null;
    return tasks[drag.fromDay]?.find(t => t.id === drag.taskId);
  }, [drag, tasks]);

  // count nbr of total tasks shown (for empty-state messaging — not used)
  return (
    <div style={{
      height: '100%', width: '100%',
      background: C.paper,
      fontFamily, color: C.ink,
      display: 'flex', flexDirection: 'column',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* tiny status row — file name and week range */}
      <div style={{
        padding: '14px 20px 4px',
        display: 'flex', justifyContent: 'space-between',
        fontFamily, fontSize: 10, letterSpacing: 2, color: C.muted,
        textTransform: 'uppercase',
      }}>
        <span>{days[0].date} {days[0].month} — {days[13].date} {days[13].month}</span>
        <span>tono</span>
      </div>

      {/* scrollable editor body */}
      <div
        ref={scrollerRef}
        style={{
          flex: 1, overflowY: 'auto', overflowX: 'hidden',
          padding: '4px 0 80px',
          WebkitOverflowScrolling: 'touch',
          scrollbarWidth: 'none',
        }}
      >
        {days.map((d, i) => {
          const isWeekStart = i === 7;
          return (
            <React.Fragment key={d.key}>
              {isWeekStart && (
                <div style={{
                  textAlign: 'center', margin: '32px 16px 4px',
                  fontFamily, fontSize: 10, letterSpacing: 3, color: C.muted,
                  textTransform: 'uppercase',
                }}>— next week —</div>
              )}
              <DaySection
                day={d}
                tasks={tasks[d.key] || []}
                ghosts={recents[d.key] || []}
                theme={theme}
                fontFamily={fontFamily}
                dayRef={el => { dayRefs.current[d.key] = el; }}
                isDropTarget={drag?.overDay === d.key && drag?.fromDay !== d.key}
                editingValue={editing?.dayKey === d.key ? editing.value : null}
                onStartEdit={() => startEdit(d.key)}
                onEditChange={v => setEditing({ ...editing, value: v })}
                onCommit={commitEdit}
                onCancel={cancelEdit}
                draggingId={drag?.taskId}
                swipeMap={swipe}
                taskHandlers={taskHandlers}
                isFirstEmptyInList={d.isToday && !editing && (tasks[d.key] || []).length === 0}
              />
            </React.Fragment>
          );
        })}
      </div>

      {/* drag ghost */}
      {drag && dragTask && (
        <div style={{
          position: 'fixed', left: 0, top: 0,
          transform: `translate(${drag.x - 100}px, ${drag.y - 16}px)`,
          width: 200, padding: '4px 14px',
          background: C.paper,
          fontFamily, fontSize: 14, lineHeight: '24px', color: C.ink,
          boxShadow: '0 8px 24px rgba(0,0,0,0.25), 0 0 0 1px rgba(0,0,0,0.06)',
          borderRadius: 4,
          pointerEvents: 'none',
          zIndex: 100,
        }}>{dragTask.text}</div>
      )}
    </div>
  );
}

Object.assign(window, { TonoApp });
