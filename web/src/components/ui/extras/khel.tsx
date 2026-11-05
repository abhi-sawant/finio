import { useState } from 'react';

/** Seconds of today already gone, as a fraction of the day — read once, on mount. */
function todayFraction() {
  const d = new Date();
  return (d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds()) / 86_400;
}

/**
 * Khel: the hero's HUD tag — "Day n/N" of the budget period, sat in the hero's top-right corner
 * like a handheld's stage counter. Worked out from the same `elapsed` / `daysLeft` the hero uses
 * (the period runs start-of-day to end-of-day, so `elapsed × N = (N − daysLeft) + today's
 * fraction`), and drawn only when that resolves to a whole, in-range day. Decorative: the hero
 * already says how many days are left, so it is `aria-hidden`.
 */
export function KhelHud({
  elapsed,
  daysLeft,
}: {
  elapsed: number;
  daysLeft: number;
  spent: number;
}) {
  const [fraction] = useState(todayFraction);
  if (!(elapsed > 0 && elapsed < 1) || daysLeft < 1) return null;
  const total = Math.round((daysLeft - fraction) / (1 - elapsed));
  const day = total - daysLeft + 1;
  if (!Number.isFinite(total) || total < 1 || total > 366 || day < 1 || day > total) return null;
  return (
    <span className="only-khel khel-hud" aria-hidden>
      Day {day}/{total}
    </span>
  );
}
