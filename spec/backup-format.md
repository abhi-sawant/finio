# Finio backup format — the contract between web/ and android/

Both clients read and write this format. **A file exported by one client must import into the
other unchanged.** The web implementation is the reference:
`web/src/services/backup.ts` (what goes in), `web/src/utils/backupMeta.ts` (file metadata),
`web/src/utils/importValidation.ts` (what is accepted), `web/src/utils/backupCrypto.ts` (cloud
encryption). The Android ports live under `android/core/.../backup`, `.../crypto` and
`.../importing`, and are held to the web behaviour by the golden fixtures in `spec/fixtures/`.

Changing anything below means changing both clients and regenerating the fixtures
(`cd web && npm run gen:fixtures`) in the same PR.

---

## 1. Payload

A JSON object with these 16 keys, in this order:

| Key                 | Type                     | Notes                                                    |
| ------------------- | ------------------------ | -------------------------------------------------------- |
| `accounts`          | `Account[]`              | `openingBalance` may be absent in pre-v5 data (backfilled) |
| `transactions`      | `Transaction[]`          |                                                          |
| `categories`        | `Category[]`             |                                                          |
| `labels`            | `Label[]`                |                                                          |
| `budgets`           | `Budget[]`               |                                                          |
| `recurring`         | `RecurringTransaction[]` | `lastRunDate` is written as `null` when never run         |
| `templates`         | `TransactionTemplate[]`  |                                                          |
| `rules`             | `CategoryRule[]`         | array order **is** rule priority                         |
| `goals`             | `Goal[]`                 |                                                          |
| `goalContributions` | `GoalContribution[]`     |                                                          |
| `people`            | `Person[]`               |                                                          |
| `debtEntries`       | `DebtEntry[]`            |                                                          |
| `netWorthSnapshots` | `NetWorthSnapshot[]`     | identity is `periodKey`, not `id`                        |
| `loans`             | `Loan[]`                 |                                                          |
| `loanPrepayments`   | `LoanPrepayment[]`       |                                                          |
| `settings`          | `Settings`               |                                                          |

Field-level shapes are the interfaces in `web/src/types/index.ts`, mirrored 1:1 by
`android/core/.../model/Model.kt`. Conventions:

- **Ids** are opaque strings (v4 UUIDs for anything minted at runtime; `cat-N` / `lbl-N` for
  defaults). Never re-mint an id on import or undo.
- **Dates** are ISO-8601 strings. Timestamps are UTC with milliseconds
  (`2026-10-05T14:30:00.000Z`, i.e. `Date.prototype.toISOString()`). Date-only values are never
  stored. All period, day and month logic interprets them in the device's local zone.
- **Money** is a JSON number in rupees with at most 2 decimals (paise), rounded the way
  `roundMoney` rounds. There is no currency field: INR only.
- **Absent optionals are omitted**, not `null`. That rule has one exception:
  `RecurringTransaction.lastRunDate`, which is explicitly `null` until the rule first runs.
  Readers must treat a missing key and `null` the same.
- **Split transactions** have `categoryId: ""` and a `splits` array of at least 2 entries that
  sum to `amount` within 0.01.
- **Never in the payload:** app-lock config (PIN hash), backup-encryption config, the cloud auth
  token, or `lastLocalBackupAt`. They live in their own per-device stores, so restoring someone
  else's backup can't install their PIN or key.

## 2. Local file export

`finio-backup-YYYY-MM-DD.json`, pretty-printed with 2-space indentation. Two metadata keys come
first, followed by the payload keys:

```json
{ "version": 1, "exportedAt": "2026-10-05T14:30:00.000Z", "accounts": [], "…": "…", "settings": {} }
```

`version` is `BACKUP_SCHEMA_VERSION` (currently **1**). A reader warns, but still imports, when a
file's version is newer than it understands.

## 3. Cloud upload (`POST /backup/upload`)

- **Unencrypted:** the raw payload, with **no** `version` or `exportedAt` keys.
- **Encrypted (E2EE):** an envelope, so the server only ever stores ciphertext:

```json
{
  "v": 1,
  "enc": true,
  "kdf": "PBKDF2-SHA256",
  "iterations": 600000,
  "salt": "<base64url, 16 bytes>",
  "iv": "<base64url, 12 bytes>",
  "ciphertext": "<base64url of AES-GCM output>"
}
```

How the envelope is built:

1. Derive the key: PBKDF2-HMAC-SHA256 over the UTF-8 passphrase, using `salt` and `iterations`,
   producing 256 bits for AES-256-GCM.
2. The plaintext is the UTF-8 bytes of `JSON.stringify(payload)`.
3. `ciphertext` is the AES-GCM output with the 128-bit tag **appended**. WebCrypto produces this
   layout natively; on the JVM it is `AES/GCM/NoPadding` with `GCMParameterSpec(128, iv)`.
