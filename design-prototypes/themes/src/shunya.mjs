import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 4,
  slug: 'shunya',
  name: 'Shunya',
  tagline: 'Zero. Everything not carrying a figure is removed — no cards, no shadows, no fills. Just numerals, hairlines and a lot of air, for people who find other finance apps noisy.',
  fonts: 'https://fonts.googleapis.com/css2?family=Geist:wght@200;300;400;500&display=swap',
  swatch: ['#fcfcfa', '#0f0f10', '#2740ff', '#12805a'],
  swatchDark: ['#0a0a0b', '#f3f3ef', '#8f9dff', '#5fd3a0'],
  notes: [
    ['Shape', 'No containers at all. Structure is whitespace and 1px hairlines; the only filled shapes are the add button and the key moment’s dot.'],
    ['Type', 'Geist at weight 200 for the figure (88px) and 300 for keypad digits; 400 for rows. Hierarchy is size and weight, never colour.'],
    ['Density', 'Airy. Roughly half the rows per screen of Khata — long vertical rhythm, one figure per glance.'],
    ['Motion', 'Quiet: the month line draws in once, figures fade up 6px. Everything else is instant.'],
    ['Voice', 'Neutral and brief. Labels are the product’s own words; nothing is celebrated or scolded.'],
    ['Signature', 'The month line: the days of the month as a hairline with today marked, and spend drawn on the same axis — you see pace, not just percentage.'],
  ],
};

