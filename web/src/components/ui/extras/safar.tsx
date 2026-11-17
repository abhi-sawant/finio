import type { HeroExtraProps } from './hero-info';

/*
 * Safar's departures board. The hero figure is set as split-flap tiles (one per character, a
 * hinge hairline across each, flipping in once left to right), a "Day n of N" platform clock sits
 * beside it, and the budget period is a 20-segment strip with a tick for today. It restates what
 * the hero says in text, so it is aria-hidden; `.hero-fig` stays for screen readers.
 * Tile size comes from the container width and the character count (see safar.css), so a long or
 * masked figure shrinks instead of overflowing.
 */
const SEGMENTS = 20;

export function SafarExtra({ slot, info }: HeroExtraProps) {
  if (slot !== 'figure') return null;

  const chars = [...info.figure];
  const spent = Math.max(0, Math.min(100, info.spentPercent));
  const lit = Math.round((spent / 100) * SEGMENTS);
  const today = Math.max(0, Math.min(1, info.elapsed)) * 100;
  const state = info.isOver ? 'over' : spent >= 85 ? 'near' : 'ok';

  return (
    <div className="only-safar" aria-hidden>
      <div className="sf-board">
        <div className="sf-clock">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2.2">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" strokeLinecap="round" />
          </svg>
          Day {info.dayOfPeriod} of {info.totalDays}
        </div>
        <div className="sf-wrap" style={{ ['--n' as string]: chars.length }}>
          <div className="sf-flap">
            {chars.map((c, i) => (
              <span
                key={i}
                className={`sf-tile${c === ',' || c === '.' ? ' sf-p' : ''}${c === '₹' ? ' sf-r' : ''}`}
                style={{ ['--i' as string]: i }}
              >
                <b>{c}</b>
                <i />
              </span>
            ))}
          </div>
        </div>
        <div className={`sf-strip sf-${state}`} style={{ ['--today' as string]: `${today}%` }}>
          {Array.from({ length: SEGMENTS }, (_, i) => (
            <span key={i} className={i < lit ? 'sf-on' : undefined} />
          ))}
          <u />
        </div>
      </div>
    </div>
  );
}
