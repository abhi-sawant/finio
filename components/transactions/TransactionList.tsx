import React, { useCallback, useMemo, useRef } from 'react';
import { View, Text, SectionList, StyleSheet, Alert, RefreshControl } from 'react-native';
import { useRouter } from 'expo-router';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { TransactionItem } from './TransactionItem';
import { EmptyState } from '@/components/common/EmptyState';
import { groupTransactionsByDate } from '@/utils/calculations';
import { formatDate, formatCurrency } from '@/utils/formatters';
import { useFinanceStore } from '@/store/useFinanceStore';
import { buildClosingBalanceMap } from '@/store/selectors';
import { warningHaptic } from '@/utils/haptics';
import { showToast } from '@/components/common/Toast';
import type { Transaction } from '@/types';

interface TransactionListProps {
  transactions: Transaction[];
  onRefresh?: () => void;
  refreshing?: boolean;
  currency?: string;
  showDateHeaders?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
}

// Module-level constants avoid inline object allocation on every render
const CONTENT_STYLE_EMPTY = { flex: 1 } as const;
const CONTENT_STYLE_NORMAL = { paddingBottom: 100 } as const;
const keyExtractor = (item: Transaction) => item.id;

// Pure helper — no closure deps, defined outside to avoid recreation each render
function getDateTotal(txns: Transaction[]): number {
  return txns.reduce((sum, t) => {
    if (t.type === 'income') return sum + t.amount;
    if (t.type === 'expense') return sum - t.amount;
    return sum;
  }, 0);
}

export function TransactionList({
  transactions,
  onRefresh,
  refreshing = false,
  showDateHeaders = true,
  emptyTitle = 'No transactions',
  emptyDescription = 'Add a transaction to get started',
}: TransactionListProps) {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const router = useRouter();
  const deleteTransaction = useFinanceStore((s) => s.deleteTransaction);
  const currency = useFinanceStore((s) => s.settings.currency);
  // Subscribe once here so each TransactionItem doesn't need its own store subscription
  const accounts = useFinanceStore((s) => s.accounts);
  const categories = useFinanceStore((s) => s.categories);
  const labels = useFinanceStore((s) => s.labels);

  const sections = useMemo(
    () =>
      groupTransactionsByDate(transactions).map(({ date, transactions: txns }) => ({
        title: date,
        data: txns,
      })),
    [transactions],
  );

  // Pre-compute closing balances for all transactions in O(n log n) — avoids O(n×m) per item
  const closingBalanceMap = useMemo(
    () => buildClosingBalanceMap(transactions, accounts),
    [transactions, accounts],
  );

  // Pre-compute accountId → name map so renderItem doesn't linear-scan accounts per row
  const accountNameMap = useMemo(
    () => new Map(accounts.map((a) => [a.id, a.name])),
    [accounts],
  );

  // Ref holds the latest render data so renderItem doesn't need these as useCallback deps.
  // This keeps renderItem stable across store updates (prevents SectionList from
  // re-rendering all visible rows whenever transactions/accounts change).
  const renderDataRef = useRef({ closingBalanceMap, accountNameMap, categories, labels, currency });
  renderDataRef.current = { closingBalanceMap, accountNameMap, categories, labels, currency };

  const handlePress = useCallback((tx: Transaction) => {
    router.push({ pathname: '/modals/transaction-detail', params: { id: tx.id } });
  }, [router]);

  const handleEdit = useCallback((tx: Transaction) => {
    router.push({ pathname: '/modals/add-transaction', params: { id: tx.id } });
  }, [router]);

  const handleDelete = useCallback(
    (tx: Transaction) => {
      Alert.alert(
        'Delete Transaction',
        'Are you sure you want to delete this transaction? This will also update the account balance.',
        [
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
        ],
      );
    },
    [deleteTransaction],
  );

  const renderItem = useCallback(
    ({ item }: { item: Transaction }) => {
      const { closingBalanceMap: cbMap, accountNameMap: anMap, categories: cats, labels: lbls, currency: curr } = renderDataRef.current;
      return (
        <TransactionItem
          transaction={item}
          onPress={handlePress}
          onEdit={handleEdit}
          onDelete={handleDelete}
          categories={cats}
          labels={lbls}
          currency={curr}
          closingBalance={cbMap.get(item.id)}
          accountName={anMap.get(item.accountId)}
        />
      );
    },
    [handlePress, handleEdit, handleDelete],
  );

  const renderSectionHeader = useCallback(
    ({ section }: { section: { title: string; data: Transaction[] } }) => {
      if (!showDateHeaders) return null;
      const sectionTotal = getDateTotal(section.data);
      const totalColor = sectionTotal >= 0 ? colors.income : colors.expense;
      return (
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionDate}>{formatDate(section.title + 'T00:00:00')}</Text>
          <Text style={[styles.sectionTotal, { color: totalColor }]}>
            {sectionTotal >= 0 ? '+' : ''}
            {formatCurrency(Math.abs(sectionTotal), currency as 'INR')}
          </Text>
        </View>
      );
    },
    [showDateHeaders, colors, styles, currency],
  );

  if (transactions.length === 0) {
    return (
      <EmptyState
        icon="arrow-left-right"
        title={emptyTitle}
        description={emptyDescription}
        actionLabel="Add Transaction"
        onAction={() => router.push('/modals/add-transaction')}
      />
    );
  }

  return (
    <SectionList
      sections={sections}
      keyExtractor={keyExtractor}
      renderItem={renderItem}
      renderSectionHeader={renderSectionHeader}
      refreshControl={
        onRefresh ? (
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
          />
        ) : undefined
      }
      stickySectionHeadersEnabled={false}
      showsVerticalScrollIndicator={false}
      contentContainerStyle={transactions.length === 0 ? CONTENT_STYLE_EMPTY : CONTENT_STYLE_NORMAL}
      // Low-end Android performance tuning
      removeClippedSubviews
      maxToRenderPerBatch={8}
      updateCellsBatchingPeriod={50}
      windowSize={5}
      initialNumToRender={10}
    />
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    sectionHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingHorizontal: 16,
      paddingVertical: 10,
      backgroundColor: colors.background,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    sectionDate: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 13,
      color: colors.textMuted,
    },
    sectionTotal: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 13,
    },
  });
}
