// Generates Monilock's Mudra icon set (favicon.svg/.ico, PWA, maskable, apple-touch) from one
// vector source (scripts/monilock-mark.mjs): a ₹100-lavender tile carrying a padlock whose shackle
// is the banknote's colour-shift security thread and whose body has an "M" cut out of it; larger
// sizes add faint guilloche engraving.
//
//   (from web/) node scripts/gen-icons.mjs public
//
// Rasterises with sharp (librsvg), which ships with the repo's dev dependencies — no browser
// needed. favicon.ico is the 48px render, converted with ffmpeg if it is installed:
// `ffmpeg -i favicon-48.png public/favicon.ico` (the script prints the command otherwise).
import fs from 'node:fs';
import path from 'node:path';
import sharp from 'sharp';
import { BODY_PATH, SHACKLE_PATH, THREAD_STOPS, THREAD_Y } from './monilock-mark.mjs';

const OUT = process.argv[2] ?? 'public';
const r1 = (n) => Math.round(n * 10) / 10;

function rosette(cx, cy, rings, base, step, petals) {
  const paths = [];
  for (let k = 0; k < rings; k++) {
    const R = base + k * step,
      amp = 7 + (k % 3) * 3,
      pet = petals + (k % 2) * 6;
    let d = '';
    for (let i = 0; i <= 360; i += 2) {
      const t = (i * Math.PI) / 180;
      const r = R + amp * Math.sin(pet * t + k * 0.55);
      d += `${i ? 'L' : 'M'}${r1(cx + r * Math.cos(t))} ${r1(cy + r * Math.sin(t))}`;
    }
    paths.push(`<path d="${d}Z" opacity="${(0.9 - k * 0.08).toFixed(2)}"/>`);
  }
  return paths.join('');
}

/**
 * variant: 'full'  — rounded tile, engraving + thread (PWA "any" icons)
 *          'bleed' — full-bleed square, content inside the maskable safe zone (maskable, apple)
 *          'small' — rounded tile, no engraving (favicons ≤ 48px)
 */
function svg(variant) {
  const bleed = variant === 'bleed';
  const small = variant === 'small';
  const tile = bleed
    ? '<rect width="512" height="512" fill="url(#bg)"/>'
    : '<rect width="512" height="512" rx="116" fill="url(#bg)"/>';
  // Content group: the mark. Scaled into the 80% safe zone when full-bleed.
  const s = bleed ? 0.78 : 1;
  const tr = `translate(${256 - 256 * s} ${256 - 256 * s}) scale(${s})`;
  const engraving = small
    ? ''
    : `<g fill="none" stroke="#fff" stroke-width="2.2" opacity="0.16" clip-path="url(#clip)">${rosette(400, 120, 6, 70, 22, 14)}</g>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <defs>
    <radialGradient id="bg" cx="0.3" cy="0.25" r="0.95">
      <stop offset="0" stop-color="#a594f2"/>
      <stop offset="0.5" stop-color="#6c57d6"/>
      <stop offset="1" stop-color="#3d2bb0"/>
    </radialGradient>
    <linearGradient id="thread" gradientUnits="userSpaceOnUse" x1="0" y1="${THREAD_Y[0]}" x2="0" y2="${THREAD_Y[1]}">
      ${THREAD_STOPS.map(([o, c]) => `<stop offset="${o}" stop-color="${c}"/>`).join('')}
    </linearGradient>
    <clipPath id="clip"><rect width="512" height="512" rx="${bleed ? 0 : 116}"/></clipPath>
  </defs>
  ${tile}
  ${engraving}
  <g transform="${tr}">
    <path fill="url(#thread)" d="${SHACKLE_PATH}"/>
    <path fill="#fff" fill-rule="evenodd" d="${BODY_PATH}"/>
  </g>
</svg>`;
}

fs.writeFileSync(path.join(OUT, 'favicon.svg'), svg('small'));
const variants = { full: svg('full'), bleed: svg('bleed'), small: svg('small') };

const targets = [
  ['pwa-64x64.png', 64, 'small'],
  ['pwa-96x96.png', 96, 'full'],
  ['pwa-192x192.png', 192, 'full'],
  ['pwa-512x512.png', 512, 'full'],
  ['maskable-icon-512x512.png', 512, 'bleed'],
  ['apple-touch-icon-180x180.png', 180, 'bleed'],
  ['favicon-48.png', 48, 'small'],
];
for (const [name, size, variant] of targets) {
  const dest = name === 'favicon-48.png' ? path.join(process.cwd(), name) : path.join(OUT, name);
  await sharp(Buffer.from(variants[variant]), { density: (72 * size) / 512 * 2 })
    .resize(size, size)
    .png()
    .toFile(dest);
  console.log('wrote', name);
}