4. Every value above is base64url with no padding.

Restoring an envelope on another device:

- The reader derives the key from the passphrase together with the envelope's own `salt` and
  `iterations`, so a fresh device needs nothing except the passphrase.
- A failed GCM authentication check means the passphrase was wrong.
- Once the restore succeeds, the device adopts that salt as its local encryption config.

**Passphrase verifier.** The local config stores the encryption of a known plaintext so a
passphrase can be checked before any backup exists. That plaintext is the JSON string
`"finio-backup-verify-v1"`, quotes included, encrypted with its own random IV:
`{verifierIv, verifierCiphertext}`.

**Restore flow (both clients):** decode (plaintext passes through; envelope → decrypt) →
`validateBackup` → `importData(…, mode: "replace")`.

## 4. Validation (`validateBackup`)

These rules apply to every import, whether it comes from a file or the cloud.

**Whole-file rejection**
- The input must be an object containing at least one known collection or `settings`.
  Otherwise the reader throws "Not a Finio backup file".

**Row handling**
- A collection that isn't an array is skipped and reported as an issue.
- A malformed row is dropped with a reason.
- A row whose id duplicates an earlier one is dropped.
- At most 8 issues are listed, followed by "…and N more".

**Defaults applied when a field is missing**

| Field | Default |
|---|---|
| Account color | `#146b54` |
| Account icon | `landmark` |
| Category icon | `circle-ellipsis` |
| Category color | `#94a3b8` |
| Label color | `#64748b` |
| Budget period | monthly |
| Recurring `occurrenceCount` | 0 |
| Rule scope | `any` |
| Transaction `createdAt` | its `date` |

**Per-field rules**
- **Account:** an FD needs a valid `maturityDate`; an RD needs `tenureMonths` ≥ 1.
- **Transaction:**
  - `amount` must be ≥ 0.
  - A transfer needs `toAccountId`.
  - `splits` are kept only on expenses that pass the split rule in §1.
- **Budget:** `amount` > 0. `rollover` is true only when the file says exactly `true`.
- **Recurring:**
  - `maxOccurrences` is truncated and must be ≥ 1.
  - `toAccountId` is kept only on transfers.
- **Rule:** an invalid regex rejects the rule. `enabled` is true unless the file says exactly
  `false`.
- **Goal:** `targetAmount` > 0.
- **Contribution and debt entry:** amount ≠ 0.
- **Snapshot:** `periodKey` must match `^\d{4}-(0[1-9]|1[0-2])$`.
- **Loan:** principal > 0, rate ≥ 0, tenure > 0 (truncated).
- **Prepayment:** amount > 0.
- **Settings:**
  - Only known keys are kept; an invalid value falls back to its default.
  - `amoledDark` (boolean, optional, default `false`) switches dark mode to true black; it only has an effect while the resolved theme is dark.
  - `monthStartDay` is clamped to 1–28 and `notifyLeadDays` to 0–7.
  - `onboardedAt` is **dropped**, so restoring never re-runs or skips this device's onboarding.

**Warnings (the data is still imported)**
- Orphan references: transaction or recurring → account; budget → category or label;
  rule → category; contribution → goal; recurring → goal; debt → person;
  deposit → linked account; loan → account or category; prepayment → loan.
- Accounts with no opening balance.
- A newer file `version`.

## 5. Import modes (`importData`)

- **merge:** each collection becomes the union of existing and incoming rows by id, and the
  incoming row wins.
- **replace:** every collection the file provides is swapped in. Collections the file omits
  become empty, **except** `categories` and `labels`, which keep the current set.

Steps that run after both modes:
1. Dedupe snapshots by `periodKey`; the later `createdAt` wins.
2. Backfill missing opening balances.
3. Recompute every balance.
4. Merge settings, dropping the legacy `currency` key.

## 6. Ids that must be byte-identical across clients

Reminder ids are a dedupe contract. The schedule is rebuilt on every launch, so these ids must
come out the same every time:

- `bill:{ruleId}:{yyyy-MM-dd}`
- `budget:{budgetId}:{periodStartISO}:{near|over}`
- `credit:{accountId}:{dueDayKey}`
- `daily:log:{yyyy-MM-dd}`

The authoritative builder is `buildNotificationSchedule` (`web/src/utils/notificationSchedule.ts`).

## 7. PIN hashing (device-local, never in a backup)

PBKDF2-HMAC-SHA256 with **310,000** iterations, a 16-byte random salt and a 256-bit output, all
base64url without padding. PINs are 4–8 ASCII digits; setup offers 4 or 6. Comparison is
timing-safe. The config isn't portable between clients and doesn't need to be.
