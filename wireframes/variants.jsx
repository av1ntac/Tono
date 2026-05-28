// variants.jsx — three Android wireframes for the text-editor to-do app.
// Each variant shares: cream paper, mono body, today is the only color,
// no checkboxes, no icons. Differs in how day subheadings are typeset and
// how the "tap empty line" affordance reads.

const PAPER = '#f5f1e8';
const INK   = '#1a1814';
const MUTED = '#9a948a';
const HAIR  = '#d8d2c4';
const TODAY = '#f7e07a';  // soft highlighter yellow — the only color in the UI

// shared body font stack (mono) and writing font (for annotations only)
const MONO  = "'JetBrains Mono','IBM Plex Mono',ui-monospace,monospace";
const HAND  = "'Caveat','Architects Daughter',cursive";

// ─────────────────────────────────────────────────────────────────────
// Sample data — two weeks. day 0 = today (Mon).
// gaps in `tasks` model "completed tasks fully hidden" — the row went away.
// ─────────────────────────────────────────────────────────────────────
const WEEK_DATA = [
  // current week (today = index 0, Mon)
  { day: 'Mon', date: 27, isToday: true,  tasks: ['stand-up @ 10', 'review PR #482', 'tono onboarding sketch'] },
  { day: 'Tue', date: 28, tasks: ['gym', 'call mom', 'pick up package'] },
  { day: 'Wed', date: 29, tasks: ['design crit 2pm', 'ship v0.4'] },
  { day: 'Thu', date: 30, tasks: ['1:1 with sam'] },
  { day: 'Fri', date: 31, tasks: ['week retro', 'pay rent'] },
  { day: 'Sat', date: 1,  tasks: ['groceries'] },
  { day: 'Sun', date: 2,  tasks: [] },
  // next week — compressed
  { day: 'Mon', date: 3, tasks: ['flight to berlin 06:40'] },
  { day: 'Tue', date: 4, tasks: ['conference day 1'] },
  { day: 'Wed', date: 5, tasks: [] },
  { day: 'Thu', date: 6, tasks: ['fly home'] },
  { day: 'Fri', date: 7, tasks: [] },
  { day: 'Sat', date: 8, tasks: [] },
  { day: 'Sun', date: 9, tasks: [] },
];

// ─────────────────────────────────────────────────────────────────────
// Shared bits
// ─────────────────────────────────────────────────────────────────────

// A single "row" in the editor — task text or empty line. Empty lines are
// the affordance: tap one to start typing.
function EditorRow({ children, empty, indent = 16, dragging, swipingOut, cursor, fontSize = 13, lineHeight = 22 }) {
  const style = {
    fontFamily: MONO,
    fontSize, lineHeight: `${lineHeight}px`,
    color: empty ? 'transparent' : INK,
    paddingLeft: indent, paddingRight: 12,
    minHeight: lineHeight,
    position: 'relative',
    whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
    background: swipingOut ? `linear-gradient(90deg, ${TODAY} 0%, ${TODAY} 60%, transparent 100%)` : 'transparent',
    opacity: swipingOut ? 0.55 : 1,
    textDecoration: swipingOut ? 'line-through' : 'none',
    transform: dragging ? 'translateX(6px)' : 'none',
    boxShadow: dragging ? `inset 2px 0 0 ${INK}` : 'none',
  };
  return (
    <div style={style}>
      {/* blinking cursor on the empty line user is about to tap into */}
      {cursor && (
        <span style={{
          position: 'absolute', left: indent, top: 3,
          width: 1.5, height: lineHeight - 6, background: INK,
          animation: 'blink 1s steps(2) infinite',
        }} />
      )}
      {empty ? '\u00A0' : children}
    </div>
  );
}

// Wireframe annotation outside the phone — handwritten label + arrow
function Note({ x, y, w = 160, children, align = 'left', arrowTo }) {
  return (
    <div style={{
      position: 'absolute', left: x, top: y, width: w,
      fontFamily: HAND, fontSize: 17, lineHeight: 1.15,
      color: '#5a5246', textAlign: align,
      pointerEvents: 'none',
    }}>
      {children}
    </div>
  );
}

