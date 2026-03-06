# Finio – Copilot Instructions

This file provides the coding AI agent (GitHub Copilot, Cursor, etc.) with a complete picture of the project so that suggestions are accurate, idiomatic, and architecturally consistent.

---

## Project Overview

**Finio** is a personal finance tracker mobile app (React Native / Expo) with an optional cloud backup feature powered by a self-hosted PHP backend.

- **App name in Expo**: `Finio`
- **App version**: `1.1.0`
- **App bundle ID**: `com.finio.app`
- **Backend API base URL**: configured via `EXPO_PUBLIC_API_URL` in `.env` (see `.env.example`); fallback is `https://api.finio.slowatcoding.com`
- **Target platforms**: Android (primary), iOS

---

## Monorepo Structure

```
/
├── app/                    Expo Router file-based navigation
│   ├── _layout.tsx         Root layout (fonts, splash, theme, auth gate, auto-backup, update check)
│   ├── (tabs)/             Bottom-tab screens (authenticated)
│   │   ├── _layout.tsx
│   │   ├── index.tsx       Dashboard
│   │   ├── transactions.tsx
│   │   ├── accounts.tsx
│   │   ├── analytics.tsx
│   │   └── settings.tsx
│   ├── auth/               Unauthenticated auth flow
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── verify-otp.tsx
│   │   ├── forgot-password.tsx
│   │   └── reset-password.tsx
│   └── modals/             Full-screen modal routes
│       ├── add-account.tsx
│       ├── add-transaction.tsx
│       ├── add-category.tsx
│       ├── manage-categories.tsx
│       ├── manage-labels.tsx
│       └── transaction-detail.tsx
├── components/             Reusable UI components
│   ├── accounts/
│   ├── categories/
│   ├── charts/             Custom SVG charts (react-native-svg)
│   ├── common/             AmountInput, BottomSheet, ColorPicker, DatePicker,
│   │                       EmptyState, IconPicker, LabelPicker, SkeletonLoader, Toast
│   ├── dashboard/          SummaryCards, RecentTransactions
│   ├── layout/             FAB, Header, TabBar
│   └── transactions/       TransactionFilters, TransactionItem, TransactionList
├── constants/
│   └── Colors.ts           DarkColors, LightColors, Colors (alias for DarkColors),
│                           AccountColors[], CategoryColors[], ColorPalette, ColorKey
├── data/
│   └── defaultData.ts      Seed data: 24 categories, 9 labels,
│                           settings (currency: 'INR', theme: 'system', userName: 'Alex')
│                           Accounts and transactions start empty ([]).
├── hooks/
│   ├── useColors.ts        Returns the active ColorPalette based on theme setting
│   ├── useCountUp.ts       Animated number counter (Reanimated)
│   ├── useDebounce.ts
│   └── useThemeColor.ts    Thin wrapper: useColors()[colorKey]
├── services/
│   ├── api.ts              Typed fetch wrapper for the PHP REST API
│   ├── backup.ts           uploadBackup / restoreLatestBackup / autoBackupIfNeeded
│   └── updater.ts          checkForUpdate / openReleasePage (GitHub Releases)
├── store/
│   ├── useFinanceStore.ts  Zustand store (persisted via AsyncStorage)
│   ├── useAuthStore.ts     Zustand store (token/user persisted via SecureStore)
│   └── selectors.ts        Derived selectors (totals, filtered lists, period helpers,
│                           balance trend, category spending, recent transactions)
├── types/
│   └── index.ts            All shared TypeScript interfaces & union types
├── utils/
│   ├── calculations.ts     Pure financial math helpers
│   ├── formatters.ts       Currency, date, number formatters
│   └── haptics.ts          Haptic feedback helpers
└── backend/                Self-hosted PHP 8.2+ REST API
    ├── composer.json
    ├── config.example.php  Template — copy to ~/finio-config/config.php on server
    ├── schema.sql          MySQL schema (users + backups tables)
    ├── SETUP_GUIDE.txt     Step-by-step cPanel deployment guide
    ├── public/
    │   └── index.php       Front controller / router
    └── src/
        ├── Config.php      Loads config from outside public_html
        ├── Database.php    Singleton PDO connection
        ├── Router.php      Lightweight request router
        ├── helpers.php     jwt_create, json_ok, json_error, send_mail, etc.
        ├── Middleware/
        │   └── AuthMiddleware.php
        └── Controllers/
            ├── AuthController.php   register, verifyOtp, resendOtp, login,
            │                        forgotPassword, resetPassword
            ├── BackupController.php upload, latest, list, download, delete
            └── UserController.php   me (GET/PUT/DELETE)
```

