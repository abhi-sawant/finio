import type { CSSProperties } from 'react';
import type { HeroExtraProps } from './hero-info';

const BLOCKS = 20;

/**
 * Ajrakh: the stamped row, standing in for the hero's budget bar (footer slot). Twenty eight-point
 * block motifs, each 5% of the budget: inked = spent, resist outline = left, a turmeric marker at
 * today, the last blocks vermilion when the budget is near or over its limit. The blocks press
 * in once, staggered, on mount (CSS). Restates the budget line above it, so it is decorative.
 */
export function AjrakhExtra({ slot, info }: HeroExtraProps) {
  if (slot !== 'footer') return null;
  const spent = Math.max(0, info.spentPercent);
  const hot = info.isOver || spent >= 85;
  const today = Math.min(Math.max(info.elapsed, 0), 1) * 100;
  return (
    <div className="only-ajrakh ajr-stamps" aria-hidden>
      <div className="ajr-row">
        {Array.from({ length: BLOCKS }, (_, i) => {
          const f = Math.max(0, Math.min(1, (spent - i * 5) / 5));
          const cls =
            'ajr-s' + (f >= 1 ? ' on' : f > 0 ? ' pt' : '') + (hot && i >= 16 && f > 0 ? ' hot' : '');
          const style = { '--i': i, '--f': `${Math.round(f * 100)}%` } as CSSProperties;
          return <i key={i} className={cls} style={style} />;
        })}
        <u className="ajr-today" style={{ left: `${today}%` }} />
      </div>
      <p className="ajr-cap">
        <span>Each block is 5% of the limit</span>
        <span>
          Day {info.dayOfPeriod} of {info.totalDays}
        </span>
      </p>
    </div>
  );
}
