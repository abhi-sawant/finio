/* Kolam — The threshold drawing. Swept stone and red oxide, one monoline weight, dot-row dividers, and the month drawn as a kolam whose line loops every day gone. Prototype: design-prototypes/themes/src/kolam.mjs */
import { useMemo } from 'react';
import type { HeroExtraProps } from './hero-info';

type Pt = [number, number];

const f = (v: number) => (Math.round(v * 100) / 100).toString();

/**
 * One closed line woven around a route of dots: it passes alternate sides of each dot going out,
 * the opposite sides coming back, and crosses itself midway between neighbours. It starts and
 * ends beside dot `from` when given, else beside the last dot. Right-angle turns become round arcs.
 */
function weave(pts: Pt[], a: number, from = -1): string {
  const n = pts.length;
  if (!n) return '';
  const T: Pt[] = pts.map((_, i) => {
    const p = pts[Math.max(i - 1, 0)];
    const q = pts[Math.min(i + 1, n - 1)];
    const dx = q[0] - p[0];
    const dy = q[1] - p[1];
    const l = Math.hypot(dx, dy);
    return l ? [dx / l, dy / l] : [1, 0];
  });
  const dir = (i: number, j: number): Pt => {
    const l = Math.hypot(pts[j][0] - pts[i][0], pts[j][1] - pts[i][1]) || 1;
    return [(pts[j][0] - pts[i][0]) / l, (pts[j][1] - pts[i][1]) / l];
  };
  const off = (i: number, sg: number): Pt[] => {
    const N: Pt = [-T[i][1] * sg, T[i][0] * sg];
    const c: Pt = [pts[i][0] + N[0] * a, pts[i][1] + N[1] * a];
    if (i === 0 || i === n - 1) return [c];
    const d1 = dir(i - 1, i);
    const d2 = dir(i, i + 1);
    if (
      Math.abs(d1[0] * d2[0] + d1[1] * d2[1]) > 0.5 ||
      N[0] * (d2[0] - d1[0]) + N[1] * (d2[1] - d1[1]) > 0
    )
      return [c];
    return [
      [pts[i][0] - d2[0] * a, pts[i][1] - d2[1] * a],
      c,
      [pts[i][0] + d1[0] * a, pts[i][1] + d1[1] * a],
    ];
  };
  const mid = (i: number): Pt => [
    (pts[i][0] + pts[i + 1][0]) / 2,
    (pts[i][1] + pts[i + 1][1]) / 2,
  ];
  const P: Pt[] = [[pts[n - 1][0] + T[n - 1][0] * a, pts[n - 1][1] + T[n - 1][1] * a]];
  for (let i = n - 1; i >= 0; i--) {
    P.push(...off(i, i % 2 ? 1 : -1).reverse());
    if (i > 0) P.push(mid(i - 1));
  }
  P.push([pts[0][0] - T[0][0] * a, pts[0][1] - T[0][1] * a]);
  for (let i = 0; i < n; i++) {
    P.push(...off(i, i % 2 ? -1 : 1));
    if (i < n - 1) P.push(mid(i));
  }
  if (from >= 0 && from < n) {
    const j = P.findIndex((q) => Math.hypot(q[0] - pts[from][0], q[1] - pts[from][1]) < a * 1.01);
    if (j > 0) P.push(...P.splice(0, j));
  }
  const m = P.length;
  const g = (i: number) => P[((i % m) + m) % m];
  let d = `M${f(P[0][0])} ${f(P[0][1])}`;
  for (let i = 0; i < m; i++) {
    const p0 = g(i - 1);
    const p1 = g(i);
    const p2 = g(i + 1);
    const p3 = g(i + 2);
    d += `C${f(p1[0] + (p2[0] - p0[0]) / 6)} ${f(p1[1] + (p2[1] - p0[1]) / 6)} ${f(p2[0] - (p3[0] - p1[0]) / 6)} ${f(p2[1] - (p3[1] - p1[1]) / 6)} ${f(p2[0])} ${f(p2[1])}`;
  }
  return `${d}Z`;
}

const COLS = 7;
const S = 30; // dot spacing in the viewBox
const MAX_GRID_DAYS = 42;

