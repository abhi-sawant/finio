import type { HeroExtraProps, HeroInfo } from './hero-info';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
/** Past this many days a grid of cells stops being a calendar, so the piece becomes a week strip. */
const MAX_GRID_DAYS = 42;

/** The hand-drawn red loop round today; drawn once (stroke-dashoffset) and then it just stays. */
function Ring() {
  return (
    <svg className="tk-ring" viewBox="0 0 44 30" preserveAspectRatio="none" aria-hidden>
      <path
        pathLength={1}
        d="M31 4.5C22 1 6 3 3.5 13.5c-2 9.5 12 14.5 24 13C38 25 43 17 40 10 37.5 4.5 29 2.5 20 3.5"
      />
    </svg>
  );
}

function dayOn(start: Date, offset: number) {
  return new Date(start.getFullYear(), start.getMonth(), start.getDate() + offset);
}

function MonthGrid({ info }: { info: HeroInfo }) {
  const { periodStart, totalDays, dayOfPeriod, figure } = info;
  const lead = periodStart.getDay();
  const cells: React.ReactNode[] = [];
  for (let i = 0; i < lead; i++) cells.push(<div key={`b${i}`} className="tk-c tk-blank" />);
  for (let n = 1; n <= totalDays; n++) {
    const d = dayOn(periodStart, n - 1);
    const sun = d.getDay() === 0;
    const state = n < dayOfPeriod ? 'past' : n === dayOfPeriod ? 'today' : 'next';
    const label = n === 1 || d.getDate() === 1 ? d.toLocaleDateString('en-IN', { month: 'short' }) : '';
    cells.push(
      <div
        key={n}
        className={`tk-c tk-${state}${sun ? ' tk-sun' : ''}`}
        style={{ ['--i' as string]: Math.min(n, 31) }}
      >
        <b>
          {d.getDate()}
          {state === 'today' && <Ring />}
        </b>
        {label && <small className="tk-mon">{label}</small>}
        {state === 'today' && <span className="tk-td">Today</span>}
        {state === 'next' && <span className="tk-al">{figure}</span>}
        {state === 'past' && <i className="tk-nm" />}
      </div>,
    );
  }
  return (
    <div className="tk-cal">
      {WEEKDAYS.map((w, i) => (
        <span key={w} className={`tk-wd${i === 0 ? ' tk-sun' : ''}`}>
          {w}
        </span>
      ))}
      {cells}
    </div>
  );
}

/** Long periods (a year) become a strip: one tick per week, today ringed. */
function WeekStrip({ info }: { info: HeroInfo }) {
  const weeks = Math.ceil(info.totalDays / 7);
  const nowWeek = Math.min(Math.floor((info.dayOfPeriod - 1) / 7), weeks - 1);
  return (
    <div className="tk-strip">
      {Array.from({ length: weeks }, (_, w) => (
        <i key={w} className={w < nowWeek ? 'tk-past' : w === nowWeek ? 'tk-today' : ''} />
      ))}
    </div>
  );
}

/**
 * Tareekh: the budget period drawn as a wall calendar. Sunday-first, past days dimmed, today ringed
 * in red, each day ahead carrying the safe-to-spend figure. Restates the hero text, so it is
 * decorative to assistive tech.
 */
export function TareekhExtra({ slot, info }: HeroExtraProps) {
  if (slot !== 'footer' || info.totalDays < 1) return null;
  const grid = info.totalDays <= MAX_GRID_DAYS;
  return (
    <div className="only-tareekh tk" aria-hidden>
      {grid ? <MonthGrid info={info} /> : <WeekStrip info={info} />}
      <p className="tk-key">
        <i className="tk-k tk-k-no" />
        Past
        <i className="tk-k tk-k-today" />
        Today
        <span>
          Day {info.dayOfPeriod} of {info.totalDays}
        </span>
      </p>
    </div>
  );
}
