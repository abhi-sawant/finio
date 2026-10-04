import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getNetWorth, getTotalAccountBalance } from '@/utils/calculations';
import { defaultSettings } from '@/data/defaultData';
import type {
  Account,
  CategoryRule,
  ImportPayload,
  RecurringTransaction,
  Transaction,
} from '@/types';

// The store creates its persist middleware at import time, so localStorage has to exist
// before the module is pulled in. A Map-backed stub keeps the suite in the node
// environment (no jsdom needed).
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

const { useFinanceStore } = await import('./useFinanceStore');

function account(id: string, balance: number, openingBalance = balance): Account {
  return {
    id,
    name: id,
    type: 'checking',
    color: '#000',
    icon: 'landmark',
    balance,
    openingBalance,
    createdAt: '2026-01-01T00:00:00.000Z',
  };
}

function tx(
  partial: Partial<Transaction> & Pick<Transaction, 'id' | 'type' | 'amount' | 'accountId'>,
): Transaction {
  return {
    categoryId: 'cat-1',
    date: '2026-06-01T00:00:00.000Z',
    note: '',
    labels: [],
    createdAt: '2026-06-01T00:00:00.000Z',
    ...partial,
  };
}

function seed(accounts: Account[], transactions: Transaction[] = []) {
  useFinanceStore.setState({ accounts, transactions });
}

beforeEach(() => {
  backing.clear();
  useFinanceStore.getState().resetToDefaults();
  // resetToDefaults deliberately leaves settings alone, so clear them here for isolation.
  useFinanceStore.setState({ settings: defaultSettings });
});

