import React, { useCallback, useEffect } from 'react'
import {
  View,
  TouchableWithoutFeedback,
  StyleSheet,
  Dimensions,
  Text,
  TouchableOpacity,
  Modal,
  KeyboardAvoidingView,
  Platform,
} from 'react-native'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  runOnJS,
  Easing,
} from 'react-native-reanimated'
import { GestureDetector, Gesture, GestureHandlerRootView } from 'react-native-gesture-handler'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'

const { height: SCREEN_HEIGHT } = Dimensions.get('window')

interface BottomSheetProps {
  visible: boolean
  onClose: () => void
  title?: string
  snapPoint?: number // 0-1, fraction of screen height (default: 1 = full height)
  children: React.ReactNode
}

export function BottomSheet({
  visible,
  onClose,
  title,
  snapPoint = 1,
  children,
}: BottomSheetProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const initialTop = SCREEN_HEIGHT * (1 - snapPoint)
  const topValue = useSharedValue(SCREEN_HEIGHT)
  const backdropOpacity = useSharedValue(0)
  const startTop = useSharedValue(0)

  const openSheet = useCallback(() => {
    backdropOpacity.value = withTiming(1, { duration: 250 })
    topValue.value = withSpring(initialTop, { damping: 30, stiffness: 300, overshootClamping: true })
  }, [initialTop])

  const closeSheet = useCallback(() => {
    backdropOpacity.value = withTiming(0, { duration: 200 })
    topValue.value = withTiming(SCREEN_HEIGHT, { duration: 280, easing: Easing.in(Easing.ease) }, () => {
      runOnJS(onClose)()
    })
  }, [onClose])

  useEffect(() => {
    if (visible) openSheet()
    else {
      topValue.value = SCREEN_HEIGHT
      backdropOpacity.value = 0
    }
  }, [visible])

  const pan = Gesture.Pan()
    .onStart(() => {
      startTop.value = topValue.value
    })
    .onUpdate((e) => {
      const newTop = startTop.value + e.translationY
      topValue.value = Math.max(0, newTop)
    })
    .onEnd((e) => {
      'worklet'
      const distanceDown = topValue.value - initialTop
      if (e.velocityY > 800 || distanceDown > SCREEN_HEIGHT * 0.25) {
        backdropOpacity.value = withTiming(0, { duration: 200 })
        topValue.value = withTiming(SCREEN_HEIGHT, { duration: 280, easing: Easing.in(Easing.ease) }, () => {
          runOnJS(onClose)()
        })
      } else if (e.velocityY < -500 || e.translationY < -80) {
        topValue.value = withSpring(0, { damping: 30, stiffness: 300, overshootClamping: true })
      } else {
        topValue.value = withSpring(initialTop, { damping: 30, stiffness: 300, overshootClamping: true })
      }
    })

  const sheetStyle = useAnimatedStyle(() => ({
    top: topValue.value,
  }))

  const backdropStyle = useAnimatedStyle(() => ({
    opacity: backdropOpacity.value,
  }))

  return (
    <Modal
      visible={visible}
      transparent
      animationType="none"
      statusBarTranslucent
      onRequestClose={closeSheet}
    >
      <GestureHandlerRootView style={StyleSheet.absoluteFill}>
        <KeyboardAvoidingView
          style={StyleSheet.absoluteFill}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          keyboardVerticalOffset={0}
        >
        <TouchableWithoutFeedback onPress={closeSheet}>
          <Animated.View style={[styles.backdrop, backdropStyle]} />
        </TouchableWithoutFeedback>

        <Animated.View style={[styles.sheet, { paddingBottom: insets.bottom, paddingTop: insets.top + 12 }, sheetStyle]}>
          <GestureDetector gesture={pan}>
            <View>
              {/* Drag handle */}
              <View style={styles.dragHandle} />

              {/* Header */}
              {title ? (
                <View style={styles.sheetHeader}>
                  <Text style={styles.sheetTitle}>{title}</Text>
                  <TouchableOpacity onPress={closeSheet} hitSlop={8}>
                    <X size={20} color={colors.textMuted} />
                  </TouchableOpacity>
                </View>
              ) : null}
            </View>
          </GestureDetector>

          <View style={{ flex: 1 }}>
            {children}
          </View>
        </Animated.View>
        </KeyboardAvoidingView>
      </GestureHandlerRootView>
    </Modal>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.6)',
  },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    overflow: 'hidden',
  },
  dragHandle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.textMuted,
    opacity: 0.4,
    alignSelf: 'center',
    marginBottom: 12,
  },
  sheetHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  sheetTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: colors.textPrimary,
  },
})
}
