import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 7,
  slug: 'mahal',
  name: 'Mahal',
  defaultMode: 'dark',
  tagline:
    'Bombay Art Deco after dark — Marine Drive facades, gilt sunbursts and terrazzo floors. For people who like their money presented with a little ceremony, and every figure still crisp.',
  blurb:
    'Marine Drive deco: midnight peacock and antique gold, stepped ziggurat corners, porthole chips and a gilt sunburst gauge that fans open over your safe-to-spend.',
  fonts: 'https://fonts.googleapis.com/css2?family=Limelight&family=Jost:wght@300;400;500;600&display=swap',
  swatch: ['#f4ecdc', '#0f3a3f', '#9a7a35', '#1b7550'],
  swatchDark: ['#0c1f22', '#f3ead6', '#d6b46a', '#6fd3a3'],
  notes: [
    ['Shape', 'Stepped-notch corners on every panel with a gold outline and an inset keyline; the hero is an arched proscenium on ziggurat shoulders. Chips are portholes; buttons are elongated with chamfered ends.'],
    ['Type', 'Limelight — a high-contrast deco display — for titles and the hero figure (64px, numerals stay open). Jost with tabular numerals for every row, label and keypad digit; inline labels in letter-spaced caps.'],
    ['Density', 'Ceremonial and symmetric: centred hero, centred date rules, generous rhythm. Fewer things per screen, each given a frame.'],
    ['Motion', 'The sunburst fans open ray by ray on load (ease-out, staggered 22ms) and the pace needle swings into place after it; everything else stays still.'],
    ['Voice', 'Polished, a touch grand, like a cinema marquee — only in gaps: “Now showing · October 2026” lettered around the arch. Labels stay the product’s own.'],
    ['Signature', 'The sunburst gauge: a half-fan of gilt rays inside the proscenium, rays lit = share of budget spent, an ivory needle at today’s pace. Budgets get a small fan each; Housing burns vermilion.'],
  ],
};

/* ---------- authored pieces ---------- */
const R = (s) => `<span class="rs">₹</span>${s}`; // ₹ set in Jost beside Limelight figures

// Account glyphs, same 1.5 stroke idiom as ic()
const kindIc = {
  bank: (s) => `<svg class="ic" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 9.5 12 4l9 5.5z"/><path d="M5.5 10.5v7M10 10.5v7M14 10.5v7M18.5 10.5v7"/><path d="M3.5 20.5h17M4.5 18h15"/></svg>`,
  savings: (s) => `<svg class="ic" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><ellipse cx="12" cy="6.5" rx="7" ry="2.5"/><path d="M5 6.5v4c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-4"/><path d="M5 10.5v4c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-4"/><path d="M5 14.5v3c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-3"/></svg>`,
  card: (s) => `<svg class="ic" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2.5" y="5" width="19" height="14" rx="1.5"/><path d="M2.5 9.5h19M6 15h4"/></svg>`,
};

// Sunburst: n rays fanning 0..180deg from the left; lit = share of budget spent; needle = pace
const fan = ({ cx, cy, r0, r1, r2, n, pct, pace, hot = false, w = 1.6 }) => {
  let s = '';
  for (let i = 0; i < n; i++) {
    const a = (i * 180) / (n - 1);
    const lit = pct > 0 && a <= pct * 1.8 + 0.01;
    const rr = i % 2 ? r2 : r1;
    s += `<line class="ray${lit ? ' on' : ''}" x1="${cx - r0}" y1="${cy}" x2="${cx - rr}" y2="${cy}" stroke-width="${w}" style="--a:${a.toFixed(2)}deg;--i:${i};transform-origin:${cx}px ${cy}px"/>`;
  }
  const na = (pace * 1.8).toFixed(2);
  s += `<g class="ndl" style="--a:${na}deg;transform-origin:${cx}px ${cy}px"><line x1="${cx - r0 + 4}" y1="${cy}" x2="${cx - r1 - 7}" y2="${cy}"/><path d="M${cx - r1 - 7} ${cy}l3.5 -2.6 3.5 2.6 -3.5 2.6z"/></g>`;
  return `<g class="fan${hot ? ' hot' : ''}">${s}</g>`;
};

