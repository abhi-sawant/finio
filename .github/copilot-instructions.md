# Finio – Copilot Instructions

This file provides the coding AI agent (GitHub Copilot, Cursor, etc.) with a complete picture of the project so that suggestions are accurate, idiomatic, and architecturally consistent.

---

## Project Overview

**Finio** is a personal finance tracker mobile app (React Native / Expo) with an optional cloud backup feature powered by a self-hosted PHP backend.

- **App name in Expo**: `Finio`
- **App bundle ID**: `com.finio.app`
- **Backend API base URL**: configured via `EXPO_PUBLIC_API_URL` in `.env` (see `.env.example`)
- **Target platforms**: Android (primary), iOS

---

## Monorepo Structure

```
/
├── app/                    Expo Router file-based navigation
│   ├── _layout.tsx         Root layout (fonts, splash, auth gate)
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
│   ├── charts/             Victory Native charts
│   ├── common/             AmountInput, BottomSheet, ColorPicker, DatePicker,
│   │                       EmptyState, IconPicker, LabelPicker, SkeletonLoader, Toast
│   ├── dashboard/          SummaryCards, RecentTransactions
│   ├── layout/             FAB, Header, TabBar
│   └── transactions/       TransactionFilters, TransactionItem, TransactionList
├── constants/
│   └── Colors.ts           Central colour palette (dark-only theme)
├── data/
│   └── defaultData.ts      Seed data: accounts, categories, labels, transactions, settings
├── hooks/
│   ├── useCountUp.ts       Animated number counter
│   ├── useDebounce.ts
│   └── useThemeColor.ts
├── services/
│   ├── api.ts              Typed fetch wrapper for the PHP REST API
│   └── backup.ts           uploadBackup / restoreLatestBackup / autoBackupIfNeeded
├── store/
│   ├── useFinanceStore.ts  Zustand store (persisted via AsyncStorage)
│   ├── useAuthStore.ts     Zustand store (token persisted via SecureStore)
│   └── selectors.ts        Derived selectors (totals, filtered lists, etc.)
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
        └── Controllers/
            ├── AuthController.php   register, verifyOtp, login,
            │                        forgotPassword, resetPassword
            ├── BackupController.php upload, getLatest
            └── UserController.php   profile, deleteAccount
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
| Charts | Victory Native 41 |
| Forms | React Hook Form 7 + Zod 4 validation |
| Animation | `react-native-reanimated` 4 |
| Lists | `@shopify/flash-list` |
| HTTP | Native `fetch` (wrapped in `services/api.ts`) |
| Icons | `lucide-react-native` |
| Date handling | `date-fns` |
| Haptics | `expo-haptics` |

### Backend (PHP)

| Area | Detail |
|---|---|
| Language | PHP 8.2+ |
| Database | MySQL (via PDO, prepared statements only) |
| Auth | JWT (HS256) + OTP email verification |
| Email | PHPMailer via SMTP |
| Hosting target | cPanel shared hosting (MilesWeb) |
| No framework | Vanilla PHP with a tiny custom router |

---

## Data Model (TypeScript types — `types/index.ts`)

```ts
Account       { id, name, type, currency, color, icon, balance, createdAt }
Transaction   { id, type, amount, accountId, toAccountId?, categoryId, date, note, labels, createdAt }
Category      { id, name, icon, color, type }         // type: 'expense'|'income'|'both'
Label         { id, name, color }
Settings      { currency, theme, userName, useBiometrics, hapticFeedback, notifications }
```

Transaction types: `'expense' | 'income' | 'transfer'`
Account types: `'checking' | 'savings' | 'cash' | 'credit' | 'investment' | 'wallet'`
Supported currencies: `'USD' | 'EUR' | 'GBP' | 'INR' | 'JPY' | 'CAD' | 'AUD'`

---

## State Management

### `useFinanceStore` (Zustand + AsyncStorage persist)

The single source of truth for all financial data.

- **Persisted** with `zustand/middleware` `persist` via AsyncStorage.
- Exposes CRUD actions for accounts, transactions, categories, labels.
- `addTransaction` automatically adjusts account balances.
- `deleteAccount` cascades and removes related transactions.
- `importData` is used for backup restore.

### `useAuthStore` (Zustand, manual SecureStore)

- Stores `token` (JWT), `user` (`{ id, name, email }`), `lastBackupAt`.
- `setAuth` / `clearAuth` persist to `expo-secure-store`.
- `isLoaded` flag prevents rendering before hydration.

### Selectors (`store/selectors.ts`)

Pure selector functions — always import derived data from here, not re-compute it inline.

---

## Backend API

Base URL: `EXPO_PUBLIC_API_URL` (env var, fallback to `https://api.finio.slowatcoding.com`)

