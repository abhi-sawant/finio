import { vi } from 'vitest';
import {
  ENTITY_LABELS,
  hasImportableData,
  IMPORT_ENTITIES,
  validateBackup,
  type ImportReport,
} from '@/utils/importValidation';
import { gc, type GoldenCase } from '../golden';
import sampleBackup from './sampleBackup.fixture';

// Rows without a `createdAt` are stamped with `new Date()`, so validation runs under a faked
// clock pinned here — the Kotlin port takes it as its `now` argument.
const NOW = new Date('2026-10-05T09:00:00.000Z');
const T0 = '2026-01-01T00:00:00.000Z';
const T1 = '2026-06-10T08:30:00.000Z';

type Row = Record<string, unknown>;
const without = (row: Row, ...keys: string[]): Row => {
  const copy = { ...row };
  for (const k of keys) delete copy[k];
  return copy;
};

const account: Row = {
  id: 'a1',
  name: 'Bank',
  type: 'checking',
  color: '#111111',
  icon: 'landmark',
  balance: 1200.5,
  openingBalance: 1000,
  createdAt: T0,
};
const account2: Row = {
  ...account,
  id: 'a2',
  name: 'Wallet',
  type: 'wallet',
  balance: 0,
  openingBalance: 0,
};
const fdDeposit: Row = {
  amount: 50000,
  interestRate: 7.1,
  startDate: T0,
  maturityDate: '2027-01-01T00:00:00.000Z',
  compounding: 'monthly',
  linkedAccountId: 'a1',
  recurringId: 'r-x',
  maturedAt: T1,
};
const rdDeposit: Row = {
  amount: 2000,
  interestRate: 6.5,
  startDate: T0,
  tenureMonths: 12,
  linkedAccountId: 'a1',
  recurringId: 'r-rd',
};
const fd: Row = {
  ...account,
  id: 'fd1',
  name: 'FD',
  type: 'fd',
  balance: 50000,
  openingBalance: 0,
  deposit: fdDeposit,
};
const rd: Row = {
  ...account,
  id: 'rd1',
  name: 'RD',
  type: 'rd',
  balance: 4000,
  openingBalance: 0,
  deposit: rdDeposit,
};

const tx: Row = {
  id: 't1',
  type: 'expense',
  amount: 250.75,
  accountId: 'a1',
  categoryId: 'cat-1',
  date: T1,
  note: 'Lunch',
  labels: ['lbl-1'],
  createdAt: T1,
};
const category: Row = {
  id: 'c1',
  name: 'Food',
  icon: 'utensils',
  color: '#ff0000',
  type: 'expense',
};
const label: Row = { id: 'l1', name: 'Essential', color: '#00ff00' };
const budget: Row = {
  id: 'b1',
  categoryId: 'c1',
  amount: 5000,
  period: 'weekly',
  rollover: true,
  createdAt: T0,
};
const recurring: Row = {
  id: 'r1',
  type: 'expense',
  amount: 499,
  accountId: 'a1',
  categoryId: 'c1',
  note: 'Netflix',
  labels: ['l1'],
  frequency: 'monthly',
  startDate: T0,
  occurrenceCount: 3,
  lastRunDate: T1,
  createdAt: T0,
};
const template: Row = {
  id: 'tp1',
  name: 'Coffee',
  type: 'expense',
  amount: 120,
  accountId: 'a1',
  categoryId: 'c1',
  note: 'Coffee',
  labels: [],
  createdAt: T0,
};
const rule: Row = {
  id: 'ru1',
  pattern: 'swiggy',
  matchType: 'contains',
  scope: 'expense',
  categoryId: 'c1',
  labelIds: ['l1'],
  enabled: true,
  createdAt: T0,
};
const goal: Row = {
  id: 'g1',
  name: 'Trip',
  icon: 'plane',
  color: '#123456',
  targetAmount: 60000,
  targetDate: '2027-03-01T00:00:00.000Z',
  linkedAccountId: 'a1',
  createdAt: T0,
};
const contribution: Row = {
  id: 'gc1',
  goalId: 'g1',
  amount: 5000,
  date: T1,
  note: 'Start',
  createdAt: T1,
};
const person: Row = { id: 'p1', name: 'Rahul', icon: 'user', color: '#06b6d4', createdAt: T0 };
const debt: Row = {
  id: 'd1',
  personId: 'p1',
  amount: 1500,
  date: T1,
  note: 'Dinner',
  createdAt: T1,
};
const snapshot: Row = {
  id: 's1',
  periodKey: '2026-05',
  date: '2026-05-31T18:29:59.999Z',
  assets: 100000,
  liabilities: 2500.5,
  createdAt: T1,
};
const loan: Row = {
  id: 'ln1',
  name: 'Car loan',
  principal: 500000,
  interestRate: 9.5,
  tenureMonths: 60,
  startDate: T0,
  accountId: 'a1',
  categoryId: 'c1',
  recurringId: 'r-emi',
  createdAt: T0,
};
const prepayment: Row = {
  id: 'pp1',
  loanId: 'ln1',
  amount: 25000,
  date: T1,
  note: 'Bonus',
  transactionId: 't-pp',
  createdAt: T1,
};

