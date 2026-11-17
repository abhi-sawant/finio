import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 20,
  slug: 'takhti',
  name: 'Takhti',
  defaultMode: 'dark',
  tagline: 'The writing board — the classroom blackboard and the shop’s price slate. Money worked out by hand, in chalk, where you can see the working; for people who want their sums human and informal.',
  blurb: 'Blackboard and chalk (whiteboard and marker by day): the hero writes out the real sum, days are tallied in gates of five, and budgets are hand-hatched bars.',
  fonts: 'https://fonts.googleapis.com/css2?family=Kalam:wght@400;700&family=Nunito+Sans:opsz,wght@6..12,600;6..12,700&display=swap',
  swatch: ['#f4f7f9', '#15171c', '#1650cf', '#cf222e'],
  swatchDark: ['#1d2b25', '#f2efe6', '#f7d75e', '#ff93a3'],
  notes: [
    ['Shape', 'No cards. Rough outlines only where something is boxed (alert, keys, fields, “Near limit”): 1.6px strokes wobbled by an SVG turbulence filter, with chalk grain in dark and a clean marker line in light. Text is never filtered.'],
    ['Type', 'Kalam 400/700 for titles, labels, names and the big sums (68–96px answer, keypad); Nunito Sans 600/700 with tabular numerals for every list and table figure.'],
    ['Density', 'Medium-airy. Sections sit open on the board, separated by single hand-ruled lines; rows are 54–64px and everything stays on a straight left edge.'],
    ['Motion', 'On load the double underline under ₹1,562 scribbles itself, then the ring around today’s tally stroke is drawn — two stroke-dashoffset draws, about 0.9s in all. Nothing else moves.'],
    ['Voice', 'A teacher’s working, only the real arithmetic: “₹40,000 budget − ₹15,000 spent = ₹25,000 left”, “₹25,000 ÷ 16 days =”. Product labels stay verbatim.'],
    ['Signature', 'The working shown: the sum written out to its double-underlined answer, beside a 31-stroke gate-of-five tally of the month with day 15 ringed; budgets and goals as diagonal-hatched bars with the percentage written at the end.'],
  ],
};

/* ---------- helpers ---------- */
const r = (v, sign = '') => `<span class="n">${sign}₹${v}</span>`;

// hand-drawn ring that overshoots its start
const hc = (cx, cy, rx, ry) => {
  const p = (x, y) => `${(cx + x * rx).toFixed(1)} ${(cy + y * ry).toFixed(1)}`;
  return `M${p(-0.55, -0.85)}C${p(0.1, -1.15)} ${p(1, -0.8)} ${p(1, 0)}C${p(1, 0.8)} ${p(0.3, 1.05)} ${p(-0.3, 0.95)}C${p(-0.9, 0.85)} ${p(-1.05, 0.2)} ${p(-0.9, -0.35)}C${p(-0.75, -0.85)} ${p(-0.2, -1.08)} ${p(0.5, -0.92)}`;
};
const ring = (w, h) =>
  `<svg class="hc" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true"><path d="${hc(w / 2, h / 2, w / 2 - 3, h / 2 - 3)}"/></svg>`;
// double scribble underline
const dblD = (w) => `M3 4C${(w * 0.3).toFixed(0)} 1 ${(w * 0.7).toFixed(0)} 8 ${w - 3} 3.5C${(w * 0.7).toFixed(0)} 9 ${(w * 0.3).toFixed(0)} 11 7 11`;
const dbl = (w, cls = '') =>
  `<svg class="dbl ${cls}" width="${w}" height="14" viewBox="0 0 ${w} 14" aria-hidden="true"><path d="${dblD(w)}" pathLength="1"/></svg>`;
const excl = `<svg class="ex" width="16" height="18" viewBox="0 0 16 18" aria-hidden="true"><path d="M8.3 2.2C7.7 5.4 8.4 8 7.9 11.2"/><path d="M7.9 15.4l.2.2"/></svg>`;

