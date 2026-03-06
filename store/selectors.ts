import { startOfMonth, endOfMonth, isWithinInterval, parseISO, subMonths, startOfWeek, endOfWeek, startOfYear, endOfYear } from 'date-fns'
import type { Account, Category, MonthlySummary, Transaction, TransactionType } from '@/types'

// ───────────────────────────────────────────────────────────
// Basic selectors (pass state slices for memoization control)
// ───────────────────────────────────────────────────────────

export function getTotalBalance(accounts: Account[]): number {
  return accounts.reduce((sum, a) => sum + a.balance, 0)
}

export function getAccountById(accounts: Account[], id: string): Account | undefined {
  return accounts.find((a) => a.id === id)
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
    type?: TransactionType | 'all'
    accountId?: string
    categoryIds?: string[]
    startDate?: Date
    endDate?: Date
    searchQuery?: string
  }
): Transaction[] {
  return transactions.filter((t) => {
    if (options.type && options.type !== 'all' && t.type !== options.type) return false
    if (options.accountId && t.accountId !== options.accountId) return false
    if (options.categoryIds && options.categoryIds.length > 0 && !options.categoryIds.includes(t.categoryId)) return false
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
    const share = t.amount / t.labels.length
    for (const labelId of t.labels) {
      map.set(labelId, (map.get(labelId) ?? 0) + share)
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
// Balance trend (daily balances for an account for last N days)
// ───────────────────────────────────────────────────────────

export function getBalanceTrend(
  transactions: Transaction[],
  currentBalance: number,
  accountId: string,
  days = 30
): Array<{ day: number; value: number }> {
  const now = new Date()
  // Walk backwards from today, reconstructing daily balances
  const points: Array<{ day: number; value: number }> = []

  // Sort transactions for this account by date descending
  const acctTxns = transactions
    .filter((t) => t.accountId === accountId || t.toAccountId === accountId)
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())

  let runningBalance = currentBalance

  for (let i = 0; i < days; i++) {
    const day = new Date(now)
    day.setDate(now.getDate() - i)
    day.setHours(23, 59, 59, 999)

    // For each transaction that happened on this day, reverse it
    for (const t of acctTxns) {
      const txDate = parseISO(t.date)
      if (txDate.toDateString() === day.toDateString()) {
        if (t.accountId === accountId) {
          if (t.type === 'expense' || t.type === 'transfer') runningBalance += t.amount
          else if (t.type === 'income') runningBalance -= t.amount
        } else if (t.toAccountId === accountId && t.type === 'transfer') {
          runningBalance -= t.amount
        }
      }
    }

    points.unshift({ day: days - i, value: runningBalance })
  }

  return points
}
