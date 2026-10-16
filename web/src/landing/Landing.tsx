import { useEffect, useRef, useState } from 'react';
import { Download, Monitor, Smartphone } from 'lucide-react';

/**
 * The marketing page, shown to first-time visitors in a plain browser tab (see ./mode.ts).
 *
 * It is prerendered into index.html at build time, so it must render identically on the server
 * and the first client pass: no `window`, no storage, no randomness during render. Styles are in
 * ./landing.css and imported from main.tsx, so they are in the render-blocking stylesheet and
 * the prerendered markup is never unstyled. Screenshots are real captures of the app running on
 * its built-in sample data (`public/landing/`).
 */

interface InstallPromptEvent extends Event {
  prompt: () => Promise<void>;
}

const NOTES = [
  { d: '100', name: 'Bank account' },
  { d: '500', name: 'Savings' },
  { d: '2000', name: 'Credit card' },
  { d: '200', name: 'FD or RD' },
  { d: '10', name: 'Cash' },
  { d: '50', name: 'Wallet' },
  { d: '20', name: 'Investment' },
] as const;

const INSIDE: ReadonlyArray<[string, string]> = [
  [
    'Budgets that roll over',
    'Weekly, monthly or yearly, by category or label, with what you overspent carried forward.',
  ],
  [
    'Recurring and EMIs',
    'Rent, salary and subscriptions file themselves. Loans post their EMI every month.',
  ],
  [
    'FD and RD growth',
    'Accrued value computed from the actual terms, matured automatically into the linked account.',
  ],
  [
    'Forecast and net worth',
    'See where cash will stand at month end, and your net worth trend over the year.',
  ],
  [
    'Goals, and money lent',
    'Track savings goals and who owes whom, without touching real balances until you settle up.',
  ],
  [
    'SMS and statements',
    'Share a payment SMS to Finio, or import a bank statement CSV. Rules categorise as you go.',
  ],
];

function Shot({ name, alt, w, h }: { name: string; alt: string; w: number; h: number }) {
  return (
    <>
      <img
        className="lp-lt"
        src={`/landing/${name}-light.webp`}
        width={w}
        height={h}
        alt={alt}
        loading="lazy"
        decoding="async"
      />
      <img
        className="lp-dk"
        src={`/landing/${name}-dark.webp`}
        width={w}
        height={h}
        alt={alt}
        loading="lazy"
        decoding="async"
      />
    </>
  );
}

