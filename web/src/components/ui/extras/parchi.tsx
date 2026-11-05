import { useEffect, useState } from 'react';

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const DAY_MS = 86_400_000;

// The hero prints out of its slot on the first Dashboard visit of a session, not on every visit.
let printedThisSession = false;

/** Bar/space widths derived from a fixed string, with guard bars at both ends — never random. */
function barcode(seed: string) {
  const seq = [1, 1, 1, 1];
  for (const ch of seed) {
    const c = ch.charCodeAt(0);
    seq.push((c % 3) + 1, ((c >> 2) % 2) + 1, ((c >> 3) % 3) + 1, ((c >> 1) % 2) + 1);
  }
  seq.push(1, 1, 1);
  let x = 0;
  const bars: { x: number; w: number }[] = [];
  seq.forEach((w, i) => {
    if (i % 2 === 0) bars.push({ x, w });
    x += w;
  });
  return { bars, width: x };
}

/**
 * Which day of the budget period today is, and how long the period runs. Dashboard hands over
 * the elapsed fraction (by the clock) and the calendar days left including today, so
 * `elapsed · total = (day − 1) + fractionOfToday` and `day = total − daysLeft + 1`.
 */
function periodDay(elapsed: number, daysLeft: number, now: Date) {
  const midnight = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const f = (now.getTime() - midnight) / DAY_MS;
  const rest = 1 - elapsed;
  const total = rest > 1e-6 ? Math.round((daysLeft - f) / rest) : NaN;
  if (!Number.isFinite(total) || total < daysLeft) return null;
  return { day: total - daysLeft + 1, total };
}

/** Parchi: the receipt's foot under the hero — barcode and a printed stamp line. */
export function ParchiSlip({ elapsed, daysLeft }: { elapsed: number; daysLeft: number }) {
  const [fresh] = useState(() => !printedThisSession);
  useEffect(() => {
    printedThisSession = true;
  }, []);

  const now = new Date();
  const date = `${now.getDate()} ${MONTHS[now.getMonth()]} ${now.getFullYear()}`;
  const pd = periodDay(elapsed, daysLeft, now);
  const line = pd
    ? `Day ${pd.day} of ${pd.total}`
    : `${daysLeft} day${daysLeft === 1 ? '' : 's'} to go`;
  const seed = `FINIO*${String(now.getDate()).padStart(2, '0')}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getFullYear()).slice(2)}*${pd?.day ?? daysLeft}`;
  const { bars, width } = barcode(seed);

  return (
    <div className="only-parchi parchi-slip" data-feed={fresh ? '' : undefined} aria-hidden>
      <svg
        className="parchi-barcode"
        viewBox={`0 0 ${width} 30`}
        preserveAspectRatio="none"
        shapeRendering="crispEdges"
        fill="currentColor"
        focusable="false"
      >
        {bars.map((b) => (
          <rect key={b.x} x={b.x} width={b.w} height={30} />
        ))}
      </svg>
      <p className="parchi-stamp">
        <b>{date}</b>
        <span>{line}</span>
      </p>
    </div>
  );
}
