import { cn } from '@/lib/utils';

/*
 * Signature pieces that belong to one visual theme each (src/themes/*.css). They are always in the
 * DOM — only the matching theme shows them (`.only-<theme>` in themes/base.css) — which keeps the
 * Mudra markup untouched and avoids a theme check in every page. All are decorative or restate
 * text that is already on screen, so they are `aria-hidden`.
 */

const clamp01 = (n: number) => Math.min(Math.max(n, 0), 1);

/** Gulal: a price-tag burst stamped on the hero — how much of the budget is used. */
export function BurstBadge({ text, className }: { text: string; className?: string }) {
  const points = Array.from({ length: 28 }, (_, i) => {
    const a = (Math.PI * i) / 14;
    const r = i % 2 ? 31 : 38;
    return `${(40 + Math.cos(a) * r).toFixed(1)},${(40 + Math.sin(a) * r).toFixed(1)}`;
  }).join(' ');
  return (
    <span className={cn('only-gulal burst-badge', className)} aria-hidden>
      <svg viewBox="0 0 80 80" focusable="false">
        <polygon points={points} strokeLinejoin="round" />
      </svg>
      <b>{text}</b>
      <small>used</small>
    </span>
  );
}

/**
 * Shunya: the period as a hairline of day ticks with today marked, and spend drawn on the same
 * axis. Spend short of the "today" mark reads as under pace without a word of copy.
 */
export function MonthLine({
  elapsed,
  spent,
  className,
}: {
  /** 0–1, how far through the budget period today is. */
  elapsed: number;
  /** 0–100, how much of the budget is spent. */
  spent: number;
  className?: string;
}) {
  const e = clamp01(elapsed) * 100;
  const underPace = spent <= e;
  return (
    <div className={cn('only-shunya month-line', className)} aria-hidden>
      <div className="month-line-now" style={{ left: `${e}%` }}>
        <span>Today</span>
      </div>
      <div className="month-line-days">
        {Array.from({ length: 31 }, (_, i) => (
          <i key={i} data-key={i === 0 || i === 14 || i === 30 ? '' : undefined} />
        ))}
      </div>
      <div className="month-line-spend">
        <i style={{ width: `${Math.min(Math.max(spent, 0), 100)}%` }} />
      </div>
      <p data-pace={underPace ? 'under' : 'over'}>{underPace ? 'Under pace' : 'Ahead of pace'}</p>
    </div>
  );
}

/** Bagh: the sun rides an arc across the hero; where it sits is how far through the month you are. */
export function SunArc({ elapsed, className }: { elapsed: number; className?: string }) {
  const t = clamp01(elapsed);
  // Quadratic bezier from (10,86) via (171,-36) to (332,86).
  const x = (1 - t) ** 2 * 10 + 2 * (1 - t) * t * 171 + t ** 2 * 332;
  const y = (1 - t) ** 2 * 86 + 2 * (1 - t) * t * -36 + t ** 2 * 86;
  return (
    <div className={cn('only-bagh sun-arc', className)} aria-hidden>
      <svg viewBox="0 0 342 92" focusable="false">
        <path
          d="M10 86Q171-36 332 86"
          fill="none"
          strokeDasharray="2 6"
          strokeLinecap="round"
          className="sun-arc-path"
        />
        <circle cx={x} cy={y} r="22" className="sun-arc-glow" />
        <circle cx={x} cy={y} r="11.5" className="sun-arc-sun" />
      </svg>
    </div>
  );
}

/** Bagh: a goal drawn as a plant — seed, sprout, sapling, tree as it fills. */
export function GoalPlant({ percent, className }: { percent: number; className?: string }) {
  const stage = percent >= 100 ? 3 : percent >= 50 ? 2 : percent > 0 ? 1 : 0;
  return (
    <svg
      className={cn('only-bagh goal-plant', className)}
      width="40"
      height="40"
      viewBox="0 0 48 48"
      aria-hidden
      focusable="false"
    >
      <ellipse cx="24" cy="42" rx="15" ry="4.5" className="plant-soil" />
      {stage === 0 && (
        <>
          <ellipse cx="24" cy="39" rx="3.6" ry="2.4" className="plant-seed" />
          <path d="M24 36.4v-1.6" className="plant-stem" strokeWidth="1.6" strokeLinecap="round" />
        </>
      )}
      {stage === 1 && (
        <>
          <path d="M24 40V25" className="plant-stem" strokeWidth="3" strokeLinecap="round" />
          <path d="M24 29c-8 0-11-6-10-10 7-1 10 3 10 10z" className="plant-leaf" />
          <path d="M24 25c7 0 10-5 9-9-6-1-9 3-9 9z" className="plant-leaf-2" />
        </>
      )}
      {stage === 2 && (
        <>
          <path d="M24 40V18" className="plant-stem" strokeWidth="3" strokeLinecap="round" />
          <path d="M24 34c-9 0-12-6-11-10 8-1 11 3 11 10z" className="plant-leaf" />
          <path d="M24 28c8 0 11-5 10-9-7-1-10 3-10 9z" className="plant-leaf-2" />
          <path d="M24 20c-6 0-8-5-7-8 6-1 8 3 7 8z" className="plant-leaf" />
          <path d="M24 18c6 0 8-4 7-7-5-1-8 2-7 7z" className="plant-leaf-2" />
        </>
      )}
      {stage === 3 && (
        <>
          <path d="M24 41V24" className="plant-stem" strokeWidth="4" strokeLinecap="round" />
          <circle cx="24" cy="18" r="11" className="plant-leaf" />
          <circle cx="16" cy="22" r="7" className="plant-leaf-2" />
          <circle cx="32" cy="22" r="7" className="plant-leaf-2" />
          <circle cx="20" cy="14" r="2" className="plant-seed" />
          <circle cx="29" cy="19" r="2" className="plant-seed" />
        </>
      )}
    </svg>
  );
}