const defs = `<svg class="tkd" width="0" height="0" aria-hidden="true" focusable="false"><defs>
${[['tk-chalk', 4, 9, 3], ['tk-chalk2', 11, 3, 3.4]]
  .map(
    ([id, s1, s2, sc]) => `<filter id="${id}" x="-6%" y="-30%" width="112%" height="160%"><feTurbulence type="fractalNoise" baseFrequency=".035" numOctaves="2" seed="${s1}" result="w"/><feDisplacementMap in="SourceGraphic" in2="w" scale="${sc}" xChannelSelector="R" yChannelSelector="G" result="d"/><feTurbulence type="fractalNoise" baseFrequency=".85" numOctaves="1" seed="${s2}" result="g"/><feColorMatrix in="g" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 -2.4 1.85" result="m"/><feComposite in="d" in2="m" operator="in"/></filter>`,
  )
  .join('')}
${[['tk-marker', 5, 2.2], ['tk-marker2', 13, 2.6]]
  .map(
    ([id, s1, sc]) => `<filter id="${id}" x="-6%" y="-30%" width="112%" height="160%"><feTurbulence type="fractalNoise" baseFrequency=".03" numOctaves="2" seed="${s1}" result="w"/><feDisplacementMap in="SourceGraphic" in2="w" scale="${sc}" xChannelSelector="R" yChannelSelector="G"/></filter>`,
  )
  .join('')}
</defs></svg>`;

const tally = () => {
  const J = [0, 0.9, -0.7, 0.5, -1, 0.4, 0.8, -0.5];
  let s = '';
  for (let g = 0; g < 7; g++) {
    const gx = 12 + g * 44;
    const cnt = g < 6 ? 5 : 1;
    for (let k = 0; k < Math.min(cnt, 4); k++) {
      const day = g * 5 + k + 1;
      const x = gx + k * 8;
      s += `<path class="${day <= 15 ? 'go' : 'to'}" d="M${(x + J[day % 8]).toFixed(1)} ${(9 + J[(day + 2) % 8]).toFixed(1)}L${(x - J[(day + 3) % 8]).toFixed(1)} ${(37 + J[(day + 5) % 8]).toFixed(1)}"/>`;
    }
    if (cnt === 5) {
      const day = g * 5 + 5;
      s += `<path class="${day < 15 ? 'go' : day === 15 ? 'td' : 'to'}" d="M${gx - 6} ${31 + J[g]}L${gx + 30} ${14 - J[g + 1]}"/>`;
    }
  }
  return `<svg class="tally" viewBox="0 0 292 46" role="img" aria-label="Day 15 of 31: 15 days gone, 16 to go">${s}<path class="tring" pathLength="1" d="${hc(12 + 2 * 44 + 12, 23, 25, 21)}"/></svg>`;
};

const ib = (i, label) => `<span class="ib" role="button" aria-label="${label}">${ic(i, 21, 1.9)}</span>`;
const ledge = `<i class="ck a" aria-hidden="true"></i><i class="ck b" aria-hidden="true"></i>`;
const tabs = (active) =>
  `<nav class="tabs">${ledge}${D.nav
    .map(([nm, i]) => `<span class="tb${nm === active ? ' on' : ''}"><span class="tic">${ic(i, 21, 1.9)}${nm === active ? ring(46, 38) : ''}</span><em>${nm}</em></span>`)
    .join('')}</nav>`;
const fab = `<span class="fab" role="button" aria-label="Add transaction">${ic('plus', 26, 2.4)}</span>`;
const sh = (t, a = 'See all') => `<div class="sh"><h2>${t}</h2>${a ? `<a>${a}</a>` : ''}</div>`;

const working = (big = false) => `<section class="wk${big ? ' big' : ''}">
  <p class="w1">₹${D.budget} budget − ₹${D.spent} spent = <b>₹${D.left} left</b></p>
  <p class="w2">₹${D.left} ÷ ${D.days} days =</p>
  <div class="ans"><div class="fig"><strong>₹${D.safe}</strong>${dbl(big ? 300 : 216, 'draw')}</div><h2>Safe to spend today</h2></div>
