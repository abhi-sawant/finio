import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 6,
  slug: 'parchi',
  name: 'Parchi',
  tagline: 'The thermal receipt. Finio as the slip that curls out of a kirana-store billing printer — for people who trust a figure most when it is printed, itemised and totalled.',
  blurb: 'The kirana billing slip. Warm thermal paper with torn edges, condensed mono line items, dotted leaders, double-height totals, a barcode — and one receipt-red ink.',
  fonts: 'https://fonts.googleapis.com/css2?family=Martian+Mono:wdth,wght@75..112.5,400..800&display=swap',
  swatch: ['#f7f4ec', '#23211c', '#c8242b', '#2e6a3d'],
  swatchDark: ['#11131a', '#dfe6f5', '#ff7a6e', '#7fcf9a'],
  notes: [
    ['Shape', 'Square everything, zero radius. Money sits on torn receipt strips with sawtooth top and bottom edges, laid on a darker counter. The tab bar is a perforated paper strip; the FAB is a square red print key docked into it.'],
    ['Type', 'Martian Mono throughout, riding its width axis: 75% condensed for line items, 112.5% wide and bold for totals, which print double-height (scaleY 1.65) like a thermal head.'],
    ['Density', 'Dense like a bill — numbered line items, dotted leaders, dashed rules between sections and a double rule under every total. Figures stay 14px+ so they scan.'],
    ['Motion', 'The hero slip feeds out of a printer slot once, line by line (steps() timing). Supporting: the add key and CTA give a short paper-jog on press; the caret is a blinking print block.'],
    ['Voice', 'Receipt idioms in the gaps: line numbers, SUBTOTAL, BAL C/F, END OF SLIP, “THANK YOU · KEPT ON THIS DEVICE”. Product labels print as-is in capitals.'],
    ['Signature', 'The torn slip with its barcode and printed time stamp (15 OCT 2026 · 09:41, day 15 of 31) — and Budgets as an itemised bill whose NEAR LIMIT prints as an inverted red block.'],
  ],
};

/* ---------- pieces ---------- */
// Deterministic barcode: bar/space widths derived from a fixed string, guard bars at both ends.
const barcode = (seed, w = 150, h = 30) => {
  const seq = [1, 1, 1, 1];
  for (const ch of seed) {
    const c = ch.charCodeAt(0);
    seq.push((c % 3) + 1, ((c >> 2) % 2) + 1, ((c >> 3) % 3) + 1, ((c >> 1) % 2) + 1);
  }
  seq.push(1, 1, 1);
  let x = 0;
  let bars = '';
  seq.forEach((s, i) => {
    if (i % 2 === 0) bars += `<rect x="${x}" width="${s}" height="${h}"/>`;
    x += s;
  });
  return `<svg class="bc" viewBox="0 0 ${x} ${h}" width="${w}" height="${h}" preserveAspectRatio="none" shape-rendering="crispEdges" fill="currentColor" aria-hidden="true">${bars}</svg>`;
};

const slip = (inner, cls = '') => `<div class="sl ${cls}"><div class="slip">${inner}</div></div>`;
const li = (label, amt, cls = '') => `<div class="li ${cls}"><span>${label}</span><i></i><b>${amt}</b></div>`;
const dr = '<div class="dr"></div>';
const eq = '<div class="eq"></div>';
const no = (n) => `<em class="no">${String(n).padStart(2, '0')}</em>`;
const meter = (pct, today = false, warn = false) =>
  `<div class="mt${warn ? ' w' : ''}"><i style="width:${pct}%"></i>${today ? `<u style="left:${D.dayPct}%"><span>Today</span></u>` : ''}</div>`;

