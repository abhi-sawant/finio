import { differenceInMonths } from 'date-fns';
import type { Account, DepositTerms } from '@/types';
import {
  DEPOSIT_COMPOUNDING_OPTIONS,
  accountDeleteBlockers,
  accountDisplayValue,
  depositCaption,
  depositCurrentValue,
  depositInvested,
  depositMaturityAmount,
  depositMaturityDate,
  depositValueAt,
  isDepositAccount,
  planMaturities,
  rdInstallmentDate,
  rdInstallmentsOnOrBefore,
} from '@/utils/deposit';
import { at, gc, type GoldenCase } from '../golden';

// Local-midnight dates, the same shape the forms store.
const iso = (day: string) => new Date(`${day}T00:00:00`).toISOString();

function deposit(
  type: 'fd' | 'rd',
  terms: Partial<DepositTerms>,
  extra: Partial<Account> = {},
): Account {
  return {
    id: `${type}-1`,
    name: type.toUpperCase(),
    type,
    color: '#000',
    icon: 'vault',
    balance: 0,
    openingBalance: 0,
    createdAt: iso('2026-01-01'),
    deposit: {
      amount: 50000,
      interestRate: 6.65,
      startDate: iso('2026-10-01'),
      linkedAccountId: 'bank',
      ...terms,
    },
    ...extra,
  };
}
const fd = (terms: Partial<DepositTerms> = {}, extra: Partial<Account> = {}) =>
  deposit('fd', { maturityDate: iso('2029-09-01'), ...terms }, extra);
const rd = (terms: Partial<DepositTerms> = {}, extra: Partial<Account> = {}) =>
  deposit('rd', { tenureMonths: 33, ...terms }, extra);

const bank: Account = {
  ...fd(),
  id: 'bank',
  name: 'Bank',
  type: 'savings',
  balance: 1234.5,
  deposit: undefined,
};

const accounts: [string, Account][] = [
  ['fd', fd()],
  ['fd-1y-monthly', fd({ maturityDate: iso('2027-10-01'), compounding: 'monthly' })],
  ['fd-1y-quarterly', fd({ maturityDate: iso('2027-10-01'), compounding: 'quarterly' })],
  ['fd-1y-half', fd({ maturityDate: iso('2027-10-01'), compounding: 'half-yearly' })],
  ['fd-1y-yearly', fd({ maturityDate: iso('2027-10-01'), compounding: 'yearly' })],
  ['fd-1y-simple', fd({ maturityDate: iso('2027-10-01'), compounding: 'simple' })],
  [
    'fd-leap',
    fd({ startDate: iso('2024-02-29'), maturityDate: iso('2025-02-28'), interestRate: 7.1 }),
  ],
  [
    'fd-46d',
    fd({
      startDate: iso('2026-01-31'),
      maturityDate: iso('2026-03-18'),
      interestRate: 3.5,
      amount: 10000.5,
    }),
  ],
  ['fd-no-maturity', fd({ maturityDate: undefined })],
  ['fd-bad-start', fd({ startDate: 'not-a-date' })],
  ['fd-maturity-before-start', fd({ maturityDate: iso('2026-01-01') })],
  ['fd-matured', fd({ maturedAt: iso('2029-09-01') })],
  ['fd-matured-empty', fd({ maturedAt: '' })],
  ['fd-rate-int', fd({ interestRate: 7 })],
  ['fd-rate-long', fd({ interestRate: 0.1 + 0.2 })],
  ['fd-zero-rate', fd({ interestRate: 0 })],
  ['rd', rd()],
  [
    'rd-month-end',
    rd({ startDate: iso('2026-01-31'), tenureMonths: 13, amount: 2500, interestRate: 7.25 }),
  ],
  ['rd-utc-start', rd({ startDate: '2026-03-31T00:00:00.000Z', tenureMonths: 6 })],
  ['rd-no-tenure', rd({ tenureMonths: undefined })],
  ['rd-one', rd({ tenureMonths: 1 })],
  ['rd-matured', rd({ maturedAt: iso('2029-07-01') })],
  ['rd-other-link', rd({ linkedAccountId: 'other' })],
  ['savings', bank],
  ['savings-with-terms', { ...fd(), id: 'odd', type: 'savings' }],
];