describe('addAccount / updateAccount', () => {
  it('seeds the opening balance from the balance the user entered', () => {
    useFinanceStore.getState().addAccount({
      name: 'Cash',
      type: 'cash',
      color: '#000',
      icon: 'banknote',
      balance: 5000,
    });
    const [created] = useFinanceStore.getState().accounts;
    expect(created.openingBalance).toBe(5000);
  });

  it('shifts the opening balance when the current balance is edited, keeping deltas intact', () => {
    seed(
      [account('a', 800, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' })],
    );

    // User corrects the current balance to 900.
    useFinanceStore.getState().updateAccount('a', { balance: 900 });
    const [updated] = useFinanceStore.getState().accounts;
    expect(updated.openingBalance).toBe(1100);

    // The edit must survive a reconcile — that is the whole point of moving the opening balance.
    expect(useFinanceStore.getState().recomputeBalances().changed).toBe(0);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(900);
  });
});

describe('recomputeBalances', () => {
  it('repairs drifted balances and reports what it corrected', () => {
    seed(
      [account('a', 999, 1000), account('b', 500, 500)],
      [tx({ id: 't1', type: 'expense', amount: 100, accountId: 'a' })],
    );

    const result = useFinanceStore.getState().recomputeBalances();
    expect(result).toEqual({ changed: 1, totalDrift: -99 });
    expect(useFinanceStore.getState().accounts.map((a) => a.balance)).toEqual([900, 500]);
  });

  it('reports nothing to do when balances already agree', () => {
    seed(
      [account('a', 900, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 100, accountId: 'a' })],
    );
    expect(useFinanceStore.getState().recomputeBalances()).toEqual({ changed: 0, totalDrift: 0 });
  });
});

describe('deleteAccount', () => {
  it('reverses the other side of a transfer instead of leaving it inflated', () => {
    seed(
      [account('a', 700, 1000), account('b', 500, 200)],
      [tx({ id: 't1', type: 'transfer', amount: 300, accountId: 'a', toAccountId: 'b' })],
    );

    useFinanceStore.getState().deleteAccount('a');
    const remaining = useFinanceStore.getState().accounts;
    expect(remaining).toHaveLength(1);
    expect(remaining[0].balance).toBe(200);
    expect(useFinanceStore.getState().transactions).toHaveLength(0);
    // And the survivor is still internally consistent.
    expect(useFinanceStore.getState().recomputeBalances().changed).toBe(0);
  });
});

describe('setAccountArchived', () => {
  it('closes an account without touching its transactions or balance', () => {
    seed(
      [account('a', 800, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' })],
    );

    useFinanceStore.getState().setAccountArchived('a', true);

    const state = useFinanceStore.getState();
    expect(state.accounts[0].archivedAt).toEqual(expect.any(String));
    expect(state.accounts[0].balance).toBe(800);
    expect(state.transactions).toHaveLength(1);
  });

  it('drops the flag entirely when reopening, rather than leaving a falsy value behind', () => {
    seed([account('a', 100)]);
    useFinanceStore.getState().setAccountArchived('a', true);
    useFinanceStore.getState().setAccountArchived('a', false);

    expect('archivedAt' in useFinanceStore.getState().accounts[0]).toBe(false);
  });

  it('excludes archived accounts from running totals but keeps them in the list', () => {
    seed([account('a', 1000), account('b', 500)]);
    useFinanceStore.getState().setAccountArchived('b', true);

    const { accounts } = useFinanceStore.getState();
    expect(accounts).toHaveLength(2);
    expect(getTotalAccountBalance(accounts)).toBe(1000);
    expect(getNetWorth(accounts)).toBe(1000);
  });
});

describe('deleteTransaction / restoreTransaction', () => {
  it('returns the removed row and reverses its balance delta', () => {
    seed(
      [account('a', 800, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' })],
    );

    const removed = useFinanceStore.getState().deleteTransaction('t1');

    expect(removed?.id).toBe('t1');
    expect(useFinanceStore.getState().accounts[0].balance).toBe(1000);
  });

  it('returns null for an unknown id', () => {
    seed([account('a', 100)]);
    expect(useFinanceStore.getState().deleteTransaction('nope')).toBeNull();
  });

  it('restores the row under its original id and re-applies the delta', () => {
    seed(
      [account('a', 800, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' })],
    );

    const removed = useFinanceStore.getState().deleteTransaction('t1')!;
    useFinanceStore.getState().restoreTransaction(removed);

    const state = useFinanceStore.getState();
    expect(state.transactions.map((t) => t.id)).toEqual(['t1']);
    expect(state.accounts[0].balance).toBe(800);
  });

  it('ignores a repeated restore instead of double-counting the delta', () => {
    seed(
      [account('a', 800, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' })],
    );

    const removed = useFinanceStore.getState().deleteTransaction('t1')!;
    useFinanceStore.getState().restoreTransaction(removed);
    useFinanceStore.getState().restoreTransaction(removed);

    const state = useFinanceStore.getState();
    expect(state.transactions).toHaveLength(1);
    expect(state.accounts[0].balance).toBe(800);
  });

  it('reverses both sides of a transfer and puts them back on restore', () => {
    seed(
      [account('a', 700, 1000), account('b', 800, 500)],
      [tx({ id: 't1', type: 'transfer', amount: 300, accountId: 'a', toAccountId: 'b' })],
    );

    const removed = useFinanceStore.getState().deleteTransaction('t1')!;
    let accounts = useFinanceStore.getState().accounts;
    expect(accounts.find((a) => a.id === 'a')!.balance).toBe(1000);
    expect(accounts.find((a) => a.id === 'b')!.balance).toBe(500);

    useFinanceStore.getState().restoreTransaction(removed);
    accounts = useFinanceStore.getState().accounts;
    expect(accounts.find((a) => a.id === 'a')!.balance).toBe(700);
    expect(accounts.find((a) => a.id === 'b')!.balance).toBe(800);
  });
});

describe('bulkDeleteTransactions / restoreTransactions', () => {
  it('removes every listed transaction and reverses their deltas', () => {
    seed(
      [account('a', 500, 1000)],
      [
        tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' }),
        tx({ id: 't2', type: 'expense', amount: 300, accountId: 'a' }),
        tx({ id: 't3', type: 'expense', amount: 50, accountId: 'a' }),
      ],
    );

    const removed = useFinanceStore.getState().bulkDeleteTransactions(['t1', 't2']);

    expect(removed.map((t) => t.id).sort()).toEqual(['t1', 't2']);
    const state = useFinanceStore.getState();
    expect(state.transactions.map((t) => t.id)).toEqual(['t3']);
    // Reversing both removed expenses (200 + 300) off the current balance of 500.
    expect(state.accounts[0].balance).toBe(1000);
  });

  it('returns an empty array and changes nothing for unknown ids', () => {
    seed([account('a', 100)]);
    expect(useFinanceStore.getState().bulkDeleteTransactions(['nope'])).toEqual([]);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100);
  });

  it('restores every row under its original id and re-applies deltas, guarding a repeat', () => {
    seed(
      [account('a', 500, 1000)],
      [
        tx({ id: 't1', type: 'expense', amount: 200, accountId: 'a' }),
        tx({ id: 't2', type: 'expense', amount: 300, accountId: 'a' }),
      ],
    );

    const removed = useFinanceStore.getState().bulkDeleteTransactions(['t1', 't2']);
    useFinanceStore.getState().restoreTransactions(removed);
    useFinanceStore.getState().restoreTransactions(removed);

    const state = useFinanceStore.getState();
    expect(state.transactions.map((t) => t.id).sort()).toEqual(['t1', 't2']);
    expect(state.accounts[0].balance).toBe(500);
  });
});

describe('bulkRecategorize / bulkAddLabel', () => {
  it('reassigns the category of every listed transaction, leaving others untouched', () => {
    seed(
      [account('a', 100)],
      [
        tx({ id: 't1', type: 'expense', amount: 10, accountId: 'a', categoryId: 'cat-1' }),
        tx({ id: 't2', type: 'expense', amount: 10, accountId: 'a', categoryId: 'cat-1' }),
        tx({ id: 't3', type: 'expense', amount: 10, accountId: 'a', categoryId: 'cat-1' }),
      ],
    );

    useFinanceStore.getState().bulkRecategorize(['t1', 't2'], 'cat-2');

    const byId = new Map(useFinanceStore.getState().transactions.map((t) => [t.id, t]));
    expect(byId.get('t1')?.categoryId).toBe('cat-2');
    expect(byId.get('t2')?.categoryId).toBe('cat-2');
    expect(byId.get('t3')?.categoryId).toBe('cat-1');
  });

  it('clears splits when flattening a split transaction to a single category', () => {
    seed(
      [account('a', 100)],
      [
        tx({
          id: 't1',
          type: 'expense',
          amount: 100,
          accountId: 'a',
          categoryId: '',
          splits: [
            { categoryId: 'cat-1', amount: 60 },
            { categoryId: 'cat-2', amount: 40 },
          ],
        }),
      ],
    );

    useFinanceStore.getState().bulkRecategorize(['t1'], 'cat-3');

    const t1 = useFinanceStore.getState().transactions.find((t) => t.id === 't1');
    expect(t1?.categoryId).toBe('cat-3');
    expect(t1?.splits).toBeUndefined();
  });

  it('never recategorizes a transfer, and skips rows the category is not valid for', () => {
    seed(
      [account('a', 100), account('b', 0)],
      [
        tx({ id: 'exp', type: 'expense', amount: 10, accountId: 'a', categoryId: 'cat-1' }),
        tx({ id: 'inc', type: 'income', amount: 10, accountId: 'a', categoryId: 'cat-9' }),
        tx({
          id: 'xfer',
          type: 'transfer',
          amount: 10,
          accountId: 'a',
          toAccountId: 'b',
          categoryId: 'cat-13',
        }),
      ],
    );

    useFinanceStore.getState().bulkRecategorize(['exp', 'inc', 'xfer'], 'cat-2');

    const byId = new Map(useFinanceStore.getState().transactions.map((t) => [t.id, t]));
    expect(byId.get('exp')?.categoryId).toBe('cat-2');
    // Transport is an expense category: the income keeps Salary, the transfer keeps Transfer.
    expect(byId.get('inc')?.categoryId).toBe('cat-9');
    expect(byId.get('xfer')?.categoryId).toBe('cat-13');

    // Even a neutral category never lands on a transfer through a bulk move.
    useFinanceStore.getState().bulkRecategorize(['xfer', 'inc'], 'cat-24');
    const after = new Map(useFinanceStore.getState().transactions.map((t) => [t.id, t]));
    expect(after.get('xfer')?.categoryId).toBe('cat-13');
    expect(after.get('inc')?.categoryId).toBe('cat-24');
  });

  it('ignores an unknown category id', () => {
    seed([account('a', 100)], [tx({ id: 't1', type: 'expense', amount: 10, accountId: 'a' })]);
    useFinanceStore.getState().bulkRecategorize(['t1'], 'nope');
    expect(useFinanceStore.getState().transactions[0].categoryId).toBe('cat-1');
  });

  it('adds a label without duplicating it on a transaction that already carries it', () => {
    seed(
      [account('a', 100)],
      [
        tx({ id: 't1', type: 'expense', amount: 10, accountId: 'a', labels: [] }),
        tx({ id: 't2', type: 'expense', amount: 10, accountId: 'a', labels: ['lbl-1'] }),
      ],
    );

    useFinanceStore.getState().bulkAddLabel(['t1', 't2'], 'lbl-1');

    const byId = new Map(useFinanceStore.getState().transactions.map((t) => [t.id, t]));
    expect(byId.get('t1')?.labels).toEqual(['lbl-1']);
    expect(byId.get('t2')?.labels).toEqual(['lbl-1']);
  });
});

describe('bulkAddTransactions', () => {
  it('inserts every row with fresh ids/createdAt and applies balance deltas', () => {
    seed([account('a', 1000)]);

    const added = useFinanceStore.getState().bulkAddTransactions([
      {
        type: 'expense',
        amount: 100,
        accountId: 'a',
        categoryId: 'cat-1',
        date: '2026-06-01T00:00:00.000Z',
        note: 'Coffee',
        labels: [],
      },
      {
        type: 'income',
        amount: 5000,
        accountId: 'a',
        categoryId: 'cat-1',
        date: '2026-06-02T00:00:00.000Z',
        note: 'Salary',
        labels: [],
      },
    ]);

    expect(added).toBe(2);
    const state = useFinanceStore.getState();
    expect(state.transactions).toHaveLength(2);
    expect(state.transactions.every((t) => t.id && t.createdAt)).toBe(true);
    // 1000 - 100 (expense) + 5000 (income) = 5900.
    expect(state.accounts[0].balance).toBe(5900);
  });

  it('returns 0 and changes nothing for an empty list', () => {
    seed([account('a', 100)]);
    expect(useFinanceStore.getState().bulkAddTransactions([])).toBe(0);
    expect(useFinanceStore.getState().transactions).toHaveLength(0);
  });
});

describe('deleteCategory with split transactions', () => {
  it('reassigns a dangling split entry to the fallback category', () => {
    seed(
      [account('a', 100)],
      [
        tx({
          id: 't1',
          type: 'expense',
          amount: 150,
          accountId: 'a',
          categoryId: '',
          splits: [
            { categoryId: 'cat-1', amount: 100 },
            { categoryId: 'cat-2', amount: 50 },
          ],
        }),
      ],
    );

    useFinanceStore.getState().deleteCategory('cat-1');

    const t1 = useFinanceStore.getState().transactions.find((t) => t.id === 't1');
    expect(t1?.splits).toEqual([
      { categoryId: 'cat-24', amount: 100 },
      { categoryId: 'cat-2', amount: 50 },
    ]);
  });

  it('merges duplicate categories and collapses a split down to a plain category', () => {
    seed(
      [account('a', 150)],
      [
        tx({
          id: 't1',
          type: 'expense',
          amount: 150,
          accountId: 'a',
          categoryId: '',
          splits: [
            { categoryId: 'cat-1', amount: 100 },
            { categoryId: 'cat-24', amount: 50 },
          ],
        }),
      ],
    );

    useFinanceStore.getState().deleteCategory('cat-1');

    const t1 = useFinanceStore.getState().transactions.find((t) => t.id === 't1');
    expect(t1?.splits).toBeUndefined();
    expect(t1?.categoryId).toBe('cat-24');
  });
});

describe('deleteCategory protected categories', () => {
  it('refuses to delete Transfer or Miscellaneous', () => {
    const before = useFinanceStore.getState().categories.length;
    useFinanceStore.getState().deleteCategory('cat-13');
    useFinanceStore.getState().deleteCategory('cat-24');
    const ids = useFinanceStore.getState().categories.map((c) => c.id);
    expect(ids).toContain('cat-13');
    expect(ids).toContain('cat-24');
    expect(ids).toHaveLength(before);
  });

  it('never falls back to Transfer, even if Miscellaneous is somehow missing', () => {
    useFinanceStore.setState((s) => ({
      categories: s.categories.filter((c) => c.id !== 'cat-24'),
    }));
    seed([account('a', 100)], [tx({ id: 't1', type: 'expense', amount: 10, accountId: 'a' })]);

    useFinanceStore.getState().deleteCategory('cat-1');

    const t1 = useFinanceStore.getState().transactions[0];
    expect(t1.categoryId).not.toBe('cat-13');
    expect(t1.categoryId).toBe('cat-24');
  });
});

describe('deleteLabel', () => {
  it('removes budgets scoped to the deleted label, keeping the rest', () => {
    const { addBudget } = useFinanceStore.getState();
    addBudget({
      categoryId: '',
      labelId: 'lbl-1',
      amount: 500,
      period: 'monthly',
      rollover: false,
    });
    addBudget({ categoryId: 'cat-1', amount: 900, period: 'monthly', rollover: false });

    useFinanceStore.getState().deleteLabel('lbl-1');

    const budgets = useFinanceStore.getState().budgets;
    expect(budgets).toHaveLength(1);
    expect(budgets[0].categoryId).toBe('cat-1');
    expect(useFinanceStore.getState().labels.some((l) => l.id === 'lbl-1')).toBe(false);
  });
});

describe('addTemplate / deleteTemplate', () => {
  it('saves a template and returns its id', () => {
    const id = useFinanceStore.getState().addTemplate({
      name: 'Coffee',
      type: 'expense',
      amount: 150,
      accountId: 'a',
      categoryId: 'cat-1',
      note: 'Morning coffee',
      labels: [],
    });

    const [created] = useFinanceStore.getState().templates;
    expect(created.id).toBe(id);
    expect(created.name).toBe('Coffee');
    expect(created.createdAt).toEqual(expect.any(String));
  });

  it('removes a template by id', () => {
    const id = useFinanceStore.getState().addTemplate({
      name: 'Coffee',
      type: 'expense',
      amount: 150,
      accountId: 'a',
      categoryId: 'cat-1',
      note: '',
      labels: [],
    });

    useFinanceStore.getState().deleteTemplate(id);
    expect(useFinanceStore.getState().templates).toEqual([]);
  });

  it('is cleared by resetToDefaults, same as every other finance collection', () => {
    useFinanceStore.getState().addTemplate({
      name: 'Coffee',
      type: 'expense',
      amount: 150,
      accountId: 'a',
      categoryId: 'cat-1',
      note: '',
      labels: [],
    });

    useFinanceStore.getState().resetToDefaults();
    expect(useFinanceStore.getState().templates).toEqual([]);
  });
});

describe('categorization rules', () => {
  function addUberRule(overrides: Partial<Omit<CategoryRule, 'id' | 'createdAt'>> = {}) {
    return useFinanceStore.getState().addRule({
      pattern: 'uber',
      matchType: 'contains',
      scope: 'any',
      categoryId: 'cat-2',
      labelIds: ['lbl-1'],
      enabled: true,
      ...overrides,
    });
  }

  it('appends new rules so an existing rule keeps its priority', () => {
    addUberRule({ pattern: 'first' });
    addUberRule({ pattern: 'second' });
    expect(useFinanceStore.getState().rules.map((r) => r.pattern)).toEqual(['first', 'second']);
  });

  it('moveRule swaps neighbours and stops at the ends', () => {
    const a = addUberRule({ pattern: 'a' });
    const b = addUberRule({ pattern: 'b' });

    useFinanceStore.getState().moveRule(b, 'up');
    expect(useFinanceStore.getState().rules.map((r) => r.pattern)).toEqual(['b', 'a']);

    useFinanceStore.getState().moveRule(b, 'up');
    expect(useFinanceStore.getState().rules.map((r) => r.pattern)).toEqual(['b', 'a']);

    useFinanceStore.getState().moveRule(a, 'down');
    expect(useFinanceStore.getState().rules.map((r) => r.pattern)).toEqual(['b', 'a']);
  });

  it('applyRulesToExisting recategorizes matching rows and reports them for undo', () => {
    seed(
      [account('a', 1000)],
      [
        tx({ id: 't1', type: 'expense', amount: 100, accountId: 'a', note: 'Uber to work' }),
        tx({ id: 't2', type: 'expense', amount: 50, accountId: 'a', note: 'Groceries' }),
      ],
    );
    addUberRule();

    const { changed, previous } = useFinanceStore.getState().applyRulesToExisting();
    expect(changed).toBe(1);

    const [t1, t2] = useFinanceStore.getState().transactions;
    expect(t1).toMatchObject({ id: 't1', categoryId: 'cat-2', labels: ['lbl-1'] });
    expect(t2).toMatchObject({ id: 't2', categoryId: 'cat-1', labels: [] });

    useFinanceStore.getState().restoreCategorization(previous);
    expect(useFinanceStore.getState().transactions[0]).toMatchObject({
      categoryId: 'cat-1',
      labels: [],
    });
  });

  it('leaves balances alone — recategorizing is not a money change', () => {
    seed(
      [account('a', 900, 1000)],
      [tx({ id: 't1', type: 'expense', amount: 100, accountId: 'a', note: 'Uber' })],
    );
    addUberRule();

    useFinanceStore.getState().applyRulesToExisting();
    expect(useFinanceStore.getState().accounts[0].balance).toBe(900);
  });

  it('restricts the replay to one category when asked', () => {
    seed(
      [account('a', 1000)],
      [
        tx({
          id: 't1',
          type: 'expense',
          amount: 10,
          accountId: 'a',
          note: 'Uber',
          categoryId: 'cat-24',
        }),
        tx({
          id: 't2',
          type: 'expense',
          amount: 10,
          accountId: 'a',
          note: 'Uber',
          categoryId: 'cat-3',
        }),
      ],
    );
    addUberRule();

    const { changed } = useFinanceStore
      .getState()
      .applyRulesToExisting({ restrictToCategoryId: 'cat-24' });
    expect(changed).toBe(1);
    expect(useFinanceStore.getState().transactions[1].categoryId).toBe('cat-3');
  });

  it('deleting a category repoints rules at the fallback instead of orphaning them', () => {
    addUberRule({ categoryId: 'cat-2' });
    useFinanceStore.getState().deleteCategory('cat-2');
    expect(useFinanceStore.getState().rules[0].categoryId).toBe('cat-24');
  });

  it('deleting a label strips it from every rule that applied it', () => {
    addUberRule({ labelIds: ['lbl-1', 'lbl-2'] });
    useFinanceStore.getState().deleteLabel('lbl-1');
    expect(useFinanceStore.getState().rules[0].labelIds).toEqual(['lbl-2']);
  });

  it('is cleared by resetToDefaults, same as every other finance collection', () => {
    addUberRule();
    useFinanceStore.getState().resetToDefaults();
    expect(useFinanceStore.getState().rules).toEqual([]);
  });
});

describe('addGoal / updateGoal / deleteGoal', () => {
  it('creates a goal and returns its id', () => {
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });

    const [created] = useFinanceStore.getState().goals;
    expect(created.id).toBe(id);
    expect(created.name).toBe('Emergency Fund');
    expect(created.createdAt).toEqual(expect.any(String));
  });

  it('updates only the given fields', () => {
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });

    useFinanceStore.getState().updateGoal(id, { targetAmount: 15000 });
    const [updated] = useFinanceStore.getState().goals;
    expect(updated.targetAmount).toBe(15000);
    expect(updated.name).toBe('Emergency Fund');
  });

  it('deleting a goal removes every contribution logged against it, not other goals', () => {
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });
    const otherId = useFinanceStore.getState().addGoal({
      name: 'Vacation',
      icon: 'plane',
      color: '#f59e0b',
      targetAmount: 5000,
    });
    useFinanceStore.getState().addContribution({ goalId: id, amount: 1000, date: '', note: '' });
    useFinanceStore
      .getState()
      .addContribution({ goalId: otherId, amount: 500, date: '', note: '' });

    useFinanceStore.getState().deleteGoal(id);

    const state = useFinanceStore.getState();
    expect(state.goals.map((g) => g.id)).toEqual([otherId]);
    expect(state.goalContributions.map((c) => c.goalId)).toEqual([otherId]);
  });

  it('deleting a goal drops the link from any recurring rule funding it, not the rule itself', () => {
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });
    const ruleId = useFinanceStore.getState().addRecurring({
      type: 'expense',
      amount: 500,
      accountId: 'a',
      categoryId: 'cat-1',
      note: '',
      labels: [],
      frequency: 'monthly',
      startDate: '2026-01-01T00:00:00.000Z',
      goalId: id,
    });

    useFinanceStore.getState().deleteGoal(id);

    const rule = useFinanceStore.getState().recurring.find((r) => r.id === ruleId);
    expect(rule).toBeDefined();
    expect(rule?.goalId).toBeUndefined();
  });

  it('is cleared by resetToDefaults, same as every other finance collection', () => {
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });
    useFinanceStore.getState().addContribution({ goalId: id, amount: 1000, date: '', note: '' });

    useFinanceStore.getState().resetToDefaults();
    expect(useFinanceStore.getState().goals).toEqual([]);
    expect(useFinanceStore.getState().goalContributions).toEqual([]);
  });
});

