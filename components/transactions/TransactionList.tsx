import React, { useCallback } from 'react'
import { View, Text, SectionList, StyleSheet, Alert, RefreshControl } from 'react-native'
import { useRouter } from 'expo-router'
import { Colors } from '@/constants/Colors'
import { TransactionItem } from './TransactionItem'
import { EmptyState } from '@/components/common/EmptyState'
import { groupTransactionsByDate } from '@/utils/calculations'
import { formatDate, formatCurrency } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { warningHaptic } from '@/utils/haptics'
import { showToast } from '@/components/common/Toast'
import type { Transaction } from '@/types'

interface TransactionListProps {
  transactions: Transaction[]
  onRefresh?: () => void
  refreshing?: boolean
  currency?: string
  showDateHeaders?: boolean
  emptyTitle?: string
  emptyDescription?: string
}

export function TransactionList({
  transactions,
  onRefresh,
  refreshing = false,
  currency = 'INR',
  showDateHeaders = true,
  emptyTitle = 'No transactions',
  emptyDescription = 'Add a transaction to get started',
}: TransactionListProps) {
  const router = useRouter()
  const { deleteTransaction, settings } = useFinanceStore()

  const grouped = groupTransactionsByDate(transactions)

  const sections = grouped.map(({ date, transactions: txns }) => ({
    title: date,
    data: txns,
  }))

  const handlePress = useCallback((tx: Transaction) => {
    router.push({ pathname: '/modals/transaction-detail', params: { id: tx.id } })
  }, [])

  const handleEdit = useCallback((tx: Transaction) => {
    router.push({ pathname: '/modals/add-transaction', params: { id: tx.id } })
  }, [])

  const handleDelete = useCallback((tx: Transaction) => {
    Alert.alert(
      'Delete Transaction',
      'Are you sure you want to delete this transaction? This will also update the account balance.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await warningHaptic()
            deleteTransaction(tx.id)
            showToast({ message: 'Transaction deleted', type: 'error' })
          },
        },
      ]
    )
  }, [deleteTransaction])

  if (transactions.length === 0) {
    return (
      <EmptyState
        icon="arrow-left-right"
        title={emptyTitle}
        description={emptyDescription}
        actionLabel="Add Transaction"
        onAction={() => router.push('/modals/add-transaction')}
      />
    )
  }

  const getDateTotal = (txns: Transaction[]): number => {
    return txns.reduce((sum, t) => {
      if (t.type === 'income') return sum + t.amount
      if (t.type === 'expense') return sum - t.amount
      return sum
    }, 0)
  }

  return (
    <SectionList
      sections={sections}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => (
        <TransactionItem
          transaction={item}
          onPress={handlePress}
          onEdit={handleEdit}
          onDelete={handleDelete}
          currency={settings.currency}
        />
      )}
      renderSectionHeader={
        showDateHeaders
          ? ({ section }) => {
              const sectionTotal = getDateTotal(section.data)
              const totalColor = sectionTotal >= 0 ? Colors.income : Colors.expense
              return (
                <View style={styles.sectionHeader}>
                  <Text style={styles.sectionDate}>{formatDate(section.title + 'T00:00:00')}</Text>
                  <Text style={[styles.sectionTotal, { color: totalColor }]}>
                    {sectionTotal >= 0 ? '+' : ''}
                    {formatCurrency(Math.abs(sectionTotal), settings.currency as 'INR')}
                  </Text>
                </View>
              )
            }
          : undefined
      }
      refreshControl={
        onRefresh ? (
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={Colors.primary}
            colors={[Colors.primary]}
          />
        ) : undefined
      }
      stickySectionHeadersEnabled
      showsVerticalScrollIndicator={false}
      contentContainerStyle={transactions.length === 0 ? { flex: 1 } : { paddingBottom: 100 }}
    />
  )
}

const styles = StyleSheet.create({
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
    backgroundColor: Colors.background,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  sectionDate: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 13,
    color: Colors.textMuted,
  },
  sectionTotal: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 13,
  },
})
