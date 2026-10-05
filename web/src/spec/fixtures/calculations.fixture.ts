import { defaultCategories, defaultLabels } from '@/data/defaultData';
import { recomputeAccountBalances } from '@/store/balance';
import type {
  Account,
  Budget,
  Category,
  DebtEntry,
  Goal,
  GoalContribution,
  Label,
  NetWorthSnapshot,
  Person,
  RecurringTransaction,
  Transaction,
  TransactionType,
} from '@/types';
import {
  BUDGET_NEAR_LIMIT_PERCENT,
  MAX_ROLLOVER_LOOKBACK,
  TRANSFER_CATEGORY_ID,
  activeAccounts,
  budgetHealth,
  budgetMatchedAmount,
  budgetScopeKey,
  buildSearchIndex,
  computeBudgetHistory,
  computeBudgetStatuses,
  computeGoalStatus,
  computePersonBalance,
  findTransferCategory,
  getCreditCardDueInfo,
  getCreditUtilization,
  getCurrentMonthTransactions,
  getDashboardStats,
  getMonthTransactions,
  getNetWorth,
  getPreviousMonthTransactions,
  getTotalAccountBalance,
  getTotalCreditOutstanding,
  getTotalDepositValue,
  getTotalExpenses,
  getTotalIncome,
  getTotalOwedToYou,
  getTotalYouOwe,
  groupTransactionsByDate,
  isCategoryValidForType,
  isLiquidAccount,
  miscLast,
  sortTransactionsDateDesc,
  transactionCategoryAmounts,
  transactionMatchesQuery,
  transactionsInPeriod,
  transactionsToCsv,
  type BudgetStatus,
} from '@/utils/calculations';
import { periodRange, shiftPeriod, type PeriodType } from '@/utils/period';
import { vi } from 'vitest';
import { at, gc, type GoldenCase } from '../golden';

/*
 * The shared fixture ledger for calculations / netWorth / insights / merchants: eight financial
 * months (Nov 2025 – Jun 2026) of a realistic household — salary on the 25th (so monthStartDay
 * 25 is meaningful), rent, card spending with a statement cycle, an FD and an RD, split receipts,
 * transfers, labels, subscriptions (monthly + weekly), a June food spike, month-boundary and
 * date-only rows, a future-dated row and an archived account. Fully deterministic.
 *
 * Every fixture emits it once as a `ledger` case; other cases reference it by string so the JSON
 * stays small: `"@ledger.<field>"` is a ledger field, `"@tx:<id>,<id>"` is those transactions.
 */

export const NOW = at(2026, 6, 18, 15, 30);
export const iso = (y: number, m: number, d: number, h = 10, min = 0, s = 0, ms = 0) =>
  at(y, m, d, h, min, s, ms).toISOString();

export interface Ledger {
  accounts: Account[];
  transactions: Transaction[];
  categories: Category[];
  labels: Label[];
  budgets: Budget[];
  recurring: RecurringTransaction[];
  snapshots: NetWorthSnapshot[];
}

export const L = (field: keyof Ledger) => `@ledger.${field}`;
export const TX = (rows: Transaction[]) => `@tx:${rows.map((t) => t.id).join(',')}`;
export const ids = (rows: Transaction[]) => rows.map((t) => t.id);

function account(
  id: string,
  name: string,
  type: Account['type'],
  openingBalance: number,
  extra: Partial<Account> = {},
): Account {
  return {
    id,
    name,
    type,
    color: '#4b36c7',
    icon: 'landmark',
    balance: openingBalance,
    openingBalance,
    createdAt: '2025-10-01T04:30:00.000Z',
    ...extra,
  };
}

function txn(
  id: string,
  type: TransactionType,
  amount: number,
  accountId: string,
  categoryId: string,
  date: string,
  note: string,
  extra: Partial<Transaction> = {},
): Transaction {
  return { id, type, amount, accountId, categoryId, date, note, labels: [], createdAt: date, ...extra };
}