describe('addContribution / deleteContribution / restoreContribution', () => {
  it('adds a contribution and returns its id', () => {
    const id = useFinanceStore
      .getState()
      .addContribution({ goalId: 'goal-1', amount: 500, date: '2026-01-05', note: 'Bonus' });

    const [created] = useFinanceStore.getState().goalContributions;
    expect(created.id).toBe(id);
    expect(created.amount).toBe(500);
    expect(created.createdAt).toEqual(expect.any(String));
  });

  it('clamps a withdrawal to what the goal holds', () => {
    const { addContribution } = useFinanceStore.getState();
    addContribution({ goalId: 'goal-1', amount: 300, date: '2026-01-05', note: '' });
    addContribution({ goalId: 'goal-1', amount: -500, date: '2026-01-06', note: '' });
    const total = useFinanceStore
      .getState()
      .goalContributions.reduce((sum, c) => sum + c.amount, 0);
    expect(total).toBe(0);
  });

  it('deletes a contribution and returns the removed row for undo', () => {
    const id = useFinanceStore
      .getState()
      .addContribution({ goalId: 'goal-1', amount: 500, date: '2026-01-05', note: '' });

    const removed = useFinanceStore.getState().deleteContribution(id);
    expect(removed?.id).toBe(id);
    expect(useFinanceStore.getState().goalContributions).toEqual([]);
  });

  it('returns null when deleting a contribution that no longer exists', () => {
    expect(useFinanceStore.getState().deleteContribution('missing')).toBeNull();
  });

  it('restoreContribution re-inserts the exact row deleted, verbatim', () => {
    const id = useFinanceStore
      .getState()
      .addContribution({ goalId: 'goal-1', amount: 500, date: '2026-01-05', note: 'Bonus' });
    const removed = useFinanceStore.getState().deleteContribution(id)!;

    useFinanceStore.getState().restoreContribution(removed);
    expect(useFinanceStore.getState().goalContributions).toEqual([removed]);
  });

  it('guards against a double undo re-inserting the same contribution twice', () => {
    const id = useFinanceStore
      .getState()
      .addContribution({ goalId: 'goal-1', amount: 500, date: '2026-01-05', note: '' });
    const removed = useFinanceStore.getState().deleteContribution(id)!;

    useFinanceStore.getState().restoreContribution(removed);
    useFinanceStore.getState().restoreContribution(removed);
    expect(useFinanceStore.getState().goalContributions).toHaveLength(1);
  });
});

