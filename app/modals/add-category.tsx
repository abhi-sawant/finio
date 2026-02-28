import React, { useState } from 'react'
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native'
import { useRouter, useLocalSearchParams } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X, Check } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { ColorPicker } from '@/components/common/ColorPicker'
import { IconPicker } from '@/components/common/IconPicker'
import { CategoryBadge } from '@/components/categories/CategoryBadge'
import { useFinanceStore } from '@/store/useFinanceStore'
import { showToast } from '@/components/common/Toast'
import { successHaptic, errorHaptic, lightHaptic } from '@/utils/haptics'
import type { CategoryType } from '@/types'

const CATEGORY_TYPES: { key: CategoryType; label: string }[] = [
  { key: 'expense', label: 'Expense' },
  { key: 'income', label: 'Income' },
  { key: 'both', label: 'Both' },
]

export default function AddCategoryModal() {
  const colors = useColors()
  const styles = makeStyles(colors)
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { id } = useLocalSearchParams<{ id?: string }>()
  const { categories, addCategory, updateCategory, deleteCategory } = useFinanceStore()

  const existing = id ? categories.find((c) => c.id === id) : undefined
  const isEdit = !!existing

  const [name, setName] = useState(existing?.name ?? '')
  const [icon, setIcon] = useState(existing?.icon ?? 'tag')
  const [color, setColor] = useState(existing?.color ?? colors.primary)
  const [type, setType] = useState<CategoryType>(existing?.type ?? 'expense')
  const [iconPickerVisible, setIconPickerVisible] = useState(false)

  const handleSave = () => {
    if (!name.trim()) {
      errorHaptic()
      showToast({ message: 'Enter a category name', type: 'error' })
      return
    }

    const payload = { name: name.trim(), icon, color, type }

    if (isEdit && existing) {
      updateCategory(existing.id, payload)
      showToast({ message: 'Category updated', type: 'success' })
    } else {
      addCategory(payload)
      showToast({ message: 'Category added', type: 'success' })
    }
    successHaptic()
    router.back()
  }

  const handleDelete = () => {
    if (!existing) return
    Alert.alert(
      'Delete Category',
      `Delete "${existing.name}"? Transactions using this category will not be affected.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            deleteCategory(existing.id)
            showToast({ message: 'Category deleted', type: 'success' })
            router.back()
          },
        },
      ]
    )
  }

  // Preview category object
  const preview = { id: 'preview', name: name || 'Category', icon, color, type }

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 16 }]}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={colors.textMuted} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{isEdit ? 'Edit Category' : 'New Category'}</Text>
        <TouchableOpacity onPress={handleSave} hitSlop={8}>
          <Check size={22} color={colors.primary} />
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scroll}
        >
          {/* Preview */}
          <View style={styles.previewRow}>
            <CategoryBadge category={preview} size="lg" />
            <Text style={styles.previewName}>{name || 'Category name'}</Text>
          </View>

          {/* Name */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Name</Text>
            <TextInput
              style={styles.textInput}
              value={name}
              onChangeText={setName}
              placeholder="e.g. Groceries"
              placeholderTextColor={colors.textMuted}
              maxLength={40}
              autoFocus={!isEdit}
            />
          </View>

          {/* Type */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Type</Text>
            <View style={styles.typeRow}>
              {CATEGORY_TYPES.map((t) => (
                <TouchableOpacity
                  key={t.key}
                  onPress={() => {
                    lightHaptic()
                    setType(t.key)
                  }}
                  style={[
                    styles.typeChip,
                    type === t.key && styles.typeChipActive,
                  ]}
                >
                  <Text
                    style={[
                      styles.typeChipLabel,
                      type === t.key && styles.typeChipLabelActive,
                    ]}
                  >
                    {t.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* Color */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Color</Text>
            <ColorPicker selectedColor={color} onChange={setColor} />
          </View>

          {/* Icon */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Icon</Text>
            <TouchableOpacity
              style={styles.iconSelectBtn}
              onPress={() => setIconPickerVisible(true)}
            >
              <Text style={styles.iconSelectLabel}>Selected: {icon}</Text>
            </TouchableOpacity>
            <IconPicker
              selectedIcon={icon}
              onChange={setIcon}
              visible={iconPickerVisible}
              onClose={() => setIconPickerVisible(false)}
            />
          </View>

          {/* Delete (edit mode) */}
          {isEdit && (
            <TouchableOpacity style={styles.deleteBtn} onPress={handleDelete}>
              <Text style={styles.deleteBtnLabel}>Delete Category</Text>
            </TouchableOpacity>
          )}

          <View style={{ height: 40 }} />
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Save button */}
      <TouchableOpacity
        style={[styles.saveBtn, { marginBottom: insets.bottom || 16 }]}
        onPress={handleSave}
      >
        <Text style={styles.saveBtnLabel}>
          {isEdit ? 'Update Category' : 'Add Category'}
        </Text>
      </TouchableOpacity>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  flex: { flex: 1 },
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  headerTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 17,
    color: colors.textPrimary,
  },
  scroll: {
    padding: 16,
    gap: 16,
  },
  previewRow: {
    alignItems: 'center',
    gap: 12,
    paddingVertical: 20,
  },
  previewName: {
    fontFamily: 'Sora_700Bold',
    fontSize: 20,
    color: colors.textPrimary,
  },
  field: {
    gap: 8,
  },
  fieldLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: colors.textMuted,
  },
  textInput: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 14,
    fontFamily: 'DMSans_400Regular',
    fontSize: 16,
    color: colors.textPrimary,
  },
  typeRow: {
    flexDirection: 'row',
    gap: 8,
  },
  typeChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
    alignItems: 'center',
  },
  typeChipActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  typeChipLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: colors.textMuted,
  },
  typeChipLabelActive: {
    color: '#fff',
  },
  deleteBtn: {
    marginTop: 8,
    paddingVertical: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: `${colors.expense}44`,
    backgroundColor: `${colors.expense}11`,
    alignItems: 'center',
  },
  deleteBtnLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: colors.expense,
  },
  iconSelectBtn: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  iconSelectLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textPrimary,
  },
  saveBtn: {
    marginHorizontal: 16,
    backgroundColor: colors.primary,
    borderRadius: 16,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  saveBtnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: '#fff',
  },
})
}
