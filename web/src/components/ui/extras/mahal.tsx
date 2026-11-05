import { useId } from 'react';
import { format } from 'date-fns';

// The proscenium, in a 350 × 202 box: an arch on stepped ziggurat shoulders. `OUT` is the gold
// outline, `INN` the keyline six units inside it; both stop at the shoulders' foot (y 196–202),
// where the card's own frame takes over on a phone (themes/mahal.css).
const OUT_FILL =
  'M0.6 202V196H12V180H26V164H47V140A128 128 0 0 1 303 140V164H324V180H338V196H349.4V202Z';
const OUT = 'M0.6 202V196H12V180H26V164H47V140A128 128 0 0 1 303 140V164H324V180H338V196H349.4V202';
const INN = 'M6 203V202H18V186H32V170H53V140A122 122 0 0 1 297 140V170H318V186H332V202H344V203';

const CX = 175;
const CY = 140;
const R0 = 54; // rays start here
const R1 = 92; // long rays end here
const R2 = 84; // short rays end here
const RAYS = 25;

const clamp = (v: number, lo: number, hi: number) => Math.min(Math.max(v, lo), hi);

/**
 * Mahal: the sunburst gauge in the hero's arch — a half-fan of gilt rays, lit left to right for the
 * share of the budget spent, with a needle at today's place in the period. It burns vermilion once
 * the budget is over or 90% gone. Decorative: the hero's text and progress bar say the same.
 */
export function MahalSunburst({
  elapsed,
  spent,
  isOver,
}: {
  elapsed: number;
  spent: number;
  isOver: boolean;
}) {
  const arcId = useId();
  const pct = clamp(Number.isFinite(spent) ? spent : 0, 0, 100);
  const pace = clamp(Number.isFinite(elapsed) ? elapsed : 0, 0, 1);
  const hot = isOver || spent >= 90;
  const needle = (pace * 180).toFixed(2);
  const origin = `${CX}px ${CY}px`;

  return (
    <div className="only-mahal mahal-sunburst" data-hot={hot || undefined} aria-hidden>
      <svg viewBox="0 0 350 202" focusable="false">
        <path className="mahal-pf" d={OUT_FILL} />
        <path className="mahal-pk" d={OUT} vectorEffect="non-scaling-stroke" />
        <path className="mahal-pk2" d={INN} vectorEffect="non-scaling-stroke" />
        <path id={arcId} d="M65 140A110 110 0 0 1 285 140" fill="none" />
        <text className="mahal-marq">
          <textPath href={`#${arcId}`} startOffset="50%" textAnchor="middle">
            Now showing · {format(new Date(), 'MMMM yyyy')}
          </textPath>
        </text>
        <path className="mahal-hl" d="M77 140A98 98 0 0 1 273 140" />
        {Array.from({ length: RAYS }, (_, i) => {
          const a = (i * 180) / (RAYS - 1);
          const lit = pct > 0 && a <= pct * 1.8 + 0.01;
          const r = i % 2 ? R2 : R1;
          return (
            <line
              key={i}
              className="mahal-ray"
              data-lit={lit || undefined}
              x1={CX - R0}
              y1={CY}
              x2={CX - r}
              y2={CY}
              strokeWidth={1.6}
              style={
                {
                  '--a': `${a.toFixed(2)}deg`,
                  '--i': i,
                  transformOrigin: origin,
                } as React.CSSProperties
              }
            />
          );
        })}
        <g
          className="mahal-ndl"
          style={{ '--a': `${needle}deg`, transformOrigin: origin } as React.CSSProperties}
        >
          <line x1={CX - R0 + 4} y1={CY} x2={CX - R1 - 7} y2={CY} />
          <path d={`M${CX - R1 - 7} ${CY}l3.5 -2.6 3.5 2.6 -3.5 2.6z`} />
        </g>
        <path className="mahal-base" d="M53 140.5H297" />
        <text className="mahal-hub" x={CX} y={131} textAnchor="middle">
          {Math.round(clamp(spent, 0, 999))}%
        </text>
      </svg>
    </div>
  );
}
