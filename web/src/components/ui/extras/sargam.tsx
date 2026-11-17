import type { HeroExtraProps } from './hero-info';

// Tape window geometry (SVG units). A full reel is `RMAX`; a pack's area is proportional to the
// days it stands for, so its radius is sqrt(hub² + f·(RMAX² − hub²)).
const CY = 44;
const HUB = 13;
const RMAX = 37;
const LX = 76;
const RX = 224;

const packRadius = (f: number) =>
  Math.sqrt(HUB * HUB + Math.min(Math.max(f, 0), 1) * (RMAX * RMAX - HUB * HUB));

/** Tangent point from an external point to a reel pack, on the left or right side of the circle. */
function tangent(px: number, py: number, cx: number, r: number, first: boolean) {
  const d = Math.hypot(px - cx, py - CY);
  const a = Math.atan2(py - CY, px - cx);
  const b = Math.acos(Math.min(r / d, 1));
  const pts = [a + b, a - b]
    .map((t) => [cx + r * Math.cos(t), CY + r * Math.sin(t)])
    .sort((m, q) => m[0] - q[0]);
  return pts[first ? 0 : 1].map((v) => v.toFixed(1)).join(' ');
}

const TEETH = Array.from({ length: 6 }, (_, k) => k * 60);

function Reel({ cx, r }: { cx: number; r: number }) {
  return (
    <g transform={`translate(${cx} ${CY})`}>
      <circle r={RMAX} className="sg-full" />
      <circle r={r.toFixed(1)} className="sg-pack" />
      <circle r={((r + HUB) / 2).toFixed(1)} className="sg-groove" />
      <circle r={(r - 2.5).toFixed(1)} className="sg-groove" />
      <g className="sg-hub">
        <circle r={HUB} className="sg-hb" />
        <circle r="9.2" className="sg-hh" />
        <g className="sg-hb">
          {TEETH.map((deg) => (
            <rect
              key={deg}
              x="-1.5"
              y="-9.6"
              width="3"
              height="4.4"
              rx=".6"
              transform={`rotate(${deg})`}
            />
          ))}
        </g>
      </g>
    </g>
  );
}

/**
 * Sargam: the hero's footer is a cassette deck readout. A 12-bar level meter for the budget used
 * (teal to 70%, mustard to 90%, red beyond), then the tape window: the supply reel's pack is the
 * days left, the take-up reel's the days gone, drawn to scale by area inside a ring that marks a
 * full period. Restates the hero's text, so it is decorative to assistive tech.
 */
export function SargamExtra({ slot, info }: HeroExtraProps) {
  if (slot !== 'footer') return null;

  const total = Math.max(info.totalDays, 1);
  const left = Math.min(Math.max(info.daysLeft, 0), total);
  const gone = total - left;
  const rl = packRadius(left / total);
  const rr = packRadius(gone / total);
  const pct = Math.round(Math.max(info.spentPercent, 0));
  const lit = Math.min(12, Math.round((Math.min(pct, 100) * 12) / 100));
  const pathD = `M${tangent(13, 80, LX, rl, true)} L13 80 Q13 89 22 89 L278 89 Q287 89 287 80 L${tangent(287, 80, RX, rr, false)}`;

  return (
    <div className="only-sargam sg-deck" aria-hidden>
      <div className="sg-mrow">
        <div className="sg-meter">
          {Array.from({ length: 12 }, (_, k) => (
            <i key={k} className={`${k < 8 ? 't' : k < 10 ? 'm' : 'r'}${k < lit ? ' on' : ''}`} />
          ))}
        </div>
        <b>{pct}% used</b>
      </div>
      <svg className="sg-tape" viewBox="0 0 300 96">
        <rect x=".75" y=".75" width="298.5" height="94.5" rx="12" className="sg-win" />
        <path className="sg-path" d={pathD} />
        <circle cx="13" cy="82" r="3" className="sg-roll" />
        <circle cx="287" cy="82" r="3" className="sg-roll" />
        <Reel cx={LX} r={rl} />
        <Reel cx={RX} r={rr} />
      </svg>
      <p className="sg-cap">
        <span>
          {left} day{left === 1 ? '' : 's'} left
        </span>
        <b>
          Day {info.dayOfPeriod} of {total}
        </b>
        <span>
          {gone} day{gone === 1 ? '' : 's'} gone
        </span>
      </p>
    </div>
  );
}
