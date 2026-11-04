// Neutral presentation stage shared by every theme prototype. Themes only own what is inside
// .phone > .screen and .desk > .screen — the stage never takes their colours.
export function status() {
  return `<div class="sb"><span>9:41</span><span class="sb-r"><svg width="17" height="11" viewBox="0 0 17 11" fill="currentColor"><rect x="0" y="7" width="3" height="4" rx="1"/><rect x="4.5" y="5" width="3" height="6" rx="1"/><rect x="9" y="2.5" width="3" height="8.5" rx="1"/><rect x="13.5" y="0" width="3" height="11" rx="1"/></svg><svg width="25" height="12" viewBox="0 0 25 12" fill="none" stroke="currentColor"><rect x=".5" y=".5" width="21" height="11" rx="3.5" opacity=".5"/><rect x="2" y="2" width="16" height="8" rx="2" fill="currentColor" stroke="none"/><path d="M23 4v4" stroke-linecap="round" opacity=".5"/></svg></span></div>`;
}

export function page({ num, name, tagline, fonts, css, screens, notes, defaultMode = 'light' }) {
  const cap = (t) => `<figcaption>${t}</figcaption>`;
  const phones = [
    ['Home', screens.home],
    ['Transactions', screens.txns],
    ['Add transaction', screens.add],
    ['Budgets', screens.budgets],
  ]
    .map(([t, html]) => `<figure class="slot"><div class="phone"><div class="screen app">${status()}${html}</div></div>${cap(t)}</figure>`)
    .join('');
  return `<!doctype html>
<html lang="en" data-mode="${defaultMode}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Finio theme ${num} — ${name}</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="${fonts}" rel="stylesheet">
<style>
*{box-sizing:border-box;margin:0;padding:0}
html{background:#17171b}
body{font-family:ui-sans-serif,-apple-system,"Segoe UI",system-ui,sans-serif;color:#e9e9ef;background:#17171b;min-height:100vh;padding:28px 32px 72px}
.stage-h{display:flex;flex-wrap:wrap;align-items:flex-end;justify-content:space-between;gap:16px;max-width:1700px;margin:0 auto 28px}
.stage-h a{color:#9a9aa8;font-size:13px;text-decoration:none}.stage-h a:hover{color:#fff}
.stage-h h1{font-size:28px;font-weight:600;letter-spacing:-.01em;margin-top:6px}
.stage-h p{color:#a9a9b6;font-size:15px;margin-top:4px;max-width:60ch}
.seg{display:inline-flex;background:#26262d;border-radius:999px;padding:3px}
.seg button{font:inherit;font-size:13px;color:#b9b9c6;background:none;border:0;border-radius:999px;padding:7px 16px;cursor:pointer}
.seg button[aria-pressed=true]{background:#e9e9ef;color:#17171b}
.notes{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:1px;background:#2a2a32;border:1px solid #2a2a32;border-radius:12px;overflow:hidden;max-width:1700px;margin:0 auto 40px}
.notes div{background:#1d1d23;padding:14px 16px}
.notes b{display:block;font-size:12px;font-weight:600;color:#fff;margin-bottom:4px}
.notes span{font-size:13px;color:#a9a9b6;line-height:1.5}
.row{display:flex;flex-wrap:wrap;gap:36px 32px;max-width:1700px;margin:0 auto;align-items:flex-start}
.slot{margin:0}
figcaption{color:#9a9aa8;font-size:13px;margin:12px 4px 0}
h2.sec{max-width:1700px;margin:48px auto 18px;font-size:13px;font-weight:600;color:#9a9aa8}
.phone{width:390px;height:844px;border-radius:48px;padding:0;background:#000;box-shadow:0 0 0 9px #26262d,0 30px 60px -20px #000;overflow:hidden;position:relative}
.desk{width:1240px;height:780px;border-radius:14px;overflow:hidden;box-shadow:0 0 0 1px #2e2e36,0 30px 60px -20px #000;position:relative}
.screen{position:relative;width:100%;height:100%;overflow:hidden}
.sb{position:absolute;top:0;left:0;right:0;height:44px;padding:14px 30px 0;display:flex;justify-content:space-between;font-size:15px;font-weight:600;z-index:30;pointer-events:none}
.sb-r{display:flex;gap:6px;align-items:center}
.ic{flex:none;display:block}
.screen button{font:inherit;color:inherit;cursor:pointer}
@media (prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}
/* ---------- theme ---------- */
${css}
</style>
</head>
<body>
<header class="stage-h">
  <div><a href="index.html">← All themes</a><h1>${num} · ${name}</h1><p>${tagline}</p></div>
  <div class="seg" role="group" aria-label="Colour mode"><button aria-pressed="${defaultMode === 'light'}" data-m="light">Light</button><button aria-pressed="${defaultMode === 'dark'}" data-m="dark">Dark</button></div>
</header>
<section class="notes">${notes.map(([k, v]) => `<div><b>${k}</b><span>${v}</span></div>`).join('')}</section>
<div class="row">${phones}</div>
<h2 class="sec">Desktop · sidebar layout</h2>
<div class="row"><figure class="slot"><div class="desk"><div class="screen app">${screens.desk}</div></div>${cap('Home, 1240 × 780')}</figure></div>
<script>
document.querySelectorAll('.seg button').forEach(function(b){b.addEventListener('click',function(){
  document.documentElement.dataset.mode=b.dataset.m;
  document.querySelectorAll('.seg button').forEach(function(x){x.setAttribute('aria-pressed',String(x===b))});
});});
</script>
</body>
</html>`;
}
