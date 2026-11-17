import { useId } from 'react';
import type { HeroExtraProps, HeroInfo } from './hero-info';

const W = 360;
const X0 = 1;
const X1 = W - 1;
const CHAR = 7.5; // average advance of the 13px labels, used only to keep them from colliding
const BAR_TOP = 52;
const BAR_H = 18;
const DIM_Y = 40;

const r1 = (v: number) => Math.round(v * 10) / 10;
const clampPct = (n: number) => Math.min(Math.max(n, 0), 100);

interface Label {
  text: string;
  /** Preferred centre (or the edge, for start/end alignment). */
  at: number;
  align: 'start' | 'middle' | 'end';
  cls?: string;
}
interface Placed extends Label {
  left: number;
  width: number;
  row: number;
}

/** Drop each label on the first row where it clears the ones already placed. */
function place(labels: Label[], rows: number): Placed[] {
  const placed: Placed[] = [];
  for (const l of labels) {
    const width = l.text.length * CHAR;
    const raw = l.align === 'start' ? l.at : l.align === 'end' ? l.at - width : l.at - width / 2;
    const left = Math.min(Math.max(raw, 0), Math.max(W - width, 0));
    let row = 0;
    while (
      row < rows - 1 &&
      placed.some((p) => p.row === row && left < p.left + p.width + 8 && p.left < left + width + 8)
    ) {
      row++;
    }
    placed.push({ ...l, left, width, row });
  }
  return placed;
}

const arrowL = (x: number, y: number) => `M${r1(x)} ${y}l7 -2.6v5.2z`;
const arrowR = (x: number, y: number) => `M${r1(x)} ${y}l-7 -2.6v5.2z`;

/** One arrowed span with its extension lines. */
function Dim({ a, b, y, d, red }: { a: number; b: number; y: number; d: number; red?: boolean }) {
  if (b - a < 16) return null;
  return (
    <g className={red ? 'nk-dm nk-red' : 'nk-dm'} style={{ ['--d' as string]: `${d}s` }}>
      <path className="nk-dl" pathLength={1} d={`M${r1(a)} ${y}H${r1(b)}`} />
      <path className="nk-ah" d={arrowL(a, y) + arrowR(b, y)} />
    </g>
  );
}

function draw(info: HeroInfo, hatchId: string) {
  const sp = X1 - X0;
  const pct = Math.max(info.spentPercent, 0);
  const over = info.isOver;
  // Over budget the bar is drawn to the spend and the limit becomes a mark inside it.
  const scale = over ? Math.max(pct, 100.1) : 100;
  const xLimit = over ? X0 + (sp * 100) / scale : X1;
  const xSpent = over ? X1 : X0 + (sp * clampPct(pct)) / 100;
  const xDatum = X0 + (xLimit - X0) * Math.min(Math.max(info.elapsed, 0), 1);
  const dayText = `Day ${info.dayOfPeriod} of ${info.totalDays}`;

  const top: Label[] = over
    ? [
        {
          text: `${info.limitText} limit`,
          at: (X0 + xLimit) / 2,
          align: 'middle',
        },
        {
          text: `+${info.remainingText} over`,
          at: (xLimit + X1) / 2,
          align: 'middle',
          cls: 'nk-red',
        },
      ]
    : [
        { text: `${info.spentText} spent`, at: (X0 + xSpent) / 2, align: 'middle' },
        { text: `${info.remainingText} left`, at: (xSpent + X1) / 2, align: 'middle' },
      ];
  // A span too short for its label hands the label to the nearest end instead of squeezing it.
  if (!over) {
    const [spent, left] = top;
    if (xSpent - X0 < spent.text.length * CHAR + 14) {
      spent.align = 'start';
      spent.at = X0;
    }
    if (X1 - xSpent < left.text.length * CHAR + 14) {
      left.align = 'end';
      left.at = X1;
    }
  }
  const bottom: Label[] = [
    { text: '0', at: X0, align: 'start', cls: 'nk-sc' },
    {
      text: over ? `${info.spentText} spent` : info.limitText,
      at: over ? X1 : X1,
      align: 'end',
      cls: 'nk-sc',
    },
    { text: dayText, at: xDatum, align: 'middle', cls: 'nk-sc' },
  ];
  const topPlaced = place(top, 2);
  const botPlaced = place(bottom, 2);
  const topRows = [DIM_Y - 7, DIM_Y - 23];
  const botRows = [BAR_TOP + BAR_H + 26, BAR_TOP + BAR_H + 42];
  const topUsed = Math.max(...topPlaced.map((p) => p.row));
  const botUsed = Math.max(...botPlaced.map((p) => p.row));
  const yOff = topUsed > 0 ? 16 : 0;
  const H = yOff + BAR_TOP + BAR_H + 34 + botUsed * 16;
  return {
    sp,
    over,
    xLimit,
    xSpent,
    xDatum,
    topPlaced,
    botPlaced,
    topRows,
    botRows,
    yOff,
    H,
    hatchId,
  };
}

