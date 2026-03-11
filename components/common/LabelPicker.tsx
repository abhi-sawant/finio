import React, { useState } from 'react'
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  TextInput,
} from 'react-native'
import { Check, Plus, X } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { LabelColors } from '@/constants/Colors'
import { BottomSheet } from './BottomSheet'
import { useFinanceStore } from '@/store/useFinanceStore'
import { lightHaptic } from '@/utils/haptics'
import { showToast } from '@/components/common/Toast'

interface LabelPickerProps {
  selectedIds: string[]
  onChange: (ids: string[]) => void
  visible: boolean
  onClose: () => void
}

export function LabelPicker({ selectedIds, onChange, visible, onClose }: LabelPickerProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const labels = useFinanceStore((s) => s.labels)
  const addLabel = useFinanceStore((s) => s.addLabel)

  const [adding, setAdding] = useState(false)
  const [newName, setNewName] = useState('')
  const [newColor, setNewColor] = useState(LabelColors[0] as string)

  const toggle = async (id: string) => {
    await lightHaptic()
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((l) => l !== id))
    } else {
      onChange([...selectedIds, id])
    }
  }

  const handleAdd = async () => {
    if (!newName.trim()) {
      showToast({ message: 'Enter a label name', type: 'error' })
      return
    }
    await lightHaptic()
    addLabel({ name: newName.trim(), color: newColor })
    showToast({ message: 'Label added', type: 'success' })
    setNewName('')
    setNewColor(LabelColors[0] as string)
    setAdding(false)
  }

  return (
    <BottomSheet visible={visible} onClose={onClose} title="Select Labels">
      <ScrollView contentContainerStyle={styles.list} keyboardShouldPersistTaps="handled">
        {labels.map((label) => {
          const isSelected = selectedIds.includes(label.id)
          return (
            <TouchableOpacity
              key={label.id}
              onPress={() => toggle(label.id)}
              style={styles.item}
              activeOpacity={0.7}
            >
              <View style={[styles.dot, { backgroundColor: label.color }]} />
              <Text style={styles.labelName}>{label.name}</Text>
              {isSelected && (
                <Check size={18} color={colors.primary} strokeWidth={2.5} />
              )}
            </TouchableOpacity>
          )
        })}
        {labels.length === 0 && !adding && (
          <Text style={styles.empty}>No labels yet. Add one below.</Text>
        )}

        {/* Inline add form */}
        {adding ? (
          <View style={styles.addForm}>
            <View style={styles.addFormRow}>
              <TextInput
                style={styles.addInput}
                value={newName}
                onChangeText={setNewName}
                placeholder="Label name..."
                placeholderTextColor={colors.textMuted}
                autoFocus
                returnKeyType="done"
                onSubmitEditing={handleAdd}
              />
              <TouchableOpacity onPress={handleAdd} style={styles.addConfirm} activeOpacity={0.8}>
                <Check size={16} color="#fff" strokeWidth={2.5} />
              </TouchableOpacity>
              <TouchableOpacity onPress={() => setAdding(false)} style={styles.addCancel} activeOpacity={0.8}>
                <X size={16} color={colors.textMuted} strokeWidth={2} />
              </TouchableOpacity>
            </View>
            <View style={styles.colorRow}>
              {(LabelColors as readonly string[]).map((c) => (
                <TouchableOpacity
                  key={c}
                  style={[styles.colorSwatch, { backgroundColor: c }, newColor === c && styles.colorSwatchSelected]}
                  onPress={() => setNewColor(c)}
                  activeOpacity={0.8}
                />
              ))}
            </View>
          </View>
        ) : (
          <TouchableOpacity
            style={styles.addNew}
            onPress={async () => { await lightHaptic(); setAdding(true) }}
            activeOpacity={0.7}
          >
            <Plus size={16} color={colors.primary} />
            <Text style={styles.addNewText}>New Label</Text>
          </TouchableOpacity>
        )}
      </ScrollView>
    </BottomSheet>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  list: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    gap: 4,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  dot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  labelName: {
    flex: 1,
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: colors.textPrimary,
  },
  empty: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: 24,
    marginBottom: 8,
  },
  addNew: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 14,
    marginTop: 4,
  },
  addNewText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: colors.primary,
  },
  addForm: {
    marginTop: 12,
    gap: 10,
  },
  addFormRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  addInput: {
    flex: 1,
    height: 42,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: 12,
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textPrimary,
    backgroundColor: colors.surfaceElevated,
  },
  addConfirm: {
    width: 42,
    height: 42,
    borderRadius: 10,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addCancel: {
    width: 42,
    height: 42,
    borderRadius: 10,
    backgroundColor: colors.surfaceElevated,
    alignItems: 'center',
    justifyContent: 'center',
  },
  colorRow: {
    flexDirection: 'row',
    gap: 10,
    paddingVertical: 4,
  },
  colorSwatch: {
    width: 28,
    height: 28,
    borderRadius: 14,
  },
  colorSwatchSelected: {
    borderWidth: 2.5,
    borderColor: colors.white,
    shadowColor: colors.shadowColor,
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.3,
    shadowRadius: 2,
    elevation: 3,
  },
})
}
