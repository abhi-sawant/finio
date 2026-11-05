import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 8,
  slug: 'ganak',
  name: 'Ganak',
  tagline: 'The one who counts. The desk calculator every shopkeeper and parent kept by the till — putty plastic, rubber keys, a liquid-crystal window — for people who trust a figure more when it looks like it was punched in.',
  blurb: 'The pocket calculator. Putty plastic, rubber keys that really press, and an LCD window with authored seven-segment digits; the black model glows like a VFD.',
  fonts: 'https://fonts.googleapis.com/css2?family=Archivo:wdth,wght@100..125,400..800&display=swap',
  swatch: ['#e7e1d3', '#24211b', '#d4541d', '#b9c3a3'],
  swatchDark: ['#16171a', '#ebe7dc', '#ff7a3d', '#5ef2c4'],
  notes: [
    ['Shape', 'Moulded plastic. Panels are recessed bezels, never flat cards; buttons are rounded keys with a top highlight and a bottom lip that collapses when pressed.'],
    ['Type', 'The hero and the amount are seven-segment digits drawn as SVG, slanted, with unlit ghost segments behind. Everything else is Archivo with tabular numerals; key legends bold and wide.'],
    ['Density', 'Medium. Content is grouped into key zones — display, memory, keypad — the way a calculator groups its buttons.'],
    ['Motion', 'Power-on test: the LCD flashes 88,88,888 for a beat, then settles to the figure. Keys sink on press; the budget at its limit blinks its last segment.'],
    ['Voice', 'Terse legends only where they are true: M for the budget held in memory, GT beside the grand total. Every product label stays as written.'],
    ['Signature', 'The display window — LCD by day, glowing VFD on the black model — with status annunciators along its top edge and budgets drawn as bar-graph segments.'],
  ],
};

/* ---------- seven-segment digits (authored SVG) ---------- */
const W = 48, H = 88, T = 10, G = 1.6, ADV = 64, SL = 12;
const hz = (x0, x1, y) => `${x0},${y} ${x0 + T / 2},${y - T / 2} ${x1 - T / 2},${y - T / 2} ${x1},${y} ${x1 - T / 2},${y + T / 2} ${x0 + T / 2},${y + T / 2}`;
const vt = (x, y0, y1) => `${x},${y0} ${x + T / 2},${y0 + T / 2} ${x + T / 2},${y1 - T / 2} ${x},${y1} ${x - T / 2},${y1 - T / 2} ${x - T / 2},${y0 + T / 2}`;
const L = T / 2, R = W - T / 2, M = H / 2;
const SEG = {
  a: hz(L + G, R - G, T / 2),
  b: vt(R, T / 2 + G, M - G),
  c: vt(R, M + G, H - T / 2 - G),
  d: hz(L + G, R - G, H - T / 2),
  e: vt(L, M + G, H - T / 2 - G),
  f: vt(L, T / 2 + G, M - G),
  g: hz(L + G, R - G, M),
};
const MAP = ['abcdef', 'bc', 'abdeg', 'abcdg', 'bcfg', 'acdfg', 'acdefg', 'abc', 'abcdefg', 'abcdfg'];

// segDigits('1,562') → inline SVG: `cells` digit positions, each with its own point/comma slot,
// unlit segments kept as faint ghosts (the 8888 behind every LCD figure).
export function segDigits(str, { h = 60, cells = 6 } = {}) {
  const ds = [];
  for (const ch of str) {
    if (/\d/.test(ch)) ds.push({ d: +ch, sep: '' });
    else if (ds.length) ds[ds.length - 1].sep = ch;
  }
  while (ds.length < cells) ds.unshift({ d: null, sep: '' });
  const vbW = SL + cells * ADV, vbH = H + 10;
  let g = '';
  ds.forEach((c, i) => {
    const on = c.d === null ? '' : MAP[c.d];
    let s = '';
    for (const k of 'abcdefg') s += `<polygon class="${on.includes(k) ? 's1' : 's0'}" points="${SEG[k]}"/>`;
    const px = W + 6;
    const dot = `<polygon class="${c.sep ? 's1' : 's0'}" points="${px},${H - 8} ${px + 8},${H - 8} ${px + 8},${H} ${px},${H}"/>`;
    const tail = c.sep === ',' ? `<polygon class="s1" points="${px + 5},${H} ${px + 8},${H} ${px + 3},${H + 9} ${px},${H + 9}"/>` : '';
    g += `<g transform="translate(${SL + i * ADV} 0) skewX(-7)">${s}${dot}${tail}</g>`;
  });
  const wpx = Math.round((vbW * h) / H), hpx = Math.round((vbH * h) / H);
  return `<svg class="sg" width="${wpx}" height="${hpx}" viewBox="0 0 ${vbW} ${vbH}" role="img" aria-label="${str}">${g}</svg>`;
}

