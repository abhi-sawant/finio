import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 12,
  slug: 'safar',
  name: 'Safar',
  defaultMode: 'dark',
  tagline: 'The railway station. A departures board over the concourse at night, the yellow platform name board by day — for people who like their money as a timetable: what is due, when, and from which account.',
  blurb: 'The departures board. Split-flap figures on matte black, signal amber, green and red, coach-blue shell — and a chrome-yellow platform board with heavy black lettering in daylight.',
  fonts: 'https://fonts.googleapis.com/css2?family=Overpass:wght@500;600;700;800;900&family=Overpass+Mono:wght@400;500;600;700&display=swap',
  swatch: ['#d9d8d1', '#111213', '#f6c400', '#1c4a8a'],
  swatchDark: ['#0c0e11', '#f2ecdc', '#ffb224', '#5bd08a'],
  notes: [
    ['Shape', 'Boards with 4px radii and hairline frames, flap tiles and keys at 3px, corner rivets on the hero board. Nothing glossy, no soft cards; rows are separated by thin rules.'],
    ['Type', 'Overpass 800–900 for names and titles, Overpass Mono with tabular figures for every amount, date and board row. Caps only for column heads and status words.'],
    ['Density', 'Medium-dense, strict columns: every list is a board with column heads (date, name, account, amount, tag) and 38–56px rows.'],
    ['Motion', 'The hero figure flips in once: each tile drops its top flap, staggered left to right over about 1.2s, then rests. The keypad amount uses the same tiles; the caret blinks.'],
    ['Voice', 'Board wording in the gaps only, and only what the data says: “Day 15 of 31”, “Due in 3 days”, “On track”. The platform column shows the real account name.'],
    ['Signature', 'Split-flap figures with a hinge hairline, the amber “Due in 3 days” status row, 20-segment progress strips with a day tick, and a lit yellow tile for the active tab.'],
  ],
};

/* ---------- helpers ---------- */
const m = (s) => `<span class="m">${s}</span>`;
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 19, 1.75)}</span>`;
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.75)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 26, 2.25)}</span>`;

const flap = (s, cls = '') =>
  `<span class="flap ${cls}" role="img" aria-label="${s}">${[...s]
    .map((c, i) => `<span class="ft${c === ',' || c === '.' ? ' p' : ''}${c === '₹' ? ' r' : ''}" style="--i:${i}" aria-hidden="true"><b>${c}</b><i></i></span>`)
    .join('')}</span>`;

const clock = `<span class="clk">${ic('clock', 13, 2)}Day 15 of 31</span>`;
const strip = (pct, cls = '', tick = true) =>
  `<span class="sg ${cls}" style="--p:${pct}%" role="img" aria-label="${pct}% used">${tick ? `<u style="left:${D.dayPct}%"></u>` : ''}</span>`;

const band = (title, l = '', r = '', sub = '') =>
  `<header class="band">${l}<div class="bt"><h1>${title}</h1>${sub}</div><div class="rt">${r}</div></header>`;
const privacy = `<p class="pv">${ic('shield', 12, 2)}Kept only on this device</p>`;

const hero = (big = false) => `<section class="hero${big ? ' big' : ''}">
  <div class="hh"><h2>Safe to spend today</h2>${clock}</div>
  ${flap(`₹${D.safe}`, big ? 'xl' : 'lg')}
  <p class="hl">${m(`₹${D.left}`)} left this month · ${m(D.days)} days to go</p>
  ${strip(D.pct, 'hs')}
  <p class="hs2"><span>Spent ${m(`₹${D.spent}`)} of ${m(`₹${D.budget}`)}</span>${m(`${D.pct}%`)}</p>
</section>`;

const alert = `<p class="al">${ic('alert', 18, 2)}<span>${D.due.name} payment of ${m(`₹${D.due.amt}`)}</span><b>Due in ${D.due.days} days</b></p>`;

