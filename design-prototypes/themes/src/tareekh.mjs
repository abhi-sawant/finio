import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 11,
  slug: 'tareekh',
  name: 'Tareekh',
  tagline: 'The wall calendar by the fridge. Big date numerals, Sundays in red, a note in every box — for people who think about money by the date: what came in on the 2nd, what is due on the 18th, what is left for each day ahead.',
  blurb: 'The household wall calendar. White coated paper, black ink, calendar red and postal blue; the month drawn as a real grid with today ringed and every day ahead carrying its allowance.',
  fonts: 'https://fonts.googleapis.com/css2?family=Anek+Latin:wdth,wght@75..125,100..800&display=swap',
  swatch: ['#fcfdfe', '#0c0e12', '#d51f2a', '#1746b3'],
  swatchDark: ['#0a0f1f', '#edefe8', '#ff4a4d', '#86acff'],
  notes: [
    ['Shape', 'Ruled rectangular modules packed edge to edge on shared 1px rules, each opened by a small filled header tab. 0px radii, no shadows, no gaps between modules.'],
    ['Type', 'Anek Latin only, worked through its width axis: 75–80% condensed for labels and tabs (11–12px), 110–120% wide at weight 800 for date numerals and the ₹1,562 figure. Tabular numerals throughout.'],
    ['Density', 'The densest theme: hero, month grid, alert, accounts and latest all on one phone screen; budgets as four-cell ruled strips; desktop tiles seven modules with no dead gutter.'],
    ['Motion', 'Days 1–14 tick in left to right at 40ms a cell, then the red ring is drawn once around the 15th (stroke-dashoffset, 0.6s ease-out). Nothing else moves.'],
    ['Voice', 'Dates do the talking, and only true ones: “Day 15 of 31”, “Due in 3 days”, “₹1,000 left with 16 days to go”. Product labels stay verbatim.'],
    ['Signature', 'The month grid: October 2026 as a real Sunday-first calendar — 2 Oct carries the salary and the rent, today is ringed, and each of the 16 days ahead shows its ₹1,562.'],
  ],
};

/* ---------- helpers ---------- */
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 18, 1.75)}</span>`;
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 19, 1.75)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 24, 2.25)}</span>`;
const tb = (title, tone = '', right = '') => `<div class="tb ${tone}"><h2>${title}</h2>${right}</div>`;
const WD = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const sal = D.groups[0].rows[0];
const rent = D.groups[0].rows[1];

const ring = `<svg class="ring" viewBox="0 0 44 30" preserveAspectRatio="none" aria-hidden="true"><path pathLength="1" d="M31 4.5C22 1 6 3 3.5 13.5c-2 9.5 12 14.500 24 13C38 25 43 17 40 10 37.5 4.5 29 2.5 20 3.5"/></svg>`;

/* October 2026: the 1st is a Thursday, weeks start on Sunday */
const cal = (big = false) => {
  const R = big ? '₹' : '';
  let h = WD.map((w, i) => `<span class="wd${i === 0 ? ' sun c0' : ''}">${w}</span>`).join('');
  h += `<div class="c lg c0 r0"><p><i class="k out"></i>Out<i class="k in"></i>In<i class="k no"></i>No spend</p><p class="l2">Days ahead: ₹${D.safe} a day</p></div>`;
  for (let d = 1; d <= 31; d++) {
    const idx = d + 3;
    const col = idx % 7;
    const rw = Math.floor(idx / 7);
    const st = d < 15 ? 'past' : d === 15 ? 'today' : 'next';
    let inner;
    if (d === 2)
      inner = `<span class="am in">${big ? '<small>In</small>' : ''}+${R}${sal.amt}</span><span class="am">${big ? '<small>Out</small>' : ''}−${R}${rent.amt}</span><i class="mk"></i>`;
    else if (st === 'past') inner = `<i class="nm"></i>`;
    else if (st === 'today') inner = `<span class="td">Today</span>`;
    else inner = `<span class="al">${R}${D.safe}</span>`;
    h += `<div class="c ${st}${col === 0 ? ' sun c0' : ''}${rw === 0 ? ' r0' : ''}" style="--i:${d}"><b>${d}${d === 15 ? ring : ''}</b>${inner}</div>`;
  }
  return `<div class="cal${big ? ' big' : ''}" role="img" aria-label="October 2026. 2 October: salary in ₹${sal.amt}, rent out ₹${rent.amt}. Today is the 15th. ₹${D.safe} a day for the ${D.days} days ahead.">${h}</div>`;
};