describe('deleteAccount clears dangling goal links', () => {
  it('drops linkedAccountId from a goal pointing at the deleted account, without deleting the goal', () => {
    seed([account('a', 1000)]);
    const id = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
      linkedAccountId: 'a',
    });

    useFinanceStore.getState().deleteAccount('a');

    const [goal] = useFinanceStore.getState().goals;
    expect(goal.id).toBe(id);
    expect(goal.linkedAccountId).toBeUndefined();
  });
});

describe('addPerson / updatePerson / deletePerson', () => {
  it('creates a person and returns its id', () => {
    const id = useFinanceStore
      .getState()
      .addPerson({ name: 'Rahul', icon: 'user', color: '#146b54' });

    const [created] = useFinanceStore.getState().people;
    expect(created.id).toBe(id);
    expect(created.name).toBe('Rahul');
    expect(created.createdAt).toEqual(expect.any(String));
  });

  it('updates only the given fields', () => {
    const id = useFinanceStore
      .getState()
      .addPerson({ name: 'Rahul', icon: 'user', color: '#146b54' });

    useFinanceStore.getState().updatePerson(id, { name: 'Rahul Sharma' });
    const [updated] = useFinanceStore.getState().people;
    expect(updated.name).toBe('Rahul Sharma');
    expect(updated.icon).toBe('user');
  });

  it('deleting a person removes every debt entry logged against them, not other people', () => {
    const id = useFinanceStore
      .getState()
      .addPerson({ name: 'Rahul', icon: 'user', color: '#146b54' });
    const otherId = useFinanceStore
      .getState()
      .addPerson({ name: 'Priya', icon: 'user', color: '#f59e0b' });
    useFinanceStore.getState().addDebtEntry({ personId: id, amount: 500, date: '', note: '' });
    useFinanceStore.getState().addDebtEntry({ personId: otherId, amount: 200, date: '', note: '' });

    useFinanceStore.getState().deletePerson(id);

    const state = useFinanceStore.getState();
    expect(state.people.map((p) => p.id)).toEqual([otherId]);
    expect(state.debtEntries.map((e) => e.personId)).toEqual([otherId]);
  });

  it('is cleared by resetToDefaults, same as every other finance collection', () => {
    const id = useFinanceStore
      .getState()
      .addPerson({ name: 'Rahul', icon: 'user', color: '#146b54' });
    useFinanceStore.getState().addDebtEntry({ personId: id, amount: 500, date: '', note: '' });

    useFinanceStore.getState().resetToDefaults();
    expect(useFinanceStore.getState().people).toEqual([]);
    expect(useFinanceStore.getState().debtEntries).toEqual([]);
  });
});

describe('addDebtEntry / deleteDebtEntry / restoreDebtEntry', () => {
  it('adds an entry and returns its id', () => {
    const id = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 500, date: '2026-01-05', note: 'Lunch' });

    const [created] = useFinanceStore.getState().debtEntries;
    expect(created.id).toBe(id);
    expect(created.amount).toBe(500);
    expect(created.createdAt).toEqual(expect.any(String));
  });

  it('deletes an entry and returns the removed row for undo', () => {
    const id = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 500, date: '2026-01-05', note: '' });

    const removed = useFinanceStore.getState().deleteDebtEntry(id);
    expect(removed?.id).toBe(id);
    expect(useFinanceStore.getState().debtEntries).toEqual([]);
  });

  it('returns null when deleting an entry that no longer exists', () => {
    expect(useFinanceStore.getState().deleteDebtEntry('missing')).toBeNull();
  });

  it('restoreDebtEntry re-inserts the exact row deleted, verbatim', () => {
    const id = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 500, date: '2026-01-05', note: 'Lunch' });
    const removed = useFinanceStore.getState().deleteDebtEntry(id)!;

    useFinanceStore.getState().restoreDebtEntry(removed);
    expect(useFinanceStore.getState().debtEntries).toEqual([removed]);
  });

  it('guards against a double undo re-inserting the same entry twice', () => {
    const id = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 500, date: '2026-01-05', note: '' });
    const removed = useFinanceStore.getState().deleteDebtEntry(id)!;

    useFinanceStore.getState().restoreDebtEntry(removed);
    useFinanceStore.getState().restoreDebtEntry(removed);
    expect(useFinanceStore.getState().debtEntries).toHaveLength(1);
  });
  it('deleting a settled entry also deletes its transaction, and undo restores both', () => {
    seed(
      [account('a', 1500, 2000)],
      [tx({ id: 'settle', type: 'expense', amount: 500, accountId: 'a' })],
    );
    const id = useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: 'Settled up',
      settledTransactionId: 'settle',
    });

    const removed = useFinanceStore.getState().deleteDebtEntry(id)!;
    let state = useFinanceStore.getState();
    expect(state.debtEntries).toEqual([]);
    expect(state.transactions).toEqual([]);
    expect(state.accounts[0].balance).toBe(2000);

    useFinanceStore.getState().restoreDebtEntry(removed);
    useFinanceStore.getState().restoreDebtEntry(removed);
    state = useFinanceStore.getState();
    expect(state.debtEntries).toHaveLength(1);
    expect(state.transactions.map((t) => t.id)).toEqual(['settle']);
    expect(state.accounts[0].balance).toBe(1500);
  });

  it('deleting a settlement transaction also removes its entry, and undo restores both', () => {
    seed(
      [account('a', 1500, 2000)],
      [tx({ id: 'settle', type: 'expense', amount: 500, accountId: 'a' })],
    );
    useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: 'Settled up',
      settledTransactionId: 'settle',
    });
    const other = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 200, date: '2026-01-06', note: '' });

    const removed = useFinanceStore.getState().deleteTransaction('settle')!;
    let state = useFinanceStore.getState();
    expect(state.debtEntries.map((e) => e.id)).toEqual([other]);
    expect(state.accounts[0].balance).toBe(2000);

    useFinanceStore.getState().restoreTransaction(removed);
    useFinanceStore.getState().restoreTransaction(removed);
    state = useFinanceStore.getState();
    expect(state.debtEntries).toHaveLength(2);
    expect(state.debtEntries.some((e) => e.settledTransactionId === 'settle')).toBe(true);
    expect(state.accounts[0].balance).toBe(1500);
  });

  it('bulk-deleting a settlement transaction removes its entry, and bulk undo restores it', () => {
    seed(
      [account('a', 1500, 2000)],
      [
        tx({ id: 'settle', type: 'expense', amount: 500, accountId: 'a' }),
        tx({ id: 'plain', type: 'expense', amount: 1, accountId: 'a' }),
      ],
    );
    useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: 'Settled up',
      settledTransactionId: 'settle',
    });

    const removed = useFinanceStore.getState().bulkDeleteTransactions(['settle', 'plain']);
    expect(useFinanceStore.getState().debtEntries).toEqual([]);

    useFinanceStore.getState().restoreTransactions(removed);
    expect(useFinanceStore.getState().debtEntries).toHaveLength(1);
    expect(useFinanceStore.getState().transactions).toHaveLength(2);
  });

  it('deletes a settled entry cleanly when its transaction is already gone', () => {
    seed([account('a', 100)]);
    const id = useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: '',
      settledTransactionId: 'missing',
    });
    useFinanceStore.getState().deleteDebtEntry(id);
    expect(useFinanceStore.getState().debtEntries).toEqual([]);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100);
  });
});