export function Landing({ onOpenApp }: { onOpenApp: () => void }) {
  const [installEvent, setInstallEvent] = useState<InstallPromptEvent | null>(null);
  const noteRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onPrompt = (e: Event) => {
      e.preventDefault();
      setInstallEvent(e as InstallPromptEvent);
    };
    window.addEventListener('beforeinstallprompt', onPrompt);
    return () => window.removeEventListener('beforeinstallprompt', onPrompt);
  }, []);

  const open = (e: React.MouseEvent) => {
    e.preventDefault();
    onOpenApp();
  };

  const install = (e: React.MouseEvent) => {
    if (installEvent) {
      e.preventDefault();
      void installEvent.prompt();
    }
    // Otherwise the href scrolls to the install steps.
  };

  // The banknote's colour-shift thread follows the pointer, as on the app's own hero note.
  const tilt = (e: React.PointerEvent<HTMLDivElement>) => {
    const el = noteRef.current;
    if (!el || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width - 0.5;
    const y = (e.clientY - r.top) / r.height - 0.5;
    el.style.setProperty('--shift', `${x * 140}deg`);
    el.style.setProperty('--ry', `${x * 5}deg`);
    el.style.setProperty('--rx', `${-y * 4}deg`);
  };
  const untilt = () => {
    const el = noteRef.current;
    if (!el) return;
    for (const p of ['--shift', '--rx', '--ry']) el.style.setProperty(p, '0deg');
  };

  return (
    <div className="lp">
      <header className="lp-nav">
        <div className="lp-wrap">
          <a className="lp-brand" href="/" onClick={(e) => e.preventDefault()}>
            <img src="/favicon.svg" alt="" width="32" height="32" />
            Finio
          </a>
          <a className="lp-link" href="#privacy">
            Privacy
          </a>
          <a className="lp-link" href="#app">
            The app
          </a>
          <a className="lp-link" href="#inside">
            What&rsquo;s inside
          </a>
          <a className="lp-btn lp-btn-primary lp-btn-sm" href="/" onClick={open}>
            Open app
          </a>
        </div>
      </header>

      <main>
        <section className="lp-hero">
          <div className="lp-wrap">
            <h1 className="lp-rise">Your money, on your device.</h1>
            <p className="lp-lede lp-rise" style={{ '--i': 1 } as React.CSSProperties}>
              Finio is a rupee ledger that never asks for your bank login, an account, or your
              data. Everything you record stays on your phone, and it works offline from the first
              tap.
            </p>
            <div className="lp-cta lp-rise" style={{ '--i': 2 } as React.CSSProperties}>
              <a className="lp-btn lp-btn-primary" href="/" onClick={open}>
                Open Finio
              </a>
              <a className="lp-btn lp-btn-outline" href="#install" onClick={install}>
                <Download aria-hidden="true" />
                Install the app
              </a>
            </div>
            <p className="lp-fine lp-rise" style={{ '--i': 3 } as React.CSSProperties}>
              Free. No sign-up. Try it with sample data first.
            </p>

            <div className="lp-stage lp-rise" style={{ '--i': 3 } as React.CSSProperties}>
              <div
                ref={noteRef}
                className="lp-banknote"
                role="img"
                aria-label="The Finio dashboard: safe to spend today, ₹1,562"
                onPointerMove={tilt}
                onPointerLeave={untilt}
              >
                <span className="lp-rosette" />
                <span className="lp-thread" />
                <div className="lp-note-body">
                  <span className="lp-label">Safe to spend today</span>
                  <span className="lp-big">₹1,562</span>
                  <div className="lp-bar">
                    <i style={{ width: '38%' }} />
                  </div>
                  <p className="lp-sub">₹25,000 left this month · 16 days to go</p>
                </div>
                <div className="lp-microprint" aria-hidden="true">
                  {'FINIO · ON YOUR DEVICE · NO ACCOUNT · NO TRACKING · '.repeat(4)}
                </div>
              </div>
              <span className="lp-pin lp-p1">Stays on your device</span>
              <span className="lp-pin lp-p2">No account behind it</span>
              <span className="lp-pin lp-p3">Zero tracking</span>
            </div>
          </div>
        </section>

        <section id="privacy">
          <div className="lp-wrap">
            <h2 className="lp-narrow">Built like a banknote. Checked the same way.</h2>
            <div className="lp-checks">
              <div className="lp-check">
                <div className="lp-specimen lp-sp-thread" aria-hidden="true">
                  <span className="lp-thread" />
                  <span className="lp-thread" />
                  <span className="lp-thread" />
                </div>
                <h3>Thread: it stays on your phone</h3>
                <p>
                  Every account, transaction and budget lives in your browser&rsquo;s storage.
                  Switch off the internet and nothing changes.
                </p>
              </div>
              <div className="lp-check">
                <div className="lp-specimen lp-sp-mark" aria-hidden="true">
                  <span className="lp-rosette" />
                </div>
                <h3>Watermark: no account behind it</h3>
                <p>
                  There is no sign-up and no login. Accounts, budgets, loans, goals and reports all
                  work without one.
                </p>
              </div>
              <div className="lp-check">
                <div className="lp-specimen lp-sp-micro" aria-hidden="true">
                  <span>
                    NO ANALYTICS NO TELEMETRY NO THIRD-PARTY SCRIPTS NO ANALYTICS NO TELEMETRY NO
                    THIRD-PARTY SCRIPTS
                  </span>
                  <span>
                    ZERO TRACKING ZERO TRACKING ZERO TRACKING ZERO TRACKING ZERO TRACKING ZERO
                    TRACKING ZERO TRACKING
                  </span>
                  <span>
                    NO ANALYTICS NO TELEMETRY NO THIRD-PARTY SCRIPTS NO ANALYTICS NO TELEMETRY NO
                    THIRD-PARTY SCRIPTS
                  </span>
                </div>
                <h3>Microprint: nothing in the fine print</h3>
                <p>
                  No analytics, no telemetry, no third-party scripts. Cloud backup is optional, and
                  can be encrypted with a passphrase only you know.
                </p>
              </div>
            </div>
          </div>
        </section>

        <section id="app" className="lp-flush">
          <div className="lp-wrap">
            <h2 className="lp-narrow">This is the app, not a mock-up.</h2>
            <p className="lp-lede lp-mt">
              Real screens, captured from Finio running on its built-in sample data. Light or
              dark, on a phone or a desktop.
            </p>
            <div className="lp-window">
              <div className="lp-window-bar" aria-hidden="true">
                <i />
                <i />
                <i />
              </div>
              <Shot name="desktop" alt="The Finio dashboard on desktop" w={1920} h={1204} />
            </div>
            <div className="lp-phones">
              <figure className="lp-device">
                <Shot
                  name="accounts"
                  alt="Accounts, each printed as its own note"
                  w={780}
                  h={1688}
                />
              </figure>
              <figure className="lp-device lp-crop">
                <Shot
                  name="budgets"
                  alt="Budgets with progress and what is left"
                  w={780}
                  h={1688}
                />
              </figure>
              <figure className="lp-device">
                <Shot name="add" alt="Adding an expense on the number pad" w={780} h={1688} />
              </figure>
            </div>
          </div>
        </section>

        <section id="accounts" className="lp-flush">
          <div className="lp-wrap">
            <div className="lp-head">
              <h2>Every account type is printed as its own note.</h2>
              <p>
                Bank accounts, cards, cash, wallets, FDs and RDs each keep the colour of the rupee
                note they resemble, so you know which is which before you read the name.
              </p>
            </div>
            <div className="lp-strip">
              {NOTES.map((n) => (
                <div
                  key={n.d}
                  className="lp-note"
                  style={{ backgroundImage: `var(--note-${n.d})`, color: `var(--note-${n.d}-ink)` }}
                >
                  <i
                    className="lp-chip"
                    style={{
                      backgroundImage: `var(--note-${n.d})`,
                      color: `var(--note-${n.d}-ink)`,
                    }}
                  />
                  <b>{n.name}</b>
                  <small>₹{n.d} note</small>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="inside" className="lp-flush">
          <div className="lp-wrap">
            <div className="lp-head lp-head-gap">
              <h2>Everything a monthly review needs.</h2>
              <p>
                Made for Indian money: rupees, UPI, EMIs, FD and RD, and salary cycles that start on
                the 25th.
              </p>
            </div>
            <div className="lp-ledger">
              {INSIDE.map(([name, text]) => (
                <div key={name} className="lp-row">
                  <b>{name}</b>
                  <span>{text}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section id="install" className="lp-flush lp-finale">
          <div className="lp-wrap">
            <div className="lp-panel">
              <div className="lp-panel-copy">
                <h2>Open it. There&rsquo;s nothing to sign up for.</h2>
                <p className="lp-lede">
                  Start on the sample data or your own accounts. Install it when you&rsquo;re ready,
                  and it opens like any other app.
                </p>
                <div className="lp-cta">
                  <a className="lp-btn lp-btn-primary" href="/" onClick={open}>
                    Open Finio
                  </a>
                </div>
              </div>
              <ol className="lp-steps">
                <li>
                  <Smartphone aria-hidden="true" />
                  <span>
                    <b>Android:</b> menu → Add to Home screen → Install
                  </span>
                </li>
                <li>
                  <Smartphone aria-hidden="true" />
                  <span>
                    <b>iPhone:</b> Share → Add to Home Screen
                  </span>
                </li>
                <li>
                  <Monitor aria-hidden="true" />
                  <span>
                    <b>Desktop:</b> the install icon in Chrome or Edge&rsquo;s address bar
                  </span>
                </li>
              </ol>
            </div>
          </div>
        </section>
      </main>

      <footer className="lp-footer">
        <div className="lp-wrap">
          <span className="lp-brand">
            <img src="/favicon.svg" alt="" width="24" height="24" />
            Finio
          </span>
          <span>Made in India, kept on your device.</span>
          <nav>
            <a href="/privacy">Privacy</a>
            <a href="/terms">Terms</a>
          </nav>
        </div>
      </footer>
    </div>
  );
}
