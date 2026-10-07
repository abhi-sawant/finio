import { vi } from 'vitest';
import type { DebtEntry, FinanceStore, ImportPayload, Transaction } from '@/types';
import { at, gc, type GoldenCase } from '../golden';

/**
 * A long, deterministic script of actions run against the real Zustand finance store, recording
 * every action's return value and the state keys it changed. The Android
 * `FinanceStoreGoldenTest` replays the same script (args are recorded literally, so returned ids
 * and undo rows flow through) and asserts identical states.
 *
 * Determinism: `Date` is faked and moved per step (each case's first arg is that step's `now`),
 * and `crypto.randomUUID` is a counter — so ids are minted in the store's own call order, which
 * the Kotlin port has to match exactly.
 *
 * "settleUp" is the Debts page's `handleSettleSubmit` (addTransaction + addDebtEntry), which the
 * Kotlin store exposes as one atomic action. The `migrate` cases run the persist middleware's
 * own `migrate` over old blobs and shallow-merge the result over the initial state, exactly as
 * rehydration does.
 */
const DATA_KEYS = [
  'accounts',
  'transactions',
  'categories',
  'labels',
  'budgets',
  'recurring',
  'templates',
  'rules',
  'goals',
  'goalContributions',
  'people',
  'debtEntries',
  'netWorthSnapshots',
  'loans',
  'loanPrepayments',
  'settings',
  'lastLocalBackupAt',
] as const;

const ID_PREFIX = 'f5000000-0000-4000-8000-';
const iso = (y: number, m: number, d: number, h = 0, min = 0) => at(y, m, d, h, min).toISOString();

function dataOf(state: object): Record<string, unknown> {
  const s = state as Record<string, unknown>;
  return Object.fromEntries(DATA_KEYS.map((k) => [k, s[k]]));
}

