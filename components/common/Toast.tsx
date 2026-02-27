import React, { useEffect, useRef, useCallback } from 'react'
import { Text, StyleSheet, Dimensions } from 'react-native'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  runOnJS,
  Easing,
} from 'react-native-reanimated'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { Colors } from '@/constants/Colors'

const { width: SCREEN_WIDTH } = Dimensions.get('window')

type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastConfig {
  message: string
  type?: ToastType
  duration?: number
}

interface ToastHandle {
  show: (config: ToastConfig) => void
}

const TOAST_COLORS: Record<ToastType, string> = {
  success: Colors.income,
  error: Colors.expense,
  info: Colors.primary,
  warning: '#f59e0b',
}

// Global ref to trigger toasts from anywhere
let toastRef: ToastHandle | null = null

export function showToast(config: ToastConfig) {
  toastRef?.show(config)
}

export function Toast() {
  const insets = useSafeAreaInsets()
  const translateY = useSharedValue(-120)
  const opacity = useSharedValue(0)
  const hideTimeout = useRef<ReturnType<typeof setTimeout> | null>(null)

  const [currentConfig, setCurrentConfig] = React.useState<ToastConfig | null>(null)

  const hide = useCallback(() => {
    translateY.value = withTiming(-120, { duration: 300, easing: Easing.in(Easing.ease) })
    opacity.value = withTiming(0, { duration: 300 }, () => {
      runOnJS(setCurrentConfig)(null)
    })
  }, [])

  const show = useCallback((config: ToastConfig) => {
    if (hideTimeout.current) clearTimeout(hideTimeout.current)
    setCurrentConfig(config)
    translateY.value = withSpring(0, { damping: 20, stiffness: 200 })
    opacity.value = withTiming(1, { duration: 200 })
    hideTimeout.current = setTimeout(hide, config.duration ?? 3000)
  }, [hide])

  useEffect(() => {
    toastRef = { show }
    return () => { toastRef = null }
  }, [show])

  const animStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
    opacity: opacity.value,
  }))

  if (!currentConfig) return null

  const bgColor = TOAST_COLORS[currentConfig.type ?? 'info']

  return (
    <Animated.View
      style={[
        styles.container,
        {
          top: insets.top + 8,
          backgroundColor: bgColor,
        },
        animStyle,
      ]}
    >
      <Text style={styles.message}>{currentConfig.message}</Text>
    </Animated.View>
  )
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    left: 16,
    right: 16,
    zIndex: 9999,
    borderRadius: 14,
    paddingVertical: 14,
    paddingHorizontal: 18,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 10,
    maxWidth: SCREEN_WIDTH - 32,
  },
  message: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: '#fff',
    textAlign: 'center',
  },
})
