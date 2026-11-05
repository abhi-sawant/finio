import { D } from './data.mjs';

// Khel — the 8-bit handheld. Every icon in this module is authored on a pixel grid (no ic()):
// 12×12 grids drawn at 24px and 8×8 grids drawn at 16px, so one "pixel" is always 2 screen px.

export const meta = {
  num: 10,
  slug: 'khel',
  name: 'Khel',
  tagline: 'The 8-bit handheld. Money as a small pixel adventure — budgets are HP bars, goals are quests, the add button is a spinning coin — for people who stay on top of things when it feels like play.',
  blurb: 'An original pixel-art handheld: four-shade LCD green by day, a CRT night palette after dark, budgets as segmented HP bars and a coin for the add button.',
  fonts: 'https://fonts.googleapis.com/css2?family=Pixelify+Sans:wght@400;500;700&family=Silkscreen&family=VT323&display=swap',
  swatch: ['#e0efbe', '#0f380f', '#9bbc0f', '#a01641'],
  swatchDark: ['#0d0b1e', '#f6edd3', '#ffcf3f', '#ff5c72'],
  notes: [
    ['Shape', 'No radii anywhere. Panels are RPG text boxes: a stepped outer pixel border with notched corners and a second inner line. Keys and buttons sit on a 3px base and drop 2px when pressed.'],
    ['Type', 'Pixelify Sans for titles, labels, tabs and row text; VT323 for every figure (hero 76px, keypad 32px, row amounts 22px) because its 3, 5 and 8 never collide. Silkscreen only for tiny uppercase HUD tags (≥11px). Font smoothing is off on figures and titles only.'],
    ['Density', 'Chunky and HUD-like: big targets, boxed groups, accounts as three inventory slots so Home fits in one screen without scrolling.'],
    ['Motion', 'Signature: the hero score ticks up to ₹1,562 once on load (digit strips stepped, no easing) and the coin FAB idles with a 4-frame spin. HP bars fill one segment at a time; the selected category’s cursor arrow blinks.'],
    ['Voice', 'Game idioms in the gaps, always true to the data: “Day 15/31”, “38% used”, “! 3 days”, “Quest: Emergency Fund 20%”. Product labels stay as they are.'],
    ['Signature', 'The budget as a 20-segment HP bar with a pixel heart — it turns red and the heart blinks near the limit — and the Add screen’s chunky pixel keypad with an “A” action button.'],
  ],
};

// ---------- pixel icon kit ----------
const G = (s) => s.trim().split(/\s+/);
const rects = (rows) => {
  let r = '';
  rows.forEach((row, y) => {
    let x = 0;
    while (x < row.length) {
      if (row[x] === '#') {
        const s = x;
        while (x < row.length && row[x] === '#') x++;
        r += `<rect x="${s}" y="${y}" width="${x - s}" height="1"/>`;
      } else x++;
    }
  });
  return r;
};
const px = (rows, size, cls = '') =>
  `<svg class="ic px${cls ? ' ' + cls : ''}" width="${size}" height="${size}" viewBox="0 0 ${rows[0].length} ${rows.length}" fill="currentColor" shape-rendering="crispEdges" aria-hidden="true">${rects(rows)}</svg>`;

