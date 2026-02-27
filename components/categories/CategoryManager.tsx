import React, { useState } from 'react'
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Alert,
  StyleSheet,
} from 'react-native'
import { Plus, Pencil, Trash2 } from 'lucide-react-native'
import { useRouter } from 'expo-router'
import { Colors } from '@/constants/Colors'
import { LucideIcon } from '@/components/common/IconPicker'
import { hexToRgba } from '@/utils/formatters'
import { useFinanceStore } from '@/store/useFinanceStore'
import { warningHaptic, successHaptic } from '@/utils/haptics'
import { showToast } from '@/components/common/Toast'
import type { Category } from '@/types'

interface CategoryManagerProps {
  filterType?: 'expense' | 'income' | 'both' | 'all'
}

export function CategoryManager({ filterType = 'all' }: CategoryManagerProps) {
  const router = useRouter()
  const { categories, deleteCategory } = useFinanceStore()

  const filtered = filterType === 'all'
    ? categories
    : categories.filter((c) => c.type === filterType || c.type === 'both')

  const handleDelete = (cat: Category) => {
    Alert.alert(
      'Delete Category',
      `Are you sure you want to delete "${cat.name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await warningHaptic()
            deleteCategory(cat.id)
            showToast({ message: `"${cat.name}" deleted`, type: 'error' })
          },
        },
      ]
    )
  }

  const TYPE_LABELS = { expense: 'Expense', income: 'Income', both: 'Both' }

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.addBtn}
        onPress={() => router.push('/modals/add-category')}
        activeOpacity={0.8}
      >
        <Plus size={18} color={Colors.primary} />
        <Text style={styles.addBtnText}>Add Category</Text>
      </TouchableOpacity>

      <ScrollView contentContainerStyle={styles.list}>
        {filtered.map((cat) => (
          <View key={cat.id} style={styles.item}>
            <View
              style={[
                styles.iconCircle,
                { backgroundColor: hexToRgba(cat.color, 0.2) },
              ]}
            >
              <LucideIcon name={cat.icon} size={20} color={cat.color} />
            </View>

            <View style={styles.info}>
              <Text style={styles.name}>{cat.name}</Text>
              <Text style={styles.type}>{TYPE_LABELS[cat.type]}</Text>
            </View>

            <View style={styles.actions}>
              <TouchableOpacity
                onPress={() => router.push({ pathname: '/modals/add-category', params: { id: cat.id } })}
                hitSlop={8}
                style={styles.actionBtn}
              >
                <Pencil size={16} color={Colors.textMuted} />
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => handleDelete(cat)}
                hitSlop={8}
                style={styles.actionBtn}
              >
                <Trash2 size={16} color={Colors.expense} />
              </TouchableOpacity>
            </View>
          </View>
        ))}

        {filtered.length === 0 && (
          <Text style={styles.empty}>No categories yet.</Text>
        )}
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
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
    borderColor: Colors.primary,
    justifyContent: 'center',
  },
  addBtnText: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 15,
    color: Colors.primary,
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
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: Colors.border,
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
    color: Colors.textPrimary,
  },
  type: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
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
    color: Colors.textMuted,
    textAlign: 'center',
    marginTop: 24,
  },
})
