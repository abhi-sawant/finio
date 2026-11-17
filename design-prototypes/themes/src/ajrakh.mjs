import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 14,
  slug: 'ajrakh',
  name: 'Ajrakh',
  tagline: 'The hand block-printed cloth of Kutch and Sindh. For people who want their money app to feel made by hand — rich and warm, with pattern doing the decorating so the numbers can stay plain.',
  blurb: 'Block-printed cloth. Cotton panels on a madder ground by day, an indigo vat by night, printed borders, and budgets stamped out block by block.',
  fonts: 'https://fonts.googleapis.com/css2?family=Hind:wght@400;500;600;700&family=Rozha+One&display=swap',
  swatch: ['#9a2b1f', '#1b2a5c', '#e2a419', '#2c6a2b'],
  swatchDark: ['#121a4e', '#f1e9d6', '#e5553e', '#93d47c'],
  notes: [
    ['Shape', 'Cloth rectangles with 2px corners. Every panel is a printed double line (1px outline, a second line 3px inside) with a 1px off-register colour beside it and an 8px lozenge border band on its top edge.'],
    ['Type', 'Rozha One for page titles, section heads, keypad digits and the 60–84px hero figure; Hind 400–700 with tabular numerals for every row, label and amount.'],
    ['Density', 'Medium. Panels sit 8px apart on the patterned ground; rows are 40–54px. Pattern fills the gutters and any spare cloth, never a text area.'],
    ['Motion', 'One moment: the hero’s stamped blocks press in one by one (scale 1.15 to 1 with opacity, 40ms apart, ease-out). Keys and buttons dye solid when pressed.'],
    ['Voice', 'The product’s own labels. In the gaps only what is true: “Day 15 of 31”, “38% used”, “Each block is 5% of the limit”.'],
    ['Signature', 'The stamped row: 20 eight-point blocks, each 5%. Inked blocks are spent, faint resist outlines are left, a turmeric marker is today. Housing at 94% stamps its last blocks in vermilion.'],
  ],
};

/* ---------- pattern tiles (authored SVG, used as CSS data URIs) ---------- */
const uri = (w, h, body) =>
  `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns='http://www.w3.org/2000/svg' width='${w}' height='${h}' viewBox='0 0 ${w} ${h}'>${body}</svg>`)}")`;
const star = (cx, cy, R, r) =>
  'M' +
  [...Array(16)]
    .map((_, i) => {
      const a = (Math.PI / 8) * i - Math.PI / 2;
      const q = i % 2 ? r : R;
      return `${(cx + q * Math.cos(a)).toFixed(2)} ${(cy + q * Math.sin(a)).toFixed(2)}`;
    })
    .join('L') +
  'Z';
// small diamond-and-dot jaal for the ground
const jaal = (line, dot) =>
  uri(18, 18, `<path d='M9 1.5 16.5 9 9 16.5 1.5 9Z' fill='none' stroke='${line}' stroke-width='1.3'/><path d='M9 6.4 11.6 9 9 11.6 6.4 9Z' fill='${line}'/><g fill='${dot}'><circle cx='0' cy='0' r='1.3'/><circle cx='18' cy='0' r='1.3'/><circle cx='0' cy='18' r='1.3'/><circle cx='18' cy='18' r='1.3'/></g>`);
// eight-point star in a ring medallion
const medal = (ring, fill, eye) =>
  uri(24, 24, `<circle cx='12' cy='12' r='10' fill='none' stroke='${ring}' stroke-width='1.3'/><path d='${star(12, 12, 7.6, 5.2)}' fill='${fill}'/><circle cx='12' cy='12' r='1.7' fill='${eye}'/><g fill='${ring}'><circle cx='0' cy='0' r='1.7'/><circle cx='24' cy='0' r='1.7'/><circle cx='0' cy='24' r='1.7'/><circle cx='24' cy='24' r='1.7'/></g>`);
