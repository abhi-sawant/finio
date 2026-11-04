import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 5,
  slug: 'bagh',
  name: 'Bagh',
  tagline: 'A kitchen garden. Soft, warm and a little hand-made — money you tend rather than police. Goals grow from seed to sapling, and the sun in your hero tells you how far through the month you are.',
  fonts: 'https://fonts.googleapis.com/css2?family=Gabarito:wght@500;600;700&family=Figtree:wght@400;500;600;700&display=swap',
  swatch: ['#e6efdc', '#16301f', '#3f7d3a', '#f4b21b'],
  swatchDark: ['#0e1a12', '#e9f3e0', '#7fcf7a', '#ffc83d'],
  notes: [
    ['Shape', 'Leaf corners — three big radii and one tight one, flipped from card to card. Pebble buttons, floating tab bar, nothing sharp.'],
    ['Type', 'Gabarito (rounded geometric) for figures and titles, Figtree for reading. Warm, never technical.'],
    ['Density', 'Generous but not sparse: a comfortable, tactile middle. Rows breathe inside soft sheets.'],
    ['Motion', 'Slow and organic: the hero sun eases to its place, the budget fill grows with a leaf at its tip, keys sink like pebbles.'],
    ['Voice', 'Calm and nurturing. Meaning is carried by growth stages and the weather, not by alarms.'],
    ['Signature', 'The month as a sun-arc in the hero, goals drawn as plants (seed → sprout → sapling), and budgets that grow from moss toward marigold.'],
  ],
};

const catColor = { Food: '#f4a259', Transport: '#6bb5c9', Shopping: '#b58ad9', Entertainment: '#e6759a', Utilities: '#f4b21b', Healthcare: '#e0705a', Education: '#6f8fe0', Housing: '#7fb069', Income: '#7fb069' };

const plant = (pct) => {
  const soil = '<ellipse cx="24" cy="42" rx="15" ry="4.5" fill="var(--soil)"/>';
  if (pct === 0) return `<svg width="46" height="46" viewBox="0 0 48 48" aria-label="Seed">${soil}<ellipse cx="24" cy="39" rx="3.6" ry="2.4" fill="var(--gold)"/><path d="M24 36.4v-1.6" stroke="var(--leaf)" stroke-width="1.6" stroke-linecap="round"/></svg>`;
  return `<svg width="46" height="46" viewBox="0 0 48 48" aria-label="Sprout">${soil}<path d="M24 40V25" stroke="var(--leaf2)" stroke-width="3" stroke-linecap="round"/><path d="M24 29c-8 0-11-6-10-10 7-1 10 3 10 10z" fill="var(--leaf)"/><path d="M24 25c7 0 10-5 9-9-6-1-9 3-9 9z" fill="var(--leaf3)"/></svg>`;
};
const sprig = `<svg class="sprig" width="34" height="34" viewBox="0 0 34 34" aria-hidden="true"><path d="M6 30C10 20 16 12 28 6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/><path d="M12 22c-4-1-5-5-4-7 4 0 6 3 4 7zM18 16c-3-2-3-6-1-8 4 1 4 5 1 8zM23 11c1-4 5-5 7-4 0 4-3 6-7 4z" fill="currentColor"/></svg>`;

const arc = `<svg class="arc" viewBox="0 0 342 92" aria-label="Day 15 of 31" role="img">
  <path d="M10 86Q171-36 332 86" fill="none" stroke="var(--ink)" stroke-opacity=".28" stroke-width="1.5" stroke-dasharray="2 6" stroke-linecap="round"/>
  <circle class="sunglow" cx="165" cy="25" r="22" fill="var(--gold)" opacity=".28"/>
  <circle class="sun" cx="165" cy="25" r="11.5" fill="var(--gold)"/>
  <text x="10" y="91" font-size="10.5" fill="var(--mut)" font-family="Figtree" font-weight="600">1 Oct</text>
  <text x="332" y="91" font-size="10.5" fill="var(--mut)" text-anchor="end" font-family="Figtree" font-weight="600">31 Oct</text>
</svg>`;
const hills = `<svg class="hl" viewBox="0 0 342 70" preserveAspectRatio="none" aria-hidden="true"><path d="M0 22C50 4 110 8 170 24S290 36 342 14V70H0z" fill="var(--hill1)"/><path d="M0 40C60 22 130 28 190 40S300 50 342 34V70H0z" fill="var(--hill2)"/></svg>`;

