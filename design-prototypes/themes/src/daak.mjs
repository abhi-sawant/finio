import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 18,
  slug: 'daak',
  name: 'Daak',
  tagline: 'The post. Your money kept like correspondence on a pale-blue inland letter — every account a postage stamp, every day postmarked — for people who like things stamped, dated and filed.',
  blurb: 'India Post stationery. Inland-letter blue with pillar-box red, accounts as perforated stamps, a struck date postmark on the hero and a carmine rubber stamp on a budget near its limit.',
  fonts:
    'https://fonts.googleapis.com/css2?family=Courier+Prime:wght@400;700&family=Public+Sans:wght@400;500;600;700&family=Young+Serif&display=swap',
  swatch: ['#c5dcef', '#13213d', '#c4201b', '#3d5412'],
  swatchDark: ['#0d1730', '#d3e4f5', '#f0473c', '#a9c46a'],
  notes: [
    ['Shape', 'Paper rectangles with 0–2px corners. Panels carry the inland letter’s printed double border rule; sections are separated by dashed fold lines; fields are a label above a ruled line. Stamps and the tab bar have a real perforated edge (CSS mask).'],
    ['Type', 'Young Serif for titles, stamp values and the hero figure (56px phone, 84px desktop; the ₹ falls back to Public Sans because Young Serif has no rupee glyph). Public Sans with tabular numerals for rows and labels. Courier Prime only for postmark and dateline stamps.'],
    ['Density', 'Medium and orderly, like a filled-in form: Home fits the hero, one notice, a three-stamp strip, the total and the latest entry; budgets are three ruled fields and a quartered bar each.'],
    ['Motion', 'One moment: the postmark is struck on load — scale 1.25→1, rotate −14°→−9°, opacity 0→.85 with an ink-bleed blur resolving, 350ms ease-out. Keys and buttons only press down 1px.'],
    ['Voice', 'Plain. The only in-world words are true ones: the postmark reads “15 OCT 2026”, “THU” and “DAY 15 OF 31”; the notice stamp reads “DUE IN 3 DAYS”. No pin codes, no addresses.'],
    ['Signature', 'The postmark and the stamps: an authored double-ring date postmark with wavy cancellation lines in the hero corner, accounts as a strip of perforated definitive stamps, and a tilted double-bordered “NEAR LIMIT” rubber stamp.'],
  ],
};

/* ---------- helpers ---------- */
const R = '<span class="ru">₹</span>';
const ib = (i, label) => `<span class="ib" role="button" aria-label="${label}">${ic(i, 19, 1.6)}</span>`;
const tabs = (active = '') =>
  `<nav class="tabs">${D.nav.map(([nm, i]) => `<span${nm === active ? ' class="on"' : ''}>${ic(i, 20, 1.6)}<em>${nm}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" role="button" aria-label="Add transaction">${ic('plus', 24, 2.2)}</span>`;

const INK = { Income: 'olive', Housing: 'violet', Food: 'ochre', Transport: 'slate', Shopping: 'carmine', Entertainment: 'teal', Utilities: 'olive', Healthcare: 'carmine', Education: 'slate' };
const ACC = { bank: ['violet', 'home'], savings: ['olive', 'wallet'], card: ['carmine', 'banknote'] };

// small perforated stamp used as an icon holder
const ms = (icon, ink, size = 17) => `<span class="ms" style="--c:var(--${ink})" aria-hidden="true"><i>${ic(icon, size, 1.7)}</i></span>`;