---

## Technology Stack

### Mobile (React Native)

| Area | Library / Tool |
|---|---|
| Framework | Expo SDK 54, Expo Router 6 (file-based navigation) |
| Language | TypeScript 5.9 (strict) |
| State management | Zustand 5 |
| Persistence | `@react-native-async-storage/async-storage` (finance data), `expo-secure-store` (JWT token) |
| Styling | NativeWind 4 (Tailwind CSS for RN) + `StyleSheet` for complex styles |
| Charts | Custom SVG charts via `react-native-svg` (Victory Native 41 is installed but unused) |
| Forms | React Hook Form 7 + Zod 4 validation |
| Animation | `react-native-reanimated` 4 + `react-native-worklets` |
| Lists | `SectionList` (RN core) for transactions; `@shopify/flash-list` installed but not currently used |
| HTTP | Native `fetch` (wrapped in `services/api.ts`) |
| Icons | `lucide-react-native` |
| Fonts | DM Sans (400R / 500M / 700B) + Sora (700B / 800EB) via `@expo-google-fonts` |
| Date handling | `date-fns` 4 |
| Haptics | `expo-haptics` |
| Notifications | `expo-notifications` |
| File I/O | `expo-file-system`, `expo-sharing`, `expo-document-picker` |
| Gradients | `expo-linear-gradient` |
| Navigation bar | `expo-navigation-bar` (Android only) |
| App info | `expo-constants` |
| Date picker | `@react-native-community/datetimepicker` |

### Backend (PHP)

| Area | Detail |
|---|---|
| Language | PHP 8.2+ |
| Database | MySQL (via PDO, prepared statements only) |
| Auth | JWT (HS256) + OTP email verification |
| Email | PHPMailer via SMTP |
| Hosting target | cPanel shared hosting |
| No framework | Vanilla PHP with a tiny custom router |

---

## Data Model (TypeScript types — `types/index.ts`)

```ts
AccountType     = 'checking' | 'savings' | 'cash' | 'credit' | 'investment' | 'wallet'
TransactionType = 'expense' | 'income' | 'transfer'
CategoryType    = 'expense' | 'income' | 'both'
Currency        = 'USD' | 'EUR' | 'GBP' | 'INR' | 'JPY' | 'CAD' | 'AUD'
Theme           = 'dark' | 'light' | 'system'

Account        { id, name, type, currency, color, icon, balance, createdAt }
Transaction    { id, type, amount, accountId, toAccountId?, categoryId,
                 date, note, labels, createdAt }
Category       { id, name, icon, color, type }
Label          { id, name, color }
Settings       { currency, theme, userName, useBiometrics, hapticFeedback, notifications }
MonthlySummary { month, year, income, expenses, net }
```

Default `settings.theme` is `'system'`.

---

## State Management

### `useFinanceStore` (Zustand + AsyncStorage persist)

The single source of truth for all financial data.

