import {
  startOfMonth,
  endOfMonth,
  isWithinInterval,
  parseISO,
  subMonths,
  startOfWeek,
  endOfWeek,
  startOfYear,
  endOfYear,
} from 'date-fns';
import type { Account, Category, MonthlySummary, Transaction, TransactionType } from '@/types';

// ───────────────────────────────────────────────────────────
// Basic selectors (pass state slices for memoization control)
// ───────────────────────────────────────────────────────────

/**
 * Net worth = sum of all account balances.
 * Credit accounts naturally carry negative balances when money is owed,
 * so this already correctly subtracts outstanding credit debt from the total.
 */
export function getTotalBalance(accounts: Account[]): number {
  return accounts.reduce((sum, a) => sum + a.balance, 0);
}

// ───────────────────────────────────────────────────────────
// Credit card selectors
// ───────────────────────────────────────────────────────────

/**
 * Total amount currently owed across all credit accounts (always >= 0).
 */
export function getTotalCreditOutstanding(accounts: Account[]): number {
  return accounts
    .filter((a) => a.type === 'credit' && a.balance < 0)
    .reduce((sum, a) => sum + Math.abs(a.balance), 0);
}

/**
 * Returns credit accounts that currently have an outstanding balance (balance < 0),
 * sorted by amount owed descending (largest debt first).
 */
export function getUpcomingCreditPayments(accounts: Account[]): Account[] {
  return accounts
    .filter((a) => a.type === 'credit' && a.balance < 0)
    .sort((a, b) => Math.abs(b.balance) - Math.abs(a.balance));
}

export function getCategoryById(categories: Category[], id: string): Category | undefined {
  return categories.find((c) => c.id === id);
}

// ───────────────────────────────────────────────────────────
// Transaction filters
// ───────────────────────────────────────────────────────────

export function filterTransactions(
  transactions: Transaction[],
  options: {
    typeIds?: TransactionType[];
    accountId?: string;
    categoryIds?: string[];
    labelIds?: string[];
    startDate?: Date;
    endDate?: Date;
    searchQuery?: string;
  },
): Transaction[] {
  return transactions.filter((t) => {
    if (options.typeIds && options.typeIds.length > 0 && !options.typeIds.includes(t.type))
      return false;
    if (options.accountId && t.accountId !== options.accountId) return false;
    if (
      options.categoryIds &&
      options.categoryIds.length > 0 &&
      !options.categoryIds.includes(t.categoryId)
    )
      return false;
    if (options.labelIds && options.labelIds.length > 0) {
      const hasMatchingLabel = options.labelIds.some((labelId) => t.labels.includes(labelId));
      if (!hasMatchingLabel) return false;
    }
    if (options.startDate && options.endDate) {
      const txDate = parseISO(t.date);
      if (!isWithinInterval(txDate, { start: options.startDate, end: options.endDate }))
        return false;
    }
    if (options.searchQuery && options.searchQuery.trim()) {
      const q = options.searchQuery.toLowerCase();
      if (!t.note.toLowerCase().includes(q)) return false;
    }
    return true;
  });
}

// ───────────────────────────────────────────────────────────
// Monthly summaries
// ───────────────────────────────────────────────────────────

export function getMonthlySummary(
  transactions: Transaction[],
  date: Date,
): {
  income: number;
  expenses: number;
  net: number;
} {
  const start = startOfMonth(date);
  const end = endOfMonth(date);

  let income = 0;
  let expenses = 0;

  for (const t of transactions) {
    const txDate = parseISO(t.date);
    if (!isWithinInterval(txDate, { start, end })) continue;
    if (t.type === 'income') income += t.amount;
    else if (t.type === 'expense') expenses += t.amount;
  }

  return { income, expenses, net: income - expenses };
}

