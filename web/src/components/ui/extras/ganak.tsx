import { useEffect, useState } from 'react';

/*
 * Ganak's LCD window. The figure is drawn as authored seven-segment digits (slanted, unlit
 * segments kept as faint ghosts — the 8888 behind every calculator figure), with status
 * annunciators along the top and the budget as a bar graph of discrete segments underneath.
 * Ported from design-prototypes/themes/src/ganak.mjs (segDigits / bars).
 */

// Segment geometry, in viewBox units: a 48×88 digit, 10-unit strokes, 64-unit advance.
const W = 48;
const H = 88;
const T = 10;
const G = 1.6;
const ADV = 64;
const SL = 12;
const hz = (x0: number, x1: number, y: number) =>
  `${x0},${y} ${x0 + T / 2},${y - T / 2} ${x1 - T / 2},${y - T / 2} ${x1},${y} ${x1 - T / 2},${y + T / 2} ${x0 + T / 2},${y + T / 2}`;
const vt = (x: number, y0: number, y1: number) =>
  `${x},${y0} ${x + T / 2},${y0 + T / 2} ${x + T / 2},${y1 - T / 2} ${x},${y1} ${x - T / 2},${y1 - T / 2} ${x - T / 2},${y0 + T / 2}`;
const LX = T / 2;
const RX = W - T / 2;
const MY = H / 2;
const SEGMENTS: [string, string][] = [
  ['a', hz(LX + G, RX - G, T / 2)],
  ['b', vt(RX, T / 2 + G, MY - G)],
  ['c', vt(RX, MY + G, H - T / 2 - G)],
  ['d', hz(LX + G, RX - G, H - T / 2)],
  ['e', vt(LX, MY + G, H - T / 2 - G)],
  ['f', vt(LX, T / 2 + G, MY - G)],
  ['g', hz(LX + G, RX - G, MY)],
];
const DIGIT_SEGMENTS = [
  'abcdef',
  'bc',
  'abdeg',
  'abcdg',
  'bcfg',
  'acdfg',
  'acdefg',
  'abc',
  'abcdefg',
  'abcdfg',
];
const DOT_X = W + 6;

interface Cell {
  /** Lit segments ('' = blank, all ghost). */
  on: string;
  /** The separator lit in this cell's point slot: '.', ',' or ''. */
  sep: string;
}

/**
 * Turns formatCurrency output into display cells. Digits light their segments; a hidden-amount
 * mask (•) or a minus sign is a dash (segment g); ',' and '.' light the previous cell's point.
 * The ₹ is set in type beside the window, so it is skipped here.
 */
function toCells(text: string, minCells: number): Cell[] {
  const cells: Cell[] = [];
  for (const ch of text) {
    if (ch >= '0' && ch <= '9') cells.push({ on: DIGIT_SEGMENTS[+ch], sep: '' });
    else if (ch === '•' || ch === '-' || ch === '−') cells.push({ on: 'g', sep: '' });
    else if ((ch === ',' || ch === '.') && cells.length) cells[cells.length - 1].sep = ch;
  }
  while (cells.length < minCells) cells.unshift({ on: '', sep: '' });
  return cells;
}

function SegmentDigits({ text }: { text: string }) {
  const cells = toCells(text, 6);
  const vbW = SL + cells.length * ADV;
  const vbH = H + 10;
  return (
    <svg
      className="ganak-seg"
      viewBox={`0 0 ${vbW} ${vbH}`}
      width={Math.round((vbW * 58) / H)}
      height={Math.round((vbH * 58) / H)}
      focusable="false"
    >
      {cells.map((c, i) => (
        <g key={i} transform={`translate(${SL + i * ADV} 0) skewX(-7)`}>
          {SEGMENTS.map(([k, points]) => (
            <polygon key={k} className={c.on.includes(k) ? 's1' : 's0'} points={points} />
          ))}
          <polygon
            className={c.sep ? 's1' : 's0'}
            points={`${DOT_X},${H - 8} ${DOT_X + 8},${H - 8} ${DOT_X + 8},${H} ${DOT_X},${H}`}
          />
          {c.sep === ',' && (
            <polygon
              className="s1"
              points={`${DOT_X + 5},${H} ${DOT_X + 8},${H} ${DOT_X + 3},${H + 9} ${DOT_X},${H + 9}`}
            />
          )}
        </g>
      ))}
    </svg>
  );
}

const BAR_CELLS = 20;

/** The power-on test (88888 for a beat) runs once per app session, not on every visit to Home. */
let poweredOn = false;

/** Ganak: the hero figure in an LCD window, drawn as seven-segment digits. */
export function GanakDisplay({
  text,
  elapsed,
  spent,
  daysLeft,
}: {
  text: string;
  /** 0–1, how far through the budget period today is. */
  elapsed: number;
  /** 0–100+, how much of the budget is spent. */
  spent: number;
  daysLeft: number;
}) {
  const [boot] = useState(() => !poweredOn);
  useEffect(() => {
    poweredOn = true;
  }, []);

  const e = Math.min(Math.max(elapsed, 0), 1);
  // Period length from the elapsed share and the days left (today counts as a day left).
  const total = e < 1 ? Math.max(daysLeft, Math.round((daysLeft - 0.5) / (1 - e))) : daysLeft;
  const day = Math.min(Math.max(total - daysLeft + 1, 1), total);
  const periodWord = total <= 7 ? 'WEEK' : total >= 300 ? 'YEAR' : 'MONTH';
  const pct = Math.max(Math.round(spent), 0);
  const over = spent >= 100;
  const lit = Math.round((Math.min(Math.max(spent, 0), 100) / 100) * BAR_CELLS);

  return (
    <div className="only-ganak ganak-display" aria-hidden data-boot={boot ? '' : undefined}>
      <div className="ganak-ann">
        <b>M</b>
        <b>{periodWord}</b>
        <b>
          DAY {day}/{total}
        </b>
        <b className={spent > 0 ? undefined : 'off'}>{pct}%</b>
        <b className={over ? 'err' : 'off'}>E</b>
      </div>
      <div className="ganak-fig">
        <span className="ganak-rs">₹</span>
        <SegmentDigits text={text} />
      </div>
      <div className="ganak-lb">
        <u style={{ left: `calc(${(e * 100).toFixed(2)}% - 4px)` }} />
        {Array.from({ length: BAR_CELLS }, (_, i) => (
          <i key={i} className={i < lit ? (over && i === lit - 1 ? 'on bk' : 'on') : undefined} />
        ))}
      </div>
    </div>
  );
}
