import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 2,
  slug: 'khata',
  name: 'Khata',
  tagline: 'The accountant’s ledger. Columnar green paper, blue-black ink, a red margin rule — figures are set the way a bookkeeper writes them, rupees in one column and paise in the next.',
  fonts: 'https://fonts.googleapis.com/css2?family=Source+Serif+4:opsz,wght@8..60,400;8..60,600;8..60,700&family=Source+Sans+3:wght@400;500;600&display=swap',
  swatch: ['#e3ecd9', '#14234a', '#b8322a', '#1b6a42'],
  swatchDark: ['#0f1b17', '#e4eede', '#ff7b6c', '#6fd79d'],
  notes: [
    ['Shape', 'Near-square. 2px corners, 1px ink rules, double rules under headings. Nothing floats, nothing is frosted.'],
    ['Type', 'Source Serif 4 for every name and figure, Source Sans 3 for labels. Tabular, right-aligned columns.'],
    ['Density', 'Compact — 40px rows, ruled like a page. The most information per screen of the five themes.'],
    ['Motion', 'Pen-like and brief: tally strokes draw in left to right, the stamp lands with a 3° tilt. Nothing bounces.'],
    ['Voice', 'Plain bookkeeping. Totals are underlined once and double-underlined at the foot, as on paper.'],
    ['Signature', 'The ₹ | paise split column behind a red double margin rule, and a five-bar tally gate for every budget.'],
  ],
};

const m = (s) => `<span class="amt"><span class="r">${s}</span><span class="p">00</span></span>`;
const tally = (pct) => {
  const on = Math.round((pct / 100) * 40);
  let h = '';
  for (let g = 0; g < 8; g++) {
    const full = on >= (g + 1) * 5;
    let ticks = '';
    for (let t = 0; t < 5; t++) ticks += `<i${g * 5 + t < on ? ' class="f"' : ''}></i>`;
    h += `<span class="g${full ? ' full' : ''}">${ticks}</span>`;
  }
  return `<div class="tally" role="img" aria-label="${pct}% used">${h}</div>`;
};
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => `<span${n === active ? ' class="on"' : ''}>${ic(i, 20, 1.75)}${n}</span>`).join('')}</nav>`;
const hd = (title, left = '', right = '') =>
  `<header class="hd">${left}<div class="ti"><h1>${title}</h1></div><div class="rt">${right}</div></header>`;
const ib = (i) => `<span class="ib">${ic(i, 18, 1.75)}</span>`;

const row = (r, date) => `<div class="rw"><span class="dt">${date}</span><div class="nm"><b>${r.name}${r.rec ? ` <span class="rp">${ic('repeat', 12, 2)}</span>` : ''}</b><small>${r.cat} · ${r.acct}${r.tag ? ` <span class="tag">${r.tag}</span>` : ''}</small></div><div class="${r.inc ? 'pos' : ''}">${r.inc ? '+' : '−'}${m(r.amt)}</div></div>`;

const home = `
<div class="pg">
  ${hd(D.user, '', ib('eye') + ib('sliders')).replace('<h1>' + D.user + '</h1>', `<h1>${D.user}</h1><p>${ic('shield', 12, 2)} Kept only on this device</p>`)}
  <section class="tot">
    <div class="lab">Safe to spend today</div>
    <div class="big"><span class="cur">₹</span><span class="r">${D.safe}</span><span class="p">00</span></div>
    <div class="ld"><span>Left this month</span><i></i><b>₹${D.left}</b></div>
    <div class="ld"><span>Days to go</span><i></i><b>${D.days}</b></div>
    <div class="ld"><span>Spent</span><i></i><b>₹${D.spent} of ₹${D.budget}</b></div>
    ${tally(D.pct)}
  </section>
  <div class="due"><span class="stamp">DUE IN ${D.due.days} DAYS</span><span>${D.due.name} payment · <b>₹${D.due.amt}</b></span></div>
  <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
  <div class="tb">${D.accounts
    .map((a) => `<div class="ac"><div><b>${a.name}</b><small>${a.type}</small></div>${a.owed ? '<span class="neg">−' + m(a.bal) + '</span>' : m(a.bal)}</div>`)
    .join('')}
    <div class="ac sum"><b>Total balance</b>${m(D.total)}</div>
  </div>
  <div class="sh"><h2>Latest</h2><a>See all</a></div>
  <div class="tb">${row(D.groups[0].rows[1], '2 Oct')}${row(D.groups[1].rows[0], '27 Sep')}</div>