const bh = (t, link = 'See all') => `<div class="bh"><h2>${t}</h2>${link ? `<a>${link}</a>` : ''}</div>`;

const accounts = `<section class="bd ac">
  ${bh('Where it sits')}
  <div class="ch"><span>Account</span><span>Type</span><span>Balance</span></div>
  ${D.accounts
    .map((a) => `<div class="r"><b>${a.name}</b><span class="ty">${a.type}</span><span class="m v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`)
    .join('')}
  <div class="r tot"><b>Total balance</b><span class="m v">₹${D.total}</span></div>
</section>`;

const tag = (t) => (t ? `<span class="tg ${t === 'Essential' ? 'e' : 'd'}">${t}</span>` : '');
const amt = (r) => `<span class="m v${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span>`;
const nm = (r) => `<b>${r.name}${r.rec ? ic('repeat', 13, 2) : ''}</b>`;

/* ---------- screens ---------- */
// One row on the phone: a second would sit under the add button.
const latestRows = [[D.groups[0].rows[1], '2 Oct']];
const home = `
${band(D.user, '', ib('eye', 'Hide amounts') + ib('sliders', 'Settings'), privacy)}
<div class="pg">
  ${hero()}
  ${alert}
  ${accounts}
  <section class="bd lt">
    ${bh('Latest')}
    <div class="ch"><span>Date</span><span>Name</span><span>Amount</span></div>
    ${latestRows.map(([r, d]) => `<div class="r"><span class="m dt">${d}</span><div class="n">${nm(r)}<small>${r.cat} · ${r.acct}</small></div>${amt(r)}</div>`).join('')}
  </section>
</div>
${fab}${tabs('Home')}`;

const txns = `
${band('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
<div class="pg">
  <div class="sw s4"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span aria-label="Previous month">${ic('chevl', 18, 2)}</span><b>October 2026</b><span aria-label="Next month">${ic('chevr', 18, 2)}</span></div>
  <section class="bd tx">
    <div class="ch"><span>Name</span><span>Account</span><span>Amount</span></div>
    ${D.groups
      .map(
        (g) =>
          `<div class="dg m">${g.date}</div>${g.rows
            .map((r) => `<div class="r"><div class="n">${nm(r)}<small>${r.cat}${tag(r.tag)}</small></div><span class="pf">${r.acct}</span>${amt(r)}</div>`)
            .join('')}`,
      )
      .join('')}
  </section>
</div>
${fab}${tabs('Transactions')}`;

const add = `
${band('Add transaction', ib('back', 'Back'), '')}
<div class="pg">
  <div class="sw s3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amt">${flap('₹2,450', 'lg')}<i class="caret" aria-hidden="true"></i></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 1.75)].map((k) => `<button type="button">${k}</button>`).join('')}</div>
  <div class="fl"><small>Account</small><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 18, 2)}</div>
  <div class="bh ph"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.75)}${n}</span>`).join('')}</div>
