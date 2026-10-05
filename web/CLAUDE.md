# Finio Web (PWA) — web/CLAUDE.md

The React PWA. Run every command below from `web/`. Repo-wide context — the domain model, the
cross-app gotchas, the backend and the backup contract both apps share — is in the
[root CLAUDE.md](../CLAUDE.md); read it first. Visual system: [design.md](../design.md).

## Key Commands

```bash
# Development
npm run dev          # Start Vite dev server (default port 5173)

# Production
npm run build        # tsc -b && vite build → dist/
npm run preview      # Serve the dist/ build locally

# Code Quality
npm run lint         # ESLint — 1 known pre-existing error (CategoryIcon.tsx:114, react-refresh)
npm run format       # Prettier (with tailwindcss plugin)
npm run format:check # Prettier, check only

# Tests
npm test             # Vitest (unit suite, run once)
npm run test:watch   # Vitest watch mode
```

Tests live next to their subject as `*.test.ts` — 637 of them across 29 files — and cover the
pure money logic (`src/store/balance.ts`, `src/store/recurring.ts`, `src/utils/calculations.ts`,
`src/utils/period.ts`, `src/utils/importValidation.ts`, `src/utils/csvImport.ts`,
`src/utils/autoCategorize.ts`, `src/utils/analytics.ts`, `src/utils/forecast.ts`,
`src/utils/netWorth.ts`, `src/utils/insights.ts`, `src/utils/loan.ts`, `src/utils/deposit.ts`, `src/utils/merchants.ts`,
`src/utils/notifications.ts`, `src/utils/notificationSchedule.ts`, `src/utils/shareTarget.ts`,
`src/utils/pinCrypto.ts`, `src/utils/appLock.ts`, `src/utils/backupCrypto.ts`,
`src/utils/chartTable.ts`, `src/utils/formatters.ts`, `src/utils/errors.ts`) plus the finance,
app-lock and backup-crypto stores, and the deterministic onboarding sample dataset
(`src/data/sampleData.ts`). Config is in `vitest.config.ts` — separate from `vite.config.ts` and
running in the `node` environment, so no browser plugins are loaded.

That `node` environment is a real constraint: `include` matches **`.test.ts` only**, there is no
jsdom and no setup file, so `window`, `Notification` and IndexedDB do not exist. Anything
platform-facing has to be split into a pure module that is tested and a thin I/O wrapper that
isn't (`notificationDb`/`notificationRunner`, `appLockBiometric`, `LockScreen`). Node _does_
expose `crypto.subtle`, which is why `pinCrypto.ts` is fully testable.

---


## Environment Variables

Copy `.env.example` to `.env` and set:

```
VITE_API_URL=https://api.yourdomain.com
```

If `VITE_API_URL` is not set, the API client defaults to `https://api.finio.slowatcoding.com`. All `VITE_*` variables are inlined at build time by Vite.

---


## Architecture

### Frontend (src/)

**Offline-first.** The app stores all finance data in localStorage. Cloud backup is an optional feature, not a requirement for the app to function.

