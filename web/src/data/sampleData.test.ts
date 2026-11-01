import { describe, expect, it, vi } from 'vitest';
import { generateSampleData, loadSampleData, type SampleDataActions } from './sampleData';
import { defaultCategories, defaultLabels } from './defaultData';

const NOW = new Date('2026-06-15T12:00:00.000Z');

describe('generateSampleData', () => {
  it('only references categories and labels that actually exist', () => {
    const data = generateSampleData(NOW);
    const categoryIds = new Set(defaultCategories.map((c) => c.id));
    const labelIds = new Set(defaultLabels.map((l) => l.id));

    for (const t of data.transactions) {
      // A split transaction has no category of its own — its parts do.
      if (!t.splits) expect(categoryIds.has(t.categoryId)).toBe(true);
      for (const labelId of t.labels) expect(labelIds.has(labelId)).toBe(true);
    }
    for (const b of data.budgets) {
      if (b.categoryId !== '') expect(categoryIds.has(b.categoryId)).toBe(true);
      if (b.labelId) expect(labelIds.has(b.labelId)).toBe(true);
    }
    for (const t of [...data.transactions, ...data.templates]) {
      for (const s of t.splits ?? []) expect(categoryIds.has(s.categoryId)).toBe(true);
    }
    for (const t of data.templates) {
      for (const labelId of t.labels) expect(labelIds.has(labelId)).toBe(true);
    }
    for (const l of data.loans) expect(categoryIds.has(l.categoryId)).toBe(true);
    for (const r of data.recurring) expect(categoryIds.has(r.categoryId)).toBe(true);
  });

  it('only references account, goal and person keys it actually defines', () => {
    const data = generateSampleData(NOW);
    const accountKeys = new Set(data.accounts.map((a) => a.key));
    const goalKeys = new Set(data.goals.map((g) => g.key));
    const personKeys = new Set(data.people.map((p) => p.key));

    for (const t of data.transactions) {
      expect(accountKeys.has(t.accountKey)).toBe(true);
      if (t.toAccountKey) expect(accountKeys.has(t.toAccountKey)).toBe(true);
    }
    for (const r of data.recurring) {
      expect(accountKeys.has(r.accountKey)).toBe(true);
      if (r.toAccountKey) expect(accountKeys.has(r.toAccountKey)).toBe(true);
      if (r.goalKey) expect(goalKeys.has(r.goalKey)).toBe(true);
    }
    for (const c of data.contributions) expect(goalKeys.has(c.goalKey)).toBe(true);
    for (const d of data.debtEntries) expect(personKeys.has(d.personKey)).toBe(true);
  });

  it('exercises the newer features', () => {
    const data = generateSampleData(NOW);
    expect(data.transactions.some((t) => t.splits)).toBe(true);
    expect(data.budgets.some((b) => b.labelId)).toBe(true);
    expect(data.deposits.map((d) => d.type).sort()).toEqual(['fd', 'rd']);
    expect(data.loans.length).toBeGreaterThan(0);
    expect(data.loanPrepayments.length).toBeGreaterThan(0);
    expect(data.templates.some((t) => t.splits)).toBe(true);
    expect(data.goals.some((g) => g.linkedAccountKey)).toBe(true);
  });

  it('keeps every split balanced and category-free on the parent', () => {
    const data = generateSampleData(NOW);
    for (const t of [...data.transactions, ...data.templates]) {
      if (!t.splits) continue;
      expect(t.categoryId).toBe('');
      expect(t.splits.length).toBeGreaterThanOrEqual(2);
      expect(t.splits.reduce((sum, s) => sum + s.amount, 0)).toBeCloseTo(t.amount, 2);
    }
  });

  it('produces only positive amounts and parseable dates', () => {
    const data = generateSampleData(NOW);
    for (const t of data.transactions) {
      expect(t.amount).toBeGreaterThan(0);
      expect(Number.isNaN(Date.parse(t.date))).toBe(false);
    }
  });

  it('is deterministic for a given `now`', () => {
    expect(generateSampleData(NOW)).toEqual(generateSampleData(NOW));
  });
});

