<p align="center">
  <img src="assets/icon.png" width="96" alt="Finio icon" />
</p>

<h1 align="center">Finio</h1>
<p align="center">A clean, offline-first personal finance tracker built with React Native & Expo.</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%20%7C%20iOS-blue" />
  <img alt="Expo SDK" src="https://img.shields.io/badge/Expo-SDK%2054-black?logo=expo" />
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5.9-blue?logo=typescript" />
  <img alt="License" src="https://img.shields.io/badge/license-MIT-green" />
</p>

---

## Features

- **Dashboard** — greeting, balance summary cards, account carousel, spending donut chart
- **Transactions** — full list with search, date/type/category filters
- **Accounts** — multiple accounts (checking, savings, cash, credit, investment, wallet) with per-currency balances
- **Analytics** — income vs expense bar chart, balance trend line, spending breakdown
- **Categories & Labels** — fully customisable with icon and colour pickers
- **Transfers** — move money between accounts with automatic balance adjustment
- **Cloud Backup (optional)** — register / log in to sync your data to a self-hosted PHP backend; auto-backup runs every 24 hours
- **Dark theme** — dark-only UI with a purple primary palette
- **Haptic feedback** — subtle tactile responses throughout

---

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | [Expo](https://expo.dev) SDK 54 + [Expo Router](https://expo.github.io/router/) 6 |
| Language | TypeScript 5.9 (strict) |
| State | [Zustand](https://zustand.docs.pmnd.rs/) 5 + AsyncStorage persistence |
| Styling | [NativeWind](https://www.nativewind.dev/) 4 (Tailwind CSS for RN) |
| Charts | [Victory Native](https://commerce.nearform.com/open-source/victory-native/) 41 |
| Forms | [React Hook Form](https://react-hook-form.com/) 7 + [Zod](https://zod.dev/) 4 |
| Animation | [Reanimated](https://docs.swmansion.com/react-native-reanimated/) 4 |
| Lists | [@shopify/flash-list](https://shopify.github.io/flash-list/) |
| Icons | [lucide-react-native](https://lucide.dev/) |
| Dates | [date-fns](https://date-fns.org/) |
| Secure storage | [expo-secure-store](https://docs.expo.dev/versions/latest/sdk/securestore/) |
| Backend | PHP 8.2+ (self-hosted, optional) |

---

## Project Structure

```
app/            Expo Router screens (tabs, auth, modals)
components/     Reusable UI components
constants/      Color palette
data/           Default seed data
hooks/          Custom hooks (useCountUp, useDebounce, useThemeColor)
services/       API wrapper (api.ts) and backup logic (backup.ts)
store/          Zustand stores + selectors
types/          Shared TypeScript interfaces
utils/          Pure helpers (calculations, formatters, haptics)
backend/        Self-hosted PHP REST API (optional cloud backup)
```

---

## Getting Started

### Prerequisites

- [Node.js](https://nodejs.org/) 20+
- [Expo CLI](https://docs.expo.dev/more/expo-cli/) (`npm i -g expo-cli`)
- Android Studio / Xcode (for native builds) or the [Expo Go](https://expo.dev/go) app

### 1. Clone & install

```bash
git clone https://github.com/your-username/finio.git
cd finio
npm install
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set `EXPO_PUBLIC_API_URL` to your backend URL, or leave the fallback to use the app **fully offline** (no account required).

### 3. Run

```bash
# Start in Expo Go (development)
npx expo start

# Native Android build
npx expo run:android

# Native iOS build
npx expo run:ios
```

---

## Backend (Optional Cloud Backup)

The backend is a lightweight PHP 8.2+ REST API designed for cPanel shared hosting.

| Feature | Detail |
|---|---|
| Auth | JWT (HS256) + 6-digit OTP email verification |
| Database | MySQL (2 tables: `users`, `backups`) |
| Email | PHPMailer over SMTP |
| Config | Stored **outside** `public_html` for security |

### Quick deploy

1. Follow the step-by-step instructions in [`backend/SETUP_GUIDE.txt`](backend/SETUP_GUIDE.txt).
2. Copy `backend/config.example.php` to `~/finio-config/config.php` on your server and fill in the values.
3. Import `backend/schema.sql` via phpMyAdmin.
4. Set `EXPO_PUBLIC_API_URL` in `.env` to your API subdomain.

### API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | — | Register + send OTP |
| POST | `/auth/verify-otp` | — | Verify OTP, get JWT |
| POST | `/auth/resend-otp` | — | Resend OTP |
| POST | `/auth/login` | — | Login, get JWT |
| POST | `/auth/forgot-password` | — | Send reset email |
| POST | `/auth/reset-password` | — | Reset with token |
| GET | `/user/profile` | ✓ | Get profile |
| DELETE | `/user/account` | ✓ | Delete account |
| POST | `/backup/upload` | ✓ | Upload JSON backup |
| GET | `/backup/latest` | ✓ | Fetch latest backup |

---

## Data Model

```ts
Account     { id, name, type, currency, color, icon, balance, createdAt }
Transaction { id, type, amount, accountId, toAccountId?, categoryId,
              date, note, labels, createdAt }
Category    { id, name, icon, color, type }   // 'expense' | 'income' | 'both'
Label       { id, name, color }
Settings    { currency, theme, userName, useBiometrics, hapticFeedback, notifications }
```

---

## Security Notes

- JWT tokens and user credentials are stored in `expo-secure-store` (device keychain), never in plain AsyncStorage.
- The backend config file (`config.php`) lives **one level above `public_html`** on the server and is never committed to source control.
- All backend SQL queries use **PDO prepared statements** — no raw interpolation.
- OTPs are hashed with SHA-256 before storage; passwords use `bcrypt` (cost 12).
- `.env` is gitignored. Only `.env.example` (with placeholder values) is committed.

---

## Contributing

1. Fork the repo and create a feature branch.
2. Follow the conventions in [`.github/copilot-instructions.md`](.github/copilot-instructions.md).
3. Open a pull request with a clear description of your changes.

---

## License

MIT — see [LICENSE](LICENSE) for details.
