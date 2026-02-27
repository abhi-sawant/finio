import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '@/constants/Colors'
import { LucideIcon } from '@/components/common/IconPicker'
import { hexToRgba } from '@/utils/formatters'
import type { Category } from '@/types'

interface CategoryBadgeProps {
  category: Category
  size?: 'sm' | 'md' | 'lg'
  showLabel?: boolean
}

export function CategoryBadge({ category, size = 'md', showLabel = true }: CategoryBadgeProps) {
  const iconSize = size === 'sm' ? 14 : size === 'md' ? 18 : 24
  const circleSize = size === 'sm' ? 28 : size === 'md' ? 36 : 48
  const fontSize = size === 'sm' ? 11 : size === 'md' ? 13 : 15

  return (
    <View style={styles.container}>
      <View
        style={[
          styles.circle,
          {
            width: circleSize,
            height: circleSize,
            borderRadius: circleSize / 2,
            backgroundColor: hexToRgba(category.color, 0.2),
          },
        ]}
      >
        <LucideIcon name={category.icon} size={iconSize} color={category.color} />
      </View>
      {showLabel && (
        <Text style={[styles.label, { fontSize }]} numberOfLines={1}>
          {category.name}
        </Text>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    gap: 4,
  },
  circle: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontFamily: 'DMSans_400Regular',
    color: Colors.textMuted,
    maxWidth: 64,
    textAlign: 'center',
  },
})