describe('updateDebtEntry', () => {
  function seedSettlement() {
    // Rahul owed you; he paid back 500 into account `a` (an income), so the entry is -500.
    seed(
      [account('a', 2500, 2000)],
      [
        tx({
          id: 'settle',
          type: 'income',
          amount: 500,
          accountId: 'a',
          date: '2026-01-05T10:00:00.000Z',
          note: 'Settled up with Rahul',
        }),
      ],
    );
    return useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: -500,
      date: '2026-01-05T10:00:00.000Z',
      note: 'Settled up with Rahul',
      settledTransactionId: 'settle',
    });
  }

  it('edits a plain entry in place, including flipping its direction', () => {
    seed([account('a', 100)]);
    const id = useFinanceStore
      .getState()
      .addDebtEntry({ personId: 'person-1', amount: 500, date: '2026-01-05', note: 'Lunch' });

    expect(
      useFinanceStore
        .getState()
        .updateDebtEntry(id, { amount: -750, date: '2026-01-07', note: '  Dinner ' }),
    ).toBe(true);
    const entry = useFinanceStore.getState().debtEntries[0];
    expect(entry).toMatchObject({ id, amount: -750, date: '2026-01-07', note: 'Dinner' });
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100);
  });

  it('a settled edit updates the linked transaction and moves the balance by the difference', () => {
    const id = seedSettlement();
    useFinanceStore
      .getState()
      .updateDebtEntry(id, { amount: -800, date: '2026-01-09T10:00:00.000Z', note: 'Cash' });

    const state = useFinanceStore.getState();
    expect(state.debtEntries[0]).toMatchObject({
      amount: -800,
      date: '2026-01-09T10:00:00.000Z',
      note: 'Cash',
    });
    expect(state.transactions[0]).toMatchObject({
      id: 'settle',
      type: 'income',
      amount: 800,
      date: '2026-01-09T10:00:00.000Z',
      // The note is the entry's alone.
      note: 'Settled up with Rahul',
    });
    expect(state.accounts[0].balance).toBe(2800);
    expect(state.accounts[0].openingBalance).toBe(2000);
  });

  it("a settled entry's direction cannot flip — only its magnitude changes", () => {
    const id = seedSettlement();
    useFinanceStore.getState().updateDebtEntry(id, { amount: 300 });

    const state = useFinanceStore.getState();
    expect(state.debtEntries[0].amount).toBe(-300);
    expect(state.transactions[0]).toMatchObject({ type: 'income', amount: 300 });
    expect(state.accounts[0].balance).toBe(2300);
  });

  it('updateTransaction on a settlement carries amount and date over to its entry', () => {
    const id = seedSettlement();
    useFinanceStore
      .getState()
      .updateTransaction('settle', { amount: 650, date: '2026-01-06T09:00:00.000Z' });

    const state = useFinanceStore.getState();
    expect(state.debtEntries.find((e) => e.id === id)).toMatchObject({
      amount: -650,
      date: '2026-01-06T09:00:00.000Z',
      note: 'Settled up with Rahul',
    });
    expect(state.accounts[0].balance).toBe(2650);
  });

  it('updateTransaction leaves entries alone when amount and date are unchanged', () => {
    seedSettlement();
    const before = useFinanceStore.getState().debtEntries;
    useFinanceStore.getState().updateTransaction('settle', { note: 'Renamed' });
    expect(useFinanceStore.getState().debtEntries).toBe(before);
  });

  it('ignores a zero amount, and an unknown id is a no-op', () => {
    const id = seedSettlement();
    const before = useFinanceStore.getState();
    expect(useFinanceStore.getState().updateDebtEntry('missing', { amount: 1 })).toBe(false);
    expect(useFinanceStore.getState()).toBe(before);

    useFinanceStore.getState().updateDebtEntry(id, { amount: 0 });
    const state = useFinanceStore.getState();
    expect(state.debtEntries[0].amount).toBe(-500);
    expect(state.transactions[0].amount).toBe(500);
    expect(state.accounts[0].balance).toBe(2500);
  });

  it('edits a settled entry cleanly when its transaction is already gone', () => {
    seed([account('a', 100)]);
    const id = useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: '',
      settledTransactionId: 'missing',
    });
    useFinanceStore.getState().updateDebtEntry(id, { amount: 900 });
    expect(useFinanceStore.getState().debtEntries[0].amount).toBe(900);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100);
  });

  it('flips the settled entry direction when its transaction changes type', () => {
    seed(
      [account('a', 1500, 2000)],
      [tx({ id: 'settle', type: 'expense', amount: 500, accountId: 'a' })],
    );
    useFinanceStore.getState().addDebtEntry({
      personId: 'person-1',
      amount: 500,
      date: '2026-01-05',
      note: 'Settled up',
      settledTransactionId: 'settle',
    });

    useFinanceStore.getState().updateTransaction('settle', { type: 'income' });
    expect(useFinanceStore.getState().debtEntries[0].amount).toBe(-500);

    useFinanceStore.getState().updateTransaction('settle', { type: 'expense' });
    expect(useFinanceStore.getState().debtEntries[0].amount).toBe(500);
  });
});

describe('importData', () => {
  const payload: ImportPayload = {
    accounts: [account('imported', 0, 1000)],
    transactions: [tx({ id: 'it1', type: 'expense', amount: 250, accountId: 'imported' })],
  };

  it('replaces collections and recomputes balances from the imported transactions', () => {
    seed(
      [account('local', 4000)],
      [tx({ id: 'lt1', type: 'income', amount: 4000, accountId: 'local' })],
    );

    useFinanceStore.getState().importData(payload, { mode: 'replace' });
    const state = useFinanceStore.getState();

    expect(state.accounts.map((a) => a.id)).toEqual(['imported']);
    // The file's stale `balance: 0` is discarded in favour of opening + deltas.
    expect(state.accounts[0].balance).toBe(750);
    expect(state.transactions.map((t) => t.id)).toEqual(['it1']);
  });

  it('merges by id, with incoming rows winning and local rows kept', () => {
    seed(
      [account('local', 4000, 4000), account('imported', 0, 0)],
      [tx({ id: 'lt1', type: 'income', amount: 0, accountId: 'local' })],
    );

    useFinanceStore.getState().importData(payload, { mode: 'merge' });
    const state = useFinanceStore.getState();

    expect(state.accounts.map((a) => a.id).sort()).toEqual(['imported', 'local']);
    expect(state.transactions.map((t) => t.id).sort()).toEqual(['it1', 'lt1']);
    // Incoming account replaced the local stub of the same id, then got recomputed.
    expect(state.accounts.find((a) => a.id === 'imported')?.balance).toBe(750);
    expect(state.accounts.find((a) => a.id === 'local')?.balance).toBe(4000);
  });

  it('derives an opening balance for accounts imported without one', () => {
    const legacy: ImportPayload = {
      accounts: [{ ...account('legacy', 750), openingBalance: undefined }],
      transactions: [tx({ id: 'lg1', type: 'expense', amount: 250, accountId: 'legacy' })],
    };

    useFinanceStore.getState().importData(legacy, { mode: 'replace' });
    const [imported] = useFinanceStore.getState().accounts;
    expect(imported.openingBalance).toBe(1000);
    // Balance-neutral: what the backup said the balance was is what you get.
    expect(imported.balance).toBe(750);
  });

  it('in replace mode, empties data collections the file lacks but keeps categories/labels', () => {
    seed([account('local', 100)]);
    useFinanceStore.getState().addGoal({
      name: 'Old goal',
      icon: 'target',
      color: '#146b54',
      targetAmount: 1000,
    });
    useFinanceStore.setState({
      netWorthSnapshots: [
        {
          id: 'snap',
          periodKey: '2026-03',
          date: '2026-03-31',
          assets: 500,
          liabilities: 0,
          createdAt: '2026-04-01T00:00:00.000Z',
        },
      ],
    });
    const categories = useFinanceStore.getState().categories;
    const labels = useFinanceStore.getState().labels;

    useFinanceStore.getState().importData({ transactions: [] }, { mode: 'replace' });

    const state = useFinanceStore.getState();
    expect(state.categories).toBe(categories);
    expect(state.labels).toBe(labels);
    expect(state.accounts).toEqual([]);
    expect(state.goals).toEqual([]);
    expect(state.netWorthSnapshots).toEqual([]);
  });

  it('in merge mode, leaves collections the file lacks alone', () => {
    seed([account('local', 100)]);
    useFinanceStore.getState().importData({ transactions: [] }, { mode: 'merge' });
    expect(useFinanceStore.getState().accounts.map((a) => a.id)).toEqual(['local']);
  });

  it('merges goals and contributions by id, same as every other collection', () => {
    const localId = useFinanceStore.getState().addGoal({
      name: 'Local Goal',
      icon: 'target',
      color: '#146b54',
      targetAmount: 1000,
    });

    useFinanceStore.getState().importData(
      {
        goals: [
          {
            id: 'imported-goal',
            name: 'Imported Goal',
            icon: 'plane',
            color: '#f59e0b',
            targetAmount: 5000,
            createdAt: '2026-01-01T00:00:00.000Z',
          },
        ],
        goalContributions: [
          {
            id: 'imported-contrib',
            goalId: 'imported-goal',
            amount: 1000,
            date: '2026-01-02T00:00:00.000Z',
            note: '',
            createdAt: '2026-01-02T00:00:00.000Z',
          },
        ],
      },
      { mode: 'merge' },
    );

    const state = useFinanceStore.getState();
    expect(state.goals.map((g) => g.id).sort()).toEqual([localId, 'imported-goal'].sort());
    expect(state.goalContributions.map((c) => c.id)).toEqual(['imported-contrib']);
  });

  it('merges people and debt entries by id, same as every other collection', () => {
    const localId = useFinanceStore
      .getState()
      .addPerson({ name: 'Local Person', icon: 'user', color: '#146b54' });

    useFinanceStore.getState().importData(
      {
        people: [
          {
            id: 'imported-person',
            name: 'Imported Person',
            icon: 'handshake',
            color: '#f59e0b',
            createdAt: '2026-01-01T00:00:00.000Z',
          },
        ],
        debtEntries: [
          {
            id: 'imported-entry',
            personId: 'imported-person',
            amount: 500,
            date: '2026-01-02T00:00:00.000Z',
            note: '',
            createdAt: '2026-01-02T00:00:00.000Z',
          },
        ],
      },
      { mode: 'merge' },
    );

    const state = useFinanceStore.getState();
    expect(state.people.map((p) => p.id).sort()).toEqual([localId, 'imported-person'].sort());
    expect(state.debtEntries.map((e) => e.id)).toEqual(['imported-entry']);
  });
});

