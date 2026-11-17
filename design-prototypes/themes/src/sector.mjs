import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 17,
  slug: 'sector',
  name: 'Sector',
  tagline:
    'Chandigarh modernism. Raw concrete, a strict planning grid and enamel panels in pure primaries — for people who want their money big, blunt and gridded, with Swiss-poster clarity and tropical colour.',
  blurb:
    'Concrete and enamel. Flush colour blocks on a jointed grid, one heavy grotesque at poster scale, and accounts drawn as a façade whose bays are sized by their balances.',
  fonts:
    'https://fonts.googleapis.com/css2?family=Darker+Grotesque:wght@700;800;900&family=Hanken+Grotesk:wght@400;500;600;700;800&display=swap',
  swatch: ['#c8c4ba', '#141413', '#f5c400', '#1846b0'],
  swatchDark: ['#161617', '#f1eee7', '#eab700', '#2450c0'],
  notes: [
    ['Shape', 'Zero radius, zero shadow, no outlines. Everything is a flush rectangular block on a concrete ground, separated by fixed 6px joints; 8px margin to the screen edge.'],
    ['Type', 'Darker Grotesque 800–900 at poster scale with leading under 0.8 (hero figure 128px on phone, 200px on desktop); Hanken Grotesk with tabular numerals for every row, label and amount.'],
    ['Density', 'Big and blunt up top — the yellow hero owns a third of the phone — then tight 2px-jointed lists below. Desktop is an asymmetric 620 / 378 poster grid.'],
    ['Motion', 'Louvres open once on load: each bay’s enamel fill scales in from its left edge, staggered 60ms, ease-out. Keys and blocks flip to cobalt on press.'],
    ['Voice', 'Plain and brief. The gaps only say what the data says: “Day 15 of 31”, “owed”, “Solid is spent, open is left”.'],
    ['Signature', 'The façade: accounts as bays whose width is their balance, the card’s debt a true-scale red sliver; the month as 31 fins with today standing tall; budgets as bays split into spent and open.'],
  ],
};

/* ---------- helpers ---------- */
const n = (s) => Number(String(s).replace(/,/g, ''));
const rs = '<span class="rs">₹</span>';
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 20, 2)}</span>`;
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 2)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 30, 3)}</span>`;
const DATE = 'Thursday, 15 October 2026';
const TODAY = 15;
const MONTH = 31;

const fins = `<div class="fins" role="img" aria-label="Day ${TODAY} of ${MONTH}">${Array.from({ length: MONTH }, (_, k) => `<i class="${k + 1 < TODAY ? 'g' : k + 1 === TODAY ? 't' : 'o'}"></i>`).join('')}</div>
<p class="fcap" aria-hidden="true"><span>Day ${TODAY} of ${MONTH}</span></p>`;

const hero = (big = false) => `<section class="hero${big ? ' big' : ''}">
  <h2>Safe to spend today</h2>
  <p class="num">${rs}${D.safe}</p>
  ${fins}
  <p class="l1"><b>₹${D.left}</b> left this month · ${D.days} days to go</p>
  <div class="hb" aria-hidden="true"><i style="width:${D.pct}%"></i></div>
  <p class="l2">Spent ₹${D.spent} of ₹${D.budget} · ${D.pct}%</p>
</section>`;

const alert = `<p class="due">${ic('alert', 20, 2)}<span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const sh = (t, link = 'See all') => `<div class="sh"><h2>${t}</h2>${link ? `<a>${link}</a>` : ''}</div>`;

const [hdfc, sav, card] = D.accounts;
const pos = n(hdfc.bal) + n(sav.bal);
const facade = `<div class="fac">
  <div class="bay b1" style="flex:${n(hdfc.bal)} 1 0;--i:0"><span class="bn">${hdfc.name}<small>${hdfc.type}</small></span><b>${rs}${hdfc.bal}</b></div>
  <div class="bay b2" style="flex:${n(sav.bal)} 1 0;--i:1"><span class="bn">${sav.name}</span><b>${rs}${sav.bal}</b></div>
</div>
<div class="under">
  <div class="owe"><i class="sliver" style="width:max(5px,${((n(card.bal) / pos) * 100).toFixed(2)}%);--i:2" aria-hidden="true"></i><span class="bn">${card.name}<small>owed</small></span><b>−₹${card.bal}</b></div>
  <div class="tot"><span>Total balance</span><b>${rs}${D.total}</b></div>
