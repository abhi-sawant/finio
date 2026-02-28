import '../global.css'
import { useEffect } from 'react'
import { useColorScheme, Platform } from 'react-native'
import { StatusBar } from 'expo-status-bar'
import { Stack } from 'expo-router'
import { GestureHandlerRootView } from 'react-native-gesture-handler'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import * as SplashScreen from 'expo-splash-screen'
import {
  useFonts,
  DMSans_400Regular,
  DMSans_500Medium,
  DMSans_700Bold,
} from '@expo-google-fonts/dm-sans'
import { Sora_700Bold, Sora_800ExtraBold } from '@expo-google-fonts/sora'
import { useFinanceStore } from '@/store/useFinanceStore'
import { useAuthStore } from '@/store/useAuthStore'
import { autoBackupIfNeeded } from '@/services/backup'
import { useColors } from '@/hooks/useColors'
import * as NavigationBar from 'expo-navigation-bar'
import { Toast } from '@/components/common/Toast'

SplashScreen.preventAutoHideAsync()

export default function RootLayout() {
  const isHydrated = useFinanceStore((s) => s.isHydrated)
  const { isLoaded, loadAuth } = useAuthStore()
  const colors = useColors()
  const theme = useFinanceStore((s) => s.settings.theme)
  const systemScheme = useColorScheme()

  // Resolve effective theme for StatusBar
  const effectiveTheme =
    theme === 'system' ? (systemScheme ?? 'dark') : theme
  const statusBarStyle = effectiveTheme === 'light' ? 'dark' : 'light'

  const [fontsLoaded, fontError] = useFonts({
    DMSans_400Regular,
    DMSans_500Medium,
    DMSans_700Bold,
    Sora_700Bold,
    Sora_800ExtraBold,
  })

  // Sync Android navigation bar style with theme
  useEffect(() => {
    if (Platform.OS !== 'android') return
    const buttonStyle = effectiveTheme === 'light' ? 'dark' : 'light'
    NavigationBar.setButtonStyleAsync(buttonStyle)
    NavigationBar.setBackgroundColorAsync(colors.background)
  }, [effectiveTheme, colors.background])

  // Load stored JWT + user on startup
  useEffect(() => {
    loadAuth()
  }, [])

  // Hide splash once fonts, store data, and auth are ready
  useEffect(() => {
    if ((fontsLoaded || fontError) && isHydrated && isLoaded) {
      SplashScreen.hideAsync()
    }
  }, [fontsLoaded, fontError, isHydrated, isLoaded])

  // Silently auto-backup once per day in the background
  useEffect(() => {
    if (isHydrated && isLoaded) {
      autoBackupIfNeeded().catch(() => {})
    }
  }, [isHydrated, isLoaded])

  if (!fontsLoaded && !fontError) return null
  if (!isHydrated) return null

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StatusBar style={statusBarStyle} backgroundColor={colors.background} />
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: colors.background },
            animation: 'fade',
          }}
        >
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen
            name="auth"
            options={{
              headerShown: false,
              animation: 'slide_from_bottom',
            }}
          />
          <Stack.Screen
            name="modals/add-transaction"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
              headerShown: false,
            }}
          />
          <Stack.Screen
            name="modals/transaction-detail"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
              headerShown: false,
            }}
          />
          <Stack.Screen
            name="modals/add-account"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
              headerShown: false,
            }}
          />
          <Stack.Screen
            name="modals/add-category"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
              headerShown: false,
            }}
          />
          <Stack.Screen name="+not-found" />
        </Stack>
        <Toast />
      </SafeAreaProvider>
    </GestureHandlerRootView>
  )
}
