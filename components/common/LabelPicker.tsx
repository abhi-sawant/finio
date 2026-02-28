import React from 'react'
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
} from 'react-native'
import { Check } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { BottomSheet } from './BottomSheet'
import { useFinanceStore } from '@/store/useFinanceStore'
import { lightHaptic } from '@/utils/haptics'

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

  const toggle = async (id: string) => {
    await lightHaptic()
    if (selectedIds.includes(id)) {
      onChange(selectedIds.filter((l) => l !== id))
    } else {
      onChange([...selectedIds, id])
    }
  }

  return (
    <BottomSheet visible={visible} onClose={onClose} title="Select Labels" snapPoint={0.5}>
      <ScrollView contentContainerStyle={styles.list}>
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
        {labels.length === 0 && (
          <Text style={styles.empty}>No labels yet. Add some in Settings.</Text>
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
  },
})
}
