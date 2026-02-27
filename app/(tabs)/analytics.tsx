import React, { useState } from 'react'
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { Colors } from '@/constants/Colors'
import { SpendingDonut } from '@/components/charts/SpendingDonut'
import { IncomeExpenseBar } from '@/components/charts/IncomeExpenseBar'
import { BalanceTrend } from '@/components/charts/BalanceTrend'
import { useFinanceStore } from '@/store/useFinanceStore'
import {
  getLast6MonthsSummaries,
  getCategorySpending,
  getPeriodRange,
  filterTransactions,
} from '@/store/selectors'
import { formatCurrency } from '@/utils/formatters'
import { lightHaptic } from '@/utils/haptics'
import { format } from 'date-fns'
import type { PeriodKey } from '@/store/selectors'

const PERIODS: { key: PeriodKey; label: string }[] = [
  { key: 'week', label: 'Week' },
  { key: 'month', label: 'Month' },
  { key: '3months', label: '3M' },
  { key: '6months', label: '6M' },
  { key: 'year', label: 'Year' },
]

export default function AnalyticsScreen() {
  const insets = useSafeAreaInsets()
  const { transactions, accounts, categories, settings } = useFinanceStore()
  const [period, setPeriod] = useState<PeriodKey>('month')

  const { start, end } = getPeriodRange(period)
  const periodTransactions = filterTransactions(transactions, {
    startDate: start,
    endDate: end,
  })

  const monthlySummaries = getLast6MonthsSummaries(transactions)
  const categorySpending = getCategorySpending(periodTransactions, start, end)

  const totalIncome = periodTransactions
    .filter((t) => t.type === 'income')
    .reduce((sum, t) => sum + t.amount, 0)
  const totalExpense = periodTransactions
    .filter((t) => t.type === 'expense')
    .reduce((sum, t) => sum + t.amount, 0)

  const handlePeriod = (key: PeriodKey) => {
    lightHaptic()
    setPeriod(key)
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Analytics</Text>
      </View>

      {/* Period selector */}
      <View style={styles.periodRow}>
        {PERIODS.map((p) => (
          <TouchableOpacity
            key={p.key}
            onPress={() => handlePeriod(p.key)}
            style={[styles.periodChip, period === p.key && styles.periodChipActive]}
          >
            <Text
              style={[
                styles.periodLabel,
                period === p.key && styles.periodLabelActive,
              ]}
            >
              {p.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Summary row */}
        <View style={styles.summaryRow}>
          <View style={[styles.summaryCard, styles.incomeCard]}>
            <Text style={styles.summaryLabel}>Income</Text>
            <Text style={[styles.summaryValue, { color: Colors.income }]}>
              {formatCurrency(totalIncome, settings.currency)}
            </Text>
          </View>
          <View style={[styles.summaryCard, styles.expenseCard]}>
            <Text style={styles.summaryLabel}>Expenses</Text>
            <Text style={[styles.summaryValue, { color: Colors.expense }]}>
              {formatCurrency(totalExpense, settings.currency)}
            </Text>
          </View>
        </View>

        {/* Income vs Expense bar chart */}
        <View style={styles.card}>
          <IncomeExpenseBar />
        </View>

        {/* Balance trend */}
        <View style={styles.card}>
          <BalanceTrend />
        </View>

        {/* Spending by category */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Spending by Category</Text>
          <SpendingDonut startDate={start} endDate={end} />
        </View>

        {/* Top categories table */}
        {categorySpending.length > 0 && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Top Categories</Text>
            <View style={styles.categoryTable}>
              {categorySpending.slice(0, 8).map((item, index) => {
                const cat = categories.find((c) => c.id === item.categoryId)
                if (!cat) return null
                return (
                  <View key={item.categoryId} style={styles.categoryRow}>
                    <View style={styles.categoryLeft}>
                      <Text style={styles.categoryRank}>{index + 1}</Text>
                      <View
                        style={[styles.categoryDot, { backgroundColor: cat.color }]}
                      />
                      <Text style={styles.categoryName}>{cat.name}</Text>
                    </View>
                    <View style={styles.categoryRight}>
                      <Text style={styles.categoryAmount}>
                        {formatCurrency(item.amount, settings.currency)}
                      </Text>
                      <Text style={styles.categoryPct}>{item.percentage.toFixed(0)}%</Text>
                    </View>
                  </View>
                )
              })}
            </View>
          </View>
        )}

        {/* Monthly summary table */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Monthly Summary</Text>
          <View style={styles.monthlyTable}>
            <View style={styles.monthlyHeader}>
              <Text style={[styles.monthlyCell, styles.monthlyHeaderText]}>Month</Text>
              <Text style={[styles.monthlyCell, styles.monthlyHeaderText, styles.right]}>Income</Text>
              <Text style={[styles.monthlyCell, styles.monthlyHeaderText, styles.right]}>Expense</Text>
              <Text style={[styles.monthlyCell, styles.monthlyHeaderText, styles.right]}>Net</Text>
            </View>
            {monthlySummaries.slice().reverse().map((summary) => {
              const net = summary.income - summary.expenses
              const monthDate = new Date(summary.year, summary.month, 1)
              return (
                <View key={`${summary.year}-${summary.month}`} style={styles.monthlyRow}>
                  <Text style={styles.monthlyCell}>
                    {format(monthDate, 'MMM yy')}
                  </Text>
                  <Text style={[styles.monthlyCell, styles.right, { color: Colors.income }]}>
                    {formatCurrency(summary.income, settings.currency)}
                  </Text>
                  <Text style={[styles.monthlyCell, styles.right, { color: Colors.expense }]}>
                    {formatCurrency(summary.expenses, settings.currency)}
                  </Text>
                  <Text
                    style={[
                      styles.monthlyCell,
                      styles.right,
                      { color: net >= 0 ? Colors.income : Colors.expense },
                    ]}
                  >
                    {net >= 0 ? '+' : ''}
                    {formatCurrency(net, settings.currency)}
                  </Text>
                </View>
              )
            })}
          </View>
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 24,
    color: Colors.textPrimary,
  },
  periodRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingBottom: 12,
    gap: 8,
  },
  periodChip: {
    flex: 1,
    paddingVertical: 7,
    borderRadius: 8,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  periodChipActive: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  periodLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  periodLabelActive: {
    color: '#fff',
  },
  scrollContent: {
    gap: 16,
    paddingHorizontal: 16,
  },
  summaryRow: {
    flexDirection: 'row',
    gap: 12,
  },
  summaryCard: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  incomeCard: {
    borderLeftWidth: 3,
    borderLeftColor: Colors.income,
  },
  expenseCard: {
    borderLeftWidth: 3,
    borderLeftColor: Colors.expense,
  },
  summaryLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
    marginBottom: 4,
  },
  summaryValue: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
  },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    gap: 12,
  },
  cardTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  categoryTable: {
    gap: 10,
  },
  categoryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  categoryLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  categoryRank: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
    width: 16,
    textAlign: 'center',
  },
  categoryDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  categoryName: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textPrimary,
    flex: 1,
  },
  categoryRight: {
    alignItems: 'flex-end',
    gap: 2,
  },
  categoryAmount: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textPrimary,
  },
  categoryPct: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 11,
    color: Colors.textMuted,
  },
  monthlyTable: {
    gap: 8,
  },
  monthlyHeader: {
    flexDirection: 'row',
    paddingBottom: 6,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  monthlyHeaderText: {
    color: Colors.textMuted,
    fontSize: 11,
    fontFamily: 'DMSans_500Medium',
  },
  monthlyRow: {
    flexDirection: 'row',
    paddingVertical: 2,
  },
  monthlyCell: {
    flex: 1,
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textPrimary,
  },
  right: {
    textAlign: 'right',
  },
})
