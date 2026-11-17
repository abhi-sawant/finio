import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 19,
  slug: 'sargam',
  name: 'Sargam',
  tagline:
    'The film-song cassette and its J-card, played on a two-in-one. For people who want their money with some 1980s swagger — bold stripes, wide lettering, warm colour — and the month shown as tape running from one reel to the other.',
  blurb:
    'The audio cassette. A five-stripe run that turns the corner of every label, Michroma faceplate lettering, piano-key controls, equaliser-strip budgets and a tape window whose reels show the month.',
  fonts: 'https://fonts.googleapis.com/css2?family=Lexend:wght@400;500;600&family=Michroma&display=swap',
  swatch: ['#ecebe6', '#3a2315', '#f26b1d', '#188078'],
  swatchDark: ['#1a1310', '#f4eadc', '#ff8a3a', '#31b7a8'],
  notes: [
    ['Shape', 'Cassette geometry: 11px-radius label panels with a thin double-line frame, a recessed well for the tab strip, and chunky transport keys — a 4px bottom edge and one top highlight line, dropping 2px when pressed or held.'],
    ['Type', 'Michroma for titles, section legends, keypad digits and the hero figure (54px on phone, 68px on desktop); Lexend 400–600 with tabular numerals for every row, amount and caption. The rupee sign is always set in Lexend.'],
    ['Density', 'Medium with a generous hero: the label area takes about 40% of the Home screen; accounts sit as three label tiles so Latest still fits above the keys.'],
    ['Motion', 'On load both hubs of the tape window turn a third of a revolution and settle (1.4s ease-out), then idle at one turn a minute unless reduced motion is set. Keys drop 2px on press. Nothing else moves.'],
    ['Voice', 'Plain. The only tape idiom is the reel caption, and it is the calendar: “16 days left”, “Day 15 of 31”, “15 days gone”. Product labels stay as they are.'],
    ['Signature', 'The tape window: the supply reel is the 16 days left, the take-up reel the 15 days gone, drawn to scale by area inside a ring that marks a full 31-day reel. Budgets are 12-bar level meters — teal to 70%, mustard to 90%, red beyond.'],
  ],
};

