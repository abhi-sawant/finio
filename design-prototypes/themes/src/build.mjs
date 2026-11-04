import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { page } from './frame.mjs';
import * as khata from './khata.mjs';
import * as gulal from './gulal.mjs';
import * as shunya from './shunya.mjs';
import * as bagh from './bagh.mjs';

const out = join(dirname(fileURLToPath(import.meta.url)), '..');
const themes = [khata, gulal, shunya, bagh];

for (const t of themes) {
  const html = page({ ...t.meta, css: t.css, screens: t.screens });
  writeFileSync(join(out, `${t.meta.slug}.html`), html);
}

const sw = (cols) => `<span class="sw">${cols.map((c) => `<i style="background:${c}"></i>`).join('')}</span>`;
const card = (num, name, line, light, dark, href, tag = '') => `
<a class="card" href="${href}">
  <div class="sws">${sw(light)}${sw(dark)}</div>
  <div class="cn"><span class="n">${num}</span><h2>${name}</h2>${tag ? `<em>${tag}</em>` : ''}</div>
  <p>${line}</p>
</a>`;

const index = `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Finio themes — prototypes</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{background:#17171b;color:#e9e9ef;font-family:ui-sans-serif,-apple-system,"Segoe UI",system-ui,sans-serif;padding:48px 32px 80px;min-height:100vh}
.w{max-width:1100px;margin:0 auto}
h1{font-size:32px;font-weight:600;letter-spacing:-.015em}
.lead{color:#a9a9b6;font-size:16px;line-height:1.55;max-width:62ch;margin:10px 0 36px}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(320px,1fr));gap:16px}
.card{display:block;text-decoration:none;color:inherit;background:#1f1f26;border:1px solid #2c2c35;border-radius:14px;padding:18px;transition:border-color .2s,transform .2s cubic-bezier(.16,1,.3,1)}
.card:hover{border-color:#6a6a7c;transform:translateY(-2px)}
.card.def{grid-column:1/-1;display:grid;grid-template-columns:minmax(0,260px) 1fr;gap:6px 28px;align-items:center}
.card.def img{grid-row:1/4;width:100%;border-radius:8px;display:block}
.sws{display:flex;gap:10px;margin-bottom:14px}
.sw{display:flex;border-radius:8px;overflow:hidden;border:1px solid #3a3a45;flex:1;height:34px}.sw i{flex:1}
.cn{display:flex;align-items:center;gap:10px}
.n{font-size:12px;color:#8a8a98;border:1px solid #3a3a45;border-radius:99px;padding:1px 8px}
h2{font-size:21px;font-weight:600;letter-spacing:-.01em}
em{font-style:normal;font-size:12px;background:#e9e9ef;color:#17171b;border-radius:99px;padding:1px 9px;font-weight:600}
p{color:#a9a9b6;font-size:14px;line-height:1.55;margin-top:8px}
</style></head><body><div class="w">
<h1>Finio themes</h1>
<p class="lead">Five themes, five personalities — shape, type, density, motion and voice change, not just colour. Mudra stays the default. Open any prototype: each has a light/dark toggle and shows Home, Transactions, Add, Budgets and the desktop layout on identical data.</p>
<div class="grid">
<a class="card def" href="../../web/public/landing/desktop-light.webp"><img src="../../web/public/landing/desktop-light.webp" alt="Mudra desktop home"><div><div class="cn"><span class="n">1</span><h2>Mudra</h2><em>Default · shipped</em></div><p>Banknote security printing. Frosted glass over lavender-mint-peach note paper, guilloche engraving, a colour-shift thread, denomination-tinted accounts. Luxe, calm and ornamental; one lavender accent.</p></div></a>
${card(2, khata.meta.name, 'The accountant’s ledger. Celadon columnar paper, blue-black ink, red margin rule. Compact, ruled, serif; rupees and paise in split columns.', khata.meta.swatch, khata.meta.swatchDark, 'khata.html')}
${card(3, gulal.meta.name, 'Holi-powder pop. Sticker-bright, hard-shadowed, chunky; a colour per category and a price-tag burst on your budget.', gulal.meta.swatch, gulal.meta.swatchDark, 'gulal.html')}
${card(4, shunya.meta.name, 'Zero. No cards, no fills, almost no colour. Light-weight numerals, hairlines, air — and a month line that shows pace.', shunya.meta.swatch, shunya.meta.swatchDark, 'shunya.html')}
${card(5, bagh.meta.name, 'A kitchen garden. Leaf-cornered, soft and tactile; goals grow as plants, the hero sun tracks the month.', bagh.meta.swatch, bagh.meta.swatchDark, 'bagh.html')}
</div></div></body></html>`;
writeFileSync(join(out, 'index.html'), index);
console.log('built', themes.map((t) => t.meta.slug).join(', '));
