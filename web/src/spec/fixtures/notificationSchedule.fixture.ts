import type { Account, Budget, Category, Label, RecurringTransaction, Transaction } from '@/types';
import { buildNotificationSchedule, type NotificationScheduleInput } from '@/utils/notificationSchedule';
import type { NotificationPrefs } from '@/utils/notifications';
import { at, gc, type GoldenCase } from '../golden';
import { L, NOW, buildLedger } from './calculations.fixture';

// The reminder schedule — every id here is part of the cross-client dedupe contract
// (spec/backup-format.md §6), so the Kotlin port must reproduce them byte for byte. Covers the
// shared ledger at several moments plus every scenario from notificationSchedule.test.ts.

const T_NOW = at(2026, 6, 15, 12); // 15 June 2026, local noon

const categories: Category[] = [
  { id: 'cat-food', name: 'Food', icon: 'utensils', color: '#f00', type: 'expense' },
  { id: 'cat-bills', name: 'Utilities', icon: 'zap', color: '#0f0', type: 'expense' },
];
const labels: Label[] = [{ id: 'lbl-1', name: 'Essential', color: '#00f' }];

function prefs(partial: Partial<NotificationPrefs> = {}): NotificationPrefs {
  return {
    notificationsEnabled: true,
    notifyBills: true,
    notifyBudgets: true,
    notifyCreditDue: true,
    notifyLeadDays: 2,
    notifyDailyLog: true,
    hideAmounts: false,
    ...partial,
  };
}

function input(partial: Partial<NotificationScheduleInput> = {}): NotificationScheduleInput {
  return { recurring: [], budgets: [], transactions: [], accounts: [], categories, labels, monthStartDay: 1, prefs: prefs(), ...partial };
}

function rule(partial: Partial<RecurringTransaction> & Pick<RecurringTransaction, 'id' | 'startDate'>): RecurringTransaction {
  return {
    type: 'expense',
    amount: 500,
    accountId: 'acc-1',
    categoryId: 'cat-bills',
    note: 'Broadband',
    labels: [],
    frequency: 'monthly',
    occurrenceCount: 0,
    lastRunDate: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...partial,
  };
}

function budget(partial: Partial<Budget> & Pick<Budget, 'id'>): Budget {
  return { categoryId: 'cat-food', amount: 1000, period: 'monthly', rollover: false, createdAt: '2026-01-01T00:00:00.000Z', ...partial };
}

function tx(partial: Partial<Transaction> & Pick<Transaction, 'amount' | 'date'>): Transaction {
  return {
    id: `tx-${partial.date}-${partial.amount}`,
    type: 'expense',
    accountId: 'acc-1',
    categoryId: 'cat-food',
    note: '',
    labels: [],
    createdAt: partial.date,
    ...partial,
  };
}

function account(partial: Partial<Account> & Pick<Account, 'id'>): Account {
  return {
    name: 'HDFC Card',
    type: 'credit',
    color: '#000',
    icon: 'credit-card',
    balance: -5000,
    openingBalance: 0,
    createdAt: '2026-01-01T00:00:00.000Z',
    creditLimit: 50000,
    statementCloseDay: 20,
    paymentDueDays: 15,
    ...partial,
  };
}