/* ---------- helpers ---------- */
const rs = '<span class="rs">₹</span>';
const ib = (i, label) => `<span class="k ib" role="button" tabindex="0" aria-label="${label}">${ic(i, 19, 1.75)}</span>`;
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span class="k${nm === active ? ' on' : ''}">${ic(i, 19, 1.75)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" role="button" tabindex="0" aria-label="Add transaction">${ic('plus', 24, 2.25)}</span>`;

const meter = (pct, label = 'used') => {
  const lit = Math.round((pct * 12) / 100);
  return `<div class="mt" role="img" aria-label="${pct}% ${label}">${Array.from({ length: 12 }, (_, k) => `<i class="${k < 8 ? 't' : k < 10 ? 'm' : 'r'}${k < lit ? ' on' : ''}"></i>`).join('')}</div>`;
};

/* tape window — pack radii are to scale by area: full reel = 31 days */
const tape = (() => {
  const cy = 44, hub = 13, rmax = 37, Lx = 76, Rx = 224;
  const rad = (f) => Math.sqrt(hub * hub + f * (rmax * rmax - hub * hub));
  const rl = rad(16 / 31), rr = rad(15 / 31);
  const tan = (px, py, cx, r, left) => {
    const d = Math.hypot(px - cx, py - cy), a = Math.atan2(py - cy, px - cx), b = Math.acos(r / d);
    const p = [a + b, a - b].map((t) => [cx + r * Math.cos(t), cy + r * Math.sin(t)]).sort((m, q) => m[0] - q[0]);
    return (left ? p[0] : p[1]).map((v) => v.toFixed(1)).join(' ');
  };
  const teeth = Array.from({ length: 6 }, (_, k) => `<rect x="-1.5" y="-9.6" width="3" height="4.4" rx=".6" transform="rotate(${k * 60})"/>`).join('');
  const reel = (cx, r) => `<g transform="translate(${cx} ${cy})">
    <circle r="${rmax}" class="full"/>
    <circle r="${r.toFixed(1)}" class="pack"/>
    <circle r="${((r + hub) / 2).toFixed(1)}" class="groove"/><circle r="${(r - 2.5).toFixed(1)}" class="groove"/>
    <g class="hub"><circle r="${hub}" class="hb"/><circle r="9.2" class="hh"/><g class="hb">${teeth}</g></g>
  </g>`;
  return `<svg class="tw" viewBox="0 0 300 96" role="img" aria-label="Tape window: 16 days left on the supply reel, 15 days gone on the take-up reel">
    <rect x=".75" y=".75" width="298.5" height="94.5" rx="12" class="win"/>
    <path class="path" d="M${tan(13, 80, Lx, rl, true)} L13 80 Q13 89 22 89 L278 89 Q287 89 287 80 L${tan(287, 80, Rx, rr, false)}"/>
    <circle cx="13" cy="82" r="3" class="roll"/><circle cx="287" cy="82" r="3" class="roll"/>
    ${reel(Lx, rl)}${reel(Rx, rr)}
  </svg>
  <p class="twc"><span>${D.days} days left</span><b>Day 15 of 31</b><span>15 days gone</span></p>`;
})();

const band = '<i class="bd bd-h" aria-hidden="true"></i><i class="bd bd-c" aria-hidden="true"></i><i class="bd bd-v" aria-hidden="true"></i>';

const hero = (big = false) => `<section class="hero${big ? ' big' : ''}">
  ${band}
  <div class="tl"><h2>Safe to spend today</h2><span>15 Oct 2026</span></div>
  <div class="hb2">
    <div class="hl">
      <p class="fig">${rs}${D.safe}</p>
      <div class="mrow">${meter(D.pct)}<b>${D.pct}% used</b></div>
      <p class="s1">₹${D.left} left this month · ${D.days} days to go</p>
      <p class="s2">Spent ₹${D.spent} of ₹${D.budget}</p>
    </div>
    <div class="hr">${tape}</div>
  </div>
</section>`;

const alert = `<p class="al"><span class="ai">${ic('alert', 17, 2)}</span><span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const sh = (t, link = 'See all') => `<div class="sh"><h2>${t}</h2><i aria-hidden="true"></i>${link ? `<a>${link}</a>` : ''}</div>`;

const who = `<div class="who"><span class="av" aria-hidden="true">A</span><div><b>${D.user}</b><small>${ic('shield', 12, 1.75)}Kept only on this device</small></div></div>`;

const row = (r, date = '') =>
  `<div class="rw"><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 2) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header><i class="run" aria-hidden="true"></i>`;

const goal = (g) =>
  `<div class="gl"><div class="gh"><b>${g.name}</b><small>₹${g.have} of ₹${g.target}</small><span>${g.pct}<span class="rs">%</span></span></div>${meter(g.pct, 'saved')}</div>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  <header class="top">${who}<div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero()}
  ${alert}
  ${sh('Where it sits')}
  <div class="ac3">${D.accounts.map((a) => `<div class="at"><small>${a.name}</small><b${a.owed ? ' class="neg"' : ''}>${a.owed ? '−' : ''}₹${a.bal}</b></div>`).join('')}</div>
  <div class="tot"><span>Total balance</span><b>${rs}${D.total}</b></div>
  ${sh('Latest')}
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="pk p4"><span class="k on">All</span><span class="k">Expense</span><span class="k">Income</span><span class="k">Transfer</span></div>
  <div class="mo"><span class="k">${ic('chevl', 16, 2)}</span><b>October 2026</b><span class="k">${ic('chevr', 16, 2)}</span></div>
  <div class="pn ls">${D.groups.map((g) => `<div class="dl">${g.date}</div>${g.rows.map((r) => row(r)).join('')}`).join('')}</div>
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="pk p3"><span class="k on">Expense</span><span class="k">Income</span><span class="k">Transfer</span></div>
  <div class="amt"><small>Amount</small><p class="fig">${rs}2,450<i class="caret"></i></p></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)].map((k) => `<span class="k">${k}</span>`).join('')}</div>
  <div class="pn fl"><div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 16, 2)}</div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 19, 1.75)}${nm}</span>`).join('')}</div>
</div>
<div class="cta"><button class="k go">Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><b>Monthly · October 2026</b><span>Day 15 of 31</span></div>
  ${D.budgets
    .map((b) => {
      const near = b.state !== 'On track';
      return `<section class="pn bp${near ? ' nr' : ''}">
    <div class="bh"><b>${b.name}</b><span class="pl${near ? ' nr' : ''}">${ic(near ? 'alert' : 'check', 13, 2.25)}${b.state}</span></div>
    <div class="mrow">${meter(b.pct)}<strong>${b.pct}<span class="rs">%</span></strong></div>
    <div class="bf"><span>₹${b.spent} of ₹${b.limit}</span><span><b>₹${b.left}</b> left</span></div>
  </section>`;
    })
    .join('')}
</div>
${tabs()}`;