// lozenge border band between two rules
const band = (bg, rule, loz, dot, vert = false) =>
  vert
    ? uri(10, 20, `<rect width='10' height='20' fill='${bg}'/><path d='M.75 0V20M9.25 0V20' stroke='${rule}' stroke-width='1'/><path d='M5 4.5 7.4 10 5 15.5 2.6 10Z' fill='${loz}'/><g fill='${dot}'><circle cx='5' cy='0' r='1.2'/><circle cx='5' cy='20' r='1.2'/></g>`)
    : uri(20, 10, `<rect width='20' height='10' fill='${bg}'/><path d='M0 .75H20M0 9.25H20' stroke='${rule}' stroke-width='1'/><path d='M4.5 5 10 2.6 15.5 5 10 7.4Z' fill='${loz}'/><g fill='${dot}'><circle cx='0' cy='5' r='1.2'/><circle cx='20' cy='5' r='1.2'/></g>`);
// the stamp block: solid and resist-outline versions (used as masks)
const mFill = uri(16, 16, `<path fill-rule='evenodd' d='${star(8, 8, 7.7, 5.5)}M8 6.2 9.8 8 8 9.8 6.2 8Z'/>`);
const mOut = uri(16, 16, `<path d='${star(8, 8, 7.1, 5.1)}' fill='none' stroke='#000' stroke-width='1.1'/>`);

const L = { cotton: '#f7f0e1', indigo: '#1b2a5c', madder: '#9a2b1f', iron: '#241a17', tur: '#e2a419' };
const K = { vat: '#121a4e', deep: '#0c1238', resist: '#f1e9d6', madder: '#b5382a', tur: '#f0b82e' };

/* ---------- helpers ---------- */
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 19, 1.75)}</span>`;
const bandEl = '<i class="band" aria-hidden="true"></i>';
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.75)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 24, 2.25)}</span>`;

const stamps = (pct, { warn = false, go = false, today = true } = {}) => {
  const cells = [...Array(20)]
    .map((_, i) => {
      const f = Math.max(0, Math.min(1, (pct - i * 5) / 5));
      const cls = f >= 1 ? ' on' : f > 0 ? ' pt' : '';
      const hot = warn && i >= 16 && f > 0 ? ' hot' : '';
      return `<i class="s${cls}${hot}" style="--i:${i}${f > 0 && f < 1 ? `;--f:${Math.round(f * 100)}%` : ''}"></i>`;
    })
    .join('');
  return `<div class="stp${go ? ' go' : ''}" role="img" aria-label="${pct}% stamped; 20 blocks of 5% each"><div class="sr">${cells}</div>${today ? `<u class="td" style="left:${D.dayPct}%"></u>` : ''}</div>`;
};

const hero = (big = false) => `<section class="hero${big ? ' big' : ''}">
  <div class="hp">
    <div class="hx">
      <h2 class="hl">Safe to spend today</h2>
      <div class="fig"><span class="ru">₹</span>${D.safe}</div>
      <p class="sub">₹${D.left} left this month · ${D.days} days to go</p>
    </div>
    <div class="hs">
      ${stamps(D.pct, { go: true })}
      <p class="cap"><span>Spent ₹${D.spent} of ₹${D.budget} · ${D.pct}% used</span><span class="dy"><u></u>Day 15 of 31</span></p>
    </div>
  </div>
  ${bandEl}
</section>`;

const alert = `<p class="al"><span class="ai">${ic('clock', 18, 1.9)}</span><span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const sh = (t, link = 'See all') => `<div class="sh"><h2>${t}</h2>${link ? `<a>${link}</a>` : ''}</div>`;
const pn = (cls, inner) => `<section class="pn ${cls}">${bandEl}${inner}</section>`;

const accounts = pn(
  'acc',
  `${sh('Where it sits')}
  ${D.accounts
    .map(
      (a) =>
        `<div class="ar"><span class="nm"><b>${a.name}</b><small>${a.type}${a.owed ? ' · owed' : ''}</small></span><span class="v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`,
    )
    .join('')}
  <div class="ar tot"><span class="nm"><b>Total balance</b></span><span class="v">₹${D.total}</span></div>`,
);

const row = (r, date = '') =>
  `<div class="rw"><span class="sq">${ic(r.icon, 17, 1.75)}</span><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 2) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const top = (title, l = '', r = '', sub = '') =>
  `<header class="top">${l}<div class="tt"><h1>${title}</h1>${sub}</div><div class="rt">${r}</div></header>${bandEl}`;