// Proscenium hero: 350 x 312 arched frame on stepped shoulders, double gold keyline
const OUT = 'M0 312V196H12V180H26V164H47V140A128 128 0 0 1 303 140V164H324V180H338V196H350V312Z';
const INN = 'M6 306V202H18V186H32V170H53V140A122 122 0 0 1 297 140V170H318V186H332V202H344V306Z';
const hero = (id) => `<section class="hero" aria-label="Safe to spend today ₹${D.safe}">
  <svg class="pros" viewBox="0 0 350 312" width="350" height="312" aria-hidden="true">
    <path class="pf" d="${OUT}"/><path class="pk" d="${OUT}"/><path class="pk2" d="${INN}"/>
    <path id="arc-${id}" d="M65 140A110 110 0 0 1 285 140" fill="none"/>
    <text class="marq"><textPath href="#arc-${id}" startOffset="50%" text-anchor="middle">Now showing · October 2026</textPath></text>
    <path class="hl" d="M77 140A98 98 0 0 1 273 140"/>
    ${fan({ cx: 175, cy: 140, r0: 54, r1: 92, r2: 84, n: 25, pct: D.pct, pace: D.dayPct })}
    <path class="base" d="M53 140.5H297"/>
    <text class="hub" x="175" y="131" text-anchor="middle">${D.pct}%</text>
  </svg>
  <div class="hc">
    <div class="lab">Safe to spend today</div>
    <div class="fig">${R(D.safe)}</div>
    <p class="sub">₹${D.left} left this month · ${D.days} days to go</p>
    <p class="leg"><span>Spent <b>₹${D.spent}</b> of ₹${D.budget}</span><span class="ok"><u></u>Under pace</span></p>
  </div>
</section>`;

const po = (inner, cls = '') => `<span class="po${cls}">${inner}</span>`;
const ib = (i) => `<span class="ib">${ic(i, 18, 1.5)}</span>`;
const sh = (t, link = 'See all') => `<div class="sh"><h2>${t}</h2><i class="rl"></i>${link ? `<a>${link}</a>` : ''}</div>`;
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => `<span${n === active ? ' class="on"' : ''}>${ic(i, 20, 1.5)}<em>${n}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 24, 1.75)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;
const homeHd = `<header class="hd"><div class="ti"><h1 class="nm0">${D.user}</h1><p>${ic('shield', 12, 1.5)} Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></header>`;

const tag = (t) => (t ? ` · <span class="tg${t === 'Essential' ? ' e' : ' d'}"><u></u>${t}</span>` : '');
const row = (r) => `<div class="rw">${po(ic(r.icon, 16, 1.5))}<div class="nm"><b>${r.name}${r.rec ? ' <span class="rec" aria-label="Recurring">' + ic('repeat', 12, 1.75) + '</span>' : ''}</b><small>${r.cat} · ${r.acct}${tag(r.tag)}</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const accRow = (a) => `<div class="rw">${po(kindIc[a.kind](16))}<div class="nm"><b>${a.name}</b><small>${a.type}</small></div><span class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`;
const accTile = (a) => `<div class="at pn nk">${po(kindIc[a.kind](18), ' lg')}<b>${a.name}</b><span class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`;
const due = `<div class="due pn nk">${po(ic('alert', 15, 1.75), ' dn')}<p>${D.due.name} payment of <b>₹${D.due.amt}</b> is due in ${D.due.days} days</p></div>`;
const tot = `<div class="tot"><span>Total balance</span><b>₹${D.total}</b></div>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  ${homeHd}
  ${hero('h')}
  ${due}
  ${sh('Where it sits')}
  <div class="ats">${D.accounts.map(accTile).join('')}</div>
  ${tot}
  ${sh('Latest')}
  <div class="lt">${row(D.groups[0].rows[1])}</div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search') + ib('eye'))}
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo">${po(ic('chevl', 16, 1.5))}<b>October 2026</b>${po(ic('chevr', 16, 1.5))}</div>
  ${D.groups.map((g) => `<div class="dl"><span>${g.date}</span></div>${g.rows.map(row).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back'), '')}
  <div class="ty t3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtb"><small>Amount</small><div class="fig">${R('2,450')}<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 1.25)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fl pn nk">${po(kindIc.bank(16))}<div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 16, 1.5)}</div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${po(ic(i, 19, 1.5), ' lg')}${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back'), ib('eye') + ib('plus'))}
  ${D.budgets
    .map((b) => {
      const hot = b.pct > 85;
      return `<section class="bu pn${hot ? ' hot' : ''}">
    <svg class="mini" viewBox="0 0 92 52" width="92" height="52" role="img" aria-label="${b.pct}% of budget spent">${fan({ cx: 46, cy: 48, r0: 15, r1: 42, r2: 35, n: 15, pct: b.pct, pace: D.dayPct, hot, w: 1.5 })}<path class="base" d="M2 48.5H90"/></svg>
    <div class="bx">
      <div class="bh"><h3>${b.name}</h3><span class="pc">${b.pct}%</span></div>
      <div class="bs"><span class="st${hot ? ' wn' : ''}"><u></u>${b.state}</span><small>Monthly · October 2026</small></div>
      <div class="bf"><span><b>₹${b.spent}</b> of ₹${b.limit}</span><span>₹${b.left} left</span></div>
    </div>
  </section>`;
    })
    .join('')}