```
src/
├── main.tsx                  # React root, wraps in ThemeProvider
├── App.tsx                   # BrowserRouter, gates (lock → onboarding), lazy routes, Sonner
├── pages/                    # Route-level components (all lazy-loaded)
│   ├── legal/                # PrivacyPolicy, TermsOfService — static, linked from Settings' footer
│   └── auth/                 # Login, Register, VerifyOtp, ForgotPassword, ResetPassword
├── components/
│   ├── ui/                   # shadcn/ui primitives + confirm(), switch, number-pad, Header/Main page shell
│   ├── charts/                # Recharts wrappers + ChartDataTable (a11y fallback)
│   ├── analytics/             # Analytics-page cards (forecast, net worth, heatmap, insights, top merchants)
│   ├── applock/               # LockScreen + PinPad (rendered instead of the app while locked)
│   ├── onboarding/             # First-run wizard (name → account → opening balance → sample data)
│   ├── accounts/ budgets/ categories/ goals/ people/ transactions/   # Per-domain cards & icons
│   │   └── accounts/ReconcileAccountDialog.tsx  # Per-account statement reconciliation (see gotchas)
│   ├── layout/                # Layout.tsx (bottom tabs + FAB), Sidebar.tsx, navItems.ts
│   ├── settings/              # Settings-page sections: Profile, Notifications, AppLock, CloudAccount, Backup, SecretDialogShell
│   ├── ErrorBoundary.tsx      # Top-level crash boundary, above BrowserRouter
│   ├── HideAmountsToggle.tsx  # Eye/eye-off button in every data-bearing page header
│   └── ThemeProvider.tsx      # dark/light/system theme context
├── sw/sw.ts                  # Hand-written service worker (own TS project — see PWA below)
├── hooks/
│   ├── useAutoLock.ts        # visibilitychange/pagehide → re-lock after the grace window
│   └── useLongPress.ts       # Pointer-based long-press (transaction rows, template FAB)
├── store/
│   ├── useFinanceStore.ts    # All finance data + actions (Zustand + localStorage)
│   ├── balance.ts            # Pure balance math: deltas, opening-balance backfill, recompute
│   ├── recurring.ts          # Pure recurring planner (planRecurring, nextDueDate, previewBackfill)
│   ├── useAuthStore.ts       # Token, user profile, lastBackupAt (Zustand + localStorage)
│   ├── useAppLockStore.ts    # PIN hash, auto-lock delay, isLocked (key `finio-lock`)
│   └── useBackupCryptoStore.ts # Backup passphrase config + in-memory session key
├── services/
│   ├── api.ts                # Typed fetch wrapper, Bearer token injection
│   ├── backup.ts             # Cloud upload/download (E2EE) + local JSON export/import
│   ├── backupFolder.ts       # File System Access API folder handle for local auto-backups
│   ├── download.ts           # downloadBlob() — object-URL file download, shared by backup/CSV export
│   ├── notifications.ts      # DOM-side: permission, schedule refresh, periodic sync
│   ├── notificationDb.ts     # IndexedDB schedule + fired ledger (shared with the SW)
│   ├── notificationRunner.ts # Shows what's due — called by the app *and* the worker
│   ├── appLockSession.ts     # backgroundedAt timestamp (localStorage, survives a page kill)
│   └── appLockBiometric.ts   # WebAuthn platform authenticator (convenience unlock)
├── types/index.ts            # All domain interfaces (Account, Transaction, Budget, Loan, etc.)
├── utils/
│   ├── calculations.ts       # Financial aggregations, budget status/history, CSV export
│   ├── period.ts             # Weekly/monthly/yearly period math (honours monthStartDay)
│   ├── importValidation.ts   # Backup shape validation + dry-run report
│   ├── csvImport.ts          # Bank-statement CSV parsing + column mapping
│   ├── autoCategorize.ts     # Pure rule engine: note pattern → category + labels
│   ├── analytics.ts          # Period-over-period comparison, spending calendar grid, Year in Review
│   ├── forecast.ts           # Liquid cash-flow projection (recurring + category averages)
│   ├── netWorth.ts           # Net worth series, reconstruction, and monthly snapshots
│   ├── insights.ts           # Insights feed + subscription detection + normalizeNote() (shared with merchants.ts)
│   ├── loan.ts                # Pure EMI amortization schedule + prepayment "what if" calculator
│   ├── deposit.ts             # Pure FD/RD valuation, maturity planner, delete-blocker check
│   ├── merchants.ts           # Groups transactions by note into merchant summaries (heuristic, not an entity)
│   ├── notifications.ts      # Reminder types + due-selection (leaf — imported by the SW)
│   ├── notificationSchedule.ts # Pure builder: bills, budget breaches, card dues, daily-log nudge → schedule
│   ├── shareTarget.ts        # Share Target / shortcut param parsing (SMS amount extraction)
│   ├── pinCrypto.ts          # PBKDF2 PIN hashing, salt, base64url
│   ├── appLock.ts            # shouldLockOnResume + failed-attempt backoff ladder
│   ├── backupCrypto.ts       # AES-GCM envelope + PBKDF2 key derivation for cloud backups
│   ├── chartTable.ts         # sampleForTable() — thins a long series for the data table
│   ├── validation.ts         # Input caps (MAX_NAME/NOTE/PATTERN_LENGTH), cleanText, isValidEmail, date-range/scope helpers
│   ├── backupMeta.ts         # BACKUP_SCHEMA_VERSION + withBackupMeta() — version/exportedAt on file exports only
│   ├── errors.ts             # getErrorMessage() — narrows a catch block's `unknown` to a message
│   └── formatters.ts         # Currency (INR), date, number formatting
├── lib/utils.ts              # shadcn cn() helper
└── data/
    ├── defaultData.ts        # Default categories (36), labels (9), and settings
    ├── colorPalette.ts       # COLOR_PALETTE — the single 18-swatch list every color picker offers
    └── sampleData.ts         # Deterministic demo dataset offered in onboarding (also a QA fixture)
```

