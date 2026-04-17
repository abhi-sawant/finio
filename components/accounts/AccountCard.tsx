import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { LucideIcon } from '@/components/common/IconPicker';
import { formatCurrency, hexToRgba } from '@/utils/formatters';
import { useFinanceStore } from '@/store/useFinanceStore';
import { getCurrentMonthTransactions } from '@/utils/calculations';
import type { Account } from '@/types';

const ACCOUNT_TYPE_LABELS: Record<Account['type'], string> = {
  checking: 'Checking',
  savings: 'Savings',
  cash: 'Cash',
  credit: 'Credit',
  investment: 'Investment',
  wallet: 'Wallet',
};

interface AccountCardProps {
  account: Account;
  onPress?: (account: Account) => void;
  onLongPress?: (account: Account) => void;
  variant?: 'grid' | 'horizontal';
}

export function AccountCard({ account, onPress, onLongPress, variant = 'grid' }: AccountCardProps) {
  const colors = useColors();
  const styles = makeStyles(colors);
  const transactions = useFinanceStore((s) => s.transactions);

  const isCredit = account.type === 'credit';

  // "Used this month" — sum of expenses charged to this account in the current month
  const usedThisMonth = isCredit
    ? 0
    : getCurrentMonthTransactions(transactions)
        .filter((t) => t.type === 'expense' && t.accountId === account.id)
        .reduce((sum, t) => sum + t.amount, 0);
  // Amount owed is the absolute value of the (negative) balance
  const amountDue = isCredit ? Math.abs(account.balance) : 0;
  const isCreditOwed = isCredit && account.balance < 0;
  // Utilization = owed / limit, only meaningful when limit is set
  const hasLimit = isCredit && (account.creditLimit ?? 0) > 0;
  const utilization = hasLimit ? Math.min(amountDue / (account.creditLimit as number), 1) : 0;
  // Pick a utilization colour: green < 30%, amber < 70%, red >= 70%
  const utilizationColor =
    utilization < 0.3 ? colors.income : utilization < 0.7 ? colors.warning : colors.expense;

  if (variant === 'horizontal') {
    return (
      <TouchableOpacity
        onPress={() => onPress?.(account)}
        onLongPress={() => onLongPress?.(account)}
        activeOpacity={0.8}
        style={[styles.horizontal, { borderLeftColor: account.color, borderLeftWidth: 4 }]}
      >
        <View style={[styles.iconCircle, { backgroundColor: hexToRgba(account.color, 0.2) }]}>
          <LucideIcon name={account.icon} size={18} color={account.color} />
        </View>
        <View style={styles.horizontalInfo}>
          <Text style={styles.accountName} numberOfLines={1}>
            {account.name}
          </Text>
          <Text style={styles.accountType}>{ACCOUNT_TYPE_LABELS[account.type]}</Text>
        </View>
        <View style={styles.horizontalRight}>
          {isCredit ? (
            <>
              <Text style={[styles.horizontalBalance, isCreditOwed && { color: colors.expense }]}>
                {formatCurrency(amountDue, account.currency, true)}
              </Text>
              <Text style={styles.horizontalBalanceLabel}>Amount Due</Text>
            </>
          ) : (
            <Text style={styles.horizontalBalance}>
              {formatCurrency(account.balance, account.currency, true)}
            </Text>
          )}
        </View>
      </TouchableOpacity>
    );
  }

  return (
    <TouchableOpacity
      onPress={() => onPress?.(account)}
      onLongPress={() => onLongPress?.(account)}
      activeOpacity={0.8}
      style={[styles.card, { borderColor: hexToRgba(account.color, 0.3) }]}
    >
      <View style={[styles.cardTop]}>
        <View style={[styles.iconCircle, { backgroundColor: hexToRgba(account.color, 0.2) }]}>
          <LucideIcon name={account.icon} size={22} color={account.color} />
        </View>
        <View style={[styles.typeBadge, { backgroundColor: hexToRgba(account.color, 0.15) }]}>
          <Text style={[styles.typeBadgeText, { color: account.color }]}>
            {ACCOUNT_TYPE_LABELS[account.type]}
          </Text>
        </View>
      </View>

      <Text style={styles.cardName} numberOfLines={2}>
        {account.name}
      </Text>

      {isCredit ? (
        <>
          <Text style={styles.cardBalanceLabel}>Amount Due</Text>
          <Text
            style={[styles.cardBalance, { color: isCreditOwed ? colors.expense : colors.income }]}
          >
            {formatCurrency(amountDue, account.currency, true)}
          </Text>
          {/* Utilization bar */}
          {hasLimit && (
            <View style={styles.utilizationRow}>
              <View style={styles.utilizationTrack}>
                <View
                  style={[
                    styles.utilizationFill,
                    {
                      width: `${utilization * 100}%` as `${number}%`,
                      backgroundColor: utilizationColor,
                    },
                  ]}
                />
              </View>
              <Text style={[styles.utilizationLabel, { color: utilizationColor }]}>
                {Math.round(utilization * 100)}%
              </Text>
            </View>
          )}
        </>
      ) : (
        <>
          <Text style={[styles.cardBalance, { color: account.color }]}>
            {formatCurrency(account.balance, account.currency, true)}
          </Text>
          <Text style={styles.cardBalanceLabel}>Used this month</Text>
          <Text
            style={[
              styles.cardUsed,
              { color: usedThisMonth > 0 ? colors.expense : colors.textMuted },
            ]}
          >
            {formatCurrency(usedThisMonth, account.currency, true)}
          </Text>
        </>
      )}
    </TouchableOpacity>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    // Grid variant
    card: {
      backgroundColor: colors.surface,
      borderRadius: 20,
      padding: 16,
      borderWidth: 1,
      gap: 4,
      flex: 1,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.1,
      shadowRadius: 8,
      elevation: 3,
    },
    cardTop: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 4,
    },
    cardName: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 14,
      color: colors.textPrimary,
    },
    cardBalanceLabel: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 10,
      color: colors.textMuted,
      marginTop: 2,
    },
    cardUsed: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 13,
    },
    cardBalance: {
      fontFamily: 'Sora_700Bold',
      fontSize: 18,
    },
    typeBadge: {
      borderRadius: 8,
      paddingHorizontal: 8,
      paddingVertical: 3,
    },
    typeBadgeText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 10,
    },
    utilizationRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      marginTop: 4,
    },
    utilizationTrack: {
      flex: 1,
      height: 4,
      borderRadius: 2,
      backgroundColor: colors.border,
      overflow: 'hidden',
    },
    utilizationFill: {
      height: '100%',
      borderRadius: 2,
    },
    utilizationLabel: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 10,
    },

    // Horizontal variant
    horizontal: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      backgroundColor: colors.surface,
      borderRadius: 14,
      padding: 14,
      borderWidth: 1,
      borderColor: colors.border,
    },
    horizontalInfo: {
      flex: 1,
    },
    accountName: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 14,
      color: colors.textPrimary,
    },
    accountType: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 12,
      color: colors.textMuted,
      marginTop: 2,
    },
    horizontalRight: {
      alignItems: 'flex-end',
    },
    horizontalBalance: {
      fontFamily: 'Sora_700Bold',
      fontSize: 15,
      color: colors.textPrimary,
    },
    horizontalBalanceLabel: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 10,
      color: colors.textMuted,
      marginTop: 2,
    },

    // Shared
    iconCircle: {
      width: 40,
      height: 40,
      borderRadius: 20,
      alignItems: 'center',
      justifyContent: 'center',
    },
  });
}
