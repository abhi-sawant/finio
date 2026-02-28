import '../global.css'
import { useEffect } from 'react'
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
import { Colors } from '@/constants/Colors'
import { Toast } from '@/components/common/Toast'

SplashScreen.preventAutoHideAsync()

export default function RootLayout() {
  const isHydrated = useFinanceStore((s) => s.isHydrated)
  const { isLoaded, loadAuth } = useAuthStore()

  const [fontsLoaded, fontError] = useFonts({
    DMSans_400Regular,
    DMSans_500Medium,
    DMSans_700Bold,
    Sora_700Bold,
    Sora_800ExtraBold,
  })

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
        <StatusBar style="light" backgroundColor={Colors.background} />
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: Colors.background },
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