/**
 * Naksha: the budget as a bar drawn to scale on the drawing sheet. Arrowed dimension lines span
 * what is spent and what is left, a dash-dot datum stands at today, and the ends are lettered 0
 * and the limit. Over budget, the bar runs past the limit mark and the overrun is dimensioned in
 * red pencil. Restates the budget line above it, so it is decorative to assistive tech.
 */
export function NakshaExtra({ slot, info }: HeroExtraProps) {
  const raw = useId();
  if (slot !== 'footer') return null;
  const id = `nk${raw.replace(/[^a-zA-Z0-9]/g, '')}`;
  const g = draw(info, id);
  const { xLimit, xSpent, xDatum, yOff, H, over } = g;
  const bt = BAR_TOP;
  const bb = BAR_TOP + BAR_H;
  return (
    <div className="only-naksha nk-draft" aria-hidden>
      <svg
        className={over ? 'nk-svg nk-over' : 'nk-svg'}
        viewBox={`0 0 ${W} ${H}`}
        width="100%"
        style={{ maxWidth: W + 40, aspectRatio: `${W} / ${H}` }}
      >
        <defs>
          <pattern
            id={id}
            width="5"
            height="5"
            patternUnits="userSpaceOnUse"
            patternTransform="rotate(45)"
          >
            <path d="M.5 0V5" className={over ? 'nk-hp nk-hp-red' : 'nk-hp'} />
          </pattern>
          <pattern
            id={`${id}r`}
            width="5"
            height="5"
            patternUnits="userSpaceOnUse"
            patternTransform="rotate(45)"
          >
            <path d="M.5 0V5" className="nk-hp nk-hp-red" />
          </pattern>
        </defs>
        <g transform={`translate(0 ${yOff})`}>
          {/* extension lines */}
          <path
            className="nk-ex"
            d={`M${X0} ${DIM_Y - 8}V${bt}M${r1(over ? xLimit : xSpent)} ${DIM_Y - 8}V${bt}M${X1} ${DIM_Y - 8}V${bt}`}
          />
          <Dim a={X0} b={over ? xLimit : xSpent} y={DIM_Y} d={0.05} />
          <Dim a={over ? xLimit : xSpent} b={X1} y={DIM_Y} d={0.2} red={over} />
          {g.topPlaced.map((p) => (
            <text
              key={p.text}
              className={p.cls ? `nk-t ${p.cls}` : 'nk-t'}
              x={r1(p.left)}
              y={g.topRows[p.row]}
            >
              {p.text}
            </text>
          ))}
          {/* the bar, hatched where it is spent */}
          <rect
            className="nk-fill"
            x={X0}
            y={bt}
            width={r1(Math.max(xSpent - X0, 0))}
            height={BAR_H}
            fill={`url(#${id})`}
          />
          {over && (
            <rect
              className="nk-fill"
              x={r1(xLimit)}
              y={bt}
              width={r1(X1 - xLimit)}
              height={BAR_H}
              fill={`url(#${id}r)`}
            />
          )}
          <rect className="nk-bar" x={X0} y={bt} width={X1 - X0} height={BAR_H} />
          <path className={over ? 'nk-bar nk-red' : 'nk-bar'} d={`M${r1(over ? xLimit : xSpent)} ${bt}V${bb}`} />
          <path
            className="nk-tk"
            d={[0.25, 0.5, 0.75].map((f) => `M${r1(X0 + (X1 - X0) * f)} ${bb}v4`).join('')}
          />
          {/* datum: today */}
          <path className="nk-cl" d={`M${r1(xDatum)} ${DIM_Y + 6}V${bb + 9}`} />
          <path className="nk-dt" d={`M${r1(xDatum - 4)} ${bb + 13}h8l-4 -6z`} />
          {g.botPlaced.map((p) => (
            <text
              key={p.text}
              className={p.cls ? `nk-t ${p.cls}` : 'nk-t'}
              x={r1(p.left)}
              y={g.botRows[p.row]}
            >
              {p.text}
            </text>
          ))}
        </g>
      </svg>
    </div>
  );
}
