import type { HeroExtraProps, HeroInfo } from './hero-info';

/** Beyond this many days a stroke per day stops being readable and the tally becomes a hatched bar. */
const TALLY_MAX_DAYS = 62;
const GATE_W = 44;

// Fixed jitter so every stroke leans a little differently, and never between renders.
const JIT = [0, 0.9, -0.7, 0.5, -1, 0.4, 0.8, -0.5];
const j = (n: number) => JIT[((n % 8) + 8) % 8];

/** A hand-drawn ring around a centre that overshoots where it starts. */
function ringPath(cx: number, cy: number, rx: number, ry: number) {
  const p = (x: number, y: number) => `${(cx + x * rx).toFixed(1)} ${(cy + y * ry).toFixed(1)}`;
  return `M${p(-0.55, -0.85)}C${p(0.1, -1.15)} ${p(1, -0.8)} ${p(1, 0)}C${p(1, 0.8)} ${p(0.3, 1.05)} ${p(-0.3, 0.95)}C${p(-0.9, 0.85)} ${p(-1.05, 0.2)} ${p(-0.9, -0.35)}C${p(-0.75, -0.85)} ${p(-0.2, -1.08)} ${p(0.5, -0.92)}`;
}

/**
 * One stroke per day, struck through in groups of five (four uprights and a diagonal): the days
 * gone are inked, today is ringed, the days to come are faint.
 */
function Tally({ info }: { info: HeroInfo }) {
  const total = Math.max(1, Math.round(info.totalDays));
  const today = Math.min(Math.max(Math.round(info.dayOfPeriod), 1), total);
  const gates = Math.ceil(total / 5);
  const width = gates * GATE_W + 4;
  const state = (day: number) => (day < today ? 'go' : day === today ? 'td' : 'to');
  const strokes: React.ReactNode[] = [];
  for (let g = 0; g < gates; g++) {
    const gx = 12 + g * GATE_W;
    const inGate = Math.min(5, total - g * 5);
    for (let k = 0; k < Math.min(inGate, 4); k++) {
      const day = g * 5 + k + 1;
      const x = gx + k * 8;
      strokes.push(
        <path
          key={day}
          className={state(day)}
          d={`M${(x + j(day)).toFixed(1)} ${(9 + j(day + 2)).toFixed(1)}L${(x - j(day + 3)).toFixed(1)} ${(37 + j(day + 5)).toFixed(1)}`}
        />,
      );
    }
    if (inGate === 5) {
      const day = g * 5 + 5;
      strokes.push(
        <path
          key={day}
          className={state(day)}
          d={`M${gx - 6} ${(31 + j(g)).toFixed(1)}L${gx + 30} ${(14 - j(g + 1)).toFixed(1)}`}
        />,
      );
    }
  }
  const tg = Math.floor((today - 1) / 5);
  const ringX = 12 + tg * GATE_W + 12;
  return (
    <svg
      className="tk-tally"
      viewBox={`0 0 ${width} 46`}
      style={{ width: `min(100%, ${width}px)` }}
      aria-hidden
      focusable="false"
    >
      {strokes}
      <path className="tk-ring" pathLength={1} d={ringPath(ringX, 23, 25, 21)} />
    </svg>
  );
}

/** For a long period: a hatched bar, the same stroke as a budget bar, with today ringed on it. */
function HatchedPeriod({ info }: { info: HeroInfo }) {
  const gone = Math.min(Math.max(info.elapsed * 100, 0), 100);
  return (
    <div className="tk-hatch">
      <div className="tk-hatch-bar">
        <i style={{ width: `${gone}%` }} />
        <u style={{ left: `${gone}%` }} />
      </div>
    </div>
  );
}

/**
 * Takhti: the working shown. The sum is written out from the hero's own figures, ends in the
 * double-underlined answer (`figure` slot), and the period's days are tallied in gates of five
 * beneath it (`footer` slot). Text is never filtered — only the decorative strokes are.
 */
export function TakhtiExtra({ slot, info }: HeroExtraProps) {
  if (slot === 'figure') {
    const days = Math.max(info.daysLeft, 0);
    return (
      <>
        <div className="only-takhti tk-work" aria-hidden>
          <p className="tk-w1">
            {info.limitText} budget − {info.spentText} spent
            {info.isOver ? '' : ' ='} {info.isOver ? null : <b>{info.remainingText} left</b>}
          </p>
          {!info.isOver && days > 0 && (
            <p className="tk-w2">
              {info.remainingText} ÷ {days} day{days === 1 ? '' : 's'}
            </p>
          )}
        </div>
        <div className="only-takhti tk-ul" aria-hidden style={{ width: `${info.figure.length * 0.52}em` }}>
          <svg viewBox="0 0 300 14" preserveAspectRatio="none" focusable="false">
            <path
              pathLength={1}
              vectorEffect="non-scaling-stroke"
              d="M3 4C90 1 210 8 297 3.5C210 9 90 11 7 11"
            />
          </svg>
        </div>
      </>
    );
  }
  if (slot === 'footer') {
    const long = info.totalDays > TALLY_MAX_DAYS;
    return (
      <figure className="only-takhti tk-tl" aria-hidden>
        {long ? <HatchedPeriod info={info} /> : <Tally info={info} />}
        <figcaption>
          <span>
            Day {info.dayOfPeriod} of {info.totalDays}
          </span>
          <span>
            {info.daysLeft} day{info.daysLeft === 1 ? '' : 's'} to go
          </span>
          <span>{Math.round(info.spentPercent)}% spent</span>
        </figcaption>
      </figure>
    );
  }
  return null;
}
