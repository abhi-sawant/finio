import type { Account, Transaction } from '@/types';
import {
  buildPeriodComparison,
  buildSpendingCalendar,
  buildYearInReview,
  categoryMovements,
  summarizePeriod,
  type YearInReview,
} from '@/utils/analytics';
import { periodRange, shiftPeriod, type PeriodRange, type PeriodType } from '@/utils/period';
import { at, gc, type GoldenCase } from '../golden';
import { L, NOW, buildLedger } from './calculations.fixture';

// Analytics over the shared fixture ledger (calculations.fixture.ts) plus the scenarios from
// analytics.test.ts. Year in Review's `biggestExpense` is emitted as its id to keep the JSON small.

const JUNE = new Date('2026-06-15T12:00:00.000Z');

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

const account: Account = {
  id: 'acc-1',
  name: 'Checking',
  type: 'checking',
  color: '#000',
  icon: 'landmark',
  balance: 5000,
  openingBalance: 0,
  createdAt: '2025-01-01T00:00:00.000Z',
};

const yir = (r: YearInReview) => ({ ...r, biggestExpense: r.biggestExpense?.id ?? null });

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const { transactions: txns, accounts } = ledger;
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  // ── summarizePeriod ───────────────────────────────────────────────────────
  const ranges: PeriodRange[] = [
    ...([1, 25] as const).flatMap((msd) =>
      (['weekly', 'monthly', 'yearly'] as PeriodType[]).map((type) => periodRange(type, NOW, msd)),
    ),
    shiftPeriod(periodRange('monthly', NOW, 1), -1),
    shiftPeriod(periodRange('monthly', NOW, 25), -1),
    shiftPeriod(periodRange('monthly', NOW, 1), 1),
    periodRange('monthly', at(2026, 2, 10), 28),
    periodRange('monthly', at(2025, 12, 31, 23, 59), 1),
  ];
  const nows = [NOW, at(2026, 6, 1, 0, 0), at(2026, 6, 30, 23, 59, 59, 999), at(2027, 1, 1)];
  for (const range of ranges) {
    for (const now of nows) {
      for (const monthStartDay of [1, 25]) {
        const options = { now, monthStartDay };
        out.push(gc('summarizePeriod', [L('transactions'), range, options], summarizePeriod(txns, range, options)));
      }
    }
  }
  const labelled = { now: NOW, label: 'Custom label' };
  out.push(gc('summarizePeriod', [L('transactions'), ranges[1], labelled], summarizePeriod(txns, ranges[1], labelled)));
  out.push(gc('summarizePeriod', [[], ranges[1], { now: NOW }], summarizePeriod([], ranges[1], { now: NOW })));

  const juneRange = periodRange('monthly', JUNE);
  const testRows = [
    tx({ type: 'income', amount: 5000, date: '2026-06-01T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 1200, date: '2026-06-10T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 999, date: '2026-05-30T00:00:00.000Z' }),
    tx({
      type: 'expense',
      amount: 500,
      date: '2026-06-05T00:00:00.000Z',
      categoryId: '',
      splits: [
        { categoryId: 'cat-food', amount: 300 },
        { categoryId: 'cat-home', amount: 200 },
      ],
    }),
    tx({ type: 'expense', amount: 1500, date: '2026-06-01T00:00:00.000Z', id: 'x-1500' }),
  ];
  for (const now of [JUNE, new Date('2026-08-01T00:00:00.000Z'), new Date('2026-05-31T18:29:59.999Z')]) {
    out.push(gc('summarizePeriod', [testRows, juneRange, { now }], summarizePeriod(testRows, juneRange, { now })));
  }

  // ── buildPeriodComparison + categoryMovements ─────────────────────────────
  const comparisonNows = [NOW, at(2026, 6, 1, 0, 0), at(2026, 5, 24, 23, 59), at(2026, 1, 3), at(2026, 3, 31, 23)];
  for (const type of ['weekly', 'monthly', 'yearly'] as PeriodType[]) {
    for (const now of comparisonNows) {
      for (const monthStartDay of [1, 25]) {
        const options = { type, now, monthStartDay };
        const c = buildPeriodComparison(txns, options);
        out.push(gc('buildPeriodComparison', [L('transactions'), options], c));
        for (const limit of [5, 2, 0, 100, -1]) {
          out.push(gc('categoryMovements', [c.current, c.previous, limit], categoryMovements(c.current, c.previous, limit)));
        }
        if (c.lastYear) {
          out.push(gc('categoryMovements', [c.current, c.lastYear, 5], categoryMovements(c.current, c.lastYear, 5)));
        }
      }
    }
  }
  const cmpRows = [
    tx({ type: 'expense', amount: 1000, date: '2026-06-05T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 800, date: '2026-05-05T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 600, date: '2025-06-05T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 700, date: '2026-06-20T00:00:00.000Z' }),
  ];
  for (const options of [
    { now: JUNE },
    { now: JUNE, type: 'yearly' as PeriodType },
    { now: new Date('2026-07-10T12:00:00.000Z'), monthStartDay: 25 },
  ]) {
    out.push(gc('buildPeriodComparison', [cmpRows, options], buildPeriodComparison(cmpRows, options)));
  }
  const curSummary = summarizePeriod(
    [
      tx({ type: 'expense', amount: 1000, date: '2026-06-05T00:00:00.000Z', categoryId: 'food' }),
      tx({ type: 'expense', amount: 300, date: '2026-06-06T00:00:00.000Z', categoryId: 'new' }),
      tx({ type: 'expense', amount: 50, date: '2026-06-06T00:00:00.000Z', categoryId: 'same' }),
    ],
    juneRange,
    { now: JUNE },
  );
  const prevSummary = summarizePeriod(
    [
      tx({ type: 'expense', amount: 400, date: '2026-05-05T00:00:00.000Z', categoryId: 'food' }),
      tx({ type: 'expense', amount: 50, date: '2026-05-06T00:00:00.000Z', categoryId: 'same' }),
      tx({ type: 'expense', amount: 300, date: '2026-05-07T00:00:00.000Z', categoryId: 'gone' }),
    ],
    periodRange('monthly', new Date('2026-05-15T12:00:00.000Z')),
    { now: JUNE },
  );
  out.push(gc('categoryMovements', [curSummary, prevSummary, 5], categoryMovements(curSummary, prevSummary)));
  out.push(gc('categoryMovements', [prevSummary, curSummary, 5], categoryMovements(prevSummary, curSummary)));

  // ── buildSpendingCalendar ─────────────────────────────────────────────────
  const calendarNows = [NOW, at(2026, 6, 1, 0, 0), at(2025, 1, 1), at(2027, 1, 1)];
  const calendarRanges = [
    periodRange('monthly', NOW, 1),
    periodRange('monthly', NOW, 25),
    shiftPeriod(periodRange('monthly', NOW, 1), -1),
    periodRange('monthly', at(2026, 2, 10), 1),
    periodRange('monthly', at(2026, 3, 10), 1),
    periodRange('weekly', NOW, 1),
  ];
  for (const range of calendarRanges) {
    for (const now of calendarNows) {
      out.push(gc('buildSpendingCalendar', [L('transactions'), range, now], buildSpendingCalendar(txns, range, now)));
    }
  }
  const calRows = [
    tx({ type: 'expense', amount: 100, date: '2026-06-02T09:00:00.000Z' }),
    tx({ type: 'expense', amount: 300, date: '2026-06-02T18:00:00.000Z' }),
    tx({ type: 'expense', amount: 50, date: '2026-06-03T10:00:00.000Z' }),
    tx({ type: 'income', amount: 9000, date: '2026-06-03T10:00:00.000Z' }),
    tx({ type: 'expense', amount: 400, date: '2026-06-04T10:00:00.000Z' }),
    tx({ type: 'expense', amount: 0, date: '2026-06-05T10:00:00.000Z' }),
  ];
  out.push(gc('buildSpendingCalendar', [calRows, juneRange, JUNE], buildSpendingCalendar(calRows, juneRange, JUNE)));
  out.push(gc('buildSpendingCalendar', [[], juneRange, JUNE], buildSpendingCalendar([], juneRange, JUNE)));

  // ── buildYearInReview ─────────────────────────────────────────────────────
  for (const now of [NOW, at(2026, 1, 3), at(2026, 1, 25, 0, 0), at(2025, 12, 31, 23, 59)]) {
    for (const monthStartDay of [1, 25]) {
      for (const yearOffset of [0, -1, -2, 1]) {
        const input = { transactions: L('transactions'), accounts: L('accounts'), now, monthStartDay, yearOffset };
        out.push(
          gc(
            'buildYearInReview',
            [input],
            yir(buildYearInReview({ transactions: txns, accounts, now, monthStartDay, yearOffset })),
          ),
        );
      }
    }
  }
  const yirRows = [
    tx({ type: 'income', amount: 10000, date: '2026-01-10T00:00:00.000Z' }),
    tx({ type: 'expense', amount: 3000, date: '2026-01-10T00:00:00.000Z', categoryId: 'food' }),
    tx({ type: 'expense', amount: 500, date: '2026-03-05T00:00:00.000Z', categoryId: 'fun' }),
    tx({ type: 'expense', amount: 9999, date: '2025-01-10T00:00:00.000Z', categoryId: 'food' }),
    tx({ type: 'expense', amount: 3000, date: '2026-02-10T00:00:00.000Z', categoryId: 'fun', id: 'tie' }),
    tx({ type: 'income', amount: 2000, date: '2026-03-01T00:00:00.000Z' }),
  ];
  for (const input of [
    { transactions: yirRows, accounts: [account], now: JUNE },
    { transactions: [], accounts: [account], now: JUNE },
    { transactions: yirRows, accounts: [account], now: JUNE, yearOffset: -1 },
    { transactions: yirRows, accounts: [], now: JUNE },
  ]) {
    out.push(gc('buildYearInReview', [input], yir(buildYearInReview(input))));
  }

  return out;
}
