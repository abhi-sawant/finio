# Finio Android — CLAUDE.md

Native Android client: Kotlin + Jetpack Compose, a **1:1 port of the PWA in `web/`**. The web app
is the reference implementation — read the matching web file before building or changing a screen.
Product rules, parity rules and every domain gotcha live in the [root CLAUDE.md](../CLAUDE.md) and
[web/CLAUDE.md](../web/CLAUDE.md); they all apply here. The visual system is [design.md](../design.md)
("Mudra"). This file covers only what is Android-specific.

---

## Build, test, install

JDK = Android Studio's JBR. Run from `android/`:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :core:test                        # pure domain logic + golden fixtures (spec/fixtures)
./gradlew :core:test :app:testDebugUnitTest # + app-layer JVM tests (platform seams, routes)
./gradlew :app:assembleDebug                # debug APK
./gradlew :app:installDebug                 # install on the connected device
adb shell am start -n com.slowatcoding.finio/.MainActivity

# Deep links (notification clicks / shortcuts use the same scheme)
adb shell am start -a android.intent.action.VIEW -d "finio://open?path=%2Fbudgets" com.slowatcoding.finio
```

`-PfinioApiUrl=https://api.example.com` overrides the backend (web: `VITE_API_URL`). Tests run in
`Asia/Kolkata`, like vitest. `core` tests read `../spec/fixtures` — regenerate with
`cd web && npm run gen:fixtures` when the TS logic changes, then make the Kotlin port pass.

Debug builds only: long-press the **Tools** page title to open the Mudra component gallery
(`Routes.DebugGallery`, `src/debug/.../gallery/GalleryScreen.kt`). `src/release` defines
`debugGallery = null`, so it never ships.

---

## Module map

```
android/
├── core/  (pure Kotlin/JVM — no Android imports; every money rule, unit-tested against golden fixtures)
│   └── com.slowatcoding.finio.core
│       ├── model/      Model.kt (every domain type, wire names), FinioJson
│       ├── store/      FinanceStore (all actions, atomic), Persistence (encode/decodePersisted, v1→v17
│       │               migrations), Balance, Recurring, AppLockState, AuthState, BackupCryptoState
│       ├── calc/       Calculations, Analytics, Forecast, Insights, Merchants, NetWorth
│       ├── period/     Period (financial months, monthStartDay), DateExtras
│       ├── loan/ deposit/ rules/ (AutoCategorize, JsRegex) csv/ importing/ (ImportValidation)
│       ├── notify/     Notifications (selection), NotificationSchedule (pure builder)
│       ├── backup/     BackupPayload (collectBackupPayload), BackupMeta
│       ├── crypto/     PinCrypto, BackupCrypto (AES-GCM envelope), JsJson
│       ├── applock/    AppLock (shouldLockOnResume, penalty ladder, countdown)
│       ├── share/      ShareTarget (parseSharePayload), WhatwgUrl
│       ├── format/     Formatters (formatCurrency, formatShortDate…)
│       ├── data/       DefaultData, ColorPalette, SampleData (deterministic onboarding dataset)
│       └── js/ money/ id/ util/   JS-faithful dates/numbers, roundMoney, ids, Validation, Errors
└── app/
    └── com.slowatcoding.finio
        ├── FinioApp.kt          Application: builds the AppContainer, implements AutoBackupHost
        ├── MainActivity.kt      FragmentActivity: parses intents → LaunchTarget, setContent { FinioRoot }
        ├── di/AppContainer.kt   Manual DI + hydration, persistence, auto-lock, the foreground pass
        ├── platform/            Android I/O behind small seams (all written before the UI)
        │   ├── storage/   JsonFileStore (atomic JSON files = web localStorage keys), DebouncedJsonWriter
        │   ├── api/       FinioApi (OkHttp, typed endpoints)
        │   ├── backup/    AutoBackup (+ policy, workers, scheduler, host contracts)
        │   ├── files/     BackupFolder (SAF), Documents, ShareExport, BackupRotation
        │   ├── lock/      BiometricUnlock, AppLifecycleWatcher, BackgroundedAtStore, SecureWindowEffect
        │   ├── notify/    NotificationScheduler/Runner/Store, permission requester
        │   └── share/     LaunchTarget, DeepLinks, IncomingIntent
        └── ui/
            ├── theme/ mudra/ components/ icons/   The Mudra design system (see below)
            ├── shell/       FinioRoot (gates), AppShell (NavHost + chrome), FinioTabBar, TemplatesFab, ShellState
            ├── navigation/  Routes, FinioNavigator, FinioNavGraph, NavItems (tabs + Tools list)
            ├── common/      Screen helpers (store access, money, header parts, guards, loaders, menus)
            └── screens/<feature>/<Name>Screen.kt   One file per web page
```

