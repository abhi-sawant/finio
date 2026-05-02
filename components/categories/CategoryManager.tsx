import React, { useCallback, useMemo } from 'react';
import { View, Text, TouchableOpacity, FlatList, Alert, StyleSheet } from 'react-native';
import { Plus, Pencil, Trash2 } from 'lucide-react-native';
import { useRouter } from 'expo-router';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { LucideIcon } from '@/components/common/IconPicker';
import { hexToRgba } from '@/utils/formatters';
import { useFinanceStore } from '@/store/useFinanceStore';
import { warningHaptic } from '@/utils/haptics';
import { showToast } from '@/components/common/Toast';
import type { Category } from '@/types';

interface CategoryManagerProps {
  filterType?: 'expense' | 'income' | 'both' | 'all';
}

const TYPE_LABELS = { expense: 'Expense', income: 'Income', both: 'Both' } as const;
const keyExtractor = (item: Category) => item.id;

export function CategoryManager({ filterType = 'all' }: CategoryManagerProps) {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const router = useRouter();
  const { categories, deleteCategory } = useFinanceStore();

  const filtered = useMemo(
    () =>
      filterType === 'all'
        ? categories
        : categories.filter((c) => c.type === filterType || c.type === 'both'),
    [categories, filterType],
  );

  const handleDelete = useCallback(
    (cat: Category) => {
      Alert.alert('Delete Category', `Are you sure you want to delete "${cat.name}"?`, [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await warningHaptic();
            deleteCategory(cat.id);
            showToast({ message: `"${cat.name}" deleted`, type: 'error' });
          },
        },
      ]);
    },
    [deleteCategory],
  );

  const handleEdit = useCallback(
    (id: string) => {
      router.push({ pathname: '/modals/add-category', params: { id } });
    },
    [router],
  );

  const renderItem = useCallback(
    ({ item }: { item: Category }) => (
      <View style={styles.item}>
        <View style={[styles.iconCircle, { backgroundColor: hexToRgba(item.color, 0.2) }]}>
          <LucideIcon name={item.icon} size={20} color={item.color} />
        </View>
        <View style={styles.info}>
          <Text style={styles.name}>{item.name}</Text>
          <Text style={styles.type}>{TYPE_LABELS[item.type]}</Text>
        </View>
        <View style={styles.actions}>
          <TouchableOpacity onPress={() => handleEdit(item.id)} hitSlop={8} style={styles.actionBtn}>
            <Pencil size={16} color={colors.textMuted} />
          </TouchableOpacity>
          <TouchableOpacity onPress={() => handleDelete(item)} hitSlop={8} style={styles.actionBtn}>
            <Trash2 size={16} color={colors.expense} />
          </TouchableOpacity>
        </View>
      </View>
    ),
    [styles, colors.textMuted, colors.expense, handleEdit, handleDelete],
  );

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.addBtn}
        onPress={() => router.push('/modals/add-category')}
        activeOpacity={0.8}
      >
        <Plus size={18} color={colors.primary} />
        <Text style={styles.addBtnText}>Add Category</Text>
      </TouchableOpacity>

      <FlatList
        data={filtered}
        keyExtractor={keyExtractor}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<Text style={styles.empty}>No categories yet.</Text>}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      flex: 1,
    },
    addBtn: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
      margin: 16,
      padding: 14,
      borderRadius: 14,
      borderWidth: 1.5,
      borderColor: colors.primary,
      justifyContent: 'center',
    },
    addBtnText: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 15,
      color: colors.primary,
    },
    list: {
      paddingHorizontal: 16,
      gap: 8,
      paddingBottom: 60,
    },
    item: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      backgroundColor: colors.surface,
      borderRadius: 14,
      padding: 14,
      borderWidth: 1,
      borderColor: colors.border,
    },
    iconCircle: {
      width: 40,
      height: 40,
      borderRadius: 20,
      alignItems: 'center',
      justifyContent: 'center',
    },
    info: {
      flex: 1,
    },
    name: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 15,
      color: colors.textPrimary,
    },
    type: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 12,
      color: colors.textMuted,
      marginTop: 2,
    },
    actions: {
      flexDirection: 'row',
      gap: 8,
    },
    actionBtn: {
      padding: 6,
    },
    empty: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 14,
      color: colors.textMuted,
      textAlign: 'center',
      marginTop: 24,
    },
  });
}