</div>`;

const row = (r, { date = '', tag = true } = {}) =>
  `<div class="tr"><span class="tc${r.inc ? ' inc' : ''}">${ic(r.icon, 20, 2)}</span><div class="tn"><b>${r.name}${r.rec ? ic('repeat', 13, 2.25) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}</small></div><div class="tv"><span class="ta${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span>${
    tag && r.tag ? `<em class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</em>` : ''
  }</div></div>`;

const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1>${r}</header>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  <header class="top"><div class="who"><b>${D.user}</b><small>${ic('shield', 13, 2)}Kept only on this device</small></div>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</header>
  ${hero()}
  ${alert}
  ${sh('Where it sits')}
  ${facade}
  ${sh('Latest')}
  <div class="lst short">${row(D.groups[0].rows[1], { tag: false })}</div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="sw s4"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span aria-label="Previous month">${ic('chevl', 18, 2.25)}</span><b>October 2026</b><span aria-label="Next month">${ic('chevr', 18, 2.25)}</span></div>
  ${D.groups.map((g) => `<h3 class="dl">${g.date}</h3><div class="lst">${g.rows.map((r) => row(r)).join('')}</div>`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg nb">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="sw s3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amt"><p class="num">${rs}2,450<i class="caret"></i></p></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 2)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fl"><span class="fc">${ic('wallet', 20, 2)}</span><div><small>Account</small><b>HDFC Checking</b></div><em>Bank account</em>${ic('chevd', 18, 2.25)}</div>
  <div class="sh"><h2>Category</h2><a class="btn">Split</a></div>
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 22, 2)}${nm}</span>`).join('')}</div>
</div>
<div class="cta"><button>${ic('plus', 22, 3)}Add transaction</button></div>`;

const budgets = `
<div class="pg nf">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><b>Monthly · October 2026</b><span>Day ${TODAY} of ${MONTH}</span></div>
  ${D.budgets
    .map((b, k) => {
      const warn = b.pct > 85;
      const solid = warn ? 85 : b.pct;
      return `<section class="bd${warn ? ' warn' : ''}">
    <div class="bh"><h2>${b.name}</h2><em class="stl ${warn ? 'nl' : 'ok'}">${ic(warn ? 'alert' : 'check', 13, 2.5)}${b.state}</em><strong>${b.pct}%</strong></div>
    <div class="bb" role="img" aria-label="${b.pct}% spent">${b.pct ? `<i class="sp" style="width:${solid}%;--i:${k}"></i>` : ''}${warn ? `<i class="hx" style="width:${b.pct - 85}%"></i>` : ''}<u style="left:${D.dayPct}%"></u></div>
    <div class="bf"><span>₹${b.spent} of ₹${b.limit}</span><span><b>₹${b.left}</b> left</span></div>
  </section>`;
    })
    .join('')}
  <p class="foot"><u></u><span>Solid is spent, open is left. The black fin marks day ${TODAY} of ${MONTH}; Housing has used 94% with ${D.days} days to go.</span></p>
</div>
${tabs('')}`;

const mark = `<svg class="mk" width="30" height="30" viewBox="0 0 30 30" aria-hidden="true"><rect width="18" height="30" fill="var(--b)"/><rect x="20" width="10" height="18" fill="var(--y)"/><rect x="20" y="20" width="10" height="10" fill="var(--r)"/></svg>`;

const desk = `
<aside class="pier">
  <div class="brand">${mark}<b>Finio</b></div>
  <span class="addb">${ic('plus', 20, 3)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 18, 2)}${nm}</span>`).join('')}</nav>
  <h3 class="gl">Tools</h3>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="top"><div class="who"><b>${D.user}</b><small>${ic('shield', 13, 2)}Kept only on this device</small></div><span class="dt">${DATE}</span>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</header>
  ${hero(true)}
  <div class="cR">
    ${alert}
    ${sh('Latest')}
    <div class="lst">${D.groups.flatMap((g) => g.rows.map((r) => row(r, { date: g.date.split(', ')[1] }))).join('')}</div>
  </div>
  <div class="cS">${sh('Where it sits')}${facade}</div>
  <div class="cG">${sh('Savings goals')}
    ${D.goals
      .map(
        (g, k) =>
          `<section class="go"><div class="bh"><h2>${g.name}</h2><strong>${g.pct}%</strong></div><div class="bb" role="img" aria-label="${g.pct}% saved">${g.pct ? `<i class="sp" style="width:${g.pct}%;--i:${k + 3}"></i>` : ''}</div><div class="bf"><span>₹${g.have} of ₹${g.target}</span></div></section>`,
      )
      .join('')}
  </div>
</main>`;

