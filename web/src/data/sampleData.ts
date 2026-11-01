import { addDays, subMonths } from 'date-fns';
import type {
  AccountType,
  Budget,
  BudgetPeriod,
  DebtEntry,
  DepositCompounding,
  Goal,
  GoalContribution,
  Loan,
  LoanPrepayment,
  NewDeposit,
  Person,
  RecurrenceFrequency,
  RecurringTransaction,
  Transaction,
  TransactionSplit,
  TransactionTemplate,
  TransactionType,
} from '@/types';

/**
 * A hand-designed sample dataset offered in onboarding, so a new install doesn't start on a
 * totally empty screen — and doubles as a fixture for manual QA. Everything here is a plain,
 * cross-referenced spec rather than a real entity: accounts, goals and people don't have ids
 * yet (the store mints those), so transactions/budgets/recurring rules/contributions reference
 * them by a local `key` that `loadSampleData` resolves once each is actually created.
 *
 * Deliberately deterministic — no `Math.random()` — so the same `now` always produces the same
 * dataset, which is what makes this testable and makes a screenshot reproducible.
 */

export interface SampleAccountSpec {
  key: string;
  name: string;
  type: AccountType;
  color: string;
  icon: string;
  balance: number;
  creditLimit?: number;
  statementCloseDay?: number;
  paymentDueDays?: number;
  minimumDuePercent?: number;
}

export interface SampleTransactionSpec {
  type: TransactionType;
  amount: number;
  accountKey: string;
  toAccountKey?: string;
  categoryId: string;
  date: string;
  note: string;
  labels: string[];
  /** ≥2 entries summing to `amount`; `categoryId` is then `''`. */
  splits?: TransactionSplit[];
}

export interface SampleBudgetSpec {
  categoryId: string;
  /** Set for a label budget (with `categoryId` `''`). */
  labelId?: string;
  amount: number;
  period: BudgetPeriod;
  rollover: boolean;
}

export interface SampleRecurringSpec {
  type: TransactionType;
  amount: number;
  accountKey: string;
  toAccountKey?: string;
  categoryId: string;
  note: string;
  labels: string[];
  frequency: RecurrenceFrequency;
  startDate: string;
  goalKey?: string;
}

export interface SampleGoalSpec {
  key: string;
  name: string;
  icon: string;
  color: string;
  targetAmount: number;
  targetDate?: string;
  linkedAccountKey?: string;
}

export interface SampleContributionSpec {
  goalKey: string;
  amount: number;
  date: string;
  note: string;
}

export interface SamplePersonSpec {
  key: string;
  name: string;
  icon: string;
  color: string;
}

export interface SampleDebtEntrySpec {
  personKey: string;
  amount: number;
  date: string;
  note: string;
}

export interface SampleTemplateSpec {
  name: string;
  type: TransactionType;
  amount: number;
  accountKey: string;
  toAccountKey?: string;
  categoryId: string;
  note: string;
  labels: string[];
  splits?: TransactionSplit[];
}

export interface SampleDepositSpec {
  type: 'fd' | 'rd';
  name: string;
  color: string;
  /** FD principal, or RD monthly installment. */
  amount: number;
  interestRate: number;
  startDate: string;
  maturityDate?: string;
  tenureMonths?: number;
  compounding?: DepositCompounding;
  linkedAccountKey: string;
  deductPast: boolean;
}

export interface SampleLoanSpec {
  key: string;
  name: string;
  principal: number;
  interestRate: number;
  tenureMonths: number;
  startDate: string;
  accountKey: string;
  categoryId: string;
}

export interface SampleLoanPrepaymentSpec {
  loanKey: string;
  amount: number;
  date: string;
  note: string;
}

export interface SampleData {
  accounts: SampleAccountSpec[];
  transactions: SampleTransactionSpec[];
  budgets: SampleBudgetSpec[];
  recurring: SampleRecurringSpec[];
  goals: SampleGoalSpec[];
  contributions: SampleContributionSpec[];
  people: SamplePersonSpec[];
  debtEntries: SampleDebtEntrySpec[];
  templates: SampleTemplateSpec[];
  deposits: SampleDepositSpec[];
  loans: SampleLoanSpec[];
  loanPrepayments: SampleLoanPrepaymentSpec[];
}