describe('loadSampleData', () => {
  function fakeActions() {
    let nextId = 0;
    const calls: Record<string, unknown[]> = {
      addAccount: [],
      addGoal: [],
      addPerson: [],
      addBudget: [],
      addRecurring: [],
      addContribution: [],
      addDebtEntry: [],
      addDeposit: [],
      addTemplate: [],
      addLoan: [],
      addLoanPrepayment: [],
      bulkAddTransactions: [],
    };
    const actions: SampleDataActions = {
      addAccount: vi.fn((a) => {
        calls.addAccount.push(a);
        return `acc-${nextId++}`;
      }),
      addGoal: vi.fn((g) => {
        calls.addGoal.push(g);
        return `goal-${nextId++}`;
      }),
      addPerson: vi.fn((p) => {
        calls.addPerson.push(p);
        return `person-${nextId++}`;
      }),
      addBudget: vi.fn((b) => {
        calls.addBudget.push(b);
      }),
      addRecurring: vi.fn((r) => {
        calls.addRecurring.push(r);
        return `recurring-${nextId++}`;
      }),
      addContribution: vi.fn((c) => {
        calls.addContribution.push(c);
        return `contribution-${nextId++}`;
      }),
      addDebtEntry: vi.fn((d) => {
        calls.addDebtEntry.push(d);
        return `debt-${nextId++}`;
      }),
      addDeposit: vi.fn((d) => {
        calls.addDeposit.push(d);
        return `acc-${nextId++}`;
      }),
      addTemplate: vi.fn((t) => {
        calls.addTemplate.push(t);
        return `template-${nextId++}`;
      }),
      addLoan: vi.fn((l) => {
        calls.addLoan.push(l);
        return `loan-${nextId++}`;
      }),
      addLoanPrepayment: vi.fn((p) => {
        calls.addLoanPrepayment.push(p);
        return `prepayment-${nextId++}`;
      }),
      bulkAddTransactions: vi.fn((t) => {
        calls.bulkAddTransactions.push(t);
        return t.length;
      }),
    };
    return { actions, calls };
  }

  it('creates every account once and resolves transactions to real account ids', () => {
    const { actions, calls } = fakeActions();
    loadSampleData(actions, NOW);

    const data = generateSampleData(NOW);
    expect(calls.addAccount).toHaveLength(data.accounts.length);

    const txns = calls.bulkAddTransactions[0] as Array<{ accountId: string; toAccountId?: string }>;
    expect(txns).toHaveLength(data.transactions.length);
    for (const t of txns) {
      expect(t.accountId).toMatch(/^acc-\d+$/);
      if (t.toAccountId) expect(t.toAccountId).toMatch(/^acc-\d+$/);
    }
  });

  it('links the goal-funding recurring rule to the real goal id, not the spec key', () => {
    const { actions, calls } = fakeActions();
    loadSampleData(actions, NOW);

    const rules = calls.addRecurring as Array<{ goalId?: string; toAccountId?: string }>;
    const goalFundingRule = rules.find((r) => r.goalId !== undefined);

    expect(goalFundingRule).toBeDefined();
    expect(goalFundingRule?.goalId).toMatch(/^goal-\d+$/);
    expect(goalFundingRule?.toAccountId).toMatch(/^acc-\d+$/);
  });

  it('resolves the debt entry to the real person id', () => {
    const { actions, calls } = fakeActions();
    loadSampleData(actions, NOW);

    expect(calls.addDebtEntry.length).toBeGreaterThan(0);
    for (const e of calls.addDebtEntry as Array<{ personId: string }>) {
      expect(e.personId).toMatch(/^person-\d+$/);
    }
  });

  it('resolves the manual contribution to the real goal id', () => {
    const { actions, calls } = fakeActions();
    loadSampleData(actions, NOW);

    expect(calls.addContribution.length).toBeGreaterThan(0);
    for (const c of calls.addContribution as Array<{ goalId: string }>) {
      expect(c.goalId).toMatch(/^goal-\d+$/);
    }
  });

  it('resolves deposits, loans, prepayments and templates to real ids', () => {
    const { actions, calls } = fakeActions();
    loadSampleData(actions, NOW);

    for (const d of calls.addDeposit as Array<{ terms: { linkedAccountId: string } }>) {
      expect(d.terms.linkedAccountId).toMatch(/^acc-\d+$/);
    }
    for (const l of calls.addLoan as Array<{ accountId: string }>) {
      expect(l.accountId).toMatch(/^acc-\d+$/);
    }
    expect(calls.addLoanPrepayment.length).toBeGreaterThan(0);
    for (const p of calls.addLoanPrepayment as Array<{ loanId: string }>) {
      expect(p.loanId).toMatch(/^loan-\d+$/);
    }
    for (const t of calls.addTemplate as Array<{ accountId: string }>) {
      expect(t.accountId).toMatch(/^acc-\d+$/);
    }
  });
});
