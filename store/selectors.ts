import { startOfMonth, endOfMonth, isWithinInterval, parseISO, subMonths, startOfWeek, endOfWeek, startOfYear, endOfYear } from 'date-fns'
import type { Account, Category, MonthlySummary, Transaction, TransactionType } from '@/types'

// ───────────────────────────────────────────────────────────
// Basic selectors (pass state slices for memoization control)
// ───────────────────────────────────────────────────────────

/**
 * Net worth = sum of all account balances.
 * Credit accounts naturally carry negative balances when money is owed,
 * so this already correctly subtracts outstanding credit debt from the total.
 */
export function getTotalBalance(accounts: Account[]): number {
  return accounts.reduce((sum, a) => sum + a.balance, 0)
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
    .reduce((sum, a) => sum + Math.abs(a.balance), 0)
}

/**
 * Returns credit accounts that currently have an outstanding balance (balance < 0),
 * sorted by amount owed descending (largest debt first).
 */
export function getUpcomingCreditPayments(accounts: Account[]): Account[] {
  return accounts
    .filter((a) => a.type === 'credit' && a.balance < 0)
    .sort((a, b) => Math.abs(b.balance) - Math.abs(a.balance))
}

export function getCategoryById(categories: Category[], id: string): Category | undefined {
  return categories.find((c) => c.id === id)
}

// ───────────────────────────────────────────────────────────
// Transaction filters
// ───────────────────────────────────────────────────────────

export function filterTransactions(
  transactions: Transaction[],
  options: {
    typeIds?: TransactionType[]
    accountId?: string
    categoryIds?: string[]
    labelIds?: string[]
    startDate?: Date
    endDate?: Date
    searchQuery?: string
  }
): Transaction[] {
  return transactions.filter((t) => {
    if (options.typeIds && options.typeIds.length > 0 && !options.typeIds.includes(t.type)) return false
    if (options.accountId && t.accountId !== options.accountId) return false
    if (options.categoryIds && options.categoryIds.length > 0 && !options.categoryIds.includes(t.categoryId)) return false
    if (options.labelIds && options.labelIds.length > 0) {
      const hasMatchingLabel = options.labelIds.some(labelId => t.labels.includes(labelId))
      if (!hasMatchingLabel) return false
    }
    if (options.startDate && options.endDate) {
      const txDate = parseISO(t.date)
      if (!isWithinInterval(txDate, { start: options.startDate, end: options.endDate })) return false
    }
    if (options.searchQuery && options.searchQuery.trim()) {
      const q = options.searchQuery.toLowerCase()
      if (!t.note.toLowerCase().includes(q)) return false
    }
    return true
  })
}

// ───────────────────────────────────────────────────────────
// Monthly summaries
// ───────────────────────────────────────────────────────────

export function getMonthlySummary(transactions: Transaction[], date: Date): {
  income: number
  expenses: number
  net: number
} {
  const start = startOfMonth(date)
  const end = endOfMonth(date)

  let income = 0
  let expenses = 0

  for (const t of transactions) {
    const txDate = parseISO(t.date)
    if (!isWithinInterval(txDate, { start, end })) continue
    if (t.type === 'income') income += t.amount
    else if (t.type === 'expense') expenses += t.amount
  }

  return { income, expenses, net: income - expenses }
}

export function getLast6MonthsSummaries(transactions: Transaction[]): MonthlySummary[] {
  const now = new Date()
  const summaries: MonthlySummary[] = []

  for (let i = 5; i >= 0; i--) {
    const date = subMonths(now, i)
    const { income, expenses, net } = getMonthlySummary(transactions, date)
    summaries.push({ month: date.getMonth(), year: date.getFullYear(), income, expenses, net })
  }

  return summaries
}

// ───────────────────────────────────────────────────────────
// Category spending
// ───────────────────────────────────────────────────────────

export interface CategorySpending {
  categoryId: string
  amount: number
  percentage: number
}

