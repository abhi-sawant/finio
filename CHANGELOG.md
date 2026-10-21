# Changelog

Finio follows [Semantic Versioning](https://semver.org). The repo-root `VERSION` file is the single
source of truth for both clients; see "Releasing" in [CLAUDE.md](CLAUDE.md).

## 3.0.3 — 2026-10-21

### Added

- **Transactions mode.** If you only want to log what you earn and spend, pick "Transactions" in
  onboarding or Settings. Finio then records transactions without keeping account balances: totals,
  card dues, the forecast and net worth are hidden, and onboarding skips the opening-balance step.
  Switching back to full tracking asks what each account holds today and sets the books to match,
  without posting any adjustment. Fixed and recurring deposits still post their transactions. The new
  `trackingMode` setting is part of the backup format; see [spec/backup-format.md](spec/backup-format.md).
  Older backups import unchanged.

### Fixed

- **Fixed and recurring deposits no longer appear in the Add Transaction account picker.** They are
  funded and paid out by the deposit flow, so they can't be picked, or be the default, by hand. A
  transaction already on one keeps it.

## 3.0.2 — 2026-10-19

### Added

- **Five new starter labels:** Reimbursable, Business, One-time, Planned and Family. They appear on
  new installs and after a reset to defaults; labels you already have are left alone.

### Changed

- **The "For" field is gone from Add Transaction.** The "For Self" and "For Others" labels cover the
  same need. A "For" value in an older backup is ignored on import. The backup format otherwise is
  unchanged; see [spec/backup-format.md](spec/backup-format.md).

## 3.0.1 — 2026-10-16

### Changed

- **Back to Finio.** The 3.0.0 rename to Monilock is undone: the name, logo, install icon,
  notifications, legal pages and home page all say Finio again. Your data, backups, settings and
  installed copy carry over untouched, and the Android update installs over 3.0.0. The first-time
  home page and the Privacy Policy / Terms access from 3.0.0 stay. The backup format is unchanged.

## 3.0.0 — 2026-10-16

### Changed

- **Privacy Policy and Terms (web)** can now be read before you have set the app up.

### Added

- **A home page for first-time visitors (web).** Opening the site in a browser before you have set
  the app up now shows a Finio home page that explains what the app is, with real screenshots,
  instead of dropping straight into setup. If you have already set the app up, or you open the
  installed app, nothing changes: you go straight to your data. The page is readable by search
  engines without JavaScript.

### Self-hosters

- The home page, `robots.txt`, `sitemap.xml` and the page's structured data in `web/index.html` name
  `https://finio.slowatcoding.com/`. If you host the web app on your own domain, update those URLs.

### Backup format

- Unchanged. See [spec/backup-format.md](spec/backup-format.md).

## 2.0.4 — 2026-10-08

### Fixed

- **Smoother screen changes (Android).** Switching between screens no longer flashes a loading
  spinner in the top half of the screen or makes the page jump. Pages now open with their content
  already positioned below the header, screens you have just visited show their data immediately,
  and the outgoing screen no longer shows through the incoming one for a frame.

## 2.0.3 — 2026-10-08

### Added

- **Merchant and For on transactions (both clients).** Add Transaction has two new optional fields:
  **Merchant** (who the money went to or came from) and **For** (who it was for, e.g. "Mom"), both
  with suggestions drawn from your history. A transaction with no note shows its merchant as the
  row title, search matches both fields, and duplicating a transaction carries them over.
- **In-app update prompt (Android).** On every app open Finio checks GitHub for the latest release
  and, if it is newer than the installed version, shows its changelog with **Update** (opens the
  release page to download the APK), **Not now** (asks again next open) and **Skip this version**
  (stays quiet until a newer release is published). The PWA already updates itself, so it has no prompt.

### Fixed

- The selected category tile's ring and shadow are no longer clipped by the category grid's scroll
  area on either client.

### Backup format

- `merchant` and `forWhom` are new optional transaction fields, omitted when blank. Backups without
  them import unchanged, and older apps ignore them. See [spec/backup-format.md](spec/backup-format.md).

## 2.0.2 — 2026-10-07

### Changed

- **New typefaces on both clients.** Titles and headline amounts now use Familjen Grotesk, and body
  text uses Instrument Sans, replacing Unbounded and Geist. Colours, layout and data are unchanged.
  No backup-format changes.

## 2.0.1 — 2026-10-07

### Added

- **AMOLED dark mode** on both clients. Turn on "Use AMOLED colors in dark mode" in Settings →
  Profile & preferences for a true-black background that switches pixels off on OLED screens.
  Surfaces stay readable through a lavender hairline and a faint wash instead of shadows; hues and
  semantic colours are unchanged. It only applies while the theme resolves to dark.
- `amoledDark` is a new optional setting in the backup format (default `false`), so the choice
  travels in backups between the web and Android apps. See [spec/backup-format.md](spec/backup-format.md).

## 2.0.0 — 2026-10-07

The first versioned release: Finio is now one product on two clients, the PWA and a native Android
app, maintained in one monorepo.

### Added

- **Native Android app** (`android/`, Kotlin + Jetpack Compose, Android 8.0+). It's a screen-for-screen
  port of the PWA with the same features, the same "Mudra" look in light and dark, and the same data
  model:
  - Dashboard, accounts (including FD/RD deposits and statement reconciliation), transactions
    (search, filters, bulk actions, splits, CSV export), analytics with every chart, Year in Review,
    merchants, budgets, recurring rules, goals, debts with settle-up, loans with an amortization
    schedule and a prepayment calculator, categories, labels, auto-categorization rules, and CSV
    import.
  - App lock with a PIN and optional fingerprint/face unlock.
  - Reminders that fire while the app is closed (via WorkManager).
  - Daily auto-backup to a folder you choose.
  - Opt-in cloud backup, with the same end-to-end encryption as the web app.
  - Share an SMS or text into Finio to pre-fill a transaction.
  - Launcher shortcuts for Add Expense, Add Income, Transactions and Budgets.
- **Cross-client backups.** A backup exported by either app restores in the other, and both use the
  same cloud backups, encrypted or not. The format is specified in
  [spec/backup-format.md](spec/backup-format.md).
- **Version shown in Settings** on both clients.

### Changed

- **Monorepo layout:** the PWA moved to `web/`, the backend stays in `backend/`, the Android app is
  in `android/`, and the shared contract is in `spec/`. Self-hosters should build from `web/` and
  deploy `web/dist/`.
- On Android, recurring transactions, deposit maturities, net-worth snapshots and reminders are
  processed every time the app returns to the foreground, not only at launch.

### Quality

- **The money logic matches across clients exactly.** About 10,700 golden test cases are generated
  by running the web's TypeScript logic, and the Android port must reproduce every one. They cover:
  balances, periods, budgets with rollover, recurring rules, loans, deposits, forecasts, insights,
  CSV parsing, backup validation, reminder ids, and PIN and backup crypto (byte-for-byte).

### Fixed

- `npm run build` failed on a type error in a test fixture file.

## 1.x

These releases were unversioned: the PWA only, before the monorepo.