const P = {
  house: G(`
.....##.....
....####....
...######...
..########..
.##########.
############
..#......#..
..#......#..
..#..##..#..
..#..##..#..
..#..##..#..
..########..`),
  utensils: G(`
...#..#..#..
..#..#..#...
...#..#..#..
............
############
#..........#
#..........#
.#........#.
.##......##.
..########..
....####....
............`),
  car: G(`
............
...######...
..#......#..
..#......#..
.##########.
############
##.######.##
############
############
.##......##.
.##......##.
............`),
  bag: G(`
....####....
...#....#...
...#....#...
.##########.
.#........#.
.#........#.
.#..#..#..#.
.#...##...#.
.#........#.
.#........#.
.##########.
............`),
  film: G(`
............
############
#.#......#.#
###......###
#.#......#.#
###......###
#.#......#.#
###......###
#.#......#.#
############
............
............`),
  zap: G(`
......###...
.....###....
....###.....
...###......
..########..
......###...
.....###....
....###.....
...##.......
..##........
..#.........
............`),
  heart: G(`
............
..##....##..
.####..####.
####.######.
###.#######.
############
.##########.
..########..
...######...
....####....
.....##.....
............`),
  book: G(`
............
.####..####.
#....##....#
#....##....#
#....##....#
#....##....#
#....##....#
#....##....#
#.##.##.##.#
#....##....#
.####..####.
............`),
  banknote: G(`
............
............
############
#..........#
#.#..##..#.#
#...####...#
#...#..#...#
#...####...#
#.#..##..#.#
#..........#
############
............`),
  bank: G(`
.....##.....
...##..##...
.##......##.
############
............
.#..#..#..#.
.#..#..#..#.
.#..#..#..#.
.#..#..#..#.
............
############
############`),
  savings: G(`
............
..########..
.#........#.
..########..
.#........#.
..########..
.#........#.
..########..
.#........#.
..########..
............
............`),
  card: G(`
............
............
############
#..........#
############
############
#..........#
#.###......#
#..........#
############
............
............`),
  wallet: G(`
............
..########..
.#........#.
############
#..........#
#..........#
#.......####
#.......#.##
#.......####
#..........#
############
............`),
  arrows: G(`
............
.......#....
.......##...
##########..
.......##...
.......#....
....#.......
...##.......
..##########
...##.......
....#.......
............`),
  chart: G(`
............
.........##.
.........##.
.....##..##.
.....##..##.
.##..##..##.
.##..##..##.
.##..##..##.
.##..##..##.
.##..##..##.
############
............`),
  sliders: G(`
............
...##.......
###########.
...##.......
............
.......##...
###########.
.......##...
............
.##.........
###########.
.##.........`),
  search: G(`
............
..####......
.#....#.....
#......#....
#......#....
#......#....
#......#....
.#....#.....
..####.#....
.......##...
........##..
.........##.`),
  eye: G(`
............
............
....####....
..##....##..
.#...##...#.
#...####...#
#...####...#
.#...##...#.
..##....##..
....####....
............
............`),
  plus: G(`
............
.....##.....
.....##.....
.....##.....
.....##.....
.##########.
.##########.
.....##.....
.....##.....
.....##.....
.....##.....
............`),
  back: G(`
............
.....#......
....##......
...##.......
..##........
.##########.
.##########.
..##........
...##.......
....##......
.....#......
............`),
  backspace: G(`
............
............
...#########
..##.......#
.##..#...#.#
##....#.#..#
#......#...#
##....#.#..#
.##..#...#.#
..##.......#
...#########
............`),
};
const S = {
  shield: G(`
########
#......#
#.#..#.#
#..##..#
.#....#.
.#....#.
..#..#..
...##...`),
  heart: G(`
........
.##.##..
#######.
#######.
.#####..
..###...
...#....
........`),
  chevl: G(`
....##..
...##...
..##....
.##.....
.##.....
..##....
...##...
....##..`),
  chevr: G(`
..##....
...##...
....##..
.....##.
.....##.
....##..
...##...
..##....`),
  chevd: G(`
........
........
##....##
.##..##.
..####..
...##...
........
........`),
  repeat: G(`
..####.#
.#....##
#....###
#.......
.......#
###....#
##....#.
#.####..`),
  cursor: G(`
#.......
##......
###.....
####....
###.....
##......
#.......
........`),
  down: G(`
#######.
.#####..
..###...
...#....
........
........
........
........`),
  bang: G(`
...##...
...##...
...##...
...##...
...##...
........
...##...
...##...`),
};
const pi = (n, s = 24) => px(P[n], s);
const si = (n, s = 16, cls = '') => px(S[n], s, cls);

// The coin: three layers on a 14×14 grid (disc, outline, face), drawn at 56px = 4px pixels.
const coinSvg = (size = 56) => {
  const disc = G(`
....######....
..##########..
.############.
.############.
##############
##############
##############
##############
##############
##############
.############.
.############.
..##########..
....######....`);
  const ring = G(`
....######....
..##......##..
.#..........#.
.#..........#.
#............#
#............#
#............#
#............#
#............#
#............#
.#..........#.
.#..........#.
..##......##..
....######....`);
  const face = G(`
..............
..............
..............
......##......
......##......
......##......
...########...
...########...
......##......
......##......
......##......
..............
..............
..............`);
  const shine = G(`
..............
..............
...##.........
..#...........
..#...........
..............`);
  return `<svg class="coin" width="${size}" height="${size}" viewBox="0 0 14 14" shape-rendering="crispEdges" aria-hidden="true"><g fill="var(--coin)">${rects(disc)}</g><g fill="var(--shine)">${rects(shine)}</g><g fill="var(--coinI)">${rects(ring)}${rects(face)}</g></svg>`;
};

// A pixel-circle badge with the letter A: the CTA's action button.
const aBtn = `<span class="abtn" aria-hidden="true"><svg width="32" height="32" viewBox="0 0 8 8" shape-rendering="crispEdges">${rects(G(`
..####..
.######.
########
########
########
########
.######.
..####..`))}</svg><b>A</b></span>`;

