import type { HeroExtraProps } from './hero-info';

/** Beyond this many days a fin per day is too fine to read, so the month becomes a plain bar. */
const MAX_FINS = 62;

/**
 * Sector: the month as a row of enamel fins under the hero figure — one per day, the days gone
 * solid, today standing tall, the days ahead open. The louvres open once, staggered. Longer
 * periods (a yearly budget) fall back to a proportional bar. Footer slot only; it restates the
 * budget line above it, so it is decorative to assistive tech.
 */
export function SectorExtra({ slot, info }: HeroExtraProps) {
  if (slot !== 'footer') return null;
  const total = Math.max(1, Math.round(info.totalDays));
  const day = Math.min(Math.max(Math.round(info.dayOfPeriod), 1), total);
  const caption = `Day ${day} of ${total}`;

  if (total > MAX_FINS) {
    return (
      <div className="only-sector sct-month" aria-hidden>
        <div className="sct-run">
          <i style={{ width: `${(day / total) * 100}%` }} />
        </div>
        <p className="sct-cap">{caption}</p>
      </div>
    );
  }

  return (
    <div className="only-sector sct-month" aria-hidden>
      <div className="sct-fins" style={{ ['--n' as string]: total }}>
        {Array.from({ length: total }, (_, k) => (
          <i
            key={k}
            className={k + 1 < day ? 'g' : k + 1 === day ? 't' : 'o'}
            style={{ ['--i' as string]: k }}
          />
        ))}
      </div>
      <p className="sct-cap" style={{ paddingLeft: `${((day - 1) / total) * 100}%` }}>
        {caption}
      </p>
    </div>
  );
}
