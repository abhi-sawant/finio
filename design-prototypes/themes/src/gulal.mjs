import { D } from './data.mjs';
import { ic } from './icons.mjs';

export const meta = {
  num: 3,
  slug: 'gulal',
  name: 'Gulal',
  tagline: 'Holi-powder pop. Loud, sticker-bright and a little cheeky — for people who’d rather money felt like a game they’re winning than homework they’re behind on.',
  fonts: 'https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,500;12..96,700;12..96,800&display=swap',
  swatch: ['#ffe14d', '#ff5fa8', '#4457ff', '#1b1230'],
  swatchDark: ['#1c1233', '#ff5fa8', '#ffe14d', '#fff3d6'],
  notes: [
    ['Shape', 'Chunky 22px radii, 2.5px ink outlines and hard offset shadows. Things look cut from sticker paper.'],
    ['Type', 'Bricolage Grotesque at 800 for every figure and title — wide, friendly, a touch wonky. Figures run to 76px.'],
    ['Density', 'Roomy and tappable. Big targets, one idea per block; built for a thumb, in a hurry, in a good mood.'],
    ['Motion', 'Physical: keys press down into their own shadow, the burst badge wobbles, the hero sticker tilts in.'],
    ['Voice', 'Warm and encouraging in the gaps (“you’ve got this”). Labels and figures stay exactly as in the product.'],
    ['Signature', 'The burst badge — budget used, stamped on the hero like a price sticker — and a colour per category.'],
  ],
};

const catColor = { Food: '#ff9a3d', Transport: '#6fd0ff', Shopping: '#a98bff', Entertainment: '#ff5fa8', Utilities: '#ffe14d', Healthcare: '#ff6b6b', Education: '#8ea2ff', Housing: '#5ee0b5', Income: '#5ee0b5' };

const burst = (n = 14, r1 = 38, r2 = 31, c = 40) => {
  let p = '';
  for (let i = 0; i < n * 2; i++) {
    const a = (Math.PI * i) / n;
    const r = i % 2 ? r2 : r1;
    p += `${(c + Math.cos(a) * r).toFixed(1)},${(c + Math.sin(a) * r).toFixed(1)} `;
  }
  return `<svg class="bsvg" viewBox="0 0 80 80" aria-hidden="true"><polygon points="${p}" fill="#ffe14d" stroke="#1b1230" stroke-width="2.5" stroke-linejoin="round"/></svg>`;
};
const spark = (s = 26, extra = '') =>
  `<svg class="spk" width="${s}" height="${s}" viewBox="0 0 24 24" ${extra} aria-hidden="true"><path d="M12 1C13 8 16 11 23 12 16 13 13 16 12 23 11 16 8 13 1 12 8 11 11 8 12 1Z" fill="#ffe14d" stroke="#1b1230" stroke-width="1.6" stroke-linejoin="round"/></svg>`;
const squig = `<svg class="sq" width="150" height="12" viewBox="0 0 150 12" aria-hidden="true"><path d="M2 6Q10 0 18 6T34 6 50 6 66 6 82 6 98 6 114 6 130 6 146 6" fill="none" stroke="#4457ff" stroke-width="4" stroke-linecap="round"/></svg>`;

const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n, i]) => (n === active ? `<span class="on">${ic(i, 20, 2.4)}${n}</span>` : `<span aria-label="${n}">${ic(i, 22, 2.2)}</span>`)).join('')}</nav>`;
const fab = `<span class="fab">${ic('plus', 28, 3)}</span>`;
const circ = (i) => `<span class="cb">${ic(i, 18, 2.2)}</span>`;

const item = (r) => `<div class="it"><span class="cat" style="background:${catColor[r.inc ? 'Income' : r.cat]}">${ic(r.icon, 20, 2.2)}</span><div class="nm"><b>${r.name}${r.rec ? ' ' + ic('repeat', 13, 2.4) : ''}</b><small>${r.cat} · ${r.acct}</small>${r.tag ? `<em class="chip2">${r.tag}</em>` : ''}</div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;