const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => `<span${n === active ? ' class="on"' : ''}>${ic(i, 21, 2)}${n}</span>`).join('')}</nav>`;
const fab = `<span class="fab">${ic('plus', 27, 2.6)}</span>`;
const pb = (i) => `<span class="pb">${ic(i, 18, 2)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;

const item = (r) => `<div class="it"><span class="cat" style="--c:${catColor[r.inc ? 'Income' : r.cat]}">${ic(r.icon, 19, 2)}</span><div class="nm"><b>${r.name}${r.rec ? ' ' + ic('repeat', 12, 2.2) : ''}</b><small>${r.cat} · ${r.acct}${r.tag ? ` <em class="${r.tag === 'Essential' ? 'e' : 'd'}">${r.tag}</em>` : ''}</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const tile = (a, i) => `<div class="tile ${a.kind}${i % 2 ? ' flip' : ''}">${sprig}<b>${a.name}</b><small>${a.type}</small><span class="bal">${a.owed ? '−' : ''}₹${a.short}</span></div>`;
const goal = (g) => `<div class="gi">${plant(g.pct)}<div class="gt"><div class="bm"><b>${g.name}</b><span>₹${g.have} / ₹${g.target}</span></div><div class="bar"><i style="width:${g.pct}%"></i></div></div></div>`;
const alert = `<div class="alert">${ic('clock', 18, 2)}<span>${D.due.name} payment due in ${D.due.days} days · <b>₹${D.due.amt}</b></span></div>`;

const hero = (wide) => `<section class="hero${wide ? ' wide' : ''}">
  <div class="top">${arc}</div>
  <div class="mid"><span class="lab">Safe to spend today</span><div class="fig">₹${D.safe}</div><p>₹${D.left} left this month · ${D.days} days to go</p></div>
  <div class="low">${hills}<div class="lc"><div class="bar"><i style="width:${D.pct}%"></i></div><span class="bl">Spent ₹${D.spent} of ₹${D.budget}</span></div></div>
</section>`;

const home = `
<div class="pg">
  <header class="hd"><div class="ti"><h1 class="nm0">${D.user}</h1><p>${ic('shield', 13, 2)} Kept only on this device</p></div><div class="rt">${pb('eye')}${pb('sliders')}</div></header>
  ${hero(false)}
  ${alert}
  <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
  <div class="strip">${D.accounts.map(tile).join('')}</div>
  <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
  <div class="sheet">${D.goals.map(goal).join('')}</div>
  <div class="sh"><h2>Latest</h2><a>See all</a></div>
  <div class="sheet">${item(D.groups[0].rows[1])}</div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', pb('search') + pb('eye'))}
  <div class="pills"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo">${pb('chevl')}<b>October 2026</b>${pb('chevr')}</div>
  ${D.groups.map((g, k) => `<div class="dl">${g.date}</div><div class="sheet${k % 2 ? ' f2' : ''}">${g.rows.map(item).join('')}</div>`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', pb('back'), '<span style="width:40px"></span>')}
  <div class="seg3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtc"><small>Amount</small><div class="fig">₹2,450<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 2)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="sel"><small>Account</small><b>HDFC Checking</b><em>Bank account</em>${ic('chevd', 17, 2.2)}</div>
  <div class="sh sm"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}><b style="--c:${catColor[n]}">${ic(i, 21, 2)}${k === 0 ? `<u>${ic('check', 11, 3.5)}</u>` : ''}</b>${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', pb('back'), pb('eye') + pb('plus'))}
  ${D.budgets
    .map(
      (b, k) => `<section class="bc${k % 2 ? ' f2' : ''}">
    <div class="bt"><div><h3>${b.name}</h3><small>Monthly · October 2026</small></div><span class="stt ${b.pct > 85 ? 'warn' : 'ok'}">${b.state}</span></div>
    <div class="bm"><span>₹${b.spent} of ₹${b.limit}</span><b>${b.pct}%</b></div>
    <div class="bar${b.pct > 85 ? ' w' : ''}"><i style="width:${b.pct}%"></i></div>
    <div class="bl">₹${b.left} left this month</div>
  </section>`,
    )
    .join('')}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span class="mk">${ic('home', 18, 2.2)}</span>Finio</div>
  <span class="addb">${ic('plus', 19, 2.6)} Add transaction</span>
  ${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 19, 2)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="hd"><div class="ti"><h1 class="nm0">${D.user}</h1><p>${ic('shield', 13, 2)} Kept only on this device</p></div><div class="rt">${pb('eye')}${pb('sliders')}</div></header>
  ${hero(true)}
  ${alert}
  <div class="cols">
    <div>
      <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
      <div class="strip">${D.accounts.map(tile).join('')}</div>
      <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
      <div class="sheet">${D.goals.map(goal).join('')}</div>
    </div>
    <div>
      <div class="sh"><h2>Latest</h2><a>See all</a></div>
      <div class="sheet f2">${D.groups.flatMap((g) => g.rows).map(item).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{--bg:#e6efdc;--sheet:#f8faf1;--rim:#d3dfc4;--ink:#16301f;--mut:#4b6652;--leaf:#4f9a4a;--leaf2:#3f7d3a;--leaf3:#78b45f;--moss:#3a7a36;--moss-t:#d4e8bd;--gold:#f4b21b;--clay:#c4552f;--soil:#8a6446;--pos:#16683a;
  --sky1:#cde8ee;--sky2:#eaf3d4;--hill1:#7fb069;--hill2:#2f6b3a;--shd:0 14px 26px -16px rgb(35 75 40 / .45);
  --leafr:34px 34px 34px 10px;--leafr2:10px 34px 34px 34px;
  background:var(--bg);color:var(--ink);font-family:'Figtree',system-ui,sans-serif;font-weight:500;font-size:14px;font-variant-numeric:tabular-nums;--disp:'Gabarito','Figtree',sans-serif}
html[data-mode=dark] .app{--bg:#0e1a12;--sheet:#172a1d;--rim:#0b140e;--ink:#e9f3e0;--mut:#a3bba8;--leaf:#7fcf7a;--leaf2:#5fae5a;--leaf3:#9be08f;--moss:#7fcf7a;--moss-t:#27472d;--gold:#ffc83d;--clay:#ff8e68;--soil:#5b4330;--pos:#7fe0a3;
  --sky1:#12303a;--sky2:#1a3523;--hill1:#2c6038;--hill2:#17391f;--shd:0 14px 26px -16px rgb(0 0 0 / .7)}
.app ::selection{background:var(--gold);color:#16301f}
.app .pg{position:absolute;inset:44px 0 0;padding:6px 16px;overflow:hidden}
.hd{display:flex;align-items:center;gap:8px;padding:8px 0 14px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-family:var(--disp);font-size:28px;font-weight:700;letter-spacing:-.02em;line-height:1.1}
.hd h1.nm0{font-size:22px}
.hd p{font-size:13px;color:var(--mut);display:flex;gap:5px;align-items:center;margin-top:2px;font-weight:500}
.hd .rt{display:flex;gap:8px}
.pb{width:40px;height:40px;border-radius:50%;background:var(--sheet);box-shadow:0 3px 0 var(--rim),var(--shd);display:grid;place-items:center;flex:none}
.hero{position:relative;border-radius:var(--leafr);overflow:hidden;background:linear-gradient(180deg,var(--sky1),var(--sky2));box-shadow:var(--shd);animation:grow .8s cubic-bezier(.16,1,.3,1) backwards}
@keyframes grow{from{opacity:0;transform:translateY(8px) scale(.98)}}
.hero .top{padding:12px 12px 0}
.arc{display:block;width:100%;height:auto}
.sun{animation:rise 1.4s cubic-bezier(.16,1,.3,1) backwards}
@keyframes rise{from{transform:translate(-120px,40px)}}
.hero .mid{padding:10px 20px 8px}
.app .pg{-webkit-mask-image:linear-gradient(#000 calc(100% - 130px),transparent calc(100% - 80px));mask-image:linear-gradient(#000 calc(100% - 130px),transparent calc(100% - 80px))}
.app .pg:has(~ .cta){-webkit-mask-image:none;mask-image:none}
.lab{font-size:14px;color:var(--mut);font-weight:600}
.fig{font-family:var(--disp);font-size:58px;font-weight:700;letter-spacing:-.035em;line-height:1.05;margin:2px 0}
.hero .mid p{font-size:14px;font-weight:600}
.low{position:relative;margin-top:2px;padding:28px 20px 18px;color:#fff;background:var(--hill2)}
.low .hl{position:absolute;left:0;top:-1px;width:100%;height:70px;display:block}
.low .lc{position:relative}
.bar{height:16px;border-radius:99px;background:color-mix(in srgb,var(--ink) 12%,transparent);position:relative}
.bar i{display:block;height:100%;border-radius:99px;background:linear-gradient(90deg,#7fb069,#4f9a4a);position:relative;animation:fill 1.2s cubic-bezier(.16,1,.3,1) backwards}
@keyframes fill{from{width:0!important}}
.bar i:not([style*="width:0%"])::after{content:'';position:absolute;right:-3px;top:-4px;width:22px;height:22px;border-radius:50% 50% 50% 14%;background:var(--gold);box-shadow:0 2px 0 rgb(0 0 0 / .15);transform:rotate(-12deg)}
.bar.w i{background:linear-gradient(90deg,#f4b21b,#e8863a)}
.low .bar{background:rgb(255 255 255 / .22);margin-bottom:8px}
.bl{font-weight:700;font-size:13.5px;display:block}
.alert{margin-top:14px;display:flex;align-items:center;gap:10px;padding:11px 16px;border-radius:24px 24px 24px 8px;background:color-mix(in srgb,var(--clay) 18%,var(--sheet));font-weight:600;font-size:13.5px}
.alert .ic{color:var(--clay)}
.sh{display:flex;justify-content:space-between;align-items:center;margin:20px 4px 10px}
.sh.sm{margin:10px 4px 8px}
.sh h2{font-family:var(--disp);font-size:19px;font-weight:600;letter-spacing:-.01em}
.sh a{font-size:13px;font-weight:600;color:var(--moss)}
.strip{display:flex;gap:12px;overflow:hidden;padding:0 0 6px}
.tile{position:relative;flex:none;width:152px;min-height:112px;border-radius:30px 30px 30px 9px;padding:14px 14px 12px;display:flex;flex-direction:column;box-shadow:var(--shd);overflow:hidden}
.tile.flip{border-radius:9px 30px 30px 30px}
.tile.bank{background:#cde7ec;color:#16301f}.tile.savings{background:#d3e8b8;color:#16301f}.tile.card{background:#f5d0bc;color:#3a1f12}
html[data-mode=dark] .tile.bank{background:#1f4048;color:#e4f2f4}html[data-mode=dark] .tile.savings{background:#27472d;color:#e6f3dd}html[data-mode=dark] .tile.card{background:#4c2f23;color:#fbe6da}
.tile .sprig{position:absolute;right:8px;top:8px;opacity:.35}
.tile b{font-family:var(--disp);font-weight:600;font-size:15px;line-height:1.15;max-width:100px}.tile small{font-size:12px;font-weight:600;opacity:.75}
.tile .bal{margin-top:auto;font-family:var(--disp);font-size:24px;font-weight:700;letter-spacing:-.02em}
.sheet{background:var(--sheet);border-radius:var(--leafr);box-shadow:var(--shd);overflow:hidden}
.sheet.f2{border-radius:var(--leafr2)}
.it,.gi{display:flex;align-items:center;gap:12px;padding:12px 16px}
.it+.it,.gi+.gi{border-top:1.5px solid var(--rim)}
.cat{width:42px;height:42px;border-radius:50% 50% 50% 16%;background:color-mix(in srgb,var(--c) 32%,var(--sheet));display:grid;place-items:center;flex:none}
.it .nm{flex:1;min-width:0}
.it .nm b{font-family:var(--disp);font-weight:600;font-size:15.5px;display:flex;align-items:center;gap:5px;line-height:1.2}
.it .nm small{font-size:12px;color:var(--mut);font-weight:500}
.it em{font-style:normal;font-size:11px;font-weight:600;padding:0 7px;border-radius:99px;margin-left:3px;white-space:nowrap}
.it em.e{background:var(--moss-t);color:var(--ink)}.it em.d{background:color-mix(in srgb,var(--gold) 35%,var(--sheet));color:var(--ink)}
.it .a{font-family:var(--disp);font-weight:600;font-size:16px;white-space:nowrap}
.it .a.inc{color:var(--pos);background:var(--moss-t);padding:2px 10px;border-radius:99px}
.gi .gt{flex:1}
.gi .bm{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px}.gi .bm b{font-family:var(--disp);font-weight:600;font-size:15px}.gi .bm span{font-size:12.5px;color:var(--mut)}
.gi .bar{height:12px}
.tabs{position:absolute;left:14px;right:14px;bottom:22px;height:72px;border-radius:32px 32px 32px 12px;background:var(--sheet);box-shadow:0 3px 0 var(--rim),0 20px 34px -14px rgb(25 60 30 / .5);display:flex;align-items:center;justify-content:space-around;padding:0 6px}
.tabs span{display:flex;flex-direction:column;align-items:center;gap:2px;font-size:11px;font-weight:600;color:var(--mut);padding:8px 11px;border-radius:22px}
.tabs span.on{background:var(--moss-t);color:var(--moss)}
html[data-mode=light] .tabs span.on{color:#2a5a2d}
.fab{position:absolute;right:20px;bottom:108px;width:60px;height:60px;border-radius:50% 50% 50% 14%;background:var(--gold);color:#16301f;display:grid;place-items:center;box-shadow:0 4px 0 rgb(150 95 0 / .45),0 16px 24px -10px rgb(190 120 0 / .6);z-index:5}
.pills{display:flex;gap:8px;margin-bottom:10px}
.pills span{padding:7px 15px;border-radius:99px;background:var(--sheet);font-weight:600;font-size:13.5px;box-shadow:0 2px 0 var(--rim)}
.pills span.on{background:var(--moss);color:#fff}
html[data-mode=dark] .pills span.on{color:#0e1a12}
.mo{display:flex;justify-content:space-between;align-items:center;font-family:var(--disp);font-weight:600;font-size:16px;margin-bottom:2px}
.mo .pb{width:34px;height:34px}
.dl{font-size:13px;color:var(--mut);font-weight:600;margin:14px 6px 6px}
.seg3{display:flex;background:var(--sheet);border-radius:99px;padding:4px;margin-bottom:12px;box-shadow:0 3px 0 var(--rim)}
.seg3 span{flex:1;text-align:center;padding:9px 0;border-radius:99px;font-weight:600;font-size:14.5px;color:var(--mut)}
.seg3 span.on{background:var(--moss);color:#fff;box-shadow:0 3px 8px -2px rgb(40 100 45 / .5)}
html[data-mode=dark] .seg3 span.on{color:#0e1a12}
.amtc{background:var(--sheet);border-radius:var(--leafr);padding:10px 20px 8px;box-shadow:var(--shd);margin-bottom:12px}
.amtc small{font-size:13px;color:var(--mut);font-weight:600}
.amtc .fig{font-size:60px;display:flex;justify-content:center;align-items:center}
.caret{width:4px;height:48px;border-radius:2px;background:var(--gold);margin-left:4px;animation:bl 1s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:9px;margin-bottom:12px}
.np span{height:52px;border-radius:26px;background:var(--sheet);box-shadow:0 4px 0 var(--rim);display:grid;place-items:center;font-family:var(--disp);font-size:25px;font-weight:600;transition:transform .12s,box-shadow .12s}
.np span:active{transform:translateY(3px);box-shadow:0 1px 0 var(--rim)}
.sel{position:relative;display:flex;align-items:center;gap:8px;background:var(--sheet);border-radius:22px 22px 22px 8px;padding:12px 16px;box-shadow:0 3px 0 var(--rim)}
.sel small{position:absolute;top:-9px;left:16px;font-size:12px;color:var(--mut);background:var(--bg);padding:0 6px;border-radius:99px;font-weight:600}
.sel b{font-family:var(--disp);font-weight:600;font-size:16px}.sel em{flex:1;font-style:normal;font-size:12.5px;color:var(--mut)}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:8px 6px}
.cg span{display:flex;flex-direction:column;align-items:center;gap:4px;font-size:11.5px;font-weight:600}
.cg b{position:relative;width:48px;height:48px;border-radius:50% 50% 50% 16%;background:color-mix(in srgb,var(--c) 34%,var(--sheet));display:grid;place-items:center}
.cg .on b{box-shadow:0 0 0 3px var(--bg),0 0 0 5.5px var(--moss)}
.cg u{position:absolute;right:-5px;top:-5px;width:19px;height:19px;border-radius:50%;background:var(--moss);color:#fff;display:grid;place-items:center}
html[data-mode=dark] .cg u{color:#0e1a12}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 16px 30px;background:linear-gradient(transparent,var(--bg) 26%)}
.cta button{width:100%;height:56px;border:0;border-radius:28px 28px 28px 10px;background:var(--moss);color:#fff;font-family:var(--disp);font-weight:600;font-size:18px;box-shadow:0 4px 0 color-mix(in srgb,var(--moss) 55%,#000),0 16px 24px -12px rgb(40 100 45 / .6)}
html[data-mode=dark] .cta button{color:#0e1a12}
.bc{background:var(--sheet);border-radius:var(--leafr);padding:14px 18px 14px;margin-bottom:12px;box-shadow:var(--shd)}
.bc.f2{border-radius:var(--leafr2)}
.bt{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:8px}
.bt h3{font-family:var(--disp);font-size:18px;font-weight:600}.bt small{font-size:12px;color:var(--mut)}
.stt{font-size:12px;font-weight:700;padding:3px 11px;border-radius:99px}
.stt.ok{background:var(--moss-t);color:var(--ink)}.stt.warn{background:color-mix(in srgb,var(--gold) 40%,var(--sheet));color:var(--ink)}
.bm{display:flex;justify-content:space-between;align-items:baseline;font-size:14px;font-weight:600;margin-bottom:8px}.bm b{font-family:var(--disp);font-weight:700}
.bc .bl{font-weight:500;font-size:12.5px;color:var(--mut);margin-top:8px}
/* desktop */
.sd{position:absolute;left:16px;top:16px;bottom:16px;width:222px;background:var(--sheet);border-radius:34px 34px 34px 10px;box-shadow:var(--shd);padding:20px 12px;display:flex;flex-direction:column;gap:3px;overflow:hidden}
.logo{font-family:var(--disp);font-size:22px;font-weight:700;display:flex;align-items:center;gap:10px;margin:0 8px 14px}
.mk{width:36px;height:36px;border-radius:50% 50% 50% 14%;background:var(--gold);color:#16301f;display:grid;place-items:center}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:46px;border-radius:23px 23px 23px 8px;background:var(--moss);color:#fff;font-family:var(--disp);font-weight:600;margin-bottom:10px;box-shadow:0 3px 0 color-mix(in srgb,var(--moss) 55%,#000)}
html[data-mode=dark] .addb{color:#0e1a12}
.ni{display:flex;align-items:center;gap:10px;padding:9px 14px;border-radius:99px;font-weight:600;color:var(--mut)}
.ni.on{background:var(--moss-t);color:var(--ink)}
.gl{font-size:12.5px;font-weight:600;color:var(--mut);padding:12px 14px 3px}
.mn{position:absolute;left:254px;right:0;top:0;bottom:0;padding:20px 36px 0 8px;overflow:hidden}
.mn .hd{padding-top:4px}
.hero.wide{display:grid;grid-template-columns:1fr 1fr;grid-template-rows:auto auto}
.hero.wide .mid{grid-column:1;grid-row:1;padding:20px 8px 16px 28px}
.hero.wide .top{grid-column:2;grid-row:1;padding:16px 28px 0 0;align-self:end}
.hero.wide .low{grid-column:1/-1;grid-row:2;padding:32px 28px 18px}
.hero.wide .lc{max-width:480px}
.hero.wide .fig{font-size:76px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 28px}
`;

export const screens = { home, txns, add, budgets, desk };
