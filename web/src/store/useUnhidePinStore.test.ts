import { beforeEach, describe, expect, it } from 'vitest';
import { defaultSettings } from '@/data/defaultData';
import type { AppLockConfig, UnhidePinConfig } from '@/types';

const backing = new Map<string, string>();
globalThis.localStorage = {
  getItem: (key: string) => backing.get(key) ?? null,
  setItem: (key: string, value: string) => void backing.set(key, value),
  removeItem: (key: string) => void backing.delete(key),
  clear: () => backing.clear(),
  key: (index: number) => [...backing.keys()][index] ?? null,
  get length() {
    return backing.size;
  },
} as Storage;

const { useUnhidePinStore } = await import('./useUnhidePinStore');
const { isUnhideProtected, verifyUnhidePin } = await import('@/utils/unhidePin');
const { derivePinHash, generateSalt, PIN_HASH_ITERATIONS } = await import('@/utils/pinCrypto');

const NOW = new Date('2026-06-15T12:00:00.000Z').getTime();

function own(partial: Partial<UnhidePinConfig> = {}): UnhidePinConfig {
  return {
    source: 'own',
    salt: 'AAAAAAAAAAAAAAAAAAAAAA',
    hash: 'BBBBBBBBBBBBBBBBBBBBBB',
    iterations: 1000,
    pinLength: 4,
    autoHideOnExit: true,
    createdAt: '2026-06-01T00:00:00.000Z',
    ...partial,
  };
}

function lock(partial: Partial<AppLockConfig> = {}): AppLockConfig {
  return {
    enabled: true,
    salt: 'AAAAAAAAAAAAAAAAAAAAAA',
    hash: 'BBBBBBBBBBBBBBBBBBBBBB',
    iterations: 1000,
    pinLength: 4,
    autoLockMinutes: 1,
    webauthnCredentialId: null,
    createdAt: '2026-06-01T00:00:00.000Z',
    ...partial,
  };
}

function persisted() {
  return JSON.parse(backing.get('finio-unhide-pin') ?? '{"state":{}}').state as Record<
    string,
    unknown
  >;
}

beforeEach(() => {
  backing.clear();
  useUnhidePinStore.setState({
    config: null,
    failedAttempts: 0,
    lockedOutUntil: null,
    dialog: null,
    intent: 'toggle',
  });
});

describe('useUnhidePinStore', () => {
  it('setConfig stores the config and clears failures', () => {
    useUnhidePinStore.setState({ failedAttempts: 3, lockedOutUntil: NOW });
    useUnhidePinStore.getState().setConfig(own());
    const s = useUnhidePinStore.getState();
    expect(s.config?.source).toBe('own');
    expect(s.failedAttempts).toBe(0);
    expect(s.lockedOutUntil).toBeNull();
  });

  it('counts failures and starts a cooldown after the free attempts', () => {
    for (let i = 0; i < 4; i++) useUnhidePinStore.getState().registerFailure(NOW);
    expect(useUnhidePinStore.getState().lockedOutUntil).toBeNull();
    useUnhidePinStore.getState().registerFailure(NOW);
    expect(useUnhidePinStore.getState().lockedOutUntil).toBeGreaterThan(NOW);
    useUnhidePinStore.getState().expireLockout();
    expect(useUnhidePinStore.getState().failedAttempts).toBe(5);
  });

  it('toggles auto-hide in place and ignores it with no config', () => {
    useUnhidePinStore.getState().setAutoHideOnExit(false);
    expect(useUnhidePinStore.getState().config).toBeNull();
    useUnhidePinStore.getState().setConfig(own());
    useUnhidePinStore.getState().setAutoHideOnExit(false);
    expect(useUnhidePinStore.getState().config?.autoHideOnExit).toBe(false);
  });

  it('stores and clears the biometric credential in place', () => {
    useUnhidePinStore.getState().setBiometricCredentialId('cred');
    expect(useUnhidePinStore.getState().config).toBeNull();
    useUnhidePinStore.getState().setConfig(own());
    useUnhidePinStore.getState().setBiometricCredentialId('cred');
    expect(useUnhidePinStore.getState().config?.biometricCredentialId).toBe('cred');
    useUnhidePinStore.getState().setBiometricCredentialId(null);
    expect(useUnhidePinStore.getState().config?.biometricCredentialId).toBeNull();
  });

  it('never persists the transient dialog state', () => {
    useUnhidePinStore.getState().setConfig(own());
    useUnhidePinStore.getState().openDialog('verify', 'remove');
    const state = persisted();
    expect(Object.keys(state).sort()).toEqual(['config', 'failedAttempts', 'lockedOutUntil']);
  });

  it('clearConfig drops the PIN and cooldown', () => {
    useUnhidePinStore.getState().setConfig(own());
    useUnhidePinStore.getState().registerFailure(NOW);
    useUnhidePinStore.getState().clearConfig();
    expect(useUnhidePinStore.getState().config).toBeNull();
    expect(useUnhidePinStore.getState().failedAttempts).toBe(0);
  });

  it('keeps PIN material out of Settings', () => {
    expect(Object.keys(defaultSettings).filter((k) => /pin|lock|unhide/i.test(k))).toEqual([]);
  });
});

describe('isUnhideProtected', () => {
  it('is false with no config', () => {
    expect(isUnhideProtected(null, lock())).toBe(false);
  });
  it('an own PIN stands alone', () => {
    expect(isUnhideProtected(own(), null)).toBe(true);
  });
  it('an applock-sourced config dies with the app lock', () => {
    const linked = own({ source: 'applock', salt: '', hash: '', iterations: 0 });
    expect(isUnhideProtected(linked, lock())).toBe(true);
    expect(isUnhideProtected(linked, lock({ enabled: false }))).toBe(false);
    expect(isUnhideProtected(linked, null)).toBe(false);
  });
});

describe('verifyUnhidePin', () => {
  it('checks an own PIN and an app-lock-linked PIN', async () => {
    const salt = generateSalt();
    const hash = await derivePinHash('1234', salt);
    const mine = own({ salt, hash, iterations: PIN_HASH_ITERATIONS });
    expect(await verifyUnhidePin('1234', mine, null)).toBe(true);
    expect(await verifyUnhidePin('9999', mine, null)).toBe(false);

    const lockSalt = generateSalt();
    const lockHash = await derivePinHash('5678', lockSalt);
    const appLock = lock({ salt: lockSalt, hash: lockHash, iterations: PIN_HASH_ITERATIONS });
    const linked = own({ source: 'applock', salt: '', hash: '', iterations: 0 });
    expect(await verifyUnhidePin('5678', linked, appLock)).toBe(true);
    expect(await verifyUnhidePin('1234', linked, appLock)).toBe(false);
    expect(await verifyUnhidePin('5678', linked, lock({ ...appLock, enabled: false }))).toBe(false);
    expect(await verifyUnhidePin('5678', linked, null)).toBe(false);
  });
});
