import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '@/constants/Colors'
import { formatCurrency, hexToRgba } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { getCategorySpending } from '@/store/selectors'
import type { Currency } from '@/types'

interface SpendingDonutProps {
  startDate?: Date
  endDate?: Date
  compact?: boolean
}

export function SpendingDonut({ startDate, endDate, compact = false }: SpendingDonutProps) {
  const { transactions, categories, settings } = useFinanceStore()

  const now = new Date()
  const start = startDate ?? new Date(now.getFullYear(), now.getMonth(), 1)
  const end = endDate ?? now

  const spending = getCategorySpending(transactions, start, end).slice(0, 6)
  const currency = settings.currency as Currency

  if (spending.length === 0) {
    return (
      <View style={[styles.container, compact && styles.compact]}>
        <Text style={styles.emptyText}>No expense data for this period</Text>
      </View>
    )
  }

  const total = spending.reduce((s, item) => s + item.amount, 0)

  const COLORS = [
    Colors.primary,
    '#22c55e',
    '#ef4444',
    '#f59e0b',
    '#3b82f6',
    '#ec4899',
  ]

  return (
    <View style={[styles.container, compact && styles.compact]}>
      {!compact && <Text style={styles.title}>Spending by Category</Text>}

      {/* Simple horizontal bar chart fallback (Victory Native XL may need Skia) */}
      <View style={styles.barChart}>
        {spending.map((item, idx) => {
          const category = categories.find((c) => c.id === item.categoryId)
          const color = category?.color ?? COLORS[idx % COLORS.length]
          const width = `${Math.max(item.percentage, 2)}%` as `${number}%`

          return (
            <View key={item.categoryId} style={styles.barRow}>
              <View style={[styles.colorDot, { backgroundColor: color }]} />
              <Text style={styles.categoryName} numberOfLines={1}>
                {category?.name ?? 'Other'}
              </Text>
              <View style={styles.barTrack}>
                <View
                  style={[
                    styles.barFill,
                    { width, backgroundColor: color },
                  ]}
                />
              </View>
              <Text style={styles.barAmount}>
                {formatCurrency(item.amount, currency, true)}
              </Text>
            </View>
          )
        })}
      </View>

      {/* Donut legend row */}
      <View style={styles.donutRow}>
        {spending.slice(0, 4).map((item, idx) => {
          const category = categories.find((c) => c.id === item.categoryId)
          const color = category?.color ?? COLORS[idx % COLORS.length]
          const pct = item.percentage.toFixed(0)

          return (
            <View key={item.categoryId} style={styles.legendItem}>
              <View style={[styles.legendDot, { backgroundColor: color }]} />
              <Text style={styles.legendPct}>{pct}%</Text>
            </View>
          )
        })}
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: Colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 12,
  },
  compact: {
    marginHorizontal: 16,
    marginTop: 8,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  emptyText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: 'center',
    paddingVertical: 24,
  },
  barChart: {
    gap: 10,
  },
  barRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  colorDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  categoryName: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
    width: 70,
  },
  barTrack: {
    flex: 1,
    height: 6,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 3,
    overflow: 'hidden',
  },
  barFill: {
    height: 6,
    borderRadius: 3,
  },
  barAmount: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 12,
    color: Colors.textPrimary,
    minWidth: 60,
    textAlign: 'right',
  },
  donutRow: {
    flexDirection: 'row',
    gap: 12,
    flexWrap: 'wrap',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendPct: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 11,
    color: Colors.textMuted,
  },
})