const PRIMARY = 'primary';
const SAVINGS = 'savings';
const CARD = 'card';
const CASH = 'cash';
const VACATION_GOAL = 'vacation';
const EMERGENCY_GOAL = 'emergency';
const LAPTOP_GOAL = 'laptop';
const RAHUL = 'rahul';
const PRIYA = 'priya';
const AMIT = 'amit';
const CAR_LOAN = 'car-loan';

/** Day-of-month offsets used to spread a category's charges across a sample month. */
const GROCERY_DAYS = [3, 10, 17, 24];
const FOOD_DAYS = [2, 6, 13, 20, 27];

function at(now: Date, monthsAgo: number, day: number, hour = 12): string {
  const base = subMonths(now, monthsAgo);
  return new Date(base.getFullYear(), base.getMonth(), day, hour).toISOString();
}

export function generateSampleData(now = new Date()): SampleData {
  const accounts: SampleAccountSpec[] = [
    {
      key: PRIMARY,
      name: 'HDFC Savings',
      type: 'savings',
      color: '#146b54',
      icon: 'landmark',
      balance: 35000,
    },
    {
      key: SAVINGS,
      name: 'SBI Savings',
      type: 'savings',
      color: '#22c55e',
      icon: 'piggy-bank',
      balance: 120000,
    },
    {
      key: CASH,
      name: 'Cash Wallet',
      type: 'cash',
      color: '#f59e0b',
      icon: 'banknote',
      balance: 4000,
    },
    {
      key: CARD,
      name: 'Credit Card',
      type: 'credit',
      color: '#ef4444',
      icon: 'credit-card',
      balance: 0,
      creditLimit: 150000,
      statementCloseDay: 28,
      paymentDueDays: 18,
      minimumDuePercent: 5,
    },
  ];

  const transactions: SampleTransactionSpec[] = [];
  const add = (
    type: TransactionType,
    amount: number,
    accountKey: string,
    categoryId: string,
    date: string,
    note: string,
    labels: string[] = [],
    extra: { toAccountKey?: string; splits?: TransactionSplit[] } = {},
  ) => {
    transactions.push({ type, amount, accountKey, categoryId, date, note, labels, ...extra });
  };

  // Month-by-month routine, oldest first. Salary, rent and bills run on the primary account;
  // the card takes fuel, shopping and subscriptions; small spends come out of the cash wallet.
  const electricity: Record<number, number> = { 3: 2200, 2: 2650, 1: 2400 };
  const bigShop: Record<number, number> = { 3: 2400, 2: 2850, 1: 2600 };
  const groceryAmounts = [1800, 2200, 1500, 2000];
  const foodAmounts = [350, 620, 480, 900, 275];
  for (let m = 3; m >= 1; m -= 1) {
    add('income', 72000, PRIMARY, 'cat-9', at(now, m, 1), 'Monthly salary');
    add('expense', 18000, PRIMARY, 'cat-8', at(now, m, 2), 'Monthly rent', ['lbl-1', 'lbl-5']);
    add('expense', 799, PRIMARY, 'cat-5', at(now, m, 7), 'Airtel broadband', ['lbl-1', 'lbl-3']);
    add('expense', 5000, PRIMARY, 'cat-20', at(now, m, 7), 'Mutual fund SIP', ['lbl-6', 'lbl-3']);
    add('expense', electricity[m], PRIMARY, 'cat-5', at(now, m, 12), 'Electricity bill', ['lbl-1']);
    add('transfer', 3000, PRIMARY, 'cat-13', at(now, m, 10), 'ATM withdrawal', [], {
      toAccountKey: CASH,
    });

    GROCERY_DAYS.forEach((day, i) => {
      add('expense', groceryAmounts[i], PRIMARY, 'cat-1', at(now, m, day), 'Groceries', ['lbl-1']);
    });
    FOOD_DAYS.forEach((day, i) => {
      add(
        'expense',
        foodAmounts[i],
        PRIMARY,
        'cat-1',
        at(now, m, day),
        i % 2 === 0 ? 'Lunch with friends' : 'Zomato order',
        i % 2 === 0 ? ['lbl-8'] : [],
      );
    });

    add('expense', 450, PRIMARY, 'cat-2', at(now, m, 22), 'Cab rides');
    add('expense', 350, CASH, 'cat-1', at(now, m, 4), 'Vegetable vendor', ['lbl-1']);
    add('expense', 180, CASH, 'cat-2', at(now, m, 9), 'Auto rickshaw');
    add('expense', 140, CASH, 'cat-1', at(now, m, 18), 'Street food', ['lbl-8']);

    // Card: a recognisable recurring-looking subscription (same note, same amount, monthly)
    // left uncovered by any recurring rule on purpose, so the Insights feed offers to turn it
    // into one; plus a split bill — one receipt, two categories.
    add('expense', 1500, CARD, 'cat-2', at(now, m, 8), 'Fuel');
    add('expense', 499, CARD, 'cat-18', at(now, m, 5), 'Netflix', ['lbl-3']);
    add('expense', 1400, CARD, 'cat-3', at(now, m, 14), 'Online shopping', ['lbl-8']);
    add('expense', 800, CARD, 'cat-4', at(now, m, 19), 'Movie night', ['lbl-8']);
    add('expense', bigShop[m], CARD, '', at(now, m, 16), 'DMart monthly stock-up', ['lbl-14'], {
      splits: [
        { categoryId: 'cat-1', amount: bigShop[m] - 600 },
        { categoryId: 'cat-17', amount: 600 },
      ],
    });
  }

  // Paying the card down, and the rest of the one-offs.
  for (let m = 2; m >= 1; m -= 1) {
    add('transfer', 6500, PRIMARY, 'cat-13', at(now, m, 20), 'Credit card payment', [], {
      toAccountKey: CARD,
    });
  }
  add('income', 12000, PRIMARY, 'cat-9', at(now, 2, 15), 'Freelance project', ['lbl-11']);
  add('expense', 999, CARD, 'cat-18', at(now, 2, 16), 'Domain renewal', ['lbl-11']);
  add('income', 25000, PRIMARY, 'cat-33', at(now, 2, 28), 'Performance bonus');
  add('income', 1240, SAVINGS, 'cat-23', at(now, 2, 28), 'Savings interest');
  add('expense', 12500, PRIMARY, 'cat-26', at(now, 3, 15), 'Health insurance premium', [
    'lbl-5',
    'lbl-4',
  ]);
  add('expense', 6000, PRIMARY, 'cat-20', at(now, 1, 15), 'Advance tax', ['lbl-4', 'lbl-5']);
  add('expense', 4200, CARD, 'cat-19', at(now, 2, 18), 'Car service', ['lbl-1']);
  add('expense', 650, PRIMARY, 'cat-6', at(now, 2, 24), 'Pharmacy', ['lbl-14']);
  add('expense', 800, PRIMARY, 'cat-6', at(now, 1, 9), 'Doctor consultation', ['lbl-14']);
  add('expense', 1500, CARD, 'cat-16', at(now, 2, 10), 'Birthday gift for Neha', ['lbl-9']);
  add('expense', 600, CASH, 'cat-17', at(now, 2, 21), 'Salon', ['lbl-8']);
  add('expense', 150, CASH, 'cat-24', at(now, 1, 6), 'Courier charges');
  add('expense', 12400, CARD, 'cat-15', at(now, 1, 12), 'Goa flights', ['lbl-14']);
  add('expense', 9600, CARD, 'cat-15', at(now, 1, 13), 'Goa hotel', ['lbl-14']);
  add('expense', 3000, PRIMARY, 'cat-1', at(now, 1, 16), 'Team dinner (paid for all)', ['lbl-7']);
  add('expense', 2799, CARD, '', at(now, 1, 22), 'Amazon order', [], {
    splits: [
      { categoryId: 'cat-3', amount: 1999 },
      { categoryId: 'cat-16', amount: 800 },
    ],
  });

  const budgets: SampleBudgetSpec[] = [
    { categoryId: '', amount: 50000, period: 'monthly', rollover: false },
    { categoryId: 'cat-1', amount: 9000, period: 'monthly', rollover: false },
    { categoryId: 'cat-3', amount: 3000, period: 'monthly', rollover: true },
    { categoryId: 'cat-2', amount: 1500, period: 'weekly', rollover: false },
    { categoryId: 'cat-16', amount: 8000, period: 'yearly', rollover: false },
    { categoryId: '', labelId: 'lbl-14', amount: 10000, period: 'monthly', rollover: false },
  ];

  const goals: SampleGoalSpec[] = [
    {
      key: VACATION_GOAL,
      name: 'Vacation Fund',
      icon: 'plane',
      color: '#f59e0b',
      targetAmount: 60000,
      targetDate: addDays(now, 240).toISOString(),
      linkedAccountKey: SAVINGS,
    },
    {
      key: EMERGENCY_GOAL,
      name: 'Emergency Fund',
      icon: 'target',
      color: '#146b54',
      targetAmount: 150000,
      linkedAccountKey: SAVINGS,
    },
    {
      key: LAPTOP_GOAL,
      name: 'New Laptop',
      icon: 'laptop',
      color: '#6366f1',
      targetAmount: 90000,
      targetDate: addDays(now, 150).toISOString(),
    },
  ];

  const contributions: SampleContributionSpec[] = [
    { goalKey: EMERGENCY_GOAL, amount: 20000, date: at(now, 2, 5), note: 'Starting balance' },
    { goalKey: EMERGENCY_GOAL, amount: 10000, date: at(now, 1, 5), note: 'Monthly top-up' },
    { goalKey: LAPTOP_GOAL, amount: 15000, date: at(now, 2, 20), note: 'From the bonus' },
    { goalKey: LAPTOP_GOAL, amount: 10000, date: at(now, 1, 20), note: 'Side-project income' },
    { goalKey: VACATION_GOAL, amount: 6000, date: at(now, 1, 25), note: 'Seed money' },
  ];

  // Rules going forward (the matching past transactions above are their history): rent, the
  // SIP, a card subscription, and a goal-linked transfer that auto-funds the Vacation Fund.
  const recurring: SampleRecurringSpec[] = [
    {
      type: 'expense',
      amount: 18000,
      accountKey: PRIMARY,
      categoryId: 'cat-8',
      note: 'Monthly rent',
      labels: ['lbl-1', 'lbl-5'],
      frequency: 'monthly',
      startDate: at(now, 0, 2),
    },
    {
      type: 'expense',
      amount: 5000,
      accountKey: PRIMARY,
      categoryId: 'cat-20',
      note: 'Mutual fund SIP',
      labels: ['lbl-6', 'lbl-3'],
      frequency: 'monthly',
      startDate: at(now, 0, 7),
    },
    {
      type: 'expense',
      amount: 130,
      accountKey: CARD,
      categoryId: 'cat-18',
      note: 'Google One storage',
      labels: ['lbl-3'],
      frequency: 'monthly',
      startDate: at(now, 0, 21),
    },
    {
      type: 'transfer',
      amount: 3000,
      accountKey: PRIMARY,
      toAccountKey: SAVINGS,
      categoryId: 'cat-13',
      note: 'Vacation savings',
      labels: [],
      frequency: 'monthly',
      startDate: at(now, 0, 25),
      goalKey: VACATION_GOAL,
    },
  ];

  const templates: SampleTemplateSpec[] = [
    {
      name: 'DMart run',
      type: 'expense',
      amount: 2500,
      accountKey: CARD,
      categoryId: '',
      note: 'DMart',
      labels: ['lbl-14'],
      splits: [
        { categoryId: 'cat-1', amount: 1900 },
        { categoryId: 'cat-17', amount: 600 },
      ],
    },
    {
      name: 'Coffee',
      type: 'expense',
      amount: 180,
      accountKey: CASH,
      categoryId: 'cat-1',
      note: 'Coffee',
      labels: ['lbl-8'],
    },
    {
      name: 'Cab to office',
      type: 'expense',
      amount: 250,
      accountKey: CARD,
      categoryId: 'cat-2',
      note: 'Cab to office',
      labels: [],
    },
    {
      name: 'ATM withdrawal',
      type: 'transfer',
      amount: 2000,
      accountKey: PRIMARY,
      toAccountKey: CASH,
      categoryId: 'cat-13',
      note: 'ATM withdrawal',
      labels: [],
    },
  ];

  // A fixed deposit (funded by a real transfer from SBI) and a recurring deposit (past
  // installments folded into its opening balance).
  const deposits: SampleDepositSpec[] = [
    {
      type: 'fd',
      name: 'SBI Fixed Deposit',
      color: '#6366f1',
      amount: 50000,
      interestRate: 7.1,
      startDate: at(now, 4, 5),
      maturityDate: addDays(now, 600).toISOString(),
      compounding: 'quarterly',
      linkedAccountKey: SAVINGS,
      deductPast: true,
    },
    {
      type: 'rd',
      name: 'Monthly RD',
      color: '#0ea5e9',
      amount: 5000,
      interestRate: 6.5,
      startDate: at(now, 3, 5),
      tenureMonths: 24,
      linkedAccountKey: PRIMARY,
      deductPast: false,
    },
  ];

  // A car loan whose EMIs due before today were paid outside Finio, plus one prepayment that
  // is a real expense.
  const loans: SampleLoanSpec[] = [
    {
      key: CAR_LOAN,
      name: 'Car Loan',
      principal: 450000,
      interestRate: 9.2,
      tenureMonths: 60,
      startDate: at(now, 3, 10),
      accountKey: PRIMARY,
      categoryId: 'cat-27',
    },
  ];
  const loanPrepayments: SampleLoanPrepaymentSpec[] = [
    { loanKey: CAR_LOAN, amount: 20000, date: at(now, 1, 26), note: 'Part-payment from bonus' },
  ];

  const people: SamplePersonSpec[] = [
    { key: RAHUL, name: 'Rahul', icon: 'user', color: '#06b6d4' },
    { key: PRIYA, name: 'Priya', icon: 'user', color: '#ec4899' },
    { key: AMIT, name: 'Amit', icon: 'user', color: '#8b5cf6' },
  ];

  const debtEntries: SampleDebtEntrySpec[] = [
    { personKey: RAHUL, amount: 1500, date: at(now, 1, 16), note: 'Lent for dinner' },
    { personKey: RAHUL, amount: -500, date: at(now, 0, 3), note: 'Paid back via UPI' },
    { personKey: PRIYA, amount: -2200, date: at(now, 2, 9), note: 'Concert tickets she booked' },
    { personKey: AMIT, amount: 5000, date: at(now, 2, 25), note: 'Short-term loan' },
  ];

  return {
    accounts,
    transactions,
    budgets,
    recurring,
    goals,
    contributions,
    people,
    debtEntries,
    templates,
    deposits,
    loans,
    loanPrepayments,
  };
}

