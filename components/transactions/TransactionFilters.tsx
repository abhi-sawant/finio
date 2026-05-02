import React, { useState, useMemo, useEffect } from 'react';
import { View, Text, TouchableOpacity, ScrollView, TextInput, StyleSheet } from 'react-native';
import Animated, { useSharedValue, useAnimatedStyle, withTiming } from 'react-native-reanimated';
import { Search, X, ChevronDown, Check, FilterX } from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { BottomSheet } from '@/components/common/BottomSheet';
import { LucideIcon } from '@/components/common/IconPicker';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useDebounce } from '@/hooks/useDebounce';
import { lightHaptic } from '@/utils/haptics';
import { hexToRgba } from '@/utils/formatters';
import type { TransactionType } from '@/types';

export interface FilterState {
  typeIds: TransactionType[];
  accountId: string | null;
  categoryIds: string[];
  labelIds: string[];
  searchQuery: string;
}

interface TransactionFiltersProps {
  filters: FilterState;
  onChange: (filters: FilterState) => void;
}

const TYPE_FILTERS: Array<{ value: TransactionType; label: string }> = [
  { value: 'income', label: 'Income' },
  { value: 'expense', label: 'Expense' },
  { value: 'transfer', label: 'Transfer' },
];

export function TransactionFilters({ filters, onChange }: TransactionFiltersProps) {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const accounts = useFinanceStore((s) => s.accounts);
  const categories = useFinanceStore((s) => s.categories);
  const labels = useFinanceStore((s) => s.labels);
  const [searchExpanded, setSearchExpanded] = useState(false);
  const [showTypeSheet, setShowTypeSheet] = useState(false);
  const [showAccountSheet, setShowAccountSheet] = useState(false);
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);
  const [showLabelPicker, setShowLabelPicker] = useState(false);

  // Local search state so keystrokes don't trigger filterTransactions on every character
  const [localQuery, setLocalQuery] = useState(filters.searchQuery);
  const debouncedQuery = useDebounce(localQuery, 300);

  // Propagate to parent only when the debounced value settles
  useEffect(() => {
    if (debouncedQuery !== filters.searchQuery) {
      onChange({ ...filters, searchQuery: debouncedQuery });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery]);

  // Keep local query in sync if parent clears the filter externally (e.g. "Clear all")
  useEffect(() => {
    if (filters.searchQuery !== localQuery && filters.searchQuery === '') {
      setLocalQuery('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.searchQuery]);

  const searchWidth = useSharedValue(0);

  const selectedAccount = useMemo(
    () => accounts.find((a) => a.id === filters.accountId),
    [accounts, filters.accountId],
  );

  const toggleSearch = async () => {
    await lightHaptic();
    if (searchExpanded) {
      searchWidth.value = withTiming(0, { duration: 200 });
      setLocalQuery('');
      onChange({ ...filters, searchQuery: '' });
      setSearchExpanded(false);
    } else {
      searchWidth.value = withTiming(200, { duration: 250 });
      setSearchExpanded(true);
    }
  };

  const searchStyle = useAnimatedStyle(() => ({
    width: searchWidth.value,
    overflow: 'hidden',
  }));

  const typeIds = filters.typeIds ?? [];
  const categoryIds = filters.categoryIds ?? [];
  const labelIds = filters.labelIds ?? [];
  const hasActiveFilters =
    typeIds.length > 0 ||
    !!filters.accountId ||
    categoryIds.length > 0 ||
    labelIds.length > 0 ||
    !!filters.searchQuery?.trim();

  const selectedTypeLabel =
    typeIds.length > 0
      ? typeIds.length === 1
        ? TYPE_FILTERS.find((f) => f.value === typeIds[0])?.label || 'Type'
        : `${typeIds.length} Types`
      : 'Type';

  const clearAllFilters = async () => {
    await lightHaptic();
    setLocalQuery('');
    onChange({
      typeIds: [],
      accountId: null,
      categoryIds: [],
      labelIds: [],
      searchQuery: '',
    });
    if (searchExpanded) {
      searchWidth.value = withTiming(0, { duration: 200 });
      setSearchExpanded(false);
    }
  };

  return (
    <View style={styles.container}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Type filter */}
        <TouchableOpacity
          onPress={() => setShowTypeSheet(true)}
          style={[styles.chip, typeIds.length > 0 && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text style={[styles.chipText, typeIds.length > 0 && styles.chipTextActive]}>
            {selectedTypeLabel}
          </Text>
          <ChevronDown size={12} color={typeIds.length > 0 ? colors.primary : colors.textMuted} />
        </TouchableOpacity>

        {/* Account filter */}
        <TouchableOpacity
          onPress={() => setShowAccountSheet(true)}
          style={[styles.chip, filters.accountId && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text style={[styles.chipText, filters.accountId && styles.chipTextActive]}>
            {selectedAccount ? selectedAccount.name : 'Account'}
          </Text>
          <ChevronDown size={12} color={filters.accountId ? colors.primary : colors.textMuted} />
        </TouchableOpacity>

        {/* Category filter */}
        <TouchableOpacity
          onPress={() => setShowCategoryPicker(true)}
          style={[styles.chip, categoryIds.length > 0 && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text style={[styles.chipText, categoryIds.length > 0 && styles.chipTextActive]}>
            {categoryIds.length > 0 ? `${categoryIds.length} Categories` : 'Category'}
          </Text>
          <ChevronDown
            size={12}
            color={categoryIds.length > 0 ? colors.primary : colors.textMuted}
          />
        </TouchableOpacity>

        {/* Label filter */}
        <TouchableOpacity
          onPress={() => setShowLabelPicker(true)}
          style={[styles.chip, labelIds.length > 0 && styles.chipActive]}
          activeOpacity={0.7}
        >
          <Text style={[styles.chipText, labelIds.length > 0 && styles.chipTextActive]}>
            {labelIds.length > 0 ? `${labelIds.length} Labels` : 'Label'}
          </Text>
          <ChevronDown size={12} color={labelIds.length > 0 ? colors.primary : colors.textMuted} />
        </TouchableOpacity>

        {/* Clear filters */}
        {hasActiveFilters && (
          <TouchableOpacity onPress={clearAllFilters} style={styles.clearBtn} hitSlop={4}>
            <FilterX size={16} color={colors.error} />
          </TouchableOpacity>
        )}

        {/* Search */}
        <View style={styles.searchWrapper}>
          <TouchableOpacity onPress={toggleSearch} style={styles.searchBtn} hitSlop={4}>
            {searchExpanded ? (
              <X size={16} color={colors.primary} />
            ) : (
              <Search size={16} color={colors.textMuted} />
            )}
          </TouchableOpacity>
          <Animated.View style={[styles.searchInput, searchStyle]}>
            <TextInput
              value={localQuery}
              onChangeText={setLocalQuery}
              placeholder="Search..."
              placeholderTextColor={colors.textMuted}
              style={styles.searchText}
              autoFocus={searchExpanded}
              selectionColor={colors.primary}
            />
          </Animated.View>
        </View>
      </ScrollView>

      {/* Type picker sheet */}
      <BottomSheet
        visible={showTypeSheet}
        onClose={() => setShowTypeSheet(false)}
        title="Filter by Type"
        snapPoint={0.4}
      >
        <ScrollView contentContainerStyle={styles.accountList}>
          {TYPE_FILTERS.map((f) => {
            const isSelected = typeIds.includes(f.value);
            return (
              <TouchableOpacity
                key={f.value}
                style={[styles.accountItem, isSelected && styles.accountItemActive]}
                onPress={async () => {
                  await lightHaptic();
                  const currentTypeIds = filters.typeIds ?? [];
                  onChange({
                    ...filters,
                    typeIds: isSelected
                      ? currentTypeIds.filter((id) => id !== f.value)
                      : [...currentTypeIds, f.value],
                  });
                }}
              >
                <Text style={[styles.accountItemText, isSelected && { color: colors.primary }]}>
                  {f.label}
                </Text>
                {isSelected && <Check size={18} color={colors.primary} />}
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </BottomSheet>

      {/* Account picker sheet */}
      <BottomSheet
        visible={showAccountSheet}
        onClose={() => setShowAccountSheet(false)}
        title="Filter by Account"
        snapPoint={0.5}
      >
        <ScrollView contentContainerStyle={styles.accountList}>
          {accounts.map((a) => {
            const isSelected = filters.accountId === a.id;
            return (
              <TouchableOpacity
                key={a.id}
                style={[styles.accountItem, isSelected && styles.accountItemActive]}
                onPress={async () => {
                  await lightHaptic();
                  onChange({ ...filters, accountId: a.id });
                  setShowAccountSheet(false);
                }}
              >
                <Text style={[styles.accountItemText, isSelected && { color: colors.primary }]}>
                  {a.name}
                </Text>
                {isSelected && <Check size={18} color={colors.primary} />}
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </BottomSheet>

      {/* Category picker — multi-select */}
      <BottomSheet
        visible={showCategoryPicker}
        onClose={() => setShowCategoryPicker(false)}
        title="Filter by Category"
        snapPoint={0.6}
      >
        <ScrollView contentContainerStyle={styles.categoryList}>
          {categories.map((cat) => {
            const isSelected = categoryIds.includes(cat.id);
            return (
              <TouchableOpacity
                key={cat.id}
                style={[styles.categoryItem, isSelected && styles.categoryItemActive]}
                onPress={async () => {
                  await lightHaptic();
                  const currentCategoryIds = filters.categoryIds ?? [];
                  onChange({
                    ...filters,
                    categoryIds: isSelected
                      ? currentCategoryIds.filter((id) => id !== cat.id)
                      : [...currentCategoryIds, cat.id],
                  });
                }}
              >
                <View style={styles.categoryItemLeft}>
                  <View
                    style={[styles.categoryIcon, { backgroundColor: hexToRgba(cat.color, 0.2) }]}
                  >
                    <LucideIcon name={cat.icon} size={18} color={cat.color} />
                  </View>
                  <Text style={[styles.categoryItemText, isSelected && { color: colors.primary }]}>
                    {cat.name}
                  </Text>
                </View>
                {isSelected && <Check size={18} color={colors.primary} />}
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </BottomSheet>

      {/* Label picker — multi-select */}
      <BottomSheet
        visible={showLabelPicker}
        onClose={() => setShowLabelPicker(false)}
        title="Filter by Label"
        snapPoint={0.5}
      >
        <ScrollView contentContainerStyle={styles.categoryList}>
          {labels.map((label) => {
            const isSelected = labelIds.includes(label.id);
            return (
              <TouchableOpacity
                key={label.id}
                style={[styles.categoryItem, isSelected && styles.categoryItemActive]}
                onPress={async () => {
                  await lightHaptic();
                  const currentLabelIds = filters.labelIds ?? [];
                  onChange({
                    ...filters,
                    labelIds: isSelected
                      ? currentLabelIds.filter((id) => id !== label.id)
                      : [...currentLabelIds, label.id],
                  });
                }}
              >
                <View style={styles.categoryItemLeft}>
                  <View
                    style={[styles.categoryIcon, { backgroundColor: hexToRgba(label.color, 0.2) }]}
                  >
                    <View style={[styles.labelDot, { backgroundColor: label.color }]} />
                  </View>
                  <Text style={[styles.categoryItemText, isSelected && { color: colors.primary }]}>
                    {label.name}
                  </Text>
                </View>
                {isSelected && <Check size={18} color={colors.primary} />}
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </BottomSheet>
    </View>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
      backgroundColor: colors.background,
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
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
    },
    chipActive: {
      backgroundColor: colors.primary + '20',
      borderColor: colors.primary,
    },
    chipText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 13,
      color: colors.textMuted,
    },
    chipTextActive: {
      color: colors.primary,
    },
    clearBtn: {
      paddingHorizontal: 12,
      paddingVertical: 8,
      backgroundColor: colors.surface,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.border,
    },
    searchWrapper: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: colors.surface,
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.border,
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
      color: colors.textPrimary,
      height: 36,
      paddingRight: 12,
    },
    accountList: {
      paddingHorizontal: 20,
      paddingVertical: 8,
    },
    accountItem: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingVertical: 14,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    accountItemActive: {
      backgroundColor: 'transparent',
    },
    accountItemText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 15,
      color: colors.textPrimary,
    },
    categoryList: {
      paddingHorizontal: 20,
      paddingVertical: 8,
    },
    categoryItem: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      paddingVertical: 12,
      borderBottomWidth: 1,
      borderBottomColor: colors.border,
    },
    categoryItemActive: {
      backgroundColor: 'transparent',
    },
    categoryItemLeft: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      flex: 1,
    },
    categoryIcon: {
      width: 36,
      height: 36,
      borderRadius: 18,
      alignItems: 'center',
      justifyContent: 'center',
    },
    categoryItemText: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 15,
      color: colors.textPrimary,
    },
    labelDot: {
      width: 12,
      height: 12,
      borderRadius: 6,
    },
  });
}
