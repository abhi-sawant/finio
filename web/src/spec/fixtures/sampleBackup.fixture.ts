import { vi } from 'vitest';
import { generateSampleData, loadSampleData } from '@/data/sampleData';
import { gc, type GoldenCase } from '../golden';

/**
 * A full backup *file* (version/exportedAt + the 16 payload keys) built the way the app builds
 * one: the onboarding sample dataset loaded through the real store's actions into a fresh store,
 * then `collectBackupPayload()` + `withBackupMeta()`. It is the shared round-trip fixture —
 * Android must import it (validateBackup → importData) and arrive at the same state.
 *
 * Made deterministic by faking `Date` (store actions stamp `createdAt` with `new Date()`) and
 * replacing `crypto.randomUUID` with a counter for the duration of the build.
 */
const NOW = new Date('2026-06-15T12:00:00.000Z');

export default async function cases(): Promise<GoldenCase[]> {
  // The store creates its persist middleware at import time, so localStorage has to exist
  // before the module is pulled in (same Map-backed stub as useFinanceStore.test.ts).
  if (typeof globalThis.localStorage === 'undefined') {
    const backing = new Map<string, string>();
    globalThis.localStorage = {
      getItem: (key: string) => backing.get(key) ?? null,
      setItem: (key: string, value: string) => void backing.set(key, value),
      removeItem: (key: string) => void backing.delete(key),
      clear: () => backing.clear(),
      key: (index: number) => Array.from(backing.keys())[index] ?? null,
      get length() {
        return backing.size;
      },
    } as Storage;
  }

  const { useFinanceStore } = await import('@/store/useFinanceStore');
  const { collectBackupPayload } = await import('@/services/backup');
  const { withBackupMeta } = await import('@/utils/backupMeta');
  const { defaultSettings } = await import('@/data/defaultData');

  let counter = 0;
  const uuid = vi
    .spyOn(crypto, 'randomUUID')
    .mockImplementation(
      () =>
        `00000000-0000-4000-8000-${String(++counter).padStart(12, '0')}` as `${string}-${string}-${string}-${string}-${string}`,
    );
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(NOW);
  try {
    useFinanceStore.getState().resetToDefaults();
    useFinanceStore.setState({ settings: defaultSettings });
    const s = useFinanceStore.getState();
    loadSampleData(
      {
        addAccount: s.addAccount,
        addGoal: s.addGoal,
        addPerson: s.addPerson,
        addBudget: s.addBudget,
        addRecurring: s.addRecurring,
        addContribution: s.addContribution,
        addDebtEntry: s.addDebtEntry,
        bulkAddTransactions: s.bulkAddTransactions,
      },
      NOW,
    );
    const file = withBackupMeta(collectBackupPayload(), NOW);
    // Round-trip through JSON so the fixture is exactly what a file on disk holds.
    const out = JSON.parse(JSON.stringify(file)) as unknown;
    // Sanity: the sample is what the file contains, not an empty store.
    if (
      (out as { transactions: unknown[] }).transactions.length !==
      generateSampleData(NOW).transactions.length
    ) {
      throw new Error('sample data did not load into the store');
    }
    return [gc('sampleBackup', [NOW], out)];
  } finally {
    vi.useRealTimers();
    uuid.mockRestore();
    useFinanceStore.getState().resetToDefaults();
  }
}