describe('addBudget', () => {
  const base = { amount: 1000, period: 'monthly' as const, rollover: false };

  it('replaces an existing budget for the same scope', () => {
    useFinanceStore.getState().addBudget({ ...base, categoryId: 'cat-1' });
    useFinanceStore.getState().addBudget({ ...base, categoryId: 'cat-1', amount: 2000 });

    const { budgets } = useFinanceStore.getState();
    expect(budgets).toHaveLength(1);
    expect(budgets[0].amount).toBe(2000);
  });

  it('keeps overall, category and label budgets side by side', () => {
    useFinanceStore.getState().addBudget({ ...base, categoryId: '' });
    useFinanceStore.getState().addBudget({ ...base, categoryId: 'cat-1' });
    useFinanceStore.getState().addBudget({ ...base, categoryId: '', labelId: 'lbl-1' });

    expect(useFinanceStore.getState().budgets).toHaveLength(3);
  });
});

describe('recurring rules', () => {
  function recurringRule(partial: Partial<RecurringTransaction> = {}): RecurringTransaction {
    return {
      id: 'r1',
      type: 'expense',
      amount: 300,
      accountId: 'a',
      categoryId: 'cat-1',
      note: '',
      labels: [],
      frequency: 'monthly',
      // Long past, but capped at a single occurrence so the test is date-independent.
      startDate: '2020-01-01T00:00:00.000Z',
      maxOccurrences: 1,
      occurrenceCount: 0,
      lastRunDate: null,
      createdAt: '2020-01-01T00:00:00.000Z',
      ...partial,
    };
  }

  it('generates a transfer occurrence and moves both balances', () => {
    seed([account('a', 1000), account('b', 500)]);
    useFinanceStore.setState({
      recurring: [recurringRule({ type: 'transfer', toAccountId: 'b' })],
    });

    expect(useFinanceStore.getState().processRecurring()).toHaveLength(1);

    const state = useFinanceStore.getState();
    expect(state.transactions[0].toAccountId).toBe('b');
    expect(state.accounts.find((a) => a.id === 'a')?.balance).toBe(700);
    expect(state.accounts.find((a) => a.id === 'b')?.balance).toBe(800);
    expect(state.recurring[0].occurrenceCount).toBe(1);
    // And the rule is spent — a second pass adds nothing.
    expect(useFinanceStore.getState().processRecurring()).toHaveLength(0);
  });

  it('skips a transfer rule whose destination account is gone', () => {
    seed([account('a', 1000)]);
    useFinanceStore.setState({
      recurring: [recurringRule({ type: 'transfer', toAccountId: 'missing' })],
    });

    expect(useFinanceStore.getState().processRecurring()).toHaveLength(0);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(1000);
  });

  it('generates nothing while paused and resumes cleanly', () => {
    seed([account('a', 1000)]);
    useFinanceStore.setState({ recurring: [recurringRule()] });

    useFinanceStore.getState().setRecurringPaused('r1', true);
    expect(useFinanceStore.getState().processRecurring()).toHaveLength(0);

    useFinanceStore.getState().setRecurringPaused('r1', false);
    expect('pausedAt' in useFinanceStore.getState().recurring[0]).toBe(false);
    expect(useFinanceStore.getState().processRecurring()).toHaveLength(1);
  });

  it('returns the generated rows so a caller can undo them via bulkDeleteTransactions', () => {
    seed([account('a', 1000)]);
    useFinanceStore.setState({ recurring: [recurringRule()] });

    const generated = useFinanceStore.getState().processRecurring();
    expect(generated).toHaveLength(1);
    expect(generated[0].recurringId).toBe('r1');

    useFinanceStore.getState().bulkDeleteTransactions(generated.map((t) => t.id));
    expect(useFinanceStore.getState().transactions).toHaveLength(0);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(1000);
  });

  it('drops transfer rules that pointed at a deleted account', () => {
    seed([account('a', 1000), account('b', 500)]);
    useFinanceStore.setState({
      recurring: [recurringRule({ type: 'transfer', toAccountId: 'b' })],
    });

    useFinanceStore.getState().deleteAccount('b');
    expect(useFinanceStore.getState().recurring).toEqual([]);
  });

  it('auto-funds a linked goal with a matching contribution on each occurrence', () => {
    seed([account('a', 1000)]);
    const goalId = useFinanceStore.getState().addGoal({
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 10000,
    });
    useFinanceStore.setState({ recurring: [recurringRule({ goalId })] });

    useFinanceStore.getState().processRecurring();

    const { goalContributions } = useFinanceStore.getState();
    expect(goalContributions).toHaveLength(1);
    expect(goalContributions[0]).toMatchObject({ goalId, amount: 300 });
  });

  it('never resurrects a contribution for a goal that no longer exists', () => {
    seed([account('a', 1000)]);
    useFinanceStore.setState({ recurring: [recurringRule({ goalId: 'deleted-goal' })] });

    useFinanceStore.getState().processRecurring();

    expect(useFinanceStore.getState().goalContributions).toEqual([]);
  });
});

describe('v7 migration', () => {
  it('gives existing budgets and rules their pre-v7 behaviour explicitly', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 6,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          budgets: [{ id: 'b1', categoryId: 'cat-1', amount: 500, createdAt: '2026-01-01' }],
          recurring: [
            {
              id: 'r1',
              type: 'expense',
              amount: 10,
              accountId: 'a',
              categoryId: 'cat-1',
              note: '',
              labels: [],
              frequency: 'monthly',
              startDate: '2026-01-01T00:00:00.000Z',
              lastRunDate: null,
              createdAt: '2026-01-01T00:00:00.000Z',
            },
          ],
          settings: { theme: 'dark', userName: 'Alex', autoLocalBackup: false },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const state = useFinanceStore.getState();

    expect(state.budgets[0]).toMatchObject({ period: 'monthly', rollover: false });
    expect(state.recurring[0].occurrenceCount).toBe(0);
    expect(state.settings.monthStartDay).toBe(1);
    expect(state.settings.userName).toBe('Alex');
  });
});

describe('v5 migration', () => {
  it('backfills opening balances from persisted v4 state without moving any balance', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 4,
        state: {
          accounts: [
            { ...account('a', 750), openingBalance: undefined, currency: 'INR' },
            { ...account('b', 1200), openingBalance: undefined },
          ],
          transactions: [
            tx({ id: 't1', type: 'expense', amount: 250, accountId: 'a' }),
            tx({ id: 't2', type: 'transfer', amount: 200, accountId: 'a', toAccountId: 'b' }),
          ],
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const accounts = useFinanceStore.getState().accounts;

    expect(accounts.map((a) => a.balance)).toEqual([750, 1200]);
    expect(accounts.map((a) => a.openingBalance)).toEqual([1200, 1000]);
    // Reconciling straight after a migration must be a no-op.
    expect(useFinanceStore.getState().recomputeBalances().changed).toBe(0);
  });
});

describe('v9 migration', () => {
  it('seeds empty goals and goalContributions arrays for pre-v9 state', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 8,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            monthStartDay: 1,
            hideAmounts: false,
          },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const state = useFinanceStore.getState();

    expect(state.goals).toEqual([]);
    expect(state.goalContributions).toEqual([]);
  });
});

describe('v10 migration', () => {
  it('seeds empty people and debtEntries arrays for pre-v10 state', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 9,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            monthStartDay: 1,
            hideAmounts: false,
          },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const state = useFinanceStore.getState();

    expect(state.people).toEqual([]);
    expect(state.debtEntries).toEqual([]);
  });
});

