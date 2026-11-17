import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 16,
  slug: 'maidan',
  name: 'Maidan',
  tagline: 'The cricket ground. A month’s budget read as a run chase — so much in hand, so many days left, and a required rate — for people who already read a scorecard without thinking.',
  blurb: 'The cricket ground. Outfield green, a painted-tin scoreboard of black number plates, a worm chart of spend against par, and a red-ball add button.',
  fonts:
    'https://fonts.googleapis.com/css2?family=Big+Shoulders+Display:wght@700;800;900&family=Barlow:wght@400;500;600;700&family=Barlow+Semi+Condensed:wght@500;600;700&display=swap',
  swatch: ['#2a7436', '#141a16', '#b81d24', '#1c7230'],
  swatchDark: ['#0d2217', '#fff1cf', '#ff5aa8', '#5fe07f'],
  notes: [
    ['Shape', 'Painted tin. Figures sit on black number plates (2px radius, a hook notch and a top hairline) grouped in a white-framed board; lists are ruled scorecards with a 2px rule over the total row; buttons are squared at 4px.'],
    ['Type', 'Big Shoulders Display 800/900 for the hero digits (56–84px), every plate and title; Barlow for rows, Barlow Semi Condensed for table figures and painted caps labels. Tabular throughout.'],
    ['Density', 'Scorecard-tight: 32px account rows, four stat plates across, hero board plus worm chart in the top half of the phone; desktop runs the board and a large worm across the top, three scorecards below.'],
    ['Motion', 'The worm draws once left to right in 1.2s, then the ball lands on day 15 with one small bounce. Plates never animate; keypad plates depress 2px.'],
    ['Voice', 'Cricket idiom only where it is literally true: “₹4,355 under par at day 15”, “16 days to go”. No overs, wickets, teams or scores. Product labels stay verbatim.'],
    ['Signature', 'The chase: Safe to spend today as the required rate on digit plates, with Spent / Budget / Days / Left plates, and a worm chart of cumulative spend against a dashed par line to ₹40,000.'],
  ],
};

/* ---------- helpers ---------- */
const n = (s) => Number(String(s).replace(/,/g, ''));
const inr = (v) => v.toLocaleString('en-IN');
const R = '<span class="r">₹</span>';
const DAY = 15;
const MONTH = 31;
const parNow = Math.round((n(D.budget) * DAY) / MONTH); // 19,355
const under = inr(parNow - n(D.spent)); // 4,355

const ib = (i, label) => `<span class="ib" aria-label="${label}">${ic(i, 20, 1.75)}</span>`;
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.75)}<em>${nm}</em></span>`).join('')}</nav>`;

const fab = `<span class="fab" aria-label="Add transaction"><svg width="56" height="56" viewBox="0 0 56 56" fill="none" aria-hidden="true">
  <path d="M17.5 3.5C9 17 9 39 17.5 52.5" stroke="var(--seam)" stroke-width="2"/>
  <path d="M13.6 6.5C6 19 6 37 13.6 49.5M21.6 2.6C13 17 13 39 21.6 53.4" stroke="var(--stitch)" stroke-width="1.4" stroke-dasharray="1.6 3.4" stroke-linecap="round"/>
  <path d="M29 18v20M19 28h20" stroke="var(--on-ball)" stroke-width="4" stroke-linecap="round"/>
</svg></span>`;

const ump = `<svg class="ic" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2.5v10"/><path d="M7.5 12.5h9v5a4 4 0 0 1-4 4h-1a4 4 0 0 1-4-4z"/></svg>`;

const digs = (s, extra = '') =>
  `<span class="digs" role="img" aria-label="₹${s}">${R}${[...s].map((c) => (/\d/.test(c) ? `<b>${c}</b>` : `<i>${c}</i>`)).join('')}${extra}</span>`;

const board = `<section class="board">
  <div class="hl"><h2>Safe to spend today</h2><span class="cap">${D.pct}% used</span></div>
  <div class="hf">${digs(D.safe)}<p class="hn"><em class="cap">per day</em><b>₹${D.left} left this month</b><span>${D.days} days to go</span></p></div>
  <div class="pls">
    <div class="st"><small>Spent</small><span class="pl">${R}${D.spent}</span></div>
    <div class="st"><small>Budget</small><span class="pl">${R}${D.budget}</span></div>
    <div class="st"><small>Days</small><span class="pl">${DAY} / ${MONTH}</span></div>
    <div class="st"><small>Left</small><span class="pl">${R}${D.left}</span></div>
  </div>
</section>`;

