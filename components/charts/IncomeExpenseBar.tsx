import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { format } from 'date-fns'
import { Colors } from '@/constants/Colors'
import { formatCurrency, hexToRgba } from '@/utils/formatters'
import { getLast6MonthsSummaries } from '@/store/selectors'
import { useFinanceStore } from '@/store/useFinanceStore'
import type { Currency } from '@/types'

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function IncomeExpenseBar() {
  const { transactions, settings } = useFinanceStore()
  const summaries = getLast6MonthsSummaries(transactions)
  const currency = settings.currency as Currency

  const maxAmount = Math.max(
    ...summaries.map((s) => Math.max(s.income, s.expenses)),
    1
  )

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Income vs Expenses</Text>

      <View style={styles.chart}>
        {summaries.map((summary, idx) => {
          const incomeHeight = (summary.income / maxAmount) * 120
          const expenseHeight = (summary.expenses / maxAmount) * 120

          return (
            <View key={idx} style={styles.monthGroup}>
              <View style={styles.barsRow}>
                {/* Income bar */}
                <View style={styles.barWrapper}>
                  <View
                    style={[
                      styles.bar,
                      {
                        height: Math.max(incomeHeight, 2),
                        backgroundColor: Colors.income,
                      },
                    ]}
                  />
                </View>
                {/* Expense bar */}
                <View style={styles.barWrapper}>
                  <View
                    style={[
                      styles.bar,
                      {
                        height: Math.max(expenseHeight, 2),
                        backgroundColor: Colors.expense,
                      },
                    ]}
                  />
                </View>
              </View>
              <Text style={styles.monthLabel}>{MONTH_NAMES[summary.month]}</Text>
            </View>
          )
        })}
      </View>

      {/* Legend */}
      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: Colors.income }]} />
          <Text style={styles.legendText}>Income</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: Colors.expense }]} />
          <Text style={styles.legendText}>Expenses</Text>
        </View>
      </View>
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
    gap: 16,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  chart: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    height: 140,
    paddingBottom: 20,
  },
  monthGroup: {
    flex: 1,
    alignItems: 'center',
    gap: 4,
  },
  barsRow: {
    flexDirection: 'row',
    gap: 2,
    alignItems: 'flex-end',
    flex: 1,
  },
  barWrapper: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  bar: {
    borderRadius: 4,
    minHeight: 2,
  },
  monthLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 10,
    color: Colors.textMuted,
    position: 'absolute',
    bottom: 0,
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
    color: Colors.textMuted,
  },
})
