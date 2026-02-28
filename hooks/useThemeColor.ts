import { useColors } from '@/hooks/useColors'
import type { ColorKey } from '@/constants/Colors'

export function useThemeColor(colorKey: ColorKey): string {
  const colors = useColors()
  return colors[colorKey]
}