</div>
<div class="cta"><button type="button">Add transaction</button></div>`;

const budgetRow = (b) => {
  const warn = b.pct > 85;
  return `<section class="br${warn ? ' w' : ''}">
    <div class="b1"><b>${b.name}</b><span class="st ${warn ? 'wn' : 'ok'}">${ic(warn ? 'alert' : 'check', 13, 2.5)}${b.state}</span></div>
    <div class="b2">${strip(b.pct, warn ? 'wn' : 'ok')}<strong class="m">${b.pct}%</strong></div>
    <div class="b3"><span><small>Spent</small>${m(`₹${b.spent}`)}</span><span><small>Limit</small>${m(`₹${b.limit}`)}</span><span><small>Left</small>${m(`₹${b.left}`)}</span></div>
  </section>`;
};
const budgets = `
${band('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
<div class="pg">
  <div class="per"><b>Monthly · October 2026</b>${clock}</div>
  <section class="bd bg">
    <div class="ch"><span>Budget</span><span>Status</span></div>
    ${D.budgets.map(budgetRow).join('')}
  </section>
  <p class="lg2"><u></u>Tick marks day 15 of 31 · ${D.days} days to go</p>
</div>
${tabs()}`;

const desk = `
<aside class="sd">
  <div class="plate">Finio</div>
  <span class="addb">${ic('plus', 17, 2.5)}Add transaction</span>
  <nav>${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 18, 1.75)}${n}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="band dband"><div class="bt"><h1>${D.user}</h1>${privacy}</div><span class="m dd">Thursday, 15 October 2026</span><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  <div class="dg2">
    <div class="c1">
      ${hero(true)}
      ${alert}
      <section class="bd gl2">
        ${bh('Savings goals')}
        <div class="ch"><span>Goal</span><span>Saved</span><span>Done</span></div>
        ${D.goals
          .map((g) => `<div class="gr"><div class="g1"><b>${g.name}</b>${m(`₹${g.have} of ₹${g.target}`)}<strong class="m">${g.pct}%</strong></div>${strip(g.pct, 'ok', false)}</div>`)
          .join('')}
      </section>
    </div>
    <div class="c2">
      ${accounts}
      <section class="bd dl">
        ${bh('Latest')}
        <div class="ch"><span>Date</span><span>Name</span><span>Account</span><span>Amount</span><span>Tag</span></div>
        ${D.groups
          .flatMap((g) =>
            g.rows.map(
              (r) =>
                `<div class="r"><span class="m dt">${g.date.split(', ')[1]}</span><div class="n">${nm(r)}<small>${r.cat}</small></div><span class="pf">${r.acct}</span>${amt(r)}<span class="tc">${tag(r.tag)}</span></div>`,
            ),
          )
          .join('')}
      </section>
    </div>
  </div>
</main>`;

export const css = `
.app{--g:#d9d8d1;--board:#efeee8;--board2:#e3e2db;--rule:#bdbcb3;--frame:#111213;--fw:1.5px;--ink:#111213;--mut:#55554e;
  --yel:#f6c400;--on-yel:#111213;--blue:#1c4a8a;--blue-d:#143a70;--on-blue:#eef3fa;--blue-mut:#b4c8e4;
  --amber:#f2a900;--amber-fill:#b87400;--green:#0b6b3a;--red:#b3261e;--act:#1c4a8a;
  --band:#f6c400;--band-ink:#111213;--band-rule:#111213;--band-mut:#3b3200;
  --hero:#f6c400;--hero-ink:#111213;--hero-mut:#3b3200;--hero-fill:#111213;--hero-track:rgba(17,18,19,.2);--rivet:rgba(17,18,19,.55);
  --t1:#24272c;--t2:#17191d;--flap:#f4efe0;--clk-ink:#f6c400;
  --sel:#111213;--on-sel:#f6c400;--key:#f4f3ee;--key-b:#111213;
  --al:#15171a;--al-ink:#f4efe0;--al-b:#15171a;--track:rgba(17,18,19,.17);--warn-bg:rgba(242,169,0,.16);
  --lamp:#1c4a8a;--on-lamp:#fff;--cta:#1c4a8a;--on-cta:#fff;
  --sans:'Overpass',system-ui,sans-serif;--mono:'Overpass Mono',ui-monospace,Menlo,monospace;
  background:var(--g);color:var(--ink);font-family:var(--sans);font-size:14px;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--act)}
