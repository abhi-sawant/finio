import React, { useState } from 'react'
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native'
import Svg, { G, Path, Circle } from 'react-native-svg'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { formatCurrency } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { getCategorySpending } from '@/store/selectors'
import type { Currency } from '@/types'

interface SpendingDonutProps {
  startDate?: Date
  endDate?: Date
  compact?: boolean
}

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const rad = ((angleDeg - 90) * Math.PI) / 180
  return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) }
}

function describeArc(
  cx: number,
  cy: number,
  outerR: number,
  innerR: number,
  startAngle: number,
  endAngle: number
): string {
  const largeArc = endAngle - startAngle > 180 ? 1 : 0
  const o1 = polarToCartesian(cx, cy, outerR, startAngle)
  const o2 = polarToCartesian(cx, cy, outerR, endAngle)
  const i1 = polarToCartesian(cx, cy, innerR, endAngle)
  const i2 = polarToCartesian(cx, cy, innerR, startAngle)
  return (
    `M ${o1.x.toFixed(2)} ${o1.y.toFixed(2)} ` +
    `A ${outerR} ${outerR} 0 ${largeArc} 1 ${o2.x.toFixed(2)} ${o2.y.toFixed(2)} ` +
    `L ${i1.x.toFixed(2)} ${i1.y.toFixed(2)} ` +
    `A ${innerR} ${innerR} 0 ${largeArc} 0 ${i2.x.toFixed(2)} ${i2.y.toFixed(2)} Z`
  )
}