// ---------- shared pieces ----------
const catIcon = { Food: 'utensils', Transport: 'car', Shopping: 'bag', Entertainment: 'film', Utilities: 'zap', Healthcare: 'heart', Education: 'book', Housing: 'house', Income: 'banknote' };
const navIcon = { Home: 'house', Accounts: 'wallet', Transactions: 'arrows', Analytics: 'chart', Settings: 'sliders' };
const acctIcon = { bank: 'bank', savings: 'savings', card: 'card' };

const ib = (n, label) => `<span class="ib" role="button" aria-label="${label}">${pi(n, 24)}</span>`;
const hd = (title, l = '', r = '') => `<header class="hd">${l}<div class="ti"><h1>${title}</h1></div><div class="rt">${r}</div></header>`;
const tabs = (active = 'Home') =>
  `<nav class="tabs">${D.nav.map(([n]) => `<span${n === active ? ' class="on"' : ''}>${pi(navIcon[n], 24)}<em>${n}</em></span>`).join('')}</nav>`;
const fab = `<span class="fab" role="button" aria-label="Add transaction">${coinSvg(56)}</span>`;

const roll = (s) =>
  s
    .split('')
    .map((ch) => {
      if (!/\d/.test(ch)) return `<span class="cm">${ch}</span>`;
      const n = ch === '0' ? 10 : Number(ch);
      return `<span class="dg"><span class="st" style="--n:${n}">${'01234567890'.split('').map((d) => `<i>${d}</i>`).join('')}</span></span>`;
    })
    .join('');

const hp = (pct, { danger = false, day = true, label = '' } = {}) => {
  const on = Math.round(pct / 5);
  let segs = '';
  for (let i = 0; i < 20; i++) segs += `<i class="${i < on ? 'f' : ''}" style="--i:${i}"></i>`;
  return `<div class="hp${danger ? ' dg' : ''}" role="img" aria-label="${label || pct + '% used'}">${si('heart', 16, 'hrt')}<div class="segs">${segs}${day ? `<span class="dm" style="left:${D.dayPct}%">${si('down', 16)}</span>` : ''}</div></div>`;
};

const tag = (t) => `<span class="tg${t === 'Essential' ? ' e' : ' d'}">${t}</span>`;
const row = (r) => `<div class="rw"><span class="it">${pi(catIcon[r.cat], 24)}</span><div class="nm"><b>${r.name}${r.rec ? si('repeat', 16, 'rec') : ''}</b><small>${r.cat} · ${r.acct}${r.tag ? ' · ' + tag(r.tag) : ''}</small></div><span class="a${r.inc ? ' inc' : ''}">${r.inc ? '+' : '−'}₹${r.amt}</span></div>`;
const slot = (a) => `<div class="aslot${a.owed ? ' owe' : ''}"><span class="si">${pi(acctIcon[a.kind], 24)}</span><small>${a.name}</small><b>${a.owed ? '−' : ''}₹${a.bal}</b></div>`;
const acc = (a) => `<div class="rw"><span class="it">${pi(acctIcon[a.kind], 24)}</span><div class="nm"><b>${a.name}</b><small>${a.type}</small></div><span class="a${a.owed ? ' neg' : ''}">${a.owed ? '−' : ''}₹${a.bal}</span></div>`;
const due = `<div class="due bx"><span class="wb">${si('bang', 16, 'blk')}<span>${D.due.days} days</span></span><p>${D.due.name} payment of <b class="amt">₹${D.due.amt}</b> is due in ${D.due.days} days</p></div>`;
const badge = `<p class="badge">${si('shield', 16)}Kept only on this device</p>`;

const hero = (wide = false) => `<section class="hero bx${wide ? ' w' : ''}">
  <div class="hl"><span class="lab">Safe to spend today</span><span class="lv">Day 15/31</span></div>
  <div class="fig" aria-label="₹${D.safe}"><span class="cm r">₹</span><span aria-hidden="true" class="rl">${roll(D.safe)}</span></div>
  <p class="sub"><span class="amt">₹${D.left}</span> left this month · ${D.days} days to go</p>
  <div class="hpw">
    ${hp(D.pct, { label: `Spent ₹${D.spent} of ₹${D.budget}, ${D.pct}% used, day 15 of 31` })}
    <div class="hpl"><span>Spent <span class="amt">₹${D.spent}</span> of <span class="amt">₹${D.budget}</span></span><span class="pc"><span class="amt">${D.pct}%</span> used</span></div>
  </div>
</section>`;