/** The store actions `loadSampleData` needs — matches (a subset of) `FinanceStore`. */
export interface SampleDataActions {
  addAccount: (account: {
    name: string;
    type: AccountType;
    color: string;
    icon: string;
    balance: number;
    creditLimit?: number;
    statementCloseDay?: number;
    paymentDueDays?: number;
    minimumDuePercent?: number;
  }) => string;
  addDeposit: (deposit: NewDeposit) => string;
  addGoal: (goal: Omit<Goal, 'id' | 'createdAt'>) => string;
  addPerson: (person: Omit<Person, 'id' | 'createdAt'>) => string;
  addBudget: (budget: Omit<Budget, 'id' | 'createdAt'>) => void;
  addRecurring: (
    rule: Omit<RecurringTransaction, 'id' | 'createdAt' | 'occurrenceCount' | 'lastRunDate'>,
  ) => string;
  addTemplate: (template: Omit<TransactionTemplate, 'id' | 'createdAt'>) => string;
  addLoan: (loan: Omit<Loan, 'id' | 'createdAt' | 'recurringId' | 'closedAt'>) => string;
  addLoanPrepayment: (
    prepayment: Omit<LoanPrepayment, 'id' | 'createdAt' | 'transactionId'>,
  ) => string | null;
  addContribution: (contribution: Omit<GoalContribution, 'id' | 'createdAt'>) => string;
  addDebtEntry: (entry: Omit<DebtEntry, 'id' | 'createdAt'>) => string;
  bulkAddTransactions: (transactions: Omit<Transaction, 'id' | 'createdAt'>[]) => number;
}