export function buildLedger(): Ledger {
  const accounts: Account[] = [
    account('acc-bank', 'HDFC Salary', 'checking', 50000),
    account('acc-savings', 'SBI Savings', 'savings', 200000),
    account('acc-cash', 'Cash', 'cash', 3000),
    account('acc-card', 'Amex Platinum', 'credit', 0, {
      creditLimit: 150000,
      statementCloseDay: 20,
      paymentDueDays: 18,
    }),
    account('acc-card2', 'ICICI Coral', 'credit', -4200, {
      creditLimit: 50000,
      statementCloseDay: 5,
      paymentDueDays: 20,
      minimumDuePercent: 3.5,
    }),
    account('acc-wallet', 'Paytm Wallet', 'wallet', 100),
    account('acc-fd', 'HDFC FD', 'fd', 100000, {
      deposit: {
        amount: 100000,
        interestRate: 7.1,
        startDate: iso(2025, 9, 1, 0),
        maturityDate: iso(2027, 9, 1, 0),
        compounding: 'quarterly',
        linkedAccountId: 'acc-bank',
      },
    }),
    account('acc-rd', 'SBI RD', 'rd', 0, {
      deposit: {
        amount: 5000,
        interestRate: 6.5,
        startDate: iso(2025, 11, 10, 0),
        tenureMonths: 24,
        linkedAccountId: 'acc-bank',
        recurringId: 'rec-rd',
      },
    }),
    account('acc-old', 'Old Axis', 'checking', 5000, { archivedAt: '2026-02-01T04:30:00.000Z' }),
  ];

  const tx: Transaction[] = [];
  const months: Array<[number, number]> = [
    [2025, 11],
    [2025, 12],
    [2026, 1],
    [2026, 2],
    [2026, 3],
    [2026, 4],
    [2026, 5],
    [2026, 6],
  ];
  const foodNotes = ['Swiggy/9921', 'Swiggy 449', 'Zomato order #4471', 'BigBasket'];
  months.forEach(([y, m], k) => {
    const mm = `${y}${String(m).padStart(2, '0')}`;
    const inJune = y === 2026 && m === 6;
    // Salary lands on the 25th of the *previous* month — the salary cycle.
    const [py, pm] = m === 1 ? [y - 1, 12] : [y, m - 1];
    tx.push(
      txn(`sal-${mm}`, 'income', 85000, 'acc-bank', 'cat-9', iso(py, pm, 25, 9), 'Salary ACME Corp'),
    );
    tx.push(
      txn(`rent-${mm}`, 'expense', 22000, 'acc-bank', 'cat-28', iso(y, m, 1, 9), `Rent ${mm}`, {
        labels: ['lbl-1', 'lbl-5'],
      }),
    );
    tx.push(
      txn(`xsav-${mm}`, 'transfer', 10000, 'acc-bank', 'cat-13', iso(y, m, 2, 8), 'To savings', {
        toAccountId: 'acc-savings',
      }),
    );
    [3, 9, 14, 20].forEach((d, j) => {
      if (inJune && d > 18) return;
      const note = foodNotes[(j + k) % foodNotes.length];
      tx.push(
        txn(
          `food-${mm}-${j}`,
          'expense',
          650 + k * 37 + j * 113.5,
          j % 2 === 0 ? 'acc-card' : 'acc-bank',
          note === 'BigBasket' ? 'cat-25' : 'cat-1',
          iso(y, m, d, 20, 15),
          note,
          j === 1 ? { labels: ['lbl-2'] } : {},
        ),
      );
    });
    tx.push(
      txn(`netflix-${mm}`, 'expense', 649, 'acc-card', 'cat-18', iso(y, m, 7, 6), 'NETFLIX.COM 8812', {
        labels: ['lbl-3'],
      }),
    );
    tx.push(
      txn(
        `spotify-${mm}`,
        'expense',
        k === 5 ? 121 : 119,
        'acc-card',
        'cat-18',
        iso(y, m, 15, 6),
        'Spotify',
      ),
    );
    tx.push(
      txn(`hotstar-${mm}`, 'expense', 299, 'acc-card', 'cat-18', iso(y, m, 16, 6), 'Hotstar 299'),
    );
    tx.push(
      txn(
        `elec-${mm}`,
        'expense',
        1100 + ((k * 173) % 900),
        'acc-bank',
        'cat-5',
        iso(y, m, 12, 11),
        'BESCOM electricity',
      ),
    );
    tx.push(
      txn(`uber-${mm}`, 'expense', 340.75, 'acc-cash', 'cat-2', iso(y, m, 11, 19), 'Uber trip', {
        labels: ['lbl-2'],
      }),
    );
    tx.push(
      txn(`rd-${mm}`, 'transfer', 5000, 'acc-bank', 'cat-13', iso(y, m, 10, 0), 'RD installment', {
        toAccountId: 'acc-rd',
        recurringId: 'rec-rd',
      }),
    );
    tx.push(
      txn(`split-${mm}`, 'expense', 3000.5, 'acc-card', '', iso(y, m, 17, 18), 'DMart Ready', {
        labels: ['lbl-1'],
        splits: [
          { categoryId: 'cat-25', amount: 2000.25 },
          { categoryId: 'cat-32', amount: 1000.25 },
        ],
      }),
    );
    if (!inJune) {
      tx.push(
        txn(
          `cardpay-${mm}`,
          'transfer',
          9000 + k * 250,
          'acc-bank',
          'cat-13',
          iso(y, m, 22, 10),
          'Amex bill payment',
          { toAccountId: 'acc-card' },
        ),
      );
    }
    if (k % 2 === 1) {
      tx.push(
        txn(`int-${mm}`, 'income', 412.37, 'acc-savings', 'cat-23', iso(y, m, 28, 23, 59), 'Interest'),
      );
    }
  });

  // Weekly gym from 6 Apr 2026 (a Monday), every 7 days up to before NOW.
  for (let i = 0; i < 11; i += 1) {
    tx.push(
      txn(
        `gym-${i}`,
        'expense',
        299,
        'acc-card',
        'cat-29',
        new Date(at(2026, 4, 6, 7).getTime() + i * 7 * 86_400_000).toISOString(),
        'Cult.fit class',
      ),
    );
  }

  tx.push(
    // A March holiday, the June food spike, a big freelance gig and odd rows.
    txn('trip-mar', 'expense', 18000, 'acc-card', 'cat-15', iso(2026, 3, 5, 14), 'IndiGo 6E-221', {
      labels: ['lbl-2'],
    }),
    txn('party-jun', 'expense', 6500, 'acc-card', 'cat-1', iso(2026, 6, 13, 22), 'Swiggy party'),
    txn('fl-jun', 'income', 32000.5, 'acc-bank', 'cat-10', iso(2026, 6, 10, 12), 'Upwork payout'),
    txn('gift-dec', 'income', 2500, 'acc-cash', 'cat-21', iso(2025, 12, 25, 12), 'Gift from Mom'),
    txn('wallet-1', 'expense', 350, 'acc-wallet', 'cat-1', iso(2026, 5, 6, 13), 'Chai Point'),
    txn('old-1', 'expense', 1200, 'acc-old', 'cat-3', iso(2025, 12, 4, 12), 'Myntra'),
    txn('chai-dateonly', 'expense', 99, 'acc-cash', 'cat-1', '2026-05-31', 'Chai'),
    txn('midnight-jun', 'expense', 180, 'acc-cash', 'cat-1', iso(2026, 6, 1, 0, 0), 'Midnight snack'),
    txn(
      'lastms-may',
      'expense',
      220,
      'acc-cash',
      'cat-1',
      iso(2026, 5, 31, 23, 59, 59, 999),
      'Late dinner',
    ),
    txn('cyc-start', 'expense', 75, 'acc-cash', 'cat-2', iso(2026, 5, 25, 0, 0), 'Auto 25th'),
    txn('cyc-end', 'expense', 60, 'acc-cash', 'cat-2', iso(2026, 5, 24, 23, 59), 'Auto 24th'),
    txn('future-rent', 'expense', 22000, 'acc-bank', 'cat-28', iso(2026, 6, 25, 9), 'Rent advance'),
    txn('paise', 'expense', 0.1 + 0.2, 'acc-cash', 'cat-24', iso(2026, 6, 4, 9), '+91 tip', {
      labels: ['lbl-2', 'lbl-9'],
    }),
    txn('unknown-cat', 'expense', 450, 'acc-cash', 'cat-404', iso(2026, 6, 5, 9), '=cmd|calc'),
    txn('empty-splits', 'expense', 800, 'acc-card', 'cat-3', iso(2026, 6, 6, 9), 'Amazon order', {
      splits: [],
    }),
    txn('refund', 'income', 1999, 'acc-card', 'cat-3', iso(2026, 6, 8, 9), 'Amazon refund'),
  );

  // Balances are a cache of opening + deltas; make them consistent.
  const settled = recomputeAccountBalances(accounts, tx);

  const budget = (id: string, categoryId: string, amount: number, extra: Partial<Budget> = {}) =>
    ({
      id,
      categoryId,
      amount,
      period: 'monthly',
      rollover: false,
      createdAt: '2025-11-01T04:30:00.000Z',
      ...extra,
    }) as Budget;
  const budgets: Budget[] = [
    budget('b-overall', '', 60000),
    budget('b-food', 'cat-1', 5000, { rollover: true, createdAt: '2026-01-15T04:30:00.000Z' }),
    budget('b-groc', 'cat-25', 3000, { rollover: true, createdAt: '2025-10-01T04:30:00.000Z' }),
    budget('b-disc', '', 4000, { labelId: 'lbl-2' }),
    budget('b-gym', 'cat-29', 500, {
      period: 'weekly',
      rollover: true,
      createdAt: '2026-05-01T04:30:00.000Z',
    }),
    budget('b-travel', 'cat-15', 25000, {
      period: 'yearly',
      rollover: true,
      createdAt: '2025-01-01T04:30:00.000Z',
    }),
    budget('b-ent', 'cat-4', 0),
    budget('b-transport', 'cat-2', 2000, { rollover: true, createdAt: '2026-09-01T04:30:00.000Z' }),
    budget('b-util', 'cat-5', 1500, { rollover: true, createdAt: 'not a date' }),
    budget('b-home', 'cat-32', 900),
    budget('b-subs', 'cat-18', 800, { rollover: true }),
    budget('b-emptylabel', 'cat-28', 21000, { labelId: '' }),
  ];

  const recurring: RecurringTransaction[] = [
    {
      id: 'rec-rd',
      type: 'transfer',
      amount: 5000,
      accountId: 'acc-bank',
      toAccountId: 'acc-rd',
      categoryId: 'cat-13',
      note: 'RD installment',
      labels: [],
      frequency: 'monthly',
      startDate: iso(2025, 11, 10, 0),
      occurrenceCount: 8,
      lastRunDate: iso(2026, 6, 10, 0),
      createdAt: iso(2025, 11, 1),
    },
    {
      id: 'rec-hotstar',
      type: 'expense',
      amount: 299,
      accountId: 'acc-card',
      categoryId: 'cat-18',
      note: 'Hotstar',
      labels: [],
      frequency: 'monthly',
      startDate: iso(2025, 11, 16, 6),
      occurrenceCount: 0,
      lastRunDate: null,
      createdAt: iso(2025, 11, 1),
    } as RecurringTransaction,
  ];

  const snapshots: NetWorthSnapshot[] = [
    {
      id: 's1',
      periodKey: '2026-01',
      date: iso(2026, 1, 31, 23, 59, 59, 999),
      assets: 400000,
      liabilities: 12000.5,
      createdAt: iso(2026, 2, 1),
    },
    {
      id: 's2',
      periodKey: '2026-03',
      date: iso(2026, 4, 2, 8),
      assets: 410123.45,
      liabilities: 30000,
      createdAt: iso(2026, 4, 2, 8),
    },
    {
      id: 's-current',
      periodKey: '2026-06',
      date: iso(2026, 6, 10),
      assets: 1,
      liabilities: 1,
      createdAt: iso(2026, 6, 10),
    },
    {
      id: 's-cycle',
      periodKey: '2026-04',
      date: iso(2026, 5, 24, 23, 59, 59, 999),
      assets: 420000,
      liabilities: 15000,
      createdAt: iso(2026, 5, 25),
    },
  ];

  return {
    accounts: settled,
    transactions: tx,
    categories: [
      ...defaultCategories,
      { id: 'cat-x', name: 'Side hustle', icon: 'tag', color: '#000', type: 'both' },
    ],
    labels: defaultLabels,
    budgets,
    recurring,
    snapshots,
  };
}

