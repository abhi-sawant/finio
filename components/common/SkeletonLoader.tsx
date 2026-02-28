import React, { useEffect } from 'react'
import { View, StyleSheet, type ViewStyle } from 'react-native'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'

interface SkeletonBoxProps {
  style?: ViewStyle
  borderRadius?: number
}

function SkeletonBox({ style, borderRadius = 8 }: SkeletonBoxProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const opacity = useSharedValue(1)

  useEffect(() => {
    opacity.value = withRepeat(
      withSequence(
        withTiming(0.3, { duration: 700 }),
        withTiming(1, { duration: 700 })
      ),
      -1,
      false
    )
  }, [])

  const animStyle = useAnimatedStyle(() => ({ opacity: opacity.value }))

  return (
    <Animated.View
      style={[
        { backgroundColor: colors.surfaceElevated, borderRadius },
        style,
        animStyle,
      ]}
    />
  )
}

export function TransactionSkeleton() {
  const colors = useColors()
  const styles = makeStyles(colors)
  return (
    <View style={styles.txnRow}>
      <SkeletonBox style={{ width: 44, height: 44 }} borderRadius={22} />
      <View style={styles.txnMiddle}>
        <SkeletonBox style={{ width: '70%', height: 14 }} />
        <SkeletonBox style={{ width: '40%', height: 12, marginTop: 6 }} />
      </View>
      <SkeletonBox style={{ width: 60, height: 16 }} />
    </View>
  )
}

export function CardSkeleton() {
  const colors = useColors()
  const styles = makeStyles(colors)
  return (
    <View style={styles.card}>
      <SkeletonBox style={{ width: 36, height: 36 }} borderRadius={18} />
      <View style={{ flex: 1, gap: 8 }}>
        <SkeletonBox style={{ width: '60%', height: 14 }} />
        <SkeletonBox style={{ width: '40%', height: 12 }} />
      </View>
    </View>
  )
}

export function DashboardSkeleton() {
  const colors = useColors()
  const styles = makeStyles(colors)
  return (
    <View style={styles.dashContainer}>
      <SkeletonBox style={styles.balanceCard} borderRadius={20} />
      <View style={styles.accountsRow}>
        <SkeletonBox style={styles.accountCard} borderRadius={16} />
        <SkeletonBox style={styles.accountCard} borderRadius={16} />
      </View>
      {Array.from({ length: 5 }).map((_, i) => (
        <TransactionSkeleton key={i} />
      ))}
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  txnRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  txnMiddle: {
    flex: 1,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 16,
    backgroundColor: colors.surface,
    borderRadius: 16,
    marginHorizontal: 16,
    marginVertical: 4,
  },
  dashContainer: {
    gap: 12,
  },
  balanceCard: {
    height: 140,
    marginHorizontal: 16,
    marginTop: 8,
  },
  accountsRow: {
    flexDirection: 'row',
    gap: 12,
    paddingHorizontal: 16,
  },
  accountCard: {
    flex: 1,
    height: 80,
  },
})
}
