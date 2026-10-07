import type { Account, RecurringTransaction, Transaction, TransactionType } from '@/types';
import {
  DEFAULT_FORECAST_DAYS,
  DEFAULT_LOOKBACK_DAYS,
  buildCashFlowCalendarMonth,
  buildCashFlowForecast,
  categoryDailyAverages,
  liquidAccountIds,
  liquidBalance,
  liquidDelta,
  type ForecastInput,
} from '@/utils/forecast';
import { periodRange, shiftPeriod } from '@/utils/period';
import { at, gc, type GoldenCase } from '../golden';
import { L, NOW, buildLedger } from './calculations.fixture';

// Cash-flow forecast over the shared fixture ledger plus the scenarios from forecast.test.ts.
// Extra recurring rules (salary, weekly, daily, yearly, paused, ended, capped, card, internal
// transfer) exercise every scheduling branch.

const T_NOW = new Date('2026-06-15T12:00:00.000Z');

function account(partial: Partial<Account> & Pick<Account, 'id'>): Account {
  return {
    name: partial.id,
    type: 'checking',
    color: '#000',
    icon: 'landmark',
    balance: 0,
    openingBalance: 0,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...partial,
  };
}

function tx(partial: Partial<Transaction> & Pick<Transaction, 'type' | 'amount' | 'date'>): Transaction {
  return {
    id: partial.id ?? `${partial.type}-${partial.amount}-${partial.date}`,
    accountId: 'checking',
    categoryId: 'cat-food',
    note: '',
    labels: [],
    createdAt: partial.date,
    ...partial,
  };
}

function rule(partial: Partial<RecurringTransaction> & Pick<RecurringTransaction, 'id' | 'amount'>): RecurringTransaction {
  return {
    type: 'expense',
    accountId: 'checking',
    categoryId: 'cat-bills',
    note: 'Rent',
    labels: [],
    frequency: 'monthly',
    startDate: '2026-06-20T00:00:00.000Z',
    occurrenceCount: 0,
    lastRunDate: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...partial,
  };
}

const ACCOUNTS = [
  account({ id: 'checking', balance: 10000 }),
  account({ id: 'card', type: 'credit', balance: -3000, creditLimit: 50000 }),
  account({ id: 'closed', balance: 999, archivedAt: '2026-02-01T00:00:00.000Z' }),
];

