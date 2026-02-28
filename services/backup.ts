import { useFinanceStore } from '@/store/useFinanceStore'
import { useAuthStore } from '@/store/useAuthStore'
import { api } from './api'

/** Upload current app state to the server. Returns the ISO timestamp of the backup. */
export async function uploadBackup(): Promise<string> {
  const { token } = useAuthStore.getState()
  if (!token) throw new Error('Not signed in')

  const { accounts, transactions, categories, labels, settings } = useFinanceStore.getState()
  const payload = { accounts, transactions, categories, labels, settings }

  await api.uploadBackup(token, payload)

  const now = new Date().toISOString()
  await useAuthStore.getState().setLastBackupAt(now)
  return now
}

/** Download the latest backup from the server and import it into the app. */
export async function restoreLatestBackup(): Promise<void> {
  const { token } = useAuthStore.getState()
  if (!token) throw new Error('Not signed in')

  const res = await api.getLatestBackup(token)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  useFinanceStore.getState().importData(res.data as any)
}

/**
 * Silently upload a backup if the last one was more than 24 hours ago.
 * Intended to be called on app startup — errors are swallowed.
 */
export async function autoBackupIfNeeded(): Promise<void> {
  const { token, lastBackupAt } = useAuthStore.getState()
  if (!token) return

  if (!lastBackupAt) {
    await uploadBackup()
    return
  }

  const hoursSinceLast = (Date.now() - new Date(lastBackupAt).getTime()) / (1000 * 60 * 60)
  if (hoursSinceLast >= 24) {
    await uploadBackup()
  }
}