const isoFromNow = (days: number, h = 9) => at(2026, 6, 15 + days, h).toISOString();

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  // ── The shared ledger at several moments ──────────────────────────────────
  const ledgerInput = (monthStartDay: number, p: NotificationPrefs) => ({
    recurring: ledger.recurring,
    budgets: ledger.budgets,
    transactions: ledger.transactions,
    accounts: ledger.accounts,
    categories: ledger.categories,
    labels: ledger.labels,
    monthStartDay,
    prefs: p,
  });
  const ledgerArg = (monthStartDay: number, p: NotificationPrefs) => ({
    recurring: L('recurring'),
    budgets: L('budgets'),
    transactions: L('transactions'),
    accounts: L('accounts'),
    categories: L('categories'),
    labels: L('labels'),
    monthStartDay,
    prefs: p,
  });
  const nows = [
    NOW,
    at(2026, 6, 13, 23), // a transaction was logged today (party-jun) → no daily nudge
    at(2026, 6, 18, 22), // after DAILY_LOG_HOUR
    at(2026, 6, 1, 0, 0),
    at(2026, 5, 24, 23, 59),
    at(2026, 7, 5, 8, 59),
    at(2026, 7, 8, 12), // ledger card due dates have passed
    at(2026, 12, 31, 23, 59),
  ];
  const prefGrid = [
    prefs(),
    prefs({ notifyLeadDays: 0 }),
    prefs({ notifyLeadDays: 7, hideAmounts: true }),
    prefs({ notifyBills: false, notifyDailyLog: false }),
    prefs({ notifyBudgets: false, notifyCreditDue: false }),
    prefs({ notificationsEnabled: false }),
  ];
  for (const now of nows) {
    for (const monthStartDay of [1, 25]) {
      for (const p of prefGrid) {
        out.push(
          gc('buildNotificationSchedule', [ledgerArg(monthStartDay, p), now], buildNotificationSchedule(ledgerInput(monthStartDay, p), now)),
        );
      }
    }
  }

  // ── notificationSchedule.test.ts scenarios ────────────────────────────────
  const scenarios: Array<[string, NotificationScheduleInput, Date]> = [];
  const add = (name: string, data: NotificationScheduleInput, now: Date = T_NOW) => scenarios.push([name, data, now]);
  const full = input({
    recurring: [rule({ id: 'r1', startDate: isoFromNow(3) })],
    budgets: [budget({ id: 'b1', amount: 100 })],
    transactions: [tx({ amount: 900, date: isoFromNow(-1) })],
    accounts: [account({ id: 'acc-card' })],
  });
  add('master-off', { ...full, prefs: prefs({ notificationsEnabled: false }) });
  add('no-bills', { ...full, prefs: prefs({ notifyBills: false }) });
  add('no-budgets', { ...full, prefs: prefs({ notifyBudgets: false }) });
  add('no-credit', { ...full, prefs: prefs({ notifyCreditDue: false }) });
  add('full', full);
  add('full-later', full, at(2026, 6, 15, 18, 30));
  add('weekly', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), frequency: 'weekly' })] }));
  add('daily-rule', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(0, 8), frequency: 'daily' })] }));
  add('lead', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(5) })] }));
  add('clamped', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(1) })] }));
  add('today-later', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(0, 18) })] }));
  add('paused', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), pausedAt: '2026-06-01T00:00:00.000Z' })] }));
  add('exhausted', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), maxOccurrences: 2, occurrenceCount: 2 })] }));
  add('ended', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), endDate: isoFromNow(1) })] }));
  add('horizon', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(200), frequency: 'yearly' })] }));
  add('horizon-edge', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(45, 12) }), rule({ id: 'r2', startDate: isoFromNow(45, 13) })] }));
  add('blank-note', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), note: '   ' }), rule({ id: 'r2', startDate: isoFromNow(4), note: '', categoryId: 'cat-gone' })] }));
  add('paise', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3), amount: 1234567.5, note: ' Big EMI ' })] }));
  for (const spend of [840, 850, 1010]) {
    add(`budget-${spend}`, input({ budgets: [budget({ id: 'b1', amount: 1000 })], transactions: [tx({ amount: spend, date: isoFromNow(-1) })] }));
  }
  add('budget-june', input({ budgets: [budget({ id: 'b1', amount: 1000 })], transactions: [tx({ amount: 1010, date: at(2026, 6, 14).toISOString() })] }));
  add('budget-july', input({ budgets: [budget({ id: 'b1', amount: 1000 })], transactions: [tx({ amount: 1010, date: at(2026, 7, 14).toISOString() })] }), at(2026, 7, 15, 12));
  for (const monthStartDay of [1, 25]) {
    add(`budget-msd-${monthStartDay}`, input({ budgets: [budget({ id: 'b1', amount: 1000 })], transactions: [tx({ amount: 1010, date: isoFromNow(-1) })], monthStartDay }));
  }
  add(
    'budget-scopes',
    input({
      budgets: [
        budget({ id: 'b-overall', categoryId: '', amount: 1000 }),
        budget({ id: 'b-label', categoryId: '', labelId: 'lbl-1', amount: 500 }),
        budget({ id: 'b-label-gone', categoryId: '', labelId: 'lbl-gone', amount: 500 }),
        budget({ id: 'b-cat-gone', categoryId: 'cat-gone', amount: 1 }),
        budget({ id: 'b-weekly', amount: 300, period: 'weekly' }),
        budget({ id: 'b-yearly', amount: 1000, period: 'yearly' }),
        budget({ id: 'b-zero', amount: 0 }),
      ],
      transactions: [
        tx({ amount: 1010.5, date: isoFromNow(-1), labels: ['lbl-1'] }),
        tx({ amount: 5, date: isoFromNow(-2), categoryId: 'cat-gone' }),
      ],
    }),
  );
  add('card', input({ accounts: [account({ id: 'acc-card' })] }));
  add('card-no-cycle', input({ accounts: [account({ id: 'acc-card', statementCloseDay: undefined })] }));
  add('card-zero', input({ accounts: [account({ id: 'acc-card', balance: 0 })] }));
  add('card-archived', input({ accounts: [account({ id: 'acc-card', archivedAt: '2026-05-01T00:00:00.000Z' })] }));
  add('savings', input({ accounts: [account({ id: 'acc-1', type: 'savings', balance: -500 })] }));
  add('card-overdue', input({ accounts: [account({ id: 'acc-card', statementCloseDay: 1, paymentDueDays: 3, balance: -2500.55, minimumDuePercent: 3.5 })] }));
  add('card-today', input({ accounts: [account({ id: 'acc-card', statementCloseDay: 10, paymentDueDays: 5 })] }));
  add('card-tomorrow', input({ accounts: [account({ id: 'acc-card', statementCloseDay: 10, paymentDueDays: 6 })] }));
  add('daily', input());
  add('daily-late', input(), at(2026, 6, 15, 22));
  add('daily-logged', input({ transactions: [tx({ amount: 200, date: isoFromNow(0) })] }));
  add('daily-logged-yesterday', input({ transactions: [tx({ amount: 200, date: isoFromNow(-1, 23) })] }));
  add('daily-off', input({ prefs: prefs({ notifyDailyLog: false }) }));
  add('hidden', input({ recurring: [rule({ id: 'r1', startDate: isoFromNow(3) })], prefs: prefs({ hideAmounts: true }) }));
  add(
    'hidden-all',
    { ...full, accounts: [account({ id: 'acc-card' })], prefs: prefs({ hideAmounts: true, notifyLeadDays: 7 }) },
    at(2026, 6, 15, 0, 0),
  );

  for (const [name, data, now] of scenarios) {
    out.push(gc('buildNotificationSchedule', [data, now], buildNotificationSchedule(data, now), name));
  }

  return out;
}
