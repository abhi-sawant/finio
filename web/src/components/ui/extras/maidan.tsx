import type { HeroExtraProps } from './hero-info';

const clamp = (n: number, lo: number, hi: number) => Math.min(Math.max(n, lo), hi);

// Chart space: x 0–310 is the whole period, y 92 is nothing spent, y 8 is the full budget.
const X_MAX = 310;
const Y_ZERO = 92;
const Y_FULL = 8;

/**
 * Maidan: the chase. In the `figure` slot the safe-to-spend figure stands on black number plates
 * (one per character, with the hook notch of a scoreboard digit), standing in for `.hero-fig`,
 * which the theme hides visually and keeps for screen readers. In the `footer` slot a par chart:
 * a dashed par line from nothing to the whole budget across the period, and the ball at today,
 * where spending actually stands. Only a straight line from the start to the ball is drawn — the
 * hero is not given daily history, so a stepped worm of spend would be invented.
 */
export function MaidanExtra({ slot, info }: HeroExtraProps) {
  if (slot === 'figure') {
    const len = [...info.figure].length;
    const size = len <= 6 ? 's1' : len <= 8 ? 's2' : 's3';
    return (
      <div
        className={`only-maidan mdn-plates-wrap${info.isOver ? ' mdn-over' : ''}`}
        aria-hidden
      >
        <span className={`mdn-plates mdn-${size}`}>
          {[...info.figure].map((c, i) =>
            /[\d•]/.test(c) ? (
              <b key={i}>{c}</b>
            ) : c === '₹' ? (
              <span key={i} className="mdn-rs">
                {c}
              </span>
            ) : (
              <i key={i}>{c}</i>
            ),
          )}
        </span>
      </div>
    );
  }

  if (slot !== 'footer') return null;

  const elapsed = clamp(info.elapsed, 0, 1);
  const par = elapsed * 100;
  const spent = Math.max(info.spentPercent, 0);
  const x = elapsed * X_MAX;
  const yOf = (pct: number) => Y_ZERO - (clamp(pct, 0, 108) / 100) * (Y_ZERO - Y_FULL);
  const ballY = yOf(spent);
  const gap = Math.round(spent - par);
  const day = info.dayOfPeriod;

  let caption: string;
  if (info.isOver) {
    caption = `Over budget: ${Math.round(spent - 100)}% past the limit at day ${day}`;
  } else if (gap === 0) {
    caption = `On par at day ${day}`;
  } else if (gap < 0) {
    caption = `${Math.abs(gap)}% of the budget under par at day ${day}`;
  } else {
    caption = `${gap}% of the budget over par at day ${day}`;
  }

  const ticks = Array.from({ length: info.totalDays }, (_, i) => {
    const tx = ((i + 1) / info.totalDays) * X_MAX;
    return `M${tx.toFixed(1)} ${Y_ZERO}v${(i + 1) % 5 === 0 ? 6 : 3}`;
  }).join('');
  const tail = (1 - elapsed) * 100;

  return (
    <figure className={`only-maidan mdn-par${info.isOver ? ' mdn-over' : ''}`} aria-hidden>
      <div className="mdn-plot">
        <svg viewBox={`0 0 ${X_MAX} 100`} preserveAspectRatio="none" fill="none">
          <path
            d={`M0 ${Y_ZERO}H${X_MAX}`}
            className="mdn-chalk"
            strokeWidth={1.5}
            vectorEffect="non-scaling-stroke"
          />
          <path d={ticks} className="mdn-chalk" strokeWidth={1} vectorEffect="non-scaling-stroke" />
          <path
            d={`M0 ${Y_ZERO}L${X_MAX} ${Y_FULL}`}
            className="mdn-parline"
            strokeWidth={2}
            strokeDasharray="6 5"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
        <svg
          className="mdn-run"
          viewBox={`0 0 ${X_MAX} 100`}
          preserveAspectRatio="none"
          fill="none"
          style={{ clipPath: `inset(-10px ${tail}% -10px 0)` }}
        >
          <path
            d={`M0 ${Y_ZERO}L${x.toFixed(1)} ${ballY.toFixed(1)}`}
            className="mdn-runline"
            strokeWidth={3.5}
            strokeLinecap="round"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
        <span
          className="mdn-ball"
          style={{ left: `${(elapsed * 100).toFixed(2)}%`, top: `${ballY.toFixed(1)}%` }}
        />
        <span className="mdn-lb mdn-lb-par">Limit {info.limitText}</span>
      </div>
      <div className="mdn-ax">
        <span>Day 1</span>
        <span style={{ left: `${(elapsed * 100).toFixed(2)}%` }}>Day {day}, today</span>
        <span>Day {info.totalDays}</span>
      </div>
      <figcaption>
        <i />
        {caption}
      </figcaption>
    </figure>
  );
}
