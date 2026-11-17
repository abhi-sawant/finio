import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 15,
  slug: 'kolam',
  name: 'Kolam',
  tagline: 'The rice-flour drawing made fresh on the threshold each morning: a grid of dots and one line looped around them. For people who want calm, order and the small daily satisfaction of closing the loop on today.',
  blurb: 'The threshold drawing. Swept stone and red oxide, one monoline weight, dot-row dividers, and the month drawn as a kolam whose line loops every day gone.',
  fonts: 'https://fonts.googleapis.com/css2?family=Kumbh+Sans:wght@300;400;500;600;700&display=swap',
  swatch: ['#e3ddd1', '#2a1b15', '#8f2d1c', '#1c6436'],
  swatchDark: ['#651e14', '#f8f0e4', '#f4c24f', '#aee8c4'],
  notes: [
    ['Shape', 'One line weight (1.5–2px, round caps) draws everything. No filled cards: areas are marked by dot rows; where a container is needed it is a single pill outline. Keys, icons and tabs are circles.'],
    ['Type', 'Kumbh Sans throughout, a monoline geometric that matches the drawn line: 300 for the hero figure (58px phone, 104px desktop), 600 for rows and headings, tabular numerals.'],
    ['Density', 'Airy to medium and symmetric: 20px gutters, 56–60px transaction rows, dot rows instead of rules, a kaavi band holding the header on every screen.'],
    ['Motion', 'The kolam line draws itself once in 1.2s, ease-out, leaving today, rounding the fifteen dots and coming home to today; then today’s lamp ring settles in. Keys fill on press.'],
    ['Voice', 'Plain. Only “Day 15 of 31”, a three-item key (Gone, Today, To come) and “Each dot is 5% of the limit” are added, all true of the data.'],
    ['Signature', 'The month as a kolam: 31 dots (5-7-7-7-5), one continuous woven line that turns only at right angles and loops the 15 days gone, today ringed in lamp yellow. Budgets and goals reuse it as a 20-dot row.'],
  ],
};

/* ---------- kolam geometry (generated at build time) ---------- */
const f = (v) => (Math.round(v * 100) / 100).toString();

// One closed line woven around a route of dots: it passes alternate sides of each dot going out,
// the opposite sides coming back, and crosses itself midway between neighbours.
// It starts and ends beside the last dot of the route, or beside dot \`from\` when given.
function weave(pts, a, from = -1) {
  const n = pts.length;
  if (!n) return '';
  const T = pts.map((_, i) => {
    const p = pts[Math.max(i - 1, 0)], q = pts[Math.min(i + 1, n - 1)];
    const dx = q[0] - p[0], dy = q[1] - p[1], l = Math.hypot(dx, dy);
    return l ? [dx / l, dy / l] : [1, 0];
  });
  const dir = (i, j) => { const l = Math.hypot(pts[j][0] - pts[i][0], pts[j][1] - pts[i][1]); return [(pts[j][0] - pts[i][0]) / l, (pts[j][1] - pts[i][1]) / l]; };
  // Points of one strand beside dot i, in travel order. On the outside of a right-angle turn the
  // strand takes three points on the circle around the dot, so the corner is a round arc.
  const off = (i, sg) => {
    const N = [-T[i][1] * sg, T[i][0] * sg], c = [pts[i][0] + N[0] * a, pts[i][1] + N[1] * a];
    if (i === 0 || i === n - 1) return [c];
    const d1 = dir(i - 1, i), d2 = dir(i, i + 1);
    if (Math.abs(d1[0] * d2[0] + d1[1] * d2[1]) > 0.5 || N[0] * (d2[0] - d1[0]) + N[1] * (d2[1] - d1[1]) > 0) return [c];
    return [[pts[i][0] - d2[0] * a, pts[i][1] - d2[1] * a], c, [pts[i][0] + d1[0] * a, pts[i][1] + d1[1] * a]];
  };
  const mid = (i) => [(pts[i][0] + pts[i + 1][0]) / 2, (pts[i][1] + pts[i + 1][1]) / 2];
  const P = [[pts[n - 1][0] + T[n - 1][0] * a, pts[n - 1][1] + T[n - 1][1] * a]];
  for (let i = n - 1; i >= 0; i--) {
    P.push(...off(i, i % 2 ? 1 : -1).reverse());
    if (i > 0) P.push(mid(i - 1));
  }
  P.push([pts[0][0] - T[0][0] * a, pts[0][1] - T[0][1] * a]);
  for (let i = 0; i < n; i++) {
    P.push(...off(i, i % 2 ? -1 : 1));
    if (i < n - 1) P.push(mid(i));
  }
  if (from >= 0) {
    const j = P.findIndex((q) => Math.hypot(q[0] - pts[from][0], q[1] - pts[from][1]) < a * 1.01);
    P.push(...P.splice(0, j));
  }
  const m = P.length, g = (i) => P[((i % m) + m) % m];
  let d = `M${f(P[0][0])} ${f(P[0][1])}`;
  for (let i = 0; i < m; i++) {
    const p0 = g(i - 1), p1 = g(i), p2 = g(i + 1), p3 = g(i + 2);
    d += `C${f(p1[0] + (p2[0] - p0[0]) / 6)} ${f(p1[1] + (p2[1] - p0[1]) / 6)} ${f(p2[0] - (p3[0] - p1[0]) / 6)} ${f(p2[1] - (p3[1] - p1[1]) / 6)} ${f(p2[0])} ${f(p2[1])}`;
  }
  return d + 'Z';
}

