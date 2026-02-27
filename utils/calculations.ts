import { parseISO, isWithinInterval, startOfMonth, endOfMonth, subMonths } from 'date-fns'
import type { Transaction, Account } from '@/types'

export function getTotalIncome(transactions: Transaction[]): number {
  return transactions
    .filter((t) => t.type === 'income')
    .reduce((sum, t) => sum + t.amount, 0)
}

export function getTotalExpenses(transactions: Transaction[]): number {
  return transactions
    .filter((t) => t.type === 'expense')
    .reduce((sum, t) => sum + t.amount, 0)
}

export function getNetBalance(transactions: Transaction[]): number {
  return getTotalIncome(transactions) - getTotalExpenses(transactions)
}

export function getTotalAccountBalance(accounts: Account[]): number {
  return accounts.reduce((sum, a) => sum + a.balance, 0)
}

export function getCurrentMonthTransactions(transactions: Transaction[]): Transaction[] {
  const now = new Date()
  const start = startOfMonth(now)
  const end = endOfMonth(now)
  return transactions.filter((t) => {
    const date = parseISO(t.date)
    return isWithinInterval(date, { start, end })
  })
}

export function getMonthTransactions(transactions: Transaction[], monthsAgo: number): Transaction[] {
  const date = subMonths(new Date(), monthsAgo)
  const start = startOfMonth(date)
  const end = endOfMonth(date)
  return transactions.filter((t) => {
    const txDate = parseISO(t.date)
    return isWithinInterval(txDate, { start, end })
  })
}

export function groupTransactionsByDate(
  transactions: Transaction[]
): Array<{ date: string; transactions: Transaction[] }> {
  const sorted = [...transactions].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
  )

  const map = new Map<string, Transaction[]>()

  for (const t of sorted) {
    const dateKey = t.date.slice(0, 10)
    const existing = map.get(dateKey)
    if (existing) {
      existing.push(t)
    } else {
      map.set(dateKey, [t])
    }
  }

  return Array.from(map.entries()).map(([date, txs]) => ({ date, transactions: txs }))
}

export function calculateCategoryTotals(
  transactions: Transaction[]
): Map<string, number> {
  const map = new Map<string, number>()
  for (const t of transactions) {
    if (t.type === 'expense') {
      map.set(t.categoryId, (map.get(t.categoryId) ?? 0) + t.amount)
    }
  }
  return map
}

export function getSpendingByDay(
  transactions: Transaction[],
  days: number
): Array<{ date: string; amount: number }> {
  const result: Array<{ date: string; amount: number }> = []
  const now = new Date()

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    const dateKey = d.toISOString().slice(0, 10)
    const amount = transactions
      .filter((t) => t.type === 'expense' && t.date.startsWith(dateKey))
      .reduce((sum, t) => sum + t.amount, 0)
    result.push({ date: dateKey, amount })
  }

  return result
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}