describe('v11 migration', () => {
  it('seeds an empty rules array for pre-v11 state, recategorizing nothing on upgrade', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 10,
        state: {
          accounts: [account('a', 100)],
          transactions: [
            tx({ id: 't1', type: 'expense', amount: 20, accountId: 'a', note: 'Uber' }),
          ],
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            monthStartDay: 1,
            hideAmounts: false,
          },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const state = useFinanceStore.getState();

    expect(state.rules).toEqual([]);
    expect(state.transactions[0].categoryId).toBe('cat-1');
  });
});

describe('v8 migration', () => {
  it('seeds hideAmounts and an empty templates array for pre-v8 state', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 7,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          settings: { theme: 'dark', userName: 'Alex', autoLocalBackup: false, monthStartDay: 1 },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const state = useFinanceStore.getState();

    expect(state.settings.hideAmounts).toBe(false);
    expect(state.settings.userName).toBe('Alex');
    expect(state.templates).toEqual([]);
  });
});

describe('v6 migration', () => {
  it('marks an existing install as onboarded so the first-run wizard stays hidden', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 5,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          settings: { theme: 'dark', userName: 'Alex', autoLocalBackup: false },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();
    const { settings } = useFinanceStore.getState();

    expect(settings.onboardedAt).toEqual(expect.any(String));
    // The migration must not overwrite settings the user already chose.
    expect(settings.userName).toBe('Alex');
    expect(settings.theme).toBe('dark');
  });

  it('leaves a fresh install un-onboarded so the wizard runs', () => {
    expect(defaultSettings.onboardedAt).toBeUndefined();
    expect(defaultSettings.userName).toBe('');
  });

  it('keeps the user onboarded after a data reset', () => {
    useFinanceStore.setState({
      settings: { ...defaultSettings, userName: 'Riya', onboardedAt: '2026-01-01T00:00:00.000Z' },
    });
    seed([account('a', 100)], [tx({ id: 't1', type: 'expense', amount: 10, accountId: 'a' })]);

    useFinanceStore.getState().resetToDefaults();

    const state = useFinanceStore.getState();
    expect(state.accounts).toEqual([]);
    expect(state.transactions).toEqual([]);
    expect(state.settings.onboardedAt).toBe('2026-01-01T00:00:00.000Z');
    expect(state.settings.userName).toBe('Riya');
  });
});

describe('captureNetWorthSnapshots', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-15T12:00:00.000Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('freezes each completed month once, and is a no-op on the next run', () => {
    seed(
      [account('a', 10000, 3000)],
      [
        tx({
          id: 't1',
          type: 'income',
          amount: 4000,
          accountId: 'a',
          date: '2026-04-10T00:00:00.000Z',
        }),
        tx({
          id: 't2',
          type: 'income',
          amount: 3000,
          accountId: 'a',
          date: '2026-05-10T00:00:00.000Z',
        }),
      ],
    );

    expect(useFinanceStore.getState().captureNetWorthSnapshots()).toBe(2);
    const snapshots = useFinanceStore.getState().netWorthSnapshots;
    expect(snapshots.map((s) => s.periodKey)).toEqual(['2026-04', '2026-05']);
    // April closed on 10000 minus May's 3000.
    expect(snapshots[0].assets).toBe(7000);

    expect(useFinanceStore.getState().captureNetWorthSnapshots()).toBe(0);
  });

  it('keeps a captured month steady when history is edited afterwards', () => {
    seed(
      [account('a', 10000, 6000)],
      [
        tx({
          id: 't1',
          type: 'income',
          amount: 4000,
          accountId: 'a',
          date: '2026-05-10T00:00:00.000Z',
        }),
      ],
    );
    useFinanceStore.getState().captureNetWorthSnapshots();
    const before = useFinanceStore.getState().netWorthSnapshots[0];

    useFinanceStore.getState().deleteTransaction('t1');

    expect(useFinanceStore.getState().netWorthSnapshots[0]).toEqual(before);
  });

  it('is cleared by resetToDefaults, same as every other finance collection', () => {
    seed(
      [account('a', 10000, 6000)],
      [
        tx({
          id: 't1',
          type: 'income',
          amount: 4000,
          accountId: 'a',
          date: '2026-05-10T00:00:00.000Z',
        }),
      ],
    );
    useFinanceStore.getState().captureNetWorthSnapshots();
    expect(useFinanceStore.getState().netWorthSnapshots).toHaveLength(1);

    useFinanceStore.getState().resetToDefaults();
    expect(useFinanceStore.getState().netWorthSnapshots).toEqual([]);
  });
});

describe('importData with net worth snapshots', () => {
  const snapshot = (periodKey: string, assets: number, createdAt: string) => ({
    id: `snap-${periodKey}-${createdAt}`,
    periodKey,
    date: `${periodKey}-28T23:59:59.999Z`,
    assets,
    liabilities: 0,
    createdAt,
  });

  it('keeps one snapshot per month, preferring the later capture', () => {
    useFinanceStore.setState({
      netWorthSnapshots: [snapshot('2026-04', 1000, '2026-05-01T00:00:00.000Z')],
    });

    useFinanceStore
      .getState()
      .importData(
        { netWorthSnapshots: [snapshot('2026-04', 2000, '2026-05-02T00:00:00.000Z')] },
        { mode: 'merge' },
      );

    const merged = useFinanceStore.getState().netWorthSnapshots;
    expect(merged).toHaveLength(1);
    expect(merged[0].assets).toBe(2000);
  });

  it('replaces the collection outright in replace mode', () => {
    useFinanceStore.setState({
      netWorthSnapshots: [snapshot('2026-03', 500, '2026-04-01T00:00:00.000Z')],
    });

    useFinanceStore
      .getState()
      .importData(
        { netWorthSnapshots: [snapshot('2026-04', 900, '2026-05-01T00:00:00.000Z')] },
        { mode: 'replace' },
      );

    expect(useFinanceStore.getState().netWorthSnapshots.map((s) => s.periodKey)).toEqual([
      '2026-04',
    ]);
  });
});

describe('v12 migration', () => {
  it('seeds an empty snapshot list for pre-v12 state', async () => {
    backing.set(
      'finio-storage',
      JSON.stringify({
        version: 11,
        state: {
          accounts: [account('a', 100)],
          transactions: [],
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            monthStartDay: 1,
            hideAmounts: false,
          },
        },
      }),
    );

    await useFinanceStore.persist.rehydrate();

    expect(useFinanceStore.getState().netWorthSnapshots).toEqual([]);
  });
});

describe('v13 migration', () => {
  const preV13 = (settings: Record<string, unknown>) =>
    JSON.stringify({
      version: 12,
      state: {
        accounts: [account('a', 100)],
        transactions: [],
        settings: {
          theme: 'dark',
          userName: 'Alex',
          autoLocalBackup: false,
          monthStartDay: 1,
          hideAmounts: false,
          ...settings,
        },
      },
    });

  it('leaves reminders off, so an upgrade never opts anyone into notifications', async () => {
    backing.set('finio-storage', preV13({}));

    await useFinanceStore.persist.rehydrate();

    const { settings } = useFinanceStore.getState();
    expect(settings.notificationsEnabled).toBe(false);
  });

  it('defaults the per-trigger switches on, so the master switch alone is useful', async () => {
    backing.set('finio-storage', preV13({}));

    await useFinanceStore.persist.rehydrate();

    const { settings } = useFinanceStore.getState();
    expect(settings.notifyBills).toBe(true);
    expect(settings.notifyBudgets).toBe(true);
    expect(settings.notifyCreditDue).toBe(true);
    expect(settings.notifyLeadDays).toBe(2);
    expect(settings.notifyDailyLog).toBe(true);
  });

  it('preserves settings the user already chose', async () => {
    backing.set('finio-storage', preV13({ theme: 'dark', monthStartDay: 25 }));

    await useFinanceStore.persist.rehydrate();

    const { settings } = useFinanceStore.getState();
    expect(settings.theme).toBe('dark');
    expect(settings.userName).toBe('Alex');
    expect(settings.monthStartDay).toBe(25);
  });
});

