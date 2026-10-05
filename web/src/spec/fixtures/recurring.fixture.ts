import {
  MAX_OCCURRENCES_PER_RULE,
  futureOccurrences,
  isRuleFinished,
  isRulePaused,
  lastOccurrenceOnOrBefore,
  nextDueDate,
  nextOccurrence,
  planRecurring,
  previewBackfill,
  remainingOccurrences,
} from '@/store/recurring';
import type { RecurrenceFrequency, RecurringTransaction } from '@/types';
import { at, gc, type GoldenCase } from '../golden';

function rule(
  partial: Partial<RecurringTransaction> & Pick<RecurringTransaction, 'id'>,
): RecurringTransaction {
  return {
    type: 'expense',
    amount: 100,
    accountId: 'acc-1',
    categoryId: 'cat-1',
    note: '',
    labels: [],
    frequency: 'monthly',
    startDate: '2026-01-10T00:00:00.000Z',
    occurrenceCount: 0,
    lastRunDate: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...partial,
  };
}

const NOW = new Date('2026-06-15T12:00:00.000Z');
const FREQS: RecurrenceFrequency[] = ['daily', 'weekly', 'monthly', 'yearly'];

const rules: RecurringTransaction[] = [
  rule({ id: 'basic' }),
  rule({ id: 'resumed', lastRunDate: '2026-04-10T00:00:00.000Z', occurrenceCount: 3 }),
  rule({ id: 'future', startDate: '2026-08-01T00:00:00.000Z' }),
  rule({ id: 'gone-account', accountId: 'gone' }),
  rule({ id: 'daily-old', frequency: 'daily', startDate: '2021-01-01T00:00:00.000Z' }),
  rule({ id: 'daily-recent', frequency: 'daily', startDate: '2026-06-01T00:00:00.000Z' }),
  rule({ id: 'weekly', frequency: 'weekly', startDate: '2026-03-02T00:00:00.000Z' }),
  rule({ id: 'yearly-leap', frequency: 'yearly', startDate: '2020-02-29T00:00:00.000Z' }),
  // Local-midnight 31st (IST, stored as 18:30Z on the 30th) — month-end clamping drifts it.
  rule({ id: 'month-end', startDate: at(2026, 1, 31).toISOString() }),
  rule({ id: 'month-end-utc', startDate: '2026-01-31T00:00:00.000Z' }),
  rule({ id: 'broken', startDate: 'not-a-date' }),
  rule({ id: 'broken-last', lastRunDate: 'garbage' }),
  rule({ id: 'empty-start', startDate: '' }),
  rule({ id: 'paused', pausedAt: '2026-02-01T00:00:00.000Z' }),
  rule({ id: 'paused-empty', pausedAt: '' }),
  rule({ id: 'ended', endDate: '2026-03-31T23:59:59.000Z' }),
  rule({ id: 'ended-exact', endDate: '2026-03-10T00:00:00.000Z' }),
  rule({ id: 'ended-before-start', endDate: '2026-01-01T00:00:00.000Z' }),
  rule({ id: 'max2', maxOccurrences: 2 }),
  rule({ id: 'max-used', maxOccurrences: 2, occurrenceCount: 2 }),
  rule({ id: 'max-over', maxOccurrences: 2, occurrenceCount: 5 }),
  rule({ id: 'max0', maxOccurrences: 0 }),
  rule({ id: 'max10', maxOccurrences: 10 }),
  rule({ id: 'transfer', type: 'transfer', toAccountId: 'acc-2' }),
  rule({ id: 'transfer-gone', type: 'transfer', toAccountId: 'acc-9' }),
  rule({ id: 'transfer-none', type: 'transfer' }),
  rule({ id: 'income', type: 'income', amount: 52000.5, startDate: '2026-05-31T18:30:00.000Z' }),
  rule({ id: 'date-only', startDate: '2026-02-15', frequency: 'weekly' }),
  rule({ id: 'local-dt', startDate: '2026-03-01T09:30:00', endDate: '2026-05-01T09:30:00' }),
  rule({
    id: 'daily-capped-max',
    frequency: 'daily',
    startDate: '2024-01-01T00:00:00.000Z',
    maxOccurrences: 365,
  }),
  rule({
    id: 'daily-end-at-cap',
    frequency: 'daily',
    startDate: '2025-01-01T00:00:00.000Z',
    endDate: '2025-12-31T00:00:00.000Z',
  }),
  rule({
    id: 'daily-end-after-cap',
    frequency: 'daily',
    startDate: '2025-01-01T00:00:00.000Z',
    endDate: '2026-01-01T00:00:00.000Z',
  }),
];

