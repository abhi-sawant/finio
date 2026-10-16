/**
 * Which front door does this visit get — the marketing page or the app?
 *
 * The landing page is shown only to a first-time visitor in a plain browser tab, on `/`. Anyone
 * who has set the app up, anyone running the installed PWA, and any deep link (share target,
 * manifest shortcut, notification click, `/privacy`) goes straight to the app.
 *
 * `index.html` carries a hand-copied, ES5 version of this decision in its head script, so the
 * right half of the page can be hidden before first paint. Change the two together.
 */

/** sessionStorage flag: this tab chose "Open Finio", so a reload stays in the app. */
export const ENTERED_KEY = 'finio-entered';

/** The persist version that introduced `settings.onboardedAt` (see the store's `migrate`). */
const ONBOARDED_AT_VERSION = 6;

/**
 * Has this browser already been used as the app? Reads the raw `finio-storage` value rather than
 * the store, so the landing page never pulls the store (and the whole app) into its bundle.
 * Anything we cannot parse counts as "used": showing a marketing page to someone whose data is
 * sitting right there is the worse mistake.
 */
export function hasUsedApp(raw: string | null): boolean {
  if (raw === null) return false;
  try {
    const parsed = JSON.parse(raw) as {
      version?: number;
      state?: { settings?: { onboardedAt?: unknown } };
    } | null;
    if (!parsed || typeof parsed !== 'object') return true;
    if (parsed.state?.settings?.onboardedAt) return true;
    // States from before onboarding existed are backdated by the store's migration on load.
    return typeof parsed.version === 'number' && parsed.version < ONBOARDED_AT_VERSION;
  } catch {
    return true;
  }
}

export function isStandalone(): boolean {
  const nav = navigator as Navigator & { standalone?: boolean };
  return (
    nav.standalone === true ||
    ['standalone', 'fullscreen', 'minimal-ui'].some(
      (m) => window.matchMedia(`(display-mode: ${m})`).matches,
    )
  );
}

export interface LandingInputs {
  pathname: string;
  standalone: boolean;
  entered: boolean;
  usedApp: boolean;
}

export function shouldShowLanding({ pathname, standalone, entered, usedApp }: LandingInputs) {
  return pathname === '/' && !standalone && !entered && !usedApp;
}

/** Reads the real browser state. Storage can throw (blocked cookies); treat that as "no". */
export function detectLanding(): boolean {
  const read = (fn: () => string | null) => {
    try {
      return fn();
    } catch {
      return null;
    }
  };
  return shouldShowLanding({
    pathname: window.location.pathname,
    standalone: isStandalone(),
    entered: read(() => sessionStorage.getItem(ENTERED_KEY)) === '1',
    usedApp: hasUsedApp(read(() => localStorage.getItem('finio-storage'))),
  });
}

export function rememberEntered() {
  try {
    sessionStorage.setItem(ENTERED_KEY, '1');
  } catch {
    /* private mode: the in-memory swap still works, a reload just shows the landing again */
  }
}
