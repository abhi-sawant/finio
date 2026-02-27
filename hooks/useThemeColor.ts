import { Colors } from '@/constants/Colors'
import type { ColorKey } from '@/constants/Colors'

export function useThemeColor(colorKey: ColorKey): string {
  return Colors[colorKey]
}