const priv = `<p class="pv">${ic('shield', 12, 2)}Kept only on this device</p>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  ${top(`Hello, ${D.user}`, '', ib('eye', 'Hide amounts') + ib('sliders', 'Settings'), priv)}
  <div class="bd">
    ${hero()}
    ${alert}
    ${accounts}
    ${pn('lat', `${sh('Latest')}${row(D.groups[0].rows[1], '2 Oct')}`)}
  </div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${top('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="bd">
    <div class="seg s4"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
    <div class="mo"><span class="mb" aria-label="Previous month">${ic('chevl', 16, 2)}</span><b>October 2026</b><span class="mb" aria-label="Next month">${ic('chevr', 16, 2)}</span></div>
    ${D.groups.map((g) => `<section class="pn gp"><h3 class="dt">${g.date}</h3>${g.rows.map((r) => row(r)).join('')}</section>`).join('')}
  </div>
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg nb">
  ${top('Add transaction', ib('back', 'Back'), '')}
  <div class="bd">
    <div class="seg s3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
    <div class="pn amt"><div class="fig"><span class="ru">₹</span>2,450<i class="caret"></i></div></div>
    <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)].map((k) => `<span>${k}</span>`).join('')}</div>
    <div class="pn fl"><div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 16, 2)}</div>
    ${pn('cat', `${sh('Category', 'Split')}<div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 19, 1.75)}${nm}</span>`).join('')}</div>`)}
  </div>
</div>
<div class="cta">${bandEl}<button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${top('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="bd">
    <div class="mo"><b>Monthly · October 2026</b><span class="dy"><u></u>Day 15 of 31</span></div>
    ${D.budgets
      .map((b) => {
        const warn = b.pct > 85;
        return pn(
          `bg${warn ? ' warn' : ''}`,
          `<div class="bh"><b>${b.name}</b><em class="${warn ? 'wn' : 'ok'}">${ic(warn ? 'alert' : 'check', 12, 2.25)}${b.state}</em></div>
          <div class="br">${stamps(b.pct, { warn })}<strong>${b.pct}%</strong></div>
          <div class="bf"><span>Spent <b>₹${b.spent}</b></span><span>Limit <b>₹${b.limit}</b></span><span>Left <b>₹${b.left}</b></span></div>`,
        );
      })
      .join('')}
    <p class="lg"><i class="s on"></i>Each block is 5% of the limit.<u class="tk"></u>The marker is today.</p>
  </div>
</div>
${tabs('')}`;

const desk = `
<aside class="sd">
  <div class="mk"><i class="s on" aria-hidden="true"></i>Finio</div>
  <span class="addb">${ic('plus', 16, 2.25)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 17, 1.75)}${nm}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
  <i class="sp" aria-hidden="true"></i>
  <i class="vb" aria-hidden="true"></i>
</aside>
<main class="mn">
  ${top(`Hello, ${D.user}`, '', ib('eye', 'Hide amounts') + ib('sliders', 'Settings'), priv)}
  <div class="dc">
    ${hero(true)}
    ${alert}
    <div class="cols">
      ${accounts}
      ${pn(
        'gls',
        `${sh('Savings goals')}${D.goals
          .map(
            (g) =>
              `<div class="go"><div class="gh"><b>${g.name}</b><strong>${g.pct}%</strong></div>${stamps(g.pct, { today: false })}<small>₹${g.have} of ₹${g.target}</small></div>`,
          )
          .join('')}`,
      )}
      ${pn('lat', `${sh('Latest')}${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}`)}
    </div>
  </div>
</main>`;

export const css = `
.app{--ground:${L.madder};--on-g:#f8f0df;--on-g-mut:#f3d3c5;--g-line:rgba(248,240,223,.55);
  --panel:${L.cotton};--ink:${L.indigo};--mut:#4d5273;--line:${L.iron};--line2:rgba(154,43,31,.5);--mis:${L.tur};--hair:rgba(27,42,92,.17);
  --dye:${L.indigo};--on-dye:#f8f0df;--dye-press:#111c42;--key-press:#e6dcc4;
  --tur:${L.tur};--on-tur:${L.iron};--pos:#2c6a2b;--warn:#7a5200;--neg:#c62812;--neg-fill:#c62812;--on-neg:#fff6e8;
  --stamp:${L.indigo};--stamp-mis:#cf4a34;--hot-mis:${L.iron};--resist:rgba(27,42,92,.36);
  --p-jaal:${jaal('#7d1e15', 'rgba(248,240,223,.5)')};
  --p-med:${medal('rgba(247,240,225,.8)', '#cf4a34', L.indigo)};
  --p-band:${band(L.cotton, L.iron, L.indigo, L.madder)};
  --p-vband:${band(L.cotton, L.iron, L.indigo, L.madder, true)};
  --m-fill:${mFill};--m-out:${mOut};
  --dp:'Rozha One','Hind',serif;--tx:'Hind',system-ui,sans-serif;
  background:var(--ground) var(--p-jaal) 0 0/18px 18px;color:var(--on-g);font-family:var(--tx);font-size:14px;line-height:1.3;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--neg)}