const leaf = `<div class="leaf"><small>Oct</small><b>15</b><em>Thu</em></div>`;
const figure = `<div class="fg"><div class="n">₹${D.safe}</div><p>₹${D.left} left this month · ${D.days} days to go</p></div>`;
const dbar = (pct, warn = false, flag = false) =>
  `<div class="dbar${warn ? ' w' : ''}"><i style="width:${pct}%"></i><u style="left:${D.dayPct}%"></u>${flag ? `<em style="left:${D.dayPct}%">Day 15 of 31</em>` : ''}</div>`;
const spend = `<div class="sp"><div class="spl"><span>Spent <b>₹${D.spent}</b> of ₹${D.budget}</span><b>${D.pct}%</b></div>${dbar(D.pct, false, true)}</div>`;
const due = `<div class="due">${ic('alert', 15, 2)}<b>Due in ${D.due.days} days</b><span>${D.due.name} payment of <strong>₹${D.due.amt}</strong></span>${ic('chevr', 15, 2)}</div>`;

const accounts = `<section class="md ac">${tb('Where it sits', 'b', '<a>See all</a>')}
  <div class="acr">${D.accounts
    .map((a) => `<div class="ar"><b>${a.name}</b><small>${a.type}</small><span class="v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`)
    .join('')}</div>
  <div class="at"><b>Total balance</b><span>₹${D.total}</span></div>
</section>`;

const row = (r) =>
  `<div class="rw"><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 2) : ''}</b><small>${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const grp = (g, rows = g.rows) => {
  const [wd, rest] = g.date.split(', ');
  const [d, m] = rest.split(' ');
  return `<div class="grp"><div class="dlf${wd === 'Sun' ? ' sun' : ''}"><b>${d}</b><em>${wd} · ${m}</em></div><div class="grs">${rows.map(row).join('')}</div></div>`;
};

const top = `<header class="tp"><div class="who"><h1>${D.user}</h1><p>${ic('shield', 12, 2)}Kept only on this device</p></div>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</header>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;

/* ---------- screens ---------- */
const home = `
<div class="pg home">
  ${top}
  <section class="md hero">${tb('Safe to spend today', 'r', '<span>October 2026</span>')}
    <div class="hb">${leaf}${figure}</div>
    ${spend}
    ${cal()}
  </section>
  ${due}
  ${accounts}
  <section class="md lat">${tb('Latest', '', '<a>See all</a>')}${grp(D.groups[0], [rent])}</section>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span>${ic('chevl', 16, 2)}</span><b>October 2026</b><span>${ic('chevr', 16, 2)}</span></div>
  <div class="lst">${D.groups.map((g) => grp(g)).join('')}</div>
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg addp">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty t3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amt"><span class="cur">₹</span><span class="n">2,450</span><i class="caret"></i></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)].map((k) => `<span>${k}</span>`).join('')}</div>
  <section class="md fld">${tb('Account', 'b')}<div class="fl"><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 16, 2)}</div></section>
  <section class="md">${tb('Category', '', '<a>Split</a>')}
    <div class="cg">${D.cats.map(([nm, i], k) => `<span class="${k === 0 ? 'on ' : ''}k${k % 4} q${k < 4 ? 0 : 1}">${ic(i, 19, 1.75)}<em>${nm}</em></span>`).join('')}</div>
  </section>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><b>Monthly · October 2026</b><span>Day 15 of 31</span></div>
  ${D.budgets
    .map((b) => {
      const warn = b.pct > 85;
      return `<section class="md bd${warn ? ' warn' : ''}">${tb(b.name, warn ? 'r' : 'b', `<span class="st ${warn ? 'wn' : 'ok'}">${ic(warn ? 'alert' : 'check', 13, 2.25)}${b.state}</span>`)}
    <div class="bc"><div class="pc"><b>${b.pct}%</b></div><div><small>Spent</small><b>₹${b.spent}</b></div><div><small>Limit</small><b>₹${b.limit}</b></div><div><small>Left</small><b>₹${b.left}</b></div></div>
    <div class="bw">${dbar(b.pct, warn, true)}</div>
    ${warn ? `<p class="nl">${ic('alert', 14, 2)}<b>${b.state}</b><span>₹${b.left} left with ${D.days} days to go</span></p>` : ''}
  </section>`;
    })
    .join('')}
