import React, { useEffect } from 'react'
import { TouchableOpacity, StyleSheet } from 'react-native'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  withRepeat,
  withSequence,
  Easing,
} from 'react-native-reanimated'
import { Plus } from 'lucide-react-native'
import { LinearGradient } from 'expo-linear-gradient'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { lightHaptic } from '@/utils/haptics'

interface FABProps {
  onPress: () => void
  bottom?: number
}

export function FAB({ onPress, bottom }: FABProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const scale = useSharedValue(0)
  const pulseScale = useSharedValue(1)
  const pulseOpacity = useSharedValue(0.6)

  useEffect(() => {
    // Entrance animation
    scale.value = withSpring(1, { damping: 12, stiffness: 150 })

    // Pulse animation
    pulseScale.value = withRepeat(
      withSequence(
        withTiming(1.4, { duration: 1000, easing: Easing.out(Easing.ease) }),
        withTiming(1, { duration: 1000, easing: Easing.in(Easing.ease) })
      ),
      -1,
      false
    )
    pulseOpacity.value = withRepeat(
      withSequence(
        withTiming(0, { duration: 1000 }),
        withTiming(0.5, { duration: 1000 })
      ),
      -1,
      false
    )
  }, [])

  const containerStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }))

  const pulseStyle = useAnimatedStyle(() => ({
    transform: [{ scale: pulseScale.value }],
    opacity: pulseOpacity.value,
  }))

  const handlePress = async () => {
    scale.value = withSequence(
      withTiming(0.9, { duration: 80 }),
      withSpring(1, { damping: 10, stiffness: 200 })
    )
    await lightHaptic()
    onPress()
  }

  const bottomInset = bottom ?? insets.bottom + 80

  return (
    <Animated.View style={[styles.wrapper, { bottom: bottomInset }, containerStyle]}>
      <Animated.View style={[styles.pulse, pulseStyle]} />
      <TouchableOpacity
        onPress={handlePress}
        activeOpacity={0.85}
        style={styles.touchable}
        accessibilityRole="button"
        accessibilityLabel="Add transaction"
      >
        <LinearGradient
          colors={['#8B84FF', '#6C63FF', '#4D44CC']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.gradient}
        >
          <Plus size={28} color="#fff" strokeWidth={2.5} />
        </LinearGradient>
      </TouchableOpacity>
    </Animated.View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  wrapper: {
    position: 'absolute',
    right: 20,
    width: 56,
    height: 56,
    zIndex: 999,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pulse: {
    position: 'absolute',
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primary,
  },
  touchable: {
    width: 56,
    height: 56,
    borderRadius: 28,
    overflow: 'hidden',
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.5,
    shadowRadius: 16,
    elevation: 10,
  },
  gradient: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
})
}
