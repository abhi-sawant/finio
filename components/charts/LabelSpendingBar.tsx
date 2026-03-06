import React from 'react'
import { View, Text, StyleSheet, Dimensions } from 'react-native'
import Svg, { Rect, G, Text as SvgText } from 'react-native-svg'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { formatCurrency } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { getLabelSpending } from '@/store/selectors'
import type { Currency } from '@/types'

interface LabelSpendingBarProps {
  startDate?: Date
  endDate?: Date
}

const SCREEN_WIDTH = Dimensions.get('window').width
// 16 scrollContent padding + 16 card padding on each side
const CHART_WIDTH = SCREEN_WIDTH - 64
const LABEL_COL = 78
const AMOUNT_COL = 58
const BAR_AREA = CHART_WIDTH - LABEL_COL - AMOUNT_COL - 8
const BAR_H = 12
const ROW_H = 26

export function LabelSpendingBar({ startDate, endDate }: LabelSpendingBarProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const { transactions, labels: allLabels, settings } = useFinanceStore()
  const currency = settings.currency as Currency

  const now = new Date()
  const start = startDate ?? new Date(now.getFullYear(), now.getMonth(), 1)
  const end = endDate ?? now

  const labelSpending = getLabelSpending(transactions, start, end).slice(0, 8)

  if (labelSpending.length === 0) {
    return (
      <View style={styles.empty}>
        <Text style={styles.emptyText}>No labeled expenses for this period</Text>
      </View>
    )
  }

  const svgHeight = labelSpending.length * ROW_H

  return (
    <Svg width={CHART_WIDTH} height={svgHeight}>
      {labelSpending.map((item, idx) => {
        const label = allLabels.find((l) => l.id === item.labelId)
        if (!label) return null

        const barW = Math.max((item.percentage / 100) * BAR_AREA, 4)
        const y = idx * ROW_H
        const barY = y + (ROW_H - BAR_H) / 2
        const textY = y + ROW_H / 2 + 4 // vertical center of row

        const displayName =
          label.name.length > 10 ? label.name.slice(0, 9) + '…' : label.name

        return (
          <G key={item.labelId}>
            {/* Label name */}
            <SvgText
              x={0}
              y={textY}
              fill={colors.textPrimary}
              fontSize={11}
            >
              {displayName}
            </SvgText>

            {/* Track */}
            <Rect
              x={LABEL_COL}
              y={barY}
              width={BAR_AREA}
              height={BAR_H}
              fill={colors.surfaceElevated}
              rx={BAR_H / 2}
            />

            {/* Fill */}
            <Rect
              x={LABEL_COL}
              y={barY}
              width={barW}
              height={BAR_H}
              fill={label.color}
              rx={BAR_H / 2}
            />

            {/* Amount */}
            <SvgText
              x={CHART_WIDTH}
              y={textY}
              fill={colors.textMuted}
              fontSize={10}
              textAnchor="end"
            >
              {formatCurrency(item.amount, currency, true)}
            </SvgText>
          </G>
        )
      })}
    </Svg>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    empty: {
      paddingVertical: 20,
      alignItems: 'center',
    },
    emptyText: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 14,
      color: colors.textMuted,
    },
  })
}