export default async function cases(): Promise<GoldenCase[]> {
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
  const { defaultCategories, defaultLabels, defaultSettings, MISC_CATEGORY_ID } =
    await import('@/data/defaultData');
  const { cleanText, MAX_NOTE_LENGTH } = await import('@/utils/validation');

  let counter = 0;
  const uuid = vi
    .spyOn(crypto, 'randomUUID')
    .mockImplementation(
      () =>
        `${ID_PREFIX}${String(++counter).padStart(12, '0')}` as `${string}-${string}-${string}-${string}-${string}`,
    );
  vi.useFakeTimers({ toFake: ['Date'] });

  const out: GoldenCase[] = [];
  const get = () => useFinanceStore.getState();

  /** The Debts page's settle-up flow, with the page's own validation. */
  function settleUp(personId: string, amount: number, accountId: string, note: string) {
    const s = get();
    const person = s.people.find((p) => p.id === personId);
    if (!person) return null;
    const balance = s.debtEntries
      .filter((e) => e.personId === personId)
      .reduce((sum, e) => sum + e.amount, 0);
    if (!amount || amount <= 0) return null;
    if (amount > Math.abs(balance) + 0.005) return null;
    if (!accountId) return null;
    const type = balance > 0 ? 'income' : 'expense';
    const cleaned = cleanText(note, MAX_NOTE_LENGTH) || `Settled up with ${person.name}`;
    const date = new Date().toISOString();
    const transactionId = s.addTransaction({
      type,
      amount,
      accountId,
      categoryId: MISC_CATEGORY_ID,
      date,
      note: cleaned,
      labels: [],
    });
    const entryId = get().addDebtEntry({
      personId,
      amount: balance > 0 ? -amount : amount,
      date,
      note: cleaned,
      settledTransactionId: transactionId,
    });
    return { transactionId, entryId };
  }

  // Each case records only the state keys that changed since the previous step (the first case
  // records all of them); the Kotlin replay asserts every other key stayed put too.
  let previous: Record<string, string> | null = null;
  function changedKeys(): Record<string, unknown> {
    const data = dataOf(get());
    const changed: Record<string, unknown> = {};
    const next: Record<string, string> = {};
    for (const key of DATA_KEYS) {
      next[key] = JSON.stringify(data[key] ?? null);
      if (!previous || previous[key] !== next[key]) changed[key] = JSON.parse(next[key]);
    }
    previous = next;
    return changed;
  }

  /** Run one step at `now`, record it, and hand back the return value. */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function run(fn: string, now: string, ...args: any[]): any {
    vi.setSystemTime(new Date(now));
    // Snapshot the args before the call — the store never mutates them, but be explicit.
    const recorded = JSON.parse(JSON.stringify(args)) as unknown[];
    let ret: unknown;
    if (fn === 'setState') {
      useFinanceStore.setState(args[0]);
    } else if (fn === 'settleUp') {
      ret = settleUp(args[0], args[1], args[2], args[3]);
    } else {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ret = (get() as any)[fn](...args);
    }
    out.push(
      gc(
        fn,
        [now, ...recorded],
        { ret: ret === undefined ? null : JSON.parse(JSON.stringify(ret)), changed: changedKeys() },
        String(out.length),
      ),
    );
    return ret;
  }

  try {
    const T0 = iso(2026, 3, 10, 9);
    vi.setSystemTime(new Date(T0));
    get().resetToDefaults();
    run('setState', T0, {
      settings: defaultSettings,
      lastLocalBackupAt: null,
      categories: defaultCategories,
      labels: defaultLabels,
    });

    // ---- Accounts & transactions -----------------------------------------------------------
    const bank = run('addAccount', T0, {
      name: '  HDFC Savings  ',
      type: 'savings',
      color: '#4b36c7',
      icon: 'landmark',
      balance: 50000,
    });
    const card = run('addAccount', T0, {
      name: 'Card',
      type: 'credit',
      color: '#c026d3',
      icon: 'credit-card',
      balance: 0,
      creditLimit: 100000,
      statementCloseDay: 20,
      paymentDueDays: 20,
    });
    const cash = run('addAccount', T0, {
      name: 'Cash',
      type: 'cash',
      color: '#22c55e',
      icon: 'wallet',
      balance: 2000.5,
    });
    const groceries = run('addTransaction', T0, {
      type: 'expense',
      amount: 1200.35,
      accountId: bank,
      categoryId: 'cat-25',
      date: iso(2026, 3, 5, 10),
      note: '  Groceries at DMart ',
      labels: ['lbl-1'],
    });
    const xfer = run('addTransaction', T0, {
      type: 'transfer',
      amount: 500,
      accountId: bank,
      toAccountId: cash,
      categoryId: 'cat-13',
      date: iso(2026, 3, 6, 11),
      note: 'ATM',
      labels: [],
    });
    const split = run('addTransaction', T0, {
      type: 'expense',
      amount: 1500,
      accountId: card,
      categoryId: '',
      date: iso(2026, 3, 7, 20),
      note: 'Mall',
      labels: [],
      splits: [
        { categoryId: 'cat-1', amount: 1000 },
        { categoryId: 'cat-3', amount: 500 },
      ],
    });
    const salary = run('addTransaction', T0, {
      type: 'income',
      amount: 80000,
      accountId: bank,
      categoryId: 'cat-9',
      date: iso(2026, 3, 1, 9),
      note: 'Salary March',
      labels: [],
    });
    run('updateAccount', T0, bank, { balance: 130000 });
    run('updateAccount', T0, cash, { name: 'Wallet cash' });
    run('recomputeBalances', T0);
    run('setState', T0, {
      accounts: get().accounts.map((a) => (a.id === cash ? { ...a, balance: 1 } : a)),
    });
    run('recomputeBalances', T0);
    run('updateTransaction', T0, groceries, { amount: 1300, date: iso(2026, 3, 4, 10) });
    run('updateTransaction', T0, xfer, { note: 'ATM withdrawal' });

    const T1 = iso(2026, 3, 12, 18);
    run('bulkAddTransactions', T1, [
      {
        type: 'expense',
        amount: 240,
        accountId: card,
        categoryId: 'cat-24',
        date: iso(2026, 3, 11, 8),
        note: 'UBER trip',
        labels: [],
      },
      {
        type: 'expense',
        amount: 449,
        accountId: card,
        categoryId: 'cat-24',
        date: iso(2026, 3, 11, 21),
        note: 'Swiggy 449',
        labels: ['lbl-2'],
      },
      {
        type: 'income',
        amount: 120.5,
        accountId: bank,
        categoryId: 'cat-23',
        date: iso(2026, 3, 12, 0),
        note: 'Interest credit',
        labels: [],
      },
    ]);
    const [uberTx, swiggyTx, interestTx] = get().transactions.map((t) => t.id);
    run('bulkRecategorize', T1, [uberTx, split, xfer, interestTx], 'cat-2');
    run('bulkRecategorize', T1, [interestTx, xfer], 'cat-24');
    run('bulkRecategorize', T1, [uberTx], 'nope');
    run('bulkAddLabel', T1, [uberTx, swiggyTx, salary], 'lbl-2');

    // ---- Rules -----------------------------------------------------------------------------
    run('addRule', T1, {
      pattern: 'uber',
      matchType: 'contains',
      scope: 'any',
      categoryId: 'cat-19',
      labelIds: ['lbl-3'],
      enabled: true,
    });
    const swiggyRule = run('addRule', T1, {
      pattern: 'swiggy\\s\\d+',
      matchType: 'regex',
      scope: 'expense',
      categoryId: 'cat-1',
      labelIds: ['lbl-1', 'lbl-2'],
      enabled: true,
    });
    run('moveRule', T1, swiggyRule, 'up');
    run('moveRule', T1, swiggyRule, 'up');
    const applied = run('applyRulesToExisting', T1);
    run('restoreCategorization', T1, applied.previous);
    run('applyRulesToExisting', T1, { restrictToCategoryId: 'cat-24' });
    run('updateRule', T1, swiggyRule, { enabled: false });

    // ---- Templates, categories, labels, budgets ------------------------------------------------
    const tmpl = run('addTemplate', T1, {
      name: 'Coffee',
      type: 'expense',
      amount: 150,
      accountId: cash,
      categoryId: 'cat-1',
      note: 'Morning coffee',
      labels: [],
    });
    run('addTemplate', T1, {
      name: 'Rent split',
      type: 'expense',
      amount: 30000,
      accountId: bank,
      categoryId: '',
      note: 'Rent',
      labels: ['lbl-5'],
      splits: [
        { categoryId: 'cat-28', amount: 25000 },
        { categoryId: 'cat-5', amount: 5000 },
      ],
    });
    run('deleteTemplate', T1, tmpl);
    run('addCategory', T1, { name: 'Side gigs', icon: 'laptop', color: '#146b54', type: 'income' });
    run('updateCategory', T1, get().categories.at(-1)!.id, { name: 'Side gig income' });
    run('addLabel', T1, { name: 'Trip', color: '#0ea5e9' });
    const trip = get().labels.at(-1)!.id;
    run('updateLabel', T1, trip, { color: '#f59e0b' });
    run('bulkAddLabel', T1, [groceries], trip);
    run('addBudget', T1, { categoryId: '', amount: 30000, period: 'monthly', rollover: false });
    run('addBudget', T1, { categoryId: 'cat-1', amount: 20000, period: 'monthly', rollover: true });
    run('addBudget', T1, { categoryId: 'cat-1', amount: 25000, period: 'weekly', rollover: false });
    run('addBudget', T1, {
      categoryId: '',
      labelId: trip,
      amount: 5000,
      period: 'yearly',
      rollover: false,
    });
    run('updateBudget', T1, get().budgets[0].id, { amount: 32000 });
    run('addRule', T1, {
      pattern: 'dmart',
      matchType: 'startsWith',
      scope: 'any',
      categoryId: 'cat-25',
      labelIds: [trip, 'lbl-1'],
      enabled: true,
    });
    run('deleteLabel', T1, trip);
    run('addTransaction', T1, {
      type: 'expense',
      amount: 500,
      accountId: cash,
      categoryId: '',
      date: iso(2026, 3, 12, 13),
      note: 'Lunch + misc',
      labels: [],
      splits: [
        { categoryId: 'cat-1', amount: 300.25 },
        { categoryId: 'cat-24', amount: 199.75 },
      ],
    });
    run('addTransaction', T1, {
      type: 'expense',
      amount: 900,
      accountId: cash,
      categoryId: '',
      date: iso(2026, 3, 12, 14),
      note: 'Three-way',
      labels: [],
      splits: [
        { categoryId: 'cat-1', amount: 300 },
        { categoryId: 'cat-4', amount: 300 },
        { categoryId: 'cat-24', amount: 300 },
      ],
    });
    run('deleteCategory', T1, 'cat-1');
    run('deleteCategory', T1, 'cat-13');
    run('deleteCategory', T1, 'cat-24');
    run('deleteBudget', T1, get().budgets[0].id);

    // ---- Goals & recurring -------------------------------------------------------------------
    const T2 = iso(2026, 3, 20, 10);
    const goal = run('addGoal', T2, {
      name: '  Emergency fund that has a rather long name to cap  ',
      icon: 'target',
      color: '#146b54',
      targetAmount: 100000,
      targetDate: iso(2027, 3, 1),
      linkedAccountId: cash,
    });
    const c1 = run('addContribution', T2, { goalId: goal, amount: 5000, date: T2, note: 'Start' });
    run('addContribution', T2, { goalId: goal, amount: -9000, date: T2, note: 'Too much' });
    const removedContribution = run('deleteContribution', T2, c1);
    run('deleteContribution', T2, 'missing');
    run('restoreContribution', T2, removedContribution);
    run('restoreContribution', T2, removedContribution);
    run('updateGoal', T2, goal, { targetAmount: 150000 });
    const sip = run('addRecurring', T2, {
      type: 'expense',
      amount: 2500,
      accountId: bank,
      categoryId: 'cat-11',
      note: 'SIP',
      labels: ['lbl-6'],
      frequency: 'monthly',
      startDate: iso(2026, 1, 15, 9),
      maxOccurrences: 5,
      goalId: goal,
    });
    run('addRecurring', T2, {
      type: 'transfer',
      amount: 1000,
      accountId: bank,
      toAccountId: cash,
      categoryId: 'cat-13',
      note: 'Weekly cash',
      labels: [],
      frequency: 'weekly',
      startDate: iso(2026, 2, 1, 8),
      endDate: iso(2026, 2, 28),
    });
    run('addRecurring', T2, {
      type: 'expense',
      amount: 199,
      accountId: card,
      categoryId: 'cat-18',
      note: 'Streaming',
      labels: [],
      frequency: 'monthly',
      startDate: iso(2025, 6, 2, 9),
      lastRunDate: iso(2026, 3, 2, 9),
    });
    run('addRecurring', T2, {
      type: 'transfer',
      amount: 50,
      accountId: bank,
      toAccountId: 'missing-account',
      categoryId: 'cat-13',
      note: 'Orphan',
      labels: [],
      frequency: 'daily',
      startDate: iso(2026, 3, 1),
    });
    run('processRecurring', T2);
    run('setRecurringPaused', T2, sip, true);
    run('processRecurring', iso(2026, 4, 20, 10));
    run('setRecurringPaused', iso(2026, 4, 20, 10), sip, false);
    run('updateRecurring', iso(2026, 4, 20, 10), sip, { amount: 3000 });
    run('processRecurring', iso(2026, 4, 20, 10));
    run('deleteGoal', iso(2026, 4, 20, 10), goal);
    const goal2 = run('addGoal', iso(2026, 4, 20, 10), {
      name: 'Bike',
      icon: 'bike',
      color: '#f59e0b',
      targetAmount: 90000,
      linkedAccountId: card,
    });

    // ---- People, debts, settle up ---------------------------------------------------------
    const T3 = iso(2026, 4, 21, 12);
    const rahul = run('addPerson', T3, { name: 'Rahul', icon: 'user', color: '#146b54' });
    run('updatePerson', T3, rahul, { name: 'Rahul S' });
    run('addDebtEntry', T3, { personId: rahul, amount: 1500, date: T3, note: 'Lent for tickets' });
    const borrow = run('addDebtEntry', T3, {
      personId: rahul,
      amount: -200,
      date: T3,
      note: 'Borrowed',
    });
    run('settleUp', T3, rahul, 5000, bank, '');
    run('settleUp', T3, rahul, 0, bank, '');
    run('settleUp', T3, rahul, 1300, '', '');
    const settled = run('settleUp', T3, rahul, 1300, bank, '');
    run('updateDebtEntry', T3, settled.entryId, { amount: 1000, note: '  cash back  ' });
    run('updateTransaction', T3, settled.transactionId, { type: 'expense' });
    run('updateTransaction', T3, settled.transactionId, { type: 'income', amount: 1100 });
    const removedSettleTx: Transaction = run('deleteTransaction', T3, settled.transactionId);
    run('restoreTransaction', T3, removedSettleTx);
    run('restoreTransaction', T3, removedSettleTx);
    const removedSettleEntry: DebtEntry = run('deleteDebtEntry', T3, settled.entryId);
    run('restoreDebtEntry', T3, removedSettleEntry);
    run('restoreDebtEntry', T3, removedSettleEntry);
    run('updateDebtEntry', T3, 'missing', { amount: 1 });
    run('updateDebtEntry', T3, borrow, { amount: 0, date: '' });
    run('updateDebtEntry', T3, borrow, { amount: 350.555, date: iso(2026, 4, 1) });
    const removedPlain = run('deleteDebtEntry', T3, borrow);
    run('restoreDebtEntry', T3, removedPlain);
    const bulk = run('bulkDeleteTransactions', T3, [settled.transactionId, uberTx, 'missing']);
    run('restoreTransactions', T3, bulk);
    run('restoreTransactions', T3, bulk);
    run('bulkDeleteTransactions', T3, ['missing']);
    const priya = run('addPerson', T3, { name: 'Priya', icon: 'user', color: '#f59e0b' });
    run('addDebtEntry', T3, { personId: priya, amount: -700, date: T3, note: 'Dinner' });
    run('settleUp', T3, priya, 700, cash, 'Paid back');
    run('deletePerson', T3, priya);

    // ---- Deposits ----------------------------------------------------------------------------
    const T4 = iso(2026, 4, 22, 9);
    const fdFuture = run('addDeposit', T4, {
      type: 'fd',
      name: 'FD future',
      color: '#eab308',
      terms: {
        amount: 50000,
        interestRate: 6.65,
        startDate: iso(2026, 5, 1),
        maturityDate: iso(2027, 5, 1),
        linkedAccountId: bank,
      },
    });
    run('addDeposit', T4, {
      type: 'fd',
      name: 'FD held',
      color: '#eab308',
      terms: {
        amount: 20000,
        interestRate: 7,
        startDate: iso(2025, 12, 1),
        maturityDate: iso(2026, 12, 1),
        compounding: 'yearly',
        linkedAccountId: bank,
      },
      deductPast: false,
    });
    run('addDeposit', T4, {
      type: 'fd',
      name: 'FD deducted',
      color: '#eab308',
      terms: {
        amount: 10000,
        interestRate: 6,
        startDate: iso(2025, 12, 1),
        maturityDate: iso(2026, 9, 1),
        compounding: 'simple',
        linkedAccountId: bank,
      },
      deductPast: true,
    });
    const rdFolded = run('addDeposit', T4, {
      type: 'rd',
      name: 'RD folded',
      color: '#eab308',
      terms: {
        amount: 5000,
        interestRate: 7,
        startDate: iso(2025, 12, 5),
        tenureMonths: 6,
        linkedAccountId: bank,
      },
      deductPast: false,
    });
    run('addDeposit', T4, {
      type: 'rd',
      name: 'RD deducted',
      color: '#eab308',
      terms: {
        amount: 2000,
        interestRate: 6.5,
        startDate: iso(2026, 1, 10),
        tenureMonths: 12,
        linkedAccountId: cash,
      },
      deductPast: true,
    });
    run('processRecurring', T4);
    run('updateDeposit', T4, fdFuture, {
      name: 'FD renamed',
      compounding: 'monthly',
      interestRate: 7.1,
    });
    run('updateDeposit', T4, rdFolded, {
      name: 'RD renamed',
      compounding: 'monthly',
      maturityDate: T4,
    });
    run('updateDeposit', T4, bank, { name: 'Not a deposit' });
    run('deleteAccount', T4, bank);
    run('processMaturities', T4);

    const T5 = iso(2026, 9, 2, 10);
    run('processMaturities', T5);
    run('processRecurring', T5);
    run('processMaturities', T5);

    // ---- Loans -------------------------------------------------------------------------------
    const bikeLoan = run('addLoan', T5, {
      name: 'Bike',
      principal: 36000,
      interestRate: 0,
      tenureMonths: 12,
      startDate: iso(2026, 6, 5),
      accountId: bank,
      categoryId: 'cat-27',
    });
    const carLoan = run(
      'addLoan',
      T5,
      {
        name: 'Car',
        principal: 500000,
        interestRate: 9.5,
        tenureMonths: 60,
        startDate: iso(2026, 7, 10),
        accountId: bank,
        categoryId: 'cat-27',
      },
      { logPastEmis: true },
    );
    run('processRecurring', T5);
    run('updateLoan', T5, carLoan, { principal: 450000, name: 'Car loan' });
    run('addLoanPrepayment', T5, { loanId: carLoan, amount: 25000, date: T5, note: '  ' });
    run('addLoanPrepayment', T5, { loanId: bikeLoan, amount: 99999999, date: T5, note: 'Payoff' });
    run('addLoanPrepayment', T5, { loanId: bikeLoan, amount: 1, date: T5, note: '' });
    run('addLoanPrepayment', T5, { loanId: 'missing', amount: 1, date: T5, note: '' });
    const carPrepayment = get().loanPrepayments.find((p) => p.loanId === carLoan)!.id;
    const removedPrepayment = run('deleteLoanPrepayment', T5, carPrepayment);
    run('restoreLoanPrepayment', T5, removedPrepayment);
    run('restoreLoanPrepayment', T5, removedPrepayment);
    run('setLoanClosed', T5, bikeLoan, false);
    run('setLoanClosed', T5, carLoan, true);
    run('deleteLoan', T5, bikeLoan);

    // ---- Net worth, settings, archive, delete ----------------------------------------------
    run('captureNetWorthSnapshots', T5);
    run('captureNetWorthSnapshots', T5);
    run('updateSettings', T5, { monthStartDay: 25, userName: 'Asha' });
    run('captureNetWorthSnapshots', T5);
    run('setAccountArchived', T5, cash, true);
    run('setAccountArchived', T5, cash, false);
    run('setAccountArchived', T5, card, true);
    run('setLastLocalBackupAt', T5, '2026-09-02');
    run('updateAccount', T5, card, { balance: -5000, openingBalance: -100 });
    run('deleteAccount', T5, cash);
    run('deleteAccount', T5, card);
    run('deleteAccount', T5, 'missing');
    run('updateGoal', T5, goal2, { name: 'Bike fund' });

    // ---- Import --------------------------------------------------------------------------------
    const T6 = iso(2026, 9, 3, 10);
    const existingSnapshot = get().netWorthSnapshots[0];
    const mergePayload: ImportPayload = {
      accounts: [
        {
          id: 'imp-acct',
          name: 'Imported',
          type: 'checking',
          color: '#000000',
          icon: 'landmark',
          balance: 750,
          createdAt: '2026-01-01T00:00:00.000Z',
        },
        { ...get().accounts.find((a) => a.id === bank)!, balance: 123456, name: 'Bank (backup)' },
      ],
      transactions: [
        {
          id: 'imp-t1',
          type: 'expense',
          amount: 250,
          accountId: 'imp-acct',
          categoryId: 'cat-2',
          date: iso(2026, 8, 1),
          note: 'Imported row',
          labels: [],
          createdAt: iso(2026, 8, 1),
        },
      ],
      netWorthSnapshots: [
        { ...existingSnapshot, id: 'imp-snap', assets: 1, createdAt: '2099-01-01T00:00:00.000Z' },
        {
          id: 'imp-old',
          periodKey: '2020-01',
          date: '2020-01-31T18:29:59.999Z',
          assets: 5,
          liabilities: 1,
          createdAt: '2020-02-01T00:00:00.000Z',
        },
      ],
      people: [
        { id: 'imp-person', name: 'Imported person', icon: 'user', color: '#000', createdAt: T6 },
      ],
      settings: { ...defaultSettings, theme: 'dark', userName: 'From backup', monthStartDay: 5 },
    };
    run('importData', T6, mergePayload, { mode: 'merge' });
    run(
      'importData',
      T6,
      {
        accounts: [
          {
            id: 'r-acct',
            name: 'Replaced',
            type: 'savings',
            color: '#000000',
            icon: 'landmark',
            balance: 0,
            openingBalance: 1000,
            createdAt: '2026-01-01T00:00:00.000Z',
          },
        ],
        transactions: [
          {
            id: 'r-t1',
            type: 'income',
            amount: 99.99,
            accountId: 'r-acct',
            categoryId: 'cat-9',
            date: iso(2026, 8, 2),
            note: '',
            labels: [],
            createdAt: iso(2026, 8, 2),
          },
        ],
        goals: [
          {
            id: 'r-goal',
            name: 'Replaced goal',
            icon: 'target',
            color: '#000',
            targetAmount: 10,
            createdAt: T6,
          },
        ],
      } satisfies ImportPayload,
      { mode: 'replace' },
    );
    run('resetToDefaults', T6);
  } finally {
    vi.useRealTimers();
    uuid.mockRestore();
    useFinanceStore.getState().resetToDefaults();
    useFinanceStore.setState({ settings: defaultSettings, lastLocalBackupAt: null });
  }

  out.push(...(await migrationCases()));
  return out;
}