const dates = [
  iso('2026-09-30'),
  iso('2026-10-01'),
  iso('2026-10-15'),
  iso('2026-11-01'),
  iso('2026-12-01'),
  iso('2027-02-15'),
  iso('2027-02-28'),
  iso('2027-03-31'),
  iso('2027-10-01'),
  iso('2028-02-29'),
  iso('2029-06-30'),
  iso('2029-07-01'),
  iso('2029-08-31'),
  iso('2029-09-01'),
  iso('2031-01-01'),
  '2026-03-17T23:59:59.999Z',
  '2026-02-28T18:30:00.000Z',
  iso('2025-01-15'),
  iso('2026-04-30'),
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('DEPOSIT_COMPOUNDING_OPTIONS', [], DEPOSIT_COMPOUNDING_OPTIONS));

  // date-fns differenceInMonths — the Kotlin port re-implements its JS setMonth overflow quirks.
  const dm = [
    at(2014, 1, 31),
    at(2014, 9, 1),
    at(2026, 1, 31),
    at(2026, 2, 28),
    at(2026, 2, 28, 12),
    at(2026, 3, 1),
    at(2026, 3, 28),
    at(2026, 3, 30),
    at(2026, 3, 31),
    at(2026, 4, 30),
    at(2024, 2, 29),
    at(2025, 2, 28),
    at(2024, 1, 31, 23, 59),
    at(2024, 3, 31),
    at(2026, 10, 1),
    at(2026, 9, 30, 23, 59, 59, 999),
    at(2026, 12, 31),
    at(2027, 1, 1),
  ];
  for (const a of dm)
    out.push(
      gc(
        'differenceInMonths[]',
        [a, dm],
        dm.map((b) => differenceInMonths(a, b)),
      ),
    );

  for (const [name, account] of accounts) {
    out.push(gc('isDepositAccount', [account.type], isDepositAccount(account), name));
    out.push(gc('depositMaturityDate', [account], depositMaturityDate(account), name));
    out.push(gc('depositInvested', [account], depositInvested(account), name));
    out.push(gc('depositMaturityAmount', [account], depositMaturityAmount(account), name));
    out.push(gc('depositCaption', [account], depositCaption(account), name));
    // One case per account over the whole date grid (out[i] ↔ dates[i]) keeps the file small.
    const each = <T>(f: (d: Date) => T) => dates.map((d) => f(new Date(d)));
    out.push(
      gc(
        'depositValueAt[]',
        [account, dates],
        each((d) => depositValueAt(account, d)),
        name,
      ),
    );
    out.push(
      gc(
        'depositCurrentValue[]',
        [account, dates],
        each((d) => depositCurrentValue(account, d)),
        name,
      ),
    );
    out.push(
      gc(
        'accountDisplayValue[]',
        [account, dates],
        each((d) => accountDisplayValue(account, d)),
        name,
      ),
    );
    if (account.deposit) {
      const terms = account.deposit;
      out.push(
        gc(
          'rdInstallmentsOnOrBefore[]',
          [terms, dates],
          each((d) => rdInstallmentsOnOrBefore(terms, d)),
          name,
        ),
      );
      for (const i of [0, 1, 4, 12, 32, 33, -1]) {
        out.push(
          gc(
            'rdInstallmentDate',
            [account.deposit, i],
            rdInstallmentDate(account.deposit, i),
            name,
          ),
        );
      }
    }
  }
  // Pick<Account, 'type' | 'deposit'> — the Add Account form's preview, which is not an account.
  const preview = { type: 'rd' as const, deposit: rd().deposit };
  out.push(gc('depositMaturityAmount', [preview], depositMaturityAmount(preview), 'preview'));

  const all = accounts.map(([, a]) => a);
  for (const now of [
    iso('2029-08-31'),
    iso('2029-09-01'),
    iso('2027-10-01'),
    iso('2035-01-01'),
    iso('2020-01-01'),
  ]) {
    out.push(
      gc(
        'planMaturities',
        [all, now],
        planMaturities(all, new Date(now)).map((a) => a.id),
      ),
    );
    out.push(
      gc(
        'planMaturities',
        [[bank, fd()], now],
        planMaturities([bank, fd()], new Date(now)).map((a) => a.id),
      ),
    );
    out.push(
      gc(
        'planMaturities',
        [[fd()], now],
        planMaturities([fd()], new Date(now)).map((a) => a.id),
      ),
    );
  }
  for (const id of ['bank', 'other', 'nobody', 'fd-1']) {
    out.push(gc('accountDeleteBlockers', [all, id], accountDeleteBlockers(all, id)));
  }
  const selfLinked = fd({ linkedAccountId: 'fd-1' });
  out.push(
    gc(
      'accountDeleteBlockers',
      [[selfLinked], 'fd-1'],
      accountDeleteBlockers([selfLinked], 'fd-1'),
    ),
  );
  return out;
}