const toPlan = (p: ReturnType<typeof planRecurring>) => ({
  occurrences: p.occurrences.map((o) => ({ ruleId: o.rule.id, date: o.date })),
  rules: p.rules,
  cappedRuleIds: p.cappedRuleIds,
});

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('MAX_OCCURRENCES_PER_RULE', [], MAX_OCCURRENCES_PER_RULE));
  const bases = [
    new Date('2026-03-10T00:00:00.000Z'),
    at(2026, 1, 31),
    at(2024, 2, 29, 10),
    new Date('2026-12-31T23:59:59.999Z'),
  ];
  for (const base of bases) {
    for (const f of FREQS) out.push(gc('nextOccurrence', [base, f], nextOccurrence(base, f)));
  }

  const accounts = ['acc-1', 'acc-2'];
  for (const r of rules) {
    out.push(gc('isRulePaused', [r], isRulePaused(r), r.id));
    // Infinity → JSON null; the Kotlin side encodes POSITIVE_INFINITY as null too.
    const rem = remainingOccurrences(r);
    out.push(gc('remainingOccurrences', [r], Number.isFinite(rem) ? rem : null, r.id));
    out.push(gc('nextDueDate', [r], nextDueDate(r), r.id));
    out.push(gc('isRuleFinished', [r], isRuleFinished(r), r.id));
    for (const now of [
      NOW,
      new Date('2026-01-10T00:00:00.000Z'),
      new Date('2019-01-01T00:00:00.000Z'),
    ]) {
      out.push(gc('lastOccurrenceOnOrBefore', [r, now], lastOccurrenceOnOrBefore(r, now), r.id));
      out.push(gc('previewBackfill', [r, accounts, now], previewBackfill(r, accounts, now), r.id));
    }
    out.push(
      gc('planRecurring', [[r], accounts, NOW], toPlan(planRecurring([r], accounts, NOW)), r.id),
    );
    const horizon = new Date('2026-09-15T12:00:00.000Z');
    out.push(gc('futureOccurrences', [r, NOW, horizon], futureOccurrences(r, NOW, horizon), r.id));
    out.push(
      gc(
        'futureOccurrences',
        [r, NOW, new Date('2027-12-31T00:00:00.000Z'), 5],
        futureOccurrences(r, NOW, new Date('2027-12-31T00:00:00.000Z'), 5),
        r.id,
      ),
    );
  }

  // All rules together: order and per-rule caps must be preserved.
  const all = planRecurring(rules, accounts, NOW);
  out.push(gc('planRecurring', [rules, accounts, NOW], toPlan(all), 'all'));
  // A second pass over the advanced rules generates only what the cap held back.
  out.push(
    gc(
      'planRecurring',
      [all.rules, accounts, NOW],
      toPlan(planRecurring(all.rules, accounts, NOW)),
      'second-pass',
    ),
  );
  out.push(
    gc('planRecurring', [rules, [], NOW], toPlan(planRecurring(rules, [], NOW)), 'no-accounts'),
  );
  out.push(
    gc(
      'lastOccurrenceOnOrBefore',
      [{ startDate: '1950-01-01T00:00:00.000Z', frequency: 'daily' }, NOW],
      lastOccurrenceOnOrBefore({ startDate: '1950-01-01T00:00:00.000Z', frequency: 'daily' }, NOW),
      'scan-cap',
    ),
  );
  return out;
}
