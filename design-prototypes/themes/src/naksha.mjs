import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 13,
  slug: 'naksha',
  name: 'Naksha',
  defaultMode: 'dark',
  tagline: 'The drawing sheet. Your month drafted to scale on a blueprint — every amount with an extent gets a dimension line — for people who want their money measured, not estimated.',
  blurb: 'The architect’s plan. Prussian-blue cyanotype or tracing paper, 1px linework with crop marks, hatched selections, red-pencil revisions, and every budget dimensioned to scale.',
  fonts:
    'https://fonts.googleapis.com/css2?family=Azeret+Mono:wght@400;500;600;700&family=Sofia+Sans+Semi+Condensed:wght@400;500;600;700&display=swap',
  swatch: ['#f6f9fc', '#0d2c6b', '#1647a6', '#c4271b'],
  swatchDark: ['#12397a', '#f4f8ff', '#ffe98a', '#ff9684'],
  notes: [
    ['Shape', 'Nothing is filled. Panels are 1px drawn rectangles with 5px crop marks at the corners, 0 radius; section headers are ruled title-block strips; selection is a 45° hatch, never a solid.'],
    ['Type', 'Azeret Mono 500–700 for every figure and annotation label (13–72px, tabular); Sofia Sans Semi Condensed for names and notes. Labels are lettered in caps, row names stay normal case.'],
    ['Density', 'Drawing-sheet tight: 8px drafting grid, 1px rules between rows, no padding spent on cards. Desktop is one framed sheet with a legend column and a corner title block.'],
    ['Motion', 'Dimension lines draw in once, left to right (0.55s ease-out), then their arrowheads appear. Nothing else moves; keys and buttons hatch on press.'],
    ['Voice', 'Drafting notes in the gaps, all true to the data: “÷ 16 days”, “Day 15 of 31 · 48%”, “Scale 0 to ₹40,000”. Product labels are untouched.'],
    ['Signature', 'Dimension lines. The budget is a bar drawn to scale with arrowed spans for spent and left, a dash-dot datum at today, and the ₹1,562 callout on a leader. Housing at 94% gets a red-pencil revision cloud.'],
  ],
};

/* ---------- helpers ---------- */
let uid = 0;
const r1 = (v) => Math.round(v * 10) / 10;
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 19, 1.5)}</span>`;
const tabs = (active) =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.5)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction"><i>${ic('plus', 24, 2)}</i></span>`;
const tb = (t, v = '') => `<div class="tb"><h2>${t}</h2><i></i>${v ? `<a>${v}</a>` : ''}</div>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;

const arL = (x, y) => `M${x} ${y}l7 -2.6v5.2z`;
const arR = (x, y) => `M${x} ${y}l-7 -2.6v5.2z`;
const hatch = (id, warn) =>
  `<pattern id="${id}" width="5" height="5" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><path d="M.5 0V5" class="hp${warn ? ' w' : ''}"/></pattern>`;
// one dimension line: arrowed span x1→x2 at height y with its figure sitting above it
const dim = (x1, x2, y, label, d = 0, tx = null, anchor = 'middle') =>
  `<g class="dm" style="--d:${d}s"><path class="dl" pathLength="1" d="M${r1(x1)} ${y}H${r1(x2)}"/><path class="ah" d="${arL(r1(x1), y)}${arR(r1(x2), y)}"/><text x="${r1(tx ?? (x1 + x2) / 2)}" y="${y - 6}" text-anchor="${anchor}">${label}</text></g>`;

/* the hero drawing: budget bar to scale, chained dimensions, datum at today, leader from the callout */
const heroDraw = (W, shelf, bh = 18) => {
  const id = `h${++uid}`;
  const x0 = 1, x1 = W - 1, sp = x1 - x0;
  const xs = x0 + (sp * D.pct) / 100;
  const xd = x0 + (sp * 15) / 31;
  const dy = 66, bt = 78, bb = bt + bh, H = bb + 22;
  const land = xs + 40;
  return `<svg class="draw" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Spent ₹${D.spent} of ₹${D.budget}, ₹${D.left} left. Today is day 15 of 31.">
  <defs>${hatch(id)}</defs>
  <g class="ld" aria-hidden="true"><path class="dl" pathLength="1" d="M0 4H${shelf}L${r1(land)} ${dy - 3}"/><circle class="ah" cx="${r1(land)}" cy="${dy}" r="2.6"/></g>
  <text x="0" y="22">₹${D.left} <tspan class="w">÷ ${D.days} days</tspan></text>
  <path class="ex" d="M${x0} ${dy - 8}V${bt}M${r1(xs)} ${dy - 8}V${bt}M${x1} ${dy - 8}V${bt}"/>
  ${dim(x0, xs, dy, `₹${D.spent} <tspan class="w">spent</tspan>`, 0.05)}
  ${dim(xs, x1, dy, `₹${D.left} <tspan class="w">left</tspan>`, 0.2, (xs + x1) / 2 + 14)}
  <rect class="fill" x="${x0}" y="${bt}" width="${r1(xs - x0)}" height="${bh}" fill="url(#${id})"/>
  <rect class="bar" x="${x0}" y="${bt}" width="${sp}" height="${bh}"/>
  <path class="bar" d="M${r1(xs)} ${bt}V${bb}"/>
  <path class="tk" d="${[0.25, 0.5, 0.75].map((f) => `M${r1(x0 + sp * f)} ${bb}v4`).join('')}"/>
  <path class="cl" d="M${r1(xd)} ${dy + 6}V${bb + 7}"/><path class="dt" d="M${r1(xd - 4)} ${dy + 3}h8l-4 6z"/>
  <text class="sc" x="${x0}" y="${bb + 17}">0</text>
  <text class="sc" x="${r1(xd)}" y="${bb + 19}" text-anchor="middle"><tspan class="w">Day</tspan> 15 <tspan class="w">of</tspan> 31 · ${D.dayPct}%</text>
  <text class="sc" x="${x1}" y="${bb + 17}" text-anchor="end">₹${D.budget}</text>