html[data-mode=dark] .app{--ground:${K.vat};--on-g:${K.resist};--on-g-mut:#bcc2e2;--g-line:rgba(241,233,214,.4);
  --panel:#1c2868;--ink:${K.resist};--mut:#b7bddd;--line:#4354a8;--line2:rgba(241,233,214,.26);--mis:${K.deep};--hair:rgba(241,233,214,.15);
  --dye:${K.madder};--on-dye:#fdf3e0;--dye-press:#8e2a1f;--key-press:#2a3990;
  --tur:${K.tur};--on-tur:#1a1206;--pos:#93d47c;--warn:${K.tur};--neg:#ff9279;--neg-fill:#e5553e;--on-neg:#1a0d0a;
  --stamp:${K.resist};--stamp-mis:#e5553e;--hot-mis:${K.resist};--resist:rgba(241,233,214,.36);
  --p-jaal:${jaal('#1f2c76', 'rgba(241,233,214,.34)')};
  --p-med:${medal('rgba(241,233,214,.8)', K.vat, K.tur)};
  --p-band:${band(K.deep, K.resist, K.resist, K.tur)};
  --p-vband:${band(K.deep, K.resist, K.resist, K.tur, true)}}
.app ::selection{background:var(--tur);color:var(--on-tur)}
.app :focus-visible{outline:2px solid var(--tur);outline-offset:2px}
.app h1,.app h2,.app h3{font-weight:400}
.app .pg{position:absolute;inset:0 0 74px;overflow:hidden}
.app .pg.nb{bottom:96px}
.bd{padding:10px 12px 0;display:flex;flex-direction:column;gap:8px}
.band{display:block;height:10px;background:var(--p-band) 0 0/20px 10px repeat-x;flex:none}

/* top strip: plain dyed selvedge, no pattern under the title */
.top{background:var(--ground);padding:46px 14px 8px;display:flex;align-items:center;gap:8px;min-height:102px}
.tt{flex:1;min-width:0}
.top h1{font-family:var(--dp);font-size:27px;line-height:1.1;letter-spacing:.005em}
.pv{display:flex;align-items:center;gap:5px;font-size:12.5px;color:var(--on-g-mut);margin-top:1px}
.rt{display:flex;gap:8px}
.ib{width:38px;height:38px;display:grid;place-items:center;border:1px solid var(--g-line);border-radius:2px;box-shadow:inset 0 0 0 2px var(--ground),inset 0 0 0 3px var(--g-line);color:var(--on-g);flex:none}
.ib:active{background:var(--on-g);color:var(--ground)}