export function getLast6MonthsSummaries(transactions: Transaction[]): MonthlySummary[] {
  const now = new Date();

  // Build the 6-month bucket array (oldest → newest)
  const months = Array.from({ length: 6 }, (_, i) => {
    const d = subMonths(now, 5 - i);
    return { month: d.getMonth(), year: d.getFullYear(), income: 0, expenses: 0, net: 0 };
  });

  const earliest = startOfMonth(subMonths(now, 5));

  // Single O(n) pass — one parseISO call per transaction instead of 6×
  for (const t of transactions) {
    if (t.type === 'transfer') continue;
    const d = parseISO(t.date);
    if (d < earliest) continue;
    const idx = months.findIndex((m) => m.month === d.getMonth() && m.year === d.getFullYear());
    if (idx === -1) continue;
    if (t.type === 'income') months[idx]!.income += t.amount;
    else months[idx]!.expenses += t.amount;
  }

  return months.map((m) => ({ ...m, net: m.income - m.expenses }));
}

// ───────────────────────────────────────────────────────────
// Category spending
// ───────────────────────────────────────────────────────────

export interface CategorySpending {
  categoryId: string;
  amount: number;
  percentage: number;
}

export function getCategorySpending(
  transactions: Transaction[],
  startDate: Date,
  endDate: Date,
): CategorySpending[] {
  const map = new Map<string, number>();

  for (const t of transactions) {
    if (t.type !== 'expense') continue;
    const txDate = parseISO(t.date);
    if (!isWithinInterval(txDate, { start: startDate, end: endDate })) continue;
    map.set(t.categoryId, (map.get(t.categoryId) ?? 0) + t.amount);
  }

  const total = Array.from(map.values()).reduce((s, v) => s + v, 0);

  return Array.from(map.entries())
    .map(([categoryId, amount]) => ({
      categoryId,
      amount,
      percentage: total > 0 ? (amount / total) * 100 : 0,
    }))
    .sort((a, b) => b.amount - a.amount);
}

// ───────────────────────────────────────────────────────────
// Label spending
// ───────────────────────────────────────────────────────────

export interface LabelSpending {
  labelId: string;
  amount: number;
  percentage: number;
}

export function getLabelSpending(
  transactions: Transaction[],
  startDate: Date,
  endDate: Date,
): LabelSpending[] {
  const map = new Map<string, number>();

  for (const t of transactions) {
    if (t.type !== 'expense') continue;
    const txDate = parseISO(t.date);
    if (!isWithinInterval(txDate, { start: startDate, end: endDate })) continue;
    if (t.labels.length === 0) continue;
    // Count the full amount for each label (not split)
    for (const labelId of t.labels) {
      map.set(labelId, (map.get(labelId) ?? 0) + t.amount);
    }
  }

  const total = Array.from(map.values()).reduce((s, v) => s + v, 0);

  return Array.from(map.entries())
    .map(([labelId, amount]) => ({
      labelId,
      amount,
      percentage: total > 0 ? (amount / total) * 100 : 0,
    }))
    .sort((a, b) => b.amount - a.amount);
}

// ───────────────────────────────────────────────────────────
// Period helpers
// ───────────────────────────────────────────────────────────

export type PeriodKey = 'week' | 'month' | '3months' | '6months' | 'year';

export function getPeriodRange(period: PeriodKey): { start: Date; end: Date } {
  const now = new Date();
  switch (period) {
    case 'week':
      return {
        start: startOfWeek(now, { weekStartsOn: 1 }),
        end: endOfWeek(now, { weekStartsOn: 1 }),
      };
    case 'month':
      return { start: startOfMonth(now), end: endOfMonth(now) };
    case '3months':
      return { start: startOfMonth(subMonths(now, 2)), end: endOfMonth(now) };
    case '6months':
      return { start: startOfMonth(subMonths(now, 5)), end: endOfMonth(now) };
    case 'year':
      return { start: startOfYear(now), end: endOfYear(now) };
  }
}

// ───────────────────────────────────────────────────────────
// Recent transactions
// ───────────────────────────────────────────────────────────

