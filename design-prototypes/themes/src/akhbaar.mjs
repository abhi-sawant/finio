import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 9,
  slug: 'akhbaar',
  name: 'Akhbaar',
  tagline: 'The morning broadsheet. Your money set like the front page you read with your chai — a headline, a deck, a few tight columns — for people who like their numbers reported, not decorated.',
  blurb: 'The morning paper. Newsprint and black ink with one masthead red, a blackletter flag and dateline, halftone pace charts, and accounts set like the markets table.',
  fonts:
    'https://fonts.googleapis.com/css2?family=Newsreader:ital,opsz,wght@0,6..72,400;0,6..72,500;0,6..72,600;1,6..72,400&family=Oswald:wght@500;600;700&family=UnifrakturMaguntia&display=swap',
  swatch: ['#f2efe6', '#151515', '#c4161c', '#1e6a3b'],
  swatchDark: ['#17181b', '#ece8dc', '#ff6259', '#6fcf8f'],
  notes: [
    ['Shape', 'No cards and no radii. Structure is the newspaper grid: column rules, gutters, a heavy-and-thin double rule under the masthead and 2px section rules.'],
    ['Type', 'UnifrakturMaguntia for the “Finio” flag only; Oswald 600 for headlines and the figure (88–100px); Newsreader with tabular numerals for every row, table and amount.'],
    ['Density', 'Packed like page one: dateline, lead story, breaking strip, markets table and briefs on one phone screen; three ruled columns on desktop.'],
    ['Motion', 'The banner headline is inked once — a left-to-right wipe that resolves from a slight bleed to sharp. Halftone bars then fill dot by dot.'],
    ['Voice', 'Headline voice in the gaps only, and only what the data says: “Due in 3 days”, “Spending runs behind the calendar”. Labels stay the product’s own.'],
    ['Signature', 'The masthead and dateline, the halftone pace chart (budget used against month gone), and accounts as a markets table with drawn triangles.'],
  ],
};

/* ---------- helpers ---------- */
const n = (s) => Number(String(s).replace(/,/g, ''));
const tri = (up) =>
  `<svg class="tri ${up ? 'up' : 'dn'}" width="9" height="8" viewBox="0 0 9 8" aria-hidden="true"><path d="${up ? 'M4.5 0.5 8.5 7.5H0.5Z' : 'M0.5 0.5H8.5L4.5 7.5Z'}" fill="currentColor"/></svg>`;
const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 20, 1.5)}</span>`;
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.5)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 24, 2)}</span>`;
const DATE = 'Thursday, 15 October 2026';

const total = n(D.total);
const share = (a) => `${a.owed ? '−' : ''}${((n(a.bal) / total) * 100).toFixed(1)}%`;

const mast = `<header class="mast">
  <div class="ears">${ib('eye', 'Hide amounts')}<h1 class="flag">Finio</h1>${ib('sliders', 'Settings')}</div>
  <p class="motto">${ic('shield', 12, 1.5)}Kept only on this device</p>
  <div class="dateline"><span>${DATE}</span><span>Edition for ${D.user}</span></div>
</header>`;

const pace = (big = false) => `<figure class="pace${big ? ' big' : ''}" role="img" aria-label="Budget used ${D.pct}%, month gone ${D.dayPct}%">
  <div class="pr"><span class="pl">Budget used</span><div class="bar"><i class="ht" style="width:${D.pct}%"></i><u style="left:${D.dayPct}%"></u></div><b>${D.pct}%</b></div>
  <div class="pr"><span class="pl">Month gone</span><div class="bar"><i class="ht lt" style="width:${D.dayPct}%"></i><u style="left:${D.dayPct}%"></u></div><b>${D.dayPct}%</b></div>
  <figcaption>Day 15 of 31 — spending runs behind the calendar.</figcaption>
</figure>`;

const lead = (big = false) => `<section class="lead${big ? ' big' : ''}">
  <h2 class="hed"><span class="fig">₹${D.safe}</span><span class="hl">Safe to spend today</span></h2>
  <p class="deck">₹${D.left} left this month · ${D.days} days to go</p>
  <p class="by">Spent ₹${D.spent} of ₹${D.budget} · ${D.pct}% of budget</p>
  ${pace(big)}
</section>`;

const brk = `<p class="brk"><b>Due in ${D.due.days} days</b><span>${D.due.name} payment of <strong>₹${D.due.amt}</strong></span></p>`;