</svg>`;
};

/* a scaled bar with a chained dimension (the part) above and the overall dimension below */
const barDraw = (W, pct, part, whole, warn = false, d = 0) => {
  const id = `h${++uid}`;
  const x0 = 1, x1 = W - 1, sp = x1 - x0;
  const xs = x0 + (sp * pct) / 100;
  const narrow = xs - x0 < 96;
  const top =
    pct === 0
      ? `<text x="${x0 + 5}" y="14">${part}</text>`
      : narrow
        ? dim(x0, xs, 20, part, d, xs + 8, 'start')
        : dim(x0, xs, 20, part, d);
  return `<svg class="draw${warn ? ' warn' : ''}" width="${W}" height="70" viewBox="0 0 ${W} 70" aria-hidden="true">
  <defs>${hatch(id, warn)}</defs>
  <path class="ex" d="M${x0} ${pct ? 12 : 4}V68${pct ? `M${r1(xs)} 12V30` : ''}M${x1} 44V68"/>
  ${top}
  ${pct ? `<rect class="fill" x="${x0}" y="30" width="${r1(xs - x0)}" height="14" fill="url(#${id})"/><path class="bar pt" d="M${r1(xs)} 30V44"/>` : ''}
  <rect class="bar" x="${x0}" y="30" width="${sp}" height="14"/>
  ${dim(x0, x1, 64, whole, d + 0.1)}
</svg>`;
};

/* red-pencil revision cloud around a note */
const cloud = (label) => {
  const r = 5, nx = 10, ny = 2, p = 6;
  const a = (dx, dy) => `a${r} ${r} 0 0 1 ${dx} ${dy}`;
  const dPath = `M${p} ${p}${a(2 * r, 0).repeat(nx)}${a(0, 2 * r).repeat(ny)}${a(-2 * r, 0).repeat(nx)}${a(0, -2 * r).repeat(ny)}z`;
  const w = nx * 2 * r + 2 * p, h = ny * 2 * r + 2 * p;
  return `<span class="cloud"><svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true"><path d="${dPath}"/></svg><b>${label}</b></span>`;
};

const callout = (fs) => `<div class="co"><h2>Safe to spend today</h2><div class="fig" style="font-size:${fs}px">₹${D.safe}</div></div>`;
const hero = (W, fs, bh) => `<section class="hero">
  ${callout(fs)}
  ${heroDraw(W, Math.round(fs * 3.72) + 6, bh)}
  <p class="hn"><span>₹${D.left} left this month · ${D.days} days to go</span><span>${D.pct}% used</span></p>
</section>`;