const alert = `<div class="alert"><span class="ai">${ic('alert', 16, 2.4)}</span><span>${D.due.name} payment due in ${D.due.days} days · <b>₹${D.due.amt}</b></span></div>`;

const hero = (wide) => `<section class="hero${wide ? ' wide' : ''}">
  <span class="chip">Safe to spend today</span>
  <div class="fig">₹${D.safe}</div>${squig}
  <p>₹${D.left} left this month · ${D.days} days to go — you’ve got this</p>
  <div class="bar"><i style="width:${D.pct}%"></i></div>
  <div class="bl"><b>Spent ₹${D.spent} of ₹${D.budget}</b></div>
  <span class="burst">${burst()}<b>${D.pct}%</b><small>used</small></span>
  ${spark(30, 'style="position:absolute;left:-10px;top:60px;transform:rotate(-12deg)"')}${spark(18, 'style="position:absolute;right:22px;bottom:70px"')}
</section>`;

const tile = (a) => `<div class="tile ${a.kind}"><b>${a.name}</b><small>${a.type}</small><span class="bal">${a.owed ? '−' : ''}₹${a.short}</span></div>`;

const home = `
<div class="pg">
  <header class="hd"><span class="av">A</span><div class="ti"><h1>${D.user}</h1><p>${ic('shield', 13, 2.4)} Kept only on this device</p></div>${circ('eye')}${circ('sliders')}</header>
  ${hero(false)}
  ${alert}
  <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
  <div class="strip">${D.accounts.map(tile).join('')}</div>
  <div class="sh"><h2>Latest</h2><a>See all</a></div>
  <div class="list">${item(D.groups[0].rows[1])}${item(D.groups[1].rows[0])}</div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  <header class="hd"><div class="ti"><h1 class="big">Transactions</h1></div>${circ('search')}${circ('eye')}</header>
  <div class="pills"><span class="on">All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span class="cb">${ic('chevl', 16, 3)}</span><b>October 2026</b><span class="cb">${ic('chevr', 16, 3)}</span></div>
  ${D.groups.map((g) => `<div class="dl"><span>${g.date}</span></div><div class="list">${g.rows.map(item).join('')}</div>`).join('')}
</div>
${fab}${tabs('Transactions')}`;

const add = `
<div class="pg">
  <header class="hd">${circ('back')}<div class="ti c"><h1>Add transaction</h1></div><span style="width:38px"></span></header>
  <div class="seg3"><span class="on">Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtc"><small>Amount</small><div class="fig">₹2,450<i class="caret"></i></div></div>
  <div class="np">${['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', ic('backspace', 26, 2.4)].map((k, n) => `<span${n === 11 ? ' class="del"' : ''}>${k}</span>`).join('')}</div>
  <div class="sel"><small>Account</small><b>HDFC Checking</b><em>Bank account</em>${ic('chevd', 18, 2.6)}</div>
  <div class="sh sm"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n, i], k) => `<span${k === 0 ? ' class="on"' : ''}><b style="background:${catColor[n]}">${ic(i, 22, 2.2)}${k === 0 ? `<u>${ic('check', 11, 3.5)}</u>` : ''}</b>${n}</span>`).join('')}</div>
</div>
<div class="cta"><button>Add transaction</button></div>`;

