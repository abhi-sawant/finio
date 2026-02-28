import React from 'react'
import { View, Text, TouchableOpacity, ScrollView, StyleSheet } from 'react-native'
import * as LucideIcons from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { BottomSheet } from './BottomSheet'
import { lightHaptic } from '@/utils/haptics'

export const AVAILABLE_ICONS = [
  'landmark', 'piggy-bank', 'wallet', 'credit-card', 'banknote', 'coins',
  'briefcase', 'laptop', 'trending-up', 'building-2', 'home', 'car',
  'utensils', 'shopping-bag', 'film', 'music', 'heart-pulse', 'book-open',
  'zap', 'droplets', 'wifi', 'phone', 'gift', 'star',
  'coffee', 'pizza', 'shirt', 'plane', 'train', 'bus',
  'dumbbell', 'gamepad-2', 'scissors', 'wrench', 'package', 'truck',
  'repeat', 'circle-ellipsis', 'tag', 'folder', 'bookmark', 'bell',
] as const

export type IconName = (typeof AVAILABLE_ICONS)[number]

function toPascalCase(str: string): string {
  return str
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')
}

export function LucideIcon({
  name,
  size = 20,
  color = colors.textPrimary,
  strokeWidth = 2,
}: {
  name: string
  size?: number
  color?: string
  strokeWidth?: number
}) {
  const iconName = toPascalCase(name) as keyof typeof LucideIcons
  const IconComponent = LucideIcons[iconName] as React.ComponentType<{
    size: number
    color: string
    strokeWidth: number
  }> | undefined

  if (!IconComponent) {
    const Fallback = LucideIcons.Circle as React.ComponentType<{
      size: number
      color: string
      strokeWidth: number
    }>
    return <Fallback size={size} color={color} strokeWidth={strokeWidth} />
  }

  return <IconComponent size={size} color={color} strokeWidth={strokeWidth} />
}

interface IconPickerProps {
  selectedIcon: string
  onChange: (icon: string) => void
  accentColor?: string
  visible: boolean
  onClose: () => void
}

export function IconPicker({
  selectedIcon,
  onChange,
  accentColor = colors.primary,
  visible,
  onClose,
}: IconPickerProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const handleSelect = async (icon: string) => {
    await lightHaptic()
    onChange(icon)
    onClose()
  }

  return (
    <BottomSheet visible={visible} onClose={onClose} title="Select Icon" snapPoint={0.75}>
      <ScrollView contentContainerStyle={styles.grid}>
        {AVAILABLE_ICONS.map((icon) => {
          const isSelected = icon === selectedIcon
          return (
            <TouchableOpacity
              key={icon}
              onPress={() => handleSelect(icon)}
              style={[
                styles.iconBtn,
                isSelected && { backgroundColor: accentColor + '33', borderColor: accentColor },
              ]}
              activeOpacity={0.7}
            >
              <LucideIcon
                name={icon}
                size={22}
                color={isSelected ? accentColor : colors.textPrimary}
              />
            </TouchableOpacity>
          )
        })}
      </ScrollView>
    </BottomSheet>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: 16,
    gap: 10,
    justifyContent: 'center',
  },
  iconBtn: {
    width: 52,
    height: 52,
    borderRadius: 14,
    backgroundColor: colors.surfaceElevated,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
})
}