describe('deposits', () => {
  const day = (d: string) => new Date(`${d}T00:00:00`).toISOString();

  afterEach(() => {
    vi.useRealTimers();
  });

  it('funds an FD from its source account', () => {
    // Pin the clock before the start date: an FD dated in the past is treated as already funded
    // (see the past-dated cases below), so an unpinned clock made this test expire on 1 Oct 2026.
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-09-15')));
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'fd',
      name: 'FD',
      color: '#000',
      terms: {
        amount: 50000,
        interestRate: 6.65,
        startDate: day('2026-10-01'),
        maturityDate: day('2029-09-01'),
        linkedAccountId: 'bank',
      },
    });
    const { accounts, transactions } = useFinanceStore.getState();
    expect(accounts.find((a) => a.id === 'bank')?.balance).toBe(50000);
    const fd = accounts.find((a) => a.id === id);
    expect(fd?.balance).toBe(50000);
    expect(fd?.openingBalance).toBe(0);
    expect(fd?.deposit?.compounding).toBe('quarterly');
    expect(transactions).toHaveLength(1);
    expect(transactions[0]).toMatchObject({ type: 'transfer', accountId: 'bank', toAccountId: id });
  });

  const pastFd = (deductPast: boolean) => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-10-15')));
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'fd',
      name: 'FD',
      color: '#000',
      terms: {
        amount: 50000,
        interestRate: 6.5,
        startDate: day('2026-07-05'),
        maturityDate: day('2029-07-05'),
        linkedAccountId: 'bank',
      },
      deductPast,
    });
    return { id, ...useFinanceStore.getState() };
  };

  it('treats a past-dated FD as an opening balance when not deducting it', () => {
    const { id, accounts, transactions } = pastFd(false);
    const fd = accounts.find((a) => a.id === id);
    expect(fd?.balance).toBe(50000);
    expect(fd?.openingBalance).toBe(50000);
    expect(transactions).toHaveLength(0);
    expect(accounts.find((a) => a.id === 'bank')?.balance).toBe(100000);
  });

  it('posts a past-dated FD funding transfer from the linked account when deducting it', () => {
    const { id, accounts, transactions } = pastFd(true);
    expect(accounts.find((a) => a.id === 'bank')?.balance).toBe(50000);
    expect(accounts.find((a) => a.id === id)?.balance).toBe(50000);
    expect(accounts.find((a) => a.id === id)?.openingBalance).toBe(0);
    expect(transactions).toHaveLength(1);
  });

  it('folds past RD installments into the opening balance when not deducting them', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-10-15')));
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'rd',
      name: 'RD',
      color: '#000',
      terms: {
        amount: 5000,
        interestRate: 7,
        startDate: day('2026-07-05'),
        tenureMonths: 12,
        linkedAccountId: 'bank',
      },
      deductPast: false,
    });
    const state = useFinanceStore.getState();
    const rd = state.accounts.find((a) => a.id === id)!;
    // Jul, Aug, Sep, Oct 5 — four installments already paid.
    expect(rd.openingBalance).toBe(20000);
    const rule = state.recurring.find((r) => r.id === rd.deposit?.recurringId)!;
    expect(rule.occurrenceCount).toBe(4);
    expect(rule.lastRunDate).toBe(day('2026-10-05'));
    // Nothing is backfilled, and the bank is untouched.
    expect(useFinanceStore.getState().processRecurring()).toHaveLength(0);
    expect(useFinanceStore.getState().accounts.find((a) => a.id === 'bank')?.balance).toBe(100000);
  });

  it('posts past RD installments from the linked account when deducting them', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-10-15')));
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'rd',
      name: 'RD',
      color: '#000',
      terms: {
        amount: 5000,
        interestRate: 7,
        startDate: day('2026-07-05'),
        tenureMonths: 12,
        linkedAccountId: 'bank',
      },
      deductPast: true,
    });
    expect(useFinanceStore.getState().processRecurring()).toHaveLength(4);
    const { accounts } = useFinanceStore.getState();
    expect(accounts.find((a) => a.id === 'bank')?.balance).toBe(80000);
    expect(accounts.find((a) => a.id === id)?.balance).toBe(20000);
  });

  it('pays out a matured FD exactly once and archives it', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-10-01')));
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'fd',
      name: 'FD',
      color: '#000',
      terms: {
        amount: 50000,
        interestRate: 6.65,
        startDate: day('2026-10-01'),
        maturityDate: day('2029-09-01'),
        linkedAccountId: 'bank',
      },
    });
    expect(useFinanceStore.getState().processMaturities()).toHaveLength(0);

    vi.setSystemTime(new Date(day('2029-09-02')));
    const posted = useFinanceStore.getState().processMaturities();
    expect(posted.map((t) => t.type)).toEqual(['income', 'transfer']);
    expect(posted[0]).toMatchObject({ amount: 10621.08, categoryId: 'cat-23' });

    const { accounts } = useFinanceStore.getState();
    const fd = accounts.find((a) => a.id === id)!;
    expect(fd.balance).toBe(0);
    expect(fd.archivedAt).toBeDefined();
    expect(fd.deposit?.maturedAt).toBe(day('2029-09-01'));
    expect(accounts.find((a) => a.id === 'bank')?.balance).toBe(110621.08);

    expect(useFinanceStore.getState().processMaturities()).toHaveLength(0);
  });

  it('refuses to delete an account an open deposit pays out to', () => {
    seed([account('bank', 100000)]);
    const id = useFinanceStore.getState().addDeposit({
      type: 'fd',
      name: 'FD',
      color: '#000',
      terms: {
        amount: 50000,
        interestRate: 6.65,
        startDate: day('2026-10-01'),
        maturityDate: day('2029-09-01'),
        linkedAccountId: 'bank',
      },
    });
    expect(useFinanceStore.getState().deleteAccount('bank')).toBe(false);
    expect(useFinanceStore.getState().accounts).toHaveLength(2);

    // Deleting the deposit itself unwinds its funding and frees the bank account.
    expect(useFinanceStore.getState().deleteAccount(id)).toBe(true);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100000);
    expect(useFinanceStore.getState().deleteAccount('bank')).toBe(true);
  });
});

describe('addLoan with a past first EMI date', () => {
  const day = (d: string) => new Date(`${d}T00:00:00`).toISOString();

  afterEach(() => {
    vi.useRealTimers();
  });

  const pastLoan = (logPastEmis?: boolean) => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(day('2026-10-15')));
    seed([account('a', 100_000, 100_000)]);
    const loanId = useFinanceStore.getState().addLoan(
      {
        name: 'Bike',
        principal: 36_000,
        interestRate: 0,
        tenureMonths: 12,
        startDate: day('2026-07-05'), // EMIs fell due 5 Jul, 5 Aug, 5 Sep, 5 Oct
        accountId: 'a',
        categoryId: 'cat-1',
      },
      logPastEmis === undefined ? undefined : { logPastEmis },
    );
    const state = useFinanceStore.getState();
    const loan = state.loans.find((l) => l.id === loanId)!;
    const rule = state.recurring.find((r) => r.id === loan.recurringId)!;
    return { rule, loanId };
  };

  it('treats past EMIs as already paid by default: nothing is ever back-posted', () => {
    const { rule } = pastLoan();
    expect(rule.occurrenceCount).toBe(4);
    expect(rule.lastRunDate).not.toBeNull();

    const posted = useFinanceStore.getState().processRecurring();
    expect(posted).toHaveLength(0);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(100_000);
  });

  it('posts the past EMIs as real expenses when logPastEmis is on', () => {
    const { rule } = pastLoan(true);
    expect(rule.occurrenceCount).toBe(0);
    expect(rule.lastRunDate).toBeNull();

    const posted = useFinanceStore.getState().processRecurring();
    expect(posted).toHaveLength(4);
    expect(posted.every((t) => t.type === 'expense' && t.amount === 3000)).toBe(true);
    expect(useFinanceStore.getState().accounts[0].balance).toBe(88_000);
  });
});

describe('addLoanPrepayment', () => {
  const setup = () => {
    seed([account('a', 1_000_000, 1_000_000)]);
    const loanId = useFinanceStore.getState().addLoan({
      name: 'Car',
      principal: 500_000,
      interestRate: 0,
      tenureMonths: 36,
      // First EMI is next month, so nothing is paid yet and the full principal is owed.
      startDate: new Date(Date.now() + 40 * 86_400_000).toISOString(),
      accountId: 'a',
      categoryId: 'cat-1',
    });
    return loanId;
  };
  const today = () => new Date().toISOString();

  it('caps an oversized prepayment at what is owed and closes the loan', () => {
    const loanId = setup();
    const id = useFinanceStore
      .getState()
      .addLoanPrepayment({ loanId, amount: 9_999_999, date: today(), note: '' });
    expect(id).not.toBeNull();
    const state = useFinanceStore.getState();
    expect(state.loanPrepayments[0].amount).toBe(500_000);
    expect(state.accounts[0].balance).toBe(500_000);
    expect(state.loans[0].closedAt).toBeDefined();
  });

  it('records a partial prepayment as-is and leaves the loan open', () => {
    const loanId = setup();
    useFinanceStore
      .getState()
      .addLoanPrepayment({ loanId, amount: 100_000, date: today(), note: '' });
    expect(useFinanceStore.getState().loanPrepayments[0].amount).toBe(100_000);
    expect(useFinanceStore.getState().loans[0].closedAt).toBeUndefined();
  });

  it('refuses once nothing is left to prepay', () => {
    const loanId = setup();
    const { addLoanPrepayment } = useFinanceStore.getState();
    addLoanPrepayment({ loanId, amount: 500_000, date: today(), note: '' });
    expect(addLoanPrepayment({ loanId, amount: 1, date: today(), note: '' })).toBeNull();
  });
});