const TODAY = 15, MONTH = 31;

// 31 dots as 5-7-7-7-5, days in reading order. The line's route visits exactly the 15 dots gone,
// by right-angle steps only, so every turn is a round kolam corner; it starts and ends at today.
const ROUTE = [[6, 1], [5, 1], [5, 0], [4, 0], [4, 1], [3, 1], [3, 0], [2, 0], [2, 1], [2, 2], [1, 2], [0, 2], [0, 1], [1, 1], [1, 0]];
function kolam(s, sw) {
  const m = Math.round(s * 0.6);
  const rows = [[1, 2, 3, 4, 5], [0, 1, 2, 3, 4, 5, 6], [0, 1, 2, 3, 4, 5, 6], [0, 1, 2, 3, 4, 5, 6], [1, 2, 3, 4, 5]];
  const route = ROUTE.map(([c, r]) => [m + c * s, m + r * s]);
  const pts = rows.flatMap((cs, r) => cs.map((c) => [m + c * s, m + r * s]));
  const a = s * 0.31, r = Math.min(3.4, s * 0.075), w = 2 * m + 6 * s, h = 2 * m + 4 * s;
  const t = pts[TODAY - 1];
  return `<svg class="ks" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" role="img" aria-label="Day ${TODAY} of ${MONTH}: ${TODAY} dots looped, ${MONTH - TODAY} to come">
<path class="kp" pathLength="1" d="${weave(route, a, 9)}" stroke-width="${sw}"/>
${pts.map((p, i) => (i === TODAY - 1 ? '' : `<circle class="kd${i < TODAY ? ' g' : ''}" cx="${p[0]}" cy="${p[1]}" r="${f(r)}"/>`)).join('')}
<circle class="kr" cx="${t[0]}" cy="${t[1]}" r="${f(a * 0.56)}" stroke-width="${sw}"/><circle class="kt" cx="${t[0]}" cy="${t[1]}" r="${f(r * 1.25)}"/>
</svg>`;
}

// 20 dots, 5% each; the line loops the spent share (rounded to the nearest dot).
function dots(pct, cls = '') {
  const N = 20, p = 16, k = Math.round(pct / 5);
  const pts = Array.from({ length: N }, (_, i) => [p / 2 + i * p, 10]);
  return `<svg class="dg ${cls}" viewBox="0 0 ${N * p} 20" role="img" aria-label="${pct}% used">${k ? `<path d="${weave(pts.slice(0, k), 5.6)}"/>` : ''}${pts
    .map((q, i) => `<circle class="${i < k ? 'g' : ''}" cx="${q[0]}" cy="10" r="1.8"/>`)
    .join('')}</svg>`;
}

const glyph = (k) =>
  `<svg class="gy" width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">${
    k === 'g' ? '<circle class="gl" cx="8" cy="8" r="6.2"/><circle class="gd" cx="8" cy="8" r="2"/>' : k === 't' ? '<circle class="gr" cx="8" cy="8" r="5"/><circle class="gt" cx="8" cy="8" r="2.4"/>' : '<circle class="gb" cx="8" cy="8" r="2"/>'
  }</svg>`;