// ---------- screens ----------
const home = `
<div class="pg">
  <header class="hd hud"><div class="av" aria-hidden="true">A</div><div class="ti"><h1 class="nm0">${D.user}</h1>${badge}</div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero()}
  ${due}
  <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
  <div class="inv">${D.accounts.map(slot).join('')}</div>
  <div class="tot"><span>Total balance</span><b>₹${D.total}</b></div>
  <div class="sh"><h2>Latest</h2><a>See all</a></div>
  <div class="lst bx">${row(D.groups[0].rows[1])}</div>
</div>
${fab}${tabs('Home')}`;

const txns = `
<div class="pg">
  ${hd('Transactions', '', ib('search', 'Search') + ib('eye', 'Hide amounts'))}
  <div class="ty"><span class="on">${si('cursor', 16, 'cur')}All</span><span>Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="mo"><span class="ib s">${si('chevl', 16)}</span><b>October 2026</b><span class="ib s">${si('chevr', 16)}</span></div>
  <div class="lst bx">${D.groups.map((g) => `<div class="dl">${g.date}</div>${g.rows.map(row).join('')}`).join('')}</div>
</div>
${fab}${tabs('Transactions')}`;

const keys = ['7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', 'bk'];
const add = `
<div class="pg">
  ${hd('Add transaction', ib('back', 'Back'), '')}
  <div class="ty t3"><span class="on">${si('cursor', 16, 'cur')}Expense</span><span>Income</span><span>Transfer</span></div>
  <div class="amtb"><small>Amount</small><div class="fig"><span class="r">₹</span>2,450<i class="caret"></i></div></div>
  <div class="np">${keys.map((k) => (k === 'bk' ? `<span class="k" aria-label="Delete">${pi('backspace', 24)}</span>` : `<span class="k${k === '0' ? ' dn' : ''}">${k}</span>`)).join('')}</div>
  <div class="fl bx"><span class="it">${pi('bank', 24)}</span><div><small>Account</small><b>HDFC Checking</b></div><span class="ft">Bank account</span>${si('chevd', 16)}</div>
  <div class="sh"><h2>Category</h2><a>Split</a></div>
  <div class="cg">${D.cats.map(([n], k) => `<span class="c${k === 0 ? ' on' : ''}">${k === 0 ? si('cursor', 16, 'cur blk') : ''}${pi(catIcon[n], 24)}<em>${n}</em></span>`).join('')}</div>
</div>
<div class="cta"><button>${aBtn}<span>Add transaction</span></button></div>`;

const budgets = `
<div class="pg">
  ${hd('Budgets', ib('back', 'Back'), ib('eye', 'Hide amounts') + ib('plus', 'Add budget'))}
  ${D.budgets
    .map((b) => {
      const dg = b.pct > 85;
      return `<section class="bu bx${dg ? ' dgr' : ''}">
    <div class="bh"><h3>${b.name}</h3>${dg ? `<span class="nl">${si('bang', 16, 'blk')}${b.state}</span>` : `<span class="ok">${b.state}</span>`}</div>
    <small class="per">Monthly · October 2026</small>
    ${hp(b.pct, { danger: dg, day: false, label: `${b.name}: ₹${b.spent} of ₹${b.limit}, ${b.pct}% used, ${b.state}` })}
    <div class="bf"><span><span class="amt">₹${b.spent}</span> of <span class="amt">₹${b.limit}</span></span><b class="pc"><span class="amt">${b.pct}%</span> used</b></div>
    <div class="bl"><span class="amt">₹${b.left}</span> left this month</div>
  </section>`;
    })
    .join('')}
</div>
${tabs('Settings')}`;

const desk = `
<aside class="sd">
  <div class="logo">${coinSvg(28)}Finio</div>
  <span class="addb">${aBtn}<span>Add transaction</span></span>
  ${D.nav.map(([n]) => `<span class="ni${n === 'Home' ? ' on' : ''}">${pi(navIcon[n], 24)}${n}</span>`).join('')}
  <div class="gl">Tools</div>
  ${D.tools.map((t) => `<span class="ni t">${t}</span>`).join('')}
</aside>
<main class="mn">
  <header class="hd hud"><div class="av" aria-hidden="true">A</div><div class="ti"><h1 class="nm0">${D.user}</h1>${badge}</div><div class="rt">${ib('eye', 'Hide amounts')}${ib('sliders', 'Settings')}</div></header>
  ${hero(true)}
  ${due}
  <div class="cols">
    <div>
      <div class="sh"><h2>Where it sits</h2><a>See all</a></div>
      <div class="lst bx">${D.accounts.map(acc).join('')}<div class="tot in"><span>Total balance</span><b>₹${D.total}</b></div></div>
      <div class="sh"><h2>Savings goals</h2><a>See all</a></div>
      <div class="lst bx">${D.goals
        .map((g) => `<div class="qs"><div class="qh"><b><span class="q">Quest</span>${g.name}</b><span class="pc amt">${g.pct}%</span></div>${hp(g.pct, { day: false, label: `${g.name}: ₹${g.have} of ₹${g.target}, ${g.pct}%` }).replace(/<svg[^>]*class="ic px hrt"[\s\S]*?<\/svg>/, '')}<small><span class="amt">₹${g.have}</span> / <span class="amt">₹${g.target}</span></small></div>`)
        .join('')}</div>
    </div>
    <div>
      <div class="sh"><h2>Latest</h2><a>See all</a></div>
      <div class="lst bx">${D.groups.flatMap((g) => g.rows).map(row).join('')}</div>
    </div>
  </div>
</main>`;