/** Each variant is validated alone, as `{ [entity]: [variant] }`, so every issue text shows. */
const variants: Record<string, Row[]> = {
  accounts: [
    account,
    without(account, 'id'),
    { ...account, id: '   ' },
    { ...account, id: 5 },
    without(account, 'name'),
    { ...account, name: '' },
    { ...account, type: 'bogus' },
    without(account, 'type'),
    { ...account, type: 7 },
    { ...account, type: null },
    { ...account, type: {} },
    { ...account, type: [1, 'x', null] },
    { ...account, type: true },
    { ...account, type: 2.5 },
    { ...account, balance: '12' },
    without(account, 'balance'),
    { ...account, balance: null },
    { ...account, balance: true },
    without(account, 'openingBalance'),
    { ...account, openingBalance: '50' },
    without(account, 'color', 'icon', 'createdAt'),
    { ...account, color: 5, icon: null, createdAt: 'garbage' },
    {
      ...account,
      type: 'credit',
      creditLimit: 100000,
      statementCloseDay: 15,
      paymentDueDays: 20,
      minimumDuePercent: 5,
    },
    { ...account, creditLimit: '100', statementCloseDay: null, minimumDuePercent: 'x' },
    { ...account, archivedAt: T1 },
    { ...account, archivedAt: 'nope' },
    { ...account, currency: 'USD', extra: { nested: true } },
    { ...account, deposit: fdDeposit },
    fd,
    { ...fd, deposit: without(fdDeposit, 'compounding', 'recurringId', 'maturedAt') },
    { ...fd, deposit: { ...fdDeposit, compounding: 'weekly', tenureMonths: 12 } },
    { ...fd, deposit: without(fdDeposit, 'maturityDate') },
    { ...fd, deposit: { ...fdDeposit, maturityDate: 'never' } },
    { ...fd, deposit: { ...fdDeposit, amount: 0 } },
    { ...fd, deposit: { ...fdDeposit, amount: '50000' } },
    { ...fd, deposit: { ...fdDeposit, interestRate: -1 } },
    { ...fd, deposit: { ...fdDeposit, interestRate: 0 } },
    { ...fd, deposit: { ...fdDeposit, startDate: '' } },
    { ...fd, deposit: without(fdDeposit, 'linkedAccountId') },
    { ...fd, deposit: { ...fdDeposit, maturedAt: 'soon' } },
    without(fd, 'deposit'),
    { ...fd, deposit: [fdDeposit] },
    rd,
    {
      ...rd,
      deposit: { ...rdDeposit, tenureMonths: 12.6, maturityDate: T1, compounding: 'yearly' },
    },
    { ...rd, deposit: { ...rdDeposit, tenureMonths: 12.5 } },
    { ...rd, deposit: { ...rdDeposit, tenureMonths: 0.5 } },
    { ...rd, deposit: without(rdDeposit, 'tenureMonths') },
    { ...rd, deposit: { ...rdDeposit, tenureMonths: '12' } },
    { ...rd, deposit: null },
  ],
  transactions: [
    tx,
    { ...tx, toAccountId: 'a2', recurringId: 'r1' },
    without(tx, 'id'),
    { ...tx, type: 'refund' },
    without(tx, 'type'),
    { ...tx, amount: '5' },
    without(tx, 'amount'),
    { ...tx, amount: -1 },
    { ...tx, amount: 0 },
    without(tx, 'accountId'),
    { ...tx, accountId: ' ' },
    { ...tx, date: '2026-13-01T00:00:00.000Z' },
    without(tx, 'date'),
    { ...tx, date: 12345 },
    { ...tx, date: '' },
    { ...tx, date: '2026-06-10' },
    { ...tx, type: 'transfer' },
    { ...tx, type: 'transfer', toAccountId: 'a2' },
    { ...tx, type: 'transfer', toAccountId: '' },
    without(tx, 'categoryId', 'note', 'labels', 'createdAt'),
    { ...tx, categoryId: 5, note: 5, labels: ['a', 1, null, 'b', {}], createdAt: 'bad' },
    { ...tx, labels: 'lbl-1' },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100 },
        { categoryId: 'c2', amount: 200 },
      ],
    },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100.005 },
        { categoryId: 'c2', amount: 200.004, extra: 1 },
      ],
    },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100 },
        { categoryId: 'c2', amount: 200.02 },
      ],
    },
    { ...tx, amount: 300, splits: [{ categoryId: 'c1', amount: 300 }] },
    { ...tx, amount: 300, splits: [{ categoryId: 'c1', amount: 300 }, 'x'] },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 300 },
        { categoryId: 'c2', amount: 0 },
      ],
    },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: '', amount: 100 },
        { categoryId: 'c2', amount: 200 },
      ],
    },
    {
      ...tx,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: '100' },
        { categoryId: 'c2', amount: 200 },
      ],
    },
    { ...tx, amount: 300, splits: { categoryId: 'c1', amount: 300 } },
    {
      ...tx,
      type: 'income',
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100 },
        { categoryId: 'c2', amount: 200 },
      ],
    },
  ],
  categories: [
    category,
    { ...category, type: 'both' },
    { ...category, type: 'income' },
    { ...category, type: 'transfer' },
    without(category, 'type'),
    without(category, 'name'),
    without(category, 'id'),
    without(category, 'icon', 'color'),
  ],
  labels: [
    label,
    without(label, 'color'),
    { ...label, color: 0 },
    without(label, 'name'),
    { id: 'l2' },
  ],
  budgets: [
    budget,
    { ...budget, categoryId: '', labelId: 'l1' },
    { ...budget, labelId: '' },
    without(budget, 'categoryId'),
    { ...budget, categoryId: 5 },
    { ...budget, amount: 0 },
    { ...budget, amount: -10 },
    { ...budget, amount: '100' },
    without(budget, 'period', 'rollover', 'createdAt'),
    { ...budget, period: 'daily', rollover: 'true' },
    { ...budget, rollover: 1 },
    without(budget, 'id'),
  ],
  recurring: [
    recurring,
    {
      ...recurring,
      type: 'transfer',
      toAccountId: 'a2',
      endDate: '2027-01-01T00:00:00.000Z',
      maxOccurrences: 12,
      pausedAt: T1,
      goalId: 'g1',
    },
    { ...recurring, toAccountId: 'a2' },
    { ...recurring, maxOccurrences: 3.9, occurrenceCount: 2.7 },
    { ...recurring, maxOccurrences: 0.5, occurrenceCount: -2 },
    { ...recurring, maxOccurrences: '5', occurrenceCount: '3', lastRunDate: 'yesterday' },
    without(
      recurring,
      'occurrenceCount',
      'lastRunDate',
      'createdAt',
      'note',
      'labels',
      'categoryId',
    ),
    { ...recurring, lastRunDate: null, endDate: 'x', pausedAt: 7, goalId: '' },
    { ...recurring, type: 'transfer' },
    { ...recurring, type: 'gift' },
    { ...recurring, frequency: 'hourly' },
    without(recurring, 'frequency'),
    { ...recurring, startDate: 'later' },
    without(recurring, 'startDate'),
    { ...recurring, amount: -499 },
    { ...recurring, amount: null },
    without(recurring, 'accountId'),
  ],
  templates: [
    template,
    { ...template, toAccountId: 'a2' },
    { ...template, type: 'transfer', toAccountId: 'a2' },
    { ...template, type: 'transfer' },
    without(template, 'name'),
    { ...template, type: 'loan' },
    { ...template, amount: -5 },
    { ...template, amount: 'x' },
    without(template, 'accountId'),
    without(template, 'createdAt', 'categoryId', 'note', 'labels'),
    {
      ...template,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100 },
        { categoryId: 'c2', amount: 200 },
      ],
    },
    {
      ...template,
      amount: 300,
      splits: [
        { categoryId: 'c1', amount: 100 },
        { categoryId: 'c2', amount: 100 },
      ],
    },
  ],
  rules: [
    rule,
    { ...rule, matchType: 'regex', pattern: '^upi/(swiggy|zomato)\\b' },
    { ...rule, matchType: 'regex', pattern: '(' },
    { ...rule, matchType: 'regex', pattern: '[a-' },
    { ...rule, matchType: 'contains', pattern: '(' },
    { ...rule, matchType: 'fuzzy' },
    without(rule, 'matchType'),
    without(rule, 'pattern'),
    { ...rule, pattern: '  ' },
    without(rule, 'categoryId'),
    without(rule, 'scope', 'labelIds', 'enabled', 'createdAt'),
    { ...rule, scope: 'transfer', enabled: 'no', labelIds: ['l1', 2, 'l2'] },
    { ...rule, enabled: false },
    { ...rule, enabled: 0 },
    { ...rule, matchType: 'startsWith' },
    { ...rule, matchType: 'endsWith', scope: 'income' },
    { ...rule, matchType: 'equals', scope: 'any' },
  ],
  goals: [
    goal,
    without(goal, 'targetDate', 'linkedAccountId', 'icon', 'color', 'createdAt'),
    { ...goal, targetDate: 'someday', linkedAccountId: '' },
    { ...goal, targetAmount: 0 },
    { ...goal, targetAmount: 'x' },
    without(goal, 'targetAmount'),
    without(goal, 'name'),
    without(goal, 'id'),
  ],
  goalContributions: [
    contribution,
    { ...contribution, amount: -2000 },
    { ...contribution, amount: 0 },
    { ...contribution, amount: '10' },
    without(contribution, 'goalId'),
    { ...contribution, date: 'today' },
    without(contribution, 'note', 'createdAt'),
  ],
  people: [
    person,
    without(person, 'icon', 'color', 'createdAt'),
    without(person, 'name'),
    { ...person, id: '' },
  ],
  debtEntries: [
    debt,
    { ...debt, amount: -300, settledTransactionId: 't1' },
    { ...debt, amount: 0 },
    without(debt, 'amount'),
    without(debt, 'personId'),
    { ...debt, date: null },
    without(debt, 'note', 'createdAt'),
  ],
  netWorthSnapshots: [
    snapshot,
    { ...snapshot, periodKey: '2026-13' },
    { ...snapshot, periodKey: '2026-1' },
    { ...snapshot, periodKey: 202601 },
    without(snapshot, 'periodKey'),
    { ...snapshot, periodKey: '2026-00' },
    { ...snapshot, periodKey: '2026-12' },
    { ...snapshot, date: 'x' },
    { ...snapshot, assets: '1' },
    without(snapshot, 'liabilities'),
    { ...snapshot, liabilities: -5 },
    without(snapshot, 'createdAt'),
  ],
  loans: [
    loan,
    { ...loan, tenureMonths: 12.9, closedAt: T1, interestRate: 0 },
    without(loan, 'recurringId', 'createdAt'),
    { ...loan, principal: 0 },
    { ...loan, principal: '5' },
    { ...loan, interestRate: -1 },
    without(loan, 'interestRate'),
    { ...loan, tenureMonths: 0 },
    { ...loan, tenureMonths: 0.5 },
    { ...loan, startDate: 'x' },
    without(loan, 'accountId'),
    without(loan, 'categoryId'),
    without(loan, 'name'),
    { ...loan, closedAt: 'x' },
  ],
  loanPrepayments: [
    prepayment,
    without(prepayment, 'transactionId', 'note', 'createdAt'),
    { ...prepayment, amount: 0 },
    { ...prepayment, amount: -5 },
    without(prepayment, 'loanId'),
    { ...prepayment, date: 'x' },
  ],
};

