<p align="center">
  <img src="assets/icon.png" width="96" alt="Finio icon" />
</p>

<h1 align="center">Finio</h1>
<p align="center">An offline-first personal finance tracker for Android and iOS, built with React Native and Expo.</p>

<p align="center">
  <a href="https://github.com/abhi-sawant/finio/releases"><img alt="Latest release" src="https://img.shields.io/github/v/release/abhi-sawant/finio?style=flat-square" /></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%20%7C%20iOS-blue?style=flat-square" />
  <img alt="Expo SDK" src="https://img.shields.io/badge/Expo-SDK%2054-000?logo=expo&style=flat-square&cacheSeconds=0" />
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.9-3178c6?logo=typescript&logoColor=white&style=flat-square" />
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green?style=flat-square" />
</p>

---

Track income, expenses, and transfers across multiple accounts — all stored locally on your device. An optional self-hosted PHP backend adds cloud backup and account sync for those who want it.

## Features

- **Dashboard** — time-aware greeting, total balance card with animated counter, horizontal account carousel, monthly income/expense summary, recent transactions, and a spending donut chart
- **Transactions** — full list with free-text search, type / account / category filters, and daily date-group headers with net totals; swipe left or right to edit/delete
- **Accounts** — checking, savings, cash, credit, investment, and wallet accounts; each with a custom colour, icon, and currency; balances update automatically on every add/edit/delete
- **Accounts** — checking, savings, cash, credit, investment, and wallet accounts; each with a custom colour, icon, and currency; balances update automatically on every add/edit/delete
- **Credit cards** — credit accounts track outstanding balance and optional credit limit; the Dashboard shows an Upcoming Payments section listing cards with money owed, sorted by amount due and including a utilization indicator
- **Transfers** — move money between any two accounts with fully bidirectional balance adjustment and one-tap reversal
- **Analytics** — income vs expense bar chart, 30-day balance trend, spending donut, label spending bar, and a top-categories table; filtered by week / month / 3M / 6M / year
- **Categories & Labels** — 24 defaults included; fully customisable with icon and colour pickers; labels support multi-select tagging on individual transactions
- **Themes** — light, dark, and system modes; Android navigation bar colour follows the active theme
- **Local export / import** — share your full data as a JSON file and import it back on any device
- **Daily reminders** — opt-in push notification at 09:00 to prompt daily expense logging
- **Cloud backup** _(optional)_ — register and log in to sync to a self-hosted backend; auto-backup runs silently on startup if the last backup is more than 24 hours old
- **In-app update checker** — compares the installed version against the latest GitHub Release and prompts to download when a newer version is available
- **Haptic feedback** — subtle tactile responses throughout the UI

## Tech stack