---

## The app shell

- **`FinioApp` → `AppContainer.start()`** hydrates every store before any activity or worker runs.
  Lock, auth and backup-crypto files are tiny and read synchronously (the lock decision must land
  before the first frame — the web's `onRehydrateStorage`). The finance store decodes on IO;
  `FinanceStore.isHydrated` flips when it lands.
- **Persistence**: every store change goes through a `DebouncedJsonWriter` (finance 300ms; lock,
  auth and backup-crypto immediately — a failed-PIN count must survive the app being killed), and
  all writers are `flushBlocking()`ed when the process goes to the background.
- **Gates** (`ui/shell/FinioRoot.kt`), each rendered *instead of* the app and none ever navigating,
  in App.tsx's order: hydration → lock → onboarding → `AppShell`. The NavController, its graph and
  a `SaveableStateHolder` live above the gates, so lock → unlock returns to the same screen with
  its state; the NavHost itself leaves composition while locked (which also dismisses any dialog a
  screen had open — dialogs are separate windows and would otherwise draw over the lock screen).
- **Auto-lock** (`AppContainer.onBackground/onForeground`, the port of `useAutoLock`): background →
  stamp `BackgroundedAtStore` (clear it when locked or the lock is off), lock immediately when
  `autoLockMinutes == 0`; foreground → `shouldLockOnResume`. `AppLockState.lock()` dismisses all
  toasts and clears the stamp. `FLAG_SECURE` is on whenever the lock is enabled.
- **The foreground pass** (web `Layout.tsx` effects, same order): `processRecurring` (Undo toast) →
  `processMaturities` (toast) → `captureNetWorthSnapshots` → `refreshReminders()` → cloud + local
  auto backups. Runs on cold start **and** on every return to the foreground, but only once the
  gates are down (hydrated, unlocked, onboarded) — on the web it lives in `Layout`, which only
  mounts behind them. (The web runs it only on hydration — improvements.md §1.4; Android fixes it.)
- **Launch targets**: `MainActivity` parses `ACTION_SEND` / `finio://open?path=…` in `onCreate`
  (fresh launches only) and `onNewIntent`, and parks the `LaunchTarget` on the container. `AppShell`
  — which only exists once the gates have lifted — navigates to it and consumes it. Deep links are
  pushed on top of the Dashboard, so back always lands in the app.
- **Chrome** (`AppShell`): on layout routes (Dashboard, Accounts, Transactions, Analytics, Tools,
  Settings, SettingsCategory) the glass `FinioTabBar` floats at the bottom; the `TemplatesFab` coin
  floats 88dp above the nav-bar inset except on Accounts, Tools and Settings*, and while a screen
  calls `SuppressFab(true)`. Both step aside while the keyboard is up. Full-screen routes get
  neither. Page transitions are instant, like the web.

### Reminders

`AppContainer.refreshReminders()` = web `refreshNotificationSchedule()`: rebuilds the schedule with
core `buildNotificationSchedule` and `NotificationScheduler.publish`es it; without permission (or
with reminders off) it writes an empty schedule but keeps the fired ledger. Call it after any
reminder-preference change. `AppContainer.disableReminders()` = `teardownNotifications()` (wipes
schedule + ledger, cancels workers) — for the Settings "off" switch.

---

## Screen conventions (read before writing a screen)

**Signature.** Every screen is `@Composable fun <Name>Screen(nav: FinioNavigator, …args)` in
`ui/screens/<feature>/`. The NavHost (`ui/navigation/FinioNavGraph.kt`) calls it; keep the
signature. Route args arrive already decoded (ids, `SharedTransactionDraft?`, `RuleScope?`).

**Data.** No ViewModels. Read and act on the store directly:

```kotlin
val store = financeStore()                 // actions: store.addBudget(…), store.deleteTransaction(id)…
val state by collectFinanceState()         // FinanceState, lifecycle-aware
val accounts = remember(state.accounts) { activeAccounts(state.accounts) }       // cheap: remember
val statuses = rememberDerived(state.budgets, state.transactions, monthStartDay) { // heavy: off-main
    computeBudgetStatuses(…)
}                                            // null until the first result → SectionLoader()
```

Store actions are synchronous, atomic and safe on the main thread. Never re-implement domain
logic in a screen — call the `core` function the web page calls (same names). Other stores:
`appContainer().appLock`, `.auth`, `.backupCrypto`, `.api`, `.backgroundedAt`.

**Money.** `val money = rememberMoneyFormatter()` then `money(amount)` / `money(x, compact = true)`
— `formatCurrency` with `hidden` bound to `settings.hideAmounts`. Never format money any other way.
Headline figures use `FinioType.money`/`displayMoney` (Familjen Grotesk); row amounts stay Instrument Sans.

**Header.** `FinioScreen(header = { … }) { body }` (`ui/components/PageShell.kt`).
Tab pages: `PageTitle("Accounts")` + trailing actions. Sub-pages: `BackButton(nav)`,
`ScreenTitle("Budgets")`, then a trailing action or `HeaderIconSpacer()` to keep the title centred.
Data-bearing pages put `HideAmountsToggle()` in the header. Header buttons are `HeaderIconButton`.
`FinioMain` already pads 160dp at the bottom so content clears the tab bar and coin.
`FinioScreen` ends its scroller at the keyboard (imePadding) and treats the floating header as
off-screen when bringing a focused field into view; a screen that floats its own sticky footer
passes its height as `bottomObscured`. Field triggers (select/date/time) clear text focus before
opening, so the keyboard never pops back when their popup closes.

**Navigation.** Only through `FinioNavigator`: `nav.navigate(Routes.Budgets)`, `nav.back()`,
`nav.openTab(FinioTab.Accounts)`, `nav.replace(Routes.Settings)` (web `<Navigate replace>`),
`nav.navigateToPath("/budgets")` (insight `action.to`, notification URLs). Always qualify routes
(`Routes.Settings` — names collide with core types). The web's `location.state` payloads are route
args (`Routes.VerifyOtp(email)`, `Routes.CategoryRules(pattern, scope)`). Edit routes are already
wrapped in `EditGuard` by the NavHost (a stale id replaces itself with the list, checked once at
entry) — a screen that deletes its own entity just calls `nav.back()`.

**Toasts.** `toast.success("Saved")`, `toast.error(getErrorMessage(e, "Something went wrong"))` (global, Sonner-like, from
`ui/components/FinioToast.kt`). Every destructive or automatic change offers Undo:
`undoToast("Transaction deleted") { store.restoreTransaction(removed) }`.

**Confirm.** `val confirm = LocalConfirm.current; scope.launch { if (confirm.confirm("Delete budget?", "…", confirmLabel = "Delete")) … }`
— never a hand-rolled AlertDialog. Long-press row menus: `Modifier.longPressable(onClick, onLongClick)`
+ `ActionMenu(onDismissRequest, listOf(MenuAction(…)))` from `ui/common/LongPress.kt`.

**Chrome hooks.** Transactions' bulk-selection mode: `SuppressFab(active = selecting)` (ui/shell).

**Loading / empty.** `PageLoader()`, `SectionLoader()`, `Spinner()` (ui/common/Loading.kt);
`EmptyState(title, description, icon, action)` (ui/components).

**Placeholders.** Unported screens render `PlaceholderScreen(title, nav)` — replace the whole body.

---

## Platform mapping (PWA → Android)

| PWA                                              | Android                                                                                     |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------- |
| localStorage keys (`finio-storage`, `finio-lock`…) | One atomic JSON file each in `filesDir` (`JsonFileStore`, `StoreFiles`), `.bak` fallback       |
| Zustand `persist` + `migrate`                    | `encodePersisted`/`decodePersisted` (same `{state, version}` envelope, v17) + debounced writer |
| `useAutoLock` (visibilitychange/pagehide)        | `AppLifecycleWatcher` (ProcessLifecycleOwner) → `AppContainer.onBackground/onForeground`     |
| Task-switcher snapshot of the lock screen        | `FLAG_SECURE` while the lock is enabled (`SecureWindowEffect`)                               |
| WebAuthn platform authenticator                  | `BiometricPrompt` BIOMETRIC_STRONG (`BiometricUnlock`); flag stored in `webauthnCredentialId` |
| Web Share Target `/share-target`                 | `ACTION_SEND text/plain` → `LaunchTarget.AddTransaction` → `Routes.AddTransaction(draft)`    |
| Manifest shortcuts                               | `res/xml/shortcuts.xml` → `finio://open?path=/add-transaction%3Ftype%3Dexpense` etc.         |
| Service worker + IndexedDB reminders             | WorkManager (12h periodic + one-shot at next `fireAt`) + JSON schedule/ledger files          |
| Periodic Background Sync                         | `NotificationWorker` periodic                                                                |
| Notification click → `client.navigate(url)`      | `finio://open?path=<url>` PendingIntent → `onNewIntent` → LaunchTarget                       |
| `autoBackupIfNeeded` / local download backups     | `AutoBackup` + `CloudAutoBackupWorker` / `LocalAutoBackupWorker` into a SAF folder            |
| File System Access API folder                    | Storage Access Framework tree URI (`BackupFolder`)                                           |
| `<input type=file>` / downloads                  | `Documents` (SAF open/create) / `ShareExport` (FileProvider share sheet)                     |
| Sonner `<Toaster>`                               | `toast` + `FinioToastHost` (root of FinioRoot)                                               |
| `useConfirm()`                                   | `LocalConfirm.current.confirm(…)` (`ConfirmHost` at the root)                                |
| React Router + `<Navigate>`                      | Navigation-Compose type-safe routes + `FinioNavigator`                                       |
| `ErrorBoundary`                                  | none — Compose has no render-error boundary; a crash is a crash (see compromises)           |
| `prefers-color-scheme` / `.dark`                 | `FinioTheme(settings.theme)` + `isSystemInDarkTheme()`; bar icons follow via `enableEdgeToEdge` |

---

## Visual compromises (Mudra on Android)

- **No backdrop blur.** Android can't blur what is behind an arbitrary view, so `card-elevated`,
  `glass-chrome` (header once scrolled, tab bar) and toasts are translucency only. To keep the
  scrolled header legible without the blur, floating chrome (header, tab bar, Add Transaction's
  submit bar) uses `chromeGlass` — `--glass-strong` raised to 92% opacity. Dialogs do get a
  4dp blur-behind on API 31+.
- **Selects are bottom sheets**, not anchored popups (thumb reach; 44dp rows instead of 28dp).
- **Time picking** uses the Material time dial in a Mudra dialog (the web uses the browser's
  native time input). Calendar cells are 36dp rather than 28px, for touch.
- **The NoteCard tilts under a finger** (the web only tilts for a mouse pointer); reduced motion
  removes it.
- **Toasts render beneath an open dialog** (the host draws in the activity window).
- **Tab bar and coin hide while the keyboard is up** (on the web the keyboard just covers them).
- **No error boundary** — the web's "Something went wrong / Go home" fallback has no Compose
  equivalent.
- Biometric unlock is never auto-prompted on the lock screen — same as the web (tap the
  fingerprint key).

---

## Gotchas

- **`MainActivity` must extend `FragmentActivity`.** androidx `BiometricPrompt` hosts itself in a
  fragment; a plain `ComponentActivity` crashes the biometric unlock.
- **Hold the `LaunchTarget` until the gates lift.** Never navigate from `MainActivity`, the lock
  screen or onboarding; `AppShell` consumes `pendingLaunch`. Parse intents only when
  `savedInstanceState == null` (and in `onNewIntent`) or a rotation replays the share.
- **The NavGraph is built once** (`remember(navController)` in FinioRoot). Rebuilding it inside the gated
  branch would hand `NavController.setGraph` a fresh graph on every unlock, which can reset the
  back stack; building it above the gates is what makes lock → unlock land on the same screen.
- **CSS shadow modifier order:** `cssShadow(shape, …)` goes **before** `background`/`clip`
  (outer layers paint behind the element), `cssInsetShadow(shape, …)` goes **after** the
  background. Wrong order = invisible or self-showing shadows.
- **Mudra classes are Kotlin APIs**, not resources: `FinioCard` (card-elevated), `glassSurface`,
  `FinioTheme.brushes.gradPrimary` (bg-grad-primary, always deep indigo — white text in both
  modes), `FinioTheme.shadows.*`, `FinioType.*`. Never use raw hex or stock Material colours.
  Income is `colors.positive`, never `primary`.
- **Dialogs and popovers are separate windows**: anything a screen shows in one must come from that
  screen's composition (so it disappears when the NavHost leaves on lock) — never from the
  container or a global.
- **`FinanceStore` actions are synchronous and atomic** — don't wrap them in coroutines. PBKDF2
  (`AppLockState.checkPin/setPin`, backup passphrase derivation) is slow: run it on
  `Dispatchers.Default`.
- **Workers can start the process with no UI.** `AppContainer.backupDataSource` waits for
  hydration internally; never read `financeStore.current` from a worker without
  `awaitHydrated()`.
- **Lint/compile**: `FinioRoute` must stay a `@Serializable sealed interface`; every new route
  is a `@Serializable` object/data class inside `Routes`, plus a `composable<…>` line in
  `FinioNavGraph.kt` and, if it is reachable by URL, a branch in `routeForPath`.
