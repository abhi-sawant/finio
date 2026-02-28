export const DarkColors = {
  background: '#0f1117',
  surface: '#1a1d27',
  surfaceElevated: '#242736',
  primary: '#6C63FF',
  primaryLight: '#8B84FF',
  primaryDark: '#4D44CC',
  income: '#22c55e',
  expense: '#ef4444',
  transfer: '#3b82f6',
  textPrimary: '#f1f5f9',
  textMuted: '#64748b',
  textDisabled: '#334155',
  border: 'rgba(255,255,255,0.08)',
  borderStrong: 'rgba(255,255,255,0.16)',
  white: '#ffffff',
  black: '#000000',

  // semantic
  success: '#22c55e',
  warning: '#f59e0b',
  error: '#ef4444',
  info: '#3b82f6',

  // card shadows
  shadowColor: '#000000',
} as const

export const LightColors = {
  background: '#f4f6fb',
  surface: '#ffffff',
  surfaceElevated: '#eef0f7',
  primary: '#6C63FF',
  primaryLight: '#8B84FF',
  primaryDark: '#4D44CC',
  income: '#16a34a',
  expense: '#dc2626',
  transfer: '#2563eb',
  textPrimary: '#0f172a',
  textMuted: '#64748b',
  textDisabled: '#94a3b8',
  border: 'rgba(0,0,0,0.08)',
  borderStrong: 'rgba(0,0,0,0.16)',
  white: '#ffffff',
  black: '#000000',

  // semantic
  success: '#16a34a',
  warning: '#d97706',
  error: '#dc2626',
  info: '#2563eb',

  // card shadows
  shadowColor: '#000000',
} as const

// Kept for static/non-component usage (e.g. chart configs, accent palette arrays)
export const Colors = DarkColors

export type ColorPalette = typeof DarkColors
export type ColorKey = keyof ColorPalette

export const AccountColors = [
  '#6C63FF',
  '#22c55e',
  '#f59e0b',
  '#ef4444',
  '#3b82f6',
  '#ec4899',
  '#8b5cf6',
  '#06b6d4',
  '#10b981',
  '#f97316',
  '#64748b',
  '#a855f7',
] as const

export const CategoryColors = [
  '#ef4444',
  '#f97316',
  '#f59e0b',
  '#22c55e',
  '#10b981',
  '#06b6d4',
  '#3b82f6',
  '#6C63FF',
  '#8b5cf6',
  '#ec4899',
  '#a855f7',
  '#64748b',
] as const

export const LabelColors = [
  '#fbbf24',
  '#34d399',
  '#60a5fa',
  '#f472b6',
  '#a78bfa',
  '#fb923c',
] as const