</div>
${tabs('Settings')}`;

const logo = `<svg width="30" height="18" viewBox="0 0 92 52" aria-hidden="true" class="lg">${[0, 30, 60, 90, 120, 150, 180].map((a) => `<line x1="31" y1="48" x2="6" y2="48" style="transform:rotate(${a}deg);transform-origin:46px 48px"/>`).join('')}<path d="M2 49H90"/></svg>`;

const desk = `
<aside class="sd">
  <div class="logo">${logo}Finio</div>
  <span class="addb">${ic('plus', 16, 1.75)} Add transaction</span>
  ${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 18, 1.5)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni t">${t}</span>`).join('')}
</aside>
<main class="mn">
  ${homeHd}
  <div class="dg">
    <div>${hero('d')}${due}</div>
    <div>
      ${sh('Where it sits')}
      ${D.accounts.map(accRow).join('')}
      ${tot}
      ${sh('Savings goals')}
      ${D.goals
        .map(
          (g) => `<div class="gr"><div class="gt"><b>${g.name}</b><span>${g.pct}%</span></div><div class="stp" role="img" aria-label="${g.pct}% saved">${Array.from({ length: 20 }, (_, k) => `<i${k < g.pct / 5 ? ' class="on"' : ''}></i>`).join('')}</div><small>₹${g.have} of ₹${g.target}</small></div>`,
        )
        .join('')}
    </div>
    <div>
      ${sh('Latest')}
      ${D.groups.flatMap((g) => g.rows).map(row).join('')}
    </div>
  </div>
