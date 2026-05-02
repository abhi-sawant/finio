import React, { useCallback, useMemo } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { ArrowRight } from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { TransactionItem } from '@/components/transactions/TransactionItem';
import { EmptyState } from '@/components/common/EmptyState';
import { useFinanceStore } from '@/store/useFinanceStore';
import { getRecentTransactions } from '@/store/selectors';
import { warningHaptic } from '@/utils/haptics';
import { showToast } from '@/components/common/Toast';
import type { Transaction } from '@/types';

export const RecentTransactions = React.memo(function RecentTransactions() {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const router = useRouter();
  const transactions = useFinanceStore((s) => s.transactions);
  const deleteTransaction = useFinanceStore((s) => s.deleteTransaction);
  const categories = useFinanceStore((s) => s.categories);
  const labels = useFinanceStore((s) => s.labels);
  const currency = useFinanceStore((s) => s.settings.currency);

  const recent = useMemo(() => getRecentTransactions(transactions, 8), [transactions]);

  const handlePress = useCallback(
    (tx: Transaction) => {
      router.push({ pathname: '/modals/transaction-detail', params: { id: tx.id } });
    },
    [router],
  );

  const handleEdit = useCallback(
    (tx: Transaction) => {
      router.push({ pathname: '/modals/add-transaction', params: { id: tx.id } });
    },
    [router],
  );

  const handleDelete = useCallback(
    (tx: Transaction) => {
      Alert.alert('Delete Transaction', 'This will also update the account balance.', [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await warningHaptic();
            deleteTransaction(tx.id);
            showToast({ message: 'Transaction deleted', type: 'error' });
          },
        },
      ]);
    },
    [deleteTransaction],
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.sectionTitle}>Recent Transactions</Text>
        <TouchableOpacity
          onPress={() => router.push('/(tabs)/transactions')}
          style={styles.seeAll}
          activeOpacity={0.7}
        >
          <Text style={styles.seeAllText}>See all</Text>
          <ArrowRight size={14} color={colors.primary} />
        </TouchableOpacity>
      </View>

      {recent.length === 0 ? (
        <EmptyState
          icon="arrow-left-right"
          title="No transactions yet"
          description="Tap the + button to add your first transaction"
        />
      ) : (
        <View style={styles.list}>
          {recent.map((tx) => (
            <TransactionItem
              key={tx.id}
              transaction={tx}
              onPress={handlePress}
              onEdit={handleEdit}
              onDelete={handleDelete}
              categories={categories}
              labels={labels}
              currency={currency}
            />
          ))}
        </View>
      )}
    </View>
  );
});

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      marginTop: 8,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 16,
      paddingVertical: 12,
    },
    sectionTitle: {
      fontFamily: 'Sora_700Bold',
      fontSize: 16,
      color: colors.textPrimary,
    },
    seeAll: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 4,
    },
    seeAllText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 13,
      color: colors.primary,
    },
    list: {
      borderTopWidth: 1,
      borderTopColor: colors.border,
    },
  });
}
