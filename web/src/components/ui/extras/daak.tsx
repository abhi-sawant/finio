import { useId } from 'react';
import type { HeroExtraProps } from './hero-info';

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const DAYS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

/**
 * Daak: the double-ring date postmark in the hero's corner, with wavy cancellation lines beside it
 * on wide screens. It is floated (see themes/daak.css), so text wraps around it and the figure
 * drops below it rather than ever sitting under it. Struck once on load by CSS.
 */
export function DaakExtra({ slot, info }: HeroExtraProps) {
  const pathId = useId();
  if (slot !== 'scene') return null;

  const today = new Date(info.periodStart);
  today.setDate(today.getDate() + info.dayOfPeriod - 1);
  const dateText = `${today.getDate()} ${MONTHS[today.getMonth()]} ${today.getFullYear()}`;
  const dayText = DAYS[today.getDay()];
  const progress = `DAY ${info.dayOfPeriod} OF ${info.totalDays}`;

  return (
    <div className="only-daak" aria-hidden>
      <div className="dk-pm">
        <svg className="dk-pm-waves" viewBox="0 0 60 80" fill="none" stroke="currentColor">
          <g strokeWidth="1.7" strokeLinecap="round">
            {[12, 26, 40, 54, 68].map((y) => (
              <path key={y} d={`M2 ${y}q7-6 14 0t14 0 14 0 14 0`} />
            ))}
          </g>
        </svg>
        <svg className="dk-pm-ring" viewBox="0 0 140 140" fill="none" stroke="currentColor">
          <defs>
            <path id={pathId} d="M70 116a46 46 0 1 1 .01 0" />
          </defs>
          <circle cx="70" cy="70" r="66" strokeWidth="2.2" />
          <circle cx="70" cy="70" r="61.5" strokeWidth="1" />
          <circle cx="70" cy="70" r="37" strokeWidth="1.4" />
          <text stroke="none" fill="currentColor" fontSize="9.6">
            <textPath href={`#${pathId}`} startOffset="9" textLength="262" lengthAdjust="spacing">
              FINIO · KEPT ONLY ON THIS DEVICE
            </textPath>
          </text>
          <circle cx="70" cy="114.5" r="2" fill="currentColor" stroke="none" />
          <path d="M35 59h70M35 81h70" strokeWidth="1.5" />
          <text x="70" y="74.2" textAnchor="middle" stroke="none" fill="currentColor" fontSize="10.6">
            {dateText}
          </text>
          <text x="70" y="52.5" textAnchor="middle" stroke="none" fill="currentColor" fontSize="7.6">
            {dayText}
          </text>
          <text x="70" y="93.5" textAnchor="middle" stroke="none" fill="currentColor" fontSize="7.6">
            {progress}
          </text>
        </svg>
      </div>
    </div>
  );
}