All endpoints return JSON. Authenticated routes require `Authorization: Bearer <token>`.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | No | Create account, send OTP |
| POST | `/auth/verify-otp` | No | Verify OTP, receive JWT |
| POST | `/auth/resend-otp` | No | Resend OTP |
| POST | `/auth/login` | No | Login, receive JWT |
| POST | `/auth/forgot-password` | No | Send reset email |
| POST | `/auth/reset-password` | No | Reset with token |
| GET | `/user/profile` | Yes | Get profile |
| DELETE | `/user/account` | Yes | Delete account |
| POST | `/backup/upload` | Yes | Upload JSON backup |
| GET | `/backup/latest` | Yes | Fetch latest backup |

JWT access tokens expire in **30 days**. All secrets live in `~/finio-config/config.php` (outside `public_html`).

---

## Navigation Architecture

Expo Router with three route groups:

1. **(tabs)** — bottom-tab navigator, shown to authenticated users
2. **auth/** — stack for unauthenticated users (login, register, OTP, password reset)
3. **modals/** — full-screen modals pushed over tabs

The root `_layout.tsx` redirects between auth and tabs based on `useAuthStore().token`.

---

## Styling Conventions

- **Dark-only** theme. All colours come from `constants/Colors.ts`.
- Use NativeWind `className` props for spacing/layout when practical.
- Use `StyleSheet.create()` for anything that involves colours, shadows, or complex overrides.
- The colour palette keys you'll reference most: `Colors.primary`, `Colors.background`, `Colors.surface`, `Colors.text`, `Colors.textMuted`, `Colors.border`, `Colors.error`, `Colors.success`.

---

## Code Conventions

- **Language**: TypeScript, strict mode. All new files must be `.ts` / `.tsx`.
- **No `any`** except where unavoidable; prefer `unknown` and narrowing.
- **Imports**: Use the `@/` path alias (maps to project root). Never use relative `../..` chains.
- **Components**: Functional components only. No class components.
- **Exports**: Named exports for components and utilities. Default export for screens/layouts.
- **ID generation**: Use the local `generateUUID()` in `useFinanceStore.ts` (crypto-grade UUIDs client-side); do not add nanoid or uuid packages.
- **Dates**: Always store as ISO 8601 strings. Use `date-fns` for formatting/parsing.
- **Currency amounts**: Store as plain `number` (float). Formatting is done via `utils/formatters.ts`.
- **Forms**: Always use `react-hook-form` + `zod` for any input that modifies store/API data.
- **Error handling**: Surface errors via the `<Toast>` component, not `Alert.alert`.

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
3. Update the `add-transaction.tsx` modal form + Zod schema.
4. Update `TransactionItem.tsx` and `transaction-detail.tsx` display.
5. Update `backup/upload` PHP endpoint if the field needs to be persisted server-side.

### Add a new screen
1. Create the file under `app/(tabs)/` or `app/modals/`.
2. If it's a tab, add a `<Tabs.Screen>` entry in `app/(tabs)/_layout.tsx` and a tab button in `components/layout/TabBar.tsx`.

### Add a new API endpoint
1. Add the route in `backend/public/index.php`.
2. Add the controller method in the appropriate `Controllers/*.php` file.
3. Add the typed `api.*` wrapper in `services/api.ts`.

### Run the app
```bash
npx expo start          # Expo Go or dev build
npx expo run:android    # Native Android build
```

### Backend deployment
Follow `backend/SETUP_GUIDE.txt` for full cPanel/MilesWeb deployment instructions.

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