- **Persisted** under key `'finio-storage'` via AsyncStorage.
- Exposes CRUD actions for accounts, transactions, categories, labels.
- `addTransaction` / `updateTransaction` / `deleteTransaction` automatically adjust account balances (including transfer reversal logic).
- `deleteAccount` cascades and removes related transactions (both as `accountId` and `toAccountId`).
- `deleteCategory` only removes the category record — it does **not** scrub `categoryId` from existing transactions.
- `deleteLabel` removes the label ID from all transactions' `labels` array.
- `importData` is used for backup restore and local JSON import (merges; keeps existing values for fields not provided).
- `resetToDefaults` clears accounts and transactions to empty arrays and resets categories, labels, and settings to seed defaults.
- `generateUUID()` is a local UUID generator (Math.random-based); do not replace with an external package.

### `useAuthStore` (Zustand, manual SecureStore)

- Stores `token` (JWT), `user` (`{ id, name, email }`), `lastBackupAt`.
- `loadAuth` / `setAuth` / `clearAuth` / `setLastBackupAt` persist to `expo-secure-store`.
- `isLoaded` flag prevents rendering before hydration.

### Selectors (`store/selectors.ts`)

Pure selector functions — always import derived data from here, not inline.

Key exports:
- `getTotalBalance`, `getAccountById`, `getCategoryById`
- `filterTransactions` (type / accountId / categoryIds / date range / search query — note: search matches on `note` field only)
- `getMonthlySummary`, `getLast6MonthsSummaries`
- `getCategorySpending` → `CategorySpending[]` — expense-only, sorted desc by amount
- `getLabelSpending` → `LabelSpending[]` — expense-only, splits evenly across labels, top 8
- `getPeriodRange(period: PeriodKey)` — `PeriodKey = 'week' | 'month' | '3months' | '6months' | 'year'`
- `getRecentTransactions`
- `getBalanceTrend(txs, currentBalance, accountId, days)` — reconstructs per-account daily balance history (30-day default)

---

## Services

### `services/api.ts`

Typed `fetch` wrapper. All auth routes are public; backup and user routes require `Authorization: Bearer <token>`.

```ts
api.register(name, email, password)
api.verifyOtp(email, otp)          → LoginResult
api.resendOtp(email)
api.login(email, password)         → LoginResult
api.forgotPassword(email)
api.resetPassword(email, otp, password)
api.uploadBackup(token, data)
api.getLatestBackup(token)         → { data: Record<string, unknown> }
```

> **Note**: The remaining backend endpoints (`/backup/list`, `/backup/{date}` GET/DELETE, `/user/me` GET/PUT/DELETE) exist in the PHP router but do **not** have typed client-side wrappers in `api.ts` yet.

### `services/backup.ts`

- `uploadBackup()` — serialises the full store and uploads it; updates `lastBackupAt`.
- `restoreLatestBackup()` — downloads and imports latest cloud backup.
- `autoBackupIfNeeded()` — uploads if last backup was > 24 h ago; called silently on app start.

### `services/updater.ts`

- `checkForUpdate()` — fetches the latest GitHub Release (configured with `GITHUB_OWNER = 'abhi-sawant'` / `GITHUB_REPO = 'finio'`), compares semver against `Constants.expoConfig?.version`, and returns `ReleaseInfo | null`.
- `openReleasePage(url)` — opens the release URL in the device browser via `Linking.openURL`.
- Called in root `_layout.tsx` on startup; displays an `Alert.alert` (with "Later" / "Download" buttons) when a newer version exists. Release notes are truncated to 200 characters.

---

## Backend API