const alertN = `<p class="rev">${ic('alert', 18, 1.6)}<span>${D.due.name} payment of <b>₹${D.due.amt}</b></span><em>Due in ${D.due.days} days</em></p>`;

const sits = `<section class="pn">${tb('Where it sits', 'See all')}
  ${D.accounts.map((a) => `<div class="ac"><div><b>${a.name}</b><small>${a.type}</small></div><span class="v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`).join('')}
  <div class="ac tot"><div><b>Total balance</b></div><span class="v">₹${D.total}</span></div>
</section>`;

const row = (r, date = '') =>
  `<div class="rw"><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 1.75) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  <header class="hd"><div class="who"><h1>Hello, ${D.user}</h1><p>${ic('shield', 13, 1.6)}Kept only on this device</p></div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero(350, 54, 18)}
  ${alertN}
  ${sits}
  <section class="pn">${tb('Latest', 'See all')}${row(D.groups[0].rows[1])}</section>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on"><em>All</em></span><span><em>Expense</em></span><span><em>Income</em></span><span><em>Transfer</em></span></div>
  <div class="mo"><span>${ic('chevl', 16, 1.75)}</span><b>October 2026</b><span>${ic('chevr', 16, 1.75)}</span></div>
  ${D.groups.map((g) => `<section class="pn">${tb(g.date)}${g.rows.map((r) => row(r)).join('')}</section>`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty"><span class="on"><em>Expense</em></span><span><em>Income</em></span><span><em>Transfer</em></span></div>
  <div class="amt"><span class="fig">₹2,450<i class="caret"></i></span></div>
  <div class="np pn">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.5)].map((k) => `<button>${k}</button>`).join('')}</div>
  <div class="fl pn"><small>Account</small><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 16, 1.75)}</div>
  <section class="pn">${tb('Category', 'Split')}
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.5)}<em>${nm}</em></span>`).join('')}</div></section>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="tb per"><h2>Monthly · October 2026</h2><i></i><a>Day 15 of 31</a></div>
  ${D.budgets
    .map((b, k) => {
      const warn = b.pct > 85;
      return `<section class="bd${warn ? ' warn' : ''}">
    <div class="bh"><b>${b.name}</b><span class="pc">${b.pct}%</span>${warn ? cloud(b.state) : `<span class="ok">${b.state}</span>`}</div>
    ${barDraw(350, b.pct, `₹${b.spent} <tspan class="w">spent</tspan>`, `₹${b.limit} <tspan class="w">limit</tspan>`, warn, 0.05 + k * 0.06)}
    <p class="bl"><span>₹${b.left} left</span></p>
  </section>`;
    })
    .join('')}
</div>
${tabs('')}`;

const desk = `
<div class="sheet">
<aside class="sd">
  <div class="mark"><svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true"><rect x="4.5" y="4.5" width="17" height="17"/><path d="M13 0v26M0 13h26"/></svg>Finio</div>
  <button class="addb">${ic('plus', 16, 2)} Add transaction</button>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 17, 1.5)}<em>${nm}</em></span>`).join('')}</nav>
  ${tb('Tools')}
  <div class="tls">${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}</div>
</aside>
<main class="mn">
  <div class="lc">
    <header class="hd"><div class="who"><h1>Hello, ${D.user}</h1><p>Thursday, 15 October 2026</p></div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
    ${hero(600, 72, 38)}
    ${alertN}
    <div class="two">
      ${sits}
      <section class="pn gl">${tb('Savings goals', 'See all')}
        ${D.goals.map((g, k) => `<div class="go"><div class="bh"><b>${g.name}</b><span class="pc">${g.pct}%</span></div>${barDraw(266, g.pct, `₹${g.have}`, `₹${g.target}`, false, 0.25 + k * 0.08)}</div>`).join('')}
      </section>
    </div>
  </div>
  <div class="rc">
    <section class="pn">${tb('Latest', 'See all')}${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date))).join('')}</section>
    <div class="ttl pn" aria-label="Title block">
      <div class="t1"><small>Sheet</small><b>Finio</b></div>
      <div><small>Drawn for</small><b>${D.user}</b></div>
      <div><small>Period</small><b>October 2026</b></div>
      <div><small>Datum</small><b>Day 15 of 31</b></div>
      <div><small>Scale</small><b>0 to ₹${D.budget}</b></div>
      <div class="t6">${ic('shield', 14, 1.6)}Kept only on this device</div>
    </div>
  </div>