</section>`;
const month = `<figure class="tl">${tally()}<figcaption><span>Day 15 of 31</span><span>${D.days} days to go</span><span>${D.pct}% spent</span></figcaption></figure>`;
const alertB = `<p class="al">${excl}<span>${D.due.name} payment of ${r(D.due.amt)} due in ${D.due.days} days</span></p>`;

const accounts = `<div class="ac">${D.accounts
  .map(
    (a) =>
      `<div class="ar"><div><b>${a.name}</b><small>${a.type}${a.owed ? ' · owed' : ''}</small></div><span class="n v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`,
  )
  .join('')}
  <div class="at"><b>Total balance</b><span class="tv"><span class="n">₹${D.total}</span>${dbl(104)}</span></div>
</div>`;

const row = (x, date = '') =>
  `<div class="rw"><span class="ri">${ic(x.icon, 20, 1.8)}</span><div class="nm"><b>${x.name}${x.rec ? ic('repeat', 13, 2) : ''}</b><small>${date ? `${date} · ` : ''}${x.cat} · ${x.acct}${
    x.tag ? `<span class="tg">${x.tag}</span>` : ''
  }</small></div><span class="n a${x.inc ? ' inc' : ''}">${x.inc ? '+' : '−'}₹${x.amt}</span></div>`;

const hd = (title, l = '', rt = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${rt}</div></header>`;
const DATE = 'Thursday, 15 October 2026';

/* ---------- screens ---------- */
const home = `${defs}
<div class="pg">
  <header class="hi"><div><h1>Hello, ${D.user}</h1><p class="pv">${ic('shield', 13, 1.9)}Kept only on this device</p></div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  <p class="dt">${DATE}</p>
  ${working()}
  ${month}
  ${alertB}
  ${sh('Where it sits')}
  ${accounts}
  ${sh('Latest')}
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on">All${dbl(34)}</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span class="mb" role="button" aria-label="Previous month">${ic('chevl', 17, 2)}</span><b>October 2026</b><span class="mb" role="button" aria-label="Next month">${ic('chevr', 17, 2)}</span></div>
  ${D.groups.map((g) => `<div class="dl">${g.date}</div>${g.rows.map((x) => row(x)).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg pa">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty t3"><span class="on">Expense${dbl(74)}</span><span>Income</span><span>Transfer</span></div>
  <div class="amt"><span class="fig">₹2,450</span><i class="caret"></i></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 1.9)]
    .map((k) => `<button type="button"${k.length > 1 ? ' aria-label="Delete"' : ''}><span>${k}</span></button>`)
    .join('')}</div>
  <div class="fl"><small>Account</small><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 17, 2)}</div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}><span class="ci">${ic(i, 22, 1.8)}${k === 0 ? ring(50, 42) : ''}</span>${nm}</span>`).join('')}</div>
</div>
<div class="cta">${ledge}<button type="button"><span>Add transaction</span></button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><span>Monthly · October 2026</span><span>Day 15 of 31</span></div>
  ${D.budgets
    .map((b) => {
      const w = b.pct > 85;
      return `<section class="bg${w ? ' w' : ''}">
    <div class="bh"><h3>${b.name}</h3><span class="bs ${w ? 'wn' : 'ok'}">${w ? excl : ic('check', 14, 2.4)}${b.state}</span></div>
    <div class="bb"><div class="hb"><i style="width:${b.pct}%"></i></div><b>${b.pct}%</b></div>
    <p class="bw">${r(b.limit)} limit − ${r(b.spent)} spent = <strong>${r(b.left)} left</strong></p>
  </section>`;
    })
    .join('')}
</div>
${tabs('')}`;

const desk = `
<aside class="sd">
  <div class="mark">Finio</div>
  <button type="button" class="addb"><span>${ic('plus', 18, 2.4)}Add transaction</span></button>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 19, 1.9)}${nm}${nm === 'Home' ? ring(112, 40) : ''}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl2">${t}</span>`).join('')}