const sh = (t, extra = '') => `<div class="sh"><h2>${t}${extra}</h2><a>See all</a></div>`;

const markets = `<table class="mk">
  <thead><tr><th>Account</th><th>Share</th><th>Balance</th></tr></thead>
  <tbody>${D.accounts
    .map(
      (a) =>
        `<tr><td><b>${a.name}</b><small>${a.type}</small></td><td class="s">${share(a)}</td><td class="v${a.owed ? ' neg' : ''}">${tri(!a.owed)}${a.owed ? '−' : ''}₹${a.bal}</td></tr>`,
    )
    .join('')}</tbody>
  <tfoot><tr><td colspan="2">Total balance</td><td class="v">₹${D.total}</td></tr></tfoot>
</table>
<p class="key">${tri(true)} held ${tri(false)} owed</p>`;

const row = (r, date = '') =>
  `<div class="rw"><div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 1.75) : ''}</b><small>${date ? `${date} · ` : ''}${r.cat} · ${r.acct}${
    r.tag ? ` · <span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  ${mast}
  ${lead()}
  ${brk}
  ${sh('Where it sits')}
  ${markets}
  ${sh('Latest', '<i>, in brief</i>')}
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span>${ic('chevl', 16, 1.75)}</span><b>October 2026</b><span>${ic('chevr', 16, 1.75)}</span></div>
  ${D.groups.map((g) => `<div class="dl"><span>${g.date}</span></div>${g.rows.map((r) => row(r)).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty t3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtb"><div class="fig"><sup>₹</sup>2,450<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.5)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fl"><div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 16, 1.75)}</div>
  <div class="sh"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.5)}${nm}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><span>Monthly · October 2026</span><span>Day 15 of 31</span></div>
  <div class="st">
    <div class="sth"><span>Budget</span><span>Spent</span><span>Limit</span><span>Left</span><span>Used</span></div>
    ${D.budgets
      .map((b) => {
        const warn = b.pct > 85;
        return `<section class="sr">
      <div class="srow"><b>${b.name}</b><span>₹${b.spent}</span><span>₹${b.limit}</span><span>₹${b.left}</span><strong>${b.pct}%</strong></div>
      <div class="sbar"><div class="bar"><i class="ht${warn ? ' w' : ''}" style="width:${b.pct}%"></i><u style="left:${D.dayPct}%"></u></div><em class="${warn ? 'wn' : 'ok'}">${b.state}</em></div>
    </section>`;
      })
      .join('')}
  </div>
  <p class="foot"><u></u>Red rule marks day 15 of 31. Housing has used 94% with ${D.days} days to go.</p>
</div>
${tabs('Settings').replace(' class="on"', '')}`;

const desk = `
<aside class="sd">
  <div class="flag">Finio</div>
  <span class="addb">${ic('plus', 16, 2)} Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 17, 1.5)}${nm}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="dh">
    <p class="motto">${ic('shield', 13, 1.5)}Kept only on this device</p>
    <p class="dl2">${DATE} · Edition for ${D.user}</p>
    <div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div>
  </header>
  <div class="fp">
    <div class="c1">${lead(true)}${brk}</div>
    <div class="c2">${sh('Where it sits')}${markets}
      ${sh('Savings goals')}
      ${D.goals
        .map(
          (g) =>
            `<div class="gr"><div class="gh"><b>${g.name}</b><span>${g.pct}%</span></div><div class="bar sm"><i class="ht" style="width:${g.pct}%"></i></div><small>₹${g.have} of ₹${g.target}</small></div>`,
        )
        .join('')}
    </div>
    <div class="c3">${sh('Latest', '<i>, in brief</i>')}${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}</div>
  </div>
</main>`;

export const css = `
.app{--paper:#f2efe6;--ink:#151515;--mut:#57534a;--rule:#cbc5b5;--red:#c4161c;--on-red:#f7f4ec;--pos:#1e6a3b;--warn:#8a5600;--dot:#151515;--dotlt:rgba(21,21,21,.42);--track:rgba(21,21,21,.16);
  --mast:'UnifrakturMaguntia',serif;--hd:'Oswald',sans-serif;--tx:'Newsreader',serif;
  background:var(--paper);color:var(--ink);font-family:var(--tx);font-optical-sizing:auto;font-size:14px;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--red)}
html[data-mode=dark] .app{--paper:#17181b;--ink:#ece8dc;--mut:#a8a397;--rule:#3a3b41;--red:#ff6259;--on-red:#17181b;--pos:#6fcf8f;--warn:#e8b04a;--dot:#ece8dc;--dotlt:rgba(236,232,220,.4);--track:rgba(236,232,220,.14)}
.app ::selection{background:var(--red);color:var(--on-red)}
.app :focus-visible{outline:2px solid var(--red);outline-offset:2px}
.app .pg{position:absolute;inset:44px 0 64px;padding:0 18px;overflow:hidden}
.ib{width:36px;height:36px;display:grid;place-items:center;color:var(--ink);flex:none}

/* masthead */
.mast{padding-top:2px}
.ears{display:flex;align-items:center;justify-content:space-between}
.flag{font-family:var(--mast);font-weight:400;font-size:40px;line-height:1;letter-spacing:.01em;color:var(--ink)}
.motto{display:flex;align-items:center;justify-content:center;gap:5px;font-style:italic;font-size:12.5px;color:var(--mut);margin:1px 0 6px}
.dateline{display:flex;justify-content:space-between;font-size:10.5px;letter-spacing:.09em;text-transform:uppercase;font-weight:500;border-top:1px solid var(--ink);padding:4px 0 5px;position:relative}
.dateline::after{content:'';position:absolute;left:0;right:0;bottom:-6px;height:6px;border-top:3px solid var(--red);border-bottom:1px solid var(--red)}

/* lead story */
.lead{padding:12px 0 0}
.hed{display:flex;flex-direction:column;font-family:var(--hd);color:var(--ink);animation:ink .95s cubic-bezier(.2,.75,.3,1) .12s backwards}
@keyframes ink{0%{clip-path:inset(-12% 100% -12% 0);filter:blur(2.4px)}58%{clip-path:inset(-12% 0 -12% 0);filter:blur(1px)}100%{clip-path:inset(-12% 0 -12% 0);filter:blur(0)}}
.hed .fig{font-size:76px;font-weight:600;line-height:.92;letter-spacing:-.01em;margin-left:-2px}
.hed .hl{font-size:21px;font-weight:500;text-transform:uppercase;letter-spacing:.035em;line-height:1.1;margin-top:4px}
.deck{font-style:italic;font-size:17px;line-height:1.25;margin-top:4px}
.by{font-size:10.5px;letter-spacing:.09em;text-transform:uppercase;color:var(--mut);font-weight:500;margin-top:5px;padding-bottom:9px;border-bottom:1px solid var(--rule)}

/* halftone */
.ht{background-image:radial-gradient(circle,var(--dot) 1.4px,transparent 1.7px);background-size:4px 4px;background-position:0 0}
.ht.lt{background-image:radial-gradient(circle,var(--dotlt) .95px,transparent 1.25px)}
.ht.w{background-image:radial-gradient(circle,var(--warn) 1.25px,transparent 1.55px)}
.bar{position:relative;height:14px;border-bottom:1px solid var(--ink);background-image:radial-gradient(circle,var(--track) .6px,transparent .9px);background-size:4px 4px}
.bar i{position:absolute;left:0;top:0;bottom:0;-webkit-mask-image:linear-gradient(90deg,#000 70%,rgba(0,0,0,.5));mask-image:linear-gradient(90deg,#000 70%,rgba(0,0,0,.5));animation:dots .9s steps(14,end) .75s backwards}
@keyframes dots{from{clip-path:inset(0 100% 0 0)}}
.bar u{position:absolute;top:-4px;bottom:-4px;width:2px;margin-left:-1px;background:var(--red)}
.pace{margin:8px 0 0}
.pr{display:grid;grid-template-columns:84px 1fr 38px;align-items:center;gap:10px;margin-bottom:4px}
.pl{font-family:var(--hd);font-weight:500;font-size:11.5px;text-transform:uppercase;letter-spacing:.06em}
.pr b{font-family:var(--hd);font-weight:600;font-size:17px;text-align:right}
.pace figcaption{font-style:italic;font-size:13px;color:var(--mut);margin-top:2px}

/* breaking strip */
.brk{display:flex;align-items:stretch;margin:10px 0 0;border-top:1px solid var(--red);border-bottom:1px solid var(--red);font-size:14px}
.brk b{background:var(--red);color:var(--on-red);font-family:var(--hd);font-weight:600;font-size:12px;text-transform:uppercase;letter-spacing:.07em;padding:7px 9px;display:flex;align-items:center;white-space:nowrap}
.brk span{padding:6px 10px;display:block;align-self:center;line-height:1.3}
.brk strong{font-weight:600}

/* section heads */
.sh{display:flex;justify-content:space-between;align-items:baseline;border-top:2px solid var(--ink);margin-top:11px;padding-top:3px}
.sh h2{font-family:var(--hd);font-weight:600;font-size:16px;text-transform:uppercase;letter-spacing:.04em}
.sh h2 i{font-family:var(--tx);font-weight:400;text-transform:none;letter-spacing:0;color:var(--mut)}
.sh a{font-size:13px;font-style:italic;text-decoration:underline;text-underline-offset:2px;text-decoration-thickness:1px}

/* markets table */
.mk{width:100%;border-collapse:collapse;margin-top:3px}
.mk th{font-family:var(--hd);font-weight:500;font-size:10.5px;text-transform:uppercase;letter-spacing:.08em;color:var(--mut);text-align:right;padding:2px 0 3px;border-bottom:1px solid var(--ink)}
.mk th:first-child,.mk td:first-child{text-align:left}
.mk td{padding:3px 0;border-bottom:1px solid var(--rule);text-align:right;vertical-align:middle}
.mk td b{display:block;font-weight:600;font-size:15px;line-height:1.2}
.mk td small{display:block;font-size:11.5px;color:var(--mut);line-height:1.2}
.mk .s{font-size:13.5px;color:var(--mut);width:64px}
.mk .v{font-size:15.5px;font-weight:600;white-space:nowrap;width:118px}
.mk .v.neg{color:var(--red)}
.tri{display:inline-block;vertical-align:1px;margin-right:6px}
.tri.up{color:var(--pos)}.tri.dn{color:var(--red)}
.mk tfoot td{border-bottom:3px double var(--ink);padding:4px 0;font-family:var(--hd);font-weight:500;font-size:12px;text-transform:uppercase;letter-spacing:.07em}
.mk tfoot .v{font-family:var(--tx);font-size:17px;text-transform:none;letter-spacing:0;font-weight:600}
.key{display:none}

/* rows */
.rw{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:8px 0;border-bottom:1px solid var(--rule)}
.rw .nm{min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:600;font-size:16px;line-height:1.2}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:block;font-size:12.5px;color:var(--mut);margin-top:2px;line-height:1.3}
.rw .a{font-size:16px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{font-size:10px;letter-spacing:.07em;text-transform:uppercase;font-weight:600;border:1px solid currentColor;padding:0 3px;white-space:nowrap}
.tg.e{color:var(--pos)}.tg.d{color:var(--warn)}

/* tab bar + fab */
.tabs{position:absolute;left:0;right:0;bottom:0;height:64px;background:var(--paper);border-top:1px solid var(--ink);display:flex;padding:0 6px}
.tabs::before{content:'';position:absolute;left:0;right:0;top:2px;border-top:1px solid var(--ink)}
.tabs span{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;padding-top:11px;color:var(--mut);position:relative}
.tabs em{font-style:normal;font-family:var(--hd);font-weight:500;font-size:10px;text-transform:uppercase;letter-spacing:.07em}
.tabs span.on{color:var(--red)}
.tabs span.on::before{content:'';position:absolute;top:-1px;left:14%;right:14%;height:4px;background:var(--red)}
.fab{position:absolute;right:18px;bottom:80px;width:52px;height:52px;background:var(--ink);color:var(--paper);display:grid;place-items:center;z-index:5;box-shadow:0 0 0 3px var(--paper)}

/* page headers */
.hd{display:flex;align-items:center;gap:4px;padding:8px 0 6px;border-bottom:3px solid var(--ink)}
.hd h1{flex:1;font-family:var(--hd);font-weight:600;font-size:30px;line-height:1.05;letter-spacing:-.005em}
.hd .ib:first-child{margin-left:-8px}
.hd .rt{display:flex;margin-right:-8px}
.ty{display:grid;grid-template-columns:repeat(4,1fr);border-bottom:1px solid var(--ink)}
.ty.t3{grid-template-columns:repeat(3,1fr)}
.ty span{text-align:center;font-family:var(--hd);font-weight:500;font-size:12.5px;text-transform:uppercase;letter-spacing:.07em;color:var(--mut);padding:9px 0 8px;position:relative}
.ty span+span{border-left:1px solid var(--rule)}
.ty span.on{color:var(--ink)}
.ty span.on::after{content:'';position:absolute;left:18%;right:18%;bottom:-1px;height:3px;background:var(--red)}
.mo{display:flex;justify-content:space-between;align-items:center;padding:9px 0;border-bottom:1px solid var(--rule)}
.mo b{font-weight:600;font-size:16px;font-variant:small-caps;letter-spacing:.04em}
.mo span{color:var(--mut);display:grid}
.dl{display:flex;align-items:center;gap:8px;margin:14px 0 0;font-family:var(--hd);font-weight:500;font-size:11.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut)}
.dl::after{content:'';flex:1;border-top:1px solid var(--ink)}

/* add */
.amtb{padding:12px 0 6px;text-align:center}
.amtb .fig{font-family:var(--hd);font-weight:600;font-size:72px;line-height:1;display:inline-flex;align-items:flex-start}
.amtb sup{font-size:30px;font-weight:500;margin:8px 4px 0 0;color:var(--mut)}
.caret{width:3px;height:62px;background:var(--red);margin:6px 0 0 5px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid var(--ink);border-bottom:1px solid var(--ink)}
.np span{height:50px;display:grid;place-items:center;font-family:var(--hd);font-weight:500;font-size:25px;border-bottom:1px solid var(--rule)}
.np span:nth-child(3n+2){border-left:1px solid var(--rule);border-right:1px solid var(--rule)}
.np span:nth-last-child(-n+3){border-bottom:0}
.np span:active{background:var(--red);color:var(--on-red)}
.fl{display:flex;align-items:center;gap:10px;border-bottom:1px solid var(--rule);padding:8px 0}
.fl div{flex:1}.fl small{display:block;font-family:var(--hd);font-weight:500;font-size:10.5px;text-transform:uppercase;letter-spacing:.08em;color:var(--mut)}
.fl b{font-weight:600;font-size:16px}.fl span{font-size:13px;font-style:italic;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);margin-top:4px;border-bottom:1px solid var(--rule)}
.cg span{display:flex;flex-direction:column;align-items:center;gap:5px;padding:10px 0 9px;font-size:12px;color:var(--mut);position:relative}
.cg span:not(:nth-child(4n+1)){border-left:1px solid var(--rule)}
.cg span:nth-child(-n+4){border-bottom:1px solid var(--rule)}
.cg span .ic{color:var(--ink)}
.cg span.on{color:var(--ink);font-weight:600}
.cg span.on .ic{color:var(--red)}
.cg span.on::after{content:'';position:absolute;left:22%;right:22%;bottom:-1px;height:3px;background:var(--red)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:10px 18px 30px;background:var(--paper);border-top:1px solid var(--ink)}
.cta button{width:100%;height:52px;border:0;border-radius:0;background:var(--ink);color:var(--paper);font-family:var(--hd);font-weight:600;font-size:16px;text-transform:uppercase;letter-spacing:.08em}

/* budgets standings */
.per{display:flex;justify-content:space-between;font-size:10.5px;letter-spacing:.09em;text-transform:uppercase;font-weight:500;padding:6px 0 7px;position:relative;margin-bottom:8px}
.per::after{content:'';position:absolute;left:0;right:0;bottom:-6px;height:6px;border-top:3px solid var(--red);border-bottom:1px solid var(--red)}
.st{margin-top:14px}
.sth,.srow{display:grid;grid-template-columns:1fr 60px 60px 56px 40px;align-items:baseline;gap:0 4px}
.sth{font-family:var(--hd);font-weight:500;font-size:10.5px;text-transform:uppercase;letter-spacing:.08em;color:var(--mut);padding-bottom:3px;border-bottom:1px solid var(--ink)}
.sth span:not(:first-child),.srow span,.srow strong{text-align:right}
.sr{padding:10px 0 9px;border-bottom:1px solid var(--rule)}
.srow b{font-weight:600;font-size:15.5px;line-height:1.2}
.srow span{font-size:13.5px}
.srow strong{font-family:var(--hd);font-weight:600;font-size:18px}
.sbar{display:grid;grid-template-columns:1fr 78px;gap:10px;align-items:center;margin-top:8px}
.sbar .bar{height:12px}
.sbar em{font-style:normal;font-family:var(--hd);font-weight:600;font-size:10.5px;text-transform:uppercase;letter-spacing:.07em;text-align:right}
.sbar em.ok{color:var(--pos)}
.sbar em.wn{color:var(--on-red);background:var(--warn);padding:2px 0;text-align:center}
html[data-mode=dark] .app .sbar em.wn{color:var(--paper)}
.foot{display:flex;gap:8px;align-items:center;font-style:italic;font-size:13px;color:var(--mut);margin-top:12px;line-height:1.35}
.foot u{flex:none;width:2px;height:16px;background:var(--red)}

/* desktop front page */
.sd{position:absolute;left:0;top:0;bottom:0;width:224px;border-right:1px solid var(--ink);padding:24px 22px;display:flex;flex-direction:column}
.sd::after{content:'';position:absolute;top:0;bottom:0;right:3px;border-right:1px solid var(--ink)}
.sd .flag{font-size:44px;padding-bottom:12px;border-bottom:3px solid var(--red);position:relative;margin-bottom:18px}
.sd .flag::after{content:'';position:absolute;left:0;right:0;bottom:-6px;border-bottom:1px solid var(--red)}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:40px;background:var(--ink);color:var(--paper);font-family:var(--hd);font-weight:600;font-size:13px;text-transform:uppercase;letter-spacing:.07em;margin-bottom:14px}
.sd nav{display:flex;flex-direction:column}
.ni{display:flex;align-items:center;gap:10px;padding:8px 0 8px 10px;font-family:var(--hd);font-weight:500;font-size:13px;text-transform:uppercase;letter-spacing:.07em;color:var(--mut);border-bottom:1px solid var(--rule);position:relative}
.ni.on{color:var(--red)}
.ni.on::before{content:'';position:absolute;left:0;top:6px;bottom:6px;width:3px;background:var(--red)}
.gl{font-family:var(--hd);font-weight:600;font-size:13px;text-transform:uppercase;letter-spacing:.07em;border-top:2px solid var(--ink);margin:20px 0 4px;padding-top:5px}
.tl{font-size:14px;padding:4px 0;color:var(--ink)}
.mn{position:absolute;left:224px;right:0;top:0;bottom:0;padding:14px 30px 20px;overflow:hidden}
.dh{display:grid;grid-template-columns:1fr auto 1fr;align-items:center;border-bottom:1px solid var(--ink);padding-bottom:4px;position:relative;margin-bottom:12px}
.dh::after{content:'';position:absolute;left:0;right:0;bottom:-7px;height:6px;border-top:3px solid var(--red);border-bottom:1px solid var(--red)}
.dh .motto{justify-content:flex-start;margin:0;font-size:13px}
.dl2{font-size:11.5px;letter-spacing:.1em;text-transform:uppercase;font-weight:500}
.dh .rt{display:flex;justify-content:flex-end;margin-right:-8px}
.fp{display:grid;grid-template-columns:1.08fr 1.08fr 1fr;margin-top:6px;height:calc(100% - 60px)}
.fp>div{padding:0 22px;min-width:0}
.fp>div:first-child{padding-left:0}
.fp>div:last-child{padding-right:0}
.fp>div+div{border-left:1px solid var(--rule)}
.lead.big{padding-top:16px}
.lead.big .by{padding-bottom:14px}
.lead.big .fig{font-size:104px}
.lead.big .hl{font-size:26px}
.lead.big .deck{font-size:19px}
.pace.big{margin-top:24px}
.pace.big .bar{height:34px}
.pace.big .pr{margin-bottom:12px;grid-template-columns:92px 1fr 44px}
.pace.big .pr b{font-size:20px}
.c1 .brk{margin-top:30px}
.c1 .brk span{font-size:13.5px;padding:9px 10px}
.c2 .sh,.c3 .sh{margin-top:6px}
.c2 .sh+*{margin-top:3px}
.c2 .sh:nth-of-type(2){margin-top:22px}
.mn .mk td{padding:8px 0}
.mn .mk .s{width:50px}
.mn .mk .v{width:auto;padding-left:8px}
.mn .key{display:flex;align-items:center;gap:4px;font-size:11.5px;font-style:italic;color:var(--mut);margin-top:6px}
.key .tri{margin:0 0 0 6px}.key .tri:first-child{margin-left:0}
.gr{padding:14px 0;border-bottom:1px solid var(--rule)}
.gh{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px}
.gh b{font-weight:600;font-size:15px}.gh span{font-family:var(--hd);font-weight:600;font-size:15px}
.bar.sm{height:10px}
.gr small{display:block;font-size:12.5px;color:var(--mut);margin-top:4px}
.c3 .rw{padding:14px 0}
.c3 .rw .nm b{font-size:15.5px}
`;

export const screens = { home, txns, add, budgets, desk };