</main>`;

/* ---------- styles ---------- */
const NOTCH = (n) =>
  `polygon(${n} 0,calc(100% - ${n}) 0,calc(100% - ${n}) ${n},100% ${n},100% calc(100% - ${n}),calc(100% - ${n}) calc(100% - ${n}),calc(100% - ${n}) 100%,${n} 100%,${n} calc(100% - ${n}),0 calc(100% - ${n}),0 ${n},${n} ${n})`;
const CHAMF = 'polygon(16px 0,calc(100% - 16px) 0,100% 50%,calc(100% - 16px) 100%,16px 100%,0 50%)';
const ZIG = 'polygon(0 100%,0 70%,18% 70%,18% 38%,34% 38%,34% 0,66% 0,66% 38%,82% 38%,82% 70%,100% 70%,100% 100%)';

export const css = `
.app{
  --bg:#f4ecdc;--surf:#fbf6ea;--ink:#0f3a3f;--mut:#4d6562;--gold:#9a7a35;--goldt:#7a5a1c;--gline:rgba(154,122,53,.45);--hair:rgba(154,122,53,.22);
  --pos:#1b7550;--neg:#b3321b;--warn:#8a5600;--btn:#0f3a3f;--btntx:#f6eedd;--needle:#0f3a3f;--unlit:rgba(154,122,53,.24);
  --tz1:rgba(15,58,63,.07);--tz2:rgba(154,122,53,.16);--tz3:rgba(179,50,27,.06);
  background-color:var(--bg);
  background-image:radial-gradient(circle at 22% 31%,var(--tz1) 0 1.1px,transparent 1.7px),radial-gradient(circle at 71% 64%,var(--tz2) 0 1.4px,transparent 2px),radial-gradient(circle at 47% 12%,var(--tz3) 0 .9px,transparent 1.4px),radial-gradient(circle at 83% 22%,var(--tz2) 0 .8px,transparent 1.3px),radial-gradient(circle at 12% 78%,var(--tz1) 0 1.6px,transparent 2.2px);
  background-size:37px 41px,53px 47px,29px 33px,61px 59px,83px 79px;
  color:var(--ink);font-family:'Jost',system-ui,sans-serif;font-size:14px;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--gold)}
html[data-mode=dark] .app{
  --bg:#0c1f22;--surf:#112a2e;--ink:#f3ead6;--mut:#b5b09b;--gold:#d6b46a;--goldt:#d6b46a;--gline:rgba(214,180,106,.42);--hair:rgba(214,180,106,.16);
  --pos:#6fd3a3;--neg:#ff8266;--warn:#f2b84f;--btn:#d6b46a;--btntx:#0c1f22;--needle:#f3ead6;--unlit:rgba(214,180,106,.2);
  --tz1:rgba(243,234,214,.05);--tz2:rgba(214,180,106,.12);--tz3:rgba(111,211,163,.06)}
.app ::selection{background:var(--gold);color:var(--bg)}
.app :focus-visible{outline:1.5px solid var(--gold);outline-offset:3px}
.app .pg{position:absolute;inset:44px 0 78px;padding:4px 20px;overflow:hidden}

/* panels: gold outline + stepped-notch corners + inset keyline */
.pn{position:relative;isolation:isolate;background:var(--gline);clip-path:${NOTCH('7px')}}
.pn::before{content:'';position:absolute;inset:1px;background:var(--surf);clip-path:${NOTCH('7px')};z-index:-1}
.pn::after{content:'';position:absolute;inset:9px;border:1px solid var(--hair);z-index:-1;pointer-events:none}
.pn.nk::after{display:none}

/* portholes */
.po{flex:none;width:36px;height:36px;border-radius:50%;display:grid;place-items:center;color:var(--gold);border:1px solid var(--gline);box-shadow:inset 0 0 0 3px var(--surf),inset 0 0 0 4px var(--hair);background:var(--surf)}
.po.lg{width:46px;height:46px}
.po.dn{width:30px;height:30px;color:var(--neg);border-color:var(--neg)}

/* header */
.hd{display:flex;align-items:center;gap:10px;padding:4px 0 8px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-family:'Limelight',serif;font-weight:400;font-size:28px;line-height:1.15;letter-spacing:.01em}
.hd h1.nm0{font-size:24px}
.hd p{font-size:12.5px;color:var(--mut);display:flex;gap:5px;align-items:center;margin-top:2px;letter-spacing:.02em}
.hd p .ic{color:var(--gold)}
.hd .rt{display:flex;gap:8px}
.ib{width:38px;height:38px;border-radius:50%;display:grid;place-items:center;color:var(--ink);border:1px solid var(--gline)}