/* worm chart: x = day × 10 (0–310), y = 92 − value / 40,000 × 84 */
const yv = (v) => +(92 - (v / n(D.budget)) * 84).toFixed(1);
const ticks = Array.from({ length: MONTH }, (_, i) => `M${(i + 1) * 10} 92v${(i + 1) % 5 === 0 || i === 0 ? 6 : 3}`).join('');
const NS = 'vector-effect="non-scaling-stroke"';
const worm = `<figure class="worm" role="img" aria-label="Cumulative spend ₹${D.spent} at day ${DAY}, ₹${under} under the par line to ₹${D.budget}">
  <div class="plot">
    <svg viewBox="0 0 310 100" preserveAspectRatio="none" fill="none" aria-hidden="true">
      <path d="M0 92H310" stroke="var(--chalk)" stroke-opacity=".55" stroke-width="1.5" ${NS}/>
      <path d="${ticks}" stroke="var(--chalk)" stroke-opacity=".5" stroke-width="1" ${NS}/>
      <path d="M0 92L310 8" stroke="var(--par)" stroke-width="2" stroke-dasharray="6 5" ${NS}/>
    </svg>
    <svg class="wl" viewBox="0 0 310 100" preserveAspectRatio="none" fill="none" aria-hidden="true">
      <path d="M${DAY * 10} ${yv(parNow)}V${yv(n(D.spent))}" stroke="var(--chalk)" stroke-width="1.5" ${NS}/>
      <path d="M0 92H20V${yv(n(D.spent))}H${DAY * 10}" stroke="var(--chalk)" stroke-width="3.5" stroke-linejoin="round" stroke-linecap="round" ${NS}/>
    </svg>
    <span class="ball" aria-hidden="true"></span>
    <span class="lb sp">Spent ₹${D.spent}</span>
    <span class="lb pr">Par to ₹${D.budget}</span>
  </div>
  <div class="ax"><span>Day 1</span><span>Day ${DAY}, today</span><span>Day ${MONTH}</span></div>
  <figcaption><i></i>₹${under} under par at day ${DAY}</figcaption>
</figure>`;

const alert = `<p class="al"><span class="pl">${ic('clock', 15, 2.2)}</span><span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const sh = (t, a = '<a>See all</a>') => `<div class="sh"><h2>${t}</h2>${a}</div>`;

const card = `<table class="sc">
  <thead><tr><th>Account</th><th class="v">Balance</th></tr></thead>
  <tbody>${D.accounts
    .map((a) => `<tr><td><b>${a.name}</b><small>${a.type}</small></td><td class="v${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</td></tr>`)
    .join('')}</tbody>
  <tfoot><tr><td>Total balance</td><td class="v"><span class="pl">${R}${D.total}</span></td></tr></tfoot>
