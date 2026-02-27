import * as Haptics from 'expo-haptics'
import { useFinanceStore } from '@/store/useFinanceStore'

function isHapticEnabled(): boolean {
  try {
    return useFinanceStore.getState().settings.hapticFeedback
  } catch {
    return true
  }
}

export async function lightHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light)
  } catch {
    // Ignore on devices without haptic support
  }
}

export async function mediumHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium)
  } catch {}
}

export async function heavyHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy)
  } catch {}
}

export async function successHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success)
  } catch {}
}

export async function errorHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error)
  } catch {}
}

export async function warningHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning)
  } catch {}
}

export async function selectionHaptic(): Promise<void> {
  if (!isHapticEnabled()) return
  try {
    await Haptics.selectionAsync()
  } catch {}
}