/** Rules over the shared ledger's accounts. */
function ledgerRules(): RecurringTransaction[] {
  const r = (partial: Partial<RecurringTransaction> & Pick<RecurringTransaction, 'id' | 'amount'>) =>
    rule({ accountId: 'acc-bank', ...partial });
  return [
    r({ id: 'r-salary', amount: 85000, type: 'income', note: 'Salary ACME Corp', categoryId: 'cat-9', startDate: '2025-11-25T03:30:00.000Z', lastRunDate: '2026-05-25T03:30:00.000Z', occurrenceCount: 7 }),
    r({ id: 'r-rent', amount: 22000, note: 'Rent', categoryId: 'cat-28', startDate: '2026-07-01T03:30:00.000Z' }),
    r({ id: 'r-elec', amount: 1450.55, note: 'BESCOM electricity', categoryId: 'cat-5', startDate: '2026-06-12T05:30:00.000Z', occurrenceCount: 1, lastRunDate: '2026-06-12T05:30:00.000Z' }),
    r({ id: 'r-milk', amount: 56.5, note: 'Milk', categoryId: 'cat-25', frequency: 'daily', startDate: '2026-06-18T01:30:00.000Z', accountId: 'acc-cash' }),
    r({ id: 'r-gym', amount: 299, note: 'Cult.fit class', categoryId: 'cat-29', frequency: 'weekly', startDate: '2026-06-22T01:30:00.000Z', maxOccurrences: 4 }),
    r({ id: 'r-ins', amount: 24000, note: 'Insurance', categoryId: 'cat-5', frequency: 'yearly', startDate: '2025-08-01T03:30:00.000Z', occurrenceCount: 1, lastRunDate: '2025-08-01T03:30:00.000Z' }),
    r({ id: 'r-paused', amount: 999, note: 'Paused', startDate: '2026-06-20T03:30:00.000Z', pausedAt: '2026-06-01T00:00:00.000Z' }),
    r({ id: 'r-ended', amount: 500, note: 'Ended', frequency: 'weekly', startDate: '2026-06-19T03:30:00.000Z', endDate: '2026-07-05T00:00:00.000Z' }),
    r({ id: 'r-card', amount: 649, note: 'NETFLIX.COM', categoryId: 'cat-18', accountId: 'acc-card', startDate: '2026-07-07T00:30:00.000Z' }),
    r({ id: 'r-cardpay', amount: 9000, type: 'transfer', note: 'Amex bill payment', categoryId: 'cat-13', toAccountId: 'acc-card', startDate: '2026-06-22T04:30:00.000Z' }),
    r({ id: 'r-internal', amount: 10000, type: 'transfer', note: 'To savings', categoryId: 'cat-13', toAccountId: 'acc-savings', startDate: '2026-07-02T02:30:00.000Z' }),
    r({ id: 'r-refund', amount: 100, type: 'income', note: 'Card cashback', categoryId: 'cat-23', accountId: 'acc-card', startDate: '2026-06-25T00:00:00.000Z' }),
    r({ id: 'r-sameday', amount: 2000, note: 'Same day as rent', categoryId: 'cat-1', startDate: '2026-07-01T10:30:00.000Z' }),
  ];
}

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const { accounts, transactions: txns } = ledger;
  const rules = ledgerRules();
  const allRules = [...ledger.recurring, ...rules];
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  out.push(gc('constants', [], { DEFAULT_FORECAST_DAYS, DEFAULT_LOOKBACK_DAYS }));

  // ── Liquidity ─────────────────────────────────────────────────────────────
  for (const accs of [accounts, ACCOUNTS, []]) {
    const arg = accs === accounts ? L('accounts') : accs;
    out.push(gc('liquidAccountIds', [arg], [...liquidAccountIds(accs)]));
    out.push(gc('liquidBalance', [arg], liquidBalance(accs)));
  }
  const liquid = liquidAccountIds(accounts);
  const ids = ['acc-bank', 'acc-savings', 'acc-card', 'acc-rd', 'acc-old', 'nope'];
  for (const type of ['expense', 'income', 'transfer'] as TransactionType[]) {
    for (const accountId of ids) {
      for (const toAccountId of type === 'transfer' ? [...ids, '', undefined] : [undefined]) {
        const item = { type, amount: 123.45, accountId, toAccountId };
        out.push(gc('liquidDelta', [item, L('accounts')], liquidDelta(item, liquid)));
      }
    }
  }

  // ── categoryDailyAverages ─────────────────────────────────────────────────
  const nows = [NOW, at(2026, 6, 1, 0, 0), at(2025, 11, 2), at(2026, 9, 30, 12), at(2025, 6, 1)];
  for (const now of nows) {
    for (const lookbackDays of [undefined, 1, 7, 30, 365, 0, -5]) {
      for (const recurring of [undefined, 'ledger', 'all'] as const) {
        const options = {
          now,
          lookbackDays,
          recurring: recurring === undefined ? undefined : recurring === 'ledger' ? ledger.recurring : allRules,
        };
        out.push(
          gc(
            'categoryDailyAverages',
            [L('transactions'), L('accounts'), { now, lookbackDays, recurring: recurring === 'ledger' ? L('recurring') : recurring === 'all' ? allRules : undefined }],
            categoryDailyAverages(txns, accounts, options),
          ),
        );
      }
    }
  }
  const rent = rule({ id: 'rule-rent', amount: 5000, note: 'Rent' });
  const avgRows = [
    tx({ type: 'expense', amount: 100, date: '2026-06-14T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 100, date: '2026-06-15T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 5000, date: '2026-06-10T00:00:00.000Z', note: 'Rent', categoryId: 'cat-bills' }),
    tx({ type: 'expense', amount: 5100, date: '2026-06-09T00:00:00.000Z', note: 'RENT 0601', categoryId: 'cat-bills', id: 'rent-close' }),
    tx({ type: 'expense', amount: 5000, date: '2026-06-15T00:00:00.000Z', recurringId: 'rule-rent', id: 'gen' }),
    tx({ type: 'expense', amount: 700, date: '2026-06-15T00:00:00.000Z', accountId: 'card', id: 'c1' }),
    tx({ type: 'expense', amount: 700, date: '2026-06-15T00:00:00.000Z', accountId: 'closed', id: 'c2' }),
    tx({ type: 'expense', amount: 300, date: '2026-06-15T00:00:00.000Z', categoryId: '', id: 'split', splits: [{ categoryId: 'cat-food', amount: 200 }, { categoryId: 'cat-home', amount: 100 }] }),
    tx({ type: 'expense', amount: 42, date: 'not a date', id: 'bad' }),
    tx({ type: 'expense', amount: 42, date: '2026-06-16T00:00:00.000Z', id: 'future' }),
    tx({ type: 'income', amount: 42, date: '2026-06-15T00:00:00.000Z', id: 'inc' }),
  ];
  for (const recurring of [undefined, [rent], [{ ...rent, pausedAt: '2026-06-01T00:00:00.000Z' }], [{ ...rent, pausedAt: '' }], [{ ...rent, type: 'income' as const }]]) {
    out.push(
      gc('categoryDailyAverages', [avgRows, ACCOUNTS, { now: T_NOW, recurring }], categoryDailyAverages(avgRows, ACCOUNTS, { now: T_NOW, recurring })),
    );
  }
  out.push(gc('categoryDailyAverages', [[], ACCOUNTS, { now: T_NOW }], categoryDailyAverages([], ACCOUNTS, { now: T_NOW })));

  // ── buildCashFlowForecast ─────────────────────────────────────────────────
  const forecastNows = [NOW, at(2026, 6, 30, 23, 59), at(2026, 7, 1, 0, 0)];
  for (const now of forecastNows) {
    for (const days of [undefined, 1, 30, 0]) {
      for (const lookbackDays of [undefined, 14]) {
        const input: ForecastInput = { accounts, transactions: txns, recurring: allRules, now, days, lookbackDays };
        out.push(
          gc(
            'buildCashFlowForecast',
            [{ accounts: L('accounts'), transactions: L('transactions'), recurring: allRules, now, days, lookbackDays }],
            buildCashFlowForecast(input),
          ),
        );
      }
    }
  }
  out.push(
    gc(
      'buildCashFlowForecast',
      [{ accounts: L('accounts'), transactions: L('transactions'), recurring: L('recurring'), now: NOW, days: 365 }],
      buildCashFlowForecast({ accounts, transactions: txns, recurring: ledger.recurring, now: NOW, days: 365 }),
    ),
  );
  const testInputs: ForecastInput[] = [
    { accounts: ACCOUNTS, transactions: [], recurring: [], now: T_NOW, days: 30 },
    {
      accounts: [account({ id: 'checking', balance: 10000 })],
      transactions: [tx({ type: 'expense', amount: 100, date: '2026-06-15T00:00:00.000Z' })],
      recurring: [rule({ id: 'rule-rent', amount: 2000 })],
      now: T_NOW,
      days: 30,
    },
    {
      accounts: [account({ id: 'checking', balance: 0 })],
      transactions: [],
      recurring: [rule({ id: 'rule-salary', amount: 50000, type: 'income', note: 'Salary' })],
      now: T_NOW,
      days: 30,
    },
    {
      accounts: [account({ id: 'checking', balance: 10000 })],
      transactions: [],
      recurring: [rule({ id: 'rule-rent', amount: 2000, pausedAt: '2026-06-01T00:00:00.000Z' })],
      now: T_NOW,
      days: 60,
    },
    {
      accounts: ACCOUNTS,
      transactions: [],
      recurring: [rule({ id: 'rule-card', amount: 900, accountId: 'card', note: 'Netflix' })],
      now: T_NOW,
      days: 60,
    },
    {
      accounts: [account({ id: 'checking', balance: 1000 })],
      transactions: [tx({ type: 'expense', amount: 100, date: '2026-06-15T00:00:00.000Z' })],
      recurring: [],
      now: T_NOW,
      days: 30,
    },
    { accounts: [], transactions: [], recurring: [], now: T_NOW },
    { accounts: [account({ id: 'checking', balance: 500 })], transactions: [], recurring: [], now: T_NOW },
    {
      accounts: [account({ id: 'checking', balance: 10000 })],
      transactions: [],
      recurring: [
        rule({ id: 'rule-rent', amount: 2000, startDate: '2026-06-20T00:00:00.000Z' }),
        rule({ id: 'rule-salary', amount: 50000, type: 'income', note: 'Salary', startDate: '2026-06-20T00:00:00.000Z' }),
        rule({ id: 'rule-late', amount: 2000, startDate: '2026-07-03T00:00:00.000Z' }),
      ],
      now: T_NOW,
      days: 60,
    },
  ];
  for (const input of testInputs) {
    out.push(gc('buildCashFlowForecast', [input], buildCashFlowForecast(input)));
  }

  // ── buildCashFlowCalendarMonth ────────────────────────────────────────────
  const scheduled = buildCashFlowForecast({ accounts, transactions: txns, recurring: allRules, now: NOW, days: 60 }).scheduled;
  const monthRanges = [
    periodRange('monthly', NOW, 1),
    periodRange('monthly', NOW, 25),
    shiftPeriod(periodRange('monthly', NOW, 1), 1),
    shiftPeriod(periodRange('monthly', NOW, 1), 2),
    periodRange('monthly', at(2026, 2, 10), 1),
  ];
  for (const range of monthRanges) {
    for (const now of [NOW, at(2026, 7, 1, 0, 0)]) {
      out.push(gc('buildCashFlowCalendarMonth', [scheduled, range, now], buildCashFlowCalendarMonth(scheduled, range, now)));
    }
  }
  const t9 = buildCashFlowForecast(testInputs[8]).scheduled;
  const juneRange = periodRange('monthly', T_NOW);
  out.push(gc('buildCashFlowCalendarMonth', [t9, juneRange, T_NOW], buildCashFlowCalendarMonth(t9, juneRange, T_NOW)));
  out.push(gc('buildCashFlowCalendarMonth', [[], juneRange, T_NOW], buildCashFlowCalendarMonth([], juneRange, T_NOW)));

  return out;
}
