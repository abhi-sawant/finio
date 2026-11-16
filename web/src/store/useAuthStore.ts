import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { AuthUser } from '@/services/api';

interface AuthStore {
  token: string | null;
  user: AuthUser | null;
  lastBackupAt: string | null;
  /** The server rejected our token (401) — cloud backups can't run until the user signs in again. */
  sessionExpired: boolean;
  isLoaded: boolean;

  loadAuth: () => void;
  setAuth: (token: string, user: AuthUser) => void;
  clearAuth: () => void;
  markSessionExpired: () => void;
  setLastBackupAt: (date: string) => void;
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      lastBackupAt: null,
      sessionExpired: false,
      isLoaded: false,

      loadAuth: () => {
        set({ isLoaded: true });
      },

      setAuth: (token, user) => {
        set({ token, user, sessionExpired: false });
      },

      clearAuth: () => {
        set({ token: null, user: null, lastBackupAt: null, sessionExpired: false });
      },

      markSessionExpired: () => {
        set({ sessionExpired: true });
      },

      setLastBackupAt: (date) => {
        set({ lastBackupAt: date });
      },
    }),
    {
      name: 'finio-auth',
      storage: createJSONStorage(() => localStorage),
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.loadAuth();
        }
      },
    },
  ),
);