export function getCategorySpending(
  transactions: Transaction[],
  startDate: Date,
  endDate: Date
): CategorySpending[] {
  const map = new Map<string, number>()

  for (const t of transactions) {
    if (t.type !== 'expense') continue
    const txDate = parseISO(t.date)
    if (!isWithinInterval(txDate, { start: startDate, end: endDate })) continue
    map.set(t.categoryId, (map.get(t.categoryId) ?? 0) + t.amount)
  }

  const total = Array.from(map.values()).reduce((s, v) => s + v, 0)

  return Array.from(map.entries())
    .map(([categoryId, amount]) => ({
      categoryId,
      amount,
      percentage: total > 0 ? (amount / total) * 100 : 0,
    }))
    .sort((a, b) => b.amount - a.amount)
}

// ───────────────────────────────────────────────────────────
// Label spending
// ───────────────────────────────────────────────────────────

export interface LabelSpending {
  labelId: string
  amount: number
  percentage: number
}

export function getLabelSpending(
  transactions: Transaction[],
  startDate: Date,
  endDate: Date
): LabelSpending[] {
  const map = new Map<string, number>()

  for (const t of transactions) {
    if (t.type !== 'expense') continue
    const txDate = parseISO(t.date)
    if (!isWithinInterval(txDate, { start: startDate, end: endDate })) continue
    if (t.labels.length === 0) continue
    // Count the full amount for each label (not split)
    for (const labelId of t.labels) {
      map.set(labelId, (map.get(labelId) ?? 0) + t.amount)
    }
  }

  const total = Array.from(map.values()).reduce((s, v) => s + v, 0)

  return Array.from(map.entries())
    .map(([labelId, amount]) => ({
      labelId,
      amount,
      percentage: total > 0 ? (amount / total) * 100 : 0,
    }))
    .sort((a, b) => b.amount - a.amount)
}

// ───────────────────────────────────────────────────────────
// Period helpers
// ───────────────────────────────────────────────────────────

export type PeriodKey = 'week' | 'month' | '3months' | '6months' | 'year'

export function getPeriodRange(period: PeriodKey): { start: Date; end: Date } {
  const now = new Date()
  switch (period) {
    case 'week':
      return { start: startOfWeek(now, { weekStartsOn: 1 }), end: endOfWeek(now, { weekStartsOn: 1 }) }
    case 'month':
      return { start: startOfMonth(now), end: endOfMonth(now) }
    case '3months':
      return { start: startOfMonth(subMonths(now, 2)), end: endOfMonth(now) }
    case '6months':
      return { start: startOfMonth(subMonths(now, 5)), end: endOfMonth(now) }
    case 'year':
      return { start: startOfYear(now), end: endOfYear(now) }
  }
}

// ───────────────────────────────────────────────────────────
// Recent transactions
// ───────────────────────────────────────────────────────────

export function getRecentTransactions(transactions: Transaction[], limit = 8): Transaction[] {
  return [...transactions]
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, limit)
}

// ───────────────────────────────────────────────────────────
// Calculate balance after a specific transaction
// ───────────────────────────────────────────────────────────

export function getBalanceAfterTransaction(
  transactions: Transaction[],
  transaction: Transaction,
  currentAccountBalance: number
): number {
  // Get all transactions for the same account that happened after this transaction
  const laterTransactions = transactions.filter((t) => {
    const isSameAccount = t.accountId === transaction.accountId || t.toAccountId === transaction.accountId
    const txDate = new Date(t.date).getTime()
    const targetDate = new Date(transaction.date).getTime()
    // Include transactions that are later by date, or same date but created later
    const isLater = txDate > targetDate || (txDate === targetDate && t.createdAt > transaction.createdAt)
    return isSameAccount && isLater && t.id !== transaction.id
  })

  // Start with current balance and reverse the effects of later transactions
  let balanceAtTransaction = currentAccountBalance

  for (const t of laterTransactions) {
    if (t.accountId === transaction.accountId) {
      // This account was the source
      if (t.type === 'income') {
        balanceAtTransaction -= t.amount
      } else if (t.type === 'expense' || t.type === 'transfer') {
        balanceAtTransaction += t.amount
      }
    } else if (t.toAccountId === transaction.accountId && t.type === 'transfer') {
      // This account was the destination of a transfer
      balanceAtTransaction -= t.amount
    }
  }

  return balanceAtTransaction
}
