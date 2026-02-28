import { useColorScheme } from 'react-native'
import { DarkColors, LightColors } from '@/constants/Colors'
import type { ColorPalette } from '@/constants/Colors'
import { useFinanceStore } from '@/store/useFinanceStore'

export function useColors(): ColorPalette {
  const systemScheme = useColorScheme()
  const theme = useFinanceStore((s) => s.settings.theme)

  if (theme === 'system') {
    return systemScheme === 'light' ? LightColors : DarkColors
  }
  return theme === 'light' ? LightColors : DarkColors
}
