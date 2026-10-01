# Finio Web — Manual Test Report

**Date:** 2026-10-01 · **Build under test:** `main` @ `2e734b0` (Vite dev server, `localhost:5173`)
**Browser:** in-app Chromium pane · **TZ of the browser:** `Asia/Calcutta` (UTC+5:30)
**Viewports:** 320×640, 411×963, 603×963, 1024×700, 1280×800 · light and dark

> Several findings below depend on the browser being in a positive-UTC-offset timezone (India is Finio's primary market). They are marked 🕐.

---

## 1. Summary

| Severity | Count | Meaning                                                   |
| -------- | ----- | --------------------------------------------------------- |
| High     | 9     | Wrong data, data loss, blocked flows, or unusable UI      |
| Medium   | 17    | Incorrect/misleading behaviour, missing validation, UX gaps |
| Low      | 22    | Polish, copy, accessibility, inconsistencies              |

**Top 5 to fix first**

1. **H1** Switching Expense ⇄ Income keeps the old category → income saved under "Food", expense under "Salary".
2. **H2** 🕐 Transactions are bucketed by the **UTC** day/month → entries between 00:00–05:30 IST land on the previous day/month; Income-vs-Expenses chart shows a ₹1Cr expense in the wrong month.
3. **H3** Dialogs positioned with `top-1/4` + `-translate-y-1/2` are clipped off the top of the screen (title and ✕ unreachable at 1024×700).
4. **H4** Edit Account form goes stale after Reconcile — pressing **Update Account** silently reverses the reconciliation.
5. **H5/H6/H7** Loan prepayment, goal withdrawal and debt settle-up have no upper/lower bounds, producing nonsensical balances.

Automated checks run alongside: `npm test` → 27 files / 552 tests **pass**; `npm run lint` → the 4 known pre-existing errors only (no new ones).

---

## 2. Coverage

Everything reachable was exercised, including conditional UI and empty states:

| Area | Covered |
| --- | --- |
| Onboarding | name step, account step (all types), opening-balance pad, Back, Skip, "Explore with sample data" |
| Dashboard | empty / zero-account / budgeted / over-budget / credit-card-due / deposits / attention banner / hide amounts |
| Accounts | add (all 8 types incl. Credit Card, FD, RD), edit, archive, reopen, delete + confirm, delete-blocker (FD payout account), Reconcile dialog |
| Transactions | add (expense/income/transfer/split), edit, delete + Undo, duplicate, save-as-template, multi-select, search (11 hostile queries), all filters, Clear filters, CSV export, long-press menu |
| Analytics | all period chips, custom range picker, insights, charts + data tables, forecast (30/60/90d), cash-flow calendar, net-worth (6/12/24m), compare periods, spending calendar |
| Budgets / Recurring / Goals / Debts / Loans | create, validate, edit, pause/resume, delete + confirm, history, backfill preview, settle-up, prepay, mark paid off |
| Merchants, Year in Review (incl. empty years), Categories, Labels, Categorization Rules (regex validation) | ✔ |
| Import / export | bank CSV (column mapping, bad rows, duplicates), JSON backup export, JSON import (garbage, wrong shape, round-trip) |
| Settings | name edit, theme, month-start day (25th cycle), App Lock (set / mismatch / lockout / unlock / disable / Forgot PIN), Reminders, Reset to Defaults |
| Auth screens | login, register, forgot password, verify-otp, reset-password (direct load) |
| Routing | unknown route, invalid `:id` on 3 edit routes, `/share-target` with SMS text, legal pages |
| Cross-cutting | 320 px overflow sweep on 24 routes, dark mode, a11y labels, console errors, double-submit |

**Not testable here:** actual cloud sign-in/backup (no backend credentials; I deliberately did not send fake credentials to the production API), browser notifications (permission is `denied` in this pane, so the Reminders sub-options never appear), Backup Folder (native picker), service worker / offline (`injectManifest` is not built under `vite dev`), real touch gestures (long-press was simulated with pointer events).

---

## 3. Findings

### 3.1 High

#### H1 — Changing transaction type keeps the previous type's category ✅ fixed (code; manual retest pending)
- **Where:** Add and Edit Transaction — [`AddTransaction.tsx:160`](src/pages/AddTransaction.tsx:160)
- **Steps:** Add → pick **Food** → tap **Income** → enter 77 → Add. Or Edit an income "Salary" → tap **Expense** → Update.
- **Expected:** category is cleared (or the form refuses) because "Food" is not an income category.
- **Actual:** the UI shows **no** category selected, yet the save succeeds and stores `type:"income", categoryId:"cat-1"` (Food). Edit case stored `type:"expense", categoryId:"cat-9"` (Salary). The "Select a category" guard never fires because the stale id is still in state.
- **Cause:** `handleTypeChange` calls `setType(next)` but never resets `categoryId`.
- **Impact:** silently corrupts category analytics, budgets (an income counted against a Food budget) and insights.

#### H2 — 🕐 Day/month bucketing uses the UTC date, not the local date ✅ fixed (code; manual retest pending)
- **Where:** [`calculations.ts:178`](src/utils/calculations.ts:178) (`t.date.slice(0, 10)`), [`IncomeExpenseBar.tsx:20`](src/components/charts/IncomeExpenseBar.tsx:20) (`t.date.slice(0, 7)`)
- **Steps:** In an IST browser, add an FD / loan prepayment / any date-picker entry for "Oct 1" (stored as local midnight = `2026-09-30T18:30Z`).
- **Actual:**
  - Transactions list files it under **YESTERDAY** and as a separate group from other Oct-1 rows (screenshot: FD row between "Today" and "Yesterday" headings).
  - Analytics → Income vs Expenses data table: `Sep 26 ₹0 / ₹1Cr` and `Oct 26 ₹99,999 / ₹5,075` — the ₹99,99,999 prepayment dated 1 Oct is counted in **September**.
  - The chart also ignores `Settings.monthStartDay`, contradicting the project rule "never bucket months by hand".
- **Impact:** wrong period totals and day headings for every user east of UTC, for anything logged 00:00–05:30 local.

#### H3 — Dialogs are clipped off the top of the viewport ✅ fixed (code; manual retest pending)
- **Where:** `className="bg-card top-1/4 …"` in 7 places — `ReconcileAccountDialog.tsx:76`, `Goals.tsx:392`, `CategoryRules.tsx:470`, `ManageLabels.tsx:83`, `Debts.tsx:365,407`, `Loans.tsx:388`
- **Cause:** base dialog uses `-translate-y-1/2`, so `top-1/4` centres the dialog at 25 % of the viewport. Any dialog taller than 50 % of the screen overflows the top.
- **Measured:** Settle-up dialog at 1024×700 → `top: -106px`, its **Close (✕) button at y = -98px** (unreachable; Esc is the only way out). At 411×963 the Reconcile dialog was `top: -20px` with the title half-cut.
- **Impact:** every number-pad dialog (settle up, prepay, reconcile, withdraw, add funds) hides its title and close button on laptop-height screens.

#### H4 — Edit Account form goes stale after "Reconcile Balance"; saving reverts it ✅ fixed (code; manual retest pending)
- **Where:** `AddAccount.tsx` (edit mode) + [`ReconcileAccountDialog`](src/components/accounts/ReconcileAccountDialog.tsx)
- **Steps:** Edit "HDFC Checking" (balance ₹1,25,075) → Reconcile Balance → statement ₹1,20,000 → Add adjustment (balance becomes ₹1,20,000, ‑₹5,075 adjustment posted) → the **Current Balance** field still shows **1,25,075** → tap **Update Account**.
- **Actual:** balance returns to ₹1,25,075 and `openingBalance` is bumped by +₹5,075 (20,075); the adjustment transaction remains, so history now contains a ‑₹5,075 entry that no longer matches reality.
- **Expected:** the form re-syncs after reconcile (or Update is disabled until clean).

#### H5 — Loan prepayment has no upper bound and leaves the loan inconsistent ✅ fixed (code; manual retest pending)
- **Steps:** Add ₹5,00,000 loan (0 %, 36 months) → Add Prepayment → 99,99,999 → Record.
- **Actual:** accepted; the source account goes to **‑₹97,74,925**. Loan card then reads `0 of 1 installments · ₹5L outstanding`, Details shows `Payoff date 05 October 2026`, `Prepayments ₹1Cr`. The outstanding is unchanged and the tenure collapsed to 1 instalment.
- **Expected:** reject amounts above the outstanding principal (or cap and auto-close the loan).

#### H6 — Savings goals: negative balances and a nonsensical completion estimate ✅ fixed (code; manual retest pending)
- **Withdraw past zero:** withdrawing ₹500 from "Vacation Fund" (₹0 saved) is accepted → card shows `-₹500 of ₹60,000 · -1%`.
- **Projection:** Emergency Fund `₹20,000 of ₹1,00,000 … ₹80,000 to go · at this pace, by 05 October 2026` (4 days away). The only contribution is a "Starting balance" dated 5 Aug, but pace is measured from `goal.createdAt` ([`calculations.ts:536`](src/utils/calculations.ts:536)), which is *today* for any new goal, giving ₹20,000/day. Any goal created with an opening contribution gets this.

#### H7 — Debt "Settle up" accepts any amount and flips the relationship ✅ fixed (code; manual retest pending)
- Rahul owes ₹1,500. Settle up → ₹99,999 → **Settle**: a real ₹99,999 income transaction is created and the ledger flips to `You owe ₹98,499`. No warning, no cap; dialog copy says "Record what they paid you back".

#### H8 — `/verify-otp` and `/reset-password` are blank dead ends when opened directly ✅ fixed (code; manual retest pending)
- **Steps:** load `http://localhost:5173/verify-otp` (or `/reset-password`) with no router state (e.g. refresh after being sent there).
- **Actual:** empty white page, URL unchanged, nothing to tap. Source: `if (!email) { navigate(...); return null; }` — `navigate()` is called **during render**, which does not redirect. Use `<Navigate>` or an effect.

#### H9 — "Reset to Defaults" leaves the app in an unrecoverable half-state ✅ fixed (code; manual retest pending)
- After Reset: 0 accounts, `onboardedAt` still set (no onboarding), user name replaced by `User`. Dashboard says "Add your first account", but **Add Transaction gives no feedback at all** (button disabled, no message), and "Explore with sample data" can no longer be reached. The confirm dialog also doesn't say that the name/onboarding state are wiped.

---

### 3.2 Medium — ✅ all fixed (code; manual retest pending)

| # | Finding | Detail |
| --- | --- | --- |
| M1 ✅ | **Rapid multi-click on "Add Transaction" creates duplicates** | 3 quick clicks → 3 transactions + 3 toasts. No in-flight guard on submit. Likely the same on other forms. |
| M2 ✅ | **CSV export is vulnerable to formula injection** | A note `=HYPERLINK("http://evil.example","click")` is exported verbatim (`…,"=HYPERLINK(""http://evil.example"",""click"")",…`). Notes can be attacker-controlled via bank CSV import or the Share Target. Prefix cells starting with `= + - @` with `'`. |
| M3 ✅ | **Import "Replace everything" is offered when nothing valid will be imported** | `{"accounts":"x","transactions":[{"id":1}]}` → Review dialog shows `Accounts 0 / Transactions 0 (1 skipped)` and still offers **Replace everything** (wipe). `validateBackup` only requires keys to be *present* ([`importValidation.ts:777`](src/utils/importValidation.ts:777)). |
| M4 ✅ | **Raw JS error shown to users on bad import** | Non-JSON file → `Unexpected token 'h', "this is not json" is not valid JSON`. |
| M5 ✅ | **Year in Review: navigating to an empty year removes the year navigation** | Prev-year ×1 → "Add some transactions to see a year in review." with **no prev/next controls** (not even a year label). Only browser Back/reload escapes. Header alignment also differs from other pages. |
| M6 ✅ | **Analytics custom-range popover overflows the viewport** | At 411 px the two-month calendar starts at x=276 and ends at x=488 (viewport 411) — right half unreachable. |
| M7 ✅ | **Dashboard Savings-Goals card overflows with a long unbroken name** | A 120-char goal name runs past the card edge on the dashboard (the Goals page truncates correctly). |
| M8 ✅ | **Insights/forecast are misleading early in a period** | Day 1 of October with a single ₹5,075 adjustment: "Overall spending is on pace to go over — you'll spend ₹1.6L against ₹40,000", "Miscellaneous is 100 % of this month's spending", "Food is down 100 % on your average", "Housing −100 %". "THIS MONTH" label is shown even when a different period chip is selected. |
| M9 ✅ | **Forecast double-counts recurring-looking spend** | Sample data's historic "Monthly rent" rows have no `recurringId`, so the forecast reports "Everyday spend ₹850.57/day, led by **Housing**…" *and* lists Monthly rent ₹15,000 as scheduled. Same happens to any user who logged rent manually before creating a rule. |
| M10 ✅ | **`BalanceTrend` emits duplicate x-axis keys** | Console: `Encountered two children with the same key … Oct 26` ([`BalanceTrend.tsx:63,71`](src/components/charts/BalanceTrend.tsx:63): last monthly bucket and the appended end-point share the label). Affects every >90-day range; also uses calendar months, not the financial month. |
| M11 ✅ | **Silent disabled submit buttons — no message says what is missing** | Add Account (empty name, FD without rate/dates, maturity ≤ start, negative/>100 % rate) is correctly blocked, but the button just looks washed-out. Add Transaction with ₹0 or no account does nothing and shows nothing, while a missing category shows a toast — three different behaviours on one form. |
| M12 ✅ | **Duplicate names accepted silently** | Two accounts named "HDFC Savings" (a transfer reads `HDFC Savings → HDFC Savings`); a second category "food" next to "Food" with no warning and no success toast. |
| M13 ✅ | **Invalid `:id` on edit routes shows the *Add* form** | `/edit-account/bogus`, `/edit-transaction/bogus`, `/edit-loan/bogus` render "Add Account/…" under an edit URL; saving creates a new record. Should 404/redirect. |
| M14 ✅ | **Archive and Delete icons sit side-by-side and the list shifts after Archive** | After archiving "Savings" the cursor lands on the next row's red Delete icon (hover state visible). A second tap = delete-confirm dialog. Small (~28 px) targets. |
| M15 ✅ | **"Mark Paid Off" has no confirmation, toast or Undo** | One tap silently closes the loan, pauses its EMI rule and moves it to a collapsed "Paid off (1)" group. |
| M16 ✅ | **Desktop users can't reach key actions** | Row actions (Select / Duplicate / Save as template / Delete) and Templates are long-press only — no right-click, kebab or tooltip. PIN pad, amount pad and onboarding pad ignore the physical keyboard (typed `9999`, `12345.678.9`: nothing registered). |
| M17 ✅ | **Core features are buried** | Budgets, Recurring, Goals, Debts, Loans, Merchants, Year in Review live only in a long Settings list (sidebar and tab bar don't list them). |

---

### 3.3 Low — UI polish, copy, consistency

**Formatting & copy**
- L1 Inconsistent decimals: `₹450.5`, `₹1,306.5`, `-₹96,85,388.5`, `₹43,523.7` (hero number, list rows, forecast) vs `₹1,290.32` elsewhere — `maximumFractionDigits: 2` with `minimumFractionDigits: 0` ([`formatters.ts:41`](src/utils/formatters.ts:41)). Currency should pad to 2 when fractional.
- L2 Grammar: with one extra item the banner reads "1 more thing **need** attention this week" (`Dashboard.tsx:402` only pluralises "thing", never the verb → should be "needs").
- L3 Four date formats in use: `05 October 2026`, `5 Oct 2026`, `Oct 5, 2026`, `Sun, 27 Sep`, `October 1st, 2026`.
- L4 Compact amounts lose precision: Accounts "Net balance ₹1.7L" vs dashboard ₹1,65,075.
- L5 "Sign In — Sync your data across devices" while the product (and Privacy Policy) is backup, not sync.
- L6 "₹1,00,000" vs "₹1L" mixed within one goal card ("₹20,000 / ₹1L").
- L7 Split form says "Fully allocated" when rows are `-500 + 1500`; submit then fails with the generic "Fill in every split row" instead of "amounts must be positive".

**Layout**
- L8 Dashboard "Total balance" card shows a divider and empty space when a budget is set and the month has no transactions ([`Dashboard.tsx:316`](src/pages/Dashboard.tsx:316)).
- L9 Toasts render over the page header/primary buttons on mobile and sit half-clipped at the top.
- L10 The FAB overlaps right-aligned values (Compare-Periods "‑100 %", list amounts) on every Layout page, including Accounts where a header "+" already exists.
- L11 Category picker is a 12-tile inner scroller (32 categories, hidden scrollbar) — easy to miss that more exist.
- L12 Charts: y-axis labels clipped on the left ("₹4,500" loses the ₹), single data point renders as a lone dot with no empty-state, bar gradient contradicts the "no gradients" rule in design.md.
- L13 Credit-card add form stacks two full number pads (Current Due + Credit Limit) — ~2 screens of scrolling.
- L14 Income category list includes "Transfer", and "Gifts"/"Rent" appear in both type lists; pickers like Rules' "File into" show duplicates with no type hint.

**Validation / behaviour**
- L15 Goals accept a target date in the past (no overdue state). Filters accept From > To with no hint. Budget scope select defaults to an already-used scope ("Overall Expenses already has a budget" only on Save).
- L16 Add Loan: category not preselected (a "Loan / EMI" category exists), no success toast (Add Account/Transaction both toast).
- L17 CSV import: date format defaults to `YYYY-MM-DD` with no auto-detect; amount `1e3` is parsed as ₹13; a negative value in a Debit column is rejected as "no debit or credit amount"; amounts print as `-₹450.5`.
- L18 Calendar pickers have no year/month jump — an FD maturing in 5 years needs 60 "next month" clicks.
- L19 App-lock: stale "Incorrect PIN" text remains after the lockout expires; default auto-lock of 1 minute is aggressive; a reload within the grace window does **not** lock (by design, but unexpected).
- L20 Name field keeps leading whitespace (`"    <img…`), no max length anywhere (name, account, goal, note).
- L21 Register has no confirm-password or show-password toggle; Login/Register do no client-side email check.
- L22 Backup JSON has no schema version / export timestamp.

---

### 3.4 Accessibility

- Icon-only buttons with **no accessible name**: every back arrow, the number-pad backspace (all pads), all Edit/Delete icons on Categories (68 buttons), Budgets' Edit/Delete (labelled but identical on every card).
- Document `<title>` is the same on every route.
- Reminders toggle shows "Blocked in your browser settings" but gives no way/link to fix it.
- Charts have a data-table fallback ✔ (good); Recharts' off-screen measuring `<span>` ("Jan") leaks into `innerText` on every page.

---

## 4. Verified working (negative tests that passed)

- Onboarding blocks empty/whitespace names; amount pad caps at 2 decimals, ignores a 2nd "·", caps at ₹99,99,99,999.
- XSS in the user name (`<img onerror>`) is rendered as text everywhere.
- Search is safe for `(`, `[a-`, `.*`, emoji, `<b>`; case-insensitive.
- Transfer picker excludes the source account; delete-account confirm states the transaction count and offers Archive; deleting an account that is an FD payout target is blocked with a clear toast.
- Delete → Undo restores both the row and the balance; Reconcile posts an adjustment with Undo; recurring backfill preview ("Add past transactions?") and "Start from today" work; budget form rejects 0 and duplicates; rule form flags an invalid regex inline.
- App Lock: PIN mismatch rejected, back-off after 5 wrong PINs, "Forgot PIN" copy is honest about data, disabling requires the PIN.
- CSV import: bad-date rows reported per row, duplicate detection on re-import; JSON export → import (Replace) round-trips balances, goals, loans, recurring and snapshots exactly.
- Hide Amounts masks every screen (incl. chart data tables); month-start day = 25 is honoured by Dashboard, Budgets, Analytics and Transactions.
- No page-level horizontal overflow on any of 24 routes at 320 px; dark mode is legible throughout.
- Share Target extracts the amount from SMS text (`Paid Rs. 499.50 to Zomato` → 499.5).

---

## 5. Test-data notes

The browser profile now contains my test data (sample dataset plus abusive entries: ₹99,99,999 prepayment, ₹99,999 settle-up, `=HYPERLINK` note, duplicate "food" category, a closed car loan, an FD). Clear site data for `localhost:5173` or use **Settings → Reset to Defaults** before further manual QA. Some early "duplicate goal/person" rows on the dashboard were caused by my own hand-editing of `localStorage` between sample-data loads, not by the app.