interface Drawing {
  mode: 'grid' | 'row';
  w: number;
  h: number;
  dots: Pt[];
  path: string;
  gone: number;
  today: Pt;
  ringR: number;
  dotR: number;
  stroke: number;
}

/** Pure function of the day counts. Rows of up to 7 dots. */
function draw(total: number, day: number): Drawing {
  const n = Math.max(1, Math.round(total));
  const gone = Math.min(n, Math.max(0, Math.round(day)));

  if (n > MAX_GRID_DAYS) {
    const N = 20;
    const p = 16;
    const k = Math.round((gone / n) * N);
    const dots: Pt[] = Array.from({ length: N }, (_, i) => [p / 2 + i * p, 10]);
    return {
      mode: 'row',
      w: N * p,
      h: 20,
      dots,
      path: k ? weave(dots.slice(0, k), 5.6) : '',
      gone: k,
      today: dots[Math.min(Math.max(k - 1, 0), N - 1)],
      ringR: 6,
      dotR: 1.8,
      stroke: 1.6,
    };
  }

  // Dots lie on a snake: even rows run left to right, odd rows right to left, and the days are
  // numbered along it, so the days gone are always a prefix of one axis-aligned route and every
  // turn is a round kolam corner.
  const cols = Math.min(n, COLS);
  const rows = Math.ceil(n / cols);
  const m = Math.round(S * 0.6);
  const dots: Pt[] = Array.from({ length: n }, (_, i) => {
    const r = Math.floor(i / cols);
    const c = i % cols;
    return [m + (r % 2 ? cols - 1 - c : c) * S, m + r * S];
  });
  const w = 2 * m + (cols - 1) * S;
  const h = 2 * m + (rows - 1) * S;
  const a = S * 0.31;
  const todayIdx = Math.min(Math.max(gone - 1, 0), n - 1);
  return {
    mode: 'grid',
    w,
    h,
    dots,
    path: gone ? weave(dots.slice(0, gone), a, gone - 1) : '',
    gone,
    today: dots[todayIdx],
    ringR: a * 0.56,
    dotR: Math.min(3.4, S * 0.075),
    stroke: 1.75,
  };
}

/**
 * Kolam: the budget period as a kolam — one dot per day, one woven line looped around the days
 * gone, today ringed in the lamp colour. The line draws itself once (CSS, stopped under
 * prefers-reduced-motion). Periods over 42 days fall back to a single 20-dot progress row.
 */
export function KolamExtra({ slot, info }: HeroExtraProps) {
  const { totalDays, dayOfPeriod } = info;
  const d = useMemo(() => draw(totalDays, dayOfPeriod), [totalDays, dayOfPeriod]);
  if (slot !== 'footer') return null;
  const day = Math.min(Math.max(dayOfPeriod, 1), totalDays);
  const todayIdx = d.dots.findIndex((p) => p === d.today);
  return (
    <figure className="only-kolam kol" aria-hidden>
      <div className="kol-in">
        <svg
          className={`kol-svg kol-${d.mode}`}
          viewBox={`0 0 ${d.w} ${d.h}`}
          style={{ aspectRatio: `${d.w} / ${d.h}` }}
        >
          {d.path && (
            <path className="kol-line" pathLength={1} d={d.path} strokeWidth={d.stroke} />
          )}
          {d.dots.map((p, i) =>
            i === todayIdx ? null : (
              <circle
                key={i}
                className={i < d.gone ? 'kol-dot kol-gone' : 'kol-dot'}
                cx={p[0]}
                cy={p[1]}
                r={d.dotR}
              />
            ),
          )}
          <circle
            className="kol-ring"
            cx={d.today[0]}
            cy={d.today[1]}
            r={d.ringR}
            strokeWidth={d.stroke}
          />
          <circle className="kol-today" cx={d.today[0]} cy={d.today[1]} r={d.dotR * 1.25} />
        </svg>
        <figcaption className="kol-key">
          <b>
            Day {day} of {totalDays}
          </b>
          <span className="kol-k kol-k-gone">Gone</span>
          <span className="kol-k kol-k-today">Today</span>
          <span className="kol-k kol-k-come">To come</span>
        </figcaption>
      </div>
    </figure>
  );
}
