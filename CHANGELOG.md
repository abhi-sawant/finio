# Changelog

Finio follows [Semantic Versioning](https://semver.org). The repo-root `VERSION` file is the single
source of truth for both clients; see "Releasing" in [CLAUDE.md](CLAUDE.md).

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
