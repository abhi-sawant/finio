import React, { useState, useCallback } from 'react'
import {
  View,
  Text,
  StyleSheet,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { TransactionFilters, FilterState } from '@/components/transactions/TransactionFilters'
import { TransactionList } from '@/components/transactions/TransactionList'
import { useFinanceStore } from '@/store/useFinanceStore'
import { filterTransactions } from '@/store/selectors'
import { lightHaptic } from '@/utils/haptics'

const DEFAULT_FILTERS: FilterState = {
  type: 'all',
  accountId: null,
  categoryIds: [],
  searchQuery: '',
}

export default function TransactionsScreen() {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const { transactions } = useFinanceStore()
  const [refreshing, setRefreshing] = useState(false)
  const [filters, setFilters] = useState<FilterState>(DEFAULT_FILTERS)

  const filtered = filterTransactions(transactions, {
    ...(filters.type !== 'all' && { type: filters.type }),
    ...(filters.accountId && { accountId: filters.accountId }),
    ...(filters.categoryIds.length > 0 && { categoryIds: filters.categoryIds }),
    ...(filters.searchQuery && { searchQuery: filters.searchQuery }),
  })

  const onRefresh = useCallback(async () => {
    setRefreshing(true)
    await lightHaptic()
    setTimeout(() => setRefreshing(false), 500)
  }, [])

  const hasFilters =
    filters.type !== 'all' ||
    !!filters.accountId ||
    filters.categoryIds.length > 0 ||
    !!filters.searchQuery

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>Transactions</Text>
        {hasFilters && (
          <Text style={styles.filterCount}>
            {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          </Text>
        )}
      </View>

      {/* Filters */}
      <TransactionFilters filters={filters} onChange={setFilters} />

      {/* Transaction list */}
      <TransactionList
        transactions={filtered}
        onRefresh={onRefresh}
        refreshing={refreshing}
        emptyTitle={hasFilters ? 'No results' : 'No transactions yet'}
        emptyDescription={
          hasFilters
            ? 'Try adjusting your filters'
            : 'Tap the + button to record your first transaction'
        }
      />
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 24,
    color: colors.textPrimary,
  },
  filterCount: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 13,
    color: colors.textMuted,
  },
})
}