</table>`;

const row = (r, date = '') =>
  `<div class="rw">${date ? `<span class="dt">${date}</span>` : ''}<div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 2) : ''}</b><small>${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;
const top = `<header class="top"><span class="av pl" aria-hidden="true">${D.user[0]}</span><div class="who"><b>${D.user}</b><small>${ic('shield', 12, 2)}Kept only on this device</small></div>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</header>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  <div class="gnd">${top}${board}${worm}</div>
  <div class="bd">
    ${alert}
    ${sh('Where it sits')}
    ${card}
    ${sh('Latest')}
    ${row(D.groups[0].rows[1])}
  </div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  <div class="gnd">
    ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
    <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  </div>
  <div class="bd">
    <div class="mo"><span>${ic('chevl', 18, 2)}</span><b>October 2026</b><span>${ic('chevr', 18, 2)}</span></div>
    <div class="tb">${D.groups.map((g) => `<div class="dl">${g.date}</div>${g.rows.map((r) => row(r)).join('')}`).join('')}</div>
  </div>
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg nt">
  <div class="gnd">
    ${hd('Add transaction', ib('back', 'Back'), '')}
    <div class="ty t3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
    <div class="board amt"><span class="cap">Amount</span>${digs('2,450', '<u class="caret"></u>')}</div>
    <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)].map((k) => `<span>${k}</span>`).join('')}</div>
  </div>
  <div class="bd">
    <div class="fl"><div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 18, 2)}</div>
    ${sh('Category', '<a class="splt">Split</a>')}
    <div class="cg">${D.cats.map(([nm, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.75)}${nm}</span>`).join('')}</div>
  </div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  <div class="gnd">
    ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
    <div class="per"><span>Monthly · October 2026</span><span>Day ${DAY} of ${MONTH}</span></div>
  </div>
  <div class="bd">
    ${D.budgets
      .map((b) => {
        const warn = b.pct > 85;
        return `<section class="bc${warn ? ' warn' : ''}">
      <div class="bh"><h3>${b.name}</h3><span class="stt ${warn ? 'wn' : 'ok'}">${warn ? ump : ic('check', 13, 2.6)}${b.state}</span></div>
      <div class="bt"><div><small>Spent</small><b>₹${b.spent}</b></div><div><small>Limit</small><b>₹${b.limit}</b></div><div><small>Left</small><b>₹${b.left}</b></div><div><small>Used</small><span class="pl">${b.pct}%</span></div></div>
      <div class="bb"><i style="width:${b.pct}%"></i><u style="left:${D.dayPct}%"></u></div>
    </section>`;
      })
      .join('')}
    <p class="ft"><u></u>Tick marks par, day ${DAY} of ${MONTH}. Housing has used 94% with ${D.days} days to go.</p>
  </div>
</div>
${tabs('')}`;

const desk = `
<aside class="sd">
  <div class="brand"><span class="bl" aria-hidden="true"></span>Finio</div>
  <span class="addb">${ic('plus', 17, 2.6)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 18, 1.75)}${nm}</span>`).join('')}</nav>
  <div class="gl cap">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <div class="dg">
    ${top}
    <div class="dhero"><div class="lc">${board}${alert}</div>${worm}</div>
  </div>
  <div class="dw">
    <div>${sh('Where it sits')}${card}</div>
    <div>${sh('Savings goals')}
      ${D.goals
        .map(
          (g) =>
            `<div class="go"><div class="gh"><b>${g.name}</b><span class="pl">${g.pct}%</span></div><div class="bb"><i style="width:${g.pct}%"></i></div><small>₹${g.have} of ₹${g.target}</small></div>`,
        )
        .join('')}
    </div>
    <div>${sh('Latest')}${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}</div>
  </div>
</main>`;

