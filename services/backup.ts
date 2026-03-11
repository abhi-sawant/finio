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

  // The server streams the raw backup JSON directly (no {data:...} wrapper)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const res = await api.getLatestBackup(token) as any
  useFinanceStore.getState().importData(res)
}

/**
 * Silently upload a backup if the last one was more than 24 hours ago.
 * Intended to be called on app startup — errors are swallowed.
 *
 * Guard: skip if local store has no data. On iOS, SecureStore survives
 * reinstalls but AsyncStorage does not, so without this check the auto-
 * backup would overwrite the real cloud backup with an empty state.
 */
export async function autoBackupIfNeeded(): Promise<void> {
  const { token, lastBackupAt } = useAuthStore.getState()
  if (!token) return

  // Don't upload empty data — protects against post-reinstall data loss
  const { accounts, transactions } = useFinanceStore.getState()
  if (accounts.length === 0 && transactions.length === 0) return

  if (!lastBackupAt) {
    await uploadBackup()
    return
  }

  const hoursSinceLast = (Date.now() - new Date(lastBackupAt).getTime()) / (1000 * 60 * 60)
  if (hoursSinceLast >= 24) {
    await uploadBackup()
  }
}