</aside>
<i class="vr" aria-hidden="true"></i>
<main class="mn">
  <header class="hi"><div><h1>Hello, ${D.user}</h1><p class="pv">${ic('shield', 13, 1.9)}Kept only on this device</p></div><div class="rt"><p class="dt">${DATE}</p>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  <div class="r1">
    ${working(true)}
    <div class="side">${month}${alertB}</div>
  </div>
  <div class="r2">
    <div>${sh('Where it sits')}${accounts}</div>
    <div>${sh('Savings goals')}
      ${D.goals
        .map(
          (g) =>
            `<section class="bg gg"><div class="bh"><h3>${g.name}</h3></div><div class="bb"><div class="hb"><i style="width:${g.pct}%"></i></div><b>${g.pct}%</b></div><p class="bw">${r(g.have)} of ${r(g.target)}</p></section>`,
        )
        .join('')}
    </div>
    <div>${sh('Latest')}${D.groups.flatMap((g) => g.rows.map((x) => row(x, g.date.split(', ')[1]))).join('')}</div>
  </div>
</main>`;

const grain = (rgb, a) =>
  `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='240' height='240'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.75' numOctaves='2' stitchTiles='stitch'/%3E%3CfeColorMatrix values='0 0 0 0 ${rgb} 0 0 0 0 ${rgb} 0 0 0 0 ${rgb} 0 0 0 ${a} 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")`;