const mark = `<svg width="30" height="21" viewBox="0 0 30 21" fill="none" stroke="currentColor" stroke-width="1.75" aria-hidden="true"><rect x="1" y="1" width="28" height="19" rx="3.5"/><circle cx="10" cy="10" r="3"/><circle cx="20" cy="10" r="3"/><path d="M8 20l1.5-4h11L22 20"/></svg>`;

const desk = `
<aside class="sd">
  <div class="mk">${mark}<b>Finio</b></div>
  <span class="k go addb" role="button" tabindex="0">${ic('plus', 16, 2.5)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 17, 1.75)}${nm}</span>`).join('')}</nav>
  <div class="tg2">Tools</div>
  ${D.tools.map((t) => `<span class="to">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="top">${who}<div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero(true)}
  ${alert}
  <div class="cols">
    <div>
      ${sh('Where it sits')}
      <div class="acl">${D.accounts.map((a) => `<div class="ar"><b>${a.name}</b><small>${a.type}</small><span${a.owed ? ' class="neg"' : ''}>${a.owed ? '−' : ''}₹${a.bal}</span></div>`).join('')}</div>
      <div class="tot"><span>Total balance</span><b>${rs}${D.total}</b></div>
      ${sh('Savings goals')}
      <div class="gls">${D.goals.map(goal).join('')}</div>
    </div>
    <div>
      ${sh('Latest')}
      ${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}
    </div>
  </div>
</main>`;

export const css = `
.app{--paper:#ecebe6;--card:#f8f7f3;--well:#dcd9d1;--ink:#3a2315;--mut:#6a564a;--rule:#cdc7bc;--line:#3a2315;--key:#fbfaf6;--edge:#b3aa9d;--hi:rgba(255,255,255,.95);
  --s1:#f26b1d;--s2:#e3aa1e;--s3:#d0214f;--s4:#5c2a3d;--s5:#188078;
  --act:#f26b1d;--act-d:#b04608;--on-act:#2a150a;--lit:#f26b1d;--lit-d:#b04608;
  --pos:#0d6e63;--warn:#7d5600;--neg:#b51643;--off:#d2cec5;--plate:#d0214f;--on-plate:#fff;--av:#f8f7f3;
  --win:#2a1a12;--winl:#3a2315;--pack:#70493a;--groove:#8d6450;--hub:#f1ece3;--full:rgba(241,236,227,.34);
  --dp:'Michroma',sans-serif;--tx:'Lexend',sans-serif;--sw:6px;--ri:14px;
  --run:linear-gradient(180deg,var(--s1) 0 20%,var(--s2) 20% 40%,var(--s3) 40% 60%,var(--s4) 60% 80%,var(--s5) 80% 100%);
  --runv:linear-gradient(270deg,var(--s1) 0 20%,var(--s2) 20% 40%,var(--s3) 40% 60%,var(--s4) 60% 80%,var(--s5) 80% 100%);
  background:var(--paper);color:var(--ink);font-family:var(--tx);font-size:14px;line-height:1.3;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--act)}
html[data-mode=dark] .app{--paper:#1a1310;--card:#241a15;--well:#110c09;--ink:#f4eadc;--mut:#bba898;--rule:#3f3027;--line:#5e493c;--key:#35271f;--edge:#090605;--hi:rgba(255,232,200,.12);
  --s1:#ff8a3a;--s2:#f5c443;--s3:#f24a74;--s4:#93506c;--s5:#31b7a8;
  --act:#ff8a3a;--act-d:#9c450b;--on-act:#24130a;--lit:#ffb84d;--lit-d:#9a6412;
  --pos:#56d3c0;--warn:#f5c443;--neg:#ff7896;--off:#3e2f26;--plate:#f24a74;--on-plate:#24130a;--av:#fff6ea;
  --win:#0c0806;--winl:#5e493c;--pack:#7d5643;--groove:#9c7059;--hub:#f4eadc;--full:rgba(255,184,77,.5)}
.app ::selection{background:var(--act);color:var(--on-act)}
.app :focus-visible{outline:2px solid var(--act);outline-offset:2px}
.app .pg{position:absolute;inset:44px 0 72px;padding:0 16px;overflow:hidden}
.rs{font-family:var(--tx);font-weight:500;font-size:.74em;margin-right:.05em}
.neg{color:var(--neg)}

/* transport keys */
.k{background:var(--key);color:var(--ink);border:1px solid var(--edge);border-bottom-width:4px;border-radius:9px;box-shadow:inset 0 1px 0 var(--hi);display:grid;place-items:center}
.k:active,.k.on{transform:translateY(2px);border-bottom-width:2px}
.k.on{background:var(--lit);border-color:var(--lit-d);color:var(--on-act);box-shadow:none}
.k.go{background:var(--act);border-color:var(--act-d);color:var(--on-act);box-shadow:inset 0 1px 0 rgba(255,255,255,.35)}
.ib{width:40px;height:40px;border-bottom-width:3px;flex:none}
.rt{display:flex;gap:8px}

/* panels: thin double-line label frame */
.pn{background:var(--card);border:1px solid var(--line);border-radius:11px;box-shadow:inset 0 0 0 3px var(--card),inset 0 0 0 4px var(--rule)}

/* greeting */
.top{display:flex;align-items:center;justify-content:space-between;height:50px;padding-bottom:6px}
.who{display:flex;align-items:center;gap:10px}
.av{width:38px;height:38px;border-radius:9px;background:var(--s4);color:var(--av);display:grid;place-items:center;font-family:var(--dp);font-size:15px}
.who b{display:block;font-family:var(--dp);font-weight:400;font-size:16px;line-height:1.15}
.who small{display:flex;align-items:center;gap:4px;font-size:12px;color:var(--mut);margin-top:3px}

/* hero: the cassette label */
.hero{position:relative;background:var(--card);border:1.5px solid var(--line);border-radius:12px;overflow:hidden;padding:calc(var(--sw)*5 + 11px) calc(var(--sw)*5 + 14px) 11px 16px}
.hero::before{content:'';position:absolute;inset:3px;border:1px solid var(--rule);border-radius:8px;pointer-events:none}
.bd{position:absolute;display:block}
.bd-h{top:0;left:0;right:calc(var(--sw)*5 + var(--ri));height:calc(var(--sw)*5);background:var(--run)}
.bd-v{right:0;bottom:0;top:calc(var(--sw)*5 + var(--ri));width:calc(var(--sw)*5);background:var(--runv)}
.bd-c{top:0;right:0;width:calc(var(--sw)*5 + var(--ri));height:calc(var(--sw)*5 + var(--ri));
  background:radial-gradient(circle at 0 100%,transparent 0 var(--ri),var(--s5) var(--ri) calc(var(--ri) + var(--sw)),var(--s4) calc(var(--ri) + var(--sw)) calc(var(--ri) + var(--sw)*2),var(--s3) calc(var(--ri) + var(--sw)*2) calc(var(--ri) + var(--sw)*3),var(--s2) calc(var(--ri) + var(--sw)*3) calc(var(--ri) + var(--sw)*4),var(--s1) calc(var(--ri) + var(--sw)*4) calc(var(--ri) + var(--sw)*5),var(--card) calc(var(--ri) + var(--sw)*5))}
.tl{position:relative;display:flex;justify-content:space-between;align-items:baseline;gap:8px;border-bottom:1px solid var(--line);padding-bottom:5px}
.tl h2{font-family:var(--dp);font-weight:400;font-size:12px;letter-spacing:.02em;white-space:nowrap}
.tl span{font-size:13px;color:var(--mut);white-space:nowrap}
.hero .fig{font-family:var(--dp);font-size:54px;line-height:1;margin-top:11px;white-space:nowrap}
.mrow{display:flex;align-items:center;gap:10px;margin-top:10px}
.mrow b{font-weight:600;font-size:13.5px;white-space:nowrap}
.s1{font-size:13px;font-weight:500;margin-top:8px;letter-spacing:-.01em}
.s2{font-size:13px;color:var(--mut);margin-top:2px}

/* level meter: 12 slim bars */
.mt{flex:1;display:grid;grid-template-columns:repeat(12,1fr);gap:3px;height:16px}
.mt i{border-radius:2.5px;background:var(--off)}
.mt i.on.t{background:var(--s5)}.mt i.on.m{background:var(--s2)}.mt i.on.r{background:var(--s3)}

/* tape window */
.tw{display:block;width:100%;height:auto;margin-top:10px}
.tw .win{fill:var(--win);stroke:var(--winl);stroke-width:1.5}
.tw .path{fill:none;stroke:var(--groove);stroke-width:1.5;stroke-linejoin:round}
.tw .roll{fill:var(--hub)}
.tw .full{fill:none;stroke:var(--full);stroke-width:1;stroke-dasharray:1.5 3.5}
.tw .pack{fill:var(--pack)}
.tw .groove{fill:none;stroke:var(--groove);stroke-width:.75;opacity:.7}
.tw .hb{fill:var(--hub)}.tw .hh{fill:var(--win)}
.tw .hub{transform-box:fill-box;transform-origin:center;animation:reel 1.4s cubic-bezier(.16,1,.3,1) both}
@keyframes reel{from{transform:rotate(-120deg)}to{transform:rotate(0)}}
@keyframes idle{from{transform:rotate(0)}to{transform:rotate(360deg)}}
@media (prefers-reduced-motion:no-preference){.tw .hub{animation:reel 1.4s cubic-bezier(.16,1,.3,1) both,idle 60s linear 1.4s infinite}}
.twc{display:grid;grid-template-columns:1fr auto 1fr;align-items:baseline;font-size:13px;color:var(--mut);margin-top:5px}
.twc b{font-weight:600;color:var(--ink)}
.twc span:last-child{text-align:right}

/* alert */
.al{display:flex;align-items:center;gap:10px;margin-top:10px;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:6px 10px 6px 6px;font-size:13.5px}
.ai{flex:none;width:30px;height:30px;border-radius:6px;background:var(--s2);color:#2a150a;display:grid;place-items:center}
.al b{font-weight:600}

/* section legends */
.sh{display:flex;align-items:center;gap:10px;margin-top:13px;height:20px}
.sh h2{font-family:var(--dp);font-weight:400;font-size:12px;letter-spacing:.02em;white-space:nowrap}
.sh i{flex:1;height:5px;border-top:1px solid var(--line);border-bottom:1px solid var(--line)}
.sh a{font-size:13px;font-weight:500;text-decoration:underline;text-decoration-color:var(--act);text-decoration-thickness:2px;text-underline-offset:3px}

/* accounts (phone) */
.ac3{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:8px}
.at{background:var(--card);border:1px solid var(--rule);border-radius:8px;padding:7px 9px 8px;min-width:0}
.at small{display:block;font-size:12px;color:var(--mut);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.at b{display:block;font-weight:600;font-size:15px;margin-top:2px;white-space:nowrap}
.tot{display:flex;justify-content:space-between;align-items:baseline;padding:8px 2px 0}
.tot span{font-size:13.5px;font-weight:500}
.tot b{font-family:var(--dp);font-weight:400;font-size:16px}

/* track-listing rows */
.rw{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:9px 2px;border-bottom:1px solid var(--rule)}
.rw .nm{min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:500;font-size:15px;line-height:1.25}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:block;font-size:12.5px;color:var(--mut);margin-top:2px}
.rw .a{font-size:15px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{display:inline-block;margin-left:7px;font-size:11px;font-weight:500;color:var(--ink);border:1px solid var(--edge);border-radius:4px;padding:0 5px;line-height:16px;vertical-align:1px}
html[data-mode=dark] .app .tg{border-color:var(--line)}

/* tab strip + record button */
.tabs{position:absolute;left:0;right:0;bottom:0;height:72px;background:var(--well);border-top:1.5px solid var(--line);box-shadow:inset 0 4px 0 rgba(0,0,0,.07);display:flex;gap:5px;padding:9px 8px 13px}
.tabs .k{flex:1;min-width:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;border-radius:8px;border-bottom-width:4px;color:var(--mut)}
.tabs .k.on{border-bottom-width:2px;color:var(--on-act)}
.tabs em{font-style:normal;font-size:10px;font-weight:500;letter-spacing:-.01em}
.fab{position:absolute;right:16px;bottom:86px;width:58px;height:58px;border-radius:50%;background:var(--act);color:var(--on-act);border:1.5px solid var(--act-d);box-shadow:0 3px 0 var(--act-d);display:grid;place-items:center;z-index:5}
.fab::before{content:'';position:absolute;inset:6px;border-radius:50%;border:1.5px solid var(--on-act);opacity:.5}
.fab:active{transform:translateY(2px);box-shadow:0 1px 0 var(--act-d)}

/* page header */
.hd{display:flex;align-items:center;gap:10px;height:50px;padding-bottom:6px}
.hd h1{flex:1;font-family:var(--dp);font-weight:400;font-size:18px;line-height:1.1;white-space:nowrap}
.run{display:block;height:10px;background:var(--run);border-radius:2px}

/* piano-key switch */
.pk{display:grid;gap:4px;margin-top:11px;background:var(--well);border:1px solid var(--line);border-radius:11px;padding:5px 5px 6px}
.pk.p4{grid-template-columns:repeat(4,1fr)}.pk.p3{grid-template-columns:repeat(3,1fr)}
.pk .k{height:38px;font-family:var(--dp);font-size:10.5px;letter-spacing:.02em;border-radius:7px}
.mo{display:flex;align-items:center;justify-content:space-between;margin-top:10px}
.mo .k{width:36px;height:34px;border-bottom-width:3px}
.mo b{font-family:var(--dp);font-weight:400;font-size:13px}
.ls{margin-top:10px;padding:5px 14px 8px}
.dl{font-family:var(--dp);font-size:10.5px;letter-spacing:.04em;color:var(--mut);padding:13px 2px 5px;border-bottom:1px solid var(--line)}
.ls .rw{padding:14px 2px}
.ls .rw:last-child{border-bottom:0}

/* add */
.amt{position:relative;margin-top:10px;background:var(--well);border:1px solid var(--line);border-radius:11px;box-shadow:inset 0 3px 0 rgba(0,0,0,.07);height:84px;display:grid;place-items:center}
.amt small{position:absolute;left:12px;top:8px;font-family:var(--dp);font-size:10.5px;letter-spacing:.04em;color:var(--mut)}
.amt .fig{font-family:var(--dp);font-size:42px;line-height:1;display:flex;align-items:center;margin-top:12px}
.caret{width:3px;height:40px;background:var(--act);margin-left:6px;border-radius:1px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:10px}
.np .k{height:51px;font-family:var(--dp);font-size:18px;border-bottom-width:5px}
.np .k:active{border-bottom-width:3px}
.fl{display:flex;align-items:center;gap:10px;margin-top:10px;padding:9px 13px}
.fl div{flex:1}
.fl small{display:block;font-family:var(--dp);font-size:10.5px;letter-spacing:.04em;color:var(--mut)}
.fl b{display:block;font-weight:600;font-size:15px;margin-top:2px}
.fl span{font-size:13px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px}
.cg span{position:relative;overflow:hidden;display:flex;flex-direction:column;align-items:center;gap:4px;height:63px;padding-top:10px;background:var(--card);border:1px solid var(--rule);border-radius:8px;font-size:11px;color:var(--mut);letter-spacing:-.01em}
.cg span .ic{color:var(--ink)}
.cg span.on{border-color:var(--line);color:var(--ink);font-weight:600;padding-top:7px}
.cg span.on::after{content:'';position:absolute;left:0;right:0;bottom:0;height:10px;background:var(--run)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:11px 16px 26px;background:var(--well);border-top:1.5px solid var(--line);box-shadow:inset 0 4px 0 rgba(0,0,0,.07)}
.cta button{width:100%;height:54px;border-radius:10px;border-bottom-width:5px;font-family:var(--dp);font-size:13px;letter-spacing:.03em}
.app .cta button.k.go{border:1px solid var(--act-d);border-bottom-width:5px}

/* budgets */
.per{display:flex;justify-content:space-between;align-items:baseline;margin-top:11px}
.per b{font-family:var(--dp);font-weight:400;font-size:11.5px;letter-spacing:.02em}
.per span{font-size:13px;color:var(--mut)}
.bp{margin-top:12px;padding:19px 16px 18px}
.bp.nr{border-color:var(--neg);border-width:1.5px}
.bh{display:flex;justify-content:space-between;align-items:center}
.bh b{font-weight:600;font-size:16px}
.pl{display:inline-flex;align-items:center;gap:4px;height:22px;padding:0 8px;border:1px solid var(--pos);border-radius:11px;color:var(--pos);font-size:12px;font-weight:600}
.pl.nr{background:var(--plate);border-color:var(--plate);color:var(--on-plate);border-radius:4px}
.bp .mrow{margin-top:14px}
.bp .mt{height:24px}
.bp .mrow strong{font-family:var(--dp);font-weight:400;font-size:15px;width:48px;text-align:right}
.bp.nr .mrow strong{color:var(--neg)}
.bf{display:flex;justify-content:space-between;margin-top:12px;font-size:13.5px;color:var(--mut)}
.bf b{font-weight:600;color:var(--ink)}

/* desktop: sidebar as the J-card spine */
.sd{position:absolute;left:0;top:0;bottom:0;width:244px;background:var(--card);border-right:1.5px solid var(--line);padding:26px 18px 20px 67px;display:flex;flex-direction:column}
.sd::before{content:'';position:absolute;left:0;top:0;bottom:0;width:50px;background:linear-gradient(90deg,var(--s1) 0 20%,var(--s2) 20% 40%,var(--s3) 40% 60%,var(--s4) 60% 80%,var(--s5) 80% 100%)}
.mk{display:flex;align-items:center;gap:9px;margin-bottom:22px}
.mk b{font-family:var(--dp);font-weight:400;font-size:21px;line-height:1}
.addb{display:flex;align-items:center;justify-content:center;gap:7px;height:44px;font-weight:600;font-size:13.5px;margin-bottom:18px}
.sd nav{display:flex;flex-direction:column;gap:3px}
.ni{display:flex;align-items:center;gap:10px;height:38px;padding:0 10px;border-radius:8px;font-weight:500;font-size:14.5px;border:1px solid transparent}
.ni .ic{color:var(--mut)}
.ni.on{background:var(--lit);border-color:var(--lit-d);color:var(--on-act);font-weight:600}
.ni.on .ic{color:inherit}
.tg2{font-family:var(--dp);font-size:10.5px;letter-spacing:.06em;color:var(--mut);border-bottom:1px solid var(--line);padding:0 2px 6px;margin:24px 0 6px}
.to{font-size:14px;padding:6px 10px;color:var(--ink)}
.mn{position:absolute;left:244px;right:0;top:0;bottom:0;padding:18px 30px 18px}
.hero.big{--sw:8px;--ri:18px;padding:calc(var(--sw)*5 + 13px) calc(var(--sw)*5 + 22px) 14px 22px}
.hero.big .tl h2{font-size:13px}
.hb2{display:block}
.hero.big .hb2{display:grid;grid-template-columns:1fr 450px;gap:36px;align-items:center}
.hero.big .fig{font-size:68px;margin-top:14px}
.hero.big .mrow{margin-top:18px;max-width:400px}
.hero.big .mt{height:18px}
.hero.big .s1{font-size:15px;margin-top:14px}
.hero.big .s2{font-size:13.5px;margin-top:3px}
.hero.big .tw{margin-top:12px}
.mn .al{margin-top:12px;padding:7px 12px 7px 7px;font-size:14px}
.cols{display:grid;grid-template-columns:1fr 1.12fr;gap:0 34px;margin-top:2px}
.cols .sh{margin-top:12px}
.ar{display:grid;grid-template-columns:auto 1fr auto;align-items:baseline;gap:10px;padding:9px 2px;border-bottom:1px solid var(--rule)}
.ar b{font-weight:500;font-size:15px}
.ar small{font-size:12.5px;color:var(--mut)}
.ar span{font-weight:600;font-size:15px}
.acl{margin-top:3px}
.gls{margin-top:6px}
.gl{padding:8px 2px 10px;border-bottom:1px solid var(--rule)}
.gh{display:flex;align-items:baseline;gap:10px;margin-bottom:7px}
.gh small{flex:1}
.gh>span{width:52px;text-align:right}

.gh b{font-weight:500;font-size:14.5px}
.gh>span{font-family:var(--dp);font-size:13px}
.gl .mt{height:14px}
.gl small{font-size:13px;color:var(--mut)}
.cols .rw{padding:11.5px 2px}
`;

export const screens = { home, txns, add, budgets, desk };