/* hero proscenium */
.hero{position:relative;width:350px;height:312px;margin:0 auto}
.pros{position:absolute;inset:0;overflow:visible}
.pf{fill:var(--surf)}
.pk{fill:none;stroke:var(--gold);stroke-width:1.25}
.pk2{fill:none;stroke:var(--gline);stroke-width:1}
.hl{fill:none;stroke:var(--hair);stroke-width:1}
.base{fill:none;stroke:var(--gline);stroke-width:1}
.marq{font-family:'Jost',sans-serif;font-size:10.5px;font-weight:500;letter-spacing:.24em;text-transform:uppercase;fill:var(--goldt)}
.hub{font-family:'Limelight',serif;font-size:27px;fill:var(--ink)}
.ray{stroke:var(--unlit);stroke-linecap:round;transform:rotate(var(--a));animation:fan 1.1s cubic-bezier(.16,1,.3,1) calc(var(--i) * 22ms) backwards}
.ray.on{stroke:var(--gold)}
.fan.hot .ray.on{stroke:var(--neg)}
.ndl{transform:rotate(var(--a));animation:fan 1.3s cubic-bezier(.16,1,.3,1) .45s backwards}
.ndl line{stroke:var(--needle);stroke-width:1.25;stroke-linecap:round}
.ndl path{fill:var(--needle)}
@keyframes fan{from{transform:rotate(0deg);opacity:0}}
.hc{position:absolute;left:0;right:0;top:148px;text-align:center;padding:0 24px}
.lab{font-size:11.5px;font-weight:500;letter-spacing:.2em;text-transform:uppercase;color:var(--mut)}
.fig{font-family:'Limelight',serif;font-weight:400;font-size:64px;line-height:1;margin:6px 0 4px;letter-spacing:.01em;animation:rise .9s cubic-bezier(.16,1,.3,1) .2s backwards}
.rs{font-family:'Jost',sans-serif;font-weight:400;font-size:.5em;vertical-align:.62em;margin-right:4px;color:var(--goldt)}
@keyframes rise{from{opacity:0;transform:translateY(8px)}}
.sub{font-size:14px;color:var(--ink)}
.leg{display:flex;justify-content:center;gap:14px;margin-top:12px;padding-top:10px;border-top:1px solid var(--hair);font-size:12.5px;color:var(--mut)}
.leg b{font-weight:500;color:var(--ink)}
.ok,.st{display:inline-flex;align-items:center;gap:6px;color:var(--pos);font-weight:500}
.ok u,.st u,.tg u{display:inline-block;width:6px;height:6px;transform:rotate(45deg);background:currentColor}

/* due */
.due{display:flex;align-items:center;gap:12px;padding:8px 14px;margin-top:10px;font-size:13.5px}
.due p{line-height:1.35}.due b{font-weight:600}

/* sections */
.sh{display:flex;align-items:center;gap:12px;margin:13px 0 7px}
.sh h2{font-family:'Limelight',serif;font-weight:400;font-size:18px;white-space:nowrap}
.sh .rl{flex:1;height:4px;border-top:1px solid var(--gline);border-bottom:1px solid var(--hair)}
.sh a{font-size:11.5px;font-weight:500;letter-spacing:.16em;text-transform:uppercase;color:var(--goldt);white-space:nowrap}

/* account tiles */
.ats{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}
.at{display:flex;flex-direction:column;align-items:center;text-align:center;padding:9px 4px 8px;gap:1px}
.at .po{margin-bottom:4px}
.at .po.lg{width:36px;height:36px}
.at b{font-weight:400;font-size:12.5px;color:var(--mut)}
.at .a{font-size:16px;font-weight:500}
.a.inc{color:var(--pos)}.a.neg{color:var(--neg)}
.tot{display:flex;justify-content:space-between;align-items:baseline;margin-top:8px;padding:7px 2px 0;border-top:3px double var(--gline);color:var(--mut);font-size:13px;letter-spacing:.04em}
.tot b{font-size:19px;font-weight:500;color:var(--ink);letter-spacing:0}

/* rows */
.rw{display:flex;align-items:center;gap:12px;padding:9px 0;border-bottom:1px solid var(--hair)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:500;font-size:15.5px;line-height:1.25}
.rec{color:var(--goldt);display:inline-flex}
.rw small{display:block;font-size:12.5px;color:var(--mut);margin-top:2px}
.rw .a{font-size:16px;font-weight:500;white-space:nowrap}
.tg{white-space:nowrap}.tg u{width:5px;height:5px;margin:0 5px 1px 1px}
.tg.e u{color:var(--pos)}.tg.d u{color:var(--warn)}