const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => `<span${n === active ? ' class="on"' : ''}>${ic(i, 18, 1.75)}${n}</span>`).join('')}</nav>`;
const fab = `<span class="fab" aria-label="Add transaction">${ic('plus', 24, 2.25)}</span>`;
const ib = (i) => `<span class="ib">${ic(i, 18, 1.75)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;
const sh = (t, a = 'See all') => `<div class="sh"><h2>${t}</h2>${a ? `<a>${a}</a>` : ''}</div>`;

const tag = (t) => (t ? ` · <span class="tg ${t === 'Essential' ? 'e' : 'd'}">${t}</span>` : '');
const row = (r) => `<div class="tx"><div class="l1"><span class="nm">${r.name}${r.rec ? ic('repeat', 12, 2) : ''}</span><i></i><b class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</b></div><div class="l2">${r.cat} · ${r.acct}${tag(r.tag)}</div></div>`;
const acc = (a, k) => `<div class="tx ac"><div class="l1">${no(k + 1)}<span class="nm">${a.name}</span><i></i><b class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</b></div><div class="l2">${a.type}</div></div>`;
const due = `<div class="due">${ic('alert', 16, 2)}<span>${D.due.name} payment of <b>₹${D.due.amt}</b> is due in ${D.due.days} days</span></div>`;

const stamp = `<div class="bcr">${barcode('FINIO*151026*1562')}<div class="ts"><b>15 OCT 2026 · 09:41</b><span>Day 15 of 31 · Slip 015</span></div></div>`;

const hero = (wide = false) => `<div class="feedw${wide ? ' wide' : ''}"><div class="mouth"></div><div class="clip"><div class="feed">${slip(
  wide
    ? `<div class="hw"><div><div class="lab">Safe to spend today</div><div class="dh"><span class="r">₹</span>${D.safe}</div><p class="sub">₹${D.left} left this month · ${D.days} days to go</p>${stamp}</div>
       <div class="hr">${li('Spent', `₹${D.spent}`)}${li('Of budget', `₹${D.budget}`)}${dr}${li('Bal c/f', `₹${D.left}`, 'b')}${eq}${meter(D.pct, true)}<div class="mlab"><span>${D.pct}% used · <em class="ok">Under pace</em></span><span>Day 15/31</span></div></div></div>`
    : `<div class="lab">Safe to spend today</div><div class="dh"><span class="r">₹</span>${D.safe}</div><p class="sub">₹${D.left} left this month · ${D.days} days to go</p>${li('Spent', `₹${D.spent} / ₹${D.budget}`)}${meter(D.pct, true)}<div class="mlab"><span>${D.pct}% used · <em class="ok">Under pace</em></span><span>Day 15/31</span></div>${dr}${stamp}`,
  'hero',
)}</div></div></div>`;

const accounts = `${sh('Where it sits')}${dr}${D.accounts.map(acc).join('')}${dr}${li('Total balance', `₹${D.total}`, 'tot')}${eq}`;

/* ---------- screens ---------- */
const home = `
<div class="pg home">
  <header class="hd"><div class="ti"><h1 class="who">${D.user}</h1><p>${ic('shield', 12, 2)}Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></header>
  ${hero()}
  ${due}
  ${slip(accounts)}
  ${slip(`${sh('Latest')}${dr}${row(D.groups[0].rows[1])}`, 'last')}
</div>
${fab}${tabs('Home').replace('class="tabs"', 'class="tabs f"')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search') + ib('eye'))}
  <div class="tseg"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo">${ib('chevl')}<b>October 2026</b>${ib('chevr')}</div>
  ${slip(
    D.groups.map((g, k) => `${k ? dr : ''}<div class="dl"><span>${g.date}</span><span>${g.rows.length} item${g.rows.length > 1 ? 's' : ''}</span></div>${g.rows.map(row).join('')}`).join('') +
      `${eq}<p class="thx">5 items · end of slip</p>`,
    'tl',
  )}
</div>
${fab}${tabs('Transactions').replace('class="tabs"', 'class="tabs f"')}`;

const keys = ['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 22, 1.75)];
const add = `
<div class="pg">
  ${hd('Add transaction', ib('back'), '')}
  <div class="tseg s3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  ${slip(`<div class="amt"><span class="lab">Amount</span><div class="dh"><span class="r">₹</span>2,450<i class="caret"></i></div></div>`, 'amts')}
  <div class="np">${keys.map((k) => `<span>${k}</span>`).join('')}</div>
  <div class="fl"><small>Account</small><i></i><b>HDFC Checking</b>${ic('chevd', 14, 2)}<em>Bank account</em></div>
  ${sh('Category', 'Split')}
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}>${ic(i, 20, 1.75)}${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back'), ib('eye') + ib('plus'))}
  ${slip(
    `<div class="bhd"><b>Monthly · October 2026</b><span>4 budgets</span></div>${eq}
    <div class="bcol"><span>Item</span><span>Spent</span><span>Limit</span><span>Left</span></div>${dr}
    ${D.budgets
      .map(
        (b, k) => `<section class="bu">
      <div class="bt">${no(k + 1)}<h3>${b.name}</h3><b class="pc${b.pct > 85 ? ' w' : ''}">${b.pct}%</b></div>
      <div class="bn"><span></span><span>₹${b.spent}</span><span>₹${b.limit}</span><b>₹${b.left}</b></div>
      <div class="bs">${meter(b.pct, false, b.pct > 85)}${b.pct > 85 ? `<span class="nl">${b.state}</span>` : `<span class="ok">${b.state}</span>`}</div>
    </section>`,
      )
      .join(dr)}
    ${eq}<p class="thx">Left = subtotal per budget · end of bill</p>`,
    'bill',
  )}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span>Finio</span><small>Personal bill book</small></div>
  <span class="addb">${ic('plus', 16, 2.25)}Add transaction</span>
  ${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 16, 1.75)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni t">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="hd"><div class="ti"><h1 class="who">${D.user}</h1><p>${ic('shield', 12, 2)}Kept only on this device</p></div><div class="rt">${ib('eye')}${ib('sliders')}</div></header>
  ${hero(true)}
  ${due}
  <div class="cols">
    <div>
      ${slip(`${sh('Where it sits')}${dr}${D.accounts.map(acc).join('')}${dr}${li('Total balance', `₹${D.total}`, 'tot')}${eq}`)}
      ${slip(`${sh('Savings goals')}${dr}${D.goals.map((g, k) => `<div class="gi">${li(`${no(k + 1)}${g.name}`, `₹${g.have} / ₹${g.target}`)}${meter(g.pct)}</div>`).join('')}`)}
    </div>
    <div>
      ${slip(`${sh('Latest')}${dr}${D.groups.flatMap((g) => g.rows).map(row).join('')}${eq}<p class="thx">Thank you · kept on this device</p>`)}
    </div>
  </div>
</main>`;

/* ---------- styles ---------- */
export const css = `
.app{--counter:#e3ddcf;--paper:#f7f4ec;--paper2:#efeadd;--ink:#23211c;--mut:#686254;--rule:#a59d8a;--faint:#ddd6c6;
  --acc:#c8242b;--on-acc:#fbf7ef;--pos:#2e6a3d;--neg:#c8242b;--warn:#8f5c08;--slotc:#2b2925;--band:rgb(90 70 30 / .035);--shade:rgb(60 45 20 / .22);
  background:var(--counter);color:var(--ink);font-family:'Martian Mono',ui-monospace,'SF Mono',Menlo,monospace;font-stretch:87.5%;font-weight:400;font-size:12px;line-height:1.4;font-variant-numeric:tabular-nums;caret-color:var(--acc)}
html[data-mode=dark] .app{--counter:#0b0d13;--paper:#181c27;--paper2:#141822;--ink:#dfe6f5;--mut:#909bb4;--rule:#4a5470;--faint:#2a3142;
  --acc:#ff7a6e;--on-acc:#130c0e;--pos:#7fcf9a;--neg:#ff7a6e;--warn:#e8b45a;--slotc:#000;--band:rgb(150 180 255 / .03);--shade:rgb(0 0 0 / .6)}
.app ::selection{background:var(--acc);color:var(--on-acc)}
.app :focus-visible{outline:2px solid var(--acc);outline-offset:2px}
.app .ic{flex:none}
.app .pg{position:absolute;inset:44px 0 70px;padding:4px 16px;overflow:hidden}
.app .pg.home{bottom:72px}

/* header */
.hd{display:flex;align-items:center;gap:8px;padding:6px 0 10px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-size:15px;font-weight:700;font-stretch:112.5%;text-transform:uppercase;letter-spacing:.02em;display:inline-block;transform:scaleY(1.35);transform-origin:0 60%}
.hd h1.who{font-size:17px}
.hd p{font-size:10.5px;font-stretch:75%;color:var(--mut);display:flex;gap:6px;align-items:center;margin-top:5px;text-transform:uppercase;letter-spacing:.04em}
.hd .rt{display:flex;gap:6px}
.ib{width:36px;height:36px;display:grid;place-items:center;border:1.5px solid var(--ink);background:var(--paper);color:var(--ink)}

/* the slip: torn sawtooth top + bottom, thermal banding */
.sl{filter:drop-shadow(0 1.5px 1px var(--shade));margin:0 0 8px}
.slip{padding:13px 14px 12px;color:var(--ink);
  background:linear-gradient(90deg,transparent 0 31%,var(--band) 31% 33%,transparent 33% 64%,var(--band) 64% 65%,transparent 65% 88%,var(--band) 88% 91%,transparent 91%),
    repeating-linear-gradient(90deg,transparent 0 3px,var(--band) 3px 4px),var(--paper);
  -webkit-mask:conic-gradient(from 135deg at top,#0000,#000 1deg 89deg,#0000 90deg) top/10px 51% repeat-x,conic-gradient(from -45deg at bottom,#0000,#000 1deg 89deg,#0000 90deg) bottom/10px 51% repeat-x;
          mask:conic-gradient(from 135deg at top,#0000,#000 1deg 89deg,#0000 90deg) top/10px 51% repeat-x,conic-gradient(from -45deg at bottom,#0000,#000 1deg 89deg,#0000 90deg) bottom/10px 51% repeat-x}
.dr{height:1.5px;margin:7px 0;background:repeating-linear-gradient(90deg,var(--rule) 0 5px,transparent 5px 9px)}
.eq{height:5px;margin:6px 0 4px;background:repeating-linear-gradient(90deg,var(--ink) 0 7px,transparent 7px 10px) top/100% 1.5px no-repeat,repeating-linear-gradient(90deg,var(--ink) 0 7px,transparent 7px 10px) bottom/100% 1.5px no-repeat}
.no{font-style:normal;font-stretch:75%;font-size:10px;color:var(--mut);margin-right:7px;flex:none}
.li{display:flex;align-items:baseline;gap:0;font-size:11.5px;text-transform:uppercase;padding:1.5px 0}
.li span{font-stretch:75%;letter-spacing:.03em;display:flex;align-items:baseline}
.li i{flex:1;min-width:14px;margin:0 6px;border-bottom:2px dotted var(--rule);transform:translateY(-3px)}
.li b{font-weight:600;font-size:13.5px;font-stretch:100%;white-space:nowrap}
.li.tot{padding-top:3px}.li.tot span{font-weight:700}.li.tot b{font-size:17px;font-weight:800;font-stretch:112.5%}
.li.b span{font-weight:700}
.thx{text-align:center;font-size:9.5px;font-stretch:75%;letter-spacing:.16em;text-transform:uppercase;color:var(--mut);margin-top:6px}

/* section heads */
.sh{display:flex;justify-content:space-between;align-items:baseline}
.sh h2{font-size:11.5px;font-weight:700;font-stretch:112.5%;letter-spacing:.06em;text-transform:uppercase}
.sh a{font-size:10.5px;font-stretch:75%;font-weight:600;color:var(--acc);text-transform:uppercase;letter-spacing:.06em;border-bottom:1.5px solid currentColor}

/* hero */
.feedw{position:relative;padding-top:4px;margin:0 0 4px}
.mouth{position:absolute;top:0;left:-6px;right:-6px;height:6px;background:var(--slotc);z-index:3;box-shadow:0 1px 0 rgb(255 255 255 / .25)}
.mouth::after{content:'';position:absolute;left:10px;right:10px;top:2px;height:1px;background:rgb(255 255 255 / .18)}
.clip{overflow:hidden;padding:0 0 6px}
.feed{animation:feed 1.5s steps(14,jump-end) .25s backwards}
@keyframes feed{from{transform:translateY(-100%)}}
.feed .sl{margin:0}
.hero .slip{padding-top:14px}
.lab{font-size:11px;font-stretch:75%;letter-spacing:.12em;text-transform:uppercase;color:var(--mut)}
.dh{font-size:40px;font-weight:800;font-stretch:112.5%;line-height:1;letter-spacing:-.02em;display:flex;align-items:flex-start;height:72px;margin:6px 0 0;transform:scaleY(1.65);transform-origin:0 0;white-space:nowrap}
.dh .r{font-size:.5em;font-weight:600;margin:.12em .12em 0 0}
.sub{font-size:11.5px;font-stretch:75%;text-transform:uppercase;letter-spacing:.03em;margin-bottom:2px}
.mt{position:relative;height:10px;margin:8px 0 4px;background:repeating-linear-gradient(90deg,var(--faint) 0 4px,transparent 4px 6px)}
.mt i{position:absolute;left:0;top:0;bottom:0;background:repeating-linear-gradient(90deg,var(--ink) 0 4px,transparent 4px 6px)}
.mt.w i{background:repeating-linear-gradient(90deg,var(--acc) 0 4px,transparent 4px 6px)}
.mt u{position:absolute;top:-4px;bottom:-4px;width:2px;margin-left:-1px;background:var(--acc)}
.mt u span{position:absolute;bottom:100%;left:4px;font-size:9px;font-stretch:75%;letter-spacing:.1em;text-transform:uppercase;color:var(--acc);font-weight:600;line-height:1;transform:translateY(4px)}
.mlab{display:flex;justify-content:space-between;font-size:10.5px;font-stretch:75%;letter-spacing:.04em;text-transform:uppercase;color:var(--mut);margin-top:6px}
.mlab span:first-child{color:var(--ink)}
.ok{color:var(--pos);font-style:normal;font-weight:600}
.bcr{display:flex;align-items:flex-end;justify-content:space-between;gap:12px;margin-top:2px}
.bc{color:var(--ink);display:block}
.ts{text-align:right;font-stretch:75%;text-transform:uppercase;letter-spacing:.05em;line-height:1.35}
.ts b{display:block;font-size:11px;font-weight:700}.ts span{font-size:9.5px;color:var(--mut)}

/* due */
.due{display:flex;gap:8px;align-items:center;margin:6px 0 10px;padding:8px 10px;border:1.5px dashed var(--acc);color:var(--ink);font-size:10.5px;font-stretch:75%;text-transform:uppercase;letter-spacing:0;line-height:1.45;background:color-mix(in srgb,var(--acc) 6%,transparent)}
.due .ic{color:var(--acc);margin-top:1px}
.due b{font-size:11.5px;font-stretch:100%;color:var(--acc)}

/* rows */
.tx{padding:5px 0}
.l1{display:flex;align-items:baseline}
.l1 .nm{font-size:12.5px;font-weight:600;font-stretch:75%;text-transform:uppercase;letter-spacing:.02em;display:flex;align-items:center;gap:5px;min-width:0}
.l1 .nm .ic{color:var(--mut)}
.l1 i{flex:1;min-width:10px;margin:0 6px;border-bottom:2px dotted var(--rule);transform:translateY(-3px)}
.l1 .a{font-size:14px;font-weight:600;white-space:nowrap}
.a.inc{color:var(--pos)}.a.neg{color:var(--neg)}
.l2{font-size:10.5px;font-stretch:75%;color:var(--mut);text-transform:uppercase;letter-spacing:.03em;margin-top:1px}
.ac .l2{padding-left:21px}
.tg{display:inline-block;padding:0 4px;border:1px solid currentColor;line-height:1.35;font-weight:600}
.tg.e{color:var(--pos)}.tg.d{color:var(--warn)}
.sl.last{margin-bottom:0}

/* tab bar: perforated strip */
.tabs{position:absolute;left:0;right:0;bottom:0;height:70px;background:var(--paper);display:flex;justify-content:space-around;align-items:flex-start;padding:14px 6px 0;border-top:1.5px dashed var(--rule)}
.tabs::before{content:'';position:absolute;left:0;right:0;top:4px;height:6px;background:radial-gradient(circle at 5px 3px,var(--counter) 1.6px,transparent 2.1px) 0 0/10px 6px repeat-x}
.tabs.f{padding-right:76px;justify-content:space-between}
.tabs.f span{min-width:0;padding:5px 4px 4px}
.tabs span{display:flex;flex-direction:column;align-items:center;gap:4px;font-size:9.5px;font-stretch:75%;letter-spacing:.04em;text-transform:uppercase;color:var(--mut);padding:5px 5px 4px;min-width:58px}
.tabs span.on{background:var(--ink);color:var(--paper);font-weight:700}
.fab{position:absolute;right:14px;bottom:12px;width:52px;height:52px;background:var(--acc);color:var(--on-acc);display:grid;place-items:center;z-index:5;box-shadow:0 0 0 3px var(--counter),0 0 0 4.5px var(--acc)}
.fab:active,.cta button:active{animation:jog .28s steps(4,end)}
@keyframes jog{25%{transform:translateY(3px)}50%{transform:translateY(-1px)}75%{transform:translateY(1px)}}

/* txns */
.tseg{display:grid;grid-template-columns:repeat(4,1fr);border:1.5px solid var(--ink);background:var(--paper)}
.tseg.s3{grid-template-columns:repeat(3,1fr)}
.tseg span{text-align:center;padding:7px 0;font-size:11px;font-stretch:75%;letter-spacing:.06em;text-transform:uppercase;color:var(--mut)}
.tseg span+span{border-left:1.5px solid var(--ink)}
.tseg span.on{background:var(--ink);color:var(--paper);font-weight:700}
.mo{display:flex;align-items:center;justify-content:space-between;margin:10px 0 8px}
.mo b{font-size:13px;font-weight:700;font-stretch:112.5%;text-transform:uppercase;letter-spacing:.05em}
.mo .ib{width:32px;height:32px}
.dl{display:flex;justify-content:space-between;font-size:10.5px;font-stretch:75%;letter-spacing:.08em;text-transform:uppercase;color:var(--mut);padding:2px 0 3px}
.dl span:first-child{color:var(--ink);font-weight:700;font-stretch:100%}
.tl .tx{padding:4px 0 6px}

/* add */
.amts .slip{padding:14px 14px 10px}
.amt{display:flex;flex-direction:column;align-items:flex-end}
.amt .lab{align-self:flex-start}
.amt .dh{font-size:40px;height:68px;transform-origin:100% 0}
.caret{display:inline-block;width:14px;height:.8em;margin:.1em 0 0 6px;background:var(--acc);animation:bl 1s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);border:1.5px solid var(--ink);background:var(--paper);margin:2px 0 10px}
.np span{height:46px;display:grid;place-items:center;font-size:20px;font-weight:500;font-stretch:100%;border-right:1px solid var(--faint);border-bottom:1px solid var(--faint)}
.np span:nth-child(3n){border-right:0}.np span:nth-last-child(-n+3){border-bottom:0}
.np span:active{background:var(--ink);color:var(--paper)}
.fl{display:flex;align-items:baseline;flex-wrap:wrap;gap:0 6px;background:var(--paper);border:1.5px solid var(--ink);padding:8px 10px 7px;margin-bottom:12px}
.fl small{font-size:10.5px;font-stretch:75%;letter-spacing:.1em;text-transform:uppercase;color:var(--mut)}
.fl i{flex:1;border-bottom:2px dotted var(--rule);transform:translateY(-3px)}
.fl b{font-size:13px;font-weight:700;text-transform:uppercase}
.fl .ic{align-self:center}
.fl em{flex-basis:100%;text-align:right;font-style:normal;font-size:10px;font-stretch:75%;text-transform:uppercase;letter-spacing:.06em;color:var(--mut);padding-right:20px}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px}
.cg span{display:flex;flex-direction:column;align-items:center;gap:6px;padding:9px 2px 7px;background:var(--paper);border:1px solid var(--faint);font-size:9.5px;font-stretch:75%;letter-spacing:.03em;text-transform:uppercase;color:var(--mut)}
.cg span .ic{color:var(--ink)}
.cg span.on{background:var(--ink);border-color:var(--ink);color:var(--paper);font-weight:700}.cg span.on .ic{color:var(--paper)}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 16px 30px;background:var(--counter);border-top:1.5px dashed var(--rule)}
.cta button{width:100%;height:52px;border:0;border-radius:0;background:var(--acc);color:var(--on-acc);font-family:inherit;font-size:13px;font-weight:700;font-stretch:112.5%;letter-spacing:.08em;text-transform:uppercase}

/* budgets: the itemised bill */
.bill .slip{padding:16px 14px 14px}
.bhd{display:flex;justify-content:space-between;align-items:baseline;text-transform:uppercase}
.bhd b{font-size:12px;font-weight:700;font-stretch:100%;letter-spacing:.03em}.bhd span{font-size:10px;font-stretch:75%;color:var(--mut);letter-spacing:.06em}
.bcol,.bn{display:grid;grid-template-columns:1fr 76px 76px 76px;align-items:baseline}
.bcol{font-size:9.5px;font-stretch:75%;letter-spacing:.1em;text-transform:uppercase;color:var(--mut);margin-top:6px}
.bcol span:not(:first-child),.bn>*:not(:first-child){text-align:right}
.bu{padding:4px 0 3px}
.bt{display:flex;align-items:baseline}
.bt h3{flex:1;font-size:12.5px;font-weight:700;font-stretch:75%;text-transform:uppercase;letter-spacing:.03em}
.pc{font-size:18px;font-weight:800;font-stretch:112.5%;display:inline-block;transform:scaleY(1.35);transform-origin:100% 80%}
.pc.w{color:var(--acc)}
.bn{font-size:12.5px;font-weight:500;margin-top:2px}.bn b{font-weight:700}
.bs{display:flex;align-items:center;gap:10px;margin-top:2px}
.bs .mt{flex:1;margin:6px 0}
.bs>span{font-size:10px;font-stretch:75%;letter-spacing:.08em;text-transform:uppercase;white-space:nowrap;min-width:78px;text-align:center}
.nl{background:var(--acc);color:var(--on-acc);font-weight:800;padding:3px 6px}
.bs .ok{padding:2px 0}

/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:224px;background:var(--paper);padding:28px 22px;display:flex;flex-direction:column;gap:1px;filter:drop-shadow(1px 0 1px var(--shade));
  -webkit-mask:conic-gradient(from 225deg at right,#0000,#000 1deg 89deg,#0000 90deg) 0 0/100% 10px repeat-y;mask:conic-gradient(from 225deg at right,#0000,#000 1deg 89deg,#0000 90deg) 0 0/100% 10px repeat-y}
.logo{margin-bottom:22px}
.logo span{display:block;font-size:24px;font-weight:800;font-stretch:112.5%;text-transform:uppercase;letter-spacing:.04em;transform:scaleY(1.6);transform-origin:0 0;height:40px}
.logo small{font-size:9.5px;font-stretch:75%;letter-spacing:.14em;text-transform:uppercase;color:var(--mut)}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;background:var(--acc);color:var(--on-acc);font-weight:700;font-stretch:100%;font-size:11.5px;text-transform:uppercase;letter-spacing:.05em;padding:11px 0;margin-bottom:18px}
.ni{display:flex;align-items:center;gap:10px;padding:7px 8px;font-size:12px;font-stretch:87.5%;text-transform:uppercase;letter-spacing:.03em;color:var(--mut)}
.ni.on{background:var(--ink);color:var(--paper);font-weight:700}
.ni.t{padding:4px 8px;font-size:11px;font-stretch:75%}
.gl{font-size:10px;font-stretch:75%;font-weight:700;letter-spacing:.16em;text-transform:uppercase;color:var(--ink);margin:16px 0 4px;padding:0 8px 6px;background:repeating-linear-gradient(90deg,var(--rule) 0 5px,transparent 5px 9px) bottom/100% 1.5px no-repeat}
.mn{position:absolute;left:224px;right:0;top:0;bottom:0;padding:16px 44px 0 40px;overflow:hidden}
.mn .hd{padding-top:0}
.feedw.wide .slip{padding:16px 22px 16px}
.hw{display:grid;grid-template-columns:auto 1fr;gap:0 48px;align-items:start}
.hw .dh{font-size:48px;height:82px}
.hw .sub{margin-bottom:10px}
.hw .hr{padding-top:4px}
.hw .mt{margin-top:16px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 24px;align-items:start}
.gi{padding:3px 0}.gi .mt{margin:4px 0 6px}
.gi .li span{font-weight:600}
.mn .due{margin:4px 0 10px}
`;

export const screens = { home, txns, add, budgets, desk };
