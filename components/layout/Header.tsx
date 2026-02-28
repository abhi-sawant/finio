import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useRouter } from 'expo-router'
import { ChevronLeft, X } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { lightHaptic } from '@/utils/haptics'

interface HeaderProps {
  title: string
  subtitle?: string
  showBack?: boolean
  showClose?: boolean
  rightElement?: React.ReactNode
  onBack?: () => void
}

export function Header({
  title,
  subtitle,
  showBack = false,
  showClose = false,
  rightElement,
  onBack,
}: HeaderProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const router = useRouter()

  const handleBack = async () => {
    await lightHaptic()
    if (onBack) onBack()
    else router.back()
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top + 8 }]}>
      <View style={styles.innerRow}>
        {(showBack || showClose) ? (
          <TouchableOpacity onPress={handleBack} style={styles.backBtn} hitSlop={8}>
            {showClose ? (
              <X size={22} color={colors.textPrimary} />
            ) : (
              <ChevronLeft size={24} color={colors.textPrimary} />
            )}
          </TouchableOpacity>
        ) : (
          <View style={styles.backBtn} />
        )}

        <View style={styles.titleContainer}>
          <Text style={styles.title} numberOfLines={1}>{title}</Text>
          {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
        </View>

        <View style={styles.rightContainer}>
          {rightElement ?? null}
        </View>
      </View>

      {/* Drag handle for modals */}
      {showClose && (
        <View style={styles.dragHandleWrapper}>
          <View style={styles.dragHandle} />
        </View>
      )}
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    backgroundColor: colors.surface,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  dragHandleWrapper: {
    alignItems: 'center',
    marginBottom: 4,
  },
  dragHandle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.textMuted,
    opacity: 0.4,
  },
  innerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    minHeight: 44,
  },
  backBtn: {
    width: 36,
    height: 36,
    alignItems: 'center',
    justifyContent: 'center',
  },
  titleContainer: {
    flex: 1,
    alignItems: 'center',
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 18,
    color: colors.textPrimary,
  },
  subtitle: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  rightContainer: {
    width: 36,
    alignItems: 'flex-end',
  },
})
}