/** The persist middleware's own `migrate`, over old blobs, merged over the initial state. */
async function migrationCases(): Promise<GoldenCase[]> {
  const { useFinanceStore } = await import('@/store/useFinanceStore');
  const { defaultCategories, defaultLabels, defaultSettings } = await import('@/data/defaultData');
  const migrate = useFinanceStore.persist.getOptions().migrate!;
  const initial = {
    accounts: [],
    transactions: [],
    categories: defaultCategories,
    labels: defaultLabels,
    budgets: [],
    recurring: [],
    templates: [],
    rules: [],
    goals: [],
    goalContributions: [],
    people: [],
    debtEntries: [],
    netWorthSnapshots: [],
    loans: [],
    loanPrepayments: [],
    settings: defaultSettings,
    lastLocalBackupAt: null,
  } satisfies Partial<FinanceStore>;

  const NOW = '2026-06-15T12:00:00.000Z';
  const account = (id: string, balance: number, extra: object = {}) => ({
    id,
    name: id,
    type: 'checking',
    color: '#000',
    icon: 'landmark',
    balance,
    createdAt: '2026-01-01T00:00:00.000Z',
    ...extra,
  });
  const tx = (id: string, type: string, amount: number, accountId: string, extra: object = {}) => ({
    id,
    type,
    amount,
    accountId,
    categoryId: 'cat-1',
    date: '2026-06-01T00:00:00.000Z',
    note: '',
    labels: [],
    createdAt: '2026-06-01T00:00:00.000Z',
    ...extra,
  });
  // Categories as a v1 install had them: the original set, one default deleted (cat-5).
  const v1Categories = defaultCategories.filter(
    (c) =>
      ![
        'cat-25',
        'cat-26',
        'cat-27',
        'cat-28',
        'cat-29',
        'cat-30',
        'cat-31',
        'cat-32',
        'cat-33',
        'cat-34',
        'cat-35',
        'cat-36',
        'cat-5',
      ].includes(c.id),
  );

  const blobs: Array<[string, { state: Record<string, unknown>; version: number }]> = [
    [
      'v1',
      {
        version: 1,
        state: {
          accounts: [account('a', 750, { currency: 'INR' }), account('b', 1200)],
          transactions: [
            tx('t1', 'expense', 250, 'a'),
            tx('t2', 'transfer', 200, 'a', { toAccountId: 'b' }),
          ],
          categories: v1Categories,
          settings: { theme: 'dark', currency: 'USD', userName: 'Old' },
        },
      },
    ],
    [
      'v4',
      {
        version: 4,
        state: {
          accounts: [account('a', 750, { openingBalance: 900 }), account('b', 40)],
          transactions: [tx('t1', 'expense', 250, 'a')],
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
          categories: defaultCategories.slice(0, 23),
          settings: { theme: 'light', userName: 'Four', autoLocalBackup: true, monthStartDay: 40 },
          lastLocalBackupAt: '2026-01-02',
        },
      },
    ],
    [
      'v6',
      {
        version: 6,
        state: {
          accounts: [account('a', 100, { openingBalance: 100 })],
          transactions: [],
          budgets: [
            { id: 'b1', categoryId: '', amount: 500, period: 'weekly', createdAt: '2026-01-01' },
          ],
          // Every collection a later step maps over must be present: the TS steps write
          // `key: s.key` even when it is absent, and that explicit undefined then clobbers the
          // initial state on merge (the Kotlin port keeps the default instead).
          recurring: [],
          categories: defaultCategories.slice(0, 23),
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            onboardedAt: '2025-01-01T00:00:00.000Z',
          },
        },
      },
    ],
    [
      'v12',
      {
        version: 12,
        state: {
          accounts: [account('a', 100, { openingBalance: 100 })],
          transactions: [],
          categories: defaultCategories.filter((c) => c.id !== 'cat-30' && c.id !== 'cat-35'),
          settings: {
            theme: 'dark',
            userName: 'Alex',
            autoLocalBackup: false,
            monthStartDay: 25,
            hideAmounts: true,
            onboardedAt: '2025-01-01T00:00:00.000Z',
            notifyBills: false,
          },
        },
      },
    ],
    [
      'v15',
      {
        version: 15,
        state: {
          accounts: [],
          transactions: [],
          categories: defaultCategories.slice(0, 34),
          loans: [],
          settings: { ...defaultSettings, onboardedAt: '2025-01-01T00:00:00.000Z' },
        },
      },
    ],
    [
      'current',
      {
        version: 16,
        state: {
          accounts: [account('a', 100, { openingBalance: 100 })],
          settings: { ...defaultSettings, userName: 'Now' },
        },
      },
    ],
  ];

  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(NOW));
  try {
    return blobs.map(([name, envelope]) => {
      const raw = JSON.parse(JSON.stringify(envelope)) as typeof envelope;
      const migrated =
        raw.version === 16
          ? raw.state
          : (migrate(raw.state, raw.version) as Record<string, unknown>);
      const merged = { ...initial, ...migrated } as Record<string, unknown>;
      const data = Object.fromEntries(Object.keys(initial).map((k) => [k, merged[k]]));
      return gc(
        'decodePersisted',
        [JSON.stringify(envelope), NOW],
        JSON.parse(JSON.stringify(data)),
        name,
      );
    });
  } finally {
    vi.useRealTimers();
  }
}
