import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { nextLockoutUntil } from '@/utils/appLock';
import type { UnhidePinConfig } from '@/types';

interface UnhidePinStore {
  /** Null until the user first hides/unhides amounts and sets a PIN (or removes it again). */
  config: UnhidePinConfig | null;
  /** Persisted so a reload cannot reset the cooldown. */
  failedAttempts: number;
  lockedOutUntil: number | null;

  /** Transient — which prompt is open. Excluded by `partialize`. */
  dialog: 'setup' | 'verify' | null;
  /**
   * Why the dialog is open. `toggle` is the eye button (the dialog finishes by flipping the
   * amounts); `change` / `remove` come from Settings and leave the amounts as they are.
   */
  intent: 'toggle' | 'change' | 'remove';

  setConfig: (config: UnhidePinConfig) => void;
  clearConfig: () => void;
  setAutoHideOnExit: (enabled: boolean) => void;
  setBiometricCredentialId: (id: string | null) => void;
  registerFailure: (now?: number) => void;
  expireLockout: () => void;
  resetFailures: () => void;
  openDialog: (dialog: 'setup' | 'verify', intent?: 'toggle' | 'change' | 'remove') => void;
  closeDialog: () => void;
}

/**
 * Unhide-PIN state, in its own store under `finio-unhide-pin` — separate from the finance store
 * for the same reason as the app lock (see `UnhidePinConfig`): that store is exported and
 * uploaded, this must never be.
 */
export const useUnhidePinStore = create<UnhidePinStore>()(
  persist(
    (set) => ({
      config: null,
      failedAttempts: 0,
      lockedOutUntil: null,
      dialog: null,
      intent: 'toggle',

      setConfig: (config) => set({ config, failedAttempts: 0, lockedOutUntil: null }),
      clearConfig: () => set({ config: null, failedAttempts: 0, lockedOutUntil: null }),
      setAutoHideOnExit: (enabled) =>
        set((state) => ({
          config: state.config ? { ...state.config, autoHideOnExit: enabled } : null,
        })),
      setBiometricCredentialId: (id) =>
        set((state) => ({
          config: state.config ? { ...state.config, biometricCredentialId: id } : null,
        })),
      registerFailure: (now = Date.now()) =>
        set((state) => {
          const failedAttempts = state.failedAttempts + 1;
          return { failedAttempts, lockedOutUntil: nextLockoutUntil(failedAttempts, now) };
        }),
      expireLockout: () => set({ lockedOutUntil: null }),
      resetFailures: () => set({ failedAttempts: 0, lockedOutUntil: null }),
      openDialog: (dialog, intent = 'toggle') => set({ dialog, intent }),
      closeDialog: () => set({ dialog: null }),
    }),
    {
      name: 'finio-unhide-pin',
      version: 1,
      storage: createJSONStorage(() => localStorage),
      // Allow-list, so a transient field added later is dropped rather than persisted.
      partialize: (state) => ({
        config: state.config,
        failedAttempts: state.failedAttempts,
        lockedOutUntil: state.lockedOutUntil,
      }),
    },
  ),
);