/* ---------- small pieces ---------- */
const cardIc = (s = 18, sw = 1.75) =>
  `<svg class="ic" width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M2.5 10h19M6 15h4"/></svg>`;
const acctIc = { bank: (s) => ic('banknote', s, 1.75), savings: (s) => ic('wallet', s, 1.75), card: (s) => cardIc(s) };

const solar = `<span class="solar" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></span>`;
const key = (i, label) => `<span class="key ib" role="button" aria-label="${label}">${ic(i, 18, 2)}</span>`;

// LCD bar graph: n discrete blocks, lit count = %; at danger the last lit block blinks.
const bars = (pct, { n = 20, day = null, danger = false, label = '' } = {}) => {
  const lit = Math.round((pct / 100) * n);
  let b = '';
  for (let i = 0; i < n; i++) b += `<i class="${i < lit ? (danger && i === lit - 1 ? 'on bk' : 'on') : ''}"></i>`;
  const mk = day === null ? '' : `<u class="dm" style="left:calc(${day}% - 4px)"></u>`;
  return `<div class="lb" role="img" aria-label="${label}">${mk}${b}</div>`;
};

const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => `<span class="tk${n === active ? ' on' : ''}"><em></em>${ic(i, 19, 2)}<b>${n}</b></span>`).join('')}</nav>`;
const fab = `<span class="fab" role="button" aria-label="Add transaction">${ic('plus', 26, 2.6)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;

const tag = (t) => (t ? ` · <span class="tg${t === 'Essential' ? ' e' : ' d'}">${t}</span>` : '');
const row = (r) => `<div class="rw"><span class="ci">${ic(r.icon, 17, 1.9)}</span><div class="nm"><b>${r.name}${r.rec ? ' <span class="rc" aria-label="Recurring">' + ic('repeat', 12, 2.2) + '</span>' : ''}</b><small>${r.cat} · ${r.acct}${tag(r.tag)}</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const acc = (a) => `<div class="rw"><span class="ci">${acctIc[a.kind](17)}</span><div class="nm"><b>${a.name}</b><small>${a.type}</small></div><span class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`;
const total = `<div class="tot"><span>Total balance <em class="lg">GT</em></span><b>₹${D.total}</b></div>`;
const due = `<div class="due"><span class="led"></span><span class="dt">${D.due.name} payment of <b>₹${D.due.amt}</b> is due in ${D.due.days} days</span>${ic('chevr', 16, 2)}</div>`;
const sh = (t, a = 'See all') => `<div class="sh"><h2>${t}</h2>${a ? `<a>${a}</a>` : ''}</div>`;

const ann = `<div class="ann"><b>M</b><b>MONTH</b><b>DAY 15/31</b><b>${D.pct}%</b><span>E</span></div>`;
const hero = (wide = false) => `<section class="hero${wide ? ' w' : ''}">
  <div class="ht"><span class="lab">Safe to spend today</span>${solar}</div>
  <div class="hb">
    <div class="lcd">
      ${ann}
      <div class="fig"><span class="rs">₹</span>${segDigits(D.safe, { h: wide ? 70 : 58 })}</div>
      ${bars(D.pct, { day: D.dayPct, label: `Spent ₹${D.spent} of ₹${D.budget}, ${D.pct}%, day 15 of 31` })}
    </div>
    <div class="hx">
      <p class="sub">₹${D.left} left this month · ${D.days} days to go</p>
      <p class="bl"><span>Spent ₹${D.spent} of ₹${D.budget}</span><b>${D.pct}%</b></p>
    </div>
  </div>
</section>`;

const home = `
<div class="pg">
  <header class="hd"><div class="ti"><h1>${D.user}</h1><p>${ic('shield', 12, 2)} Kept only on this device</p></div><div class="rt">${key('eye', 'Hide amounts')}${key('sliders', 'Settings')}</div></header>
  ${hero()}
  ${due}
  ${sh('Where it sits')}
  <div class="pnl">${D.accounts.map(acc).join('')}${total}</div>
  ${sh('Latest')}
  <div class="pnl">${row(D.groups[0].rows[1])}</div>
</div>
${fab}${tabs('Home')}`;

const seg4 = (items, on = 0) => `<div class="ty">${items.map((t, k) => `<span class="${k === on ? 'on' : ''}">${t}</span>`).join('')}</div>`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', key('search', 'Search') + key('eye', 'Hide amounts'))}
  ${seg4(['All', 'Expense', 'Income', 'Transfer'])}
  <div class="mo"><span class="key sm">${ic('chevl', 16, 2.2)}</span><b>October 2026</b><span class="key sm">${ic('chevr', 16, 2.2)}</span></div>
  ${D.groups.map((g) => `<div class="dl">${g.date}</div><div class="pnl">${g.rows.map(row).join('')}</div>`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg ad">
  ${hd('Add transaction', key('back', 'Back'), '')}
  ${seg4(['Expense', 'Income', 'Transfer'])}
  <div class="lcd amt">
    <div class="ann"><b>EXPENSE</b><span>M</span><span>GT</span><span>E</span></div>
    <div class="fig"><span class="rs">₹</span>${segDigits('2,450', { h: 52 })}<i class="caret"></i></div>
  </div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0'].map((k) => `<span class="key">${k}</span>`).join('')}<span class="key fn" aria-label="Delete">${ic('backspace', 22, 2)}</span></div>
  <div class="fl"><span class="ci">${ic('banknote', 17, 1.9)}</span><div><small>Account</small><b>HDFC Checking</b></div><span class="ft">Bank account</span>${ic('chevd', 16, 2)}</div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([n, i], k) => `<span class="key${k === 0 ? ' on' : ''}">${k === 0 ? '<em></em>' : ''}${ic(i, 19, 1.9)}<b>${n}</b></span>`).join('')}</div>
</div>
<div class="cta"><button class="eq">Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', key('back', 'Back'), key('eye', 'Hide amounts') + key('plus', 'Add budget'))}
  ${D.budgets
    .map((b) => {
      const warn = b.pct > 85;
      return `<section class="bu pnl">
    <div class="bh"><div><h3>${b.name}</h3><small class="per">Monthly · October 2026</small></div><span class="pc${warn ? ' wn' : ''}">${b.pct}%</span></div>
    <div class="lcd mini"><span class="st${warn ? ' wn' : ''}">${b.state}</span>${bars(b.pct, { danger: warn, label: `${b.name}: ${b.pct}% used, ${b.state}` })}</div>
    <div class="bf"><span>₹${b.spent} of ₹${b.limit}</span><span>₹${b.left} left this month</span></div>
  </section>`;
    })
    .join('')}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span>Finio</span>${solar}</div>
  <span class="addb key">${ic('plus', 16, 2.4)} Add transaction</span>
  <div class="nv">${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}"><em></em>${ic(i, 17, 2)}${n}</span>`).join('')}</div>
  <div class="gl">Tools</div>
  <div class="nv t">${D.tools.map((t) => `<span class="ni"><em></em>${t}</span>`).join('')}</div>