export function SpendingDonut({ startDate, endDate, compact = false }: SpendingDonutProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const { transactions, categories, settings } = useFinanceStore()
  const [showPct, setShowPct] = useState(true)

  const now = new Date()
  const start = startDate ?? new Date(now.getFullYear(), now.getMonth(), 1)
  const end = endDate ?? now

  const allSpending = getCategorySpending(transactions, start, end)
  const currency = settings.currency as Currency

  if (allSpending.length === 0) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyText}>No expense data for this period</Text>
      </View>
    )
  }

  const topItems = allSpending.slice(0, 7)
  const otherAmount = allSpending.slice(7).reduce((s, i) => s + i.amount, 0)
  const totalAmount = allSpending.reduce((s, i) => s + i.amount, 0)

  const slices = [
    ...topItems.map((item) => {
      const cat = categories.find((c) => c.id === item.categoryId)
      return {
        label: cat?.name ?? 'Unknown',
        amount: item.amount,
        percentage: item.percentage,
        color: cat?.color ?? colors.primary,
      }
    }),
    ...(otherAmount > 0
      ? [{ label: 'Other', amount: otherAmount, percentage: (otherAmount / totalAmount) * 100, color: colors.textDisabled }]
      : []),
  ]

  const SIZE = compact ? 130 : 170
  const OUTER_R = compact ? 56 : 74
  const INNER_R = compact ? 34 : 46
  const CX = SIZE / 2
  const CY = SIZE / 2
  const GAP_DEG = slices.length > 1 ? 2 : 0

  let angle = -90
  const slicesWithPaths = slices.map((slice) => {
    const sweep = (slice.percentage / 100) * 360
    const startA = angle + (slices.length > 1 ? GAP_DEG / 2 : 0)
    const endA = angle + sweep - (slices.length > 1 ? GAP_DEG / 2 : 0)
    angle += sweep
    const isSingleFull = slices.length === 1
    return { ...slice, startA, endA, isSingleFull }
  })

  return (
    <View style={styles.container}>
      {/* Donut SVG — centred */}
      <View style={styles.donutRow}>
        <View style={[styles.donutWrapper, { width: SIZE, height: SIZE }]}>
          <Svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`}>
            <G>
              {slicesWithPaths.map((s, i) =>
                s.isSingleFull ? (
                  <G key={i}>
                    <Circle cx={CX} cy={CY} r={OUTER_R} fill={s.color} />
                    <Circle cx={CX} cy={CY} r={INNER_R} fill={colors.surface} />
                  </G>
                ) : (
                  <Path
                    key={i}
                    d={describeArc(CX, CY, OUTER_R, INNER_R, s.startA, s.endA)}
                    fill={s.color}
                  />
                )
              )}
            </G>
          </Svg>
          {/* Center label overlay */}
          <View
            style={[
              styles.centerOverlay,
              {
                width: INNER_R * 2 - 4,
                height: INNER_R * 2 - 4,
                borderRadius: INNER_R,
                backgroundColor: colors.surface,
              },
            ]}
            pointerEvents="none"
          >
            <Text style={styles.centerLabel}>Total</Text>
            <Text style={styles.centerPct} numberOfLines={1} adjustsFontSizeToFit>
              {formatCurrency(totalAmount, currency, true)}
            </Text>
          </View>
        </View>
      </View>

      {/* Toggle — only in full (non-compact) mode */}
      {!compact && (
        <View style={styles.toggleRow}>
          <TouchableOpacity
            onPress={() => setShowPct(true)}
            style={[styles.toggleBtn, showPct && styles.toggleBtnActive]}
          >
            <Text style={[styles.toggleText, showPct && styles.toggleTextActive]}>
              %
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setShowPct(false)}
            style={[styles.toggleBtn, !showPct && styles.toggleBtnActive]}
          >
            <Text style={[styles.toggleText, !showPct && styles.toggleTextActive]}>
              Amt
            </Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Legend — below the chart */}
      {!compact && (
        <View style={styles.legend}>
          {slices.map((slice, i) => (
            <View key={i} style={styles.legendItem}>
              <View style={[styles.legendDot, { backgroundColor: slice.color }]} />
              <Text style={styles.legendLabel} numberOfLines={1}>
                {slice.label}
              </Text>
              <Text style={styles.legendValue}>
                {showPct
                  ? `${slice.percentage.toFixed(0)}%`
                  : formatCurrency(slice.amount, currency, true)}
              </Text>
            </View>
          ))}
        </View>
      )}
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      gap: 16,
      alignItems: 'center',
    },
    donutRow: {
      alignItems: 'center',
    },
    donutWrapper: {
      position: 'relative',
      alignItems: 'center',
      justifyContent: 'center',
    },
    centerOverlay: {
      position: 'absolute',
      alignItems: 'center',
      justifyContent: 'center',
    },
    centerLabel: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 10,
      color: colors.textMuted,
      textAlign: 'center',
    },
    centerPct: {
      fontFamily: 'Sora_700Bold',
      fontSize: 14,
      color: colors.textPrimary,
      textAlign: 'center',
      paddingHorizontal: 4,
    },
    toggleRow: {
      flexDirection: 'row',
      backgroundColor: colors.surfaceElevated,
      borderRadius: 8,
      padding: 3,
      gap: 3,
    },
    toggleBtn: {
      paddingHorizontal: 18,
      paddingVertical: 5,
      borderRadius: 6,
    },
    toggleBtnActive: {
      backgroundColor: colors.primary,
    },
    toggleText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 13,
      color: colors.textMuted,
    },
    toggleTextActive: {
      color: '#fff',
    },
    legend: {
      alignSelf: 'stretch',
      gap: 10,
    },
    legendItem: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
    },
    legendDot: {
      width: 10,
      height: 10,
      borderRadius: 5,
      flexShrink: 0,
    },
    legendLabel: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 13,
      color: colors.textPrimary,
      flex: 1,
    },
    legendValue: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 12,
      color: colors.textMuted,
      minWidth: 40,
      textAlign: 'right',
    },
    empty: {
      paddingVertical: 24,
      alignItems: 'center',
    },
    emptyText: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 14,
      color: colors.textMuted,
      textAlign: 'center',
    },
  })
}
