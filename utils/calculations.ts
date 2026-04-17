import { parseISO, isWithinInterval, startOfMonth, endOfMonth, subMonths } from 'date-fns';
import type { Transaction, Account } from '@/types';

export function getTotalIncome(transactions: Transaction[]): number {
  return transactions.filter((t) => t.type === 'income').reduce((sum, t) => sum + t.amount, 0);
}

export function getTotalExpenses(transactions: Transaction[]): number {
  return transactions.filter((t) => t.type === 'expense').reduce((sum, t) => sum + t.amount, 0);
}

/**
 * Sum of balances for all non-credit accounts — the actual money the user has in hand.
 * Credit accounts are excluded because their balance represents debt, not owned money.
 */
export function getTotalAccountBalance(accounts: Account[]): number {
  return accounts.filter((a) => a.type !== 'credit').reduce((sum, a) => sum + a.balance, 0);
}

export function getCurrentMonthTransactions(transactions: Transaction[]): Transaction[] {
  const now = new Date();
  const start = startOfMonth(now);
  const end = endOfMonth(now);
  return transactions.filter((t) => {
    const date = parseISO(t.date);
    return isWithinInterval(date, { start, end });
  });
}

export function groupTransactionsByDate(
  transactions: Transaction[],
): Array<{ date: string; transactions: Transaction[] }> {
  const sorted = [...transactions].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
  );

  const map = new Map<string, Transaction[]>();

  for (const t of sorted) {
    const dateKey = t.date.slice(0, 10);
    const existing = map.get(dateKey);
    if (existing) {
      existing.push(t);
    } else {
      map.set(dateKey, [t]);
    }
  }

  return Array.from(map.entries()).map(([date, txs]) => ({ date, transactions: txs }));
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}