// little SVG arrow between an annotation and the phone
function Arrow({ x1, y1, x2, y2, curve = 30 }) {
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2 - curve;
  return (
    <svg style={{ position: 'absolute', left: 0, top: 0, width: '100%', height: '100%', pointerEvents: 'none', overflow: 'visible' }}>
      <path d={`M${x1},${y1} Q${mx},${my} ${x2},${y2}`}
            fill="none" stroke="#5a5246" strokeWidth="1.3" strokeLinecap="round"
            strokeDasharray="0" />
      <path d={`M${x2 - 6},${y2 - 5} L${x2},${y2} L${x2 - 7},${y2 + 3}`}
            fill="none" stroke="#5a5246" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// Status-bar-ish strip inside the phone (no real app bar — the editor IS the app)
function PaperHeader({ children }) {
  return (
    <div style={{
      padding: '14px 16px 8px',
      fontFamily: MONO, fontSize: 11, color: MUTED, letterSpacing: 0.5,
      display: 'flex', justifyContent: 'space-between',
    }}>
      {children}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────
// VARIANT A — Raw markdown editor
// `## Mon 27` headings literal; `-` bullets visible; today highlighted
// with a yellow strip behind the heading line.
// ─────────────────────────────────────────────────────────────────────
function VariantA({ density = 'cozy' }) {
  const lh = density === 'tight' ? 20 : 22;
  const empty1 = 1; // one empty line under each day for "tap to add"

  return (
    <div style={{ background: PAPER, height: '100%', overflow: 'hidden', paddingTop: 4 }}>
      <PaperHeader>
        <span>tono.md</span>
        <span>w22 · w23</span>
      </PaperHeader>

      <div style={{ padding: '4px 0 16px' }}>
        <div style={{
          fontFamily: MONO, fontSize: 13, lineHeight: `${lh}px`,
          paddingLeft: 16, paddingRight: 12, color: MUTED,
        }}># this week</div>

        {WEEK_DATA.slice(0, 7).map((d, i) => (
          <div key={i} style={{ marginTop: i === 0 ? 6 : 10 }}>
            <div style={{
              fontFamily: MONO, fontSize: 14, lineHeight: `${lh}px`,
              paddingLeft: 16, paddingRight: 12,
              color: INK, fontWeight: 600,
              background: d.isToday
                ? `linear-gradient(180deg, transparent 0%, transparent 18%, ${TODAY} 18%, ${TODAY} 82%, transparent 82%)`
                : 'transparent',
              display: 'inline-block',
            }}>## {d.day} {d.date}</div>
            {d.tasks.map((t, j) => (
              <EditorRow key={j} indent={16} fontSize={13} lineHeight={lh}
                         swipingOut={i === 1 && j === 0}
                         dragging={i === 2 && j === 0}>
                - {t}
              </EditorRow>
            ))}
            {/* one inviting empty line per day */}
            <EditorRow empty indent={16} fontSize={13} lineHeight={lh}
                       cursor={i === 0}>
              -{' '}
            </EditorRow>
          </div>
        ))}

        <div style={{
          fontFamily: MONO, fontSize: 13, lineHeight: `${lh}px`,
          paddingLeft: 16, marginTop: 16, color: MUTED,
        }}># next week</div>

        {WEEK_DATA.slice(7).map((d, i) => (
          <div key={i} style={{ marginTop: 6 }}>
            <div style={{
              fontFamily: MONO, fontSize: 13, lineHeight: `${lh}px`,
              paddingLeft: 16, color: INK, opacity: 0.75,
            }}>## {d.day} {d.date}</div>
            {d.tasks.map((t, j) => (
              <EditorRow key={j} indent={16} fontSize={13} lineHeight={lh}>- {t}</EditorRow>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────
// VARIANT B — iA Writer / hairline rule
// Uppercase day on the left, date right-aligned, thin hr below.
// Today gets a left-edge yellow bar across the whole day section.
// ─────────────────────────────────────────────────────────────────────
function VariantB({ density = 'cozy' }) {
  const lh = density === 'tight' ? 20 : 24;

  function Day({ d, i, dim }) {
    return (
      <div style={{
        marginTop: i === 0 ? 0 : 14,
        borderLeft: d.isToday ? `3px solid ${TODAY}` : '3px solid transparent',
        paddingLeft: d.isToday ? 13 : 13,
        opacity: dim ? 0.7 : 1,
      }}>
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
          fontFamily: MONO, fontSize: 11, letterSpacing: 2,
          color: INK, paddingRight: 16,
          textTransform: 'uppercase',
          fontWeight: d.isToday ? 700 : 500,
        }}>
          <span>{d.day.toUpperCase()}{d.isToday && ' · today'}</span>
          <span style={{ color: MUTED }}>{d.date} may</span>
        </div>
        <div style={{ height: 1, background: HAIR, marginTop: 6, marginRight: 16 }} />
        <div style={{ paddingTop: 4 }}>
          {d.tasks.map((t, j) => (
            <EditorRow key={j} indent={0} fontSize={13} lineHeight={lh}
                       swipingOut={i === 1 && j === 0}
                       dragging={i === 3 && j === 0}>
              {t}
            </EditorRow>
          ))}
          <EditorRow empty indent={0} fontSize={13} lineHeight={lh} cursor={i === 0}>
            {' '}
          </EditorRow>
        </div>
      </div>
    );
  }

  return (
    <div style={{ background: PAPER, height: '100%', overflow: 'hidden' }}>
      <PaperHeader>
        <span>27 — 9 may</span>
        <span>tono</span>
      </PaperHeader>
      <div style={{ padding: '8px 0 16px 16px' }}>
        {WEEK_DATA.slice(0, 7).map((d, i) => (
          <Day key={i} d={d} i={i} />
        ))}
        <div style={{
          marginTop: 22, marginRight: 16, marginBottom: 10,
          fontFamily: MONO, fontSize: 10, letterSpacing: 2, color: MUTED,
          textAlign: 'center', textTransform: 'uppercase',
        }}>— next week —</div>
        {WEEK_DATA.slice(7).map((d, i) => (
          <Day key={i} d={d} i={i + 7} dim />
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────
// VARIANT C — Date-led, big numeral
// Date is the visual anchor; day name is small caps next to it.
// Today's number sits inside a yellow square.
// ─────────────────────────────────────────────────────────────────────
function VariantC({ density = 'cozy' }) {
  const lh = density === 'tight' ? 20 : 22;
  const GUTTER = 52;

  function Day({ d, i, dim }) {
    return (
      <div style={{
        display: 'grid', gridTemplateColumns: `${GUTTER}px 1fr`,
        marginTop: i === 0 ? 0 : 10, alignItems: 'start',
        opacity: dim ? 0.6 : 1,
      }}>
        {/* date gutter */}
        <div style={{ paddingTop: 2, paddingLeft: 12 }}>
          <div style={{
            width: 30, height: 30, display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: MONO, fontSize: 18, fontWeight: 600, color: INK,
            background: d.isToday ? TODAY : 'transparent',
            borderRadius: 2,
          }}>{d.date}</div>
          <div style={{
            fontFamily: MONO, fontSize: 9, letterSpacing: 1.5, color: MUTED,
            marginTop: 2, paddingLeft: 4, textTransform: 'uppercase',
          }}>{d.day}</div>
        </div>
        {/* lines */}
        <div style={{ paddingTop: 8, paddingRight: 12 }}>
          {d.tasks.length === 0 && (
            <EditorRow empty indent={0} fontSize={13} lineHeight={lh} cursor={i === 0}>
              {' '}
            </EditorRow>
          )}
          {d.tasks.map((t, j) => (
            <EditorRow key={j} indent={0} fontSize={13} lineHeight={lh}
                       swipingOut={i === 1 && j === 0}
                       dragging={i === 2 && j === 0}>
              {t}
            </EditorRow>
          ))}
          {d.tasks.length > 0 && (
            <EditorRow empty indent={0} fontSize={13} lineHeight={lh} cursor={i === 0}>
              {' '}
            </EditorRow>
          )}
        </div>
      </div>
    );
  }

  return (
    <div style={{ background: PAPER, height: '100%', overflow: 'hidden' }}>
      <PaperHeader>
        <span>may · w22 / w23</span>
        <span>·  ·  ·</span>
      </PaperHeader>
      <div style={{ padding: '6px 0 16px' }}>
        {WEEK_DATA.slice(0, 7).map((d, i) => <Day key={i} d={d} i={i} />)}
        <div style={{ height: 1, background: HAIR, margin: '18px 16px 10px 12px' }} />
        {WEEK_DATA.slice(7).map((d, i) => <Day key={i} d={d} i={i + 7} dim />)}
      </div>
    </div>
  );
}

Object.assign(window, { VariantA, VariantB, VariantC, Note, Arrow });