</aside>
<main class="mn">
  <header class="hd"><div class="ti"><h1>${D.user}</h1><p>${ic('shield', 13, 2)} Kept only on this device</p></div><div class="rt">${key('eye', 'Hide amounts')}${key('sliders', 'Settings')}</div></header>
  ${hero(true)}
  ${due}
  <div class="cols">
    <div>
      ${sh('Where it sits')}
      <div class="pnl">${D.accounts.map(acc).join('')}${total}</div>
      ${sh('Savings goals')}
      <div class="pnl">${D.goals.map((g) => `<div class="rw g"><span class="ci">${ic('target', 17, 1.9)}</span><div class="nm"><b>${g.name}</b><small>₹${g.have} / ₹${g.target}</small></div><div class="lcd micro">${bars(g.pct, { n: 10, label: `${g.pct}% saved` })}</div></div>`).join('')}</div>
    </div>
    <div>
      ${sh('Latest')}
      <div class="pnl">${D.groups.flatMap((g) => g.rows).map(row).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{
  --body:#e7e1d3;--body2:#ddd6c5;--well:#dcd5c4;--ink:#24211b;--mut:#5f5747;--edge:#fbf8f1;--sh:rgba(74,62,38,.28);--line:rgba(74,62,38,.16);
  --kt:#f6f2e8;--kb:#e2dccd;--klip:#b3aa95;--kink:#24211b;
  --fn1:#5a554b;--fn2:#45413a;--fnlip:#25231f;--fnink:#f3efe6;
  --o1:#e8622a;--o2:#c9480f;--olip:#8f300a;--oink:#fff;--acct:#a83c0c;
  --lcd:#b9c3a3;--lcd2:#aab593;--seg:#1f2a1c;--ghost:rgba(31,42,28,.075);--lmut:#3b4a35;
  --pos:#2f6b2a;--neg:#b3261e;--warn:#8a5a00;--led:#e8622a;--ledoff:#c3bba9;
  --glow:none;
  background:var(--body) linear-gradient(180deg,rgba(255,255,255,.35),rgba(255,255,255,0) 40%);color:var(--ink);
  font-family:'Archivo',system-ui,sans-serif;font-size:14px;font-variant-numeric:tabular-nums;caret-color:var(--o1);
  & ::selection{background:var(--o1);color:#fff}
  & :focus-visible{outline:2px solid var(--o1);outline-offset:2px}
  & .pg{position:absolute;inset:44px 0 84px;padding:4px 16px;overflow:hidden}
  & .hd{display:flex;align-items:center;gap:10px;padding:8px 2px 12px}
  & .hd .ti{flex:1;min-width:0}
  & .hd h1{font-size:24px;font-weight:700;font-stretch:112%;letter-spacing:-.01em;line-height:1.1}
  & .hd p{font-size:12.5px;color:var(--mut);display:flex;gap:5px;align-items:center;margin-top:3px;font-weight:500}
  & .hd .rt{display:flex;gap:8px}
  /* keys */
  & .key{display:grid;place-items:center;border-radius:10px;color:var(--kink);background:linear-gradient(180deg,var(--kt),var(--kb));
    box-shadow:inset 0 1px 0 rgba(255,255,255,.75),inset 0 -2px 0 rgba(0,0,0,.05),0 3px 0 var(--klip),0 4px 7px var(--sh);
    transition:transform .08s ease-out,box-shadow .08s ease-out;cursor:pointer;user-select:none}
  & .key:active{transform:translateY(2px);box-shadow:inset 0 1px 0 rgba(255,255,255,.6),0 1px 0 var(--klip),0 1px 2px var(--sh)}
  & .ib{width:38px;height:36px}
  & .key.sm{width:34px;height:30px;border-radius:8px}
  /* recessed panels */
  & .pnl,& .hero,& .due,& .fl{background:var(--well);border-radius:14px;box-shadow:inset 0 2px 3px var(--sh),inset 0 -1px 0 var(--edge),0 1px 0 var(--edge)}
  & .pnl{padding:2px 12px}
  /* hero */
  & .hero{padding:10px 12px 12px;animation:in .5s cubic-bezier(.16,1,.3,1) backwards}
  & .ht{display:flex;justify-content:space-between;align-items:center;padding:0 2px 8px}
  & .lab{font-size:13.5px;font-weight:600;color:var(--ink)}
  & .solar{display:flex;gap:2px;padding:2px;border-radius:4px;background:#2b2722;box-shadow:inset 0 1px 2px rgba(0,0,0,.6),0 1px 0 var(--edge)}
  & .solar i{width:13px;height:11px;border-radius:1px;background:linear-gradient(180deg,#4a3f3a,#2e2724 50%,#3c332f);box-shadow:inset 0 -1px 0 rgba(255,255,255,.07)}
  & .lcd{position:relative;background:linear-gradient(180deg,var(--lcd2),var(--lcd) 22%);border-radius:8px;color:var(--seg);padding:6px 12px 10px;
    box-shadow:inset 0 2px 4px rgba(20,30,15,.35),inset 0 0 0 1px rgba(20,30,15,.18),0 0 0 3px #3a362e,0 0 0 4px var(--edge)}
  & .ann{display:flex;gap:12px;font-size:9px;font-weight:700;font-stretch:112%;letter-spacing:.08em;line-height:1;color:var(--seg)}
  & .ann span{color:var(--ghost)}
  & .ann b:last-of-type{margin-left:auto}
  & .fig{display:flex;align-items:flex-start;justify-content:flex-end;margin:6px 0 4px}
  & .rs{font-size:22px;font-weight:600;margin:4px 6px 0 0;color:var(--seg);text-shadow:var(--glow)}
  & .sg{display:block;overflow:visible;filter:var(--segf,none)}
  & .sg .s1{fill:var(--seg);animation:p1 .9s steps(1,end) backwards}
  & .sg .s0{fill:var(--ghost);animation:p0 .9s steps(1,end) backwards}
  & .lb{position:relative;display:flex;gap:2.5px;padding-top:7px}
  & .lb i{flex:1;height:9px;border-radius:1px;background:var(--ghost);transform:skewX(-7deg);animation:p0b .9s steps(1,end) backwards}
  & .lb i.on{background:var(--seg);animation:p1 .9s steps(1,end) backwards}
  & .lb i.bk{background:var(--neg);animation:bk 1.1s steps(1,end) infinite}
  & .lb .dm{position:absolute;top:0;width:0;height:0;border-left:4px solid transparent;border-right:4px solid transparent;border-top:5px solid var(--seg)}
  & .hx{padding:10px 2px 0}
  & .sub{font-size:14.5px;font-weight:600}
  & .bl{display:flex;justify-content:space-between;font-size:13px;color:var(--mut);margin-top:3px;font-weight:500}
  & .bl b{color:var(--ink)}
  /* due */
  & .due{display:flex;align-items:center;gap:10px;margin-top:10px;padding:11px 12px;font-size:13.5px;color:var(--ink)}
  & .due .dt{flex:1}& .due .ic{color:var(--mut)}
  & .led{width:9px;height:9px;border-radius:50%;flex:none;background:var(--neg);box-shadow:inset 0 -1px 1px rgba(0,0,0,.3),0 0 0 2px var(--body2)}
  /* rows */
  & .sh{display:flex;justify-content:space-between;align-items:baseline;margin:14px 4px 6px}
  & .sh h2{font-size:14px;font-weight:700;font-stretch:112%}
  & .sh a{font-size:13px;font-weight:600;color:var(--acct)}
  & .rw{display:flex;align-items:center;gap:11px;padding:9px 0}
  & .rw+.rw{border-top:1px solid var(--line)}
  & .ci{width:32px;height:32px;flex:none;border-radius:8px;display:grid;place-items:center;color:var(--ink);background:linear-gradient(180deg,var(--kt),var(--kb));box-shadow:inset 0 1px 0 rgba(255,255,255,.7),0 1.5px 0 var(--klip)}
  & .rw .nm{flex:1;min-width:0}
  & .rw .nm b{display:flex;align-items:center;gap:5px;font-weight:600;font-size:15px;line-height:1.25}
  & .rw small{display:block;font-size:12.5px;color:var(--mut);margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
  & .rc{color:var(--mut);display:inline-flex}
  & .rw .a{font-size:15px;font-weight:700;white-space:nowrap}
  & .a.inc{color:var(--pos)}& .a.neg{color:var(--neg)}
  & .tg{font-weight:600}& .tg.e{color:var(--pos)}& .tg.d{color:var(--warn)}
  & .tot{display:flex;justify-content:space-between;align-items:center;border-top:1.5px solid var(--line);padding:10px 0 9px;font-weight:600}
  & .tot b{font-size:18px;font-weight:800}
  & .lg{font-style:normal;font-size:9.5px;font-weight:800;font-stretch:125%;letter-spacing:.06em;padding:2px 5px;border-radius:4px;margin-left:5px;vertical-align:1px;color:var(--fnink);background:var(--fn2)}
  /* tab bar: rubber function keys */
  & .tabs{position:absolute;left:0;right:0;bottom:0;height:84px;padding:9px 10px 24px;display:flex;gap:7px;background:var(--body2);box-shadow:inset 0 1px 0 var(--edge),0 -1px 0 var(--line);z-index:4}
  & .tk{position:relative;flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;border-radius:9px;color:var(--mut);
    background:linear-gradient(180deg,var(--kt),var(--kb));box-shadow:inset 0 1px 0 rgba(255,255,255,.7),0 2.5px 0 var(--klip),0 3px 5px var(--sh)}
  & .tk b{font-size:9.5px;font-weight:700;font-stretch:108%;white-space:nowrap}
  & .tk em{position:absolute;top:5px;right:6px;width:5px;height:5px;border-radius:50%;background:var(--ledoff)}
  & .tk.on{color:var(--ink);transform:translateY(1.5px);box-shadow:inset 0 1px 0 rgba(255,255,255,.5),0 1px 0 var(--klip)}
  & .tk.on em{background:var(--led);box-shadow:var(--ledglow,none)}
  & .fab{position:absolute;right:18px;bottom:100px;width:58px;height:58px;border-radius:50%;display:grid;place-items:center;color:var(--oink);z-index:5;
    background:radial-gradient(circle at 50% 30%,var(--o1),var(--o2));box-shadow:inset 0 2px 0 rgba(255,255,255,.3),0 4px 0 var(--olip),0 7px 12px var(--sh)}
  /* type filter: a row of latched keys */
  & .ty{display:flex;gap:6px;padding:5px;border-radius:12px;background:var(--well);box-shadow:inset 0 2px 3px var(--sh),0 1px 0 var(--edge)}
  & .ty span{flex:1;text-align:center;font-size:13px;font-weight:700;padding:7px 0;border-radius:8px;color:var(--mut)}
  & .ty span.on{color:var(--kink);background:linear-gradient(180deg,var(--kt),var(--kb));box-shadow:inset 0 1px 0 rgba(255,255,255,.7),0 2px 0 var(--klip)}
  & .mo{display:flex;justify-content:space-between;align-items:center;padding:10px 2px 0;font-size:15px}
  & .mo b{font-weight:700;font-stretch:112%}
  & .dl{font-size:12.5px;font-weight:600;color:var(--mut);margin:11px 4px 5px}
  /* add */
  & .ad .ty{margin-bottom:10px}
  & .lcd.amt{padding:6px 14px 8px}
  & .lcd.amt .fig{margin:4px 0 0;align-items:center}
  & .lcd.amt .rs{align-self:flex-start}
  & .caret{width:3px;height:46px;margin:0 0 6px 4px;background:var(--seg);animation:bl 1s steps(1) infinite}
  & .np{display:grid;grid-template-columns:repeat(3,1fr);gap:9px 10px;margin:16px 4px 12px}
  & .np .key{height:44px;font-size:22px;font-weight:700;font-stretch:112%}
  & .np .fn{background:linear-gradient(180deg,var(--fn1),var(--fn2));color:var(--fnink);box-shadow:inset 0 1px 0 rgba(255,255,255,.18),0 3px 0 var(--fnlip),0 4px 7px var(--sh)}
  & .np .fn:active{box-shadow:0 1px 0 var(--fnlip)}
  & .fl{display:flex;align-items:center;gap:10px;padding:8px 12px}
  & .fl div{flex:1}& .fl small{display:block;font-size:11.5px;color:var(--mut);font-weight:500}& .fl b{font-weight:700;font-size:15px}
  & .fl .ft{font-size:12.5px;color:var(--mut)}
  & .cg{display:grid;grid-template-columns:repeat(4,1fr);gap:10px 9px;margin:0 2px}
  & .cg .key{position:relative;height:52px;display:flex;flex-direction:column;justify-content:center;gap:3px;border-radius:10px}
  & .cg b{font-size:10.5px;font-weight:700;white-space:nowrap}
  & .cg .key.on{transform:translateY(2px);background:linear-gradient(180deg,var(--kb),var(--kt));box-shadow:inset 0 2px 3px var(--sh),0 1px 0 var(--klip);color:var(--acct)}
  & .cg em{position:absolute;top:5px;right:6px;width:5px;height:5px;border-radius:50%;background:var(--led);box-shadow:var(--ledglow,none)}
  & .cta{position:absolute;left:0;right:0;bottom:0;padding:10px 16px 26px;background:var(--body2);box-shadow:inset 0 1px 0 var(--edge),0 -1px 0 var(--line);z-index:4}
  & .eq{width:100%;height:52px;border:0;border-radius:12px;font-size:18.5px;font-weight:800;font-stretch:112%;letter-spacing:.01em;color:var(--oink);
    background:linear-gradient(180deg,var(--o1),var(--o2));box-shadow:inset 0 1.5px 0 rgba(255,255,255,.3),0 4px 0 var(--olip),0 6px 10px var(--sh);transition:transform .08s ease-out,box-shadow .08s ease-out}
  & .eq:active{transform:translateY(3px);box-shadow:inset 0 1.5px 0 rgba(255,255,255,.25),0 1px 0 var(--olip)}
  /* budgets */
  & .bu{padding:11px 12px 10px;margin-bottom:10px}
  & .bh{display:flex;justify-content:space-between;align-items:flex-start}
  & .bh h3{font-size:16px;font-weight:700;font-stretch:108%}
  & .per{display:block;font-size:12px;color:var(--mut);margin-top:1px;font-weight:500}
  & .pc{font-size:22px;font-weight:800;font-stretch:112%}& .pc.wn{color:var(--neg)}
  & .lcd.mini{margin:9px 2px 8px;padding:5px 9px 8px}
  & .lcd.mini .lb{padding-top:4px}
  & .st{display:block;font-size:9px;font-weight:700;font-stretch:112%;letter-spacing:.08em;text-transform:uppercase;line-height:1;text-align:right}
  & .st.wn{color:var(--neg)}
  & .bf{display:flex;justify-content:space-between;font-size:12.5px;color:var(--mut);font-weight:500}
  & .bf span:first-child{color:var(--ink);font-weight:600}
  /* desktop */
  & .sd{position:absolute;left:0;top:0;bottom:0;width:236px;padding:24px 18px;background:var(--body2);box-shadow:inset -1px 0 0 var(--line),inset -2px 0 0 var(--edge);display:flex;flex-direction:column}
  & .logo{display:flex;align-items:center;justify-content:space-between;font-size:24px;font-weight:800;font-stretch:125%;letter-spacing:-.01em;margin:0 2px 20px}
  & .addb{display:flex;gap:8px;height:42px;font-weight:800;font-stretch:108%;font-size:14px;color:var(--oink);background:linear-gradient(180deg,var(--o1),var(--o2));box-shadow:inset 0 1.5px 0 rgba(255,255,255,.3),0 3px 0 var(--olip),0 5px 8px var(--sh);margin-bottom:18px}
  & .nv{display:flex;flex-direction:column;gap:2px}
  & .ni{position:relative;display:flex;align-items:center;gap:10px;padding:7px 10px;border-radius:9px;font-size:14px;font-weight:600;color:var(--mut)}
  & .ni em{order:9;margin-left:auto;width:5px;height:5px;border-radius:50%;background:transparent}
  & .ni.on{color:var(--ink);background:var(--well);box-shadow:inset 0 2px 3px var(--sh),0 1px 0 var(--edge)}
  & .ni.on em{background:var(--led);box-shadow:var(--ledglow,none)}
  & .nv.t .ni{padding:5px 10px;font-size:13.5px;font-weight:500}
  & .gl{font-size:12.5px;font-weight:700;color:var(--mut);margin:18px 10px 4px}
  & .mn{position:absolute;left:236px;right:0;top:0;bottom:0;padding:18px 40px;overflow:hidden}
  & .mn .hd{padding-top:2px}
  & .hero.w .hb{display:grid;grid-template-columns:420px 1fr;gap:28px;align-items:end}
  & .hero.w .hx{padding:0 0 4px}
  & .hero.w .sub{font-size:17px}
  & .hero.w .bl{font-size:14px;margin-top:6px}
  & .cols{display:grid;grid-template-columns:1fr 1fr;gap:0 24px}
  & .mn .sh{margin-top:14px}
  & .mn .rw{padding:8px 0}
  & .lcd.micro{width:120px;padding:3px 7px 6px;box-shadow:inset 0 1px 3px rgba(20,30,15,.35),0 0 0 2px #3a362e}
  & .lcd.micro .lb{padding-top:3px}& .lcd.micro .lb i{height:8px}
}
html[data-mode=dark] .app{
  --body:#16171a;--body2:#1d1e22;--well:#0f1012;--ink:#ebe7dc;--mut:#a7a193;--edge:rgba(255,255,255,.06);--sh:rgba(0,0,0,.55);--line:rgba(255,255,255,.08);
  --kt:#34363c;--kb:#28292e;--klip:#0a0a0c;--kink:#ebe7dc;
  --fn1:#5b4f45;--fn2:#463c34;--fnlip:#120e0b;--fnink:#f6eee4;
  --o1:#ff7a3d;--o2:#ef6326;--olip:#7a2d08;--oink:#1a0d05;--acct:#ff9461;
  --lcd:#07110e;--lcd2:#0a1512;--seg:#5ef2c4;--ghost:rgba(94,242,196,.07);--lmut:#5ef2c4;
  --pos:#71dc8f;--neg:#ff6b5e;--warn:#f2b84b;--led:#ff7a3d;--ledoff:#3a3b40;
  --glow:0 0 6px rgba(94,242,196,.6);--segf:drop-shadow(0 0 3px rgba(94,242,196,.55));--ledglow:0 0 5px rgba(255,122,61,.7);
  background:var(--body) linear-gradient(180deg,rgba(255,255,255,.025),rgba(255,255,255,0) 40%);
  & ::selection{background:var(--o1);color:#1a0d05}
  & .key,& .ci,& .tk,& .ty span.on{box-shadow:inset 0 1px 0 rgba(255,255,255,.09),0 3px 0 var(--klip),0 4px 7px var(--sh)}
  & .key:active{box-shadow:inset 0 1px 0 rgba(255,255,255,.06),0 1px 0 var(--klip)}
  & .tk.on{box-shadow:inset 0 1px 0 rgba(255,255,255,.06),0 1px 0 var(--klip)}
  & .cg .key.on{box-shadow:inset 0 2px 4px rgba(0,0,0,.6),0 1px 0 var(--klip)}
  & .lcd{box-shadow:inset 0 2px 6px rgba(0,0,0,.8),inset 0 0 0 1px rgba(94,242,196,.06),0 0 0 3px #050506,0 0 0 4px rgba(255,255,255,.06)}
  & .lcd.micro{box-shadow:inset 0 1px 3px rgba(0,0,0,.8),0 0 0 2px #050506}
  & .ann b,& .st{text-shadow:var(--glow)}
  & .lb i.on{box-shadow:0 0 4px rgba(94,242,196,.5)}
  & .lb i.bk{box-shadow:0 0 5px rgba(255,107,94,.7)}
  & .solar{background:#0a0a0b;box-shadow:inset 0 1px 2px rgba(0,0,0,.8),0 1px 0 rgba(255,255,255,.06)}
  & .solar i{background:linear-gradient(180deg,#3a2f2b,#221b19 50%,#2f2622)}
  & .lg{background:#3d3f45;color:#ebe7dc}
}
@keyframes p0{0%{fill:var(--ghost)}12%{fill:var(--seg)}52%{fill:var(--ghost)}}
@keyframes p1{0%{fill:var(--ghost);background:var(--ghost)}12%{fill:var(--seg);background:var(--seg)}}
@keyframes p0b{0%{background:var(--ghost)}12%{background:var(--seg)}52%{background:var(--ghost)}}
@keyframes bk{50%{opacity:.12}}
@keyframes bl{50%{opacity:0}}
@keyframes in{from{opacity:0;transform:translateY(4px)}}
`;

export const screens = { home, txns, add, budgets, desk };