const key = `<span class="ky">${glyph('g')}Gone</span><span class="ky">${glyph('t')}Today</span><span class="ky">${glyph('b')}To come</span>`;

/* ---------- atoms ---------- */
const ib = (i, label) => `<span class="ib" role="button" tabindex="0" aria-label="${label}">${ic(i, 18, 1.6)}</span>`;
const tabs = (active) =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}><i>${ic(i, 19, 1.6)}</i><em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" role="button" tabindex="0" aria-label="Add transaction"><svg width="76" height="76" viewBox="0 0 76 76" aria-hidden="true"><circle class="fd" cx="38" cy="38" r="27"/><path class="fp" d="M38 28v20M28 38h20"/><circle class="fo" cx="38" cy="4" r="2"/><circle class="fo" cx="72" cy="38" r="2"/><circle class="fo" cx="38" cy="72" r="2"/><circle class="fo" cx="4" cy="38" r="2"/></svg></span>`;
const band = (title, l = '', r = '') => `<header class="band">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;
const sh = (t, a = 'See all') => `<div class="sh"><h2>${t}</h2><a>${a}</a></div>`;
const privacy = `<small>${ic('shield', 13, 1.6)}Kept only on this device</small>`;

const row = (r, date = '') =>
  `<div class="rw"><span class="ci">${ic(r.icon, 18, 1.6)}</span><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 1.8) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const heroText = `<h2 class="lbl">Safe to spend today</h2>
<p class="fig">₹${D.safe}</p>
<p class="sub">₹${D.left} left this month · ${D.days} days to go</p>
<p class="sub s2">Spent ₹${D.spent} of ₹${D.budget} · ${D.pct}%</p>`;

