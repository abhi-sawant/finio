import React, { useEffect, useRef, useState } from 'react'
import { View, Text, StyleSheet } from 'react-native'
import Animated, {
  useSharedValue,
  withTiming,
  useAnimatedStyle,
  Easing,
  interpolate,
} from 'react-native-reanimated'
import { LinearGradient } from 'expo-linear-gradient'
import { TrendingUp, TrendingDown } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { formatCurrency, hexToRgba } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import {
  getTotalAccountBalance,
  getCurrentMonthTransactions,
  getTotalIncome,
  getTotalExpenses,
} from '@/utils/calculations'
import type { Currency } from '@/types'

function AnimatedBalance({ value, currency }: { value: number; currency: Currency }) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const animValue = useSharedValue(0)
  const [displayValue, setDisplayValue] = useState(0)

  useEffect(() => {
    animValue.value = withTiming(1, { duration: 1400, easing: Easing.out(Easing.cubic) })
    // Update display every 16ms during animation
    const interval = setInterval(() => {
      const current = animValue.value * value
      setDisplayValue(current)
      if (animValue.value >= 1) clearInterval(interval)
    }, 16)
    return () => clearInterval(interval)
  }, [value])

  return (
    <Text style={styles.balanceAmount}>
      {formatCurrency(displayValue, currency, true)}
    </Text>
  )
}

export function SummaryCards() {
  const colors = useColors()
  const styles = makeStyles(colors)
  const { accounts, transactions, settings } = useFinanceStore()
  const totalBalance = getTotalAccountBalance(accounts)
  const thisMonth = getCurrentMonthTransactions(transactions)
  const monthIncome = getTotalIncome(thisMonth)
  const monthExpenses = getTotalExpenses(thisMonth)

  const currency = settings.currency as Currency
  const isDark = colors.background === '#0f1117'
  const gradientColors = isDark
    ? (['#2d2a5e', '#1a1a3e', '#0f1117'] as const)
    : (['#ede9fe', '#ddd6fe', '#c4b5fd'] as const)

  return (
    <View style={styles.container}>
      {/* Main Balance Card */}
      <LinearGradient
        colors={gradientColors}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.balanceCard}
      >
        <Text style={styles.balanceLabel}>Total Balance</Text>
        <AnimatedBalance value={totalBalance} currency={currency} />

        {/* Month summary row */}
        <View style={styles.monthRow}>
          <View style={styles.monthItem}>
            <View style={styles.monthIcon}>
              <TrendingUp size={14} color={colors.income} />
            </View>
            <View>
              <Text style={styles.monthLabel}>Income</Text>
              <Text style={[styles.monthAmount, { color: colors.income }]}>
                {formatCurrency(monthIncome, currency, true)}
              </Text>
            </View>
          </View>

          <View style={styles.divider} />

          <View style={styles.monthItem}>
            <View style={[styles.monthIcon, { backgroundColor: hexToRgba(colors.expense, 0.15) }]}>
              <TrendingDown size={14} color={colors.expense} />
            </View>
            <View>
              <Text style={styles.monthLabel}>Expenses</Text>
              <Text style={[styles.monthAmount, { color: colors.expense }]}>
                {formatCurrency(monthExpenses, currency, true)}
              </Text>
            </View>
          </View>
        </View>
      </LinearGradient>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  balanceCard: {
    borderRadius: 24,
    padding: 24,
    gap: 8,
    borderWidth: 1,
    borderColor: hexToRgba(colors.primary, 0.3),
  },
  balanceLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textMuted,
  },
  balanceAmount: {
    fontFamily: 'Sora_800ExtraBold',
    fontSize: 36,
    color: colors.textPrimary,
    marginVertical: 4,
  },
  monthRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
    backgroundColor: hexToRgba(colors.background === '#0f1117' ? '#ffffff' : '#000000', 0.06),
    borderRadius: 16,
    padding: 14,
    gap: 16,
  },
  monthItem: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  monthIcon: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: hexToRgba(colors.income, 0.15),
    alignItems: 'center',
    justifyContent: 'center',
  },
  monthLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 11,
    color: colors.textMuted,
  },
  monthAmount: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 14,
  },
  divider: {
    width: 1,
    height: 36,
    backgroundColor: colors.border,
  },
})
}