Base URL: `EXPO_PUBLIC_API_URL` env var (fallback: `https://api.finio.slowatcoding.com`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Create account, send OTP |
| POST | `/auth/verify-otp` | No | Verify OTP, receive JWT |
| POST | `/auth/resend-otp` | No | Resend OTP |
| POST | `/auth/login` | No | Login, receive JWT |
| POST | `/auth/forgot-password` | No | Send reset email |
| POST | `/auth/reset-password` | No | Reset with OTP token |
| GET | `/user/me` | Yes | Get profile |
| PUT | `/user/me` | Yes | Update profile |
| DELETE | `/user/me` | Yes | Delete account |
| POST | `/backup/upload` | Yes | Upload JSON backup |
| GET | `/backup/latest` | Yes | Fetch latest backup |
| GET | `/backup/list` | Yes | List all backups |
| GET | `/backup/{date}` | Yes | Download a specific backup |
| DELETE | `/backup/{date}` | Yes | Delete a specific backup |

JWT access tokens expire in **30 days**. All secrets live in `~/finio-config/config.php` (outside `public_html`).

---

## Navigation Architecture

Expo Router with three route groups:

1. **(tabs)** — custom `TabBar` component with four tabs (Dashboard, Transactions, Accounts, Analytics) + a centred FAB that opens the add-transaction modal. Settings is a fifth Tabs.Screen but accessed via a gear button in the Dashboard header, not a tab button.
2. **auth/** — stack for unauthenticated users (login, register, OTP, password reset)
3. **modals/** — full-screen modals pushed over tabs

The root `_layout.tsx`:
- Loads DM Sans + Sora fonts via `expo-font`.
- Waits for `isHydrated` (store) + `isLoaded` (auth) + fonts before hiding the splash screen.
- Syncs the Android navigation bar button style and background colour to the active theme via `expo-navigation-bar`.
- Calls `autoBackupIfNeeded()` once per app launch (errors are silently swallowed).
- Calls `checkForUpdate()` and shows an `Alert` when a newer GitHub Release is available.
- Redirects between auth and tabs based on `useAuthStore().token`.
- `modals/manage-categories` and `modals/manage-labels` are accessible via Expo Router's automatic routing but are **not** registered as explicit `Stack.Screen` entries.
- `<Toast />` is rendered inside `SafeAreaProvider` at the root level.

---

## Theming

The app supports three theme modes controlled by `settings.theme: Theme`.

| Mode | Description |
|---|---|
| `'dark'` | Always use `DarkColors` |
| `'light'` | Always use `LightColors` |
| `'system'` | Follow device `useColorScheme()` (default) |

**`useColors()`** (`hooks/useColors.ts`) reads `settings.theme` from the store plus `useColorScheme()` to resolve and return the correct `ColorPalette` at runtime. Use this in every component instead of importing `Colors` or `DarkColors` directly.

**`useThemeColor(key)`** is a convenience wrapper: `useColors()[key]`.

**`useCountUp(options)`** (`hooks/useCountUp.ts`) — animates a number from `from` to `to` using Reanimated `withTiming`. Defined but not currently used in the codebase (the Dashboard uses its own inline `AnimatedBalance` implementation).

**`useDebounce<T>(value, delay?)`** (`hooks/useDebounce.ts`) — standard debounce with configurable delay (default 300 ms).

**`constants/Colors.ts`** exports:
- `DarkColors` — dark palette object (`as const`)
- `LightColors` — light palette object (`as const`)
- `Colors` — alias for `DarkColors`; used only for static/non-component contexts (e.g. chart configs, colour arrays)
- `AccountColors` — 12-entry preset colour array for account pickers
- `CategoryColors` — 12-entry preset colour array for category pickers
- `LabelColors` — 6-entry preset colour array for label pickers
- `ColorPalette` — type alias for `typeof DarkColors`
- `ColorKey` — `keyof ColorPalette`

Key palette tokens: `background`, `surface`, `surfaceElevated`, `primary`, `primaryLight`, `primaryDark`, `income`, `expense`, `transfer`, `textPrimary`, `textMuted`, `textDisabled`, `border`, `borderStrong`, `white`, `black`, `success`, `warning`, `error`, `info`, `shadowColor`.

---

## Styling Conventions

- Use `useColors()` to get the active palette in every component; never hardcode colour hex values.
- Use NativeWind `className` props for spacing/layout when practical.
- Use `StyleSheet.create()` for anything that involves colours, shadows, or complex overrides — following the pattern `const styles = makeStyles(colors)` with a `function makeStyles(colors: ColorPalette)` factory at the bottom of the file.
- Do **not** import `Colors` or `DarkColors` directly inside component files.

---

## Code Conventions

- **Language**: TypeScript, strict mode with additional flags (`noUncheckedIndexedAccess`, `noImplicitReturns`, `exactOptionalPropertyTypes`). All new files must be `.ts` / `.tsx`.
- **No `any`** except where unavoidable; prefer `unknown` and narrowing.
- **Imports**: Use the `@/` path alias (maps to project root). Never use relative `../..` chains.
- **Components**: Functional components only. No class components.
- **Exports**: Named exports for components and utilities. Default export for screens/layouts.
- **ID generation**: Use the local `generateUUID()` in `useFinanceStore.ts`; do not add nanoid or uuid packages.
- **Dates**: Always store as ISO 8601 strings. Use `date-fns` for formatting/parsing.
- **Currency amounts**: Store as plain `number` (float). Formatting is done via `utils/formatters.ts`.
- **Forms**: Use `react-hook-form` + `zod` for new forms that modify store/API data (e.g., `AccountForm` follows this pattern). Simple modals with few fields may use `useState`, but prefer RHF+Zod for anything complex.
- **Error handling**: Surface errors via the `<Toast>` / `showToast()` helper, not `Alert.alert` (use `Alert.alert` only for destructive confirmation dialogs).

### PHP Backend Conventions

- Namespace: `Finio\` (PSR-4, `src/` as root).
- All DB queries use PDO prepared statements — never interpolate user input.
- All responses use `json_ok()` / `json_error()` helpers from `helpers.php`.
- Config is always accessed via `Config::get('key')` — never hardcode credentials.
- Passwords hashed with `PASSWORD_BCRYPT`, cost 12. OTPs hashed with SHA-256, never stored plain.

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `EXPO_PUBLIC_API_URL` | Yes (or use fallback) | Backend API base URL, no trailing slash |

Create `.env` from `.env.example`. The `.env` file is gitignored.

---

## Common Tasks for AI Agents

### Add a new transaction field
1. Add the field to `Transaction` in `types/index.ts`.
2. Update `addTransaction` / `updateTransaction` in `useFinanceStore.ts`.
3. Update the `add-transaction.tsx` modal form.
4. Update `TransactionItem.tsx` and `transaction-detail.tsx` display.
5. Update `backup/upload` PHP endpoint if the field needs to be persisted server-side.

### Add a new screen
1. Create the file under `app/(tabs)/` or `app/modals/`.
2. If it's a tab, add a `<Tabs.Screen>` entry in `app/(tabs)/_layout.tsx` and a button in `components/layout/TabBar.tsx`.

### Add a new API endpoint (PHP)
1. Add the route in `backend/public/index.php`.
2. Add the controller method in the appropriate `Controllers/*.php` file.
3. Add the typed `api.*` wrapper in `services/api.ts`.

### Add or change a theme colour
1. Add/update the key in both `DarkColors` and `LightColors` in `constants/Colors.ts`.
2. The `ColorPalette` and `ColorKey` types are derived automatically.

### Run the app
```bash
npx expo start          # Expo Go or dev build
npx expo run:android    # Native Android build
```

### Backend deployment
Follow `backend/SETUP_GUIDE.txt` for full cPanel deployment instructions.

---

## What NOT to Do

- Do not introduce new state management libraries. Use Zustand.
- Do not add a CSS-in-JS library (styled-components, emotion). Use NativeWind + StyleSheet.
- Do not store credentials, tokens, or secrets in the source code or `app.json`.
- Do not add `axios`. The project uses native `fetch` via `services/api.ts`.
- Do not commit `.env`, `backend/config.php`, or any file with real credentials.
- Do not use `console.log` in production code paths; remove debug logging before committing.
- Do not add a new icon library. Use `lucide-react-native`.
- Do not add a new date library. Use `date-fns`.
- Do not import `Colors` or `DarkColors` directly inside component files. Always use `useColors()`.
- Do not hardcode colour hex values in components; reference palette tokens only.