// the signature: double-ring date postmark with wavy cancellation lines
const postmark = (id, cls = '') => `<svg class="pm ${cls}" viewBox="0 0 200 140" aria-hidden="true" fill="none" stroke="currentColor">
  <defs><path id="${id}" d="M70 116a46 46 0 1 1 .01 0"/></defs>
  <circle cx="70" cy="70" r="62" stroke-width="2.2"/><circle cx="70" cy="70" r="57.5" stroke-width="1"/>
  <circle cx="70" cy="70" r="37" stroke-width="1.4"/>
  <text class="pmr" stroke="none" fill="currentColor"><textPath href="#${id}" startOffset="9" textLength="262" lengthAdjust="spacing">FINIO · KEPT ONLY ON THIS DEVICE</textPath></text>
  <circle cx="70" cy="114.5" r="2" fill="currentColor" stroke="none"/>
  <path d="M35 59h70M35 81h70" stroke-width="1.5"/>
  <text class="pmd" x="70" y="74.2" text-anchor="middle" stroke="none" fill="currentColor">15 OCT 2026</text>
  <text class="pms" x="70" y="52.5" text-anchor="middle" stroke="none" fill="currentColor">THU</text>
  <text class="pms" x="70" y="93.5" text-anchor="middle" stroke="none" fill="currentColor">DAY 15 OF 31</text>
  <g stroke-width="1.7" stroke-linecap="round">${[44, 57, 70, 83, 96]
    .map((y, k) => `<path d="M${k === 2 ? 136 : k === 1 || k === 3 ? 134 : 129} ${y}q7-6 14 0t14 0 14 0 14 0 14 0"/>`)
    .join('')}</g>
</svg>`;

const who = `<div class="who">
  <span class="ms av" style="--c:var(--slate)" aria-hidden="true"><i>A</i></span>
  <div><b>${D.user}</b><small>${ic('shield', 12, 1.7)}Kept only on this device</small></div>
</div>`;

const hero = (id, big = false) => `<section class="hero${big ? ' big' : ''}">
  ${postmark(id)}
  <div class="ht">
    <h2>Safe to spend today</h2>
    <div class="fig">${R}${D.safe}</div>
    <p class="sub"><b>₹${D.left}</b> left this month · <span class="nw">${D.days} days to go</span></p>
  </div>
  <div class="hb">
    <div class="rb" role="img" aria-label="${D.pct}% of budget spent"><i style="width:${D.pct}%"></i></div>
    <div class="hl"><span>Spent <b>₹${D.spent}</b> of ₹${D.budget}</span><b>${D.pct}%</b></div>
    <p class="dt">Thu, 15 Oct 2026 · Day 15 of 31</p>
  </div>
</section>`;

const alert = `<p class="al"><span class="due">Due in ${D.due.days} days</span><span>${D.due.name} payment of <b>₹${D.due.amt}</b> due in ${D.due.days} days</span></p>`;

const sh = (t, link = 'See all') => `<div class="sh"><h2>${t}</h2>${link ? `<a>${link}</a>` : ''}</div>`;

const strip = `<div class="strip">${D.accounts
  .map((a) => {
    const [ink, icon] = ACC[a.kind];
    return `<div class="stw"><div class="stp" style="--c:var(--${ink})"><div class="blk">
      <span class="ty2">${a.type}${ic(icon, 14, 1.7)}</span>
      <b>${a.name}</b>
      ${a.owed ? '<em class="ow">Owed</em>' : ''}
      <span class="val">${a.owed ? '−' : ''}${R}${a.bal}</span>
    </div></div></div>`;
  })
  .join('')}</div>
<div class="tot"><span>Total balance</span><b>${R}${D.total}</b></div>`;

const row = (r, date = '') =>
  `<div class="rw">${ms(r.icon, INK[r.cat] || 'slate')}<div class="nm"><b>${r.name}${r.rec ? ic('repeat', 13, 1.9) : ''}</b><small>${date ? `<span class="rd">${date}</span>` : ''}${r.cat} · ${r.acct}${
    r.tag ? `<span class="tg ${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</span>` : ''
  }</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const hd = (title, l = '', r = '') => `<header class="hd">${l}<h1>${title}</h1><div class="rt">${r}</div></header>`;

