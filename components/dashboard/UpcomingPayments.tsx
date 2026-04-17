import React, { useMemo } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useRouter } from 'expo-router';
import { CreditCard, ArrowRight } from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { LucideIcon } from '@/components/common/IconPicker';
import { formatCurrency, hexToRgba } from '@/utils/formatters';
import { useFinanceStore } from '@/store/useFinanceStore';
import { getUpcomingCreditPayments } from '@/store/selectors';
import { lightHaptic } from '@/utils/haptics';
import type { Account } from '@/types';

interface CreditPaymentRowProps {
  account: Account;
  onPayPress: (account: Account) => void;
}

function CreditPaymentRow({ account, onPayPress }: CreditPaymentRowProps) {
  const colors = useColors();
  const styles = makeStyles(colors);
  const amountDue = Math.abs(account.balance);
  const hasLimit = (account.creditLimit ?? 0) > 0;
  const utilization = hasLimit ? Math.min(amountDue / (account.creditLimit as number), 1) : 0;
  const utilizationColor =
    utilization < 0.3 ? colors.income : utilization < 0.7 ? colors.warning : colors.expense;

  return (
    <View style={styles.row}>
      {/* Left: icon + info */}
      <View style={[styles.iconCircle, { backgroundColor: hexToRgba(account.color, 0.2) }]}>
        <LucideIcon name={account.icon} size={18} color={account.color} />
      </View>

      <View style={styles.rowInfo}>
        <Text style={styles.accountName} numberOfLines={1}>
          {account.name}
        </Text>

        <View style={styles.rowBottomLine}>
          <Text style={styles.amountDue}>{formatCurrency(amountDue, account.currency, true)}</Text>
          {hasLimit && (
            <Text style={styles.limitText}>
              {' '}
              / {formatCurrency(account.creditLimit as number, account.currency, true)} limit
            </Text>
          )}
        </View>

        {/* Utilization bar */}
        {hasLimit && (
          <View style={styles.utilizationRow}>
            <View style={styles.utilizationTrack}>
              <View
                style={[
                  styles.utilizationFill,
                  {
                    width: `${Math.round(utilization * 100)}%` as `${number}%`,
                    backgroundColor: utilizationColor,
                  },
                ]}
              />
            </View>
            <Text style={[styles.utilizationPct, { color: utilizationColor }]}>
              {Math.round(utilization * 100)}%
            </Text>
          </View>
        )}
      </View>

      {/* Pay button */}
      <TouchableOpacity
        style={[
          styles.payBtn,
          {
            backgroundColor: hexToRgba(account.color, 0.15),
            borderColor: hexToRgba(account.color, 0.4),
          },
        ]}
        onPress={() => {
          lightHaptic();
          onPayPress(account);
        }}
        activeOpacity={0.75}
      >
        <Text style={[styles.payBtnLabel, { color: account.color }]}>Pay</Text>
        <ArrowRight size={13} color={account.color} />
      </TouchableOpacity>
    </View>
  );
}

export function UpcomingPayments() {
  const colors = useColors();
  const styles = makeStyles(colors);
  const router = useRouter();
  const accounts = useFinanceStore((s) => s.accounts);
  const creditAccounts = useMemo(() => getUpcomingCreditPayments(accounts), [accounts]);

  if (creditAccounts.length === 0) return null;

  const handlePayPress = (account: Account) => {
    router.push({
      pathname: '/modals/add-transaction',
      params: { payBillAccountId: account.id },
    });
  };

  return (
    <View style={styles.section}>
      {/* Header */}
      <View style={styles.sectionHeader}>
        <View style={styles.sectionTitleRow}>
          <CreditCard size={16} color={colors.expense} />
          <Text style={styles.sectionTitle}>Upcoming Payments</Text>
        </View>
        <Text style={styles.sectionCount}>
          {creditAccounts.length} card{creditAccounts.length !== 1 ? 's' : ''}
        </Text>
      </View>

      {/* Cards */}
      <View style={styles.card}>
        {creditAccounts.map((account, index) => (
          <View key={account.id}>
            <CreditPaymentRow account={account} onPayPress={handlePayPress} />
            {index < creditAccounts.length - 1 && <View style={styles.rowDivider} />}
          </View>
        ))}
      </View>
    </View>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    section: {
      gap: 8,
      paddingHorizontal: 16,
    },
    sectionHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
    },
    sectionTitleRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
    },
    sectionTitle: {
      fontFamily: 'Sora_700Bold',
      fontSize: 16,
      color: colors.textPrimary,
    },
    sectionCount: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 12,
      color: colors.textMuted,
    },
    card: {
      backgroundColor: colors.surface,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: hexToRgba(colors.expense, 0.25),
      overflow: 'hidden',
    },
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      padding: 14,
    },
    rowDivider: {
      height: StyleSheet.hairlineWidth,
      backgroundColor: colors.border,
      marginHorizontal: 14,
    },
    iconCircle: {
      width: 38,
      height: 38,
      borderRadius: 19,
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
    },
    rowInfo: {
      flex: 1,
      gap: 3,
    },
    rowTopLine: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 6,
    },
    accountName: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 14,
      color: colors.textPrimary,
      flex: 1,
    },
    rowBottomLine: {
      flexDirection: 'row',
      alignItems: 'baseline',
    },
    amountDue: {
      fontFamily: 'Sora_700Bold',
      fontSize: 15,
      color: colors.expense,
    },
    limitText: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 11,
      color: colors.textMuted,
    },
    utilizationRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 6,
      marginTop: 2,
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
    utilizationPct: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 10,
      minWidth: 28,
      textAlign: 'right',
    },
    payBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 4,
      paddingHorizontal: 12,
      paddingVertical: 7,
      borderRadius: 20,
      borderWidth: 1,
      flexShrink: 0,
    },
    payBtnLabel: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 13,
    },
  });
}