</div>
${tabs()}`;

const desk = `
<aside class="sd">
  <div class="mark"><span class="lf"><i></i><b>F</b></span><strong>Finio</strong></div>
  <span class="addb">${ic('plus', 16, 2.25)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 16, 1.75)}${nm}</span>`).join('')}</nav>
  <div class="tls">${tb('Tools')}${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}</div>
  <p class="pv">${ic('shield', 13, 2)}Kept only on this device</p>
</aside>
<main class="mn">
  <header class="tp"><div class="who"><h1>${D.user}</h1><p>Thursday, 15 October 2026</p></div>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</header>
  <section class="md hero">${tb('Safe to spend today', 'r', '<span>October 2026</span>')}
    <div class="hw">
      <div class="hl">
        <div class="hb">${leaf}${figure}</div>
        <div class="q4"><div><small>Left this month</small><b>₹${D.left}</b></div><div><small>Days to go</small><b>${D.days}</b></div><div><small>Spent</small><b>₹${D.spent}</b></div><div><small>Budget</small><b>₹${D.budget}</b></div></div>
        ${spend}
        ${due}
      </div>
      ${cal(true)}
    </div>
  </section>
  <div class="tiles">
    ${accounts}
    <section class="md gl">${tb('Savings goals', 'b', '<a>See all</a>')}
      ${D.goals.map((g) => `<div class="gr"><div class="gh"><b>${g.name}</b><span>${g.pct}%</span></div><div class="dbar g"><i style="width:${g.pct}%"></i></div><small>₹${g.have} of ₹${g.target}</small></div>`).join('')}
    </section>
    <section class="md lat">${tb('Latest', '', '<a>See all</a>')}${D.groups.map((g) => grp(g)).join('')}</section>
  </div>
</main>`;