const monthLine = (pct, label, status = 'Under pace', warn = false) => {
  let t = '';
  for (let i = 1; i <= 31; i++) t += `<i class="${i === 1 || i === 15 || i === 31 ? 'k' : ''}"></i>`;
  return `<div class="ml" role="img" aria-label="${label}, ${pct}% of budget, ${status}">
  <div class="now" style="left:${D.dayPct}%"><span>Today</span></div>
  <div class="days">${t}</div>
  <div class="spend"><i style="width:${pct}%"></i></div>
  <div class="mlab"><span>${label}</span><span class="${warn ? 'wn' : 'ok'}"><u></u>${status}</span></div>
</div>`;
};
const ticks = () => '';
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n]) => `<span${n === active ? ' class="on"' : ''}>${n}</span>`).join('')}</nav>`;
const fab = `<span class="fab">${ic('plus', 22, 1.5)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;
const ib = (i) => `<span class="ib">${ic(i, 20, 1.25)}</span>`;

const row = (r) => `<div class="rw"><div class="nm"><b>${r.name}${r.rec ? ' ' + ic('repeat', 12, 1.5) : ''}</b><small>${r.cat} · ${r.acct}${r.tag ? ` · <span class="tg${r.tag === 'Essential' ? ' e' : ' d'}"><u></u>${r.tag}</span>` : ''}</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const acc = (a) => `<div class="rw"><div class="nm"><b>${a.name}</b><small>${a.type}</small></div><span class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`;
const due = `<p class="due"><u></u><span>${D.due.name} payment of <b>₹${D.due.amt}</b> is due in ${D.due.days} days</span></p>`;

const hero = `<section class="hero">
  <div class="lab">Safe to spend today</div>
  <div class="fig">₹${D.safe}</div>
  <p class="sub">₹${D.left} left this month · ${D.days} days to go</p>
  ${monthLine(D.pct, `Spent ₹${D.spent} of ₹${D.budget}`)}
</section>`;

const home = `
<div class="pg">
  <header class="hd"><div class="ti"><h1 class="nm0">${D.user}</h1><p>${ic('shield', 12, 1.5)} Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></header>
  ${hero}
  ${due}
  <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
  ${D.accounts.map(acc).join('')}
  <div class="tot"><span>Total balance</span><b>₹${D.total}</b></div>
  <div class="sh"><h2>Latest</h2><a>See all</a></div>
  ${row(D.groups[0].rows[1])}
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search') + ib('eye'))}
  <div class="ty"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span>${ic('chevl', 16, 1.5)}</span><b>October 2026</b><span>${ic('chevr', 16, 1.5)}</span></div>
  ${D.groups.map((g) => `<div class="dl">${g.date}</div>${g.rows.map(row).join('')}`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  ${hd('Add transaction', ib('back'), '')}
  <div class="ty"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtb"><small>Amount</small><div class="fig"><sup>₹</sup>2,450<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 24, 1.25)].map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fl"><div><small>Account</small><b>HDFC Checking</b></div><span>Bank account</span>${ic('chevd', 16, 1.5)}</div>
  <div class="sh"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 22, 1.25)}${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back'), ib('eye') + ib('plus'))}
  ${D.budgets
    .map(
      (b, k) => `<section class="bu">
    <div class="bh"><h3>${b.name}</h3><span class="pc">${b.pct}%</span></div>
    <small class="per">Monthly · October 2026</small>
    ${monthLine(b.pct, `₹${b.spent} of ₹${b.limit}`, b.state, b.pct > 85).replace('<div class="days">', '<div class="days" hidden>')}
    <div class="bf">₹${b.left} left this month</div>
  </section>`,
    )
    .join('')}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span class="z"></span>Finio</div>
  <span class="addb">${ic('plus', 16, 1.75)} Add transaction</span>
  ${D.nav.map(([n]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="hd"><div class="ti"><h1 class="nm0">${D.user}</h1><p>${ic('shield', 13, 1.5)} Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></header>
  <section class="hero w">
    <div><div class="lab">Safe to spend today</div><div class="fig">₹${D.safe}</div><p class="sub">₹${D.left} left this month · ${D.days} days to go</p></div>
    <div class="mlw">${monthLine(D.pct, `Spent ₹${D.spent} of ₹${D.budget}`)}</div>
  </section>
  ${due}
  <div class="cols">
    <div>
      <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
      ${D.accounts.map(acc).join('')}
      <div class="tot"><span>Total balance</span><b>₹${D.total}</b></div>
      <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
      ${D.goals.map((g) => `<div class="rw g"><div class="nm"><b>${g.name}</b><small>₹${g.have} / ₹${g.target}</small></div><div class="gb"><i style="width:${g.pct}%"></i></div></div>`).join('')}
    </div>
    <div>
      <div class="sh"><h2>Latest</h2><a>See all</a></div>
      ${D.groups.flatMap((g) => g.rows).map(row).join('')}
    </div>
  </div>
</main>`;

export const css = `
.app{--bg:#fcfcfa;--ink:#0f0f10;--mut:#6d6d68;--line:#e4e4de;--acc:#2740ff;--pos:#12805a;--neg:#c7361f;--warn:#9a6200;
  background:var(--bg);color:var(--ink);font-family:'Geist',system-ui,sans-serif;font-weight:400;font-size:14px;font-variant-numeric:tabular-nums;letter-spacing:-.005em}
html[data-mode=dark] .app{--bg:#0a0a0b;--ink:#f3f3ef;--mut:#9a9a94;--line:#232325;--acc:#8f9dff;--pos:#5fd3a0;--neg:#ff8a76;--warn:#f0bd5a}
.app ::selection{background:var(--acc);color:var(--bg)}
.app .pg{position:absolute;inset:44px 0 74px;padding:8px 24px;overflow:hidden}
.hd{display:flex;align-items:center;gap:8px;padding:10px 0 18px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-size:28px;font-weight:300;letter-spacing:-.03em;line-height:1.15}
.hd h1.nm0{font-size:17px;font-weight:500;letter-spacing:-.01em}
.hd p{font-size:13px;color:var(--mut);display:flex;gap:5px;align-items:center;margin-top:2px}
.hd .rt{display:flex;gap:2px}
.ib{width:40px;height:40px;display:grid;place-items:center;color:var(--ink)}
.hero{padding:20px 0 10px;animation:up .7s cubic-bezier(.16,1,.3,1) backwards}
@keyframes up{from{opacity:0;transform:translateY(6px)}}
.lab{font-size:14px;color:var(--mut)}
.fig{font-size:88px;font-weight:200;letter-spacing:-.05em;line-height:1.02;margin:4px 0 6px;margin-left:-4px}
.sub{font-size:15px;color:var(--ink);margin-bottom:26px}
.ml{position:relative;padding-top:20px}
.now{position:absolute;top:0;bottom:30px;width:1px;background:var(--acc);z-index:2}
.now span{position:absolute;top:0;left:6px;font-size:12px;color:var(--acc);line-height:1}
.now::after{content:'';position:absolute;bottom:-3px;left:-3px;width:7px;height:7px;border-radius:50%;background:var(--acc)}
.days{display:flex;justify-content:space-between;height:8px;align-items:flex-end}
.days i{width:1px;height:4px;background:var(--mut);opacity:.6}.days i.k{height:8px;opacity:1}
.spend{height:1px;background:var(--line);margin:8px 0 0;position:relative}
.spend i{position:absolute;left:0;top:-1px;height:3px;background:var(--ink);transform-origin:left;animation:draw 1.1s cubic-bezier(.16,1,.3,1) backwards}
@keyframes draw{from{transform:scaleX(0)}}
.mlab{display:flex;justify-content:space-between;font-size:13px;margin-top:12px;color:var(--mut)}
.mlab span:first-child{color:var(--ink)}
.mlab u{display:inline-block;width:6px;height:6px;border-radius:50%;margin-right:6px;vertical-align:1px;background:var(--pos)}
.mlab .wn{color:var(--warn)}.mlab .wn u{background:var(--warn)}
.due{margin:20px 0 0;font-size:14px;display:flex;align-items:center;gap:10px}
.due u{width:6px;height:6px;border-radius:50%;background:var(--neg);flex:none}
.sh{display:flex;justify-content:space-between;align-items:baseline;margin:26px 0 4px}
.sh h2{font-size:13px;font-weight:400;color:var(--mut)}
.sh a{font-size:13px;color:var(--acc)}
.rw{display:flex;justify-content:space-between;align-items:center;gap:14px;padding:11px 0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:400;font-size:17px;letter-spacing:-.01em;line-height:1.25}
.rw small{display:block;font-size:13px;color:var(--mut);margin-top:1px}
.rw .a{font-size:17px;font-weight:400;white-space:nowrap}
.a.inc{color:var(--pos)}.a.neg{color:var(--neg)}
.tg{white-space:nowrap}.tg u{display:inline-block;width:6px;height:6px;border-radius:50%;margin-right:4px;vertical-align:1px}
.tg.e u{background:var(--pos)}.tg.d u{background:var(--warn)}
.tot{display:flex;justify-content:space-between;align-items:baseline;border-top:1px solid var(--line);margin-top:8px;padding-top:12px;color:var(--mut)}
.tot b{font-size:19px;font-weight:400;color:var(--ink)}
.tabs{position:absolute;left:0;right:0;bottom:0;height:74px;border-top:1px solid var(--line);background:var(--bg);display:flex;justify-content:space-around;align-items:flex-start;padding:16px 4px 0}
.tabs span{font-size:13px;color:var(--mut);position:relative;padding-bottom:10px}
.tabs span.on{color:var(--ink);font-weight:500}
.tabs span.on::after{content:'';position:absolute;left:50%;bottom:0;width:4px;height:4px;margin-left:-2px;border-radius:50%;background:var(--acc)}
.fab{position:absolute;right:24px;bottom:96px;width:54px;height:54px;border-radius:50%;background:var(--ink);color:var(--bg);display:grid;place-items:center;z-index:5}
.ty{display:flex;gap:22px;margin-bottom:6px}
.ty span{font-size:15px;color:var(--mut);padding:8px 0;border-bottom:1.5px solid transparent}
.ty span.on{color:var(--ink);border-color:var(--ink)}
.mo{display:flex;justify-content:space-between;align-items:center;padding:14px 0 4px;font-size:15px}
.mo span{color:var(--mut)}
.dl{font-size:13px;color:var(--mut);margin:26px 0 4px}
.amtb{text-align:center;padding:12px 0 6px}.amtb small{font-size:13px;color:var(--mut)}
.amtb .fig{margin:2px 0 0;font-size:84px;display:flex;justify-content:center;align-items:flex-start}
.amtb sup{font-size:28px;font-weight:300;color:var(--mut);margin:10px 6px 0 0;letter-spacing:0}
.caret{width:1.5px;height:68px;background:var(--acc);margin:6px 0 0 6px;animation:bl 1s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);margin:6px 0 10px}
.np span{height:54px;display:grid;place-items:center;font-size:30px;font-weight:300;color:var(--ink)}
.np span:active{color:var(--acc)}
.fl{display:flex;align-items:center;gap:10px;border-top:1px solid var(--line);border-bottom:1px solid var(--line);padding:9px 0}
.fl div{flex:1}.fl small{display:block;font-size:12.5px;color:var(--mut)}.fl b{font-weight:400;font-size:17px}.fl span{font-size:13px;color:var(--mut)}
.add .sh,.pg .sh{margin-top:24px}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:14px 4px;padding-top:4px}
.cg span{display:flex;flex-direction:column;align-items:center;gap:6px;font-size:12px;color:var(--mut)}
.cg span.on{color:var(--ink);font-weight:500}.cg span.on .ic{color:var(--acc)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:10px 24px 32px;background:var(--bg);border-top:1px solid var(--line)}
.cta button{width:100%;height:54px;border:0;border-radius:99px;background:var(--ink);color:var(--bg);font-size:16px;font-weight:500}
.bu{padding:20px 0 6px}
.bh{display:flex;justify-content:space-between;align-items:baseline}
.bh h3{font-size:19px;font-weight:400;letter-spacing:-.02em}.pc{font-size:30px;font-weight:200;letter-spacing:-.04em}
.per{display:block;font-size:13px;color:var(--mut);margin-bottom:-6px}
.bu .ml{padding-top:8px}.bu .now{bottom:28px}.bu .now span{display:none}.bu .now{top:6px}
.bu .spend{margin-top:14px}
.bf{font-size:13px;color:var(--mut);margin-top:4px}
/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:216px;border-right:1px solid var(--line);padding:30px 26px;display:flex;flex-direction:column;gap:2px}
.logo{font-size:19px;font-weight:500;letter-spacing:-.02em;display:flex;align-items:center;gap:10px;margin-bottom:30px}
.z{width:18px;height:18px;border-radius:50%;border:2px solid var(--ink)}
.addb{display:flex;align-items:center;gap:8px;color:var(--acc);font-weight:500;margin-bottom:24px}
.ni{padding:7px 0;color:var(--mut);font-size:14.5px}.ni.on{color:var(--ink);font-weight:500}
.gl{font-size:12.5px;color:var(--mut);margin:26px 0 6px;opacity:.8}
.mn{position:absolute;left:216px;right:0;top:0;bottom:0;padding:26px 64px;overflow:hidden}
.mn .hd{padding-top:0}
.hero.w{display:grid;grid-template-columns:auto 1fr;gap:0 80px;align-items:end;padding-top:12px}
.hero.w .fig{font-size:96px}.hero.w .sub{margin-bottom:6px}
.hero.w .ml{padding-bottom:6px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 80px}
.rw.g .gb{width:120px;height:1px;background:var(--line);position:relative}.rw.g .gb i{position:absolute;left:0;top:-1px;height:3px;background:var(--ink)}
`;

export const screens = { home, txns, add, budgets, desk };
