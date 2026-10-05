import type { Account, Budget, Category, RecurringTransaction, Transaction } from '@/types';
import { formatCurrency } from '@/utils/formatters';
import {
  amountsMatch,
  buildInsights,
  detectSubscriptions,
  normalizeNote,
  type InsightInput,
} from '@/utils/insights';
import { at, gc, type GoldenCase } from '../golden';
import { L, NOW, buildLedger } from './calculations.fixture';

// Insights over the shared fixture ledger (calculations.fixture.ts) plus every scenario from
// insights.test.ts. `formatAmount` is named by the case's last arg so Kotlin can rebuild it.

const FORMATTERS: Record<string, (v: number) => string> = {
  currency: (v) => formatCurrency(v),
  hidden: (v) => formatCurrency(v, false, true),
  plain: (v) => `Rs${v}`,
};

const T_NOW = new Date('2026-06-15T12:00:00.000Z');
const CATEGORIES: Category[] = [
  { id: 'cat-food', name: 'Food', icon: 'utensils', color: '#f00', type: 'expense' },
  { id: 'cat-fun', name: 'Entertainment', icon: 'clapperboard', color: '#0f0', type: 'expense' },
];

function tx(partial: Partial<Transaction> & Pick<Transaction, 'type' | 'amount' | 'date'>): Transaction {
  return {
    id: partial.id ?? `${partial.type}-${partial.amount}-${partial.date}`,
    accountId: 'acc-1',
    categoryId: 'cat-food',
    note: '',
    labels: [],
    createdAt: partial.date,
    ...partial,
  };
}

function subscriptionRows(note: string, amount: number, months: number[], extra: Partial<Transaction> = {}) {
  return months.map((month) =>
    tx({
      id: `${note}-${month}`,
      type: 'expense',
      amount,
      date: `2026-0${month}-05T00:00:00.000Z`,
      note,
      categoryId: 'cat-fun',
      ...extra,
    }),
  );
}

const base = (transactions: Transaction[], extra: Partial<InsightInput> = {}): InsightInput => ({
  transactions,
  categories: CATEGORIES,
  labels: [],
  budgets: [] as Budget[],
  recurring: [] as RecurringTransaction[],
  now: T_NOW,
  ...extra,
});

