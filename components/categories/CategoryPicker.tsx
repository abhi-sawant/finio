import React from 'react'
import { View, Text, TouchableOpacity, ScrollView, StyleSheet } from 'react-native'
import { Check } from 'lucide-react-native'
import { BottomSheet } from '@/components/common/BottomSheet'
import { LucideIcon } from '@/components/common/IconPicker'
import { Colors } from '@/constants/Colors'
import { hexToRgba } from '@/utils/formatters'
import { lightHaptic } from '@/utils/haptics'
import { useFinanceStore } from '@/store/useFinanceStore'
import type { Category, CategoryType, TransactionType } from '@/types'

interface CategoryPickerProps {
  visible: boolean
  onClose: () => void
  selectedId: string
  onChange: (category: Category) => void
  transactionType?: TransactionType
}

export function CategoryPicker({
  visible,
  onClose,
  selectedId,
  onChange,
  transactionType,
}: CategoryPickerProps) {
  const categories = useFinanceStore((s) => s.categories)

  const filtered = categories.filter((c) => {
    if (!transactionType) return true
    if (transactionType === 'transfer') return false
    if (transactionType === 'income') return c.type === 'income' || c.type === 'both'
    if (transactionType === 'expense') return c.type === 'expense' || c.type === 'both'
    return true
  })

  const handleSelect = async (cat: Category) => {
    await lightHaptic()
    onChange(cat)
    onClose()
  }

  return (
    <BottomSheet visible={visible} onClose={onClose} title="Select Category" snapPoint={0.7}>
      <ScrollView contentContainerStyle={styles.list}>
        {filtered.map((cat) => {
          const isSelected = cat.id === selectedId
          return (
            <TouchableOpacity
              key={cat.id}
              style={[styles.item, isSelected && styles.itemSelected]}
              onPress={() => handleSelect(cat)}
              activeOpacity={0.7}
            >
              <View
                style={[
                  styles.iconCircle,
                  { backgroundColor: hexToRgba(cat.color, 0.2) },
                ]}
              >
                <LucideIcon name={cat.icon} size={20} color={cat.color} />
              </View>
              <Text style={[styles.name, isSelected && { color: Colors.primary }]}>
                {cat.name}
              </Text>
              {isSelected && <Check size={18} color={Colors.primary} strokeWidth={2.5} />}
            </TouchableOpacity>
          )
        })}
      </ScrollView>
    </BottomSheet>
  )
}

const styles = StyleSheet.create({
  list: {
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 8,
    paddingHorizontal: 0,
    borderRadius: 14,
    marginVertical: 2,
  },
  itemSelected: {
    backgroundColor: Colors.primary + '14',
  },
  iconCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  name: {
    flex: 1,
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.textPrimary,
  },
})