</main>
</div>`;

export const screens = { home, txns, add, budgets, desk };

export const css = `
.app{
  --bg:#f6f9fc;--ink:#0d2c6b;--note:#4a5565;--ln:#1b4aa5;--ln2:rgba(27,74,165,.34);
  --g1:rgba(0,130,215,.075);--g2:rgba(0,130,215,.15);
  --red:#c4271b;--pos:#0a6e48;--hi:#fff1a0;--hat:rgba(27,74,165,.55);--selbg:#fff1a0;
  --pbg:#1647a6;--pfg:#fff;
  --mono:"Azeret Mono",ui-monospace,Menlo,monospace;--sans:"Sofia Sans Semi Condensed","Arial Narrow",system-ui,sans-serif;
  --hatch:repeating-linear-gradient(45deg,var(--hat) 0 1px,transparent 1px 5px);
  --tick:linear-gradient(var(--ln),var(--ln));
  background-color:var(--bg);
  background-image:linear-gradient(var(--g2) 1px,transparent 1px),linear-gradient(90deg,var(--g2) 1px,transparent 1px),linear-gradient(var(--g1) 1px,transparent 1px),linear-gradient(90deg,var(--g1) 1px,transparent 1px);
  background-size:40px 40px,40px 40px,8px 8px,8px 8px;background-position:-1px 3px;
  color:var(--ink);font-family:var(--sans);font-size:15px;line-height:1.3;font-variant-numeric:tabular-nums;
}
html[data-mode=dark] .app{
  --bg:#12397a;--ink:#f4f8ff;--note:#c3d7f5;--ln:#dbe8fb;--ln2:rgba(219,232,251,.4);
  --g1:rgba(255,255,255,.045);--g2:rgba(255,255,255,.1);
  --red:#ffa08f;--pos:#a9f2d2;--hi:#ffe98a;--hat:rgba(255,233,138,.7);--selbg:transparent;
  --pbg:#f4f8ff;--pfg:#12397a;
}
.app ::selection{background:var(--hi);color:#0d2c6b}
.app :focus-visible{outline:2px solid var(--hi);outline-offset:2px}
.app h1,.app h2,.app b,.app em,.app i,.app small{font-style:normal;font-weight:inherit;font-size:inherit}
.pg{position:absolute;inset:44px 0 0;padding:4px 20px 0;display:flex;flex-direction:column;gap:11px}
.pg .ac>div{display:flex;align-items:baseline;gap:8px}
.pg .ac{padding:8px 10px}

/* drawn panel with crop marks */
.pn{position:relative;border:1px solid var(--ln);background:var(--bg)}
.pn::before{content:"";position:absolute;inset:-7px;pointer-events:none;
  background:var(--tick) left 0 top 6px/5px 1px,var(--tick) left 6px top 0/1px 5px,var(--tick) right 0 top 6px/5px 1px,var(--tick) right 6px top 0/1px 5px,
  var(--tick) left 0 bottom 6px/5px 1px,var(--tick) left 6px bottom 0/1px 5px,var(--tick) right 0 bottom 6px/5px 1px,var(--tick) right 6px bottom 0/1px 5px;background-repeat:no-repeat}

/* title-block strip */
.tb{display:flex;align-items:stretch;height:28px;border:1px solid var(--ln);background:var(--bg)}
.pn>.tb{border:0;border-bottom:1px solid var(--ln)}
.tb h2{display:flex;align-items:center;padding:0 10px;border-right:1px solid var(--ln);font:600 11.5px/1 var(--mono);letter-spacing:.08em;text-transform:uppercase;white-space:nowrap}
.tb i{flex:1;background:var(--hatch);opacity:.28}
.tb a{display:flex;align-items:center;padding:0 10px;border-left:1px solid var(--ln);font:500 13px/1 var(--sans);white-space:nowrap}

/* header */
.hd{display:flex;align-items:center;gap:10px;height:46px;flex:none}
.hd h1{font:600 24px/1.1 var(--sans);letter-spacing:-.005em;flex:1}
.hd .who{flex:1}.hd .who h1{font-size:22px}
.hd .who p{display:flex;align-items:center;gap:5px;font-size:13px;color:var(--note);margin-top:3px}
.rt{display:flex;gap:8px}
.ib{display:grid;place-items:center;width:36px;height:36px;border:1px solid var(--ln2);background:var(--bg)}
.ib:active{background-image:var(--hatch)}

/* hero */
.hero{display:flex;flex-direction:column}
.co h2{font:600 11.5px/1 var(--mono);letter-spacing:.08em;text-transform:uppercase;color:var(--note)}
.co .fig{font-family:var(--mono);font-weight:600;line-height:1;letter-spacing:-.03em;margin:6px 0 4px -2px}
.draw{display:block;overflow:visible}
.draw text{font:500 13px var(--mono);fill:var(--ink);font-variant-numeric:tabular-nums}
.draw .w{font-family:var(--sans);font-size:13.5px;fill:var(--note)}
.draw .sc{fill:var(--note)}.draw .sc .w{fill:var(--note)}
.draw .dl{fill:none;stroke:var(--ln);stroke-width:1;stroke-dasharray:1;animation:nk-draw .55s cubic-bezier(.16,1,.3,1) var(--d,0s) both}
.draw .ah{fill:var(--ln);animation:nk-in .2s ease-out calc(var(--d,0s) + .4s) both}
.draw .ex{fill:none;stroke:var(--ln2);stroke-width:1}
.draw .bar{fill:none;stroke:var(--ln);stroke-width:1.5}
.draw .tk{stroke:var(--ln);stroke-width:1}
.draw .hp{stroke:var(--ln);stroke-width:1.1;opacity:.8}
.draw .hp.w{stroke:var(--red);opacity:1}
.draw .cl{fill:none;stroke:var(--red);stroke-width:1.2;stroke-dasharray:9 3 2 3}
.draw .dt{fill:var(--red)}
.draw.warn .bar.pt{stroke:var(--red)}
@keyframes nk-draw{from{stroke-dashoffset:1}}
@keyframes nk-in{from{fill-opacity:0}}
.hn{display:flex;justify-content:space-between;gap:10px;font-size:14px;color:var(--note);margin-top:6px;padding-top:6px;border-top:1px solid var(--ln2)}
.hn span:last-child{font:500 13px/1.4 var(--mono);color:var(--ink)}

/* red-pencil revision note */
.rev{display:flex;align-items:center;gap:8px;padding:8px 10px;border:1px solid var(--red);font-size:15px;background:var(--bg)}
.rev .ic{color:var(--red)}
.rev b{font:600 14px var(--mono)}
.rev em{margin-left:auto;color:var(--red);font:600 11.5px/1 var(--mono);letter-spacing:.06em;text-transform:uppercase;white-space:nowrap}

/* accounts */
.ac{display:flex;align-items:center;justify-content:space-between;padding:7px 10px;border-bottom:1px solid var(--ln2)}
.ac:last-child{border-bottom:0}
.ac b{display:block;font-weight:600;font-size:15.5px}
.ac small{display:block;font-size:13px;color:var(--note)}
.v{font:500 15px var(--mono)}
.neg{color:var(--red)}
.ac.tot{border-top:1px solid var(--ln);padding:9px 10px}
.ac.tot b{font:600 11.5px/1 var(--mono);letter-spacing:.08em;text-transform:uppercase}
.ac.tot .v{font-weight:700;font-size:17px}

/* transaction rows */
.rw{display:flex;align-items:center;gap:10px;padding:8px 10px;border-bottom:1px solid var(--ln2)}
.rw:last-child{border-bottom:0}
.nm{flex:1;min-width:0}
.nm b{display:flex;align-items:center;gap:6px;font-weight:600;font-size:15.5px}
.nm b .ic{color:var(--note)}
.nm small{display:flex;flex-wrap:wrap;align-items:center;gap:3px 7px;font-size:13px;color:var(--note);margin-top:2px}
.tg{font:500 10.5px/1 var(--mono);letter-spacing:.05em;text-transform:uppercase;padding:2.5px 5px;border:1px solid var(--ln2);color:var(--ink)}
.tg.d{border-style:dashed}
.a{font:500 15px var(--mono);white-space:nowrap}
.a.inc{color:var(--pos);font-weight:600}

/* segmented switch + month */
.ty{display:grid;grid-auto-flow:column;grid-auto-columns:1fr;border:1px solid var(--ln);height:38px;flex:none;background:var(--bg)}
.ty span{display:grid;place-items:center;border-right:1px solid var(--ln);font-size:14.5px}
.ty span:last-child{border-right:0}
.ty em,.tabs em,.cg em,.ni em{padding:1px 4px}
.ty .on,.cg .on,.tabs .on,.ni.on{background:var(--hatch),var(--selbg)}
.ty .on em,.cg .on em,.tabs .on em,.ni.on em{background:var(--bg);font-weight:700}
.ty span:active{background-image:var(--hatch)}
.mo{display:flex;align-items:center;justify-content:space-between;height:34px;flex:none;border-bottom:1px solid var(--ln2)}
.mo b{font:600 13px var(--mono);letter-spacing:.06em;text-transform:uppercase}
.mo span{display:grid;place-items:center;width:34px;height:34px}

/* tab bar: five ruled cells */
.tabs{position:absolute;left:0;right:0;bottom:0;height:68px;display:grid;grid-template-columns:repeat(5,1fr);border-top:1px solid var(--ln);background:var(--bg);z-index:5}
.tabs span{display:flex;flex-direction:column;align-items:center;gap:4px;padding-top:9px;border-right:1px solid var(--ln2);color:var(--note)}
.tabs span:last-child{border-right:0}
.tabs em{font-size:12px;line-height:1.2}
.tabs .on{color:var(--ink)}
.tabs .on .ic{background:var(--bg);box-shadow:0 0 0 3px var(--bg)}

/* FAB: drafted circle with centre lines */
.fab{position:absolute;right:26px;bottom:88px;width:52px;height:52px;z-index:6}
.fab i{position:relative;z-index:1;display:grid;place-items:center;width:52px;height:52px;border-radius:50%;background:var(--pbg);color:var(--pfg);box-shadow:0 0 0 3px var(--bg),0 0 0 4px var(--ln)}
.fab::before,.fab::after{content:"";position:absolute;background:repeating-linear-gradient(90deg,var(--ln) 0 8px,transparent 8px 11px,var(--ln) 11px 13px,transparent 13px 16px)}
.fab::before{left:-14px;right:-14px;top:50%;height:1px}
.fab::after{top:-14px;bottom:-14px;left:50%;width:1px;background:repeating-linear-gradient(0deg,var(--ln) 0 8px,transparent 8px 11px,var(--ln) 11px 13px,transparent 13px 16px)}
.fab:active i{transform:scale(.95)}

/* add */
.amt{display:flex;justify-content:flex-end;align-items:flex-end;height:76px;flex:none;padding:0 2px 8px;border-bottom:1px solid var(--ln);position:relative}
.amt::after{content:"";position:absolute;left:0;right:0;bottom:-5px;height:4px;background:var(--tick) left/1px 100% no-repeat,var(--tick) right/1px 100% no-repeat}
.amt .fig{font:600 50px/1 var(--mono);letter-spacing:-.03em;display:flex;align-items:center}
.caret{width:2px;height:46px;margin-left:5px;background:var(--red)}
.np{display:grid;grid-template-columns:repeat(3,1fr)}
.np button{height:50px;border:0;border-right:1px solid var(--ln2);border-bottom:1px solid var(--ln2);background:none;font:500 22px var(--mono);display:grid;place-items:center}
.np button:nth-child(3n){border-right:0}.np button:nth-last-child(-n+3){border-bottom:0}
.np button:active{background-image:var(--hatch)}
.fl{display:flex;align-items:center;gap:10px;height:46px;padding-right:10px}
.fl small{align-self:stretch;display:flex;align-items:center;padding:0 10px;border-right:1px solid var(--ln);font:600 11.5px/1 var(--mono);letter-spacing:.08em;text-transform:uppercase}
.fl b{font-weight:600;font-size:15.5px;flex:1}
.fl span{font-size:13.5px;color:var(--note)}
.cg{display:grid;grid-template-columns:repeat(4,1fr)}
.cg span{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:5px;height:60px;border-right:1px solid var(--ln2);border-bottom:1px solid var(--ln2);font-size:13px}
.cg span:nth-child(4n){border-right:0}.cg span:nth-last-child(-n+4){border-bottom:0}
.cg .on .ic{background:var(--bg);box-shadow:0 0 0 3px var(--bg)}
.cta{position:absolute;left:20px;right:20px;bottom:26px}
.cta button,.app .addb{width:100%;height:52px;border:0;background:var(--pbg);color:var(--pfg);font:700 13.5px var(--mono);letter-spacing:.08em;text-transform:uppercase;display:flex;align-items:center;justify-content:center;gap:8px;box-shadow:0 0 0 3px var(--bg),0 0 0 4px var(--ln)}
.cta button:active,.app .addb:active{background-image:repeating-linear-gradient(45deg,var(--pfg) 0 1px,transparent 1px 5px)}

/* budgets */
.per a{font:500 13px var(--mono)}
.bd{padding-bottom:11px;border-bottom:1px solid var(--ln2)}
.bd:last-of-type{border-bottom:0}
.bh{display:flex;align-items:center;gap:10px;height:32px;margin-bottom:2px}
.bh b{font-weight:600;font-size:17px;flex:1}
.pc{font:700 17px var(--mono)}
.ok{font:500 11.5px/1 var(--mono);letter-spacing:.06em;text-transform:uppercase;padding:5px 7px;border:1px solid var(--ln2);color:var(--note)}
.warn .pc{color:var(--red)}
.cloud{position:relative;display:grid;place-items:center;width:112px;height:32px;margin:0 -4px 0 -2px}
.cloud svg{position:absolute;inset:0}
.cloud path{fill:none;stroke:var(--red);stroke-width:1.4}
.cloud b{position:relative;font:700 13px var(--sans);color:var(--red);flex:none}
.bl{display:flex;justify-content:flex-end;font:500 13px var(--mono);color:var(--note);margin-top:-1px}
.warn .bl{color:var(--red)}

/* desktop: one framed sheet */
.sheet{position:absolute;inset:12px;border:1.5px solid var(--ln);outline:1px solid var(--ln2);outline-offset:4px;display:flex}
.sd{width:212px;flex:none;border-right:1px solid var(--ln);padding:18px 16px;display:flex;flex-direction:column;gap:14px;background:var(--bg)}
.mark{display:flex;align-items:center;gap:10px;font:700 22px var(--mono);letter-spacing:-.02em}
.mark svg{fill:none;stroke:var(--ln);stroke-width:1}
.mark rect{stroke-width:1.5}
.app .addb{height:42px;font-size:12px;margin:4px 0 2px}
.sd nav{display:flex;flex-direction:column;border:1px solid var(--ln)}
.ni{display:flex;align-items:center;gap:10px;height:38px;padding:0 10px;border-bottom:1px solid var(--ln2);font-size:15px}
.ni:last-child{border-bottom:0}
.ni.on .ic{background:var(--bg);box-shadow:0 0 0 3px var(--bg)}
.tls{display:flex;flex-direction:column;margin-top:-14px;border:1px solid var(--ln);border-top:0}
.tl{height:31px;display:flex;align-items:center;padding:0 10px;border-bottom:1px solid var(--ln2);font-size:14.5px}
.tl:last-child{border-bottom:0}
.mn{flex:1;display:flex;gap:28px;padding:16px 24px 22px}
.lc{width:600px;flex:none;display:flex;flex-direction:column;gap:22px}
.lc .ac{padding:10px}
.lc .hd{height:44px}
.two{display:grid;grid-template-columns:1fr 288px;gap:24px;align-items:stretch}
.gl .go{padding:9px 10px 12px;border-bottom:1px solid var(--ln2)}
.gl .go:last-child{border-bottom:0}
.gl .bh{height:26px}.gl .bh b{font-size:15.5px}.gl .pc{font-size:15px}
.rc{flex:1;min-width:0;display:flex;flex-direction:column;justify-content:space-between;gap:16px;padding-top:4px}
.rc .rw{padding:10px}
.ttl{display:grid;grid-template-columns:1fr 1fr}
.ttl div{padding:7px 10px;border-right:1px solid var(--ln2);border-bottom:1px solid var(--ln2)}
.ttl div:nth-child(odd){border-right:0}
.ttl small{display:block;font:500 10.5px/1 var(--mono);letter-spacing:.08em;text-transform:uppercase;color:var(--note);margin-bottom:4px}
.ttl b{font:600 14px var(--mono)}
.ttl .t1{grid-column:1/-1;border-right:0;border-bottom:1px solid var(--ln);display:flex;align-items:baseline;justify-content:space-between}
.ttl .t1 b{font-size:22px;font-weight:700;letter-spacing:-.02em}
.ttl .t6{grid-column:1/-1;border:0;border-top:1px solid var(--ln);margin-top:-1px;display:flex;align-items:center;gap:7px;font-size:14px}
`;