export const css = `
.app{--bd:#f4f7f9;--bd2:#e6eaee;--ink:#15171c;--mut:#515865;--line:rgba(21,23,28,.72);--faint:rgba(21,23,28,.2);--hi:#1650cf;--on-hi:#ffffff;--owe:#cf222e;--link:#1650cf;--pos:#0f7a43;
  --rough:url(#tk-marker);--rough2:url(#tk-marker2);--sw:2.1px;
  --ledge:linear-gradient(#fbfcfd,#cdd2d8 45%,#989fa8);--ledge-sh:rgba(40,50,70,.22);
  --hand:'Kalam',cursive;--num:'Nunito Sans',system-ui,sans-serif;
  color:var(--ink);font-family:var(--hand);font-size:15px;line-height:1.3;caret-color:var(--hi);
  background:
    linear-gradient(112deg,rgba(255,255,255,.95) 0 9%,rgba(255,255,255,0) 22% 62%,rgba(255,255,255,.7) 74%,rgba(255,255,255,0) 86%),
    radial-gradient(60% 22% at 30% 18%,rgba(60,80,120,.06),transparent 70%),
    radial-gradient(50% 18% at 72% 52%,rgba(60,80,120,.05),transparent 70%),
    radial-gradient(70% 20% at 35% 84%,rgba(60,80,120,.055),transparent 70%),
    ${grain(0, 0.035)},
    var(--bd)}
html[data-mode=dark] .app{--bd:#1d2b25;--bd2:#15201b;--ink:#f2efe6;--mut:#aebcb2;--line:rgba(242,239,230,.7);--faint:rgba(242,239,230,.3);--hi:#f7d75e;--on-hi:#1a2620;--owe:#ff93a3;--link:#93d0f5;--pos:#a5e59a;
  --rough:url(#tk-chalk);--rough2:url(#tk-chalk2);--sw:2.3px;
  --ledge:linear-gradient(#9a6c40,#6f4c2b 45%,#432d19);--ledge-sh:rgba(0,0,0,.45);
  background:
    linear-gradient(104deg,transparent 0 24%,rgba(255,255,255,.035) 38%,transparent 52% 70%,rgba(255,255,255,.025) 82%,transparent 94%),
    radial-gradient(70% 26% at 26% 16%,rgba(255,255,255,.07),transparent 70%),
    radial-gradient(60% 22% at 78% 48%,rgba(255,255,255,.05),transparent 70%),
    radial-gradient(80% 24% at 36% 86%,rgba(255,255,255,.055),transparent 70%),
    ${grain(1, 0.1)},
    var(--bd)}
.app ::selection{background:var(--hi);color:var(--on-hi)}
.app :focus-visible{outline:2px dashed var(--hi);outline-offset:3px}
.app .tkd{position:absolute}
.app .n{font-family:var(--num);font-weight:600;font-variant-numeric:tabular-nums lining-nums;letter-spacing:.005em}
.app .pg{position:absolute;inset:44px 0 72px;padding:0 20px;overflow:hidden}
.app .pg.pa{bottom:96px}
.ib{width:38px;height:38px;display:grid;place-items:center;flex:none;border-radius:50%}
.ib:active{background:var(--faint)}

/* strokes drawn by hand */
.hc,.dbl,.ex,.tally{overflow:visible;fill:none;stroke-linecap:round;stroke-linejoin:round;filter:var(--rough)}
.hc path{stroke:var(--hi);stroke-width:var(--sw)}
.hc{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);pointer-events:none}
.dbl path{stroke:var(--hi);stroke-width:var(--sw)}
.ex path{stroke:currentColor;stroke-width:2.8px}
.ex{filter:none;flex:none}

/* home header */
.hi{display:flex;justify-content:space-between;align-items:flex-start;padding-top:6px}
.hi h1{font-size:25px;font-weight:700;line-height:1.15}
.pv{display:flex;align-items:center;gap:5px;font-size:13.5px;color:var(--mut);line-height:1.2}
.hi .rt{display:flex;align-items:center;margin-right:-8px}
.dt{font-size:13.5px;color:var(--mut);text-align:right;margin-top:-2px;line-height:1.2}

/* the working */
.wk{margin-top:6px}
.w1{font-size:15px;line-height:1.35;white-space:nowrap}
.w1 b{font-weight:700}
.w2{font-size:20px;font-weight:700;line-height:1.3;margin-top:2px}
.ans{display:flex;align-items:flex-end;gap:14px;margin-top:0}
.ans .fig{position:relative;padding-bottom:9px;flex:none}
.ans strong{display:block;font-size:66px;font-weight:700;line-height:1.02;letter-spacing:-.01em}
.ans .dbl{position:absolute;left:2px;bottom:0}
.ans h2{font-size:18px;font-weight:700;line-height:1.15;color:var(--hi);padding-bottom:20px}
.dbl.draw path{stroke-dasharray:1;animation:tkdraw .5s cubic-bezier(.2,.7,.2,1) .25s backwards}
@keyframes tkdraw{from{stroke-dashoffset:1}to{stroke-dashoffset:0}}

/* tally of the month */
.tl{margin:10px 0 0}
.tally{display:block;width:100%;height:auto;max-width:330px}
.tally path{stroke-width:2.5px}
.tally .go{stroke:var(--ink)}
.tally .to{stroke:var(--faint)}
.tally .td{stroke:var(--hi);stroke-width:3px}
.tally .tring{stroke:var(--hi);stroke-width:2.2px;stroke-dasharray:1;animation:tkdraw .45s cubic-bezier(.2,.7,.2,1) .75s backwards}
.tl figcaption{display:flex;gap:6px 16px;flex-wrap:wrap;font-size:14px;color:var(--mut);margin:3px 0 0;line-height:1.3}
.tl figcaption span:first-child{color:var(--hi);font-weight:700}

/* alert, boxed in pink chalk */
.al{position:relative;display:flex;align-items:center;gap:9px;color:var(--owe);padding:9px 12px 8px;margin-top:12px;font-size:15px;line-height:1.25}
.al::before{content:'';position:absolute;inset:0;border:1.7px solid var(--owe);border-radius:4px 7px 5px 8px;filter:var(--rough);pointer-events:none}
.al span{color:var(--ink)}
.al .n{font-weight:700}

/* section heads: a title on one hand-ruled line */
.sh{position:relative;display:flex;justify-content:space-between;align-items:baseline;margin-top:15px;padding-bottom:3px}
.sh::after,.ar::after,.rw::after,.hd::after,.mo::after,.bg::after,.gl::after{content:'';position:absolute;left:0;right:0;bottom:-5px;height:9px;background:linear-gradient(var(--line),var(--line)) center/100% 1.5px no-repeat;filter:var(--rough);pointer-events:none}
.sh h2{font-size:19px;font-weight:700;line-height:1.2}
.sh a{font-size:14.5px;color:var(--link);text-decoration:underline;text-decoration-thickness:1.5px;text-underline-offset:3px}

/* accounts: a column of figures and its sum */
.ar{position:relative;display:flex;justify-content:space-between;align-items:center;gap:10px;padding:7px 0 5px}
.ar::after{background-image:linear-gradient(var(--faint),var(--faint))}
.ar:nth-child(even)::after,.rw:nth-of-type(even)::after{filter:var(--rough2)}
.ar b{display:block;font-size:16.5px;font-weight:700;line-height:1.2;color:var(--link)}
.ar small{display:block;font-size:13px;color:var(--mut);line-height:1.2}
.ar .v{font-size:16px;font-weight:700}
.ar .v.neg{color:var(--owe)}
.ar:nth-last-child(2)::after{background-image:linear-gradient(var(--line),var(--line))}
.at{display:flex;justify-content:space-between;align-items:flex-start;padding:9px 0 0}
.at b{font-size:16.5px;font-weight:700}
.tv{position:relative;display:block;padding-bottom:12px}
.tv .n{font-size:18px;font-weight:700;display:block;line-height:1.2}
.tv .dbl{position:absolute;right:-3px;bottom:0}
.tv .dbl path{stroke:var(--ink)}

/* transaction rows */
.rw{position:relative;display:flex;align-items:center;gap:11px;padding:9px 0 8px}
.rw::after{background-image:linear-gradient(var(--faint),var(--faint))}
.ri{flex:none;width:26px;display:grid;place-items:center;color:var(--mut)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-size:16.5px;font-weight:700;line-height:1.2}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:block;font-size:13px;color:var(--mut);line-height:1.35}
.tg{display:inline-block;margin-left:7px;padding:0 6px;font-size:12.5px;line-height:1.3;color:var(--ink);border:1.2px solid var(--faint);border-radius:3px 7px 4px 8px/7px 3px 8px 4px}
.rw .a{font-size:16px;font-weight:700;white-space:nowrap}
.rw .a.inc{color:var(--pos)}

/* ledge + tab bar */
.tabs,.cta{position:absolute;left:0;right:0;bottom:0;background:var(--bd2);z-index:6}
.tabs{height:72px;display:flex;padding:8px 6px 0}
.tabs::before,.cta::before{content:'';position:absolute;left:0;right:0;top:0;height:8px;background:var(--ledge);box-shadow:0 3px 5px -1px var(--ledge-sh)}
.ck{position:absolute;top:-5px;height:5px;border-radius:2px}
.ck.a{left:24px;width:19px;background:#f2efe6}
.ck.b{left:49px;width:12px;background:#f7d75e}
html[data-mode=light] .app .ck.a{width:27px;background:#22252b;border-radius:2px 0 0 2px}
html[data-mode=light] .app .ck.b{left:51px;width:10px;background:#1650cf;border-radius:0 2px 2px 0}
.tb{flex:1;display:flex;flex-direction:column;align-items:center;gap:0;padding-top:5px;color:var(--mut)}
.tic{position:relative;width:46px;height:34px;display:grid;place-items:center}
.tb em{font-style:normal;font-size:12.5px;line-height:1.2}
.tb.on{color:var(--hi)}
.tb.on em{font-weight:700}
.tb:active .tic{background:var(--faint);border-radius:50%}
.fab{position:absolute;right:20px;bottom:86px;width:58px;height:58px;display:grid;place-items:center;color:var(--on-hi);z-index:7}
.fab::before{content:'';position:absolute;inset:0;background:var(--hi);border-radius:52% 48% 50% 50%/49% 52% 48% 51%;filter:var(--rough);box-shadow:0 4px 10px -2px var(--ledge-sh)}
.fab .ic{position:relative}
.fab:active{transform:scale(.95)}

/* page headers */
.hd{position:relative;display:flex;align-items:center;gap:2px;padding:6px 0 4px}
.hd h1{flex:1;font-size:28px;font-weight:700;line-height:1.2}
.hd .ib:first-child{margin-left:-9px}
.hd .rt{display:flex;margin-right:-8px}
.ty{display:flex;gap:26px;padding:16px 0 12px}
.ty.t3{justify-content:space-between;padding:16px 26px 8px}
.ty span{position:relative;font-size:17px;color:var(--mut);line-height:1.2}
.ty span.on{color:var(--ink);font-weight:700}
.ty .dbl{position:absolute;left:50%;transform:translateX(-50%);bottom:-13px}
.mo{position:relative;display:flex;justify-content:space-between;align-items:center;padding:8px 0 9px}
.mo b{font-size:18px;font-weight:700}
.mb{position:relative;width:32px;height:30px;display:grid;place-items:center}
.mb::before{content:'';position:absolute;inset:0;border:1.6px solid var(--line);border-radius:4px 6px 5px 7px;filter:var(--rough)}
.mb:active{background:var(--faint)}
.dl{font-size:14.5px;color:var(--mut);margin:15px 0 0;line-height:1.2}

/* add */
.amt{display:flex;justify-content:center;align-items:center;gap:6px;padding:8px 0 0}
.amt .fig{font-size:60px;font-weight:700;line-height:1.1}
.caret{width:3px;height:50px;border-radius:2px;background:var(--hi);margin-bottom:8px;animation:tkbl 1.1s steps(1) infinite}
@keyframes tkbl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:9px 11px;margin-top:4px}
.np button{position:relative;height:47px;border:0;background:none;display:grid;place-items:center;font-family:var(--hand)!important;font-size:26px;font-weight:700;line-height:1}
.np button::before{content:'';position:absolute;inset:0;border:1.7px solid var(--line);border-radius:5px 8px 6px 9px;filter:var(--rough)}
.np button:nth-child(even)::before{filter:var(--rough2)}
.np button::after{content:'';position:absolute;inset:5px;opacity:0;background:repeating-linear-gradient(-56deg,var(--hi) 0 1.8px,transparent 1.8px 5px);filter:var(--rough)}
.np button:active::after{opacity:.75}
.np button span{position:relative;z-index:1;display:grid;place-items:center;padding-top:5px}
.fl{position:relative;display:flex;align-items:baseline;gap:9px;padding:11px 13px 9px;margin-top:13px}
.fl::before{content:'';position:absolute;inset:0;border:1.7px solid var(--line);border-radius:5px 8px 6px 9px;filter:var(--rough2);pointer-events:none}
.fl small{font-size:14px;color:var(--mut)}
.fl b{flex:1;font-size:17px;font-weight:700;color:var(--link)}
.fl span{font-size:13.5px;color:var(--mut)}
.fl .ic{align-self:center;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:8px 0;margin-top:11px}
.cg>span{display:flex;flex-direction:column;align-items:center;gap:0;font-size:13.5px;color:var(--mut);line-height:1.2}
.ci{position:relative;width:50px;height:42px;display:grid;place-items:center;color:var(--ink)}
.cg>span.on{color:var(--hi);font-weight:700}
.cg>span.on .ci{color:var(--hi)}
.cg>span:active .ci{background:var(--faint);border-radius:50%}
.cta{padding:20px 20px 24px}
.cta button,.addb{position:relative;width:100%;height:52px;border:0;background:none;display:grid;place-items:center}
.cta button::before,.addb::before{content:'';position:absolute;inset:0;background:var(--hi);border-radius:6px 9px 7px 10px;filter:var(--rough)}
.cta button span,.addb span{position:relative;font-family:var(--hand);font-size:20px;font-weight:700;color:var(--on-hi);padding-top:3px;display:flex;align-items:center;gap:8px}
.cta button:active,.addb:active{transform:translateY(1px) scale(.99)}

/* budgets: hatched by hand */
.per{display:flex;justify-content:space-between;font-size:15px;color:var(--mut);padding:13px 0 0}
.per span:last-child{color:var(--hi);font-weight:700}
.bg{position:relative;padding:17px 0 15px}
.bg::after{background-image:linear-gradient(var(--faint),var(--faint))}
.bg:last-of-type::after{display:none}
.bh{display:flex;justify-content:space-between;align-items:center;min-height:30px}
.bh h3{font-size:19px;font-weight:700;line-height:1.2}
.bs{position:relative;display:flex;align-items:center;gap:5px;font-size:15px;font-weight:700;line-height:1.2}
.bs.ok{color:var(--pos)}
.bs.wn{color:var(--owe);padding:5px 10px 3px 7px}
.bs.wn::before{content:'';position:absolute;inset:0;border:1.8px solid var(--owe);border-radius:4px 7px 5px 8px;filter:var(--rough)}
.bb{display:grid;grid-template-columns:1fr 50px;align-items:center;gap:8px;margin-top:7px}
.hb{position:relative;height:22px}
.hb::before{content:'';position:absolute;inset:0;border:1.7px solid var(--line);border-radius:3px 5px 4px 6px;filter:var(--rough)}
.hb i{position:absolute;left:3px;top:3px;bottom:3px;max-width:calc(100% - 6px);background:repeating-linear-gradient(-56deg,var(--ink) 0 1.9px,transparent 1.9px 5.6px);filter:var(--rough2)}
.bg.w .hb i{background-image:repeating-linear-gradient(-56deg,var(--owe) 0 2.1px,transparent 2.1px 4.6px)}
.bg.w .hb::before{border-color:var(--owe)}
.bb b{font-size:21px;font-weight:700;text-align:right;line-height:1;padding-top:4px}
.bg.w .bb b{color:var(--owe)}
.bw{font-size:14.5px;color:var(--mut);margin-top:6px;line-height:1.3}
.bw .n{color:var(--ink);font-size:14px}
.bw strong{font-weight:700;color:var(--ink)}
.bw strong .n{font-weight:700}

/* desktop: the board wide */
.sd{position:absolute;left:0;top:0;bottom:0;width:214px;padding:22px 20px 0 26px;display:flex;flex-direction:column}
.mark{font-size:34px;font-weight:700;line-height:1.1;margin-bottom:14px}
.addb{height:44px;margin-bottom:16px}
.addb span{font-size:16.5px}
.sd nav{display:flex;flex-direction:column;gap:2px}
.ni{position:relative;display:flex;align-items:center;gap:11px;height:40px;width:112px;padding-left:12px;font-size:17px;color:var(--mut);white-space:nowrap}
.ni.on{color:var(--hi);font-weight:700}
.gl{position:relative;font-size:17px;font-weight:700;margin:20px 0 8px;padding-bottom:2px}
.tl2{font-size:15.5px;line-height:1.2;padding:5px 0 4px 12px;color:var(--ink)}
.vr{position:absolute;left:204px;top:14px;bottom:14px;width:20px;background:linear-gradient(var(--line),var(--line)) center/1.6px 100% no-repeat;filter:var(--rough)}
.mn{position:absolute;left:236px;right:0;top:0;bottom:0;padding:16px 34px 0 14px;overflow:hidden}
.mn .hi{padding-top:0}
.mn .hi h1{font-size:27px}
.mn .hi .rt{gap:2px}
.mn .dt{margin:0 12px 0 0;font-size:15px}
.r1{display:grid;grid-template-columns:1.32fr 1fr;gap:44px;align-items:start;margin-top:14px}
.wk.big{margin:0}
.wk.big .w1{font-size:22px}
.wk.big .w2{font-size:30px;margin-top:6px}
.wk.big .ans{gap:24px;margin-top:2px}
.wk.big .ans strong{font-size:96px}
.wk.big .ans .fig{padding-bottom:12px}
.wk.big .ans .dbl{left:4px;transform:scaleY(1.25);transform-origin:left bottom}
.wk.big .ans .dbl path{stroke-width:2.6px}
.wk.big .ans h2{font-size:26px;padding-bottom:34px}
.side .tl{margin-top:8px}
.side .tally{max-width:none}
.side .tl figcaption{font-size:16px;margin-top:8px}
.side .al{margin-top:18px;font-size:16px;padding:12px 14px 10px}
.r2{display:grid;grid-template-columns:1fr 1fr 1.14fr;gap:40px;margin-top:10px}
.r2 .sh{margin-top:0;margin-bottom:6px}
.r2 .ar{padding:12px 0 10px}
.r2 .at{padding-top:14px}
.r2 .rw{padding:10px 0 9px}
.bg.gg{padding:16px 0 15px}
`;

export const screens = { home, txns, add, budgets, desk };
