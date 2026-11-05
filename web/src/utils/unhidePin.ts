import type { AppLockConfig, UnhidePinConfig } from '@/types';
import { verifyPin } from './pinCrypto';

/**
 * Whether unhiding is currently PIN-protected. An `applock`-sourced config is only as alive as
 * the app lock it points at; once that is off there is no PIN to ask for, so the user is walked
 * through setup again.
 */
export function isUnhideProtected(
  config: UnhidePinConfig | null,
  appLock: AppLockConfig | null,
): config is UnhidePinConfig {
  if (!config) return false;
  if (config.source === 'applock') return appLock?.enabled === true;
  return config.hash.length > 0;
}

/** How many digits the unhide prompt should collect. */
export function unhidePinLength(config: UnhidePinConfig, appLock: AppLockConfig | null): number {
  return config.source === 'applock' ? (appLock?.pinLength ?? config.pinLength) : config.pinLength;
}

/** Checks a PIN against whichever record the config points at. Never throws. */
export async function verifyUnhidePin(
  pin: string,
  config: UnhidePinConfig,
  appLock: AppLockConfig | null,
): Promise<boolean> {
  const record = config.source === 'applock' ? appLock : config;
  if (!record || (config.source === 'applock' && !appLock?.enabled)) return false;
  return verifyPin(pin, record);
}
