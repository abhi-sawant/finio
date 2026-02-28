import React, { useEffect } from 'react'
import { View, Text, TouchableOpacity, Platform, StyleSheet } from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useRouter } from 'expo-router'
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  withRepeat,
  withSequence,
  Easing,
} from 'react-native-reanimated'
import {
  LayoutDashboard,
  ArrowLeftRight,
  Wallet,
  BarChart3,
  Settings2,
  Plus,
} from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { lightHaptic } from '@/utils/haptics'
import { LinearGradient } from 'expo-linear-gradient'

interface TabItem {
  name: string
  label: string
  Icon: React.ComponentType<{ size: number; color: string; strokeWidth?: number }>
}

const TABS: TabItem[] = [
  { name: 'index', label: 'Home', Icon: LayoutDashboard },
  { name: 'transactions', label: 'Txns', Icon: ArrowLeftRight },
  { name: 'accounts', label: 'Accounts', Icon: Wallet },
  { name: 'analytics', label: 'Analytics', Icon: BarChart3 },
]

const TAB_BAR_HEIGHT = 64

function TabButton({
  tab,
  isFocused,
  onPress,
}: {
  tab: TabItem
  isFocused: boolean
  onPress: () => void
}) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const scale = useSharedValue(1)
  const opacity = useSharedValue(isFocused ? 1 : 0.6)

  useEffect(() => {
    opacity.value = withTiming(isFocused ? 1 : 0.6, { duration: 200 })
    if (isFocused) {
      scale.value = withSpring(1.1, { damping: 10, stiffness: 200 }, () => {
        scale.value = withSpring(1, { damping: 10, stiffness: 200 })
      })
    }
  }, [isFocused])

  const animStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }))

  return (
    <TouchableOpacity
      onPress={onPress}
      style={styles.tabButton}
      activeOpacity={0.7}
      accessibilityRole="button"
      accessibilityLabel={tab.label}
    >
      <Animated.View style={[styles.tabInner, animStyle]}>
        <tab.Icon
          size={22}
          color={isFocused ? colors.primary : colors.textMuted}
          strokeWidth={isFocused ? 2.5 : 1.8}
        />
        <Text
          style={[
            styles.tabLabel,
            { color: isFocused ? colors.primary : colors.textMuted },
          ]}
        >
          {tab.label}
        </Text>
        {isFocused && <View style={styles.activeIndicator} />}
      </Animated.View>
    </TouchableOpacity>
  )
}

function FabButton({ onPress }: { onPress: () => void }) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const pulse = useSharedValue(1)

  useEffect(() => {
    pulse.value = withRepeat(
      withSequence(
        withTiming(1.15, { duration: 800, easing: Easing.out(Easing.ease) }),
        withTiming(1, { duration: 800, easing: Easing.in(Easing.ease) }),
      ),
      -1, // infinite
      false,
    )
  }, [])

  const pulseStyle = useAnimatedStyle(() => ({
    transform: [{ scale: pulse.value }],
    opacity: 2 - pulse.value,
  }))

  const handlePress = async () => {
    await lightHaptic()
    onPress()
  }

  return (
    <View style={styles.fabWrapper}>
      <Animated.View
        style={[
          styles.fabPulse,
          pulseStyle,
        ]}
      />
      <TouchableOpacity
        onPress={handlePress}
        activeOpacity={0.85}
        style={styles.fabTouchable}
        accessibilityRole="button"
        accessibilityLabel="Add transaction"
      >
        <LinearGradient
          colors={['#8B84FF', '#6C63FF', '#4D44CC']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.fabGradient}
        >
          <Plus size={26} color="#fff" strokeWidth={2.5} />
        </LinearGradient>
      </TouchableOpacity>
    </View>
  )
}

export function TabBar({ state, descriptors, navigation }: BottomTabBarProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const router = useRouter()

  const bottomPad = Math.max(insets.bottom, Platform.OS === 'ios' ? 16 : 8)

  // Split tabs: first 2 left, last 3 right (with FAB in center)
  const leftTabs = TABS.slice(0, 2)
  const rightTabs = TABS.slice(2)

  const getRouteIndex = (name: string) => TABS.findIndex((t) => t.name === name)

  const handleTabPress = async (tabName: string, routeIndex: number) => {
    const event = navigation.emit({
      type: 'tabPress',
      target: state.routes[routeIndex]?.key ?? '',
      canPreventDefault: true,
    })
    if (!event.defaultPrevented) {
      await lightHaptic()
      navigation.navigate(tabName)
    }
  }

  return (
    <View
      style={[
        styles.container,
        { paddingBottom: bottomPad, height: TAB_BAR_HEIGHT + bottomPad },
      ]}
    >
      <View style={styles.innerRow}>
        {/* Left tabs */}
        <View style={styles.tabsGroup}>
          {leftTabs.map((tab) => {
            const idx = getRouteIndex(tab.name)
            return (
              <TabButton
                key={tab.name}
                tab={tab}
                isFocused={state.index === idx}
                onPress={() => handleTabPress(tab.name, idx)}
              />
            )
          })}
        </View>

        {/* FAB center */}
        <FabButton onPress={() => router.push('/modals/add-transaction')} />

        {/* Right tabs */}
        <View style={styles.tabsGroup}>
          {rightTabs.map((tab) => {
            const idx = getRouteIndex(tab.name)
            return (
              <TabButton
                key={tab.name}
                tab={tab}
                isFocused={state.index === idx}
                onPress={() => handleTabPress(tab.name, idx)}
              />
            )
          })}
        </View>
      </View>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    backgroundColor: colors.surface,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  innerRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
  },
  tabsGroup: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  tabButton: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
  },
  tabInner: {
    alignItems: 'center',
    gap: 2,
    position: 'relative',
  },
  tabLabel: {
    fontSize: 10,
    fontFamily: 'DMSans_500Medium',
    marginTop: 2,
  },
  activeIndicator: {
    position: 'absolute',
    bottom: -6,
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.primary,
  },
  fabWrapper: {
    width: 60,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: -28,
  },
  fabPulse: {
    position: 'absolute',
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primary,
  },
  fabTouchable: {
    width: 52,
    height: 52,
    borderRadius: 26,
    overflow: 'hidden',
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 12,
    elevation: 8,
  },
  fabGradient: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
})
}