| Layer          | Library                                                                                   |
| -------------- | ----------------------------------------------------------------------------------------- |
| Framework      | [Expo](https://expo.dev) SDK 54 + [Expo Router](https://expo.github.io/router/) 6         |
| Language       | TypeScript 5.9 (strict)                                                                   |
| State          | [Zustand](https://zustand.docs.pmnd.rs/) 5 + AsyncStorage persistence                     |
| Styling        | [NativeWind](https://www.nativewind.dev/) 4 (Tailwind for RN) + `StyleSheet`              |
| Charts         | Custom SVG via [`react-native-svg`](https://github.com/software-mansion/react-native-svg) |
| Forms          | [React Hook Form](https://react-hook-form.com/) 7 + [Zod](https://zod.dev/) 4             |
| Animation      | [Reanimated](https://docs.swmansion.com/react-native-reanimated/) 4                       |
| Icons          | [lucide-react-native](https://lucide.dev/)                                                |
| Dates          | [date-fns](https://date-fns.org/) 4                                                       |
| Fonts          | DM Sans + Sora via `@expo-google-fonts`                                                   |
| Secure storage | [expo-secure-store](https://docs.expo.dev/versions/latest/sdk/securestore/)               |
| Backend        | PHP 8.2+, MySQL, PHPMailer (self-hosted, optional)                                        |

## Getting started

### Prerequisites

- [Node.js](https://nodejs.org/) 20+
- [Expo Go](https://expo.dev/go) app on your device, or Android Studio / Xcode for native builds

### 1. Clone and install

```bash
git clone https://github.com/abhi-sawant/finio.git
cd finio
npm install
```

### 2. Configure environment

```bash
cp .env.example .env
```

Set `EXPO_PUBLIC_API_URL` in `.env` to your backend URL. If you don't need cloud backup, you can leave the file as-is — the app works entirely offline without an account.

### 3. Start

```bash
# Expo Go (fastest for development)
npx expo start

# Native Android build
npx expo run:android

# Native iOS build
npx expo run:ios
```

> [!TIP]
> The app is fully functional with no backend — accounts, transactions, and all data stay on-device. Cloud backup is an optional enhancement.

## Project structure

```
app/            Expo Router screens — (tabs), auth flow, and full-screen modals
components/     Reusable UI — accounts, categories, charts, common, dashboard, layout, transactions
constants/      DarkColors, LightColors, AccountColors, CategoryColors, LabelColors
constants/      DarkColors, LightColors, LabelColors, ColorPalette, ColorKey
data/           Seed data — 24 categories, 9 labels, default settings
hooks/          useColors · useCountUp · useDebounce · useThemeColor
services/       api.ts · backup.ts · updater.ts
store/          useFinanceStore · useAuthStore · selectors.ts
types/          Shared TypeScript interfaces and union types
utils/          calculations · formatters · haptics
backend/        Self-hosted PHP REST API (optional cloud backup)
```

## Backend (optional cloud backup)

The backend is a minimal PHP 8.2+ REST API built for cPanel shared hosting — no framework, no Docker required.

| Detail   | Value                                                            |
| -------- | ---------------------------------------------------------------- |
| Auth     | JWT HS256 + 6-digit OTP email verification                       |
| Database | MySQL (2 tables: `users` and `backups`)                          |
| Email    | PHPMailer over SMTP                                              |
| Config   | Stored outside `public_html` — never committed to source control |

### Deploy in three steps

1. Follow [`backend/SETUP_GUIDE.txt`](backend/SETUP_GUIDE.txt) for the full cPanel walkthrough.
2. Copy `backend/config.example.php` → `~/finio-config/config.php` on your server and fill in your credentials.
3. Import `backend/schema.sql` via phpMyAdmin, then set `EXPO_PUBLIC_API_URL` in your app's `.env`.

### API reference

| Method | Path                    | Auth | Description                |
| ------ | ----------------------- | :--: | -------------------------- |
| POST   | `/auth/register`        |      | Create account, send OTP   |
| POST   | `/auth/verify-otp`      |      | Verify OTP, receive JWT    |
| POST   | `/auth/resend-otp`      |      | Resend OTP                 |
| POST   | `/auth/login`           |      | Login, receive JWT         |
| POST   | `/auth/forgot-password` |      | Send password-reset email  |
| POST   | `/auth/reset-password`  |      | Reset password with OTP    |
| GET    | `/user/me`              |  ✓   | Get profile                |
| PUT    | `/user/me`              |  ✓   | Update profile             |
| DELETE | `/user/me`              |  ✓   | Delete account             |
| POST   | `/backup/upload`        |  ✓   | Upload JSON backup         |
| GET    | `/backup/latest`        |  ✓   | Fetch latest backup        |
| GET    | `/backup/list`          |  ✓   | List all backups           |
| GET    | `/backup/{date}`        |  ✓   | Download a specific backup |
| DELETE | `/backup/{date}`        |  ✓   | Delete a specific backup   |

JWT tokens expire after 30 days.

## Data model

```ts
Account       { id, name, type, currency, color, icon, balance, createdAt,
               creditLimit? }   ← credit accounts only
Transaction   { id, type, amount, accountId, toAccountId?, categoryId,
                date, note, labels, createdAt }
Category      { id, name, icon, color, type }
Label         { id, name, color }
Settings      { currency, theme, userName, useBiometrics, hapticFeedback, notifications }

AccountType     = 'checking' | 'savings' | 'cash' | 'credit' | 'investment' | 'wallet'
TransactionType = 'expense' | 'income' | 'transfer'
Currency        = 'USD' | 'EUR' | 'GBP' | 'INR' | 'JPY' | 'CAD' | 'AUD'
Theme           = 'dark' | 'light' | 'system'
```

## Security

- JWT tokens and credentials are stored in `expo-secure-store` (device keychain), never in plain AsyncStorage.
- The backend `config.php` lives one level above `public_html` and is never committed to source control.
- All backend SQL queries use PDO prepared statements — no string interpolation.
- OTPs are SHA-256 hashed before storage; passwords use bcrypt (cost 12).
- `.env` is gitignored — only `.env.example` (with placeholder values) is committed.