/* ---------- screens ---------- */
const home = `
<div class="pg">
  <header class="top">${who}<div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero('pmA')}
  ${alert}
  ${sh('Where it sits')}
  ${strip}
  ${sh('Latest')}
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span class="ib sm" role="button" aria-label="Previous month">${ic('chevl', 16, 1.9)}</span><b>October 2026</b><span class="ib sm" role="button" aria-label="Next month">${ic('chevr', 16, 1.9)}</span></div>
  ${D.groups.map((g) => `<div class="dl"><span>${g.date}</span></div>${g.rows.map((r) => row(r)).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg nb">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty t3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amt"><small>Amount</small><div class="fig">${R}2,450<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.6)].map((k) => `<span role="button">${k}</span>`).join('')}</div>
  <div class="fl"><small>Account</small><div><b>HDFC Checking</b><span>Bank account</span>${ic('chevd', 16, 1.9)}</div></div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats
    .map(
      ([nm, i], k) =>
        `<div class="cs${k === 0 ? ' on' : ''}"><div class="stp" style="--c:var(--${INK[nm]})"><div class="blk">${ic(i, 20, 1.6)}</div><em>${nm}</em></div>${
          k === 0 ? '<svg class="ring" viewBox="0 0 40 40" aria-hidden="true" fill="none" stroke="currentColor"><circle cx="20" cy="20" r="18" stroke-width="2"/><circle cx="20" cy="20" r="13.5" stroke-width="1.2"/><path d="M12 20.5l5.5 5.5L28.5 14.5" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>' : ''
        }</div>`,
    )
    .join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  <div class="per"><b>Monthly · October 2026</b><span class="dt">Day 15 of 31</span></div>
  ${D.budgets
    .map((b) => {
      const near = b.state === 'Near limit';
      return `<section class="bg${near ? ' near' : ''}">
    <div class="bh"><h3>${b.name}</h3>${near ? `<span class="rs">${b.state}</span>` : `<span class="ok">${ic('check', 14, 2.4)}${b.state}</span>`}</div>
    <div class="bf"><div><small>Spent</small><b>₹${b.spent}</b></div><div><small>Limit</small><b>₹${b.limit}</b></div><div><small>Left</small><b>₹${b.left}</b></div></div>
    <div class="bb"><div class="rb q"><i style="width:${b.pct}%"></i></div><strong>${b.pct}%</strong></div>
  </section>`;
    })
    .join('')}