/* cloth panel: printed double line, off-register second colour, band on the top edge */
.pn{position:relative;background:var(--panel);color:var(--ink);border:1px solid var(--line);border-radius:2px;box-shadow:inset 0 0 0 3px var(--panel),inset 0 0 0 4px var(--line2),1px 1px 0 var(--mis);padding:6px 14px 6px}
.pn>.band{height:8px;margin:-6px -14px 4px;background-size:16px 8px;border-bottom:1px solid var(--line)}
.sh{display:flex;justify-content:space-between;align-items:baseline;padding:3px 0 2px}
.sh h2{font-family:var(--dp);font-size:18px;line-height:1.25}
.sh a{font-size:13px;font-weight:600;text-decoration:underline;text-decoration-thickness:1px;text-underline-offset:3px}

/* hero cloth */
.hero{position:relative;background:var(--dye) var(--p-med) 0 0/16px 16px round;padding:12px 12px 22px;border:1px solid var(--line);border-radius:2px;box-shadow:1px 1px 0 var(--mis)}
.hero>.band{position:absolute;left:0;right:0;bottom:0;border-top:1px solid var(--line)}
.hp{background:var(--panel);color:var(--ink);border:1px solid var(--line);box-shadow:inset 0 0 0 3px var(--panel),inset 0 0 0 4px var(--line2);padding:10px 14px 10px}
.hl{font-size:14px;font-weight:600!important;letter-spacing:.01em}
.fig{font-family:var(--dp);font-size:60px;line-height:1.02;letter-spacing:.005em;text-shadow:1px 1px 0 var(--stamp-mis)}
.sub{font-size:14.5px;font-weight:500;margin-top:-2px}
.hs{margin-top:12px}
.cap{display:flex;justify-content:space-between;gap:8px;font-size:13px;color:var(--mut);margin-top:9px}
.dy{display:inline-flex;align-items:center;gap:6px;font-size:13px;font-weight:600;color:var(--ink);white-space:nowrap}
.dy u{width:3px;height:13px;background:var(--tur);outline:1px solid var(--line);text-decoration:none}

/* the stamped row */
.stp{position:relative}
.sr{display:flex;gap:2px}
.s{flex:1;aspect-ratio:1;position:relative;display:block;min-width:0}
.s::before,.s::after{content:'';position:absolute;inset:0;-webkit-mask:var(--m-out) center/contain no-repeat;mask:var(--m-out) center/contain no-repeat;background:var(--resist)}
.s::after{-webkit-mask-image:var(--m-fill);mask-image:var(--m-fill);background:var(--stamp);opacity:0}
.s.on,.s.pt{filter:drop-shadow(1px 1px 0 var(--stamp-mis))}
.s.on::before{display:none}
.s.on::after,.s.pt::after{opacity:1}
.s.pt::after{clip-path:inset(0 calc(100% - var(--f)) 0 0)}
.s.hot::after{background:var(--neg-fill)}
.s.hot{filter:drop-shadow(1px 1px 0 var(--hot-mis))}
.td{position:absolute;top:-5px;bottom:-5px;width:3px;margin-left:-1.5px;background:var(--tur);outline:1px solid var(--line);text-decoration:none}
.td::before{content:'';position:absolute;left:50%;top:-4px;width:7px;height:7px;margin-left:-3.5px;background:var(--tur);outline:1px solid var(--line);transform:rotate(45deg)}
.stp.go .s.on::after,.stp.go .s.pt::after{animation:press .3s cubic-bezier(.16,.84,.3,1) backwards;animation-delay:calc(var(--i)*40ms + 250ms)}
@keyframes press{from{opacity:0;transform:scale(1.15)}}

