import { useEffect } from 'react'
import {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  runOnJS,
  Easing,
} from 'react-native-reanimated'

interface UseCountUpOptions {
  from?: number
  to: number
  duration?: number
  onComplete?: () => void
}

export function useCountUp({ from = 0, to, duration = 1200, onComplete }: UseCountUpOptions) {
  const animatedValue = useSharedValue(from)

  useEffect(() => {
    animatedValue.value = from
    animatedValue.value = withTiming(to, {
      duration,
      easing: Easing.out(Easing.cubic),
    }, (finished) => {
      if (finished && onComplete) {
        runOnJS(onComplete)()
      }
    })
  }, [to])

  const animatedStyle = useAnimatedStyle(() => ({
    // This can be used to drive transforms
  }))

  return { animatedValue, animatedStyle }
}