export const css = `
.app{--paper:#fcfdfe;--ink:#0c0e12;--line:#0c0e12;--mut:#4a5362;--rule:#c3cad5;--tint:#eef1f5;--red:#d51f2a;--red-d:#a8131d;--on-red:#fff;--redt:#fdecee;--blue:#1746b3;--on-blue:#fff;
  background:var(--paper);color:var(--ink);font-family:'Anek Latin',sans-serif;font-stretch:88%;font-size:13px;line-height:1.2;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--red)}
html[data-mode=dark] .app{--paper:#0a0f1f;--ink:#edefe8;--line:#56628a;--mut:#9ca7c2;--rule:#27314f;--tint:#111931;--red:#ff4a4d;--red-d:#c9282f;--on-red:#0a0f1f;--redt:#2a1422;--blue:#86acff;--on-blue:#0a0f1f}
.app ::selection{background:var(--red);color:var(--on-red)}
.app :focus-visible{outline:2px solid var(--blue);outline-offset:2px}
.app h1,.app h2,.app b,.app strong{font-weight:700}
.app small,.app em{font-style:normal}
.app .pg{position:absolute;inset:44px 0 64px;padding:0 12px 12px;display:flex;flex-direction:column;overflow:hidden}

/* boxed icon buttons */
.ib{width:34px;height:34px;display:grid;place-items:center;border:1px solid var(--line);color:var(--ink);flex:none;margin-left:-1px}
.ib:active{background:var(--ink);color:var(--paper)}

/* top bar */
.tp{display:flex;align-items:center;height:44px;flex:none}
.who{flex:1;display:flex;align-items:baseline;gap:10px;min-width:0}
.who h1{font-size:23px;font-weight:800;font-stretch:112%;line-height:1;letter-spacing:-.01em}
.who p{display:flex;align-items:center;gap:4px;font-size:12px;font-stretch:80%;color:var(--mut);white-space:nowrap}
.who p .ic{color:var(--blue)}

/* modules + header tabs */
.md{border:1px solid var(--line);margin-top:-1px;position:relative;flex:none;min-width:0}
.tb{display:flex;align-items:stretch;justify-content:space-between;height:21px;border-bottom:1px solid var(--line);flex:none}
.tb h2{background:var(--ink);color:var(--paper);font-size:12px;font-weight:700;font-stretch:80%;text-transform:uppercase;letter-spacing:.055em;line-height:21px;padding:0 9px;white-space:nowrap}
.tb.r h2{background:var(--red);color:var(--on-red)}
.tb.b h2{background:var(--blue);color:var(--on-blue)}
.tb a{font-size:12px;font-weight:600;font-stretch:80%;color:var(--blue);line-height:20px;padding:0 8px;text-decoration:underline;text-underline-offset:2px;text-decoration-thickness:1px}
.tb>span{font-size:12px;font-weight:600;font-stretch:80%;color:var(--mut);line-height:20px;padding:0 8px;text-transform:uppercase;letter-spacing:.05em}

/* hero */
.hero{display:flex;flex-direction:column;flex:1;min-height:0}
.hb{display:grid;grid-template-columns:62px 1fr;border-bottom:1px solid var(--line);flex:none}
.leaf{border-right:1px solid var(--line);display:flex;flex-direction:column;align-items:center;text-align:center}
.leaf small{align-self:stretch;background:var(--red);color:var(--on-red);font-size:11px;font-weight:700;font-stretch:80%;text-transform:uppercase;letter-spacing:.1em;line-height:15px}
.leaf b{font-size:38px;font-weight:800;font-stretch:118%;line-height:.9;margin-top:7px;letter-spacing:-.02em}
.leaf em{font-size:12px;font-weight:700;font-stretch:80%;text-transform:uppercase;letter-spacing:.12em;margin:1px 0 4px}
.fg{padding:6px 10px 5px;min-width:0}
.fg .n{font-size:50px;font-weight:800;font-stretch:108%;line-height:.92;letter-spacing:-.02em;white-space:nowrap}
.fg p{font-size:13.5px;font-stretch:82%;font-weight:500;margin-top:3px;white-space:nowrap}
.sp{padding:5px 10px 20px;border-bottom:1px solid var(--line);flex:none}
.spl{display:flex;justify-content:space-between;align-items:baseline;font-size:13px;font-stretch:82%;margin-bottom:4px}
.spl>b{font-size:15px;font-weight:800;font-stretch:100%}

/* day bar: 31 day slots, fill = used, marker = today */
.dbar{position:relative;height:11px;border:1px solid var(--line);background:var(--tint)}
.dbar i{position:absolute;left:0;top:0;bottom:0;background:var(--blue)}
.dbar::after{content:'';position:absolute;inset:0;background:repeating-linear-gradient(90deg,transparent 0 calc(100%/31 - 1px),var(--paper) calc(100%/31 - 1px) calc(100%/31));opacity:.75}
.dbar.w i{background:repeating-linear-gradient(135deg,var(--red) 0 4px,var(--red-d) 4px 6px)}
.dbar u{position:absolute;top:-4px;bottom:-5px;width:2px;margin-left:-1px;background:var(--ink);z-index:2}
.dbar em{position:absolute;top:13px;transform:translateX(-50%);font-size:11px;font-weight:600;font-stretch:78%;text-transform:uppercase;letter-spacing:.05em;white-space:nowrap;color:var(--ink)}
.dbar.g{height:9px}.dbar.g::after{background:repeating-linear-gradient(90deg,transparent 0 calc(10% - 1px),var(--paper) calc(10% - 1px) 10%)}

/* the month grid */
.cal{display:grid;grid-template-columns:repeat(7,1fr);grid-template-rows:17px repeat(5,minmax(0,1fr));flex:1;min-height:0}
.wd{font-size:11px;font-weight:700;font-stretch:80%;text-transform:uppercase;letter-spacing:.08em;line-height:17px;padding:0 4px;border-bottom:1px solid var(--line);border-left:1px solid var(--rule);background:var(--tint)}
.c{position:relative;border-left:1px solid var(--rule);border-top:1px solid var(--rule);padding:4px 3px 0 4px;min-width:0;display:flex;flex-direction:column;align-items:flex-start;line-height:1}
.c0{border-left:0}.c.r0{border-top:0}
.c>b{position:relative;font-size:15px;font-weight:800;font-stretch:112%;line-height:1;margin-bottom:2px}
.sun,.c.sun>b{color:var(--red)}
.c.past>b{color:var(--mut)}.c.past.sun>b{color:var(--red);opacity:.75}
.c .am{font-size:13px;font-weight:700;font-stretch:75%;line-height:1.02;white-space:nowrap;animation:tick .22s ease-out calc(var(--i)*40ms) backwards}
.c .am.in{color:var(--blue)}
.c .am small{font-size:11px;font-weight:600;color:var(--mut);margin-right:4px;text-transform:uppercase;letter-spacing:.04em}
.c .mk{position:absolute;left:0;right:0;bottom:0;height:4px;background:var(--red);transform-origin:left;animation:grow .3s ease-out calc(var(--i)*40ms) backwards}
.c .nm{position:absolute;left:4px;bottom:5px;width:9px;height:1px;background:var(--mut);opacity:.7;transform-origin:left;animation:grow .2s ease-out calc(var(--i)*40ms) backwards}
@keyframes tick{from{opacity:0;transform:translateY(-3px)}}
@keyframes grow{from{transform:scaleX(0)}}
.c .al{font-size:13px;font-weight:400;font-stretch:76%;color:var(--mut)}
.c.today{background:var(--redt)}
.c.today>b{color:var(--red)}
.c .td{font-size:11px;font-weight:700;font-stretch:78%;text-transform:uppercase;letter-spacing:.06em;color:var(--red);margin-top:2px}
.ring{position:absolute;left:50%;top:50%;width:calc(100% + 15px);height:calc(100% + 11px);transform:translate(-50%,-54%);overflow:visible;pointer-events:none}
.ring path{fill:none;stroke:var(--red);stroke-width:1.75;stroke-linecap:round;vector-effect:non-scaling-stroke;stroke-dasharray:1;stroke-dashoffset:0;animation:ring .6s cubic-bezier(.2,.7,.2,1) .7s backwards}
@keyframes ring{from{stroke-dashoffset:1}}
.c.lg{grid-column:1/5;background:var(--tint);justify-content:center;padding:0 6px;gap:3px}
.lg p{display:flex;align-items:center;font-size:11px;font-weight:600;font-stretch:78%;text-transform:uppercase;letter-spacing:.05em;color:var(--mut);white-space:nowrap}
.lg .l2{font-size:13px;text-transform:none;letter-spacing:0;font-weight:500;color:var(--ink)}
.lg .k{display:inline-block;width:10px;height:4px;margin:0 4px 0 9px;background:var(--red)}
.lg .k:first-child{margin-left:0}
.lg .k.in{background:var(--blue)}.lg .k.no{height:1px;background:var(--mut)}

/* alert */
.due{display:flex;align-items:center;gap:7px;border:1px solid var(--red);margin-top:-1px;background:var(--redt);font-size:13.5px;font-stretch:84%;height:31px;padding-right:6px;position:relative;z-index:1;flex:none;white-space:nowrap}
.due>.ic:first-child{display:none}
.due b{align-self:stretch;display:flex;align-items:center;background:var(--red);color:var(--on-red);font-size:12px;font-stretch:80%;text-transform:uppercase;letter-spacing:.055em;padding:0 9px}
.due span{flex:1}.due strong{font-weight:800}
.due .ic{color:var(--red)}

/* accounts */
.ar{display:grid;grid-template-columns:1fr auto auto;align-items:baseline;gap:10px;padding:0 9px;height:28px;line-height:28px;border-bottom:1px solid var(--rule)}
.ar b{font-size:14px;font-weight:650;font-stretch:90%}
.ar small{font-size:12px;font-stretch:80%;color:var(--mut)}
.ar .v{font-size:14.5px;font-weight:700;min-width:84px;text-align:right}
.v.neg{color:var(--red)}
.acr .ar:last-child{border-bottom:0}
.at{display:flex;justify-content:space-between;align-items:baseline;padding:0 9px;height:30px;line-height:30px;border-top:1px solid var(--line);background:var(--tint)}
.at b{font-size:12px;font-stretch:80%;text-transform:uppercase;letter-spacing:.055em}
.at span{font-size:17px;font-weight:800;font-stretch:104%}

/* date-grouped rows */
.grp{display:grid;grid-template-columns:50px 1fr;border-top:1px solid var(--line)}
.tb+.grp{border-top:0}
.dlf{border-right:1px solid var(--line);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:1px;background:var(--tint)}
.dlf b{font-size:22px;font-weight:800;font-stretch:116%;line-height:1;letter-spacing:-.02em}
.dlf em{font-size:11px;font-weight:700;font-stretch:76%;text-transform:uppercase;letter-spacing:.05em;color:var(--mut);white-space:nowrap}
.dlf.sun b,.dlf.sun em{color:var(--red)}
.rw{display:flex;justify-content:space-between;align-items:center;gap:8px;padding:7px 9px;min-height:50px}
.rw+.rw{border-top:1px solid var(--rule)}
.rw .nm{min-width:0}
.rw .nm b{display:flex;align-items:center;gap:5px;font-size:14.5px;font-weight:650;font-stretch:92%;line-height:1.15}
.rw .nm b .ic{color:var(--blue)}
.rw small{display:flex;align-items:center;flex-wrap:wrap;gap:2px 6px;font-size:12px;font-stretch:80%;color:var(--mut);margin-top:3px;line-height:1.2}
.rw .a{font-size:15px;font-weight:700;white-space:nowrap}
.a.inc{color:var(--blue)}
.tg{font-size:11px;font-weight:700;font-stretch:76%;text-transform:uppercase;letter-spacing:.05em;border:1px solid currentColor;padding:1px 4px 0;line-height:1.15;white-space:nowrap}
.tg.e{color:var(--blue)}.tg.d{color:var(--ink)}
.home .lat{margin-right:63px}

/* tab bar + fab */
.tabs{position:absolute;left:0;right:0;bottom:0;height:64px;background:var(--paper);border-top:2px solid var(--line);display:grid;grid-template-columns:repeat(5,1fr)}
.tabs span{display:flex;flex-direction:column;align-items:center;gap:3px;padding-top:9px;color:var(--ink)}
.tabs span+span{border-left:1px solid var(--line)}
.tabs em{font-size:11.5px;font-weight:600;font-stretch:76%;text-transform:uppercase;letter-spacing:.04em}
.tabs span.on{background:var(--red);color:var(--on-red)}
.tabs span:active{background:var(--tint)}
.fab{position:absolute;right:12px;bottom:76px;width:52px;height:52px;background:var(--red);color:var(--on-red);display:grid;place-items:center;padding-top:7px;z-index:5;border:1px solid var(--line);box-shadow:inset 0 7px 0 var(--red-d)}
.fab:active{background:var(--red-d)}

/* page headers */
.hd{display:flex;align-items:center;height:44px;flex:none}
.hd h1{flex:1;font-size:23px;font-weight:800;font-stretch:108%;line-height:1;letter-spacing:-.01em}
.hd>.ib:first-child{margin:0 10px 0 0}
.hd .rt{display:flex}
.ty{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid var(--line);flex:none}
.ty.t3{grid-template-columns:repeat(3,1fr)}
.ty span{text-align:center;font-size:12.5px;font-weight:600;font-stretch:80%;text-transform:uppercase;letter-spacing:.055em;line-height:31px}
.ty span+span{border-left:1px solid var(--line)}
.ty span.on{background:var(--ink);color:var(--paper)}
.ty span:active{background:var(--tint)}
.mo{display:grid;grid-template-columns:34px 1fr 34px;border:1px solid var(--line);margin-top:-1px;height:34px;align-items:stretch;flex:none}
.mo b{display:grid;place-items:center;font-size:15px;font-weight:800;font-stretch:112%;border-left:1px solid var(--line);border-right:1px solid var(--line)}
.mo span{display:grid;place-items:center}
.lst{border:1px solid var(--line);margin-top:8px}
.lst .grp:first-child{border-top:0}
.lst .rw{min-height:58px}

/* add */
.addp .ty{margin-bottom:8px}
.amt{display:flex;align-items:flex-end;justify-content:flex-end;border:1px solid var(--line);height:92px;padding:0 12px 10px;flex:none;background:var(--tint)}
.amt .cur{font-size:26px;font-weight:600;color:var(--mut);margin:0 6px 9px 0;line-height:1}
.amt .n{font-size:66px;font-weight:800;font-stretch:110%;line-height:.86;letter-spacing:-.02em}
.caret{width:3px;height:56px;background:var(--red);margin:0 0 1px 5px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);border:1px solid var(--line);margin-top:-1px;flex:none}
.np span{height:56px;padding:6px 0 0 10px;font-size:27px;font-weight:800;font-stretch:112%;line-height:1;border-top:1px solid var(--rule);border-left:1px solid var(--rule)}
.np span:nth-child(3n+1){border-left:0}.np span:nth-child(-n+3){border-top:0}
.np span:active{background:var(--ink);color:var(--paper)}
.np span .ic{margin-top:2px}
.fld{margin-top:8px}
.fl{display:flex;align-items:center;gap:8px;height:38px;padding:0 9px}
.fl b{font-size:15px;font-weight:650;flex:1}.fl span{font-size:12.5px;font-stretch:80%;color:var(--mut)}
.fld+.md{margin-top:8px}
.cg{display:grid;grid-template-columns:repeat(4,1fr)}
.cg span{height:66px;padding:7px 4px 6px 8px;display:flex;flex-direction:column;justify-content:space-between;border-left:1px solid var(--rule);border-top:1px solid var(--rule);min-width:0}
.cg .k0{border-left:0}.cg .q0{border-top:0}
.cg em{font-size:12.5px;font-weight:600;font-stretch:76%;white-space:nowrap}
.cg span.on{background:var(--red);color:var(--on-red)}
.cg span:active{background:var(--tint)}
.app .addp{bottom:92px}
.cta{position:absolute;left:0;right:0;bottom:0;height:92px;padding:10px 12px 0;background:var(--paper);border-top:2px solid var(--line)}
.cta button{width:100%;height:50px;border:1px solid var(--line);border-radius:0;background:var(--red);color:var(--on-red);font-size:15px;font-weight:800;font-stretch:104%;text-transform:uppercase;letter-spacing:.06em;box-shadow:inset 0 6px 0 var(--red-d)}
.cta button:active{background:var(--red-d)}

/* budgets */
.per{display:flex;justify-content:space-between;align-items:center;border:1px solid var(--line);height:32px;padding:0 9px;background:var(--tint);flex:none;margin-bottom:8px}
.per b{font-size:14px;font-weight:800;font-stretch:104%}
.per span{font-size:12px;font-weight:700;font-stretch:80%;text-transform:uppercase;letter-spacing:.055em;color:var(--red)}
.bd+.bd{margin-top:8px}
.tb .st{display:flex;align-items:center;gap:4px;font-weight:700}
.tb .st.ok{color:var(--blue)}
.tb .st.wn{background:var(--red);color:var(--on-red)}
.bc{display:grid;grid-template-columns:84px 1fr 1fr 1fr;border-bottom:1px solid var(--rule)}
.bc>div{padding:5px 8px 5px;border-left:1px solid var(--rule);display:flex;flex-direction:column;gap:2px}
.bc>div:first-child{border-left:0;justify-content:center}
.bc small{font-size:11px;font-weight:600;font-stretch:78%;text-transform:uppercase;letter-spacing:.06em;color:var(--mut)}
.bc b{font-size:15px;font-weight:700}
.bc .pc b{font-size:29px;font-weight:800;font-stretch:112%;line-height:1;letter-spacing:-.02em}
.bd.warn .pc b{color:var(--red)}
.bw{padding:9px 9px 22px}
.nl{display:flex;align-items:center;gap:6px;height:27px;padding:0 9px;border-top:1px solid var(--red);background:var(--redt);font-size:13.5px;font-stretch:84%}
.nl .ic{color:var(--red)}.nl b{color:var(--red);text-transform:uppercase;font-size:12px;font-stretch:80%;letter-spacing:.055em}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:206px;border-right:2px solid var(--line);padding:14px 14px 14px;display:flex;flex-direction:column}
.mark{display:flex;align-items:center;gap:10px;height:40px;margin-bottom:12px}
.lf{width:34px;height:38px;border:1px solid var(--line);display:flex;flex-direction:column}
.lf i{height:9px;background:var(--red)}
.lf b{flex:1;display:grid;place-items:center;font-size:21px;font-weight:800;font-stretch:120%;line-height:1}
.mark strong{font-size:27px;font-weight:800;font-stretch:120%;letter-spacing:-.02em;line-height:1}
.addb{display:flex;align-items:center;justify-content:center;gap:7px;height:40px;background:var(--red);color:var(--on-red);border:1px solid var(--line);box-shadow:inset 0 5px 0 var(--red-d);padding-top:4px;font-size:13.5px;font-weight:800;text-transform:uppercase;letter-spacing:.05em;margin-bottom:12px}
.addb:active{background:var(--red-d)}
.sd nav{display:flex;flex-direction:column;border:1px solid var(--line)}
.ni{display:flex;align-items:center;gap:9px;height:38px;padding:0 10px;font-size:14px;font-weight:600;font-stretch:92%}
.ni+.ni{border-top:1px solid var(--line)}
.ni.on{background:var(--red);color:var(--on-red);font-weight:800}
.tls{border:1px solid var(--line);margin-top:12px;display:flex;flex-direction:column}
.tl{height:30px;line-height:30px;padding:0 10px;font-size:13.5px;font-stretch:90%}
.tl+.tl{border-top:1px solid var(--rule)}
.pv{margin-top:auto;display:flex;align-items:center;gap:5px;font-size:12px;font-stretch:80%;color:var(--mut)}
.pv .ic{color:var(--blue)}
.mn{position:absolute;left:206px;right:0;top:0;bottom:0;padding:6px 16px 14px;display:flex;flex-direction:column;overflow:hidden}
.mn .who p{font-size:13px;font-weight:600;text-transform:uppercase;letter-spacing:.05em}
.hw{display:grid;grid-template-columns:316px 1fr;flex:1;min-height:0}
.hl{border-right:1px solid var(--line);display:flex;flex-direction:column;min-width:0}
.mn .hb{grid-template-columns:84px 1fr;flex:1}
.mn .leaf small{line-height:19px;font-size:12px}
.mn .leaf b{font-size:56px;margin-top:auto}
.mn .leaf em{font-size:14px;margin-bottom:auto;padding-bottom:6px}
.mn .fg{display:flex;flex-direction:column;justify-content:center;padding:8px 12px}
.mn .fg .n{font-size:62px}
.mn .fg p{font-size:13px;margin-top:6px}
.q4{display:grid;grid-template-columns:1fr 1fr;border-bottom:1px solid var(--line)}
.q4>div{padding:7px 10px 6px;display:flex;flex-direction:column;gap:2px;border-top:1px solid var(--rule);border-left:1px solid var(--rule)}
.q4>div:nth-child(odd){border-left:0}.q4>div:nth-child(-n+2){border-top:0}
.q4 small{font-size:11px;font-weight:600;font-stretch:78%;text-transform:uppercase;letter-spacing:.06em;color:var(--mut)}
.q4 b{font-size:18px;font-weight:800;font-stretch:104%}
.mn .sp{padding:9px 12px 24px}
.mn .due{border:0;margin:0;height:38px}
.cal.big{grid-template-rows:22px repeat(5,minmax(0,1fr))}
.big .wd{line-height:22px;font-size:12px;padding:0 8px}
.big .c{padding:7px 8px 0}
.big .c>b{font-size:22px;margin-bottom:5px}
.big .c .am{font-size:13.5px;font-stretch:84%;line-height:1.2}
.big .c .al{font-size:14px;font-stretch:88%}
.big .c .td{font-size:12px}
.big .c .nm{left:8px;bottom:8px;width:14px}
.big .c .mk{height:5px}
.big .c.lg{padding:0 12px;gap:6px}
.big .lg p{font-size:12px}.big .lg .l2{font-size:14px}
.tiles{display:grid;grid-template-columns:1fr .86fr 1.24fr;flex:none;margin-top:-1px}
.tiles .md{margin-top:0;display:flex;flex-direction:column}
.tiles .md+.md{margin-left:-1px}
.tiles .acr{flex:1;display:flex;flex-direction:column}
.tiles .ar{flex:1;align-items:center;height:auto}
.tiles .at{height:44px;line-height:44px}.tiles .at span{font-size:21px}
.gr{flex:1;padding:0 10px;display:flex;flex-direction:column;justify-content:center;gap:7px}
.gr+.gr{border-top:1px solid var(--rule)}
.gh{display:flex;justify-content:space-between;align-items:baseline}
.gh b{font-size:14.5px;font-weight:650}.gh span{font-size:17px;font-weight:800;font-stretch:108%}
.gr small{font-size:13px;font-stretch:84%;color:var(--mut)}
.tiles .rw{min-height:45px;padding:4px 10px}
.tiles .dlf b{font-size:19px}
.tiles .grp{grid-template-columns:54px 1fr}
`;

export const screens = { home, txns, add, budgets, desk };