Outside `src/`:

```
scripts/gen-dummydata.mjs     # Regenerates dummydata.json (seeded PRNG — reruns are reproducible)
scripts/gen-icons.mjs         # Renders the Mudra favicon/PWA/maskable/apple icons into public/ from one vector source
dummydata.json                # ~1000-transaction import fixture for load/QA testing; not used by the app
public/.htaccess              # SPA rewrite to index.html for Apache/cPanel
public/guilloche.svg          # Procedural rosette, masked behind every screen (body::before); generated, not hand-drawn
```

### State Management

Four Zustand stores, all persisted to localStorage:

- **`useFinanceStore`** (`finio-storage`) — accounts, transactions, categories, labels, budgets, recurring rules, templates, category rules, goals + contributions, people + debt entries, net-worth snapshots, loans + prepayments, settings. Exposes granular selector hooks (`useAccounts()`, `useTransactions()`, etc.) to avoid re-renders. Includes `processRecurring()` for generating due recurring transactions, `importData(payload, { mode })` for merge/replace restore, `captureNetWorthSnapshots()` for the monthly net-worth ledger, `applyRulesToExisting()` to replay categorization rules, `recomputeBalances()` to reconcile drift globally, and the loan lifecycle (`addLoan`/`updateLoan`/`setLoanClosed`/`deleteLoan`/`addLoanPrepayment`) which keeps a linked `RecurringTransaction` in sync with the recalculated EMI. Has migration support (currently v16).
- **`useAuthStore`** (`finio-auth`) — JWT token, user object, `lastBackupAt`. Use `loadAuth()` on app start to hydrate from storage.
- **`useAppLockStore`** (`finio-lock`) — PIN hash + salt, auto-lock delay, WebAuthn credential id, failed-attempt count, and the transient `isLocked`/`isReady` flags. **Separate from the finance store on purpose** — see the app-lock gotcha below. Uses `partialize` so the transient flags are never written, and decides the cold-start lock in `onRehydrateStorage` rather than an effect.
- **`useBackupCryptoStore`** (`finio-backup-crypto`) — `BackupCryptoConfig` (salt, iterations, passphrase verifier) plus the derived `CryptoKey`, which is **in-memory only** and never persisted. Separate from `Settings` for the same reason as the lock store — see the encrypted-backup gotcha below.

### Routing

React Router v7 (the `react-router` package — `react-router-dom` is not a dependency). All pages
are lazy-loaded (dynamic `import()`). **There is no auth guard on any route:** the app is
offline-first and works signed-out, so the old `ProtectedRoute` was deleted. What _does_ gate the
app is rendered above `<Routes>` in `App.tsx` — hydration → app lock → onboarding.

- `/` — Dashboard (index)
- `/accounts`, `/transactions`, `/analytics`, `/settings`, `/budgets`, `/recurring`
- `/add-transaction`, `/edit-transaction/:id`, `/add-account`, `/edit-account/:id`
- `/manage-categories`, `/manage-labels`, `/category-rules`
- `/goals`, `/debts`, `/import-csv`
- `/loans`, `/add-loan`, `/edit-loan/:id`
- `/merchants` — grouped view of who transactions went to/came from
- `/year-in-review` — annual look-back, year-offset navigable
- `/share-target` — Web Share Target; renders `AddTransaction`. **Must stay an explicit route**, or the `*` catch-all would redirect to `/` and drop the shared payload's query params.
- `/login`, `/register`, `/verify-otp`, `/forgot-password`, `/reset-password` — cloud-account routes
- `/privacy`, `/terms` — static legal pages, linked from Settings' footer
- `*` → redirects to `/`

Routes under the `<Layout>` element (Dashboard, Accounts, Transactions, Analytics, Settings) get
the bottom tab bar + FAB on mobile and the `Sidebar` on desktop; everything else is a full-screen
page.


## UI & Styling

