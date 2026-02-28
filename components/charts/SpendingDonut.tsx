import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
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
  const colors = useColors()
  const styles = makeStyles(colors)
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
    colors.primary,
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

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.border,
    gap: 12,
  },
  compact: {
    marginHorizontal: 16,
    marginTop: 8,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: colors.textPrimary,
  },
  emptyText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textMuted,
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
    color: colors.textMuted,
    width: 70,
  },
  barTrack: {
    flex: 1,
    height: 6,
    backgroundColor: colors.surfaceElevated,
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
    color: colors.textPrimary,
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
    color: colors.textMuted,
  },
})
}