export const css = `
.app{--g1:#2a7436;--g2:#256b31;--on-g:#f6f8ef;--on-g2:#e3efdd;--panel:#f6f4ea;--cell:#fcfbf5;--ink:#141a16;--mut:#566058;--rule:#d3d0bf;--band:#e9e4d0;
  --board:#12381f;--frame:#fbfaf3;--plate:#111311;--plate-bd:#111311;--plate-hi:rgba(255,255,255,.26);--on-plate:#fbfaf3;--num-glow:none;
  --ball:#b81d24;--ball-hi:#e2474e;--on-ball:#fff;--seam:#7d0f16;--stitch:rgba(255,222,214,.85);--caret:#ff5a52;--chalk:#f6f8ef;--par:#f6e9b8;
  --pos:#1c7230;--warn:#855600;--neg:#b81d24;--on-neg:#fff;--side:#163f24;--glow:linear-gradient(transparent,transparent);
  --hd:'Big Shoulders Display','Barlow Semi Condensed',sans-serif;--tx:'Barlow',sans-serif;--tc:'Barlow Semi Condensed','Barlow',sans-serif;
  --grass:var(--glow),repeating-linear-gradient(90deg,var(--g1) 0 65px,var(--g2) 65px 130px);
  background:var(--panel);color:var(--ink);font-family:var(--tx);font-size:14px;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--ball)}
html[data-mode=dark] .app{--g1:#0e2618;--g2:#0b1f14;--on-g:#f3f0e0;--on-g2:#b4c6b8;--panel:#09140e;--cell:#0f2017;--ink:#eceadb;--mut:#9bad9f;--rule:#24392c;--band:#12261a;
  --board:#050a07;--frame:#e9e3cd;--plate:#000;--plate-bd:#30493a;--plate-hi:rgba(255,240,200,.3);--on-plate:#fff1cf;--num-glow:0 0 12px rgba(255,226,160,.4);
  --ball:#ff5aa8;--ball-hi:#ffa3cf;--on-ball:#26030f;--seam:#b0246b;--stitch:rgba(70,0,34,.7);--caret:#ff5aa8;--chalk:#fff1cf;--par:#9cc4a8;
  --pos:#5fe07f;--warn:#f0b445;--neg:#ff7d6e;--on-neg:#2a0603;--side:#06100a;--glow:radial-gradient(120% 70% at 50% -12%,rgba(190,255,205,.16),transparent 62%)}
.app ::selection{background:var(--ball);color:var(--on-ball)}
.app :focus-visible{outline:2px solid var(--ball);outline-offset:2px}
.app .sb{color:var(--on-g)}
.cap{font-family:var(--tc);font-weight:600;font-size:11px;text-transform:uppercase;letter-spacing:.09em;font-style:normal}
.r{font-family:var(--tc);font-weight:600}

.pg{position:absolute;inset:0 0 64px;overflow:hidden}
.pg.nt{bottom:0}
.gnd{background:var(--grass);color:var(--on-g);padding:44px 16px 12px}
.bd{padding:0 16px}

/* plates */
.pl{display:inline-block;background:var(--plate);color:var(--on-plate);border:1px solid var(--plate-bd);border-radius:2px;box-shadow:inset 0 1px 0 var(--plate-hi);font-family:var(--hd);font-weight:800;text-shadow:var(--num-glow);white-space:nowrap}
.pl .r{font-size:.76em;margin-right:1px}

/* header */
.top,.hd{display:flex;align-items:center;gap:8px;height:48px}
.av{width:36px;height:36px;display:grid;place-items:center;font-weight:900;font-size:24px;line-height:1;flex:none}
.who{flex:1;min-width:0;margin-left:2px}
.who b{display:block;font-family:var(--hd);font-weight:800;font-size:23px;line-height:1;text-transform:uppercase;letter-spacing:.03em}
.who small{display:flex;align-items:center;gap:4px;font-size:12.5px;color:var(--on-g2);margin-top:3px}
.ib{width:38px;height:38px;display:grid;place-items:center;border:1.5px solid color-mix(in srgb,var(--on-g) 55%,transparent);border-radius:4px;color:var(--on-g);flex:none}
.ib:active{background:rgba(0,0,0,.25)}
.hd h1{flex:1;font-family:var(--hd);font-weight:800;font-size:29px;line-height:1;text-transform:uppercase;letter-spacing:.025em}
.hd .rt{display:flex;gap:8px}

/* the scoreboard */
.board{background:var(--board);border:3px solid var(--frame);border-radius:3px;padding:9px 11px 11px;color:var(--on-g);box-shadow:0 8px 16px -8px rgba(0,0,0,.55);margin-top:6px}
.hl{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:7px}
.hl h2{font-family:var(--hd);font-weight:800;font-size:19px;line-height:1;text-transform:uppercase;letter-spacing:.05em}
.hl .cap{color:var(--on-g2);font-size:12px}
.hf{display:flex;align-items:flex-end;justify-content:space-between;gap:8px}
.digs{display:flex;align-items:flex-end;gap:4px}
.digs .r{font-size:30px;line-height:1;margin:0 2px 9px 0}
.digs b{position:relative;width:40px;height:64px;display:grid;place-items:center;background:var(--plate);color:var(--on-plate);border:1px solid var(--plate-bd);border-radius:2px;box-shadow:inset 0 1px 0 var(--plate-hi);font-family:var(--hd);font-weight:900;font-size:56px;line-height:1;text-shadow:var(--num-glow)}
.digs b::before{content:'';position:absolute;top:-1px;left:50%;width:8px;height:4px;margin-left:-4px;background:var(--board);border-radius:0 0 2px 2px}
.digs i{font-style:normal;font-family:var(--hd);font-weight:900;font-size:40px;line-height:1;width:7px;text-align:center;margin-bottom:3px}
.hn{text-align:right;font-family:var(--tc);font-size:13.5px;line-height:16px;color:var(--on-g2)}
.hn em{display:block;color:var(--par);margin-bottom:2px}
.hn b{display:block;font-weight:600;color:var(--on-g)}
.hn span{display:block}
.pls{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:9px}
.st small{display:block;font-family:var(--tc);font-weight:600;font-size:10.5px;line-height:13px;text-transform:uppercase;letter-spacing:.09em;color:var(--on-g2);margin-bottom:2px}
.st .pl{display:block;text-align:center;font-size:20px;line-height:28px;height:30px}

/* worm chart */
.worm{margin:10px 0 0}
.plot{position:relative;height:76px}
.plot svg{position:absolute;inset:0;width:100%;height:100%;overflow:visible}
.plot .wl{animation:draw 1.2s cubic-bezier(.22,.8,.3,1) .15s backwards}
@keyframes draw{from{clip-path:inset(-10px 100% -10px 0)}to{clip-path:inset(-10px 51% -10px 0)}}
.ball{position:absolute;left:48.387%;top:60.5%;width:14px;height:14px;margin:-7px 0 0 -7px;border-radius:50%;background:radial-gradient(circle at 35% 30%,var(--ball-hi),var(--ball) 62%);box-shadow:0 0 0 2px var(--chalk),0 3px 5px rgba(0,0,0,.45);animation:land .55s ease-out 1.2s backwards}
@keyframes land{0%{transform:translateY(-30px);opacity:0}50%{transform:none;opacity:1}72%{transform:translateY(-6px)}100%{transform:none}}
.lb{position:absolute;font-family:var(--tc);font-weight:600;font-size:13px;line-height:1;white-space:nowrap;color:var(--on-g)}
.lb.sp{left:9%;top:calc(60.5% - 20px)}
.lb.pr{right:0;top:46%;color:var(--par)}
.ax{position:relative;display:flex;justify-content:space-between;font-family:var(--tc);font-size:13px;line-height:15px;color:var(--on-g2);margin-top:4px;padding-left:1.5%}
.ax span:nth-child(2){position:absolute;left:48.387%;transform:translateX(-50%);color:var(--on-g);font-weight:600}
.worm figcaption{display:flex;align-items:center;gap:7px;font-family:var(--tc);font-weight:600;font-size:14.5px;line-height:18px;margin-top:5px;color:var(--on-g)}
.worm figcaption i{width:9px;height:9px;border-radius:50%;background:var(--ball);box-shadow:0 0 0 1.5px var(--chalk);flex:none;margin-left:2px}

/* alert */
.al{display:flex;align-items:center;gap:9px;margin-top:8px;height:38px;padding:0 10px 0 5px;border:1.5px solid var(--ink);border-radius:3px;background:var(--cell);font-size:13.5px;line-height:1.2}
.al .pl{display:grid;place-items:center;width:28px;height:26px;background:var(--neg);color:var(--on-neg);border-color:transparent;text-shadow:none;flex:none}
.al b{font-weight:700}

/* section heads + scorecards */
.sh{display:flex;justify-content:space-between;align-items:baseline;margin-top:10px;padding-bottom:2px;border-bottom:2px solid var(--ink)}
.sh h2{font-family:var(--hd);font-weight:800;font-size:18px;line-height:22px;text-transform:uppercase;letter-spacing:.05em}
.sh a{font-family:var(--tc);font-weight:600;font-size:13px;text-decoration:underline;text-underline-offset:3px;text-decoration-thickness:2px;text-decoration-color:var(--ball)}
.sc{width:100%;border-collapse:collapse}
.sc thead{display:none}
.sc td{height:32px;padding:0;border-bottom:1px solid var(--rule);vertical-align:middle}
.sc td b{font-weight:600;font-size:15px}
.sc td small{font-size:12.5px;color:var(--mut);margin-left:8px}
.sc .v{text-align:right;font-family:var(--tc);font-weight:600;font-size:16px;white-space:nowrap}
.sc .v.neg{color:var(--neg)}
.sc tfoot td{height:38px;border-bottom:0;border-top:2px solid var(--ink);font-family:var(--hd);font-weight:800;font-size:16px;text-transform:uppercase;letter-spacing:.05em}
.sc tfoot .pl{font-size:20px;line-height:26px;padding:0 8px;letter-spacing:.01em}

.rw{display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px solid var(--rule)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:600;font-size:15.5px;line-height:1.2}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:flex;align-items:center;gap:7px;font-size:12.5px;color:var(--mut);margin-top:3px;white-space:nowrap}
.rw .a{font-family:var(--tc);font-weight:600;font-size:17px;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{font-family:var(--tc);font-weight:700;font-size:10.5px;line-height:13px;text-transform:uppercase;letter-spacing:.07em;border:1px solid currentColor;border-radius:2px;padding:0 4px}
.tg.e{color:var(--pos)}.tg.d{color:var(--warn)}

/* tab bar + ball */
.tabs{position:absolute;left:0;right:0;bottom:0;height:64px;background:var(--grass);display:flex;gap:2px;padding:10px 6px 0}
.tabs::before{content:'';position:absolute;left:0;right:0;top:0;height:4px;background:repeating-linear-gradient(115deg,var(--chalk) 0 6px,color-mix(in srgb,var(--chalk) 70%,#000) 6px 8px)}
.tabs span{flex:1;height:44px;display:flex;flex-direction:column;align-items:center;gap:2px;padding-top:5px;color:var(--on-g);border-radius:3px;border:1px solid transparent}
.tabs em{font-style:normal;font-family:var(--tc);font-weight:600;font-size:11px;letter-spacing:.03em;line-height:13px}
.tabs span.on{background:var(--plate);color:var(--on-plate);border-color:var(--plate-bd);box-shadow:inset 0 3px 0 var(--ball)}
.tabs span:active{background:rgba(0,0,0,.3)}
.fab{position:absolute;right:16px;bottom:72px;width:56px;height:56px;border-radius:50%;background:radial-gradient(circle at 34% 26%,var(--ball-hi),var(--ball) 58%,var(--seam) 135%);box-shadow:0 7px 12px -3px rgba(0,0,0,.5),0 0 0 3px var(--panel);z-index:5;overflow:hidden}
.fab svg{display:block}
.fab:active{transform:translateY(2px)}

/* scoreboard tabs */
.ty{display:grid;grid-template-columns:repeat(4,1fr);gap:5px;margin-top:8px}
.ty.t3{grid-template-columns:repeat(3,1fr)}
.ty span{height:34px;display:grid;place-items:center;font-family:var(--hd);font-weight:800;font-size:16px;text-transform:uppercase;letter-spacing:.06em;color:var(--on-g);border:1.5px solid color-mix(in srgb,var(--on-g) 50%,transparent);border-radius:3px}
.ty span.on{background:var(--plate);color:var(--on-plate);border-color:var(--frame);box-shadow:inset 0 3px 0 var(--ball)}
.ty span:active{background:rgba(0,0,0,.3)}

/* transactions */
.mo{display:flex;justify-content:space-between;align-items:center;padding:10px 0 9px}
.mo b{font-family:var(--hd);font-weight:800;font-size:21px;text-transform:uppercase;letter-spacing:.05em}
.mo span{width:34px;height:34px;display:grid;place-items:center;border:1.5px solid var(--ink);border-radius:4px}
.mo span:active{background:var(--band)}
.tb{border-top:2px solid var(--ink);border-bottom:2px solid var(--ink)}
.tb .rw:last-child{border-bottom:0}
.dl{background:var(--band);padding:4px 8px;font-family:var(--tc);font-weight:700;font-size:12px;line-height:15px;text-transform:uppercase;letter-spacing:.09em;border-bottom:1px solid var(--rule)}
.tb .rw{padding:13px 8px}

/* add */
.amt{margin-top:10px;display:flex;flex-direction:column;align-items:center;padding:7px 11px 12px}
.amt .cap{align-self:flex-start;color:var(--on-g2);margin-bottom:3px}
.caret{width:4px;height:56px;margin:0 0 4px 3px;background:var(--caret);border-radius:1px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:11px 0 5px}
.np span{height:47px;display:grid;place-items:center;background:var(--plate);color:var(--on-plate);border:1px solid var(--plate-bd);border-radius:3px;font-family:var(--hd);font-weight:800;font-size:28px;line-height:1;text-shadow:var(--num-glow);box-shadow:inset 0 1px 0 var(--plate-hi),0 3px 0 rgba(0,0,0,.5)}
.np span:active{transform:translateY(2px);box-shadow:inset 0 1px 0 var(--plate-hi),0 1px 0 rgba(0,0,0,.5)}
.fl{display:flex;align-items:center;gap:10px;margin-top:10px;height:52px;padding:0 10px;border:1.5px solid var(--ink);border-radius:4px;background:var(--cell)}
.fl div{flex:1}
.fl small{display:block;font-family:var(--tc);font-weight:600;font-size:10.5px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut)}
.fl b{font-weight:600;font-size:15.5px}
.fl span{font-size:13px;color:var(--mut)}
.sh a.splt{text-decoration:none;font-weight:700;font-size:12px;text-transform:uppercase;letter-spacing:.08em;border:1.5px solid var(--ink);border-radius:3px;padding:0 9px;line-height:18px;margin-bottom:2px}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px}
.cg span{height:57px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;font-family:var(--tc);font-weight:600;font-size:12.5px;border:1px solid var(--rule);border-radius:3px;background:var(--cell)}
.cg span.on{background:var(--plate);color:var(--on-plate);border-color:var(--plate-bd);box-shadow:inset 0 3px 0 var(--ball)}
.cg span:active{background:var(--band)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:10px 16px 28px;background:var(--panel);border-top:2px solid var(--ink)}
.cta button{width:100%;height:52px;border:0;border-radius:4px;background:var(--ball);color:var(--on-ball);font-family:var(--hd);font-weight:800;font-size:22px;text-transform:uppercase;letter-spacing:.06em;box-shadow:inset 0 -3px 0 rgba(0,0,0,.25)}
.cta button:active{transform:translateY(1px);box-shadow:none}

/* budgets */
.per{display:flex;justify-content:space-between;font-family:var(--tc);font-weight:600;font-size:12.5px;text-transform:uppercase;letter-spacing:.08em;margin-top:6px}
.bc{padding:12px 0 20px;border-top:2px solid var(--ink)}
.bd>.bc:first-child{margin-top:12px}
.bh{display:flex;justify-content:space-between;align-items:center}
.bh h3{font-family:var(--hd);font-weight:800;font-size:23px;line-height:1;text-transform:uppercase;letter-spacing:.03em}
.stt{display:inline-flex;align-items:center;gap:4px;font-family:var(--tc);font-weight:700;font-size:11.5px;line-height:16px;text-transform:uppercase;letter-spacing:.07em;border-radius:2px;padding:2px 7px;border:1.5px solid transparent}
.stt.ok{color:var(--pos);border-color:currentColor}
.stt.wn{background:var(--neg);color:var(--on-neg)}
.bt{display:grid;grid-template-columns:1fr 1fr 1fr 66px;margin-top:9px;border-top:1px solid var(--rule);border-bottom:1px solid var(--rule)}
.bt div{padding:5px 0 6px;text-align:right}
.bt div:first-child{text-align:left}
.bt small{display:block;font-family:var(--tc);font-weight:600;font-size:10.5px;line-height:14px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut)}
.bt b{font-family:var(--tc);font-weight:600;font-size:16.5px;line-height:22px}
.bt .pl{font-size:18px;line-height:20px;padding:0 6px}
.warn .bt .pl{background:var(--neg);color:var(--on-neg);border-color:transparent;text-shadow:none}
.bb{position:relative;height:9px;background:var(--rule);border-radius:1px;margin-top:13px}
.bb i{position:absolute;left:0;top:0;bottom:0;background:var(--pos);border-radius:1px}
.warn .bb i{background:var(--neg)}
.bb u{position:absolute;top:-5px;bottom:-5px;width:2px;margin-left:-1px;background:var(--ink)}
.ft{display:flex;align-items:center;gap:8px;font-size:13px;line-height:1.35;color:var(--mut);border-top:2px solid var(--ink);padding-top:9px}
.ft u{flex:none;width:2px;height:18px;background:var(--ink)}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:216px;background:var(--side);color:var(--on-g);padding:22px 16px 0 14px;display:flex;flex-direction:column}
.sd::after{content:'';position:absolute;top:0;bottom:0;right:0;width:4px;background:repeating-linear-gradient(25deg,var(--chalk) 0 6px,color-mix(in srgb,var(--chalk) 70%,#000) 6px 8px)}
.brand{display:flex;align-items:center;gap:9px;padding:0 6px;margin-bottom:18px;font-family:var(--hd);font-weight:900;font-size:34px;line-height:1;text-transform:uppercase;letter-spacing:.05em}
.bl{width:22px;height:22px;border-radius:50%;background:radial-gradient(circle at 34% 26%,var(--ball-hi),var(--ball) 60%);box-shadow:0 0 0 2px var(--chalk)}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:42px;margin-bottom:14px;background:var(--ball);color:var(--on-ball);border-radius:4px;font-family:var(--hd);font-weight:800;font-size:18px;text-transform:uppercase;letter-spacing:.05em;box-shadow:inset 0 -3px 0 rgba(0,0,0,.25)}
.addb:active{transform:translateY(1px);box-shadow:none}
.sd nav{display:flex;flex-direction:column;gap:2px}
.ni{display:flex;align-items:center;gap:10px;height:38px;padding:0 10px;border-radius:3px;border:1px solid transparent;font-weight:600;font-size:14.5px}
.ni.on{background:var(--plate);color:var(--on-plate);border-color:var(--plate-bd);box-shadow:inset 0 3px 0 var(--ball)}
.gl{color:var(--par);margin:16px 10px 6px;padding-top:13px;border-top:1px solid color-mix(in srgb,var(--chalk) 30%,transparent)}
.tl{font-size:13.5px;padding:4px 10px;color:var(--on-g2)}
.mn{position:absolute;left:216px;right:0;top:0;bottom:0;display:flex;flex-direction:column}
.dg{background:var(--grass);color:var(--on-g);padding:12px 28px 18px}
.dhero{display:grid;grid-template-columns:444px 1fr;gap:32px;margin-top:6px}
.lc{display:flex;flex-direction:column;gap:10px}
.mn .board{margin:0;padding:12px 16px 16px}
.mn .hl h2{font-size:24px}
.mn .hl{margin-bottom:9px}
.mn .digs{gap:5px}
.mn .digs .r{font-size:40px;margin:0 3px 13px 0}
.mn .digs b{width:58px;height:94px;font-size:84px}
.mn .digs b::before{width:10px;height:5px;margin-left:-5px}
.mn .digs i{font-size:56px;line-height:1;width:9px;margin-bottom:5px}
.mn .hn{font-size:14.5px;line-height:18px}
.mn .pls{gap:10px;margin-top:13px}
.mn .st .pl{font-size:25px;height:36px;line-height:34px}
.dg .al{margin:0;height:42px;font-size:14.5px;background:var(--cell);color:var(--ink);border-color:var(--frame)}
.mn .worm{margin:0;display:flex;flex-direction:column}
.mn .plot{flex:1;height:auto}
.mn .ball{width:18px;height:18px;margin:-9px 0 0 -9px}
.mn .lb{font-size:14px}
.mn .lb.sp{left:8%;top:calc(60.5% - 24px)}
.mn .worm figcaption{font-size:17px;margin-top:8px}
.dw{flex:1;display:grid;grid-template-columns:1fr 1fr 1.32fr;gap:32px;padding:6px 28px 0}
.dw>div{min-width:0}
.dw .sh{margin-top:10px}
.mn .sc thead{display:table-header-group}
.mn .sc th{height:26px;font-family:var(--tc);font-weight:600;font-size:11px;text-transform:uppercase;letter-spacing:.09em;color:var(--mut);text-align:left;border-bottom:1px solid var(--rule)}
.mn .sc th.v{text-align:right;font-size:11px}
.mn .sc td{height:70px}
.mn .sc td small{display:block;margin:2px 0 0}
.mn .sc .v{font-size:17.5px}
.mn .sc tfoot td{height:66px;font-size:18px}
.mn .sc tfoot .pl{font-size:24px;line-height:32px;padding:0 10px}
.go{padding:26px 0 24px;border-bottom:1px solid var(--rule)}
.gh{display:flex;justify-content:space-between;align-items:center}
.gh b{font-weight:600;font-size:15.5px}
.gh .pl{font-size:18px;line-height:22px;padding:0 7px}
.go .bb{margin-top:14px;height:11px}
.go small{display:block;font-family:var(--tc);font-weight:500;font-size:14px;color:var(--mut);margin-top:8px}
.dw .rw{padding:17px 0}
.dt{width:50px;flex:none;font-family:var(--tc);font-weight:600;font-size:13px;color:var(--mut)}
`;

export const screens = { home, txns, add, budgets, desk };