export function getRecentTransactions(transactions: Transaction[], limit = 8): Transaction[] {
  // addTransaction prepends, so the array is already newest-first.
  // Avoid the O(n log n) sort — a slice is O(1).
  return transactions.slice(0, limit);
}

// ───────────────────────────────────────────────────────────
// Calculate balance after a specific transaction
// ───────────────────────────────────────────────────────────

/**
 * Pre-compute the closing balance (balance of transaction.accountId immediately after the
 * transaction was applied) for ALL transactions in a single O(n log n) pass.
 *
 * Prefer this over calling getBalanceAfterTransaction per item to avoid O(n × m) work.
 */
export function buildClosingBalanceMap(
  transactions: Transaction[],
  accounts: Account[],
): Map<string, number> {
  const result = new Map<string, number>();
  const accountBalanceMap = new Map(accounts.map((a) => [a.id, a.balance]));

  // Build per-account transaction lists (include both source and transfer-destination sides)
  const byAccount = new Map<string, Transaction[]>();
  for (const t of transactions) {
    if (!byAccount.has(t.accountId)) byAccount.set(t.accountId, []);
    byAccount.get(t.accountId)!.push(t);
    if (t.type === 'transfer' && t.toAccountId) {
      if (!byAccount.has(t.toAccountId)) byAccount.set(t.toAccountId, []);
      byAccount.get(t.toAccountId)!.push(t);
    }
  }

  for (const [accountId, acctTxs] of byAccount) {
    const currentBalance = accountBalanceMap.get(accountId) ?? 0;

    // Sort newest-first (ISO strings compare lexicographically — no Date allocation needed)
    const sorted = acctTxs.slice().sort((a, b) => {
      if (a.date !== b.date) return a.date < b.date ? 1 : -1;
      return a.createdAt < b.createdAt ? 1 : -1;
    });

    // Walk backwards: the newest transaction's closing balance equals currentBalance.
    // Reverse each transaction's effect to recover the balance before it.
    let runningBalance = currentBalance;
    for (const t of sorted) {
      if (t.accountId === accountId) {
        // Store closing balance only for the source-account perspective
        result.set(t.id, runningBalance);
        // Reverse the effect
        if (t.type === 'income') runningBalance -= t.amount;
        else if (t.type === 'expense') runningBalance += t.amount;
        else if (t.type === 'transfer') runningBalance += t.amount;
      } else if (t.toAccountId === accountId && t.type === 'transfer') {
        // Transfer TO this account: balance increased; reverse by subtracting
        runningBalance -= t.amount;
      }
    }
  }

  return result;
}

export function getBalanceAfterTransaction(
  transactions: Transaction[],
  transaction: Transaction,
  currentAccountBalance: number,
): number {
  // Hoist constant outside the filter to avoid redundant Date allocations
  const targetDate = transaction.date;
  const targetCreatedAt = transaction.createdAt;

  // Get all transactions for the same account that happened after this transaction
  const laterTransactions = transactions.filter((t) => {
    const isSameAccount =
      t.accountId === transaction.accountId || t.toAccountId === transaction.accountId;
    // ISO strings compare lexicographically — no new Date() needed
    const isLater =
      t.date > targetDate || (t.date === targetDate && t.createdAt > targetCreatedAt);
    return isSameAccount && isLater && t.id !== transaction.id;
  });

  // Start with current balance and reverse the effects of later transactions
  let balanceAtTransaction = currentAccountBalance;

  for (const t of laterTransactions) {
    if (t.accountId === transaction.accountId) {
      // This account was the source
      if (t.type === 'income') {
        balanceAtTransaction -= t.amount;
      } else if (t.type === 'expense' || t.type === 'transfer') {
        balanceAtTransaction += t.amount;
      }
    } else if (t.toAccountId === transaction.accountId && t.type === 'transfer') {
      // This account was the destination of a transfer
      balanceAtTransaction -= t.amount;
    }
  }

  return balanceAtTransaction;
}