/* alert */
.al{display:flex;align-items:stretch;background:var(--panel);color:var(--ink);border:1px solid var(--line);border-radius:2px;box-shadow:1px 1px 0 var(--mis);font-size:14px;min-height:40px}
.ai{flex:none;width:40px;display:grid;place-items:center;background:var(--tur);color:var(--on-tur);border-right:1px solid var(--line)}
.al>span:last-child{padding:6px 12px;align-self:center;line-height:1.3}
.al b{font-weight:700}

/* accounts */
.ar{display:flex;justify-content:space-between;align-items:center;gap:10px;min-height:40px;border-top:1px solid var(--hair)}
.ar .nm{display:flex;align-items:baseline;gap:8px;min-width:0}
.ar b{font-weight:600;font-size:15px}
.ar small{font-size:12.5px;color:var(--mut)}
.ar .v{font-size:15.5px;font-weight:600;white-space:nowrap}
.ar .v.neg{color:var(--neg)}
.ar.tot{border-top:1px solid var(--line);box-shadow:inset 0 3px 0 var(--panel),inset 0 4px 0 var(--line)}
.ar.tot b{font-weight:700}.ar.tot .v{font-size:17px;font-weight:700}

/* rows */
.rw{display:flex;align-items:center;gap:11px;min-height:54px;border-top:1px solid var(--hair);padding:5px 0}
.sq{flex:none;width:34px;height:34px;display:grid;place-items:center;border:1px solid var(--ink);border-radius:2px;box-shadow:inset 0 0 0 2px var(--panel),inset 0 0 0 3px var(--resist)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:600;font-size:15px;line-height:1.25}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:block;font-size:12.5px;color:var(--mut);line-height:1.35}
.rw .a{font-size:15.5px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{display:inline-block;font-size:11px;font-weight:600;line-height:1.25;border:1px solid currentColor;border-radius:1px;padding:0 4px;margin-left:6px;white-space:nowrap}
.tg.e{color:var(--pos)}.tg.d{color:var(--warn)}

/* tab bar: a border band, active tab a dyed block */
.tabs{position:absolute;left:0;right:0;bottom:0;height:74px;background:var(--panel);color:var(--mut);display:flex;padding:10px 6px 0;border-top:1px solid var(--line)}
.tabs::before{content:'';position:absolute;left:0;right:0;top:0;height:10px;background:var(--p-band) 0 0/20px 10px repeat-x;border-bottom:1px solid var(--line)}
.tabs span{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;margin:6px 2px 12px;border-radius:2px}
.tabs em{font-style:normal;font-weight:600;font-size:11px}
.tabs span.on{background:var(--dye);color:var(--on-dye);box-shadow:inset 0 0 0 2px var(--dye),inset 0 0 0 3px rgba(248,240,223,.5),1px 1px 0 var(--mis)}
.fab{position:absolute;right:16px;bottom:86px;width:56px;height:56px;border-radius:50%;background:var(--dye);color:var(--on-dye);display:grid;place-items:center;z-index:5;border:1px solid var(--line);box-shadow:inset 0 0 0 4px var(--dye),inset 0 0 0 5px rgba(248,240,223,.75),1px 2px 0 var(--mis),0 6px 14px -4px rgba(0,0,0,.5)}
.fab:active{background:var(--dye-press)}

/* segmented + month */
.seg{display:grid;grid-template-columns:repeat(4,1fr);gap:4px;background:var(--panel);color:var(--mut);border:1px solid var(--line);border-radius:2px;padding:4px;box-shadow:1px 1px 0 var(--mis)}
.seg.s3{grid-template-columns:repeat(3,1fr)}
.seg span{text-align:center;font-weight:600;font-size:13.5px;padding:7px 0 6px;border-radius:1px}
.seg span.on{background:var(--dye);color:var(--on-dye);box-shadow:inset 0 0 0 2px var(--dye),inset 0 0 0 3px rgba(248,240,223,.5)}
.mo{display:flex;justify-content:space-between;align-items:center;background:var(--panel);color:var(--ink);border:1px solid var(--line);border-radius:2px;padding:4px 12px;min-height:40px;box-shadow:1px 1px 0 var(--mis)}
.mo b{font-family:var(--dp);font-weight:400;font-size:17px}
.mb{width:30px;height:30px;display:grid;place-items:center;border:1px solid var(--ink);border-radius:2px}
.mb:active{background:var(--key-press)}
.mo .mb:first-child{margin-left:-8px}.mo .mb:last-child{margin-right:-8px}
.gp{padding:4px 14px 2px}
.dt{font-size:12.5px;font-weight:700!important;color:var(--mut);padding:4px 0 3px}

/* add */
.amt{text-align:center;padding:8px 14px 6px}
.amt .fig{display:inline-flex;align-items:center;font-size:54px;text-shadow:none}
.amt .ru{font-size:36px;margin-right:4px;color:var(--mut)}
.caret{width:3px;height:46px;background:var(--neg);margin-left:6px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:6px}
.np span{height:45px;display:grid;place-items:center;background:var(--panel);color:var(--ink);font-family:var(--dp);font-size:24px;line-height:1;border:1px solid var(--line);border-radius:2px;box-shadow:inset 0 0 0 2px var(--panel),inset 0 0 0 3px var(--line2),1px 1px 0 var(--mis)}
.np span:active{background:var(--key-press)}
.fl{display:flex;align-items:center;gap:10px;padding:7px 14px}
.fl div{flex:1}.fl small{display:block;font-size:12px;font-weight:600;color:var(--mut)}
.fl b{font-weight:600;font-size:15.5px}.fl>span{font-size:13px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:5px;padding:4px 0 4px}
.cg span{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;height:58px;font-size:11.5px;font-weight:500;border:1px solid var(--ink);border-radius:2px;box-shadow:inset 0 0 0 2px var(--panel),inset 0 0 0 3px var(--resist);min-width:0}
.cg span:active{background:var(--key-press)}
.cg span.on{background:var(--dye);color:var(--on-dye);font-weight:600;border-color:var(--line);box-shadow:inset 0 0 0 2px var(--dye),inset 0 0 0 3px rgba(248,240,223,.55),1px 1px 0 var(--stamp-mis)}
.cta{position:absolute;left:0;right:0;bottom:0;height:96px;background:var(--ground)}
.cta button{display:block;width:calc(100% - 24px);margin:10px 12px 0;height:52px;border:1px solid var(--line);border-radius:2px;background:var(--dye);color:var(--on-dye);font-family:var(--dp);font-size:19px;letter-spacing:.01em;box-shadow:inset 0 0 0 3px var(--dye),inset 0 0 0 4px rgba(248,240,223,.6),1px 1px 0 var(--mis)}
.cta button:active{background:var(--dye-press)}

/* budgets */
.mo>b:first-child{font-size:16px}
.bg{padding-bottom:8px}
.bh{display:flex;justify-content:space-between;align-items:center;padding-top:2px}
.bh b{font-family:var(--dp);font-weight:400;font-size:18px;line-height:1.3}
.bh em{display:inline-flex;align-items:center;gap:4px;font-style:normal;font-weight:700;font-size:12px;padding:2px 7px 1px;border-radius:1px;border:1px solid currentColor}
.bh em.ok{color:var(--pos)}
.bh em.wn{background:var(--neg-fill);color:var(--on-neg);border-color:var(--line);box-shadow:1px 1px 0 var(--line)}
.br{display:grid;grid-template-columns:1fr 46px;align-items:center;gap:8px;margin:9px 0 8px}
.br strong{font-family:var(--dp);font-weight:400;font-size:20px;text-align:right;line-height:1}
.bg.warn .br strong{color:var(--neg)}
.bf{display:flex;justify-content:space-between;font-size:12.5px;color:var(--mut);border-top:1px solid var(--hair);padding-top:5px}
.bf b{font-size:14px;font-weight:600;color:var(--ink);margin-left:2px}
.lg{display:flex;align-items:center;gap:7px;background:var(--panel);color:var(--mut);border:1px solid var(--line);border-radius:2px;padding:7px 12px;font-size:13px}
.lg .s{flex:none;width:14px}
.lg .tk{flex:none;width:3px;height:14px;background:var(--tur);outline:1px solid var(--line);margin-left:8px}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:240px;background:var(--dye);color:var(--on-dye);padding:20px 28px 0 18px;display:flex;flex-direction:column}
.vb{position:absolute;top:0;bottom:0;right:0;width:10px;background:var(--p-vband) 0 0/10px 20px repeat-y}
.mk{display:flex;align-items:center;gap:10px;font-family:var(--dp);font-size:32px;line-height:1;margin-bottom:16px}
.mk .s{flex:none;width:26px;--stamp:var(--on-dye);--stamp-mis:var(--tur)}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:42px;background:var(--panel);color:var(--ink);font-weight:700;font-size:14px;border-radius:2px;box-shadow:inset 0 0 0 2px var(--panel),inset 0 0 0 3px var(--line2),1px 1px 0 var(--tur);margin-bottom:14px}
html[data-mode=dark] .app .addb{background:${K.resist};color:${K.vat};box-shadow:inset 0 0 0 2px ${K.resist},inset 0 0 0 3px rgba(18,26,78,.4),1px 1px 0 ${K.tur}}
.sd nav{display:flex;flex-direction:column;gap:2px}
.ni{display:flex;align-items:center;gap:10px;padding:7px 10px;font-weight:600;font-size:14px;border-radius:2px}
.ni.on{background:var(--tur);color:var(--on-tur);box-shadow:inset 0 0 0 2px var(--tur),inset 0 0 0 3px rgba(36,26,23,.45)}
.gl{font-family:var(--dp);font-size:16px;margin:16px 0 4px;padding:8px 10px 0;border-top:1px solid rgba(248,240,223,.4)}
.tl{font-size:13.5px;padding:3.5px 10px;opacity:.92}
.sp{flex:1;margin:14px -28px 0 -18px;background:var(--p-med) 0 0/40px 40px;border-top:1px solid rgba(248,240,223,.5);min-height:40px}
.mn{position:absolute;left:240px;right:0;top:0;bottom:0;overflow:hidden;display:flex;flex-direction:column}
.mn .top{padding:10px 26px;min-height:62px}
.mn .top h1{font-size:26px}
.dc{flex:1;min-height:0;padding:14px 26px 18px;display:flex;flex-direction:column;gap:12px}
.hero.big{padding:16px 16px 26px}
.hero.big .hp{display:grid;grid-template-columns:330px 1fr;gap:40px;align-items:center;padding:18px 28px 18px}
.hero.big .hl{font-size:16px}
.hero.big .fig{font-size:84px;line-height:1}
.hero.big .sub{font-size:16px;margin-top:0}
.hero.big .hs{margin-top:0}
.hero.big .sr{gap:3px}
.hero.big .cap{font-size:14px;margin-top:14px}
.hero.big .td{top:-7px;bottom:-7px}
.cols{flex:1;min-height:0;display:grid;grid-template-columns:1fr 1fr 1.2fr;gap:12px}
.cols .pn{display:flex;flex-direction:column;padding-bottom:8px}
.cols .ar,.cols .rw,.cols .go{flex:1}
.cols .ar .nm{flex-direction:column;gap:0}
.go{display:flex;flex-direction:column;justify-content:center;border-top:1px solid var(--hair);gap:7px}
.gh{display:flex;justify-content:space-between;align-items:baseline}
.gh b{font-weight:600;font-size:15px}.gh strong{font-family:var(--dp);font-weight:400;font-size:19px}
.go small{font-size:13px;color:var(--mut)}
`;

export const screens = { home, txns, add, budgets, desk };