const byId = (rows: Transaction[], ...wanted: string[]) =>
  wanted.map((id) => {
    const t = rows.find((r) => r.id === id);
    if (!t) throw new Error(`no fixture transaction ${id}`);
    return t;
  });

const statusOut = (s: BudgetStatus) => ({ ...s, budget: s.budget.id });

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const { accounts, transactions: txns, categories, labels, budgets } = ledger;
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  out.push(
    gc('constants', [], { MAX_ROLLOVER_LOOKBACK, BUDGET_NEAR_LIMIT_PERCENT, TRANSFER_CATEGORY_ID }),
  );

  // ── Categories ────────────────────────────────────────────────────────────
  out.push(gc('miscLast', [L('categories')], miscLast(categories).map((c) => c.id)));
  const pick = ['cat-1', 'cat-9', 'cat-13', 'cat-24', 'cat-x'].map(
    (id) => categories.find((c) => c.id === id)!,
  );
  for (const c of pick) {
    for (const type of ['expense', 'income', 'transfer'] as const) {
      out.push(gc('isCategoryValidForType', [c, type], isCategoryValidForType(c, type)));
    }
  }
  out.push(gc('findTransferCategory', [L('categories')], findTransferCategory(categories)));
  out.push(
    gc('findTransferCategory', [[pick[0], pick[4], pick[3]]], findTransferCategory([pick[0], pick[4], pick[3]])),
  );
  out.push(gc('findTransferCategory', [[pick[0], pick[1]]], findTransferCategory([pick[0], pick[1]])));
  for (const t of byId(txns, 'split-202606', 'empty-splits', 'rent-202606', 'unknown-cat')) {
    out.push(gc('transactionCategoryAmounts', [t], transactionCategoryAmounts(t)));
  }

  // ── Totals and accounts ───────────────────────────────────────────────────
  const june = getMonthTransactions(txns, NOW);
  out.push(gc('getTotalIncome', [L('transactions')], getTotalIncome(txns)));
  out.push(gc('getTotalExpenses', [L('transactions')], getTotalExpenses(txns)));
  out.push(gc('getTotalIncome', [TX(june)], getTotalIncome(june)));
  out.push(gc('getTotalExpenses', [TX(june)], getTotalExpenses(june)));
  out.push(gc('getTotalIncome', [[]], getTotalIncome([])));
  out.push(gc('activeAccounts', [L('accounts')], activeAccounts(accounts).map((a) => a.id)));
  const emptyArchived = { ...accounts[0], id: 'acc-empty-archived', archivedAt: '' } as Account;
  out.push(gc('activeAccounts', [[emptyArchived]], activeAccounts([emptyArchived]).map((a) => a.id)));
  for (const a of accounts) {
    out.push(gc('isLiquidAccount', [a], isLiquidAccount(a)));
    out.push(gc('getCreditUtilization', [a], getCreditUtilization(a)));
  }
  out.push(gc('getTotalAccountBalance', [L('accounts')], getTotalAccountBalance(accounts)));
  out.push(gc('getNetWorth', [L('accounts')], getNetWorth(accounts)));
  out.push(gc('getTotalCreditOutstanding', [L('accounts')], getTotalCreditOutstanding(accounts)));
  for (const now of [NOW, at(2025, 9, 1), at(2026, 12, 31, 23, 59), at(2028, 1, 1)]) {
    out.push(gc('getTotalDepositValue', [L('accounts'), now], getTotalDepositValue(accounts, now)));
  }

  // Credit card due info, from calculations.test.ts plus statement-cycle boundaries.
  const card = (extra: Partial<Account>): Account => ({
    id: 'acc-credit',
    name: 'Visa',
    type: 'credit',
    color: '#000',
    icon: 'credit-card',
    balance: -1000,
    openingBalance: 0,
    createdAt: '2026-01-01T00:00:00.000Z',
    creditLimit: 10000,
    ...extra,
  });
  const cards: Account[] = [
    ...accounts,
    card({}),
    card({ statementCloseDay: 15, paymentDueDays: 20 }),
    card({ statementCloseDay: 15, paymentDueDays: 20, archivedAt: '2026-01-01T00:00:00.000Z' }),
    card({ statementCloseDay: 15, paymentDueDays: 20, balance: 500 }),
    card({ statementCloseDay: 15, paymentDueDays: 20, balance: 0 }),
    card({ statementCloseDay: 31, paymentDueDays: 0, balance: -2500.55, minimumDuePercent: 0 }),
    card({ statementCloseDay: 0, paymentDueDays: 20 }),
    card({ statementCloseDay: 1, paymentDueDays: 45, minimumDuePercent: 100 }),
    card({ statementCloseDay: 28, paymentDueDays: 3, creditLimit: 0 }),
  ];
  const dueNows = [
    NOW,
    at(2026, 6, 20, 0, 0),
    at(2026, 6, 19, 23, 59, 59, 999),
    at(2026, 7, 7, 23, 59),
    at(2026, 1, 31, 12),
    at(2026, 3, 1, 0, 0),
    at(2026, 2, 28, 12),
    at(2026, 12, 31, 23, 59),
  ];
  for (const a of cards) {
    if (a.type !== 'credit') continue;
    out.push(gc('getCreditUtilization', [a], getCreditUtilization(a)));
    for (const now of dueNows) {
      out.push(gc('getCreditCardDueInfo', [a, now], getCreditCardDueInfo(a, now)));
    }
  }

  // ── Period slicing ────────────────────────────────────────────────────────
  const junk = [
    ...byId(txns, 'chai-dateonly', 'midnight-jun', 'lastms-may'),
    { ...txns[0], id: 'bad-date', date: 'not a date' },
    { ...txns[0], id: 'empty-date', date: '' },
  ];
  const ranges = [
    ...([1, 25] as const).flatMap((msd) =>
      (['weekly', 'monthly', 'yearly'] as PeriodType[]).map((type) => periodRange(type, NOW, msd)),
    ),
    shiftPeriod(periodRange('monthly', NOW, 1), -1),
    shiftPeriod(periodRange('monthly', NOW, 25), -1),
    periodRange('monthly', at(2026, 2, 10), 28),
  ];
  for (const range of ranges) {
    out.push(gc('transactionsInPeriod', [L('transactions'), range], ids(transactionsInPeriod(txns, range))));
  }
  out.push(gc('transactionsInPeriod', [junk, ranges[1]], ids(transactionsInPeriod(junk, ranges[1]))));
  for (const [d, msd] of [
    [NOW, 1],
    [NOW, 25],
    [at(2026, 5, 31, 23, 59), 1],
    [at(2026, 6, 1, 0, 0), 1],
    [at(2026, 5, 24, 23, 59), 25],
    [at(2026, 5, 25, 0, 0), 25],
    [at(2026, 1, 3), 15],
  ] as Array<[Date, number]>) {
    out.push(gc('getMonthTransactions', [L('transactions'), d, msd], ids(getMonthTransactions(txns, d, msd))));
  }
  vi.useFakeTimers({ now: NOW });
  try {
    for (const msd of [1, 25]) {
      out.push(
        gc('getCurrentMonthTransactions', [L('transactions'), msd, NOW], ids(getCurrentMonthTransactions(txns, msd))),
      );
      out.push(
        gc('getPreviousMonthTransactions', [L('transactions'), msd, NOW], ids(getPreviousMonthTransactions(txns, msd))),
      );
    }
  } finally {
    vi.useRealTimers();
  }

  const validTxns = txns;
  const grouped = (rows: Transaction[]) =>
    groupTransactionsByDate(rows).map((g) => ({ date: g.date, transactions: ids(g.transactions) }));
  out.push(gc('groupTransactionsByDate', [L('transactions')], grouped(validTxns)));
  const midnight = [
    { ...txns[0], id: 'midnight', date: '2026-09-30T18:30:00.000Z' },
    { ...txns[0], id: 'afternoon', date: '2026-10-01T08:00:00.000Z' },
    { ...txns[0], id: 'prev', date: '2026-09-30T10:00:00.000Z' },
    { ...txns[0], id: 'dateonly', date: '2026-10-01' },
    { ...txns[0], id: 'tie', date: '2026-10-01T08:00:00.000Z' },
  ];
  out.push(gc('groupTransactionsByDate', [midnight], grouped(midnight)));
  out.push(gc('groupTransactionsByDate', [[]], grouped([])));
  out.push(gc('sortTransactionsDateDesc', [L('transactions')], ids(sortTransactionsDateDesc(validTxns))));
  out.push(gc('sortTransactionsDateDesc', [midnight], ids(sortTransactionsDateDesc(midnight))));

  // ── Search ────────────────────────────────────────────────────────────────
  const index = buildSearchIndex(categories, accounts, labels);
  out.push(
    gc('buildSearchIndex', [L('categories'), L('accounts'), L('labels')], {
      categoryNames: Object.fromEntries(index.categoryNames),
      accountNames: Object.fromEntries(index.accountNames),
      labelNames: Object.fromEntries(index.labelNames),
    }),
  );
  const searchRows = byId(
    txns,
    'food-202606-0',
    'split-202606',
    'xsav-202606',
    'uber-202606',
    'paise',
    'fl-jun',
    'trip-mar',
    'rent-202606',
    'unknown-cat',
  );
  const queries = [
    '',
    '   ',
    'swiggy',
    'SWIGGY',
    'food',
    'groceries',
    'home maint',
    'hdfc',
    'sbi savings',
    'discretionary',
    'for others',
    '₹1,200',
    '₹32,000.50',
    '22,000',
    '340.75',
    '0.3',
    '.',
    '..',
    '3000.5',
    'zzz',
    ' rent ',
    ' rent﻿',
    'transfer',
    '=cmd',
    '6e',
  ];
  for (const q of queries) {
    out.push(
      gc(
        'transactionMatchesQuery',
        [TX(searchRows), q, L('categories'), L('accounts'), L('labels')],
        searchRows.map((t) => transactionMatchesQuery(t, q, index)),
      ),
    );
  }

  // ── Budgets ───────────────────────────────────────────────────────────────
  for (const b of budgets) out.push(gc('budgetScopeKey', [b], budgetScopeKey(b)));
  const matchRows = byId(
    txns,
    'split-202606',
    'food-202606-1',
    'uber-202606',
    'paise',
    'fl-jun',
    'xsav-202606',
    'rent-202606',
    'empty-splits',
  );
  for (const b of budgets) {
    out.push(gc('budgetMatchedAmount', [b, TX(matchRows)], matchRows.map((t) => budgetMatchedAmount(b, t))));
  }
  const statusNows = [
    NOW,
    at(2026, 6, 1, 0, 0),
    at(2026, 5, 24, 23, 59),
    at(2026, 1, 15),
    at(2026, 12, 31, 23, 59),
  ];
  for (const now of statusNows) {
    for (const monthStartDay of [1, 25]) {
      out.push(
        gc(
          'computeBudgetStatuses',
          [L('budgets'), L('transactions'), { now, monthStartDay }],
          computeBudgetStatuses(budgets, txns, { now, monthStartDay }).map(statusOut),
        ),
      );
    }
  }
  out.push(
    gc(
      'computeBudgetStatuses',
      [L('budgets'), [], { now: NOW }],
      computeBudgetStatuses(budgets, [], { now: NOW }).map(statusOut),
    ),
  );
  for (const [isOver, percent] of [
    [true, 10],
    [false, 84.99],
    [false, 85],
    [false, 100],
    [false, 0],
    [true, 150],
  ] as Array<[boolean, number]>) {
    out.push(gc('budgetHealth', [{ isOver, percent }], budgetHealth({ isOver, percent })));
  }
  for (const s of computeBudgetStatuses(budgets, txns, { now: NOW })) {
    out.push(gc('budgetHealth', [{ isOver: s.isOver, percent: s.percent }], budgetHealth(s)));
  }
  for (const b of budgets) {
    for (const monthStartDay of [1, 25]) {
      out.push(
        gc(
          'computeBudgetHistory',
          [b, L('transactions'), { now: NOW, monthStartDay }, 6],
          computeBudgetHistory(b, txns, { now: NOW, monthStartDay }, 6),
        ),
      );
    }
  }
  for (const [id, count] of [
    ['b-food', 0],
    ['b-overall', 0],
    ['b-groc', 3],
    ['b-groc', 13],
    ['b-gym', 12],
    ['b-travel', 2],
  ] as Array<[string, number]>) {
    const b = budgets.find((x) => x.id === id)!;
    out.push(
      gc(
        'computeBudgetHistory',
        [b, L('transactions'), { now: NOW }, count],
        computeBudgetHistory(b, txns, { now: NOW }, count),
      ),
    );
  }

  // ── Dashboard ─────────────────────────────────────────────────────────────
  for (const [now, monthStartDay] of [
    [NOW, 1],
    [NOW, 25],
    [at(2026, 6, 1, 0, 30), 1],
    [at(2026, 3, 31, 23), 1],
    [at(2026, 5, 26, 9), 25],
  ] as Array<[Date, number]>) {
    const cur = getMonthTransactions(txns, now, monthStartDay);
    const prev = transactionsInPeriod(txns, shiftPeriod(periodRange('monthly', now, monthStartDay), -1));
    out.push(
      gc(
        'getDashboardStats',
        [TX(cur), TX(prev), L('categories'), { now, monthStartDay }],
        getDashboardStats(cur, prev, categories, { now, monthStartDay }),
      ),
    );
  }
  const noIncomePrev = byId(txns, 'rent-202605', 'food-202605-0');
  const dashCur = byId(txns, 'split-202606', 'unknown-cat', 'fl-jun', 'paise');
  out.push(
    gc(
      'getDashboardStats',
      [TX(dashCur), TX(noIncomePrev), L('categories'), { now: NOW }],
      getDashboardStats(dashCur, noIncomePrev, categories, { now: NOW }),
    ),
  );
  out.push(
    gc('getDashboardStats', [[], [], L('categories'), { now: NOW }], getDashboardStats([], [], categories, { now: NOW })),
  );
  const onlyUnknown = byId(txns, 'unknown-cat');
  out.push(
    gc(
      'getDashboardStats',
      [TX(onlyUnknown), TX(onlyUnknown), L('categories'), { now: at(2026, 7, 1, 0, 0) }],
      getDashboardStats(onlyUnknown, onlyUnknown, categories, { now: at(2026, 7, 1, 0, 0) }),
    ),
  );

  // ── Goals and people (calculations.test.ts shapes) ────────────────────────
  const goal = (extra: Partial<Goal> = {}): Goal => ({
    id: 'goal-1',
    name: 'Emergency Fund',
    icon: 'target',
    color: '#146b54',
    targetAmount: 10000,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...extra,
  });
  const contribution = (amount: number, extra: Partial<GoalContribution> = {}): GoalContribution => ({
    id: `contrib-${amount}-${extra.date ?? ''}`,
    goalId: 'goal-1',
    amount,
    date: '2026-01-05T00:00:00.000Z',
    note: '',
    createdAt: '2026-01-05T00:00:00.000Z',
    ...extra,
  });
  const goalCases: Array<[Goal, GoalContribution[], Date]> = [
    [goal(), [contribution(2500, { date: '2026-01-11T00:00:00.000Z' })], at(2026, 1, 21)],
    [goal({ createdAt: '2026-06-01T00:00:00.000Z' }), [contribution(3000, { date: '2026-03-01T00:00:00.000Z' })], NOW],
    [goal(), [contribution(4000), contribution(9999, { goalId: 'goal-2' })], NOW],
    [goal(), [contribution(6000), contribution(-1500, { date: '2026-02-01T00:00:00.000Z' })], NOW],
    [goal(), [contribution(7000), contribution(5000)], NOW],
    [goal(), [], NOW],
    [goal(), [contribution(500), contribution(-500)], NOW],
    [goal(), [contribution(-200)], NOW],
    [goal({ targetAmount: 0 }), [], NOW],
    [goal({ targetAmount: 1000000 }), [contribution(1, { date: '2026-06-18T09:00:00.000Z' })], NOW],
    [goal({ createdAt: 'garbage' }), [contribution(100, { date: 'zzz' })], NOW],
    [goal(), [contribution(3333.33, { date: '2026-01-05' })], at(2026, 1, 5, 23)],
  ];
  for (const [g, cs, now] of goalCases) {
    out.push(gc('computeGoalStatus', [g, cs, now], { ...computeGoalStatus(g, cs, now), goal: g.id }));
  }

  const person = (id: string, name: string): Person => ({
    id,
    name,
    icon: 'user',
    color: '#146b54',
    createdAt: '2026-01-01T00:00:00.000Z',
  });
  const entry = (id: string, personId: string, amount: number, date: string): DebtEntry => ({
    id,
    personId,
    amount,
    date,
    note: '',
    createdAt: date,
  });
  const people = [person('p1', 'Rahul'), person('p2', 'Priya'), person('p3', 'Arjun'), person('p4', 'Meera')];
  const entries = [
    entry('e1', 'p1', 500, '2026-01-05T00:00:00.000Z'),
    entry('e2', 'p1', -200, '2026-03-05T00:00:00.000Z'),
    entry('e3', 'p2', -1200.5, '2026-02-01T00:00:00.000Z'),
    entry('e4', 'p2', 200.5, '2026-01-01T00:00:00.000Z'),
    entry('e5', 'p3', 300, '2026-04-01T00:00:00.000Z'),
    entry('e6', 'p3', -300, '2026-04-01'),
    entry('e7', 'ghost', 9999, '2026-05-01T00:00:00.000Z'),
  ];
  for (const p of people) {
    out.push(gc('computePersonBalance', [p, entries], { ...computePersonBalance(p, entries), person: p.id }));
  }
  out.push(gc('getTotalOwedToYou', [people, entries], getTotalOwedToYou(people, entries)));
  out.push(gc('getTotalYouOwe', [people, entries], getTotalYouOwe(people, entries)));
  out.push(gc('getTotalOwedToYou', [[], []], getTotalOwedToYou([], [])));
  out.push(gc('getTotalYouOwe', [[], []], getTotalYouOwe([], [])));

  // ── CSV ───────────────────────────────────────────────────────────────────
  out.push(
    gc(
      'transactionsToCsv',
      [L('transactions'), L('categories'), L('accounts')],
      transactionsToCsv(txns, categories, accounts),
    ),
  );
  const nasty = (id: string, note: string, extra: Partial<Transaction> = {}) =>
    txn(id, 'expense', 100, 'acc-evil', 'cat-1', '2026-06-05T00:00:00.000Z', note, extra);
  const csvRows = [
    nasty('c1', '=SUM(A1:A9)'),
    nasty('c2', '+91 98450'),
    nasty('c3', '-5 discount'),
    nasty('c4', '@SUM(1)'),
    nasty('c5', '\tTabbed'),
    nasty('c6', '\rCarriage'),
    nasty('c7', 'He said "hi", twice'),
    nasty('c8', 'a=b is fine'),
    nasty('c9', ' =leading space'),
    nasty('c10', 'split', {
      amount: 2.675 + 1.005,
      categoryId: '',
      splits: [
        { categoryId: 'cat-1', amount: 2.675 },
        { categoryId: 'cat-missing', amount: 1.005 },
      ],
    }),
    nasty('c11', 'xfer', { type: 'transfer', toAccountId: 'acc-gone', amount: 0.1 + 0.2 }),
    nasty('c12', 'xfer2', { type: 'transfer', toAccountId: 'acc-evil', amount: 1e-7 }),
    nasty('c13', '', { amount: 123456789.125 }),
  ];
  const evilAccounts = [{ ...accounts[0], id: 'acc-evil', name: '=HYPERLINK("x")' }];
  out.push(
    gc(
      'transactionsToCsv',
      [csvRows, L('categories'), evilAccounts],
      transactionsToCsv(csvRows, categories, evilAccounts),
    ),
  );
  out.push(gc('transactionsToCsv', [[], [], []], transactionsToCsv([], [], [])));

  return out;
}
