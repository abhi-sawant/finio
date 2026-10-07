import { generateSampleData, loadSampleData, type SampleDataActions } from '@/data/sampleData';
import { gc, type GoldenCase } from '../golden';

const NOWS = [
  new Date('2026-06-15T12:00:00.000Z'),
  // 01:30 IST on 1 Feb — local month differs from the UTC one.
  new Date('2026-01-31T20:00:00.000Z'),
  // subMonths from the 31st clamps (31 Mar 2024 → 29 Feb, leap year).
  new Date('2024-03-31T09:15:00.000Z'),
];

/** Fake store actions: deterministic ids, and every call recorded in order. */
function recorder() {
  let next = 0;
  const calls: Array<{ fn: string; arg: unknown }> = [];
  const rec =
    <A, R>(fn: string, result: (arg: A) => R) =>
    (arg: A): R => {
      calls.push({ fn, arg });
      return result(arg);
    };
  const actions: SampleDataActions = {
    addAccount: rec('addAccount', () => `acc-${next++}`),
    addGoal: rec('addGoal', () => `goal-${next++}`),
    addPerson: rec('addPerson', () => `person-${next++}`),
    addBudget: rec('addBudget', () => undefined),
    addRecurring: rec('addRecurring', () => `recurring-${next++}`),
    addContribution: rec('addContribution', () => `contribution-${next++}`),
    addDebtEntry: rec('addDebtEntry', () => `debt-${next++}`),
    bulkAddTransactions: rec('bulkAddTransactions', (t: unknown[]) => t.length),
  };
  return { actions, calls };
}

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  for (const now of NOWS) out.push(gc('generateSampleData', [now], generateSampleData(now)));
  for (const now of NOWS.slice(0, 2)) {
    const { actions, calls } = recorder();
    loadSampleData(actions, now);
    out.push(gc('loadSampleData', [now], calls));
  }
  return out;
}