/**
 * Applies `generateSampleData()` through the store's own actions — same code path a manual
 * entry would go through, so balances, budgets and the recurring schedule all end up exactly
 * as consistent as if a user had typed all of this in by hand.
 *
 * Call order is part of the cross-client contract (the Android port makes the same calls in the
 * same order): accounts, deposits, goals, people, budgets, recurring, templates, loans, loan
 * prepayments, contributions, debts, then one bulk transaction insert.
 */
export function loadSampleData(actions: SampleDataActions, now = new Date()): void {
  const data = generateSampleData(now);

  const accountIds = new Map(
    data.accounts.map((a) => [
      a.key,
      actions.addAccount({
        name: a.name,
        type: a.type,
        color: a.color,
        icon: a.icon,
        balance: a.balance,
        ...(a.creditLimit !== undefined ? { creditLimit: a.creditLimit } : {}),
        ...(a.statementCloseDay !== undefined ? { statementCloseDay: a.statementCloseDay } : {}),
        ...(a.paymentDueDays !== undefined ? { paymentDueDays: a.paymentDueDays } : {}),
        ...(a.minimumDuePercent !== undefined ? { minimumDuePercent: a.minimumDuePercent } : {}),
      }),
    ]),
  );
  const resolveAccount = (key: string): string => {
    const id = accountIds.get(key);
    if (!id) throw new Error(`Sample data referenced an unknown account key "${key}"`);
    return id;
  };

  for (const d of data.deposits) {
    actions.addDeposit({
      type: d.type,
      name: d.name,
      color: d.color,
      deductPast: d.deductPast,
      terms: {
        amount: d.amount,
        interestRate: d.interestRate,
        startDate: d.startDate,
        linkedAccountId: resolveAccount(d.linkedAccountKey),
        ...(d.maturityDate ? { maturityDate: d.maturityDate } : {}),
        ...(d.tenureMonths !== undefined ? { tenureMonths: d.tenureMonths } : {}),
        ...(d.compounding ? { compounding: d.compounding } : {}),
      },
    });
  }

  const goalIds = new Map(
    data.goals.map((g) => [
      g.key,
      actions.addGoal({
        name: g.name,
        icon: g.icon,
        color: g.color,
        targetAmount: g.targetAmount,
        ...(g.targetDate ? { targetDate: g.targetDate } : {}),
        ...(g.linkedAccountKey ? { linkedAccountId: resolveAccount(g.linkedAccountKey) } : {}),
      }),
    ]),
  );

  const personIds = new Map(
    data.people.map((p) => [
      p.key,
      actions.addPerson({ name: p.name, icon: p.icon, color: p.color }),
    ]),
  );

  for (const budget of data.budgets) actions.addBudget(budget);

  for (const rule of data.recurring) {
    actions.addRecurring({
      type: rule.type,
      amount: rule.amount,
      accountId: resolveAccount(rule.accountKey),
      categoryId: rule.categoryId,
      note: rule.note,
      labels: rule.labels,
      frequency: rule.frequency,
      startDate: rule.startDate,
      ...(rule.toAccountKey ? { toAccountId: resolveAccount(rule.toAccountKey) } : {}),
      ...(rule.goalKey ? { goalId: goalIds.get(rule.goalKey) } : {}),
    });
  }

  for (const t of data.templates) {
    actions.addTemplate({
      name: t.name,
      type: t.type,
      amount: t.amount,
      accountId: resolveAccount(t.accountKey),
      categoryId: t.categoryId,
      note: t.note,
      labels: t.labels,
      ...(t.toAccountKey ? { toAccountId: resolveAccount(t.toAccountKey) } : {}),
      ...(t.splits ? { splits: t.splits } : {}),
    });
  }

  const loanIds = new Map(
    data.loans.map((l) => [
      l.key,
      actions.addLoan({
        name: l.name,
        principal: l.principal,
        interestRate: l.interestRate,
        tenureMonths: l.tenureMonths,
        startDate: l.startDate,
        accountId: resolveAccount(l.accountKey),
        categoryId: l.categoryId,
      }),
    ]),
  );

  for (const p of data.loanPrepayments) {
    const loanId = loanIds.get(p.loanKey);
    if (!loanId) continue;
    actions.addLoanPrepayment({ loanId, amount: p.amount, date: p.date, note: p.note });
  }

  for (const contribution of data.contributions) {
    const goalId = goalIds.get(contribution.goalKey);
    if (!goalId) continue;
    actions.addContribution({
      goalId,
      amount: contribution.amount,
      date: contribution.date,
      note: contribution.note,
    });
  }

  for (const entry of data.debtEntries) {
    const personId = personIds.get(entry.personKey);
    if (!personId) continue;
    actions.addDebtEntry({
      personId,
      amount: entry.amount,
      date: entry.date,
      note: entry.note,
    });
  }

  actions.bulkAddTransactions(
    data.transactions.map((t) => ({
      type: t.type,
      amount: t.amount,
      accountId: resolveAccount(t.accountKey),
      categoryId: t.categoryId,
      date: t.date,
      note: t.note,
      labels: t.labels,
      ...(t.toAccountKey ? { toAccountId: resolveAccount(t.toAccountKey) } : {}),
      ...(t.splits ? { splits: t.splits } : {}),
    })),
  );
}
