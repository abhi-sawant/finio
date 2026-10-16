// The Monilock mark, as plain geometry in a 512-unit space. One source for every renderer:
// scripts/gen-icons.mjs (favicon, PWA, apple-touch), scripts/gen-android-icons.mjs (launcher and
// notification vectors) and the inline <MonilockMark /> in the app — so the three can never drift.
//
// The idea: a padlock whose shackle is the banknote's windowed security thread (so it takes the
// colour-shift gradient) and whose body has a heavy "M" cut out of it. Money, locked, in one glyph.

const r1 = (n) => Math.round(n * 10) / 10;

/** Polygon with every corner rounded by `r` (quadratic through the vertex). Handles concave too. */
function roundedPolygon(points, r) {
  const n = points.length;
  let d = '';
  for (let i = 0; i < n; i++) {
    const [px, py] = points[(i + n - 1) % n];
    const [cx, cy] = points[i];
    const [nx, ny] = points[(i + 1) % n];
    const toPrev = Math.hypot(px - cx, py - cy);
    const toNext = Math.hypot(nx - cx, ny - cy);
    const rr = Math.min(r, toPrev / 2, toNext / 2);
    const ax = cx + ((px - cx) / toPrev) * rr;
    const ay = cy + ((py - cy) / toPrev) * rr;
    const bx = cx + ((nx - cx) / toNext) * rr;
    const by = cy + ((ny - cy) / toNext) * rr;
    d += `${i ? 'L' : 'M'}${r1(ax)} ${r1(ay)}Q${cx} ${cy} ${r1(bx)} ${r1(by)}`;
  }
  return `${d}Z`;
}

function roundedRect(x, y, w, h, r) {
  return (
    `M${x + r} ${y}h${w - 2 * r}a${r} ${r} 0 0 1 ${r} ${r}v${h - 2 * r}` +
    `a${r} ${r} 0 0 1 -${r} ${r}h${-(w - 2 * r)}a${r} ${r} 0 0 1 -${r} -${r}v${-(h - 2 * r)}` +
    `a${r} ${r} 0 0 1 ${r} -${r}z`
  );
}

// The "M": two stems and a V, drawn as one outline, clockwise from the bottom-left.
const M_POINTS = [
  [174, 392],
  [174, 266],
  [212, 266],
  [256, 322],
  [300, 266],
  [338, 266],
  [338, 392],
  [302, 392],
  [302, 324],
  [268, 368],
  [244, 368],
  [210, 324],
  [210, 392],
];

/** The padlock body with the M knocked out of it — fill with fill-rule="evenodd". */
export const BODY_PATH = roundedRect(116, 224, 280, 206, 48) + roundedPolygon(M_POINTS, 6);

/** The shackle as a filled outline (legs run on under the body, which covers them). */
export const SHACKLE_PATH =
  'M165 250V176A91 91 0 0 1 347 176V250H305V176A49 49 0 0 0 207 176V250Z';

/** Colour-shift of the security thread, top of the shackle to its feet. */
export const THREAD_STOPS = [
  [0, '#4fe0a8'],
  [0.5, '#6fb4ff'],
  [1, '#d8ceff'],
];
export const THREAD_Y = [85, 250];

/** Tight bounding box of the whole mark, for re-framing it on other canvases. */
export const MARK_BOX = { x: 116, y: 85, w: 280, h: 345 };

/** Farthest any painted point sits from the tile centre (256, 256) — drives the safe-zone scale. */
export const MARK_RADIUS = 206;

/** The mark as SVG, ready to drop inside any <svg> that already defines a `thread` gradient. */
export function markSvg({ threadFill = 'url(#thread)', bodyFill = '#fff' } = {}) {
  return (
    `<path fill="${threadFill}" d="${SHACKLE_PATH}"/>` +
    `<path fill="${bodyFill}" fill-rule="evenodd" d="${BODY_PATH}"/>`
  );
}

/** A standalone SVG string for the mark alone (no tile) — used by the in-app logo. */
export function standaloneSvg() {
  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${MARK_BOX.x - 12} ${MARK_BOX.y - 12} ${MARK_BOX.w + 24} ${MARK_BOX.h + 24}">` +
    `<defs><linearGradient id="thread" gradientUnits="userSpaceOnUse" x1="0" y1="${THREAD_Y[0]}" x2="0" y2="${THREAD_Y[1]}">` +
    THREAD_STOPS.map(([o, c]) => `<stop offset="${o}" stop-color="${c}"/>`).join('') +
    `</linearGradient></defs>${markSvg()}</svg>`
  );
}
