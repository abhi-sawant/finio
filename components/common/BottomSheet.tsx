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
import { Colors } from '@/constants/Colors'

const { height: SCREEN_HEIGHT } = Dimensions.get('window')

interface BottomSheetProps {
  visible: boolean
  onClose: () => void
  title?: string
  snapPoint?: number // 0-1, fraction of screen height
  children: React.ReactNode
}

export function BottomSheet({
  visible,
  onClose,
  title,
  snapPoint = 0.6,
  children,
}: BottomSheetProps) {
  const insets = useSafeAreaInsets()
  const sheetHeight = SCREEN_HEIGHT * snapPoint
  const translateY = useSharedValue(sheetHeight)
  const backdropOpacity = useSharedValue(0)
  const startY = useSharedValue(0)

  const openSheet = useCallback(() => {
    backdropOpacity.value = withTiming(1, { duration: 250 })
    translateY.value = withSpring(0, { damping: 30, stiffness: 300, overshootClamping: true })
  }, [])

  const closeSheet = useCallback(() => {
    backdropOpacity.value = withTiming(0, { duration: 200 })
    translateY.value = withTiming(sheetHeight, { duration: 280, easing: Easing.in(Easing.ease) }, () => {
      runOnJS(onClose)()
    })
  }, [sheetHeight, onClose])

  useEffect(() => {
    if (visible) openSheet()
    else {
      translateY.value = sheetHeight
      backdropOpacity.value = 0
    }
  }, [visible])

  const pan = Gesture.Pan()
    .onStart(() => {
      startY.value = translateY.value
    })
    .onUpdate((e) => {
      const newY = startY.value + e.translationY
      translateY.value = Math.max(0, newY)
    })
    .onEnd((e) => {
      if (e.translationY > sheetHeight * 0.3 || e.velocityY > 800) {
        runOnJS(closeSheet)()
      } else {
        translateY.value = withSpring(0, { damping: 30, stiffness: 300, overshootClamping: true })
      }
    })

  const sheetStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
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

        <Animated.View style={[styles.sheet, { height: sheetHeight, paddingBottom: insets.bottom }, sheetStyle]}>
          <GestureDetector gesture={pan}>
            <View>
              {/* Drag handle */}
              <View style={styles.dragHandle} />

              {/* Header */}
              {title ? (
                <View style={styles.sheetHeader}>
                  <Text style={styles.sheetTitle}>{title}</Text>
                  <TouchableOpacity onPress={closeSheet} hitSlop={8}>
                    <X size={20} color={Colors.textMuted} />
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

const styles = StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.6)',
  },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    overflow: 'hidden',
  },
  dragHandle: {
    width: 36,
    height: 4,
    borderRadius: 2,
    backgroundColor: Colors.textMuted,
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
    borderBottomColor: Colors.border,
  },
  sheetTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: Colors.textPrimary,
  },
})
