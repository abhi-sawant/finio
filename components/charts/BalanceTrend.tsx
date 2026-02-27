import React from 'react'
import { View, Text, StyleSheet, Dimensions } from 'react-native'
import { Colors } from '@/constants/Colors'
import { formatCurrency, hexToRgba } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { getTotalAccountBalance } from '@/utils/calculations'
import { subDays, format } from 'date-fns'
import type { Currency } from '@/types'

const { width: SCREEN_WIDTH } = Dimensions.get('window')
const CHART_WIDTH = SCREEN_WIDTH - 64
const CHART_HEIGHT = 100

export function BalanceTrend() {
  const { transactions, accounts, settings } = useFinanceStore()
  const currency = settings.currency as Currency
  const currentBalance = getTotalAccountBalance(accounts)

  // Build a 30-day balance trend manually
  const days = 30
  const points: number[] = []
  let balance = currentBalance

  for (let i = 0; i < days; i++) {
    const dayStr = subDays(new Date(), i).toISOString().slice(0, 10)

    // Reverse that day's transactions
    for (const t of transactions) {
      if (!t.date.startsWith(dayStr)) continue
      if (t.type === 'expense') balance += t.amount
      else if (t.type === 'income') balance -= t.amount
      else if (t.type === 'transfer') {
        // Net transfer effect on total is zero
      }
    }

    points.unshift(balance)
  }

  const minVal = Math.min(...points)
  const maxVal = Math.max(...points)
  const range = maxVal - minVal || 1

  const getY = (val: number) => {
    return CHART_HEIGHT - ((val - minVal) / range) * (CHART_HEIGHT - 16)
  }

  // Build SVG-like path — we use a simple line visualization
  const pointCoords = points.map((v, i) => ({
    x: (i / (points.length - 1)) * CHART_WIDTH,
    y: getY(v),
  }))

  const currentBalanceFormatted = formatCurrency(currentBalance, currency, true)
  const startBalance = points[0] ?? 0
  const endBalance = points[points.length - 1] ?? currentBalance
  const diff = endBalance - startBalance
  const isPositive = diff >= 0

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Balance Trend</Text>
        <Text style={[styles.diff, { color: isPositive ? Colors.income : Colors.expense }]}>
          {isPositive ? '+' : ''}
          {formatCurrency(diff, currency, true)}
        </Text>
      </View>

      {/* Simple line visualization using View */}
      <View style={styles.chartArea}>
        <Text style={styles.maxLabel}>{formatCurrency(maxVal, currency, true)}</Text>
        <View style={[styles.lineCanvas, { width: CHART_WIDTH, height: CHART_HEIGHT }]}>
          {pointCoords.slice(1).map((point, idx) => {
            const prevPoint = pointCoords[idx]
            if (!prevPoint) return null
            const dx = point.x - prevPoint.x
            const dy = point.y - prevPoint.y
            const length = Math.sqrt(dx * dx + dy * dy)
            const angle = Math.atan2(dy, dx) * (180 / Math.PI)

            return (
              <View
                key={idx}
                style={{
                  position: 'absolute',
                  left: prevPoint.x,
                  top: prevPoint.y,
                  width: length,
                  height: 2,
                  backgroundColor: Colors.primary,
                  transformOrigin: 'left center',
                  transform: [{ rotate: `${angle}deg` }],
                  opacity: 0.8,
                }}
              />
            )
          })}
          {/* Dots */}
          {pointCoords.filter((_, i) => i % 5 === 0).map((p, i) => (
            <View
              key={i}
              style={{
                position: 'absolute',
                left: p.x - 3,
                top: p.y - 3,
                width: 6,
                height: 6,
                borderRadius: 3,
                backgroundColor: Colors.primary,
              }}
            />
          ))}
        </View>
        <Text style={styles.minLabel}>{formatCurrency(minVal, currency, true)}</Text>
        <View style={styles.dateLabels}>
          <Text style={styles.dateLabel}>30 days ago</Text>
          <Text style={styles.dateLabel}>Today</Text>
        </View>
      </View>

      <Text style={styles.currentLabel}>
        Current: <Text style={styles.currentValue}>{currentBalanceFormatted}</Text>
      </Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 12,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  diff: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 14,
  },
  chartArea: {
    gap: 4,
  },
  maxLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 10,
    color: Colors.textMuted,
    textAlign: 'right',
  },
  lineCanvas: {
    position: 'relative',
    backgroundColor: hexToRgba(Colors.primary, 0.05),
    borderRadius: 8,
    overflow: 'hidden',
  },
  minLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 10,
    color: Colors.textMuted,
  },
  dateLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 4,
  },
  dateLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 10,
    color: Colors.textMuted,
  },
  currentLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 13,
    color: Colors.textMuted,
    textAlign: 'center',
  },
  currentValue: {
    fontFamily: 'DMSans_700Bold',
    color: Colors.textPrimary,
  },
})