.lt .rw{padding-right:68px;border-bottom:0}
/* tab frieze */
.tabs{position:absolute;left:0;right:0;bottom:0;height:78px;background:var(--surf);border-top:3px double var(--gold);display:flex;justify-content:space-around;padding:12px 6px 0;z-index:4}
.tabs span{position:relative;display:flex;flex-direction:column;align-items:center;gap:5px;color:var(--mut);flex:1;min-width:0}
.tabs em{font-style:normal;font-size:9.5px;font-weight:500;letter-spacing:.06em;text-transform:uppercase}
.tabs span.on{color:var(--goldt)}
.tabs span.on em{color:var(--ink)}
.tabs span.on::before{content:'';position:absolute;top:-23px;left:50%;width:40px;height:9px;margin-left:-20px;background:var(--gold);clip-path:${ZIG}}
.fab{position:absolute;right:20px;bottom:96px;width:54px;height:54px;border-radius:50%;background:var(--btn);color:var(--btntx);display:grid;place-items:center;z-index:5;box-shadow:inset 0 0 0 4px var(--btn),inset 0 0 0 5px var(--btntx),0 0 0 4px var(--bg),0 0 0 5px var(--gline)}

/* type filter */
.ty{display:flex;justify-content:center;gap:2px;margin:2px 0 4px;padding:4px;border-top:1px solid var(--gline);border-bottom:1px solid var(--gline)}
.ty span{flex:1;text-align:center;font-size:11.5px;font-weight:500;letter-spacing:.14em;text-transform:uppercase;color:var(--mut);padding:9px 0;clip-path:${CHAMF.replace(/16px/g, '10px')}}
.ty span.on{background:var(--btn);color:var(--btntx)}
.mo{display:flex;justify-content:space-between;align-items:center;padding:12px 0 2px}
.mo .po{width:32px;height:32px;color:var(--ink)}
.mo b{font-family:'Limelight',serif;font-weight:400;font-size:21px}
.dl{display:flex;align-items:center;gap:12px;margin:16px 0 2px}
.dl::before,.dl::after{content:'';flex:1;border-top:1px solid var(--gline)}
.dl span{font-size:11px;font-weight:500;letter-spacing:.2em;text-transform:uppercase;color:var(--goldt)}

/* add */
.amtb{text-align:center;padding:10px 0 2px}
.amtb small{font-size:11px;font-weight:500;letter-spacing:.2em;text-transform:uppercase;color:var(--mut)}
.amtb .fig{display:flex;justify-content:center;align-items:flex-start;margin:4px 0 0;animation:none}
.amtb .rs{vertical-align:0;margin:6px 6px 0 0}
.caret{width:2px;height:54px;background:var(--gold);margin:4px 0 0 6px;animation:bl 1s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);margin:10px 0 12px;border-top:3px double var(--gline);border-bottom:3px double var(--gline);padding:4px 0}
.np span{height:47px;display:grid;place-items:center;font-size:27px;font-weight:300;color:var(--ink)}
.np span:active{color:var(--gold)}
.fl{display:flex;align-items:center;gap:12px;padding:9px 14px}
.fl div{flex:1}.fl small{display:block;font-size:11px;font-weight:500;letter-spacing:.16em;text-transform:uppercase;color:var(--mut)}
.fl b{font-weight:500;font-size:16px}.fl>span{font-size:12.5px;color:var(--mut)}.fl>.ic{color:var(--goldt)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:12px 4px}
.cg>span{display:flex;flex-direction:column;align-items:center;gap:6px;font-size:12px;color:var(--mut)}
.cg>span.on{color:var(--ink);font-weight:600}
.cg>span.on .po{background:var(--btn);color:var(--btntx);border-color:var(--gold);box-shadow:inset 0 0 0 3px var(--btn),inset 0 0 0 4px var(--btntx)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 20px 30px;background:var(--surf);border-top:3px double var(--gold);z-index:4}
.cta button{width:100%;height:54px;border:0;background:var(--btn);color:var(--btntx);font-size:14px;font-weight:600;letter-spacing:.18em;text-transform:uppercase;clip-path:${CHAMF}}