</div>
${tabs()}`;

const desk = `
<aside class="sd">
  <div class="mark"><span class="ms av" style="--c:var(--red)" aria-hidden="true"><i>F</i></span><b>Finio</b></div>
  <span class="addb" role="button">${ic('plus', 17, 2.2)}Add transaction</span>
  <nav>${D.nav.map(([nm, i]) => `<span class="ni${nm === 'Home' ? ' on' : ''}">${ic(i, 18, 1.6)}${nm}</span>`).join('')}</nav>
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="tl">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="top">${who}<div class="rt"><span class="dt box">Thu, 15 Oct 2026</span>${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  <div class="dg">
    <div class="c1">
      ${hero('pmB', true)}
      ${alert}
      <div>${sh('Where it sits')}${strip}</div>
    </div>
    <div class="c2">
      <div>${sh('Savings goals')}
      ${D.goals
        .map(
          (g) =>
            `<div class="gr"><div class="gh"><b>${g.name}</b><strong>${g.pct}%</strong></div><div class="rb q"><i style="width:${g.pct}%"></i></div><small>₹${g.have} of ₹${g.target}</small></div>`,
        )
        .join('')}</div>
      <div class="lt">${sh('Latest')}${D.groups.flatMap((g) => g.rows.map((r) => row(r, g.date.split(', ')[1]))).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{--bg:#c5dcef;--card:#dbeaf7;--paper:#fbfaf5;--side:#a9cae6;--ink:#13213d;--mut:#35486a;--line:#6f97c0;--fold:rgba(19,33,61,.42);--print:#235089;
  --red:#c4201b;--redt:#9c1410;--on-red:#fff8f0;--base:#2a1210;--pos:#3d5412;--warn:#6d4300;--neg:#9d1230;--pmk:#1c3566;
  --sp:#fbfaf5;--st:#fbfaf5;--carmine:#a3213a;--olive:#55671f;--ochre:#8d590b;--violet:#663a58;--slate:#2c5a86;--teal:#2d6a66;--shadow:rgba(19,33,61,.3);
  --hd:'Young Serif',Georgia,serif;--tx:'Public Sans',system-ui,sans-serif;--mono:'Courier Prime','Courier New',monospace;
  background:var(--bg);color:var(--ink);font-family:var(--tx);font-size:14px;line-height:1.3;font-variant-numeric:tabular-nums lining-nums;caret-color:var(--red)}
html[data-mode=dark] .app{--bg:#0d1730;--card:#13213f;--paper:#1b2b50;--side:#091124;--ink:#d3e4f5;--mut:#9ab0d0;--line:#3f5c8c;--fold:rgba(211,228,245,.38);--print:#8fb4e0;
  --red:#f0473c;--redt:#ff8479;--on-red:#1c0705;--base:#4a0f0b;--pos:#a9c46a;--warn:#e3b65e;--neg:#ff8a96;--pmk:#bcd7f2;
  --sp:#cfd7df;--st:#f4f2ea;--carmine:#8f1d33;--olive:#4a5a1b;--ochre:#7a4d09;--violet:#57314b;--slate:#254c72;--teal:#265a57;--shadow:rgba(0,0,0,.5)}
.app ::selection{background:var(--red);color:var(--on-red)}
.app :focus-visible{outline:2px solid var(--red);outline-offset:2px}
.app h1,.app h2,.app h3{font-family:var(--hd);font-weight:400}
.ru{font-family:var(--tx);font-weight:600;font-size:.8em;margin-right:.04em}
.app .pg{position:absolute;inset:46px 0 66px;padding:0 16px;overflow:hidden}
.app .pg.nb{bottom:92px}
.dt{font-family:var(--mono);font-weight:700;font-size:12px;letter-spacing:.04em;text-transform:uppercase;color:var(--mut)}
.dt.box{border:1px solid var(--mut);border-radius:2px;padding:4px 8px 2px;margin-right:8px}

/* icon buttons: small paper squares with a printed frame */
.ib{width:38px;height:38px;display:grid;place-items:center;flex:none;background:var(--paper);color:var(--ink);border:1px solid var(--print);border-radius:2px;box-shadow:inset 0 0 0 2px var(--paper),inset 0 0 0 3px var(--line)}
.ib:active{transform:translateY(1px);background:var(--card)}
.ib.sm{width:32px;height:32px}

/* perforation: solid centre + a row of punched holes centred on every edge */
.stp,.ms{--t:8px;--h:2.3px;background:var(--sp);
  -webkit-mask:linear-gradient(#000 0 0) center/calc(100% - var(--t)) calc(100% - var(--t)) no-repeat,radial-gradient(circle,transparent var(--h),#000 calc(var(--h) + .6px)) calc(var(--t)/-2) calc(var(--t)/-2)/var(--t) var(--t) round;
  mask:linear-gradient(#000 0 0) center/calc(100% - var(--t)) calc(100% - var(--t)) no-repeat,radial-gradient(circle,transparent var(--h),#000 calc(var(--h) + .6px)) calc(var(--t)/-2) calc(var(--t)/-2)/var(--t) var(--t) round}
.ms{--t:6px;--h:1.6px;width:36px;height:42px;padding:4px;flex:none;display:block}
.ms i{display:grid;place-items:center;width:100%;height:100%;background:var(--c);color:var(--st);font-style:normal;box-shadow:inset 0 0 0 1.5px var(--c),inset 0 0 0 2.5px rgba(255,255,255,.5)}
.ms.av{width:40px;height:46px}
.ms.av i{font-family:var(--hd);font-size:19px}

/* header */
.top{display:flex;align-items:center;justify-content:space-between;padding:4px 0 10px}
.top .rt,.hd .rt{display:flex;gap:8px;align-items:center}
.who{display:flex;align-items:center;gap:10px}
.who b{display:block;font-family:var(--hd);font-weight:400;font-size:21px;line-height:1.1}
.who small{display:flex;align-items:center;gap:4px;font-size:12px;color:var(--mut);margin-top:3px}

/* hero: the letter-card panel */
.hero{position:relative;overflow:hidden;background:var(--card);border:1px solid var(--print);border-radius:2px;padding:14px 16px 12px;box-shadow:inset 0 0 0 3px var(--card),inset 0 0 0 4px var(--line)}
.hero h2{font-size:16px;line-height:1.2}
.hero .fig{font-family:var(--hd);font-size:56px;line-height:1.08;letter-spacing:-.01em;margin-top:6px;white-space:nowrap}
.hero .fig .ru{font-size:.66em;vertical-align:.14em}
.hero .sub{font-size:13.5px;color:var(--mut);margin-top:3px;max-width:192px}
.nw{white-space:nowrap}
.hero .sub b,.hl span b{color:var(--ink);font-weight:600}
.hb{margin-top:12px;padding-top:12px;border-top:1px dashed var(--fold)}
.hl{display:flex;justify-content:space-between;align-items:baseline;font-size:13.5px;color:var(--mut);margin-top:7px}
.hl>b{font-family:var(--hd);font-weight:400;font-size:17px;color:var(--ink)}
.hero .dt{margin-top:6px}
.pm{position:absolute;top:6px;right:-30px;width:166px;color:var(--pmk);opacity:.85;transform:rotate(-9deg);transform-origin:35% 50%;pointer-events:none;animation:strike .35s cubic-bezier(.16,.84,.3,1) .3s backwards}
@keyframes strike{from{opacity:0;transform:scale(1.25) rotate(-14deg);filter:blur(1.6px)}to{opacity:.85;transform:scale(1) rotate(-9deg);filter:blur(0)}}
.pm text{font-family:var(--mono);font-weight:700}
.pmr{font-size:9.6px}.pmd{font-size:10.6px;letter-spacing:.02em}.pms{font-size:7.6px;letter-spacing:.05em}

/* ruled bar */
.rb{position:relative;height:12px;background:var(--paper);border:1px solid var(--ink)}
.rb i{position:absolute;left:0;top:0;bottom:0;background:var(--print)}
.rb.q::after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,transparent calc(100% - 1px),var(--ink) 0) 0 0/25% 100%;opacity:.55}

/* notice slip */
.al{display:flex;align-items:center;gap:10px;margin-top:10px;background:var(--paper);border:1px solid var(--line);border-radius:2px;padding:8px 10px;font-size:13.5px;line-height:1.3}
.al b{font-weight:700}
.due{flex:none;font-family:var(--mono);font-weight:700;font-size:12px;letter-spacing:.05em;text-transform:uppercase;color:var(--neg);border:1.5px solid var(--neg);border-radius:2px;padding:4px 6px 2px;white-space:nowrap}

/* section heads sit on a fold line */
.sh{display:flex;justify-content:space-between;align-items:baseline;margin-top:13px;padding-top:9px;border-top:1px dashed var(--fold)}
.sh h2{font-size:17px;line-height:1.2}
.sh a{font-size:13px;font-weight:600;color:var(--redt);text-decoration:underline;text-underline-offset:3px;text-decoration-thickness:1px}

/* the stamp strip */
.strip{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:9px}
.stw{filter:drop-shadow(0 1px 1.5px var(--shadow))}
.strip .stp{padding:5px;height:104px}
.blk{position:relative;height:100%;background:var(--c);color:var(--st);padding:9px 8px 7px;display:flex;flex-direction:column}
.blk::after{content:'';position:absolute;inset:3px;border:1px solid rgba(255,255,255,.5);pointer-events:none}
.ty2{display:flex;justify-content:space-between;align-items:center;font-size:11px;opacity:.92;white-space:nowrap}
.blk b{font-weight:600;font-size:12.5px;margin-top:3px;white-space:nowrap}
.ow{align-self:flex-start;margin-top:4px;font-style:normal;font-family:var(--mono);font-weight:700;font-size:11px;letter-spacing:.08em;text-transform:uppercase;border:1px solid currentColor;padding:2px 5px 0;line-height:1.25}
.val{margin-top:auto;font-family:var(--hd);font-size:16.5px;line-height:1.15;white-space:nowrap}
.tot{display:flex;justify-content:space-between;align-items:baseline;padding:9px 2px 5px;border-bottom:1px solid var(--ink);font-size:13.5px;font-weight:500}
.tot b{font-family:var(--hd);font-weight:400;font-size:21px}

/* rows */
.rw{display:flex;align-items:center;gap:11px;padding:8px 0;border-bottom:1px solid var(--line)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:600;font-size:15px;line-height:1.25}
.rw .nm b .ic{color:var(--mut)}
.rw small{display:flex;align-items:center;flex-wrap:wrap;gap:2px 6px;font-size:12px;color:var(--mut);margin-top:3px}
.rw .a{font-size:15.5px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}
.tg{font-size:10.5px;font-weight:600;letter-spacing:.03em;border:1px solid currentColor;border-radius:1px;padding:0 4px;white-space:nowrap}
.tg.e{color:var(--pos)}.tg.d{color:var(--warn)}
.rd{font-family:var(--mono);font-weight:700;text-transform:uppercase;letter-spacing:.03em}

/* tab bar: paper strip with a perforated top edge */
.tabs{position:absolute;left:0;right:0;bottom:0;height:68px;background:var(--paper);display:flex;padding:4px 6px 0;
  -webkit-mask:linear-gradient(#000 0 0) 0 4px/100% 100% no-repeat,radial-gradient(circle,transparent 2.4px,#000 3px) -4px -4px/8px 8px round no-repeat;
  mask:linear-gradient(#000 0 0) 0 4px/100% 100% no-repeat,radial-gradient(circle,transparent 2.4px,#000 3px) -4px -4px/8px 8px round no-repeat}
.tabs span{flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;padding-top:14px;color:var(--mut);position:relative}
.tabs em{font-style:normal;font-size:10.5px;font-weight:500}
.tabs span.on{color:var(--redt)}
html[data-mode=light] .app .tabs span.on,html[data-mode=light] .app .ni.on{color:var(--red)}
.tabs span.on em{font-weight:700}
.tabs span.on::before{content:'';position:absolute;top:5px;left:50%;width:5px;height:5px;margin-left:-2.5px;border-radius:50%;background:currentColor}

/* FAB: the pillar box */
.fab{position:absolute;right:18px;bottom:80px;width:52px;height:62px;border-radius:26px 26px 2px 2px;background:var(--red);color:var(--on-red);display:grid;place-items:center;padding-top:4px;z-index:5;box-shadow:inset 0 -6px 0 var(--base),0 8px 14px -6px var(--shadow)}
.fab::before{content:'';position:absolute;left:5px;right:5px;top:15px;border-top:2px solid rgba(0,0,0,.22)}
.fab .ic{position:relative}
.fab:active{transform:translateY(1px)}

/* page header, switches */
.hd{display:flex;align-items:center;gap:10px;padding:4px 0 10px}
.hd h1{flex:1;font-size:26px;line-height:1.15}
.ty{display:grid;grid-template-columns:repeat(4,1fr);background:var(--paper);border:1px solid var(--ink);border-radius:2px}
.ty.t3{grid-template-columns:repeat(3,1fr)}
.ty span{text-align:center;font-size:13px;font-weight:600;color:var(--mut);padding:9px 0}
.ty span+span{border-left:1px solid var(--line)}
.ty span.on{background:var(--ink);color:var(--bg)}
.mo{display:flex;justify-content:space-between;align-items:center;padding:10px 0 9px;border-bottom:1px solid var(--ink)}
.mo b{font-family:var(--hd);font-weight:400;font-size:17px}
.dl{display:flex;align-items:center;gap:8px;margin:14px 0 2px}
.dl span{font-family:var(--mono);font-weight:700;font-size:12px;letter-spacing:.05em;text-transform:uppercase;color:var(--mut);border:1px solid var(--mut);border-radius:2px;padding:3px 7px 1px}
.dl::after{content:'';flex:1;border-top:1px dashed var(--fold)}
.rw{min-height:62px}

/* add */
.amt{margin-top:10px;border-bottom:2px solid var(--ink);padding-bottom:2px}
.amt small,.fl small,.bf small{display:block;font-size:11.5px;font-weight:600;color:var(--mut)}
.amt .fig{font-family:var(--hd);font-size:52px;line-height:1.1;display:flex;justify-content:flex-end;align-items:center}
.amt .fig .ru{font-size:.62em;margin-right:5px}
.caret{width:2px;height:44px;background:var(--red);margin-left:5px;animation:bl 1.05s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;margin-top:10px}
.np span{height:48px;display:grid;place-items:center;font-family:var(--hd);font-size:23px;background:var(--paper);border:1px solid var(--print);border-radius:2px;box-shadow:inset 0 0 0 3px var(--paper),inset 0 0 0 4px var(--line)}
.np span:active{transform:translateY(1px);background:var(--card)}
.fl{margin-top:10px;border-bottom:1px solid var(--ink);padding-bottom:6px}
.fl div{display:flex;align-items:baseline;gap:8px;margin-top:2px}
.fl b{font-weight:600;font-size:15.5px;flex:1}
.fl span{font-size:13px;color:var(--mut)}
.fl .ic{align-self:center}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:8px 6px;margin-top:9px}
.cs{position:relative;filter:drop-shadow(0 1px 1.2px var(--shadow))}
.cs .stp{--t:7px;--h:1.9px;padding:4px 4px 0;height:72px;display:flex;flex-direction:column}
.cs .blk{height:auto;flex:1;padding:0;align-items:center;justify-content:center}
.cs .blk::after{inset:2px}
.cs em{font-style:normal;font-size:10.5px;font-weight:600;color:#13213d;text-align:center;line-height:19px;white-space:nowrap}
.cs.on{outline:2px solid var(--red);outline-offset:2px}
.ring{position:absolute;top:-7px;right:-7px;width:30px;height:30px;color:var(--red);background:var(--sp);border-radius:50%;transform:rotate(-8deg)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:10px 16px 28px;background:var(--bg);border-top:1px dashed var(--fold)}
.cta button{width:100%;height:52px;border:0;border-radius:2px;background:var(--red);color:var(--on-red);font-weight:700;font-size:16px;box-shadow:inset 0 0 0 3px var(--red),inset 0 0 0 4px rgba(255,255,255,.45),inset 0 -1px 0 4px transparent}
.cta button:active,.addb:active{transform:translateY(1px);filter:brightness(.92)}

/* budgets: form rows */
.per{display:flex;justify-content:space-between;align-items:baseline;padding:2px 0 9px;border-bottom:1px solid var(--ink);font-size:14px}
.per b{font-weight:600}
.bg{padding:17px 0 19px;border-bottom:1px dashed var(--fold)}
.bg:last-child{border-bottom:0}
.bh{display:flex;justify-content:space-between;align-items:center;height:28px}
.bh h3{font-size:18px;line-height:1.2}
.ok{display:flex;align-items:center;gap:4px;font-size:12.5px;font-weight:600;color:var(--pos)}
.rs{font-family:var(--mono);font-weight:700;font-size:13.5px;letter-spacing:.1em;text-transform:uppercase;color:var(--neg);border:2px solid var(--neg);outline:1px solid var(--neg);outline-offset:2px;border-radius:2px;padding:4px 8px 1px;margin-right:6px;transform:rotate(4deg);opacity:.92}
.bf{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:8px}
.bf div{border-bottom:1px solid var(--line);padding-bottom:3px}
.bf b{display:block;font-weight:600;font-size:15px;margin-top:1px}
.bb{display:grid;grid-template-columns:1fr 46px;align-items:center;gap:10px;margin-top:11px}
.bb strong{font-family:var(--hd);font-weight:400;font-size:17px;text-align:right}
.near .rb i{background:var(--neg)}
.near .bb strong{color:var(--neg)}

/* desktop: the address panel and the card */
.sd{position:absolute;left:0;top:0;bottom:0;width:236px;background:var(--side);border-right:1px solid var(--print);padding:24px 22px;display:flex;flex-direction:column}
.sd::after{content:'';position:absolute;top:0;bottom:0;right:3px;border-right:1px solid var(--line)}
.mark{display:flex;align-items:center;gap:10px;margin-bottom:18px}
.mark b{font-family:var(--hd);font-weight:400;font-size:27px}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:44px;background:var(--red);color:var(--on-red);font-weight:700;font-size:14px;border-radius:2px;box-shadow:inset 0 0 0 3px var(--red),inset 0 0 0 4px rgba(255,255,255,.45);margin-bottom:12px}
.sd nav{display:flex;flex-direction:column}
.ni{display:flex;align-items:center;gap:11px;height:42px;font-size:14.5px;font-weight:500;color:var(--ink);border-bottom:1px solid var(--print);position:relative}
.ni.on{color:var(--redt);font-weight:700}
.ni.on::after{content:'';position:absolute;right:4px;top:50%;width:6px;height:6px;margin-top:-3px;border-radius:50%;background:currentColor}
.gl{font-family:var(--hd);font-size:16px;margin:26px 0 2px}
.tl{font-size:13.5px;color:var(--ink);height:33px;display:flex;align-items:flex-end;padding-bottom:6px;border-bottom:1px dashed var(--fold)}
.mn{position:absolute;left:236px;right:0;top:0;bottom:0;padding:18px 30px 24px;display:flex;flex-direction:column}
.mn .top{padding:0 0 14px}
.dg{flex:1;min-height:0;display:grid;grid-template-columns:1.32fr 1fr;gap:32px}
.c1,.c2{display:flex;flex-direction:column;min-height:0;min-width:0}
.c1 .al{margin-top:14px;padding:11px 12px;font-size:14.5px}
.hero.big{flex:1;display:flex;flex-direction:column;justify-content:space-between;padding:24px 26px 20px}
.hero.big h2{font-size:20px}
.hero.big .fig{font-size:84px;margin-top:10px}
.hero.big .sub{font-size:16px;margin-top:6px;max-width:none}
.hero.big .rb{height:16px}
.hero.big .hl{font-size:15px;margin-top:9px}
.hero.big .hl>b{font-size:21px}
.hero.big .pm{width:240px;top:20px;right:-50px}
.mn .sh{margin-top:16px}
.mn .sh h2{font-size:19px}
.mn .strip{gap:10px;margin-top:11px}
.mn .strip .stp{height:176px;padding:6px}
.mn .blk{padding:14px 13px 11px}
.mn .ty2{font-size:12.5px}.mn .ty2 .ic{width:17px;height:17px}
.mn .blk b{font-size:15.5px;margin-top:4px}
.mn .ow{font-size:12px;margin-top:8px}
.mn .val{font-size:27px}
.mn .tot{padding:12px 2px 6px;font-size:15px}
.mn .tot b{font-size:26px}
.c2 .sh:first-child{margin-top:0;border-top:0;padding-top:0}
.gr{padding:11px 0 12px;border-bottom:1px solid var(--line)}
.gh{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:7px}
.gh b{font-weight:600;font-size:15px}.gh strong{font-family:var(--hd);font-weight:400;font-size:16px}
.gr small{display:block;font-size:13px;color:var(--mut);margin-top:6px}
.lt{flex:1;min-height:0;display:flex;flex-direction:column}
.lt .sh{margin-top:22px}
.lt .rw{flex:1;min-height:0;padding:0}
.lt .rw:last-child{border-bottom:0}
`;

export const screens = { home, txns, add, budgets, desk };
