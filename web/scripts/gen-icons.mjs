// Generates Finio's Mudra icon set (favicon.svg/.ico, PWA, maskable, apple-touch) from one
// vector source: a ₹100-lavender tile, a heavy white "F", and a windowed colour-shift security
// thread; larger sizes add faint guilloche engraving.
//
//   (from web/) npx -y -p playwright-core node scripts/gen-icons.mjs public
//
// Rasterises with a locally installed Chrome (override with CHROME_PATH). favicon.ico is the 48px
// render converted with ffmpeg: `ffmpeg -i favicon-48.png public/favicon.ico`.
import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright-core';

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
  // Content group: F + thread. Scaled into the 80% safe zone when full-bleed.
  const s = bleed ? 0.78 : 1;
  const tr = `translate(${256 - 256 * s} ${256 - 256 * s}) scale(${s})`;
  const thread = Array.from({ length: small ? 4 : 7 }, (_, i) => {
    const n = small ? 4 : 7,
      gap = small ? 26 : 16,
      h = (512 - 2 * 70 - (n - 1) * gap) / n;
    return `<rect x="372" y="${r1(70 + i * (h + gap))}" width="${small ? 34 : 26}" height="${r1(h)}" rx="${small ? 8 : 6}"/>`;
  }).join('');
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
    <linearGradient id="thread" gradientUnits="userSpaceOnUse" x1="0" y1="70" x2="0" y2="442">
      <stop offset="0" stop-color="#4fe0a8"/>
      <stop offset="0.5" stop-color="#6fb4ff"/>
      <stop offset="1" stop-color="#d8ceff"/>
    </linearGradient>
    <clipPath id="clip"><rect width="512" height="512" rx="${bleed ? 0 : 116}"/></clipPath>
  </defs>
  ${tile}
  ${engraving}
  <g transform="${tr}">
    <g fill="url(#thread)">${thread}</g>
    <path fill="#fff" d="M128 118h196a24 24 0 0 1 24 24v32a24 24 0 0 1-24 24H212v40h92a22 22 0 0 1 22 22v28a22 22 0 0 1-22 22h-92v56a24 24 0 0 1-24 24h-36a24 24 0 0 1-24-24V142a24 24 0 0 1 24-24z"/>
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
const browser = await chromium.launch({
  executablePath:
    process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
});
const page = await browser.newPage();
for (const [name, size, variant] of targets) {
  await page.setViewportSize({ width: size, height: size });
  const uri = 'data:image/svg+xml;base64,' + Buffer.from(variants[variant]).toString('base64');
  await page.setContent(
    `<html><body style="margin:0;background:transparent"><img src="${uri}" width="${size}" height="${size}" style="display:block"></body></html>`,
  );
  await page.waitForTimeout(100);
  const dest = name === 'favicon-48.png' ? path.join(process.cwd(), name) : path.join(OUT, name);
  await page.screenshot({
    path: dest,
    omitBackground: true,
    clip: { x: 0, y: 0, width: size, height: size },
  });
  console.log('wrote', name);
}
await browser.close();