export const css = `
.app{
  --bg:#e0efbe;--panel:#e0efbe;--ink:#0f380f;--mut:#306230;--line:#0f380f;--line2:#306230;
  --hl:#9bbc0f;--hlI:#0f380f;--fill:#0f380f;--empty:#9bbc0f;
  --pos:#306230;--neg:#a01641;--warn:#306230;--coin:#9bbc0f;--coinI:#0f380f;--shine:#e0efbe;
  --cta:#0f380f;--ctaI:#e0efbe;--abtn:#9bbc0f;--abtnI:#0f380f;--grid:rgba(15,56,15,.045);
  background:var(--bg);color:var(--ink);font-family:'Pixelify Sans',system-ui,sans-serif;font-weight:400;font-size:15px;
  font-variant-numeric:tabular-nums;-webkit-font-smoothing:antialiased;caret-color:var(--ink);
  background-image:linear-gradient(var(--grid) 1px,transparent 1px),linear-gradient(90deg,var(--grid) 1px,transparent 1px);background-size:3px 3px}
html[data-mode=dark] .app{
  --bg:#0d0b1e;--panel:#18143a;--ink:#f6edd3;--mut:#bdb5e2;--line:#f6edd3;--line2:#4f6bd8;
  --hl:#3a4fb8;--hlI:#f6edd3;--fill:#7be36f;--empty:#2c2760;
  --pos:#7be36f;--neg:#ff5c72;--warn:#ffcf3f;--coin:#ffcf3f;--coinI:#5a3a00;--shine:#fff6c8;
  --cta:#ffcf3f;--ctaI:#0d0b1e;--abtn:#ff5c72;--abtnI:#0d0b1e;--grid:transparent}
html[data-mode=dark] .app::after{content:'';position:absolute;inset:0;pointer-events:none;z-index:40;background:repeating-linear-gradient(0deg,rgba(0,0,0,.16) 0 1px,transparent 1px 3px)}
.app ::selection{background:var(--hl);color:var(--hlI)}
.app :focus-visible{outline:3px solid var(--line2);outline-offset:3px}
.app .px{image-rendering:pixelated}
.app h1,.app .logo{-webkit-font-smoothing:none}
/* figures: VT323 — the clearest pixel numerals (5/3/8 never collide) */
.app .fig,.app .k,.app .rw .a,.app .aslot b,.app .tot b,.app .amt{font-family:'VT323',monospace;font-weight:400;-webkit-font-smoothing:none}
.app .amt{font-size:20px;line-height:1;vertical-align:-.06em;letter-spacing:.02em}.app .aslot b.amt,.app .qs .pc.amt{font-size:21px}
.app .pg{position:absolute;inset:44px 0 76px;padding:6px 18px;overflow:hidden}
.app .pg:has(~ .cta){bottom:0}
/* the dialog box: notched stepped corners + inner line */
.bx{background:var(--panel);margin:3px;box-shadow:0 -3px 0 0 var(--line),0 3px 0 0 var(--line),-3px 0 0 0 var(--line),3px 0 0 0 var(--line),inset 0 0 0 3px var(--panel),inset 0 0 0 5px var(--line2)}
.hd{display:flex;align-items:center;gap:10px;padding:8px 0 12px}
.hd .ti{flex:1;min-width:0}
.hd h1{font-size:28px;font-weight:700;line-height:1.1;letter-spacing:0}
.hd h1.nm0{font-size:22px}
.badge{display:inline-flex;gap:6px;align-items:center;font-size:13px;white-space:nowrap;color:var(--mut);margin-top:3px;line-height:1}
.hd .rt{display:flex;gap:8px}
.av{width:42px;height:42px;display:grid;place-items:center;font-size:24px;font-weight:700;background:var(--hl);color:var(--hlI);margin:3px;-webkit-font-smoothing:none;
  box-shadow:0 -3px 0 0 var(--line),0 3px 0 0 var(--line),-3px 0 0 0 var(--line),3px 0 0 0 var(--line)}
.ib{width:38px;height:38px;display:grid;place-items:center;color:var(--ink);background:var(--panel);margin:2px;
  box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line),0 5px 0 0 var(--line)}
.ib.s{width:32px;height:32px}
/* hero = score counter */
.hero{padding:16px 18px 16px}
.hl{display:flex;justify-content:space-between;align-items:center;gap:8px}
.lab{font-size:16px;font-weight:500}
.lv{font-family:'Silkscreen',monospace;font-size:11px;line-height:1;padding:5px 7px 4px;background:var(--ink);color:var(--bg)}
.fig{font-size:76px;font-weight:400;line-height:1;margin:8px 0 6px;display:flex;align-items:flex-start;letter-spacing:0}
.fig .r{font-size:.55em;margin:.14em .1em 0 0}
.rl{display:inline-flex}
.dg{display:inline-block;height:1em;overflow:hidden}
.dg i{display:block;height:1em;line-height:1em;font-style:normal;text-align:center}
.st{display:block;transform:translateY(calc(var(--n) * -1em));animation:roll calc(var(--n) * 110ms) steps(var(--n)) 250ms backwards}
.cm{line-height:1em}
@keyframes roll{from{transform:translateY(0)}}
.sub{font-size:15px;color:var(--mut);margin-bottom:14px}
.hp{display:flex;align-items:center;gap:8px}
.hp .hrt{color:var(--ink)}
.hp.dg .hrt{color:var(--neg);animation:blink .8s steps(1) infinite}
.segs{flex:1;display:flex;gap:2px;height:16px;position:relative;padding:2px;margin:2px;background:var(--panel);
  box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line)}
.segs i{flex:1;background:var(--empty)}
.segs i.f{background:var(--fill);animation:seg 1ms steps(1) backwards;animation-delay:calc(400ms + var(--i) * 45ms)}
.hp.dg .segs i.f{background:var(--neg)}
.hp.dg .segs i:not(.f){background:repeating-linear-gradient(135deg,var(--neg) 0 2px,var(--empty) 2px 5px)}
@keyframes seg{from{background:var(--empty)}}
.dm{position:absolute;top:-14px;margin-left:-8px;color:var(--ink);line-height:0}
.hpw{padding-top:10px}
.hpl{display:flex;justify-content:space-between;font-size:14px;margin-top:8px}
.hpl .pc,.bf .pc{font-weight:700}
@keyframes blink{70%{opacity:0}}
.blk{animation:blink 1s steps(1) infinite}
/* due = warning bubble */
.due{margin-top:12px;display:flex;align-items:center;gap:12px;padding:12px 14px;font-size:15px;line-height:1.3}
.due p{flex:1}.due b{font-weight:700}
.wb{position:relative;display:inline-flex;align-items:center;gap:2px;flex:none;background:var(--ink);color:var(--bg);font-family:'Silkscreen',monospace;font-size:11px;padding:6px 8px 6px 4px;line-height:1}
.wb::after{content:'';position:absolute;right:-6px;top:50%;margin-top:-3px;border:3px solid transparent;border-left-color:var(--ink);border-right:0;width:0;height:0}
html[data-mode=dark] .app .wb{background:var(--warn);color:#0d0b1e}
html[data-mode=dark] .app .wb::after{border-left-color:var(--warn)}
.sh{display:flex;justify-content:space-between;align-items:baseline;margin:14px 2px 7px}
.sh h2{font-size:17px;font-weight:700}
.sh a{font-size:14px;color:var(--mut);text-decoration:underline;text-decoration-thickness:2px;text-underline-offset:3px}
/* accounts as inventory slots */
.inv{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:0 3px}
.aslot{display:flex;flex-direction:column;gap:4px;padding:10px 9px;background:var(--panel);margin:2px 0;
  box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line),inset 0 -4px 0 0 var(--hl)}
.aslot small{font-size:13px;color:var(--mut);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.aslot b{font-size:21px;line-height:1}
.aslot.owe b{color:var(--neg)}
.tot{display:flex;justify-content:space-between;align-items:baseline;margin:10px 4px 0;font-size:15px}
.tot b{font-size:28px;line-height:1}
.tot.in{margin:4px 0 0;padding:10px 4px 2px;border-top:2px dashed var(--line2)}
/* rows */
.lst{padding:6px 14px}
.rw{display:flex;align-items:center;gap:12px;padding:9px 0}
.rw + .rw{border-top:2px dotted var(--line2)}
.it{width:36px;height:36px;flex:none;display:grid;place-items:center;background:var(--hl);color:var(--hlI)}
.rw .nm{flex:1;min-width:0}
.rw .nm b{display:flex;align-items:center;gap:6px;font-weight:500;font-size:16px;line-height:1.25}
.rec{color:var(--mut)}
.rw small{display:block;font-size:13px;color:var(--mut);margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.rw .a{font-size:22px;white-space:nowrap;line-height:1}
.a.inc{color:var(--pos)}.a.neg{color:var(--neg)}
.tg{display:inline-block;font-size:12px;line-height:1;padding:2px 4px;color:var(--ink);border:2px solid var(--line2)}
.tg.d{border-style:dashed;color:var(--warn)}
.dl{font-family:'Silkscreen',monospace;font-size:11px;color:var(--mut);padding:12px 0 2px}
.dl:first-child{padding-top:6px}
/* tab bar */
.tabs{position:absolute;left:0;right:0;bottom:0;height:76px;background:var(--panel);display:flex;justify-content:space-around;align-items:flex-start;padding:9px 4px 0;box-shadow:0 -3px 0 0 var(--line),0 -6px 0 0 var(--panel),0 -8px 0 0 var(--line2)}
.tabs span{display:flex;flex-direction:column;align-items:center;gap:3px;color:var(--mut);padding:5px 4px 4px;min-width:60px}
.tabs em{font-style:normal;font-size:12px;line-height:1}
.tabs span.on{background:var(--ink);color:var(--bg)}
.tabs span.on em{font-weight:700}
.fab{position:absolute;right:20px;bottom:92px;width:56px;height:56px;display:grid;place-items:center;z-index:5;filter:drop-shadow(0 4px 0 var(--line))}
.coin{display:block;animation:spin 2.4s steps(1) infinite}
@keyframes spin{0%{transform:scaleX(1)}64%{transform:scaleX(.64)}72%{transform:scaleX(.14)}80%{transform:scaleX(.64)}88%{transform:scaleX(1)}}
/* type filter */
.ty{display:flex;gap:6px;margin-bottom:10px}
.ty span{position:relative;flex:1;text-align:center;font-size:15px;padding:8px 0;color:var(--mut)}
.ty span.on{background:var(--ink);color:var(--bg);font-weight:700;padding-left:12px}
.ty .cur{position:absolute;left:6px;top:50%;margin-top:-8px}
.mo{display:flex;justify-content:space-between;align-items:center;padding:4px 2px 10px;font-size:18px}
.mo b{font-weight:700}
/* add */
.ty.t3{margin:0 3px 4px}
.amtb{text-align:center;padding:4px 0 6px}
.amtb small{font-size:14px;color:var(--mut)}
.amtb .fig{justify-content:center;font-size:68px;margin:4px 0 0;align-items:center}
.amtb .r{color:var(--mut);margin-top:0}
.caret{display:inline-block;width:6px;height:46px;background:var(--ink);margin-left:6px;animation:blink 1s steps(1) infinite}
.np{display:grid;grid-template-columns:repeat(3,1fr);gap:10px 12px;padding:4px 4px 12px}
.k{height:46px;display:grid;place-items:center;font-size:32px;background:var(--panel);color:var(--ink);margin:2px;
  box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line),0 5px 0 0 var(--line),inset 0 -3px 0 0 var(--hl);transition:transform 60ms steps(1)}
.k:active,.k.dn{transform:translateY(2px);box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line),0 3px 0 0 var(--line);background:var(--hl);color:var(--hlI)}
.fl{display:flex;align-items:center;gap:12px;padding:10px 14px}
.fl div{flex:1}.fl small{display:block;font-size:13px;color:var(--mut)}.fl b{font-weight:700;font-size:17px}
.ft{font-size:13px;color:var(--mut)}
.add .sh,.pg .sh{margin-top:16px}
.cg{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.c{position:relative;display:flex;flex-direction:column;align-items:center;gap:6px;padding:9px 0 8px;color:var(--mut)}
.c em{font-style:normal;font-size:12px;line-height:1}
.c.on{background:var(--hl);color:var(--hlI);box-shadow:0 -2px 0 0 var(--line),0 2px 0 0 var(--line),-2px 0 0 0 var(--line),2px 0 0 0 var(--line)}
.c.on em{font-weight:700}
.c .cur{position:absolute;left:3px;top:12px}
.cta{position:absolute;left:0;right:0;bottom:0;padding:12px 20px 30px;background:var(--panel);box-shadow:0 -3px 0 0 var(--line)}
.cta button{width:100%;height:56px;border:0;border-radius:0;background:var(--cta);color:var(--ctaI);font-size:19px;font-weight:700;display:flex;align-items:center;justify-content:center;gap:12px;-webkit-font-smoothing:none;
  box-shadow:0 -3px 0 0 var(--cta),0 3px 0 0 var(--cta),-3px 0 0 0 var(--cta),3px 0 0 0 var(--cta),0 7px 0 0 var(--line2)}
.cta button:active{transform:translateY(2px)}
.abtn{position:relative;width:32px;height:32px;display:grid;place-items:center;flex:none}
.abtn svg{position:absolute;inset:0;fill:var(--abtn)}
.abtn b{position:relative;color:var(--abtnI);font-size:18px;font-weight:700;line-height:1}
/* budgets */
.bu{padding:12px 16px 12px;margin-bottom:12px}
.bh{display:flex;justify-content:space-between;align-items:center}
.bh h3{font-size:19px;font-weight:700}
.ok{font-family:'Silkscreen',monospace;font-size:11px;color:var(--mut)}
.nl{display:inline-flex;align-items:center;gap:2px;font-family:'Silkscreen',monospace;font-size:11px;line-height:1;background:var(--neg);color:#fff;padding:4px 7px 4px 2px}
html[data-mode=dark] .app .nl{color:#0d0b1e}
.per{display:block;font-size:13px;color:var(--mut);margin:1px 0 8px}
.bf{display:flex;justify-content:space-between;font-size:14px;margin-top:8px}
.bl{font-size:13px;color:var(--mut);margin-top:2px}
.bu.dgr .pc{color:var(--neg)}
.bu.dgr{box-shadow:0 -3px 0 0 var(--neg),0 3px 0 0 var(--neg),-3px 0 0 0 var(--neg),3px 0 0 0 var(--neg),inset 0 0 0 3px var(--panel),inset 0 0 0 5px var(--neg)}
/* desktop */
.sd{position:absolute;left:0;top:0;bottom:0;width:236px;background:var(--panel);padding:26px 20px;display:flex;flex-direction:column;gap:2px;box-shadow:3px 0 0 0 var(--line),6px 0 0 0 var(--panel),8px 0 0 0 var(--line2);z-index:2}
.logo{font-size:26px;font-weight:700;display:flex;align-items:center;gap:10px;margin-bottom:22px;-webkit-font-smoothing:none}
.logo .coin{animation:none}
.addb{display:flex;align-items:center;gap:10px;white-space:nowrap;background:var(--cta);color:var(--ctaI);font-weight:700;font-size:15px;padding:7px 8px;margin:3px 3px 22px;
  box-shadow:0 -3px 0 0 var(--cta),0 3px 0 0 var(--cta),-3px 0 0 0 var(--cta),3px 0 0 0 var(--cta),0 7px 0 0 var(--line2)}
.addb .abtn{width:28px;height:28px}.addb .abtn b{font-size:15px}
.ni{position:relative;display:flex;align-items:center;gap:12px;padding:7px 10px;color:var(--mut);font-size:16px}
.ni.on{background:var(--ink);color:var(--bg);font-weight:700}
.ni.t{padding:5px 10px;font-size:15px}
.gl{font-family:'Silkscreen',monospace;font-size:11px;color:var(--mut);margin:20px 10px 4px}
.mn{position:absolute;left:236px;right:0;top:0;bottom:0;padding:14px 48px 14px 56px;overflow:hidden}
.mn .hd{padding:0 0 10px}
.hero.w{display:grid;grid-template-columns:auto 1fr;gap:2px 56px;align-items:end;padding:12px 24px 14px}
.hero.w .hl{grid-column:1/-1}
.hero.w .fig{font-size:84px;grid-column:1;margin:4px 0 2px}
.hero.w .sub{grid-column:1;grid-row:3;margin:0}
.hero.w .hpw{grid-column:2;grid-row:2/4;padding-bottom:4px}
.mn .due{margin-top:12px;padding:8px 14px}
.cols{display:grid;grid-template-columns:1fr 1fr;gap:0 28px}
.mn .sh{margin:12px 2px 6px}
.mn .rw{padding:6px 0}
.mn .lst{padding:4px 14px}
.qs{padding:4px 0}
.qs + .qs{border-top:2px dotted var(--line2)}
.qh{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:6px}
.qh b{font-size:15px;font-weight:500;display:flex;gap:8px;align-items:baseline}
.q{font-family:'Silkscreen',monospace;font-size:11px;color:var(--mut)}
.qs .pc{font-size:15px;font-weight:700}
.qs .segs{height:12px}
.qs small{display:block;font-size:13px;color:var(--mut);margin-top:4px}
`;

export const screens = { home, txns, add, budgets, desk };