- **"Mudra" visual system** (adopted 2026-10-04, replacing the "Focus" ledger look) — the app is
  printed like a rupee note: lavender→mint→peach "note paper" in light mode, an indigo field "under a
  UV lamp" in dark, frosted-glass cards, faint guilloche engraving, microprint and a windowed
  colour-shift thread. One accent, the ₹100 lavender (`--primary: #4b36c7`, `#b9adff` in dark). The
  full spec is [design.md](../design.md); every token lives in [`src/index.css`](src/index.css), each
  with a hand-tuned dark pair. Rules that are easy to break:
  - Components use semantic tokens (`bg-card`, `text-muted-foreground`, `bg-warning-band`…), never
    raw hex or ad-hoc Tailwind palette colours (`bg-amber-100`). Income and other "good" figures are
    `text-positive` (green), never `text-primary`; caution is `text-warning`.
  - `bg-grad-*` are real ₹100-note gradients now: `bg-grad-primary` (+ `shadow-glow-primary`,
    `text-white`) is the filled-action vocabulary and the default `Button` variant. The gradient
    stays deep indigo in **both** modes because call sites hardcode white text.
  - A card is `card-elevated rounded-md` (frosted glass: translucent fill, white hairline,
    `backdrop-filter`); a homogeneous list is **one** `card-elevated divide-y` container of plain
    rows. Sticky chrome (header once scrolled, tab bar, sidebar) is `glass-chrome`.
  - The hero figure on a page is a [`NoteCard`](src/components/ui/note-card.tsx) (Dashboard "safe to
    spend", Accounts "net balance"): guilloche, pointer-driven thread hue shift, microprint privacy
    band, UV fibres in dark. Don't nest cards inside it.
  - An account's tint is its **type's** rupee denomination ([`note.ts`](src/components/accounts/note.ts):
    bank ₹100 lavender, savings ₹500 stone, card ₹2000 magenta, FD/RD ₹200 yellow…), never its
    user colour — `noteStyle(type)` for tiles, `.note-chip` for list rows.
  - No uppercase tracked micro-labels or eyebrows above headings; labels are sentence-case
    `text-xs font-medium` muted.
  - The Mudra classes (`card-elevated`, `glass-chrome`, `bg-grad-*`, `shadow-glow-*`, `bg-coin`,
    `font-money`, `thread-fill`) are Tailwind `@utility`s, so variants work
    (`data-[selected=true]:bg-grad-primary`). Don't move them into `@layer utilities` — Tailwind v4
    silently drops variants on layer classes. Stock `shadow-*` utilities are re-tinted lavender.
  - Never give `#root` (or any wrapper around the `<Toaster>`) a z-index/stacking context — toasts
    must render above portalled dialogs. The background rosette is `body::before` at `z-index: -1`.
  - Full-screen routes (forms, Tools pages) sit inside `DesktopShell` in `App.tsx`, which keeps the
    fixed desktop Sidebar; a `fixed left-0 w-full` bar on those pages needs `lg:left-60`.
  - Every page is `<Header>` + `<Main>` from `src/components/ui/` (shared `max-w-5xl` width, and
    `Main`'s large mobile bottom padding keeps content clear of the tab bar/FAB). `Header` is
    transparent at rest and frosts on scroll; the desktop `Sidebar` is `fixed`, and the content
    column carries `lg:pl-60`.
  - Every colour picker offers `COLOR_PALETTE` from [`src/data/colorPalette.ts`](src/data/colorPalette.ts).
    Changing it only changes what pickers offer, never colours already saved on entities.
  - Fonts: Geist Variable for everything people read or type; **Unbounded Variable** (the banknote
    numeral) for `h1` page titles, dialog titles (`font-heading`) and headline money (`.font-money`).
    Row amounts stay Geist.
- **Tailwind CSS v4** — configured via `@tailwindcss/vite` plugin (no `tailwind.config.js`; directives in `index.css`).
- **shadcn/ui** with `base-nova` style, using `@base-ui/react` under the hood. Add new components with `npx shadcn@latest add <component>`.
- **Lucide React** for icons.
- **Sonner** for toast notifications (mounted in `App.tsx`).
- **Recharts** for all charts in the Analytics page. Every chart whose data lived only in the SVG is paired with [`ChartDataTable`](src/components/charts/ChartDataTable.tsx) — a real `<table>` behind a "View data table" disclosure.
- **@tanstack/react-virtual** for the virtualized transactions list.
- **papaparse** for bank-statement CSV parsing.
- Confirmations go through `useConfirm()` ([`src/components/ui/confirm.tsx`](src/components/ui/confirm.tsx)), never native `confirm()`. Toggles use `Switch`/`SwitchField` ([`src/components/ui/switch.tsx`](src/components/ui/switch.tsx)), never a hand-rolled `<span role="switch">`.

---


## PWA

Configured in [vite.config.ts](vite.config.ts) via `vite-plugin-pwa`, using **`strategies:
'injectManifest'`** with a hand-written worker at [`src/sw/sw.ts`](src/sw/sw.ts).

- App name: "Finio - Finance Tracker", manifest theme color `#4b36c7`; `index.html` sets a light/dark `theme-color` pair matching the note paper
- Manifest icons: 64px, 96px, 192px, 512px, maskable 512px (in `public/`)
- `shortcuts`: Add Expense, Add Income, Transactions, Budgets (96px icon each — capped at four,
  since Android surfaces 3–4 and silently drops the rest)
- `share_target`: `GET /share-target?title=&text=&url=` — GET is forced, not preferred:
  `public/.htaccess` rewrites to a static `index.html` and cannot take a POST body, and a POST
  target also needs the SW to already be controlling, which it isn't on a cold-start share
- Notification schedule and fired-reminder ledger live in IndexedDB (`finio-notifications`), the
  only storage the page and the worker can both reach
- `src/types/periodic-sync.d.ts` and `src/types/file-system-access.d.ts` are hand-written ambient
  typings for browser APIs TypeScript's DOM lib doesn't (yet) ship — Periodic Background Sync and
  the File System Access API (used by `backupFolder.ts` for local auto-backups)

**The worker is hand-written, so nothing is free.** `workbox.runtimeCaching`, `navigateFallback`,
`cleanupOutdatedCaches` and `clientsClaim` are all `generateSW`-only options that `injectManifest`
**ignores silently, with no error**. `sw.ts` writes each of them out; the one that matters most is
the `NavigationRoute` SPA fallback, without which offline deep-links to `/settings` or `/budgets` 404. `registerType: 'autoUpdate'` additionally requires `self.skipWaiting()` and `clientsClaim()`
literally present in the worker source or updates stall behind a waiting worker.

The worker needs the **WebWorker** lib while the app needs **DOM**, so it is its own TS project:
[`tsconfig.sw.json`](tsconfig.sw.json) is in the root `references` _and_ `src/sw` is excluded from
`tsconfig.app.json`. Missing either half breaks `npm run build`. The `workbox-*` runtime packages
are explicit devDependencies — they previously resolved only through npm hoisting `workbox-build`.

`virtual:pwa-register` is deliberately never imported: `tsconfig.app.json` declares only
`["vite/client"]` and there is no `vite-env.d.ts`, so it is a hard `tsc -b` break.
`navigator.serviceWorker.ready` covers every need. **The PWA is not built under `vite dev`** — use
`npm run build && npm run preview` (there is a `finio-preview` launch config).

---


## Code Splitting

Vite manual chunks defined in [vite.config.ts](vite.config.ts):

- `vendor-react` — react, react-dom, react-router
- `vendor-charts` — recharts + d3-\*
- `vendor-dates` — date-fns
- `vendor-icons` — lucide-react

All page components are lazy-loaded. This keeps the initial bundle small.

---


## Adding a New Feature — Checklist

1. **New type?** → add interface to `src/types/index.ts`
2. **New state?** → extend `useFinanceStore` (add state + actions + localStorage key); bump migration version if changing the persisted schema
3. **New page?** → create in `src/pages/`, add a lazy `import()` in `App.tsx`, wire up the route
4. **New API call?** → add a typed function to `src/services/api.ts`; if it's a new endpoint on the backend, register it in `backend/public/index.php` **with a rate-limit bucket**, or it ships unprotected
5. **New UI primitive?** → prefer `npx shadcn@latest add` over hand-rolling
6. **New calculation?** → add to `src/utils/calculations.ts` and export a named function
7. **New money logic?** → keep it pure and add a `*.test.ts` beside it. Anything touching `window`,
   `Notification` or IndexedDB has to be split into a tested pure module and an untested I/O
   wrapper — the suite runs in `node` (see Key Commands)
8. **New entity?** → also wire it into `services/backup.ts` (export + upload) and
   `utils/importValidation.ts`, or it silently drops out of every backup
9. **Changed persisted data or money logic?** → update `../spec/backup-format.md`, run
   `npm run gen:fixtures`, and port the change to `../android/` (see the root CLAUDE.md parity rules)

---


## Web-only gotchas

- **Tailwind v4:** There is no `tailwind.config.js`. All customizations go in CSS files using `@theme`, `@layer`, etc.
- **The service worker cannot read localStorage,** where all the finance data lives — see the root
  CLAUDE.md "Common Gotchas" for the reminder rules both apps share.
