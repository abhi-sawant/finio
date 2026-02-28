import { create } from 'zustand'
import * as SecureStore from 'expo-secure-store'
import type { AuthUser } from '@/services/api'

const TOKEN_KEY = 'finio_token'
const USER_KEY = 'finio_user'
const LAST_BACKUP_KEY = 'finio_last_backup'

interface AuthStore {
  token: string | null
  user: AuthUser | null
  lastBackupAt: string | null
  isLoaded: boolean

  loadAuth: () => Promise<void>
  setAuth: (token: string, user: AuthUser) => Promise<void>
  clearAuth: () => Promise<void>
  setLastBackupAt: (date: string) => Promise<void>
}

export const useAuthStore = create<AuthStore>((set) => ({
  token: null,
  user: null,
  lastBackupAt: null,
  isLoaded: false,

  loadAuth: async () => {
    try {
      const [token, userRaw, lastBackupAt] = await Promise.all([
        SecureStore.getItemAsync(TOKEN_KEY),
        SecureStore.getItemAsync(USER_KEY),
        SecureStore.getItemAsync(LAST_BACKUP_KEY),
      ])
      const user = userRaw ? (JSON.parse(userRaw) as AuthUser) : null
      set({ token, user, lastBackupAt, isLoaded: true })
    } catch {
      set({ isLoaded: true })
    }
  },

  setAuth: async (token, user) => {
    await Promise.all([
      SecureStore.setItemAsync(TOKEN_KEY, token),
      SecureStore.setItemAsync(USER_KEY, JSON.stringify(user)),
    ])
    set({ token, user })
  },

  clearAuth: async () => {
    await Promise.all([
      SecureStore.deleteItemAsync(TOKEN_KEY),
      SecureStore.deleteItemAsync(USER_KEY),
    ])
    set({ token: null, user: null })
  },

  setLastBackupAt: async (date) => {
    await SecureStore.setItemAsync(LAST_BACKUP_KEY, date)
    set({ lastBackupAt: date })
  },
}))