</div>
<span class="fab">${ic('plus', 24, 2.2)}</span>
${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search') + ib('eye'))}
  <div class="idx"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span>${ic('chevl', 16, 2)}</span><b>October 2026</b><span>${ic('chevr', 16, 2)}</span></div>
  <div class="colh"><span>Date</span><span>Particulars</span><span>Amount ₹ | p</span></div>
  <div class="tb">${D.groups.map((g) => g.rows.map((r) => row(r, g.date.split(', ')[1])).join('')).join('')}</div>
</div>
<span class="fab">${ic('plus', 24, 2.2)}</span>
${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back'), '')}
  <div class="idx"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtbox"><small>Amount</small><div class="big"><span class="cur">₹</span><span class="r">2,450</span><span class="p">00</span></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fld"><small>Account</small><div class="sel"><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 16, 2)}</div></div>
  <div class="sh"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.75)}${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back'), ib('eye') + ib('plus'))}
  ${D.budgets
    .map(
      (b) => `<section class="bg">
    <div class="bh"><div><h3>${b.name}</h3><small>Monthly · October 2026</small></div><span class="st ${b.pct > 85 ? 'warn' : 'ok'}">${b.state}</span></div>
    <div class="ld"><span>₹${b.spent} of ₹${b.limit}</span><i></i><b>${b.pct}%</b></div>
    ${tally(b.pct)}
    <div class="bf">₹${b.left} left this month</div>
  </section>`,
    )
    .join('')}
</div>
${tabs('Settings').replace('class="on"', '')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span class="mk">F</span>Finio</div>
  <span class="addb">${ic('plus', 18, 2.2)} Add transaction</span>
  ${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 18, 1.75)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni">${t}</span>`).join('')}
</aside>
<main class="mn">
  <div class="dh"><div><h1>${D.user}</h1><p>${ic('shield', 13, 2)} Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></div>
  <section class="tot wide">
    <div><div class="lab">Safe to spend today</div><div class="big"><span class="cur">₹</span><span class="r">${D.safe}</span><span class="p">00</span></div></div>
    <div class="side">
      <div class="ld"><span>Left this month</span><i></i><b>₹${D.left}</b></div>
      <div class="ld"><span>Days to go</span><i></i><b>${D.days}</b></div>
      <div class="ld"><span>Spent</span><i></i><b>₹${D.spent} of ₹${D.budget}</b></div>
    </div>
    <div class="tl">${tally(D.pct)}</div>
  </section>
  <div class="due"><span class="stamp">DUE IN ${D.due.days} DAYS</span><span>${D.due.name} payment · <b>₹${D.due.amt}</b></span></div>
  <div class="cols">
    <div>
      <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
      <div class="tb">${D.accounts.map((a) => `<div class="ac"><div><b>${a.name}</b><small>${a.type}</small></div>${a.owed ? '<span class="neg">−' + m(a.bal) + '</span>' : m(a.bal)}</div>`).join('')}<div class="ac sum"><b>Total balance</b>${m(D.total)}</div></div>
      <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
      <div class="tb">${D.goals.map((g) => `<div class="gr"><div class="ld"><b>${g.name}</b><i></i><span>₹${g.have} / ₹${g.target}</span></div>${tally(g.pct)}</div>`).join('')}</div>
    </div>
    <div>
      <div class="sh"><h2>Latest</h2><a>See all</a></div>
      <div class="tb">${D.groups.map((g) => g.rows.map((r) => row(r, g.date.split(', ')[1])).join('')).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{--paper:#e3ecd9;--sheet:#eef4e6;--rule:#b4c9ab;--ink:#14234a;--mut:#46556e;--red:#b8322a;--redtxt:#fff;--green:#1b6a42;--bar:#14234a;--barink:#e8f0e0;--warn:#8a5a00;
  background:var(--paper);color:var(--ink);font-family:'Source Sans 3',system-ui,sans-serif;font-size:14px;font-variant-numeric:tabular-nums;
  --ser:'Source Serif 4',Georgia,serif}
html[data-mode=dark] .app{--paper:#0f1b17;--sheet:#15241f;--rule:#2a4036;--ink:#e4eede;--mut:#9db2a4;--red:#ff7b6c;--redtxt:#1a0e0c;--green:#6fd79d;--bar:#09110e;--barink:#dfe9d8;--warn:#f0c15a}
.app ::selection{background:var(--red);color:var(--redtxt)}
.app .pg{position:absolute;inset:44px 0 84px;padding:6px 16px;overflow:hidden}
.hd{display:flex;align-items:flex-end;gap:10px;padding:8px 0 10px;border-bottom:3px double var(--ink);margin-bottom:12px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-family:var(--ser);font-size:26px;font-weight:700;letter-spacing:-.01em;line-height:1.1}
.hd p,.dh p{font-size:12.5px;color:var(--mut);display:flex;gap:5px;align-items:center;margin-top:2px}
.hd .rt,.dh .rt{display:flex;gap:6px}
.ib{width:36px;height:36px;border:1px solid var(--ink);border-radius:2px;display:grid;place-items:center;background:var(--sheet)}
.tot{padding:10px 0 12px}
.tot .lab{font-size:13px;color:var(--mut);margin-bottom:2px}
.big{display:inline-flex;align-items:baseline;font-family:var(--ser);font-weight:700;color:var(--ink);border-top:1px solid var(--ink);border-bottom:3px double var(--ink);padding:2px 4px 0 0;margin-bottom:10px}
.big .cur{font-size:26px;margin-right:4px;font-weight:600}
.big .r{font-size:56px;letter-spacing:-.02em;line-height:1.1}
.big .p{font-size:22px;color:var(--mut);font-weight:400;border-left:1.5px solid var(--red);margin-left:8px;padding-left:8px}
.ld{display:flex;align-items:baseline;gap:6px;padding:3px 0}
.ld i{flex:1;border-bottom:1.5px dotted var(--mut);opacity:.55;transform:translateY(-3px)}
.ld b{font-family:var(--ser);font-weight:600}
.tally{display:flex;gap:6px;height:20px;margin-top:8px}
.tally .g{flex:1;display:flex;justify-content:space-between;position:relative}
.tally i{width:2px;background:var(--rule);border-radius:1px}
.tally i.f{background:var(--ink);animation:pen .5s cubic-bezier(.16,1,.3,1) backwards}
.tally .g:nth-child(2) i.f{animation-delay:.08s}.tally .g:nth-child(3) i.f{animation-delay:.16s}.tally .g:nth-child(4) i.f{animation-delay:.24s}
@keyframes pen{from{transform:scaleY(0);transform-origin:top}}
.tally .g.full::after{content:'';position:absolute;inset:-1px -3px;background:linear-gradient(to top right,transparent calc(50% - 1px),var(--ink) calc(50% - 1px),var(--ink) calc(50% + 1px),transparent calc(50% + 1px))}
.due{display:flex;align-items:center;gap:12px;padding:8px 0 10px;border-top:1px solid var(--rule);border-bottom:1px solid var(--rule);font-size:13.5px}
.stamp{border:2px solid var(--red);color:var(--red);border-radius:3px;padding:1px 8px;font-weight:700;font-size:11.5px;letter-spacing:.07em;transform:rotate(-3deg);white-space:nowrap;animation:stamp .4s cubic-bezier(.16,1,.3,1) .2s backwards}
@keyframes stamp{from{transform:rotate(-3deg) scale(1.5);opacity:0}}
.sh{display:flex;justify-content:space-between;align-items:baseline;border-bottom:1px solid var(--ink);margin:14px 0 2px;padding-bottom:3px}
.sh h2{font-family:var(--ser);font-size:16.5px;font-weight:600}
.sh a{font-size:12.5px;color:var(--mut);text-decoration:underline;text-underline-offset:3px}
.amt{display:inline-flex;font-family:var(--ser);font-weight:600}
.amt .r{min-width:62px;text-align:right}
.amt .p{border-left:1.5px solid var(--red);margin-left:6px;padding-left:6px;color:var(--mut);font-weight:400}
.ac{display:flex;justify-content:space-between;align-items:center;padding:7px 0;border-bottom:1px solid var(--rule)}
.ac b{font-family:var(--ser);font-weight:600;font-size:15px;display:block}
.ac small,.nm small{font-size:12px;color:var(--mut)}
.ac.sum{border-top:1px solid var(--ink);border-bottom:3px double var(--ink);margin-top:2px}
.neg{color:var(--red)}.pos{color:var(--green)}
.rw{display:grid;grid-template-columns:50px 1fr auto;gap:10px;align-items:center;padding:7px 0;border-bottom:1px solid var(--rule)}
.rw .dt{font-size:12px;color:var(--mut);align-self:stretch;display:flex;align-items:center;border-right:3px double var(--red);padding-right:8px;white-space:nowrap}
.rw .nm b{font-family:var(--ser);font-weight:600;font-size:15px;display:block;line-height:1.25}
.rw .rp{color:var(--mut);vertical-align:-1px}
.tag{border:1px solid currentColor;border-radius:2px;padding:0 5px;font-size:11px;margin-left:3px;white-space:nowrap}
.rw>div:last-child{font-family:var(--ser);font-weight:600}
.colh{display:grid;grid-template-columns:50px 1fr auto;gap:10px;font-size:11.5px;color:var(--mut);padding:4px 0 3px;border-bottom:3px double var(--ink)}
.idx{display:flex;border-bottom:1px solid var(--ink);margin:0 0 10px}
.idx span{padding:7px 14px;border:1px solid var(--rule);border-bottom:0;margin-right:-1px;font-size:13.5px;font-weight:500;color:var(--mut);background:var(--sheet)}
.idx span.on{background:var(--ink);color:var(--paper);border-color:var(--ink)}
.mo{display:flex;align-items:center;justify-content:space-between;padding:2px 0 8px;font-family:var(--ser);font-weight:600;font-size:15px}
.mo span{width:30px;height:30px;border:1px solid var(--rule);display:grid;place-items:center;border-radius:2px;background:var(--sheet)}
.tabs{position:absolute;left:0;right:0;bottom:0;height:84px;background:var(--bar);color:var(--barink);display:grid;grid-template-columns:repeat(5,1fr);padding-bottom:22px}
.tabs span{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;font-size:11.5px;font-weight:500;opacity:.8}
.tabs span.on{background:var(--red);color:var(--redtxt);opacity:1}
.fab{position:absolute;right:16px;bottom:100px;width:54px;height:54px;border:1px solid var(--ink);border-radius:3px;background:var(--red);color:var(--redtxt);display:grid;place-items:center;box-shadow:0 8px 16px -8px rgb(10 20 50 / .55);z-index:5}
.amtbox{border:1px solid var(--ink);background:var(--sheet);padding:6px 12px 2px;margin-bottom:10px}
.amtbox small{font-size:12px;color:var(--mut)}
.amtbox .big{display:flex;justify-content:flex-end;border-top:0;margin:0}
.np{display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--ink);background:var(--sheet);margin-bottom:10px}
.np span{height:50px;display:grid;place-items:center;font-family:var(--ser);font-size:25px;font-weight:600;border-right:1px solid var(--rule);border-bottom:1px solid var(--rule)}
.np span:nth-child(3n){border-right:0}.np span:nth-child(n+10){border-bottom:0}
.fld small{font-size:12px;color:var(--mut)}
.sel{display:flex;align-items:center;gap:8px;border:1px solid var(--ink);background:var(--sheet);padding:9px 12px;margin-top:3px}
.sel b{font-family:var(--ser);font-weight:600;font-size:15px}.sel span{flex:1;font-size:12px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid var(--rule);margin-top:6px;background:var(--sheet)}
.cg span{display:flex;flex-direction:column;align-items:center;gap:4px;padding:10px 2px;font-size:11.5px;border-right:1px solid var(--rule);border-bottom:1px solid var(--rule)}
.cg span:nth-child(4n){border-right:0}.cg span:nth-child(n+5){border-bottom:0}
.cg span.on{background:var(--ink);color:var(--paper)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 16px 30px;background:var(--paper);border-top:1px solid var(--ink)}
.cta button{width:100%;height:48px;border:0;border-radius:2px;background:var(--ink);color:var(--paper);font-family:var(--ser);font-size:16.5px;font-weight:600}
.bg{padding:8px 0 10px;border-bottom:1px solid var(--rule)}
.bh{display:flex;justify-content:space-between;align-items:flex-start}
.bh h3{font-family:var(--ser);font-size:17px;font-weight:600}.bh small{font-size:12px;color:var(--mut)}
.st{border:1.5px solid currentColor;border-radius:3px;padding:0 7px;font-size:11.5px;font-weight:700;transform:rotate(-2deg)}
.st.ok{color:var(--green)}.st.warn{color:var(--warn)}
.bf{font-size:12.5px;color:var(--mut);margin-top:5px}
/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:232px;background:var(--bar);color:var(--barink);padding:22px 14px;display:flex;flex-direction:column;gap:2px}
.logo{font-family:var(--ser);font-size:23px;font-weight:700;display:flex;align-items:center;gap:10px;margin:0 6px 16px}
.mk{width:32px;height:32px;display:grid;place-items:center;background:var(--red);color:var(--redtxt);border-radius:2px}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:42px;border:1px solid var(--barink);border-radius:2px;font-weight:600;margin-bottom:12px}
.ni{display:flex;align-items:center;gap:10px;padding:9px 12px;border-radius:2px;font-weight:500;opacity:.85}
.ni.on{background:var(--red);color:var(--redtxt);opacity:1}
.gl{font-size:12px;opacity:.6;padding:14px 12px 4px}
.mn{position:absolute;left:232px;right:0;top:0;bottom:0;padding:26px 40px;overflow:hidden}
.dh{display:flex;justify-content:space-between;align-items:flex-end;border-bottom:3px double var(--ink);padding-bottom:10px}
.dh h1{font-family:var(--ser);font-size:30px;font-weight:700}
.tot.wide{display:grid;grid-template-columns:auto 1fr;gap:6px 56px;align-items:end;padding:14px 0}
.tot.wide .side{padding-bottom:12px}.tot.wide .tl{grid-column:1/-1}
.mn .big .r{font-size:72px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 44px}
.gr{padding:6px 0 8px;border-bottom:1px solid var(--rule)}
`;

export const screens = { home, txns, add, budgets, desk };
