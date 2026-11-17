// Capture the real web app in one visual theme, both modes, as a contact sheet.
//
//   node design-prototypes/themes/src/app-shot.mjs <theme> [port=5173] [routes…]
//
// Needs the Vite dev server running (`npm run dev` in web/) and Playwright's headless shell in
// ~/Library/Caches/ms-playwright. Seeds the onboarding sample data once (cached next to the
// output), then writes .impeccable/review/app/<theme>-light.png and -dark.png: four phones
// (Home, Transactions, Add transaction, Budgets by default) over the desktop Home. Pass routes
// to capture other pages on the phones instead, e.g. `/accounts /goals /settings /analytics`.
import { spawn, execSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs';
import { tmpdir, homedir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const [theme, portArg, ...routeArgs] = process.argv.slice(2);
if (!theme) throw new Error('usage: app-shot.mjs <theme> [port] [routes…]');
const port = Number(portArg) || 5173;
const routes = routeArgs.length ? routeArgs : ['/', '/transactions', '/add-transaction', '/budgets'];
const origin = `http://localhost:${port}`;
const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, '../../../.impeccable/review/app');
mkdirSync(out, { recursive: true });
const seedFile = join(out, 'seed.json');
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const bin = execSync(`ls -d ${homedir()}/Library/Caches/ms-playwright/chromium_headless_shell-*/*/*shell* | tail -1`).toString().trim();
const profile = mkdtempSync(join(tmpdir(), 'finio-shot-'));
const chrome = spawn(bin, ['--headless', '--remote-debugging-port=0', `--user-data-dir=${profile}`, '--hide-scrollbars', '--force-device-scale-factor=1', 'about:blank'], { stdio: 'ignore' });
const done = () => { chrome.kill(); try { rmSync(profile, { recursive: true, force: true }); } catch { /* ignore */ } };
process.on('exit', done);

let dbg;
for (let i = 0; i < 100 && !dbg; i++) {
  await sleep(100);
  const f = join(profile, 'DevToolsActivePort');
  if (existsSync(f)) dbg = readFileSync(f, 'utf8').split('\n')[0];
}
const target = await (await fetch(`http://127.0.0.1:${dbg}/json/new?about:blank`, { method: 'PUT' })).json();
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => (ws.onopen = r));
let id = 0;
const pending = new Map();
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
};
const send = (method, params = {}) => new Promise((res, rej) => {
  const n = ++id;
  pending.set(n, (m) => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
  ws.send(JSON.stringify({ id: n, method, params }));
});
const evaluate = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description ?? r.exceptionDetails.text);
  return r.result.value;
};
const goto = async (url, settle = 1200) => {
  await send('Page.navigate', { url });
  for (let i = 0; i < 100; i++) {
    await sleep(100);
    if ((await evaluate('document.readyState').catch(() => '')) === 'complete') break;
  }
  await evaluate('document.fonts.ready.then(() => 1)').catch(() => 0);
  await sleep(settle);
};
const size = (width, height, mobile) => send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile });
const shot = async () => (await send('Page.captureScreenshot', { format: 'png' })).data;

await send('Page.enable');
await send('Runtime.enable');
await size(390, 844, true);

if (!existsSync(seedFile)) {
  await goto(`${origin}/privacy`, 300);
  await evaluate(`(async () => {
    const { loadSampleData } = await import('/src/data/sampleData.ts');
    const { useFinanceStore } = await import('/src/store/useFinanceStore.ts');
    const s = useFinanceStore.getState();
    loadSampleData({ addAccount: s.addAccount, addGoal: s.addGoal, addPerson: s.addPerson, addBudget: s.addBudget,
      addRecurring: s.addRecurring, addContribution: s.addContribution, addDebtEntry: s.addDebtEntry,
      bulkAddTransactions: s.bulkAddTransactions });
    useFinanceStore.getState().updateSettings({ userName: 'Asha', onboardedAt: new Date().toISOString() });
    await new Promise((r) => setTimeout(r, 300));
    return 1;
  })()`);
  // Open the app once so overdue recurring rules post now, not as a toast in every capture.
  await goto(`${origin}/`, 1500);
  writeFileSync(seedFile, await evaluate(`localStorage.getItem('finio-storage')`));
}
const seed = JSON.parse(readFileSync(seedFile, 'utf8'));

for (const mode of ['light', 'dark']) {
  seed.state.settings.theme = mode;
  await goto(`${origin}/privacy`, 100);
  await evaluate(`localStorage.setItem('finio-storage', ${JSON.stringify(JSON.stringify(seed))});
    localStorage.setItem('finio-visual-theme', ${JSON.stringify(JSON.stringify({ state: { visualTheme: theme }, version: 1 }))}); 1`);
  const phones = [];
  await size(390, 844, true);
  for (const r of routes) { await goto(origin + r); phones.push(await shot()); }
  await size(1280, 800, false);
  await goto(origin + '/');
  const desk = await shot();
  // Lay the captures out on one sheet and capture that.
  await size(1680, 1750, false);
  await goto('about:blank', 0);
  await evaluate(`document.body.style.cssText='margin:0;background:#17171b;padding:20px;display:flex;flex-wrap:wrap;gap:20px;align-content:flex-start';
    document.body.innerHTML=${JSON.stringify(phones.map((p) => `<img width=390 height=844 src="data:image/png;base64,${p}">`).join('') + `<img width=1280 height=800 src="data:image/png;base64,${desk}">`)};
    Promise.all([...document.images].map((i) => i.decode())).then(() => 1)`);
  const file = join(out, `${theme}-${mode}.png`);
  writeFileSync(file, Buffer.from(await shot(), 'base64'));
  console.log(file);
}
ws.close();
done();
process.exit(0);
