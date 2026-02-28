import React from 'react'
import { View, TouchableOpacity, StyleSheet, Text } from 'react-native'
import { Check } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { lightHaptic } from '@/utils/haptics'

const DEFAULT_COLORS = [
  '#ef4444', '#f97316', '#f59e0b', '#22c55e',
  '#10b981', '#06b6d4', '#3b82f6', '#6C63FF',
  '#8b5cf6', '#ec4899', '#a855f7', '#64748b',
]

interface ColorPickerProps {
  selectedColor: string
  onChange: (color: string) => void
  colorOptions?: string[]
  label?: string
}

export function ColorPicker({
  selectedColor,
  onChange,
  colorOptions = DEFAULT_COLORS,
  label = 'Color',
}: ColorPickerProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const handleSelect = async (color: string) => {
    await lightHaptic()
    onChange(color)
  }

  return (
    <View style={styles.container}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View style={styles.grid}>
        {colorOptions.map((color) => {
          const isSelected = color === selectedColor
          return (
            <TouchableOpacity
              key={color}
              onPress={() => handleSelect(color)}
              style={[
                styles.swatch,
                { backgroundColor: color },
                isSelected && styles.selectedSwatch,
              ]}
              activeOpacity={0.8}
            >
              {isSelected && (
                <Check size={16} color="#fff" strokeWidth={3} />
              )}
            </TouchableOpacity>
          )
        })}
      </View>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    flex: 1,
    gap: 12,
  },
  label: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: colors.textMuted,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  swatch: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  selectedSwatch: {
    borderWidth: 3,
    borderColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 4,
  },
})
}