/* budgets */
.bu{display:flex;align-items:center;gap:14px;padding:16px 16px 15px;margin-bottom:10px}
.bu .mini{flex:none;overflow:visible}
.bx{flex:1;min-width:0}
.bh{display:flex;justify-content:space-between;align-items:baseline}
.bh h3{font-size:16px;font-weight:600}
.pc{font-family:'Limelight',serif;font-size:26px;line-height:1}
.bu.hot .pc{color:var(--neg)}
.bs{display:flex;justify-content:space-between;align-items:center;margin:4px 0 7px;font-size:11px}
.st{font-size:10.5px;letter-spacing:.16em;text-transform:uppercase}
.st u{width:5px;height:5px}
.st.wn{color:var(--neg)}
.bs small{color:var(--mut);font-size:11.5px}
.bf{display:flex;justify-content:space-between;font-size:13px;color:var(--mut);border-top:1px solid var(--hair);padding-top:7px}
.bf b{font-weight:500;color:var(--ink)}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:220px;background:var(--surf);border-right:3px double var(--gold);padding:28px 22px;display:flex;flex-direction:column;gap:1px}
.logo{display:flex;align-items:center;gap:10px;font-family:'Limelight',serif;font-size:25px;margin-bottom:24px}
.logo .lg{overflow:visible}.logo .lg line,.logo .lg path{stroke:var(--gold);stroke-width:5;stroke-linecap:round;fill:none}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:44px;margin-bottom:20px;background:var(--btn);color:var(--btntx);font-size:12px;font-weight:600;letter-spacing:.14em;text-transform:uppercase;clip-path:${CHAMF.replace(/16px/g, '14px')}}
.ni{position:relative;display:flex;align-items:center;gap:11px;padding:8px 0 8px 14px;color:var(--mut);font-size:14.5px}
.ni .ic{color:var(--gold)}
.ni.on{color:var(--ink);font-weight:600}
.ni.on::before{content:'';position:absolute;left:0;top:7px;bottom:7px;width:5px;background:var(--gold);clip-path:polygon(0 0,40% 0,40% 20%,100% 20%,100% 80%,40% 80%,40% 100%,0 100%)}
.ni.t{padding:5px 0 5px 14px;font-size:13.5px}
.gl{font-size:11px;font-weight:500;letter-spacing:.2em;text-transform:uppercase;color:var(--goldt);margin:20px 0 6px 14px}
.mn{position:absolute;left:220px;right:0;top:0;bottom:0;padding:22px 34px;overflow:hidden}
.mn .hd{padding:0 0 14px;border-bottom:1px solid var(--hair);margin-bottom:20px}
.dg{display:grid;grid-template-columns:350px 1fr 1fr;gap:0 34px}
.dg .sh{margin-top:0}
.dg .tot+.sh{margin-top:26px}
.dg .due{margin-top:14px}
.dg .rw .po{width:32px;height:32px}
.dg .rw .nm b{font-size:14.5px}
.dg .rw small{font-size:12px}
.dg .rw .a{font-size:15px}
.gr{padding:9px 0 10px;border-bottom:1px solid var(--hair)}
.gt{display:flex;justify-content:space-between;align-items:baseline}
.gt b{font-weight:500;font-size:14.5px}.gt span{font-size:13px;color:var(--goldt);font-weight:500}
.stp{display:grid;grid-template-columns:repeat(20,1fr);gap:2px;margin:7px 0 5px;height:7px}
.stp i{background:var(--unlit)}.stp i.on{background:var(--gold)}
.gr small{font-size:12px;color:var(--mut)}
`;

export const screens = { home, txns, add, budgets, desk };