export const css = `
.app{--g:#c8c4ba;--p:#e5e2da;--p2:#d5d1c7;--w:#faf9f5;--ink:#141413;--mut:#48453e;--line:rgba(20,20,19,.6);--form:rgba(20,20,19,.045);
  --y:#f5c400;--on-y:#141413;--y-mut:#3b2f00;--r:#c01c18;--on-r:#fff;--b:#1846b0;--on-b:#fff;--b2:#8fb0ea;--on-b2:#0a1a40;--gr:#166b34;--on-gr:#fff;--pos:#14632f;--neg:#b01712;
  --j:6px;--dp:'Darker Grotesque',sans-serif;--tx:'Hanken Grotesk',sans-serif;
  background:var(--g) repeating-linear-gradient(0deg,transparent 0 25px,var(--form) 25px 26px);color:var(--ink);font-family:var(--tx);font-size:14px;line-height:1.3;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--b)}
html[data-mode=dark] .app{--g:#161617;--p:#27272a;--p2:#333337;--w:#3a3a3f;--ink:#f1eee7;--mut:#b7b3aa;--line:rgba(241,238,231,.6);--form:rgba(255,255,255,.022);
  --y:#eab700;--r:#c42a24;--b:#2450c0;--b2:#7c9fe0;--gr:#1f7d42;--pos:#62d08a;--neg:#ff7b70}
.app ::selection{background:var(--b);color:var(--on-b)}
.app :focus-visible{outline:3px solid var(--b);outline-offset:0}
.app .pg{position:absolute;inset:44px 8px 72px;display:flex;flex-direction:column;gap:var(--j);overflow:hidden}
.app .pg.nb{bottom:86px}
.rs{font-family:var(--tx);font-weight:800;font-size:.46em;line-height:0;vertical-align:.62em;margin-right:.06em}
.ib{width:44px;height:44px;display:grid;place-items:center;background:var(--p);color:var(--ink);flex:none}
.ib:active,.btn:active,.mo span:active{background:var(--b);color:var(--on-b)}

/* top row */
.top{display:flex;gap:var(--j);height:44px;flex:none}
.who{flex:1;background:var(--p);display:flex;align-items:center;justify-content:space-between;padding:0 12px 0 10px;min-width:0}
.who b{font-family:var(--dp);font-weight:900;font-size:32px;line-height:.8;padding-bottom:5px}
.who small{display:flex;align-items:center;gap:5px;font-size:12px;font-weight:500;color:var(--mut);white-space:nowrap}

/* hero */
.hero{background:var(--y);color:var(--on-y);padding:10px 14px 12px;flex:none}
.hero h2{font-family:var(--dp);font-weight:800;font-size:28px;line-height:.9}
.hero .num{font-family:var(--dp);font-weight:900;font-size:128px;line-height:.7;letter-spacing:-.02em;margin:0 0 14px -5px;white-space:nowrap}
.fins{display:flex;align-items:flex-end;gap:3px;height:42px}
.fins i{flex:1;height:64%;background:var(--on-y)}
.fins i.o{background:none;box-shadow:inset 0 0 0 1.5px var(--on-y)}
.fins i.t{height:100%}
.fcap{font-size:12px;font-weight:700;margin-top:3px;padding-left:calc(${TODAY - 1} / ${MONTH} * 100%);color:var(--on-y)}
.hero .l1{font-size:15px;font-weight:500;margin-top:7px;color:var(--y-mut)}
.hero .l1 b{font-weight:800;color:var(--on-y)}
.hb{height:8px;box-shadow:inset 0 0 0 1.5px var(--on-y);margin-top:7px}
.hb i{display:block;height:100%;background:var(--on-y);transform-origin:left;animation:louvre .7s cubic-bezier(.16,1,.3,1) .1s backwards}
.hero .l2{font-size:13.5px;font-weight:600;margin-top:5px;color:var(--y-mut)}

/* alert */
.due{display:flex;align-items:center;gap:10px;background:var(--r);color:var(--on-r);padding:0 12px;height:44px;flex:none;font-size:14px;font-weight:500}
.due b{font-weight:800}

/* section heads */
.sh{display:flex;align-items:flex-end;justify-content:space-between;padding:4px 2px 0;flex:none}
.sh h2{font-family:var(--dp);font-weight:800;font-size:30px;line-height:.8;padding-bottom:4px}
.sh a{font-size:13px;font-weight:700;text-decoration:underline;text-decoration-thickness:2px;text-underline-offset:3px;padding-bottom:1px}
.sh a.btn{text-decoration:none;background:var(--p);padding:6px 12px}

/* the façade */
@keyframes louvre{from{transform:scaleX(0)}}
.fac{display:flex;gap:var(--j);flex:1;min-height:96px}
.bay{position:relative;isolation:isolate;min-width:0;display:flex;flex-direction:column;justify-content:space-between;padding:8px 10px 6px;overflow:hidden}
.bay::before{content:'';position:absolute;inset:0;z-index:-1;background:var(--c);transform-origin:left;animation:louvre .7s cubic-bezier(.16,1,.3,1) calc(var(--i,0) * 60ms + 120ms) backwards}
.bay.b1{--c:var(--b);color:var(--on-b)}
.bay.b2{--c:var(--b2);color:var(--on-b2);padding:8px 8px 6px}
.bn{font-size:14px;font-weight:700;line-height:1.15}
.bn small{display:block;font-size:12px;font-weight:500;opacity:.86}
.bay b{font-family:var(--dp);font-weight:900;font-size:54px;line-height:.7;letter-spacing:-.01em;padding-bottom:6px;white-space:nowrap;margin-left:-2px}
.bay.b2 b{font-size:29px;padding-bottom:4px;margin-left:-1px}
.under{display:flex;flex-direction:column;gap:var(--j);flex:none}
.owe{display:flex;align-items:center;gap:10px;height:40px;background:var(--p);padding-right:12px}
.sliver{align-self:stretch;flex:none;background:var(--r);transform-origin:left;animation:louvre .7s cubic-bezier(.16,1,.3,1) calc(var(--i,0) * 60ms + 120ms) backwards}
.owe .bn{flex:1;display:flex;align-items:baseline;gap:8px}
.owe .bn small{color:var(--neg);font-weight:700;opacity:1}
.owe b{font-size:16px;font-weight:800;color:var(--neg)}
.tot{display:flex;align-items:center;justify-content:space-between;height:46px;background:var(--w);padding:0 12px}
.tot span{font-size:14px;font-weight:700}
.tot b{font-family:var(--dp);font-weight:900;font-size:40px;line-height:.7;padding-bottom:7px}

/* rows */
.lst{display:flex;flex-direction:column;gap:2px;flex:none}
.lst.short{margin-right:62px}
.tr{display:flex;align-items:center;gap:10px;height:56px;background:var(--p);padding-right:12px}
.tc{width:56px;align-self:stretch;display:grid;place-items:center;background:var(--p2);color:var(--ink);flex:none}
.tc.inc{background:var(--gr);color:var(--on-gr)}
.tn{flex:1;min-width:0}
.tn b{display:flex;align-items:center;gap:6px;font-size:15px;font-weight:700;line-height:1.2;white-space:nowrap}
.tn small{display:block;font-size:12.5px;color:var(--mut);margin-top:2px;white-space:nowrap}
.tv{display:flex;flex-direction:column;align-items:flex-end;gap:3px;flex:none}
.ta{font-size:16px;font-weight:800;white-space:nowrap}
.ta.inc{color:var(--pos)}
.tg{font-style:normal;font-size:10.5px;font-weight:700;line-height:1;padding:3px 5px}
.tg.e{background:var(--ink);color:var(--p)}
.tg.d{background:var(--p2);color:var(--ink)}

/* tab bar + fab */
.tabs{position:absolute;left:8px;right:8px;bottom:8px;height:58px;display:grid;grid-template-columns:repeat(5,1fr);gap:var(--j)}
.tabs span{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;background:var(--p);color:var(--mut)}
.tabs em{font-style:normal;font-size:10px;font-weight:700;letter-spacing:-.005em}
.tabs span.on{background:var(--ink);color:var(--g)}
.tabs span:active{background:var(--b);color:var(--on-b)}
.fab{position:absolute;right:8px;bottom:72px;width:56px;height:56px;background:var(--y);color:var(--on-y);display:grid;place-items:center;z-index:5}
.fab:active{background:var(--ink);color:var(--y)}

/* page headers */
.hd{display:flex;align-items:stretch;gap:var(--j);height:44px;flex:none}
.hd h1{flex:1;font-family:var(--dp);font-weight:900;font-size:42px;line-height:.8;letter-spacing:-.01em;align-self:center;padding:0 0 8px 2px;white-space:nowrap}
.sw{display:grid;grid-template-columns:repeat(4,1fr);gap:var(--j);flex:none}
.sw.s3{grid-template-columns:repeat(3,1fr)}
.sw span{height:40px;display:grid;place-items:center;background:var(--p);font-size:13.5px;font-weight:700;color:var(--mut)}
.sw span.on{background:var(--b);color:var(--on-b)}
.mo{display:flex;gap:var(--j);height:40px;flex:none}
.mo span{width:44px;display:grid;place-items:center;background:var(--p)}
.mo b{flex:1;display:flex;align-items:center;justify-content:center;background:var(--p);font-family:var(--dp);font-weight:800;font-size:25px;line-height:.8;padding-bottom:4px}
.dl{font-size:13px;font-weight:700;padding:5px 2px 0;flex:none}
.dl+.lst{margin-top:-2px}
.dl+.lst .tr{height:58px}

/* add */
.amt{background:var(--w);height:108px;flex:none;display:flex;align-items:flex-end;padding:0 12px;overflow:hidden}
.amt .num{font-family:var(--dp);font-weight:900;font-size:128px;line-height:.7;letter-spacing:-.02em;white-space:nowrap;display:flex;align-items:flex-end;padding-bottom:22px;margin-left:-4px}
.amt .rs{align-self:flex-start;line-height:1;margin-top:.1em}
.caret{width:7px;height:78px;background:var(--b);margin:0 0 -6px 8px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:var(--j);flex:1;min-height:0}
.np span{display:grid;place-items:center;background:var(--p);font-size:25px;font-weight:700}
.np span:active{background:var(--b);color:var(--on-b)}
.fl{display:flex;align-items:center;gap:10px;height:52px;background:var(--p);padding-right:12px;flex:none}
.fc{width:52px;align-self:stretch;display:grid;place-items:center;background:var(--b);color:var(--on-b);flex:none}
.fl div{flex:1}
.fl small{display:block;font-size:12px;font-weight:500;color:var(--mut);line-height:1.2}
.fl b{font-size:15.5px;font-weight:700}
.fl em{font-style:normal;font-size:13px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:var(--j);flex:none}
.cg span{height:64px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;background:var(--p);font-size:11.5px;font-weight:600;color:var(--ink)}
.cg span.on{background:var(--b);color:var(--on-b);font-weight:700}
.cg span:active{background:var(--ink);color:var(--g)}
.cta{position:absolute;left:8px;right:8px;bottom:24px}
.cta button{width:100%;height:56px;border:0;border-radius:0;background:var(--b);color:var(--on-b);display:flex;align-items:center;justify-content:center;gap:10px;font-family:var(--dp);font-weight:800;font-size:27px;line-height:1}
.cta button{padding-bottom:4px}
.cta button .ic{margin-top:4px}
.cta button:active{background:var(--ink);color:var(--g)}

/* budgets */
.per{display:flex;gap:var(--j);height:40px;flex:none}
.per b{flex:1;display:flex;align-items:center;background:var(--p);padding:0 12px;font-size:14px;font-weight:700}
.per span{display:flex;align-items:center;background:var(--y);color:var(--on-y);padding:0 12px;font-size:13.5px;font-weight:700}
.bd,.go{background:var(--p);padding:9px 12px 10px;flex:1;display:flex;flex-direction:column;justify-content:space-between;min-height:0}
.bh{display:flex;align-items:center;gap:10px}
.bh h2{font-family:var(--dp);font-weight:800;font-size:28px;line-height:.8;padding-bottom:5px;white-space:nowrap}
.bh strong{margin-left:auto;font-family:var(--dp);font-weight:900;font-size:38px;line-height:.7;padding-bottom:6px}
.stl{display:inline-flex;align-items:center;gap:4px;font-style:normal;font-size:12px;font-weight:700;line-height:1;padding:4px 7px 4px 5px;white-space:nowrap}
.stl.ok{background:var(--p2)}
.stl.ok .ic{color:var(--pos)}
.stl.nl{background:var(--r);color:var(--on-r)}
.bb{position:relative;display:flex;height:40px;box-shadow:inset 0 0 0 1px var(--line);background:var(--g)}
.bb .sp{display:block;background:var(--gr);transform-origin:left;animation:louvre .7s cubic-bezier(.16,1,.3,1) calc(var(--i,0) * 60ms + 120ms) backwards}
.bd.warn .sp{background:var(--r)}
.bb .hx{display:block;background:repeating-linear-gradient(135deg,var(--r) 0 4px,var(--w) 4px 7px)}
html[data-mode=dark] .app .bb .hx{background:repeating-linear-gradient(135deg,var(--r) 0 4px,var(--ink) 4px 7px)}
.bb u{position:absolute;top:-5px;bottom:-5px;width:3px;margin-left:-1.5px;background:var(--ink)}
.bf{display:flex;justify-content:space-between;font-size:13.5px;font-weight:500;color:var(--mut)}
.bf b{font-weight:800;color:var(--ink)}
.bf span:first-child{font-weight:700;color:var(--ink)}
.bd.warn .bf b{color:var(--neg)}
.foot{display:flex;gap:10px;align-items:stretch;background:var(--p);padding:9px 12px;font-size:13px;line-height:1.35;color:var(--mut);flex:none}
.foot u{flex:none;width:3px;background:var(--ink)}

/* desktop */
.pier{position:absolute;left:0;top:0;bottom:0;width:220px;background:var(--p);padding:8px;display:flex;flex-direction:column}
.brand{display:flex;align-items:center;gap:10px;height:60px;padding:0 6px}
.brand b{font-family:var(--dp);font-weight:900;font-size:50px;line-height:.7;padding-bottom:10px;letter-spacing:-.02em}
.addb{display:flex;align-items:center;gap:8px;height:52px;padding:0 12px 4px;background:var(--b);color:var(--on-b);font-family:var(--dp);font-weight:800;font-size:24px;line-height:1;margin:4px 0 12px}
.addb .ic{margin-top:4px}
.addb:active{background:var(--ink);color:var(--p)}
.pier nav{display:flex;flex-direction:column;gap:2px}
.ni{display:flex;align-items:center;gap:10px;height:40px;padding:0 12px;background:var(--p2);font-size:14px;font-weight:700;color:var(--ink)}
.ni.on{background:var(--ink);color:var(--p)}
.gl{font-family:var(--dp);font-weight:800;font-size:28px;line-height:.8;margin:26px 0 6px;padding:0 6px 4px}
.tl{font-size:14px;font-weight:500;padding:6px 6px;color:var(--ink)}
.mn{position:absolute;left:220px;right:0;top:0;bottom:0;padding:8px;display:grid;grid-template-columns:620px 1fr;grid-template-rows:44px 400px 1fr;gap:var(--j)}
.mn .top{grid-column:1/-1}
.mn .who{justify-content:flex-start;gap:16px}
.dt{display:flex;align-items:center;background:var(--p);padding:0 14px;font-size:13.5px;font-weight:700}
.hero.big{padding:18px 24px 18px;display:flex;flex-direction:column}
.hero.big h2{font-size:44px}
.hero.big .num{font-size:206px;margin:6px 0 26px -9px}
.hero.big .fins{height:78px;gap:6px}
.hero.big .fcap{font-size:13px;margin-top:5px}
.hero.big .l1{font-size:19px;margin-top:auto}
.hero.big .hb{height:10px;margin-top:9px}
.hero.big .l2{font-size:15px;margin-top:7px}
.cR,.cS,.cG{display:flex;flex-direction:column;gap:var(--j);min-height:0;min-width:0}
.cR .due{height:50px}
.cR .lst{flex:1}
.cR .tr{flex:1;height:auto}
.cS .under{flex-direction:row}
.cS .under>*{flex:1;height:50px}
.cS .bay{padding:12px 14px 10px}
.cS .bay b{font-size:80px;padding-bottom:10px;margin-left:-3px}
.cS .bay.b2 b{font-size:32px;padding-bottom:6px;margin-left:-2px}
.cS .bn{font-size:15px}
.cS .bn small{font-size:13px}
.go .bb{height:34px}
.go .bh h2{font-size:30px}
`;

export const screens = { home, txns, add, budgets, desk };
