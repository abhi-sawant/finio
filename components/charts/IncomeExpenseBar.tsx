import React from 'react'
import { View, Text, StyleSheet, Dimensions } from 'react-native'
import Svg, { Rect, Line, Text as SvgText, G } from 'react-native-svg'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { getLast6MonthsSummaries } from '@/store/selectors'
import { useFinanceStore } from '@/store/useFinanceStore'

const SCREEN_WIDTH = Dimensions.get('window').width
// 16 scrollContent padding + 16 card padding on each side
const CHART_WIDTH = SCREEN_WIDTH - 64
const CHART_HEIGHT = 130
const LABEL_H = 18
const SVG_HEIGHT = CHART_HEIGHT + LABEL_H

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function IncomeExpenseBar() {
  const colors = useColors()
  const styles = makeStyles(colors)
  const { transactions } = useFinanceStore()
  const summaries = getLast6MonthsSummaries(transactions)

  const maxAmount = Math.max(
    ...summaries.map((s) => Math.max(s.income, s.expenses)),
    1
  )

  const groupW = CHART_WIDTH / summaries.length
  const barW = Math.max(groupW * 0.28, 6)
  const barGap = 3

  const toBarH = (val: number) =>
    val > 0 ? Math.max((val / maxAmount) * (CHART_HEIGHT - 6), 3) : 0

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Income vs Expenses</Text>

      <Svg width={CHART_WIDTH} height={SVG_HEIGHT}>
        {/* Horizontal grid lines */}
        {[0, 0.25, 0.5, 0.75, 1].map((frac, i) => {
          const y = CHART_HEIGHT - frac * (CHART_HEIGHT - 6) - 3
          return (
            <Line
              key={i}
              x1={0} y1={y} x2={CHART_WIDTH} y2={y}
              stroke={colors.border}
              strokeWidth={0.5}
              strokeDasharray={i > 0 && i < 4 ? '4 4' : undefined}
            />
          )
        })}

        {summaries.map((s, idx) => {
          const cx = idx * groupW + groupW / 2
          const incomeH = toBarH(s.income)
          const expenseH = toBarH(s.expenses)

          return (
            <G key={idx}>
              {/* Income bar */}
              <Rect
                x={cx - barW - barGap / 2}
                y={CHART_HEIGHT - incomeH}
                width={barW}
                height={incomeH}
                fill={colors.income}
                rx={3}
              />
              {/* Expense bar */}
              <Rect
                x={cx + barGap / 2}
                y={CHART_HEIGHT - expenseH}
                width={barW}
                height={expenseH}
                fill={colors.expense}
                rx={3}
              />
              {/* Month label */}
              <SvgText
                x={cx}
                y={SVG_HEIGHT - 2}
                textAnchor="middle"
                fill={colors.textMuted}
                fontSize={10}
              >
                {MONTH_NAMES[s.month]}
              </SvgText>
            </G>
          )
        })}
      </Svg>

      {/* Legend */}
      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: colors.income }]} />
          <Text style={styles.legendText}>Income</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: colors.expense }]} />
          <Text style={styles.legendText}>Expenses</Text>
        </View>
      </View>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      gap: 12,
    },
    title: {
      fontFamily: 'Sora_700Bold',
      fontSize: 15,
      color: colors.textPrimary,
    },
    legend: {
      flexDirection: 'row',
      gap: 16,
    },
    legendItem: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
    },
    legendDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
    },
    legendText: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 12,
      color: colors.textMuted,
    },
  })
}