html[data-mode=dark] .app{--g:#0c0e11;--board:#15181c;--board2:#1b1e23;--rule:#2c3037;--frame:#2c3037;--fw:1px;--ink:#f2ecdc;--mut:#a39d8f;
  --yel:#f8c900;--blue:#17396a;--blue-d:#102b52;--on-blue:#e6eefa;--blue-mut:#a9bedb;
  --amber:#ffb224;--amber-fill:#ffb224;--green:#5bd08a;--red:#ff6a5c;--act:#f8c900;
  --band:#0c0e11;--band-ink:#f2ecdc;--band-rule:#f8c900;--band-mut:#a39d8f;
  --hero:#15181c;--hero-ink:#f2ecdc;--hero-mut:#a39d8f;--hero-fill:#f2ecdc;--hero-track:rgba(242,236,220,.15);--rivet:rgba(242,236,220,.3);
  --t1:#2b2f36;--t2:#1f2227;--clk-ink:#ffb224;
  --sel:#f8c900;--on-sel:#111213;--key:#1b1e23;--key-b:#343840;
  --al:#2a1f09;--al-ink:#f2ecdc;--al-b:#ffb224;--track:rgba(242,236,220,.14);--warn-bg:rgba(255,178,36,.1);
  --lamp:#f8c900;--on-lamp:#111213;--cta:#f8c900;--on-cta:#111213}
.app ::selection{background:var(--sel);color:var(--on-sel)}
.app :focus-visible{outline:2px solid var(--act);outline-offset:2px}
.app .m{font-family:var(--mono);font-variant-numeric:tabular-nums}
.app>.sb{color:var(--band-ink)}

/* header band — the name board */
.band{position:absolute;left:0;right:0;top:0;height:100px;padding:46px 16px 0;display:flex;align-items:center;gap:8px;background:var(--band);color:var(--band-ink);border-bottom:3px solid var(--band-rule);z-index:2}
.band .bt{flex:1;min-width:0}
.band h1{font-weight:900;font-size:25px;line-height:1.05;letter-spacing:-.01em}
.pv{display:flex;align-items:center;gap:5px;font-size:12px;font-weight:600;color:var(--band-mut);margin-top:1px}
.band .rt{display:flex;gap:8px}
.ib{width:38px;height:38px;display:grid;place-items:center;border:1.5px solid currentColor;border-radius:3px;flex:none}
html[data-mode=dark] .app .ib{border-color:var(--rule);background:var(--board)}
.ib:active{background:var(--sel);color:var(--on-sel)}
.app .pg{position:absolute;inset:100px 0 68px;padding:0 16px;overflow:hidden}

/* split-flap tiles */
.flap{display:inline-flex;gap:var(--fg,4px);--tw:46px;--th:66px;--tf:46px}
.flap.lg{--tw:48px;--th:68px;--tf:47px}
.flap.xl{--tw:50px;--th:76px;--tf:52px;--fg:5px}
.ft{position:relative;display:grid;place-items:center;width:var(--tw);height:var(--th);border-radius:3px;background:linear-gradient(180deg,var(--t1) 0 50%,var(--t2) 50% 100%);color:var(--flap);font:700 var(--tf)/1 var(--mono);box-shadow:inset 0 1px 0 rgba(255,255,255,.08),inset 0 -1px 0 rgba(0,0,0,.5);perspective:220px}
.ft.p{width:calc(var(--tw)*.5)}
.ft.r b{font-size:.8em}
.ft b{font-weight:700;padding-top:.08em}
.ft i{position:absolute;left:0;right:0;top:50%;height:1px;margin-top:-.5px;background:rgba(0,0,0,.6)}
.ft::before,.ft::after{content:'';position:absolute;left:0;right:0;opacity:0;backface-visibility:hidden}
.ft::before{top:0;bottom:50%;background:var(--t1);border-radius:3px 3px 0 0;transform-origin:bottom;animation:flapT .34s cubic-bezier(.3,.55,.35,1) calc(var(--i)*.09s + .15s) backwards}
.ft::after{top:50%;bottom:0;background:var(--t2);border-radius:0 0 3px 3px;transform-origin:top;animation:flapB .3s cubic-bezier(.2,.7,.3,1) calc(var(--i)*.09s + .47s) backwards}
@keyframes flapT{from{opacity:1;transform:rotateX(0)}to{opacity:1;transform:rotateX(-90deg)}}
@keyframes flapB{from{opacity:1;transform:rotateX(0)}to{opacity:1;transform:rotateX(90deg)}}

/* platform clock */
.clk{display:inline-flex;align-items:center;gap:6px;height:26px;padding:1px 9px 0;border-radius:3px;background:var(--t2);color:var(--clk-ink);font:600 13px/1 var(--mono);white-space:nowrap}
.clk .ic{margin-top:-1px}

/* segmented strip */
.sg{position:relative;display:block;height:12px;--fill:var(--green)}
.sg::before{content:'';position:absolute;inset:0;background:linear-gradient(90deg,var(--fill) var(--p),var(--track) var(--p));-webkit-mask:repeating-linear-gradient(90deg,#000 0 calc(5% - 2px),transparent calc(5% - 2px) 5%);mask:repeating-linear-gradient(90deg,#000 0 calc(5% - 2px),transparent calc(5% - 2px) 5%)}
.sg.wn{--fill:var(--amber-fill)}
.sg.hs{--fill:var(--hero-fill);--track:var(--hero-track)}
.sg u{position:absolute;top:-4px;bottom:-4px;width:2px;margin-left:-2px;background:var(--ink)}
.sg.hs u{background:var(--hero-ink)}

/* hero board */
.hero{margin:0 -16px;padding:13px 16px 13px;background-color:var(--hero);color:var(--hero-ink);border-bottom:var(--fw) solid var(--frame);
  background-image:radial-gradient(circle at 7px 7px,var(--rivet) 1.5px,transparent 2px),radial-gradient(circle at calc(100% - 7px) 7px,var(--rivet) 1.5px,transparent 2px),radial-gradient(circle at 7px calc(100% - 7px),var(--rivet) 1.5px,transparent 2px),radial-gradient(circle at calc(100% - 7px) calc(100% - 7px),var(--rivet) 1.5px,transparent 2px)}
.hh{display:flex;align-items:center;justify-content:space-between;margin-bottom:9px}
.hh h2{font-weight:800;font-size:16px}
.hl{font-weight:700;font-size:15px;margin:10px 0 9px}
.hl .m{font-weight:700;font-size:15px}
.hs2{display:flex;justify-content:space-between;align-items:baseline;font-size:13px;font-weight:600;color:var(--hero-mut);margin-top:8px}
.hs2 .m{font-size:13px;font-weight:600;color:var(--hero-ink)}

/* alert status row */
.al{display:flex;align-items:center;gap:9px;margin-top:10px;min-height:44px;padding:0 0 0 11px;background:var(--al);color:var(--al-ink);border:1px solid var(--al-b);border-radius:4px;overflow:hidden;font-weight:600;font-size:14px;line-height:1.25}
.al>.ic{color:var(--amber)}
.al span{flex:1;padding:6px 0}
.al .m{font-weight:700;font-size:14px}
.al b{align-self:stretch;display:flex;align-items:center;padding:1px 10px 0;background:var(--amber);color:#111213;font:700 12.5px/1 var(--mono);text-transform:uppercase;letter-spacing:.03em;white-space:nowrap}

/* boards */
.bd{margin-top:10px;background:var(--board);border:var(--fw) solid var(--frame);border-radius:4px;overflow:hidden}
.bh{display:flex;align-items:center;justify-content:space-between;padding:8px 11px 5px}
.bh h2{font-weight:800;font-size:15.5px}
.bh a{font-weight:700;font-size:13px;color:var(--act);text-decoration:underline;text-underline-offset:3px;text-decoration-thickness:1.5px}
.ch{display:grid;gap:0 10px;padding:4px 11px 3px;background:var(--board2);border-top:1px solid var(--rule);border-bottom:1px solid var(--rule);font-weight:700;font-size:10.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut)}
.ch span:last-child{text-align:right}
.bd .r{display:grid;gap:0 10px;align-items:center;padding:0 11px;border-bottom:1px solid var(--rule)}
.bd .r:last-child{border-bottom:0}
.bd .r b{font-weight:700;font-size:14.5px;line-height:1.2;display:flex;align-items:center;gap:6px}
.bd .r b .ic{color:var(--mut);flex:none}
.bd .r small{display:flex;align-items:center;gap:6px;font-size:12px;font-weight:500;color:var(--mut);margin-top:2px;line-height:1.2}
.bd .v{font-size:15px;font-weight:600;text-align:right;white-space:nowrap}
.v.neg{color:var(--red)}.v.inc{color:var(--green)}
.dt{font-size:13px;color:var(--mut)}
.pf{font-size:12.5px;font-weight:600;color:var(--mut);white-space:nowrap}
.tg{font:700 10px/1 var(--sans);text-transform:uppercase;letter-spacing:.07em;border:1px solid currentColor;border-radius:2px;padding:3px 4px 1px;white-space:nowrap}
.tg.e{color:var(--green)}.tg.d{color:var(--amber-fill)}

.ac .ch,.ac .r{grid-template-columns:1fr 92px 96px}
.ac .r{height:38px}
.ac .ty{font-size:12.5px;font-weight:500;color:var(--mut)}
.ac .r.tot{grid-template-columns:1fr auto;border-top:2px solid var(--ink);margin-top:-1px;height:40px}
.ac .tot .v{font-size:17px;font-weight:700}
.lt .ch,.lt .r{grid-template-columns:50px 1fr auto}
.lt .r{height:50px}

/* tab bar + lamp */
.tabs{position:absolute;left:0;right:0;bottom:0;height:68px;background:var(--blue);border-top:2px solid var(--blue-d);display:flex;gap:4px;padding:6px 8px 14px;z-index:3}
.tabs span{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;border-radius:3px;color:var(--blue-mut)}
.tabs em{font-style:normal;font-weight:700;font-size:10.5px;letter-spacing:.01em}
.tabs span.on{background:var(--yel);color:var(--on-yel);box-shadow:inset 0 -2px 0 rgba(0,0,0,.25)}
.tabs span:active{background:var(--blue-d)}
.fab{position:absolute;right:16px;bottom:80px;width:56px;height:56px;border-radius:50%;background:var(--lamp);color:var(--on-lamp);display:grid;place-items:center;z-index:5;box-shadow:0 0 0 3px var(--g),0 0 0 5px var(--frame),0 8px 16px -4px rgba(0,0,0,.45)}
html[data-mode=dark] .app .fab{box-shadow:0 0 0 3px var(--g),0 0 0 4.5px #3a3f47,0 6px 22px -2px rgba(248,201,0,.35)}
.fab:active{transform:scale(.96)}

/* switches */
.sw{display:grid;margin-top:12px;border:var(--fw) solid var(--frame);border-radius:4px;background:var(--board);padding:3px;gap:3px}
.sw.s4{grid-template-columns:repeat(4,1fr)}.sw.s3{grid-template-columns:repeat(3,1fr)}
.sw span{height:34px;display:grid;place-items:center;border-radius:3px;font-weight:700;font-size:13.5px;color:var(--mut);padding-top:1px}
.sw span.on{background:var(--sel);color:var(--on-sel)}
.mo{display:flex;align-items:center;justify-content:space-between;margin-top:10px;height:38px}
.mo b{font:600 15px/1 var(--mono);text-transform:uppercase;letter-spacing:.06em}
.mo span{width:38px;height:38px;display:grid;place-items:center;border:var(--fw) solid var(--frame);border-radius:3px;background:var(--board)}

/* transactions board */
.tx .ch,.tx .r{grid-template-columns:1fr 90px 80px}
.tx .ch span:nth-child(2){text-align:left}
.tx .r{height:54px}
.dg{padding:6px 11px 4px;font-size:13px;font-weight:600;text-transform:uppercase;letter-spacing:.05em;color:var(--ink);background:var(--board2);border-bottom:1px solid var(--rule)}
.tx .r+.dg{border-top:0}

/* add */
.amt{display:flex;align-items:center;justify-content:center;gap:6px;margin-top:10px;padding:13px 0;background:var(--board);border:var(--fw) solid var(--frame);border-radius:4px}
.caret{width:3px;height:56px;background:var(--amber-fill);animation:bl 1.05s steps(1) infinite}
html[data-mode=light] .app .caret{background:var(--blue)}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:10px}
.np button{height:50px;display:grid;place-items:center;background:var(--key);border:var(--fw) solid var(--key-b);border-radius:3px;font:600 24px/1 var(--mono);padding-top:2px;box-shadow:inset 0 -2px 0 rgba(0,0,0,.14)}
.np button:active{background:var(--sel);color:var(--on-sel);box-shadow:none;transform:translateY(1px)}
.fl{display:flex;align-items:center;gap:10px;margin-top:10px;height:48px;padding:0 11px;background:var(--board);border:var(--fw) solid var(--frame);border-radius:4px}
.fl small{font-weight:700;font-size:10.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut);width:62px}
.fl b{flex:1;font-weight:700;font-size:15px}
.fl span{font-size:12.5px;font-weight:500;color:var(--mut)}
.bh.ph{padding:10px 1px 4px}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:6px}
.cg span{height:60px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:5px;background:var(--board);border:1px solid var(--rule);border-radius:3px;font-weight:600;font-size:11.5px;color:var(--ink)}
.cg span.on{background:var(--sel);color:var(--on-sel);border-color:var(--sel);font-weight:800}
.cg span:active{border-color:var(--ink)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:10px 16px 28px;background:var(--g);border-top:var(--fw) solid var(--frame);z-index:3}
.cta button{width:100%;height:52px;border:0;border-radius:4px;background:var(--cta);color:var(--on-cta);font-weight:900;font-size:17px;padding-top:2px;box-shadow:inset 0 -3px 0 rgba(0,0,0,.22)}
.cta button:active{box-shadow:none;transform:translateY(1px)}

/* budgets */
.per{display:flex;align-items:center;justify-content:space-between;margin-top:12px}
.per b{font:600 14px/1 var(--mono);text-transform:uppercase;letter-spacing:.04em}
.bg .ch{grid-template-columns:1fr auto}
.br{padding:12px 11px 11px;border-bottom:1px solid var(--rule)}
.br:last-child{border-bottom:0}
.br.w{background:var(--warn-bg)}
.b1{display:flex;align-items:center;justify-content:space-between}
.b1 b{font-weight:800;font-size:16px}
.st{display:inline-flex;align-items:center;gap:5px;height:22px;font:700 12px/1 var(--mono);text-transform:uppercase;letter-spacing:.04em;padding-top:1px}
.st .ic{margin-top:-1px}
.st.ok{color:var(--green)}
.st.wn{background:var(--amber);color:#111213;padding:1px 8px 0;border-radius:2px}
.b2{display:grid;grid-template-columns:1fr 46px;align-items:center;gap:12px;margin-top:10px}
.b2 strong{font-size:17px;font-weight:700;text-align:right}
.b3{display:grid;grid-template-columns:repeat(3,1fr);margin-top:9px}
.b3 span{display:flex;align-items:baseline;gap:7px}
.b3 span:nth-child(2){justify-content:center}.b3 span:last-child{justify-content:flex-end}
.b3 small{font-weight:700;font-size:10.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut)}
.b3 .m{font-size:13.5px;font-weight:600}
.lg2{display:flex;align-items:center;gap:8px;margin-top:11px;font:500 13px/1.2 var(--mono);color:var(--mut)}
.lg2 u{width:2px;height:16px;background:var(--ink);flex:none}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:220px;background:var(--blue);color:var(--on-blue);padding:20px 16px;display:flex;flex-direction:column;border-right:2px solid var(--blue-d)}
.plate{align-self:flex-start;background:var(--yel);color:#111213;font-weight:900;font-size:26px;line-height:1;padding:9px 14px 5px;border-radius:3px;box-shadow:inset 0 0 0 2px #111213,inset 0 0 0 4px var(--yel);border:2px solid var(--yel);margin-bottom:20px}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:42px;background:var(--yel);color:#111213;font-weight:800;font-size:14px;border-radius:4px;padding-top:1px;margin-bottom:16px;box-shadow:inset 0 -3px 0 rgba(0,0,0,.2)}
.sd nav{display:flex;flex-direction:column;gap:3px}
.ni{display:flex;align-items:center;gap:11px;height:38px;padding:1px 10px 0;border-radius:3px;font-weight:700;font-size:14px;color:var(--on-blue)}
.ni .ic{margin-top:-1px}
.ni.on{background:var(--yel);color:#111213}
.gl{font-weight:700;font-size:10.5px;text-transform:uppercase;letter-spacing:.1em;color:var(--blue-mut);border-top:1px solid rgba(255,255,255,.2);margin:18px 0 6px;padding:12px 10px 0}
.tl{font-weight:600;font-size:13.5px;padding:5px 10px;color:var(--on-blue)}
.mn{position:absolute;left:220px;right:0;top:0;bottom:0}
.band.dband{height:66px;padding:0 24px}
.dband h1{font-size:26px;display:inline-block}
.dband .bt{display:flex;align-items:baseline;gap:14px}
.dband .pv{font-size:13px}
.dd{font-size:14px;font-weight:600;margin-right:10px}
.dg2{position:absolute;inset:66px 0 0;padding:16px 24px 20px;display:grid;grid-template-columns:372px 1fr;gap:16px}
.c1,.c2{display:flex;flex-direction:column;min-height:0;min-width:0}
.c1>:first-child,.c2>:first-child{margin-top:0}
.dg2 .bd,.dg2 .al{margin-top:14px}
.hero.big{margin:0;padding:18px 20px 18px;border:var(--fw) solid var(--frame);border-radius:4px}
.hero.big .hh{margin-bottom:13px}
.hero.big .hh h2{font-size:18px}
.hero.big .hl{font-size:16.5px;margin:14px 0 13px}
.hero.big .hl .m{font-size:16.5px}
.hero.big .sg{height:14px}
.hero.big .hs2{font-size:14px;margin-top:10px}.hero.big .hs2 .m{font-size:14px}
.dg2 .al{min-height:50px;font-size:14.5px}
.dg2 .bh{padding:10px 14px 7px}.dg2 .bh h2{font-size:16.5px}
.dg2 .ch{padding:5px 14px 4px}
.dg2 .bd .r{padding:0 14px}
.gl2,.dl{flex:1;display:flex;flex-direction:column}
.gl2 .ch{grid-template-columns:1fr auto 46px}
.gr{flex:1;display:flex;flex-direction:column;justify-content:center;gap:11px;padding:0 14px;border-bottom:1px solid var(--rule)}
.gr:last-child{border-bottom:0}
.g1{display:grid;grid-template-columns:1fr auto 46px;gap:0 10px;align-items:baseline}
.g1 b{font-weight:700;font-size:15px}
.g1 .m{font-size:13.5px;font-weight:500}
.g1 strong.m{font-size:15px;font-weight:700;text-align:right}
.dg2 .ac .ch,.dg2 .ac .r{grid-template-columns:1fr 150px 120px}
.dg2 .ac .r{height:42px}
.dg2 .ac .r.tot{grid-template-columns:1fr auto;height:46px}
.dg2 .ac .tot .v{font-size:19px}
.dl .ch,.dl .r{grid-template-columns:52px 1fr 112px 90px 104px}
.dl .ch span:last-child{text-align:left}
.dl .ch span:nth-child(4){text-align:right}
.dl .r{flex:1}
.dl .tc{display:flex}
`;

export const screens = { home, txns, add, budgets, desk };
