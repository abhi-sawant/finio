import type { Currency } from '@/types'
import { format, formatDistanceToNow, isToday, isYesterday, parseISO } from 'date-fns'

// ───────────────────────────────────────────────────────────
// Currency formatting
// ───────────────────────────────────────────────────────────

const CURRENCY_LOCALE_MAP: Record<Currency, string> = {
  INR: 'en-IN',
  USD: 'en-US',
  EUR: 'de-DE',
  GBP: 'en-GB',
  JPY: 'ja-JP',
  CAD: 'en-CA',
  AUD: 'en-AU',
}

export function formatCurrency(
  amount: number,
  currency: Currency = 'INR',
  compact = false
): string {
  const locale = CURRENCY_LOCALE_MAP[currency]

  if (compact && Math.abs(amount) >= 100000) {
    const formatted = new Intl.NumberFormat(locale, {
      style: 'currency',
      currency,
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(amount)
    return formatted
  }

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatAmount(amount: number, currency: Currency = 'INR'): string {
  return formatCurrency(amount, currency)
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat('en-IN').format(value)
}

// ───────────────────────────────────────────────────────────
// Date formatting
// ───────────────────────────────────────────────────────────

export function formatDate(dateStr: string): string {
  const date = parseISO(dateStr)
  if (isToday(date)) return 'Today'
  if (isYesterday(date)) return 'Yesterday'
  return format(date, 'EEE, d MMM')
}

export function formatFullDate(dateStr: string): string {
  return format(parseISO(dateStr), 'dd MMMM yyyy')
}

export function formatShortDate(dateStr: string): string {
  return format(parseISO(dateStr), 'dd MMM')
}

export function formatMonthYear(dateStr: string): string {
  return format(parseISO(dateStr), 'MMMM yyyy')
}

export function formatRelativeTime(dateStr: string): string {
  return formatDistanceToNow(parseISO(dateStr), { addSuffix: true })
}

export function formatTime(dateStr: string): string {
  return format(parseISO(dateStr), 'h:mm a')
}

// ───────────────────────────────────────────────────────────
// Amount input helpers
// ───────────────────────────────────────────────────────────

export function parseAmountInput(raw: string): number {
  const cleaned = raw.replace(/[^0-9.]/g, '')
  const parsed = parseFloat(cleaned)
  return isNaN(parsed) ? 0 : parsed
}

export function formatAmountInput(value: number, currency: Currency = 'INR'): string {
  if (value === 0) return ''
  const locale = CURRENCY_LOCALE_MAP[currency]
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(value)
}

// ───────────────────────────────────────────────────────────
// Misc
// ───────────────────────────────────────────────────────────

export function getInitials(name: string): string {
  return name
    .trim()
    .split(' ')
    .map((w) => w[0]?.toUpperCase() ?? '')
    .slice(0, 2)
    .join('')
}

export function truncate(text: string, maxLength: number): string {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength - 1) + '…'
}

export function hexToRgba(hex: string, alpha: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