const alert = `<p class="al"><span class="ad">${ic('alert', 15, 1.9)}</span><span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const accounts = `<div class="ac">${D.accounts
  .map(
    (a) =>
      `<div class="ar"><i></i><b>${a.name}</b><small>${a.type}</small>${a.owed ? `<span class="v neg"><em>Owed</em>−₹${a.bal}</span>` : `<span class="v">₹${a.bal}</span>`}</div>`,
  )
  .join('')}<div class="at"><b>Total balance</b><span class="v">₹${D.total}</span></div></div>`;

/* ---------- screens ---------- */
const home = `
<header class="band who"><div class="wh"><b>${D.user}</b>${privacy}</div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
<div class="pg">
  <section class="hero">
    ${heroText}
    <figure class="kf">${kolam(30, 1.75)}<figcaption><b>Day ${TODAY} of ${MONTH}</b>${key}</figcaption></figure>
  </section>
  ${alert}
  ${sh('Where it sits')}
  ${accounts}
  ${sh('Latest')}
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
${band('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
<div class="pg">
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span>${ic('chevl', 16, 1.8)}</span><b>October 2026</b><span>${ic('chevr', 16, 1.8)}</span></div>
  ${D.groups.map((g) => `<h3 class="dl">${g.date}</h3>${g.rows.map((r) => row(r)).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
${band('Add transaction', ib('back', 'Back'), '')}
<div class="pg">
  <div class="ty"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amt"><span class="cur">₹</span>2,450<i class="caret"></i></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 21, 1.6)].map((k) => `<button${k.length > 1 ? ' aria-label="Delete"' : ''}>${k}</button>`).join('')}</div>
  <div class="fl"><small>Account</small><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 16, 1.8)}</div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}><i>${ic(i, 19, 1.6)}</i><u></u>${nm}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
${band('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
<div class="pg">
  <div class="per"><b>Monthly · October 2026</b><span>Day ${TODAY} of ${MONTH}</span></div>
  ${D.budgets
    .map((b) => {
      const w = b.pct > 85;
      return `<section class="bd${w ? ' w' : ''}">
    <div class="bh"><b>${b.name}</b><em class="st ${w ? 'wn' : 'ok'}">${ic(w ? 'alert' : 'check', 13, 2)}${b.state}</em></div>
    ${dots(b.pct, w ? 'w' : '')}
    <div class="bf"><span>₹${b.spent} of ₹${b.limit}</span><span>₹${b.left} left</span><strong>${b.pct}%</strong></div>
  </section>`;
    })
    .join('')}
  <p class="foot">${glyph('b')}Each dot is 5% of the limit. The line loops what is spent.</p>
</div>
${tabs('')}`;

const desk = `
<aside class="sd">
  <div class="mk"><svg width="46" height="22" viewBox="0 0 46 22" aria-hidden="true"><path d="${weave([[9, 11], [23, 11], [37, 11]], 5.5)}"/><circle cx="9" cy="11" r="1.7"/><circle cx="23" cy="11" r="1.7"/><circle cx="37" cy="11" r="1.7"/></svg>Finio</div>
  <span class="addb" role="button" tabindex="0">${ic('plus', 16, 2)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 17, 1.6)}${nm}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="dh"><div class="wh"><b>${D.user}</b>${privacy}</div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  <div class="dg2">
    <section class="hero big">
      ${heroText}
      <figure class="kf">${kolam(62, 2)}<figcaption><b>Day ${TODAY} of ${MONTH}</b>${key}</figcaption></figure>
      ${alert}
    </section>
    <div class="dr">
      ${sh('Where it sits')}
      ${accounts}
      ${sh('Savings goals')}
      <div class="gs">${D.goals.map((g) => `<div class="go"><div class="gh"><b>${g.name}</b><span>${g.pct}%</span></div>${dots(g.pct)}<small>₹${g.have} of ₹${g.target}</small></div>`).join('')}</div>
      ${sh('Latest')}
      ${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}
    </div>
  </div>
</main>`;

export const css = `
.app{--g:#e3ddd1;--ink:#2a1b15;--mut:#645446;--band:#8f2d1c;--on-band:#f8f0e4;--band-mut:#f1d5c8;--line:#8f2d1c;--sel:#8f2d1c;--lamp:#a05f00;--dot:rgba(42,27,21,.46);--bare:#7d6c5e;--pos:#1c6436;--blk:#2a1b15;--on-blk:#f4ecdf;--blk-ic:#f4c24f;--fab:#8f2d1c;--on-fab:#f8f0e4;
  --dots:radial-gradient(circle,var(--dot) 1.1px,transparent 1.5px);
  background:var(--g);color:var(--ink);font-family:'Kumbh Sans',system-ui,sans-serif;font-size:14px;line-height:1.3;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--sel)}
html[data-mode=dark] .app{--g:#651e14;--ink:#f8f0e4;--mut:#e9cabd;--band:#4a130c;--on-band:#f8f0e4;--band-mut:#e9cabd;--line:#f8f0e4;--sel:#f4c24f;--lamp:#f4c24f;--dot:rgba(248,240,228,.5);--bare:#d9a999;--pos:#aee8c4;--blk:#f8f0e4;--on-blk:#571910;--blk-ic:#571910;--fab:#f8f0e4;--on-fab:#651e14}
.app ::selection{background:var(--sel);color:var(--g)}
.app :focus-visible{outline:2px solid var(--sel);outline-offset:3px;border-radius:999px}
.app>.sb{color:var(--on-band)}
.app b,.app h1,.app h2,.app h3,.app strong{font-weight:600}
.app em,.app i,.app u{font-style:normal;text-decoration:none}

/* kaavi band */
.band{position:absolute;left:0;right:0;top:0;height:104px;padding:50px 20px 0;display:flex;align-items:center;gap:10px;color:var(--on-band);background:radial-gradient(circle,rgba(248,240,228,.13) 1px,transparent 1.4px) 5px 3px/14px 14px,var(--band)}
.band::after{content:'';position:absolute;left:0;right:0;bottom:5px;height:4px;background:radial-gradient(circle,rgba(248,240,228,.8) 1.2px,transparent 1.6px) 1px 0/10px 4px repeat-x}
.band h1{flex:1;font-size:21px;letter-spacing:-.005em}
.band .rt,.dh .rt{display:flex;gap:8px}
.band.who{justify-content:space-between;padding-top:42px}
.wh b{display:block;font-size:20px;line-height:1.15}
.wh small{display:flex;align-items:center;gap:5px;font-size:12.5px;color:var(--band-mut);margin-top:1px}
.ib{width:36px;height:36px;border-radius:50%;border:1.5px solid currentColor;display:grid;place-items:center;flex:none}
.ib:active{background:currentColor}
.band .ib{border-color:rgba(248,240,228,.6)}
.band>.ib:first-child{margin-right:2px}
.app .pg{position:absolute;inset:104px 0 0;padding:12px 20px 0}

/* hero + the month kolam */
.lbl{font-size:14px;font-weight:500;color:var(--mut)}
.fig{font-size:58px;font-weight:300;line-height:1;letter-spacing:-.025em;margin:1px 0 4px -2px}
.sub{font-size:14.5px;font-weight:500}
.sub.s2{color:var(--mut);font-weight:400;margin-top:1px}
.kf{display:flex;align-items:center;gap:6px;margin:2px 0 0 -8px}
.ks{flex:none;display:block}
.kp{fill:none;stroke:var(--line);stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:1;animation:kdraw 1.2s cubic-bezier(.2,.7,.2,1) backwards}
@keyframes kdraw{from{stroke-dashoffset:1}}
.kd{fill:var(--bare)}.kd.g{fill:var(--ink)}
.kr{fill:none;stroke:var(--lamp);transform-box:fill-box;transform-origin:center;animation:kring .3s cubic-bezier(.2,.7,.2,1) 1.1s backwards}
.kt{fill:var(--lamp)}
@keyframes kring{from{opacity:0;transform:scale(.3)}}
.kf figcaption{display:flex;flex-direction:column;gap:5px;font-size:13px;color:var(--mut)}
.kf figcaption b{font-size:16px;color:var(--ink);margin-bottom:3px;white-space:nowrap}
.ky{display:flex;align-items:center;gap:6px}
.gy{flex:none}
.gl{fill:none;stroke:var(--line);stroke-width:1.5}.gd{fill:var(--ink)}.gr{fill:none;stroke:var(--lamp);stroke-width:1.5}.gt{fill:var(--lamp)}.gb{fill:var(--bare)}

/* alert: one pill outline */
.al{display:flex;align-items:center;gap:8px;height:42px;border:1.5px solid var(--ink);border-radius:999px;padding:0 8px 0 5px;font-size:13.5px;white-space:nowrap;margin-top:6px}
.ad{width:30px;height:30px;border-radius:50%;background:var(--blk);color:var(--blk-ic);display:grid;place-items:center;flex:none}

/* section head */
.sh{display:flex;justify-content:space-between;align-items:center;margin-top:13px;height:24px}
.sh h2{display:flex;align-items:center;gap:8px;font-size:16px}
.sh h2::before{content:'';width:6px;height:6px;border-radius:50%;background:var(--line)}
.sh a{font-size:13.5px;font-weight:600;color:var(--sel)}

/* where it sits */
.ar{display:flex;align-items:center;gap:8px;height:33px;background:var(--dots) 0 100%/8px 3px repeat-x}
.ar i{width:6px;height:6px;border-radius:50%;border:1.5px solid var(--ink);flex:none}
.ar b{font-size:15px}
.ar small{font-size:12.5px;color:var(--mut)}
.v{margin-left:auto;font-size:15px;font-weight:600;white-space:nowrap}
.v.neg{font-weight:700;display:flex;align-items:center;gap:7px}
.v.neg em{font-size:11px;font-weight:700;letter-spacing:.03em;background:var(--blk);color:var(--on-blk);border-radius:999px;padding:2px 7px}
.at{display:flex;align-items:center;height:34px;padding-left:14px}
.at b{font-size:15px}.at .v{font-size:18px}

/* rows */
.rw{display:flex;align-items:center;gap:12px;height:62px;background:var(--dots) 0 100%/8px 3px repeat-x}
.ci{width:36px;height:36px;border-radius:50%;border:1.5px solid var(--ink);display:grid;place-items:center;flex:none}
.rw .nm{min-width:0;flex:1}
.rw .nm b{display:flex;align-items:center;gap:6px;font-size:15.5px;line-height:1.2}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:block;font-size:12.5px;color:var(--mut);margin-top:3px}
.rw .a{font-size:15.5px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{display:inline-block;font-size:11px;font-weight:600;line-height:1;border:1.25px solid currentColor;border-radius:999px;padding:2px 7px;margin-left:7px;color:var(--ink);vertical-align:1px}
.tg.d{border-style:dotted;border-width:1.5px}
.hero~.rw,.sh+.rw{height:48px}

/* tab bar + fab */
.tabs{position:absolute;left:0;right:0;bottom:0;height:70px;padding:9px 8px 0;display:flex;background:var(--dots) 0 0/10px 3px repeat-x,var(--g)}
.tabs span{flex:1;display:flex;flex-direction:column;align-items:center;gap:1px;color:var(--mut)}
.tabs i{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;border:1.75px solid transparent}
.tabs em{font-size:10.5px;font-weight:500}
.tabs .on{color:var(--ink)}
.tabs .on i{border-color:var(--sel)}
.tabs .on em{font-weight:700}
.fab{position:absolute;right:10px;bottom:74px;width:76px;height:76px;z-index:5;border-radius:50%}
.fab svg{display:block}
.fd{fill:var(--fab)}.fp{stroke:var(--on-fab);stroke-width:2;stroke-linecap:round;fill:none}.fo{fill:var(--ink)}
.fab:active .fd{transform:scale(.94);transform-origin:center}

/* switch + month */
.ty{display:flex;height:40px;border:1.5px solid var(--ink);border-radius:999px;padding:3px}
.ty span{flex:1;display:grid;place-items:center;font-size:13.5px;font-weight:500;color:var(--mut);border:1.75px solid transparent;border-radius:999px}
.ty .on{border-color:var(--sel);color:var(--ink);font-weight:700}
.mo{display:flex;justify-content:space-between;align-items:center;margin:10px 0 2px}
.mo b{font-size:15.5px}
.mo span{width:32px;height:32px;border-radius:50%;border:1.5px solid var(--ink);display:grid;place-items:center}
.dl{display:flex;align-items:center;gap:8px;font-size:13px;color:var(--mut);margin:18px 0 0;height:18px}
.dl::before{content:'';width:6px;height:6px;border-radius:50%;background:var(--line)}

/* add */
.amt{display:flex;justify-content:center;align-items:center;font-size:58px;font-weight:300;line-height:1;letter-spacing:-.02em;height:84px}
.amt .cur{font-size:30px;font-weight:400;color:var(--mut);margin:6px 5px 0 0}
.caret{width:2px;height:50px;border-radius:2px;background:var(--sel);margin-left:6px;animation:kbl 1.1s steps(1) infinite}
@keyframes kbl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);grid-auto-rows:56px;place-items:center;padding:5px 6px;background:var(--dots) 56.33px 28px/112.67px 56px content-box border-box}
.np{background-image:radial-gradient(circle,var(--dot) 1.6px,transparent 2px)}
.np button{width:50px;height:50px;border-radius:50%;border:1.5px solid var(--ink);background:var(--g);font-size:22px;font-weight:500;display:grid;place-items:center}
.np button:active{background:var(--ink);color:var(--g)}
.fl{display:flex;align-items:center;gap:8px;height:48px;border:1.5px solid var(--ink);border-radius:999px;padding:0 16px 0 18px;margin-top:8px}
.fl small{font-size:12.5px;color:var(--mut)}
.fl b{font-size:15px;flex:1}
.fl span{font-size:13px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);row-gap:4px;margin-top:6px}
.cg span{display:flex;flex-direction:column;align-items:center;font-size:12px;font-weight:500;color:var(--mut)}
.cg i{width:38px;height:38px;border-radius:50%;display:grid;place-items:center;color:var(--ink);border:1.75px solid transparent}
.cg u{width:4px;height:4px;border-radius:50%;background:var(--dot);margin:1px 0 3px}
.cg .on{color:var(--ink);font-weight:700}
.cg .on i{border-color:var(--sel)}
.cg .on u{background:var(--sel)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:8px 20px 28px;background:var(--g)}
.cta button{width:100%;height:52px;border:0;border-radius:999px;background:var(--fab);color:var(--on-fab);font-size:16px;font-weight:600}
.cta button:active{transform:scale(.985)}

/* budgets */
.per{display:flex;justify-content:space-between;align-items:center;height:40px;border:1.5px solid var(--ink);border-radius:999px;padding:0 18px;font-size:13.5px;margin-top:4px}
.per span{color:var(--mut)}
.bd{padding:25px 0 21px;background:var(--dots) 0 100%/8px 3px repeat-x}
.bh{display:flex;justify-content:space-between;align-items:center}
.bh b{font-size:16.5px}
.st{display:flex;align-items:center;gap:5px;font-size:12px;font-weight:600;border-radius:999px;padding:3px 10px 3px 8px;border:1.5px solid transparent}
.st.ok{color:var(--pos);border-color:currentColor}
.st.wn{background:var(--blk);color:var(--on-blk);font-weight:700}
.st.wn .ic{color:var(--blk-ic)}
.dg{display:block;width:100%;height:auto;margin:9px 0 6px;overflow:visible}
.dg path{fill:none;stroke:var(--line);stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round;vector-effect:non-scaling-stroke}
.dg circle{fill:var(--bare)}.dg circle.g{fill:var(--ink)}
.dg.w path{stroke-width:2.6}
.bf{display:flex;align-items:baseline;gap:14px;font-size:14px}
.bf span+span{color:var(--mut)}
.bf strong{margin-left:auto;font-size:21px;font-weight:400;letter-spacing:-.01em}
.bd.w .bf strong{font-weight:700}
.foot{display:flex;align-items:center;gap:6px;font-size:13px;color:var(--mut);margin-top:14px}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:216px;padding:26px 18px;display:flex;flex-direction:column;color:var(--on-band);background:radial-gradient(circle,rgba(248,240,228,.12) 1px,transparent 1.4px) 5px 5px/14px 14px,var(--band)}
.sd::after{content:'';position:absolute;top:0;bottom:0;right:5px;width:4px;background:radial-gradient(circle,rgba(248,240,228,.8) 1.2px,transparent 1.6px) 0 1px/4px 10px repeat-y}
.mk{display:flex;align-items:center;gap:9px;font-size:24px;font-weight:600;letter-spacing:-.01em;margin:0 0 22px 2px}
.mk path{fill:none;stroke:currentColor;stroke-width:1.5}.mk circle{fill:currentColor}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:42px;border-radius:999px;background:var(--on-band);color:var(--band);font-size:14px;font-weight:600;margin-bottom:18px}
.addb:active{transform:scale(.98)}
.sd nav{display:flex;flex-direction:column;gap:3px}
.ni{display:flex;align-items:center;gap:11px;height:38px;padding:0 13px;border-radius:999px;border:1.75px solid transparent;font-size:14.5px;font-weight:500;color:var(--band-mut)}
.ni.on{border-color:var(--on-band);color:var(--on-band);font-weight:700}
html[data-mode=dark] .app .ni.on{border-color:var(--lamp)}
.gl{display:flex;align-items:center;gap:8px;font-size:13px;font-weight:600;color:var(--band-mut);margin:24px 0 6px 14px}
.gl::before{content:'';width:5px;height:5px;border-radius:50%;background:currentColor}
.tl{font-size:14px;padding:5px 0 5px 27px;color:var(--on-band)}
.mn{position:absolute;left:216px;right:0;top:0;bottom:0;padding:22px 38px 0}
.dh{display:flex;justify-content:space-between;align-items:center;padding-bottom:14px;background:var(--dots) 0 100%/8px 3px repeat-x}
.dh .wh b{font-size:22px}
.dh .wh small{color:var(--mut)}
.dg2{display:grid;grid-template-columns:450px 1fr;gap:0 46px;margin-top:16px}
.hero.big .lbl{font-size:16px}
.hero.big .fig{font-size:104px;margin:2px 0 8px -5px;letter-spacing:-.035em}
.hero.big .sub{font-size:17px}
.hero.big .sub.s2{font-size:15px;margin-top:3px}
.hero.big .kf{flex-direction:column;align-items:flex-start;gap:4px;margin:14px 0 0 -16px}
.hero.big .kf figcaption{flex-direction:row;align-items:center;gap:18px;font-size:13.5px;margin-left:16px}
.hero.big .kf figcaption b{font-size:17px;margin:0 6px 0 0}
.hero.big .al{margin-top:26px;height:48px;font-size:15px;padding-left:8px;gap:12px}
.dr .sh:first-child{margin-top:0}
.dr .sh{margin-top:24px;margin-bottom:2px}
.dr .ar{height:44px}.dr .at{height:44px}
.gs{display:grid;grid-template-columns:1fr 1fr;gap:0 26px}
.gh{display:flex;justify-content:space-between;align-items:baseline;margin-top:4px}
.gh b{font-size:15px}.gh span{font-size:15px;font-weight:600}
.go .dg{margin:5px 0 3px}
.go small{font-size:13px;color:var(--mut)}
.dr .rw,.dr .sh+.rw{height:56px}
.dr .ci{width:34px;height:34px}
`;

export const screens = { home, txns, add, budgets, desk };
