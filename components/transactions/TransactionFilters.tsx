import React, { useState } from 'react'
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  TextInput,
  StyleSheet,
} from 'react-native'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
} from 'react-native-reanimated'
import { Search, X, ChevronDown } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { BottomSheet } from '@/components/common/BottomSheet'
import { CategoryPicker } from '@/components/categories/CategoryPicker'
import { useFinanceStore } from '@/store/useFinanceStore'
import { lightHaptic } from '@/utils/haptics'
import type { TransactionType } from '@/types'

export interface FilterState {
  type: TransactionType | 'all'
  accountId: string | null
  categoryIds: string[]
  searchQuery: string
}

interface TransactionFiltersProps {
  filters: FilterState
  onChange: (filters: FilterState) => void
}

const TYPE_FILTERS: Array<{ value: FilterState['type']; label: string }> = [
  { value: 'all', label: 'All' },
  { value: 'income', label: 'Income' },
  { value: 'expense', label: 'Expense' },
  { value: 'transfer', label: 'Transfer' },
]

export function TransactionFilters({ filters, onChange }: TransactionFiltersProps) {
  const { accounts } = useFinanceStore()
  const [searchExpanded, setSearchExpanded] = useState(false)
  const [showAccountSheet, setShowAccountSheet] = useState(false)
  const [showCategoryPicker, setShowCategoryPicker] = useState(false)

  const searchWidth = useSharedValue(0)

  const toggleSearch = async () => {
    await lightHaptic()
    if (searchExpanded) {
      searchWidth.value = withTiming(0, { duration: 200 })
      onChange({ ...filters, searchQuery: '' })
      setSearchExpanded(false)
    } else {
      searchWidth.value = withTiming(200, { duration: 250 })
      setSearchExpanded(true)
    }
  }

  const searchStyle = useAnimatedStyle(() => ({
    width: searchWidth.value,
    overflow: 'hidden',
  }))

  const selectedAccount = accounts.find((a) => a.id === filters.accountId)

  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Type filters */}
        {TYPE_FILTERS.map((f) => (
          <TouchableOpacity
            key={f.value}
            onPress={async () => {
              await lightHaptic()
              onChange({ ...filters, type: f.value })
            }}
            style={[
              styles.chip,
              filters.type === f.value && styles.chipActive,
            ]}
            activeOpacity={0.7}
          >
            <Text
              style={[
                styles.chipText,
                filters.type === f.value && styles.chipTextActive,
              ]}
            >
              {f.label}
            </Text>
          </TouchableOpacity>
        ))}

        {/* Account filter */}
        <TouchableOpacity
          onPress={() => setShowAccountSheet(true)}
          style={[styles.chip, filters.accountId && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text
            style={[
              styles.chipText,
              filters.accountId && styles.chipTextActive,
            ]}
          >
            {selectedAccount ? selectedAccount.name : 'Account'}
          </Text>
          <ChevronDown size={12} color={filters.accountId ? Colors.primary : Colors.textMuted} />
          {filters.accountId && (
            <TouchableOpacity
              onPress={() => onChange({ ...filters, accountId: null })}
              hitSlop={4}
            >
              <X size={12} color={Colors.primary} />
            </TouchableOpacity>
          )}
        </TouchableOpacity>

        {/* Category filter */}
        <TouchableOpacity
          onPress={() => setShowCategoryPicker(true)}
          style={[styles.chip, filters.categoryIds.length > 0 && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text
            style={[
              styles.chipText,
              filters.categoryIds.length > 0 && styles.chipTextActive,
            ]}
          >
            {filters.categoryIds.length > 0
              ? `${filters.categoryIds.length} Categories`
              : 'Category'}
          </Text>
          <ChevronDown size={12} color={filters.categoryIds.length > 0 ? Colors.primary : Colors.textMuted} />
        </TouchableOpacity>

        {/* Search */}
        <View style={styles.searchWrapper}>
          <TouchableOpacity onPress={toggleSearch} style={styles.searchBtn} hitSlop={4}>
            {searchExpanded ? (
              <X size={16} color={Colors.primary} />
            ) : (
              <Search size={16} color={Colors.textMuted} />
            )}
          </TouchableOpacity>
          <Animated.View style={[styles.searchInput, searchStyle]}>
            <TextInput
              value={filters.searchQuery}
              onChangeText={(q) => onChange({ ...filters, searchQuery: q })}
              placeholder="Search..."
              placeholderTextColor={Colors.textMuted}
              style={styles.searchText}
              autoFocus={searchExpanded}
              selectionColor={Colors.primary}
            />
          </Animated.View>
        </View>
      </ScrollView>

      {/* Account picker sheet */}
      <BottomSheet
        visible={showAccountSheet}
        onClose={() => setShowAccountSheet(false)}
        title="Filter by Account"
        snapPoint={0.5}
      >
        <ScrollView contentContainerStyle={styles.accountList}>
          <TouchableOpacity
            style={[styles.accountItem, !filters.accountId && styles.accountItemActive]}
            onPress={() => {
              onChange({ ...filters, accountId: null })
              setShowAccountSheet(false)
            }}
          >
            <Text style={[styles.accountItemText, !filters.accountId && { color: Colors.primary }]}>
              All Accounts
            </Text>
          </TouchableOpacity>
          {accounts.map((a) => (
            <TouchableOpacity
              key={a.id}
              style={[styles.accountItem, filters.accountId === a.id && styles.accountItemActive]}
              onPress={() => {
                onChange({ ...filters, accountId: a.id })
                setShowAccountSheet(false)
              }}
            >
              <Text style={[styles.accountItemText, filters.accountId === a.id && { color: Colors.primary }]}>
                {a.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </BottomSheet>

      {/* Category picker — multi-select workaround using existing picker */}
      <CategoryPicker
        visible={showCategoryPicker}
        onClose={() => setShowCategoryPicker(false)}
        selectedId={filters.categoryIds[0] ?? ''}
        onChange={(cat) => {
          const existing = filters.categoryIds.includes(cat.id)
          onChange({
            ...filters,
            categoryIds: existing
              ? filters.categoryIds.filter((id) => id !== cat.id)
              : [...filters.categoryIds, cat.id],
          })
        }}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
    backgroundColor: Colors.background,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 8,
    flexDirection: 'row',
    alignItems: 'center',
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipActive: {
    backgroundColor: Colors.primary + '20',
    borderColor: Colors.primary,
  },
  chipText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  chipTextActive: {
    color: Colors.primary,
  },
  searchWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: 'hidden',
  },
  searchBtn: {
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  searchInput: {
    overflow: 'hidden',
  },
  searchText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 13,
    color: Colors.textPrimary,
    height: 36,
    paddingRight: 12,
  },
  accountList: {
    paddingHorizontal: 20,
    paddingVertical: 8,
  },
  accountItem: {
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  accountItemActive: {
    backgroundColor: 'transparent',
  },
  accountItemText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.textPrimary,
  },
})