/** A coherent multi-entity file — every cross-reference resolves, so no warnings. */
const fullFile = {
  version: 1,
  exportedAt: T1,
  accounts: [account, account2, fd, rd],
  transactions: [tx, { ...tx, id: 't2', type: 'transfer', toAccountId: 'a2', amount: 1000 }],
  categories: [category],
  labels: [label],
  budgets: [budget, { ...budget, id: 'b2', categoryId: '', labelId: 'l1' }],
  recurring: [recurring, { ...recurring, id: 'r2', goalId: 'g1' }],
  templates: [template],
  rules: [rule],
  goals: [goal],
  goalContributions: [contribution],
  people: [person],
  debtEntries: [debt],
  netWorthSnapshots: [snapshot],
  loans: [loan],
  loanPrepayments: [prepayment],
  settings: {
    theme: 'dark',
    userName: 'Asha',
    autoLocalBackup: true,
    monthStartDay: 25,
    onboardedAt: T0,
    hideAmounts: true,
    notificationsEnabled: true,
    notifyBills: false,
    notifyBudgets: false,
    notifyCreditDue: false,
    notifyLeadDays: 5,
    notifyDailyLog: false,
  },
};

const orphan = (id: string) => ({ ...tx, id, accountId: 'nope' });

/** Whole-file shapes: junk top levels, settings handling, warnings, caps, duplicates. */
const files: Array<[string, unknown]> = [
  ['null', null],
  ['array', []],
  ['string', 'x'],
  ['number', 42],
  ['true', true],
  ['empty object', {}],
  ['unknown keys only', { foo: 1, version: 1 }],
  ['null collections only', { accounts: null, transactions: null }],
  ['settings null', { settings: null }],
  ['settings array', { settings: [] }],
  ['settings string', { settings: 'dark' }],
  ['empty collections', { accounts: [], transactions: [] }],
  ['collection is an object', { accounts: { a1: account }, transactions: [tx] }],
  ['collection is a string', { transactions: 'x' }],
  ['collection is a number', { rules: 3, loans: true }],
  ['rows not objects', { accounts: [null, 5, 'x', [], true, account] }],
  [
    'duplicate ids',
    { accounts: [account, { ...account, name: 'Dup' }, without(account, 'id'), account] },
  ],
  [
    'duplicate after reject',
    { transactions: [{ ...tx, amount: -1 }, tx, { ...tx, note: 'again' }] },
  ],
  ['full coherent file', fullFile],
  ['settings empty', { settings: {} }],
  [
    'settings junk',
    {
      settings: {
        theme: 'blue',
        userName: '   ',
        autoLocalBackup: 'yes',
        monthStartDay: '25',
        hideAmounts: 1,
        notificationsEnabled: null,
        notifyBills: 'false',
        notifyBudgets: 0,
        notifyCreditDue: [],
        notifyLeadDays: '3',
        notifyDailyLog: {},
        currency: 'USD',
        onboardedAt: T0,
        unknownKey: 1,
      },
    },
  ],
  ['monthStartDay 0', { settings: { monthStartDay: 0, notifyLeadDays: 9 } }],
  ['monthStartDay 31', { settings: { monthStartDay: 31, notifyLeadDays: -3 } }],
  [
    'monthStartDay fractional',
    { settings: { monthStartDay: 15.7, notifyLeadDays: 2.9, theme: 'light' } },
  ],
  [
    'monthStartDay negative',
    { settings: { monthStartDay: -4.5, notifyLeadDays: 7, theme: 'system' } },
  ],
  ['userName untrimmed kept', { settings: { userName: '  Asha ' } }],
  [
    'legacy currency',
    { accounts: [{ ...account, currency: 'INR' }], settings: { currency: 'INR', theme: 'dark' } },
  ],
  ['newer version', { version: 2, exportedAt: T1, accounts: [account] }],
  ['version 1', { version: 1, exportedAt: 'not a date', accounts: [] }],
  ['version junk', { version: '3', accounts: [] }],
  ['version fractional', { version: 2.5, accounts: [] }],
  ['missing opening balance x1', { accounts: [without(account, 'openingBalance'), account2] }],
  [
    'missing opening balance x2',
    { accounts: [without(account, 'openingBalance'), without(account2, 'openingBalance')] },
  ],
  ['orphan transaction x1', { accounts: [account], transactions: [tx, orphan('t9')] }],
  [
    'orphan transactions x2',
    {
      accounts: [account],
      transactions: [orphan('t8'), { ...tx, id: 't9', type: 'transfer', toAccountId: 'zzz' }],
    },
  ],
  ['transactions without accounts', { transactions: [orphan('t9')] }],
  [
    'orphan recurring',
    {
      accounts: [account],
      recurring: [
        { ...recurring, accountId: 'x' },
        { ...recurring, id: 'r2', type: 'transfer', toAccountId: 'y' },
      ],
    },
  ],
  ['orphan recurring x1', { accounts: [account], recurring: [{ ...recurring, accountId: 'x' }] }],
  [
    'orphan budgets',
    {
      categories: [category],
      labels: [label],
      budgets: [
        { ...budget, categoryId: 'zz' },
        { ...budget, id: 'b2', categoryId: '' },
        { ...budget, id: 'b3', categoryId: '', labelId: 'zz' },
        { ...budget, id: 'b4', categoryId: 'yy', labelId: 'zz' },
      ],
    },
  ],
  [
    'orphan budget x1',
    {
      categories: [category],
      labels: [label],
      budgets: [{ ...budget, labelId: 'q', categoryId: 'q' }],
    },
  ],
  [
    'orphan rules',
    {
      categories: [category],
      rules: [
        { ...rule, categoryId: 'zz' },
        { ...rule, id: 'ru2', categoryId: 'yy' },
      ],
    },
  ],
  ['orphan rule x1', { categories: [category], rules: [{ ...rule, categoryId: 'zz' }] }],
  [
    'orphan contributions',
    {
      goals: [goal],
      goalContributions: [contribution, { ...contribution, id: 'gc2', goalId: 'zz' }],
    },
  ],
  [
    'orphan contributions x2',
    {
      goals: [],
      goalContributions: [contribution, { ...contribution, id: 'gc2', goalId: 'zz' }],
    },
  ],
  [
    'orphan recurring goal',
    {
      goals: [goal],
      recurring: [
        { ...recurring, goalId: 'zz' },
        { ...recurring, id: 'r2', goalId: 'g1' },
      ],
    },
  ],
  [
    'orphan recurring goals x2',
    {
      goals: [],
      recurring: [
        { ...recurring, goalId: 'zz' },
        { ...recurring, id: 'r2', goalId: 'g1' },
      ],
    },
  ],
  [
    'orphan debt x1',
    { people: [person], debtEntries: [debt, { ...debt, id: 'd2', personId: 'zz' }] },
  ],
  ['orphan debts x2', { people: [], debtEntries: [debt, { ...debt, id: 'd2', personId: 'zz' }] }],
  [
    'orphan deposit x1',
    { accounts: [{ ...fd, deposit: { ...fdDeposit, linkedAccountId: 'zz' } }] },
  ],
  [
    'orphan deposits x2',
    {
      accounts: [
        { ...fd, deposit: { ...fdDeposit, linkedAccountId: 'zz' } },
        { ...rd, deposit: { ...rdDeposit, linkedAccountId: 'yy' } },
      ],
    },
  ],
  [
    'orphan loans',
    {
      accounts: [account],
      categories: [category],
      loans: [{ ...loan, accountId: 'zz', categoryId: 'zz' }],
      loanPrepayments: [prepayment, { ...prepayment, id: 'pp2', loanId: 'zz' }],
    },
  ],
  [
    'orphan loans x2',
    {
      accounts: [],
      categories: [],
      loans: [loan, { ...loan, id: 'ln2' }],
      loanPrepayments: [
        { ...prepayment, loanId: 'zz' },
        { ...prepayment, id: 'pp2', loanId: 'zz' },
      ],
    },
  ],
  [
    'issues exactly 8',
    { transactions: Array.from({ length: 8 }, (_, i) => ({ ...tx, id: `t${i}`, amount: -1 })) },
  ],
  [
    'issues 9',
    { transactions: Array.from({ length: 9 }, (_, i) => ({ ...tx, id: `t${i}`, amount: -1 })) },
  ],
  [
    'issues across entities',
    {
      accounts: [without(account, 'name'), 5, account, account],
      transactions: Array.from({ length: 4 }, (_, i) => ({ ...tx, id: `t${i}`, date: 'x' })),
      categories: 'nope',
      labels: [{}, {}],
      loanPrepayments: [{ id: 'pp' }],
      settings: { theme: 'dark' },
    },
  ],
  ['every collection empty-but-present', Object.fromEntries(IMPORT_ENTITIES.map((e) => [e, []]))],
];

function hasImportable(report: ImportReport) {
  return hasImportableData(report);
}

export default async function cases(): Promise<GoldenCase[]> {
  const out: GoldenCase[] = [];
  out.push(gc('IMPORT_ENTITIES', [], IMPORT_ENTITIES));
  out.push(gc('ENTITY_LABELS', [], ENTITY_LABELS));

  // Built first: it fakes and then restores the clock itself.
  const [sample] = await sampleBackup();

  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(NOW);
  try {
    const run = (name: string, raw: unknown) => {
      try {
        const result = validateBackup(raw);
        out.push(gc('validateBackup', [raw, NOW], result, name));
        out.push(gc('hasImportableData', [result.report], hasImportable(result.report), name));
      } catch (e) {
        out.push(gc('validateBackup', [raw, NOW], { throws: (e as Error).message }, name));
      }
    };
    for (const [entity, rows] of Object.entries(variants)) {
      rows.forEach((row, i) => run(`${entity} #${i}`, { [entity]: [row] }));
    }
    for (const [name, raw] of files) run(name, raw);
    run('sample backup round-trip', sample.out);
  } finally {
    vi.useRealTimers();
  }
  return out;
}