const acct = (id: string, name: string, type: Account['type'], balance: number, extra: Partial<Account> = {}): Account => ({
  id,
  name,
  type,
  color: '#000',
  icon: 'landmark',
  balance,
  openingBalance: 0,
  createdAt: '2026-01-01T00:00:00.000Z',
  ...extra,
});

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  for (const note of [
    'UPI/Spotify/9921',
    'Spotify 449',
    '  Café  Coffee  Day ',
    'स्विगी order',
    'NETFLIX.COM',
    '',
    '123 456',
    'a b',
    'x y　z',
    'tab\tsep\nline',
    'İstanbul',
    'Kelvin',
    'ﬀ ligature',
    'Ünïcödé',
    '🍕 Pizza Hut',
    'a_b-c.d',
    'ÅNGSTRÖM',
    'Σίσυφος',
    '١٢٣ arabic digits',
    'Ⅻ roman',
  ]) {
    out.push(gc('normalizeNote', [note], normalizeNote(note)));
  }
  for (const [a, b] of [
    [100, 105],
    [100, 105.01],
    [10, 12],
    [10, 12.01],
    [0, 2],
    [499, 474.05],
    [-5, -3],
    [1e6, 1.05e6],
  ]) {
    out.push(gc('amountsMatch', [a, b], amountsMatch(a, b)));
  }

  // ── detectSubscriptions ───────────────────────────────────────────────────
  for (const now of [NOW, at(2026, 6, 7, 6), at(2026, 6, 7, 5, 59), at(2026, 2, 20), at(2027, 6, 1)]) {
    out.push(
      gc(
        'detectSubscriptions',
        [L('transactions'), L('recurring'), now],
        detectSubscriptions(ledger.transactions, ledger.recurring, now),
      ),
    );
  }
  out.push(
    gc('detectSubscriptions', [L('transactions'), [], NOW], detectSubscriptions(ledger.transactions, [], NOW)),
  );
  const subScenarios: Transaction[][] = [
    subscriptionRows('Spotify', 499, [3, 4, 5]),
    subscriptionRows('Spotify', 499, [4, 5]),
    [...subscriptionRows('Gym', 499, [3, 4]), ...subscriptionRows('Gym', 650, [5]).map((t) => ({ ...t, id: 'gym-x' }))],
    subscriptionRows('Irregular', 100, [1, 2, 5]),
    subscriptionRows('Generated', 199, [3, 4, 5], { recurringId: 'r1' }),
    subscriptionRows('EmptyRec', 199, [3, 4, 5], { recurringId: '' }),
    subscriptionRows('Split', 199, [3, 4, 5], { splits: [] }),
    subscriptionRows('Small', 10, [3, 4, 5]).map((t, i) => ({ ...t, amount: 10 + i })),
    [
      tx({ id: 'y1', type: 'expense', amount: 1499, date: '2024-06-20T00:00:00.000Z', note: 'Prime' }),
      tx({ id: 'y2', type: 'expense', amount: 1499, date: '2025-06-20T00:00:00.000Z', note: 'Prime' }),
      tx({ id: 'y3', type: 'expense', amount: 1499, date: '2026-06-15T00:00:00.000Z', note: 'Prime' }),
    ],
    [3, 10, 17, 24].map((d) =>
      tx({ id: `w${d}`, type: 'expense', amount: 99, date: `2026-05-${d < 10 ? '0' + d : d}`, note: 'Weekly 99' }),
    ),
    subscriptionRows('Future', 50, [6, 7, 8]),
  ];
  const covering: RecurringTransaction[] = [
    { ...ledger.recurring[1], id: 'cov', note: 'spotify!!' },
    { ...ledger.recurring[1], id: 'blank', note: '1234' },
  ];
  for (const rows of subScenarios) {
    out.push(gc('detectSubscriptions', [rows, [], T_NOW], detectSubscriptions(rows, [], T_NOW)));
  }
  out.push(
    gc('detectSubscriptions', [subScenarios[0], covering, T_NOW], detectSubscriptions(subScenarios[0], covering, T_NOW)),
  );

  // ── buildInsights over the ledger ─────────────────────────────────────────
  const ledgerRuns: Array<[Partial<InsightInput>, string]> = [
    [{ now: NOW }, 'currency'],
    [{ now: NOW, monthStartDay: 25 }, 'currency'],
    [{ now: NOW, limit: 20 }, 'hidden'],
    [{ now: NOW, limit: 2 }, 'plain'],
    [{ now: NOW, limit: 0 }, 'plain'],
    [{ now: NOW, limit: -1 }, 'plain'],
    [{ now: at(2026, 6, 3, 9) }, 'currency'],
    [{ now: at(2026, 6, 30, 23) }, 'currency'],
    [{ now: at(2026, 3, 20, 12), limit: 20 }, 'plain'],
    [{ now: at(2026, 5, 26, 9), monthStartDay: 25, limit: 20 }, 'plain'],
    [{ now: NOW, accounts: undefined, limit: 20 }, 'plain'],
  ];
  for (const [extra, fmt] of ledgerRuns) {
    const withAccounts = !('accounts' in extra);
    const input: InsightInput = {
      transactions: ledger.transactions,
      categories: ledger.categories,
      labels: ledger.labels,
      budgets: ledger.budgets,
      recurring: ledger.recurring,
      ...(withAccounts ? { accounts: ledger.accounts } : {}),
      ...extra,
    };
    const args = {
      transactions: L('transactions'),
      categories: L('categories'),
      labels: L('labels'),
      budgets: L('budgets'),
      recurring: L('recurring'),
      ...(withAccounts ? { accounts: L('accounts') } : {}),
      ...extra,
    };
    out.push(gc('buildInsights', [args, fmt], buildInsights(input, { formatAmount: FORMATTERS[fmt] })));
  }

  // ── buildInsights scenarios from insights.test.ts ─────────────────────────
  const monthly = (amounts: number[], dates: string[]) =>
    amounts.map((amount, i) => tx({ type: 'expense', amount, date: dates[i] }));
  const scenarios: InsightInput[] = [
    base(monthly([4000, 4000, 4000, 4000], ['2026-03-10T00:00:00.000Z', '2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    base(monthly([1000, 1000, 1000, 7000], ['2026-03-10T00:00:00.000Z', '2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    base(monthly([1000, 1000, 1000, 2100], ['2026-03-10T00:00:00.000Z', '2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    base(monthly([4000, 4000, 2000], ['2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    base(monthly([4000, 4000, 400], ['2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    ...['2026-06-03T12:00:00.000Z', '2026-06-10T12:00:00.000Z'].map((now) =>
      base(
        monthly(
          [4000, 4000, 4000, 3000, 3000, 3000],
          ['2026-03-10T00:00:00.000Z', '2026-04-10T00:00:00.000Z', '2026-05-10T00:00:00.000Z', '2026-06-01T00:00:00.000Z', '2026-06-02T00:00:00.000Z', '2026-06-03T00:00:00.000Z'],
        ),
        { now: new Date(now) },
      ),
    ),
    base(monthly([4000, 9000], ['2026-05-10T00:00:00.000Z', '2026-06-10T00:00:00.000Z'])),
    base(subscriptionRows('Spotify', 499, [3, 4, 5])),
    base(
      [
        tx({ type: 'expense', amount: 1500, date: '2026-06-05T00:00:00.000Z' }),
        tx({ type: 'expense', amount: 800, date: '2026-06-05T00:00:00.000Z', categoryId: 'cat-fun' }),
      ],
      {
        budgets: [
          { id: 'b-over', categoryId: 'cat-food', amount: 1000, period: 'monthly', rollover: false, createdAt: '2026-01-01T00:00:00.000Z' },
          { id: 'b-pace', categoryId: 'cat-fun', amount: 1000, period: 'monthly', rollover: false, createdAt: '2026-01-01T00:00:00.000Z' },
          { id: 'b-lbl', categoryId: '', labelId: 'lbl-gone', amount: 1, period: 'monthly', rollover: false, createdAt: '2026-01-01T00:00:00.000Z' },
          { id: 'b-all', categoryId: '', amount: 2000, period: 'weekly', rollover: false, createdAt: '2026-01-01T00:00:00.000Z' },
        ],
      },
    ),
    base([
      tx({ type: 'income', amount: 10000, date: '2026-06-01T00:00:00.000Z' }),
      tx({ type: 'expense', amount: 14000.25, date: '2026-06-05T00:00:00.000Z' }),
    ]),
    base([
      tx({ type: 'income', amount: 10000, date: '2026-06-01T00:00:00.000Z' }),
      tx({ type: 'expense', amount: 5000, date: '2026-06-05T00:00:00.000Z' }),
    ]),
    base([
      tx({ type: 'income', amount: 10000, date: '2026-06-01T00:00:00.000Z' }),
      tx({ type: 'expense', amount: 8000.5, date: '2026-06-05T00:00:00.000Z' }),
    ]),
    base(
      [
        tx({ type: 'income', amount: 10000, date: '2026-06-01T00:00:00.000Z' }),
        tx({ type: 'expense', amount: 14000, date: '2026-06-05T00:00:00.000Z' }),
        ...subscriptionRows('Spotify', 499, [3, 4, 5]),
      ],
      { limit: 2 },
    ),
    base([]),
    base([], {
      accounts: [
        acct('acc-checking', 'Checking', 'checking', -500),
        acct('acc-credit', 'Credit Card', 'credit', -2000),
        acct('acc-fd', 'Broken FD', 'fd', -10.5),
        acct('acc-cash', 'Cash', 'cash', -0.01),
        acct('acc-old', 'Old Wallet', 'wallet', -100, { archivedAt: '2026-02-01T00:00:00.000Z' }),
      ],
    }),
    base(
      [
        tx({ type: 'expense', amount: 600, date: '2026-06-02T00:00:00.000Z' }),
        tx({ type: 'expense', amount: 300, date: '2026-06-03T00:00:00.000Z', categoryId: 'cat-fun' }),
        tx({ type: 'expense', amount: 200, date: '2026-06-04T00:00:00.000Z', categoryId: 'cat-gone' }),
        tx({
          type: 'expense',
          amount: 1000,
          date: '2026-06-04T00:00:00.000Z',
          categoryId: '',
          splits: [
            { categoryId: 'cat-food', amount: 700 },
            { categoryId: 'cat-fun', amount: 300 },
          ],
        }),
      ],
    ),
  ];
  for (const input of scenarios) {
    for (const fmt of ['plain', 'currency']) {
      out.push(gc('buildInsights', [input, fmt], buildInsights(input, { formatAmount: FORMATTERS[fmt] })));
    }
  }
  return out;
}