const budgets = `
<div class="pg">
  <header class="hd">${circ('back')}<div class="ti c"><h1>Budgets</h1></div>${circ('plus')}</header>
  ${D.budgets
    .map(
      (b, k) => `<section class="bc${k === 0 ? ' first' : ''}">
    <div class="bt"><div><h3>${b.name}</h3><small>Monthly · October 2026</small></div><span class="stk ${b.pct > 85 ? 'warn' : 'ok'}">${ic(b.pct > 85 ? 'alert' : 'check', 13, 3)} ${b.state}</span></div>
    <div class="bm"><span>₹${b.spent} of ₹${b.limit}</span><b>${b.pct}%</b></div>
    <div class="bar ${b.pct > 85 ? 'w' : ''}"><i style="width:${b.pct}%"></i></div>
    <div class="bl">₹${b.left} left this month</div>
  </section>`,
    )
    .join('')}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo"><span class="mk">F</span>Finio</div>
  <span class="addb">${ic('plus', 20, 3)} Add transaction</span>
  ${D.nav.map(([n, i]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${ic(i, 20, 2.2)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="hd"><span class="av">A</span><div class="ti"><h1>${D.user}</h1><p>${ic('shield', 13, 2.4)} Kept only on this device</p></div>${circ('eye')}${circ('sliders')}</header>
  ${hero(true)}
  ${alert}
  <div class="cols">
    <div>
      <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
      <div class="strip">${D.accounts.map(tile).join('')}</div>
      <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
      <div class="list gl2">${D.goals.map((g) => `<div class="gi"><div class="bm"><b>${g.name}</b><span>₹${g.have} / ₹${g.target}</span></div><div class="bar"><i style="width:${Math.max(g.pct, 0)}%"></i></div></div>`).join('')}</div>
    </div>
    <div>
      <div class="sh"><h2>Latest</h2><a>See all</a></div>
      <div class="list">${D.groups.flatMap((g) => g.rows).map(item).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{--bg:#ffe14d;--card:#fff;--ink:#1b1230;--mut:#5a4780;--on:#1b1230;--pink:#ff5fa8;--blue:#4457ff;--mint:#5ee0b5;--shc:#1b1230;--shh:#1b1230;--bd:2.5px solid var(--ink);
  background:var(--bg);color:var(--ink);font-family:'Bricolage Grotesque',system-ui,sans-serif;font-weight:500;font-size:14px;font-variant-numeric:tabular-nums}
html[data-mode=dark] .app{--bg:#1c1233;--card:#2b1e4b;--ink:#fff3d6;--mut:#c8b9ec;--shc:#ff5fa8;--shh:#ffe14d;--blue:#6b7aff}
.app ::selection{background:var(--pink);color:var(--on)}
.app .pg{position:absolute;inset:44px 0 0;padding:6px 16px;overflow:hidden;-webkit-mask-image:linear-gradient(#000 calc(100% - 130px),transparent calc(100% - 92px));mask-image:linear-gradient(#000 calc(100% - 130px),transparent calc(100% - 92px))}
.app .pg:has(~ .cta){-webkit-mask-image:none;mask-image:none}
.hd{display:flex;align-items:center;gap:8px;padding:8px 0 14px}
.hd .ti{flex:1;min-width:0}.hd .ti.c{text-align:center}
.hd h1{font-size:24px;font-weight:800;letter-spacing:-.02em;line-height:1.1}.hd h1.big{font-size:32px}
.hd p{font-size:12.5px;color:var(--mut);display:flex;align-items:center;gap:5px;font-weight:600;margin-top:1px}
.av{width:44px;height:44px;border-radius:50%;background:var(--pink);color:var(--on);border:var(--bd);display:grid;place-items:center;font-weight:800;font-size:20px;box-shadow:3px 3px 0 var(--shc)}
.cb{width:38px;height:38px;border-radius:50%;background:var(--card);border:var(--bd);display:grid;place-items:center;box-shadow:2px 2px 0 var(--shc);flex:none}
.hero{position:relative;background:var(--pink);color:var(--on);border:var(--bd);border-radius:28px;padding:18px 18px 16px;box-shadow:6px 6px 0 var(--shh);transform:rotate(-.6deg);animation:tiltin .6s cubic-bezier(.16,1,.3,1) backwards}
@keyframes tiltin{from{transform:rotate(-4deg) translateY(10px);opacity:0}}
.chip{display:inline-block;background:#fff;border:2px solid var(--on);border-radius:99px;padding:3px 12px;font-weight:700;font-size:13px;transform:rotate(-2deg)}
.hero .fig{font-size:76px;font-weight:800;letter-spacing:-.04em;line-height:1;margin:12px 0 0}
.sq{display:block;margin:0 0 8px 2px}
.hero p{font-weight:600;font-size:14px;margin-bottom:12px;max-width:260px}
.bar{height:24px;border:var(--bd);border-radius:99px;background:#fff;overflow:hidden;border-color:var(--on)}
.bar i{display:block;height:100%;border-right:2.5px solid var(--on);border-radius:99px;background:repeating-linear-gradient(45deg,#4457ff 0 9px,#6b7aff 9px 18px)}
.bar.w i{background:repeating-linear-gradient(45deg,#ff8a00 0 9px,#ffb347 9px 18px)}
.bar{border-color:var(--ink)}
.hero .bar{border-color:var(--on)}
.bl{font-weight:700;font-size:13.5px;margin-top:7px}
.burst{position:absolute;right:12px;top:-20px;width:82px;height:82px;display:grid;place-items:center;align-content:center;transform:rotate(12deg);color:#1b1230;animation:wob 3.2s ease-in-out infinite}
@keyframes wob{50%{transform:rotate(4deg) scale(1.05)}}
.bsvg{position:absolute;inset:0}.burst b{position:relative;font-size:21px;font-weight:800;line-height:1}.burst small{position:relative;font-size:11px;font-weight:700;line-height:1}
.alert{margin:16px 2px 0;background:var(--ink);color:var(--bg);border-radius:99px;padding:9px 14px 9px 10px;display:flex;align-items:center;gap:10px;font-weight:600;font-size:13.5px;transform:rotate(.5deg)}
.ai{width:28px;height:28px;border-radius:50%;background:#ff9a3d;color:#1b1230;display:grid;place-items:center;flex:none}
.sh{display:flex;justify-content:space-between;align-items:center;margin:20px 0 10px}
.sh.sm{margin:12px 0 8px}
.sh h2{font-size:20px;font-weight:800;letter-spacing:-.02em}
.sh a{font-size:12.5px;font-weight:700;background:var(--card);border:2px solid var(--ink);border-radius:99px;padding:2px 11px}
.strip{display:flex;gap:12px;overflow:hidden;padding:2px 4px 8px 0}
.tile{position:relative;flex:none;width:150px;min-height:112px;border:var(--bd);border-radius:22px;padding:12px 12px 10px;box-shadow:4px 4px 0 var(--shc);color:var(--on);display:flex;flex-direction:column}
.tile::before{content:'';position:absolute;top:-8px;right:18px;width:36px;height:14px;background:rgb(255 255 255 / .65);border:1.5px solid var(--on);transform:rotate(6deg)}
.tile.bank{background:#b9a4ff}.tile.savings{background:#6fe6bf}.tile.card{background:#ffa653}
.tile b{font-weight:800;font-size:14.5px;line-height:1.15}.tile small{font-size:12px;font-weight:600;opacity:.8}.tile .bal{margin-top:auto;font-size:25px;font-weight:800;letter-spacing:-.03em}
.list{background:var(--card);border:var(--bd);border-radius:22px;box-shadow:4px 4px 0 var(--shc);overflow:hidden}
.it{display:flex;align-items:center;gap:11px;padding:11px 14px;border-bottom:2px dashed color-mix(in srgb,var(--ink) 35%,transparent)}
.it:last-child{border-bottom:0}
.cat{width:42px;height:42px;border-radius:50%;border:2px solid var(--on);color:var(--on);display:grid;place-items:center;flex:none}
.it .nm{flex:1;min-width:0;display:flex;flex-wrap:wrap;align-items:center;gap:0 6px}
.it .nm b{font-weight:800;font-size:15px;width:100%;display:flex;align-items:center;gap:5px;line-height:1.2}
.it .nm small{font-size:12px;color:var(--mut);font-weight:600}
.chip2{font-style:normal;font-size:10.5px;font-weight:700;border:1.5px solid var(--ink);border-radius:99px;padding:0 7px}
.it .a{font-weight:800;font-size:15.5px;white-space:nowrap}
.it .a.inc{background:var(--mint);color:var(--on);border:2px solid var(--on);border-radius:99px;padding:1px 9px}
.tabs{position:absolute;left:16px;right:16px;bottom:26px;height:66px;border-radius:99px;background:var(--ink);color:var(--bg);display:flex;align-items:center;justify-content:space-around;padding:0 10px;box-shadow:4px 4px 0 var(--shc)}
.tabs span{display:flex;align-items:center;gap:7px;height:46px;padding:0 12px;border-radius:99px;font-weight:800;font-size:14px}
.tabs span.on{background:#ffe14d;color:#1b1230}
.fab{position:absolute;right:22px;bottom:106px;width:62px;height:62px;border-radius:50%;background:var(--blue);color:#fff;border:var(--bd);display:grid;place-items:center;box-shadow:4px 4px 0 var(--shc);z-index:5}
.pills{display:flex;gap:8px;margin-bottom:12px}
.pills span{padding:6px 14px;border:2px solid var(--ink);border-radius:99px;background:var(--card);font-weight:700;font-size:13.5px}
.pills span.on{background:var(--ink);color:var(--bg)}
.mo{display:flex;justify-content:space-between;align-items:center;font-weight:800;font-size:16px;margin-bottom:6px}
.dl{margin:12px 0 7px}.dl span{display:inline-block;background:var(--card);border:2px solid var(--ink);border-radius:99px;padding:1px 12px;font-weight:700;font-size:12.5px;transform:rotate(-1.5deg)}
.seg3{display:flex;background:var(--card);border:var(--bd);border-radius:99px;padding:4px;margin-bottom:12px;box-shadow:3px 3px 0 var(--shc)}
.seg3 span{flex:1;text-align:center;padding:8px 0;border-radius:99px;font-weight:700;font-size:14.5px}
.seg3 span.on{background:var(--pink);color:var(--on);border:2px solid var(--on)}
.amtc{background:var(--card);border:var(--bd);border-radius:22px;padding:8px 18px 6px;box-shadow:4px 4px 0 var(--shc);margin-bottom:12px}
.amtc small{font-size:12.5px;font-weight:700;color:var(--mut)}
.amtc .fig{font-size:66px;font-weight:800;letter-spacing:-.04em;line-height:1.05;display:flex;align-items:center;justify-content:center}
.caret{display:inline-block;width:5px;height:54px;background:var(--blue);border-radius:3px;margin-left:4px;animation:bl 1s steps(1) infinite}
@keyframes bl{50%{opacity:0}}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:9px;margin-bottom:12px}
.np span{height:52px;background:var(--card);border:var(--bd);border-radius:18px;box-shadow:3px 3px 0 var(--shc);display:grid;place-items:center;font-weight:800;font-size:26px;transition:transform .08s,box-shadow .08s}
.np span:active{transform:translate(3px,3px);box-shadow:0 0 0 var(--shc)}
.np span.del{background:#ff9a3d;color:#1b1230}
.sel{display:flex;align-items:center;gap:8px;background:var(--card);border:var(--bd);border-radius:18px;padding:8px 14px;position:relative}
.sel small{position:absolute;top:-9px;left:14px;background:var(--bg);padding:0 6px;font-size:12px;font-weight:700;color:var(--mut)}
.sel b{font-weight:800;font-size:16px}.sel em{flex:1;font-style:normal;font-size:12.5px;color:var(--mut);font-weight:600}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:8px 6px}
.cg span{display:flex;flex-direction:column;align-items:center;gap:4px;font-size:11.5px;font-weight:700}
.cg b{position:relative;width:46px;height:46px;border-radius:50%;border:2px solid var(--on);color:var(--on);display:grid;place-items:center}
.cg .on b{box-shadow:0 0 0 3px var(--bg),0 0 0 5.5px var(--ink)}
.cg u{position:absolute;right:-6px;top:-6px;width:19px;height:19px;border-radius:50%;background:var(--ink);color:var(--bg);display:grid;place-items:center}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 16px 30px;background:linear-gradient(transparent,var(--bg) 24%)}
.cta button{width:100%;height:56px;border-radius:99px;background:var(--pink);color:var(--on);border:var(--bd);font-weight:800;font-size:18px;box-shadow:4px 4px 0 var(--shc)}
.bc{background:var(--card);border:var(--bd);border-radius:22px;padding:14px 16px 12px;margin-bottom:14px;box-shadow:4px 4px 0 var(--shc)}
.bc.first{background:#b9a4ff;color:var(--on)}
.bc.first small,.bc.first .bl{color:var(--on)}
.bt{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:6px}
.bt h3{font-size:19px;font-weight:800;letter-spacing:-.02em}.bt small{font-size:12px;color:var(--mut);font-weight:600}
.stk{display:inline-flex;align-items:center;gap:4px;border:2px solid var(--on);border-radius:99px;padding:1px 9px;font-weight:700;font-size:12px;color:var(--on);transform:rotate(2deg)}
.stk.ok{background:var(--mint)}.stk.warn{background:#ff9a3d}
.bm{display:flex;justify-content:space-between;font-size:14px;font-weight:600;margin-bottom:7px}.bm b{font-weight:800}
.bc .bar{height:20px;background:#fff;border-color:var(--ink)}
.bc.first .bar,.bc .bar{border-color:#1b1230}
.bc .bl{color:var(--mut);font-weight:600;font-size:12.5px}
/* desktop */
.sd{position:absolute;left:16px;top:16px;bottom:16px;width:226px;background:var(--card);border:var(--bd);border-radius:26px;box-shadow:5px 5px 0 var(--shc);padding:18px 12px;display:flex;flex-direction:column;gap:3px;overflow:hidden}
.logo{font-size:23px;font-weight:800;letter-spacing:-.03em;display:flex;align-items:center;gap:10px;margin:0 6px 14px}
.mk{width:36px;height:36px;border-radius:50%;background:var(--pink);color:var(--on);border:2px solid var(--on);display:grid;place-items:center;font-weight:800}
.addb{display:flex;align-items:center;justify-content:center;gap:8px;height:46px;border-radius:99px;background:var(--blue);color:#fff;border:var(--bd);font-weight:800;margin-bottom:10px;box-shadow:3px 3px 0 var(--shc)}
.ni{display:flex;align-items:center;gap:10px;padding:9px 14px;border-radius:99px;font-weight:700;border:2px solid transparent}
.ni.on{background:var(--pink);color:var(--on);border-color:var(--on)}
.gl{font-size:12px;font-weight:700;color:var(--mut);padding:12px 14px 3px}
.mn{position:absolute;left:258px;right:0;top:0;bottom:0;padding:18px 36px 0 6px;overflow:hidden}
.mn .hd{padding-top:6px}
.hero.wide{padding:20px 28px 18px;transform:none}
.hero.wide .fig{font-size:96px}
.hero.wide p{max-width:none}
.hero.wide .bar,.hero.wide .bl{max-width:520px}
.hero.wide .burst{right:44px;top:-22px;width:112px;height:112px}.hero.wide .burst b{font-size:30px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 28px}
.gl2 .gi{padding:12px 16px;border-bottom:2px dashed color-mix(in srgb,var(--ink) 35%,transparent)}.gl2 .gi:last-child{border:0}
.gl2 .bar{height:16px;border-color:var(--ink)}
.mn .alert{margin-top:18px}
`;

export const screens = { home, txns, add, budgets, desk };
