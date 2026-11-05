import { useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useAppLockStore } from '@/store/useAppLockStore';
import { useUnhidePinStore } from '@/store/useUnhidePinStore';
import { SwitchField } from '@/components/ui/switch';
import { PinDots, PinPad } from '@/components/applock/PinPad';
import { SecretDialogShell, SecretDialogError } from '@/components/settings/SecretDialogShell';
import { formatLockoutCountdown, remainingLockoutMs } from '@/utils/appLock';
import {
  derivePinHash,
  generateSalt,
  isPinCryptoSupported,
  PIN_HASH_ITERATIONS,
  PIN_LENGTH_OPTIONS,
  verifyPin,
} from '@/utils/pinCrypto';
import { unhidePinLength, verifyUnhidePin } from '@/utils/unhidePin';
import { notifyAmountsToggled, setAmountsHidden } from '@/services/hideAmounts';
import { EyeOff, Fingerprint } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  isPlatformAuthenticatorAvailable,
  isWebAuthnSupported,
  registerBiometric,
  verifyBiometric,
} from '@/services/appLockBiometric';

/** Mounted once from `App()`; the eye button only flips `dialog` in the store. */
export function UnhidePinDialogs() {
  const dialog = useUnhidePinStore((s) => s.dialog);
  return (
    <>
      {dialog === 'setup' && <SetupDialog />}
      {dialog === 'verify' && <VerifyDialog />}
    </>
  );
}

/**
 * Verify, then reveal amounts (or continue the Settings change / remove flow). Biometrics go
 * first when enrolled: the OS prompt appears straight away and "Use PIN instead" (or a cancelled
 * / failed prompt) falls through to the keypad.
 */
function VerifyDialog() {
  const config = useUnhidePinStore((s) => s.config);
  const intent = useUnhidePinStore((s) => s.intent);
  const lockedOutUntil = useUnhidePinStore((s) => s.lockedOutUntil);
  const appLock = useAppLockStore((s) => s.config);
  const biometricId = config?.biometricCredentialId ?? null;
  const [mode, setMode] = useState<'biometric' | 'pin'>(
    biometricId && isWebAuthnSupported() ? 'biometric' : 'pin',
  );
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const started = useRef(false);

  const remaining = remainingLockoutMs(lockedOutUntil, now);
  const lockedOut = remaining > 0;

  const close = () => useUnhidePinStore.getState().closeDialog();

  /** Shared ending for a verified PIN or biometric. */
  const onVerified = () => {
    const store = useUnhidePinStore.getState();
    store.resetFailures();
    if (intent === 'change') {
      store.openDialog('setup', 'change');
    } else if (intent === 'remove') {
      store.clearConfig();
      close();
      toast.success('Unhide PIN removed');
    } else {
      setAmountsHidden(false);
      close();
      notifyAmountsToggled(false);
    }
  };

  const runBiometric = async () => {
    if (!biometricId) return;
    setMode('biometric');
    setError(null);
    const ok = await verifyBiometric(biometricId);
    if (ok) onVerified();
    else setMode('pin');
  };

  useEffect(() => {
    if (mode !== 'biometric' || started.current) return;
    started.current = true;
    void runBiometric();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (lockedOutUntil === null) return;
    const id = window.setInterval(() => {
      const t = Date.now();
      setNow(t);
      if (remainingLockoutMs(lockedOutUntil, t) <= 0) useUnhidePinStore.getState().expireLockout();
    }, 250);
    return () => window.clearInterval(id);
  }, [lockedOutUntil]);

  if (!config) return null;
  const length = unhidePinLength(config, appLock);

  const handleComplete = async (value: string) => {
    if (busy || lockedOut) return;
    setBusy(true);
    const ok = await verifyUnhidePin(value, config, appLock);
    setBusy(false);
    setPin('');
    if (!ok) {
      useUnhidePinStore.getState().registerFailure();
      setNow(Date.now());
      setError('Incorrect PIN');
      return;
    }
    onVerified();
  };

  if (mode === 'biometric') {
    return (
      <SecretDialogShell
        open
        onOpenChange={(open) => !open && close()}
        title={intent === 'toggle' ? 'Show amounts' : 'Confirm it’s you'}
        description="Use your fingerprint, face or device unlock."
      >
        <div className="space-y-4">
          <div className="text-primary flex justify-center py-4">
            <Fingerprint size={56} aria-hidden="true" />
          </div>
          <Button size="lg" variant="outline" className="w-full" onClick={() => setMode('pin')}>
            Use PIN instead
          </Button>
        </div>
      </SecretDialogShell>
    );
  }

  return (
    <SecretDialogShell
      open
      onOpenChange={(open) => !open && close()}
      title={intent === 'toggle' ? 'Show amounts' : 'Enter your PIN'}
      description={
        intent === 'toggle'
          ? 'Enter your PIN to reveal the amounts.'
          : 'Enter your current unhide PIN to continue.'
      }
    >
      <div className="space-y-4">
        <PinDots filled={pin.length} total={length} error={!!error} />
        <div className="flex min-h-5 items-center justify-center">
          {lockedOut ? (
            <SecretDialogError
              message={`Too many attempts. Try again in ${formatLockoutCountdown(remaining)}.`}
            />
          ) : (
            <SecretDialogError message={error} />
          )}
        </div>
        <PinPad
          value={pin}
          onChange={(v) => {
            if (error) setError(null);
            setPin(v);
          }}
          maxLength={length}
          onComplete={handleComplete}
          disabled={busy || lockedOut}
          surface="card"
        />
        {biometricId && isWebAuthnSupported() && (
          <button
            onClick={() => {
              started.current = true;
              void runBiometric();
            }}
            className="text-primary flex w-full items-center justify-center gap-1.5 text-sm underline"
          >
            <Fingerprint size={16} aria-hidden="true" />
            Use biometrics
          </button>
        )}
      </div>
    </SecretDialogShell>
  );
}

/**
 * First-time setup: a PIN, its confirmation, and the auto-hide switch on one screen. Finishing
 * saves the PIN and then performs the toggle the user originally asked for.
 */
function SetupDialog() {
  const appLock = useAppLockStore((s) => s.config);
  const lockEnabled = appLock?.enabled === true;
  const [length, setLength] = useState<number>(4);
  const [first, setFirst] = useState('');
  const [second, setSecond] = useState('');
  const [step, setStep] = useState<'first' | 'second' | 'applock'>('first');
  const [autoHide, setAutoHide] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [appLockPin, setAppLockPin] = useState('');
  // Null until the async probe resolves — the switch renders nothing until then.
  const [biometricAvailable, setBiometricAvailable] = useState<boolean | null>(null);
  const [useBiometric, setUseBiometric] = useState(true);
  const userName = useFinanceStore((s) => s.settings.userName);

  useEffect(() => {
    isPlatformAuthenticatorAvailable()
      .then(setBiometricAvailable)
      .catch(() => setBiometricAvailable(false));
  }, []);

  const close = () => useUnhidePinStore.getState().closeDialog();

  /** Registers the passkey when the switch is on. A failure just means PIN-only. */
  const enrollBiometric = async (): Promise<string | null> => {
    if (!biometricAvailable || !useBiometric) return null;
    try {
      return await registerBiometric(userName);
    } catch {
      toast.error('Could not set up biometrics — you can turn them on later in Settings');
      return null;
    }
  };

  /** The toggle the user came here for, applied after the PIN exists. */
  const finish = (message: string) => {
    if (useUnhidePinStore.getState().intent === 'toggle') {
      setAmountsHidden(!useFinanceStore.getState().settings.hideAmounts);
    }
    close();
    toast.success(message);
  };

  const save = async (pin: string) => {
    setBusy(true);
    const salt = generateSalt();
    const hash = await derivePinHash(pin, salt);
    const biometricCredentialId = await enrollBiometric();
    useUnhidePinStore.getState().setConfig({
      source: 'own',
      salt,
      hash,
      iterations: PIN_HASH_ITERATIONS,
      pinLength: pin.length,
      autoHideOnExit: autoHide,
      biometricCredentialId,
      createdAt: new Date().toISOString(),
    });
    setBusy(false);
    finish('Unhide PIN set');
  };

  const handleFirst = (value: string) => {
    setFirst(value);
    setStep('second');
  };

  const handleSecond = async (value: string) => {
    if (busy) return;
    if (value !== first) {
      setFirst('');
      setSecond('');
      setStep('first');
      setError("Those didn't match. Try again.");
      return;
    }
    await save(value);
  };

  const handleAppLockPin = async (value: string) => {
    if (busy || !appLock) return;
    setBusy(true);
    const ok = await verifyPin(value, appLock);
    setBusy(false);
    setAppLockPin('');
    if (!ok) {
      setError('Incorrect app lock PIN');
      return;
    }
    setBusy(true);
    const biometricCredentialId = await enrollBiometric();
    setBusy(false);
    useUnhidePinStore.getState().setConfig({
      source: 'applock',
      salt: '',
      hash: '',
      iterations: 0,
      pinLength: appLock.pinLength,
      autoHideOnExit: autoHide,
      biometricCredentialId,
      createdAt: new Date().toISOString(),
    });
    finish('Using your app lock PIN to show amounts');
  };

  const secure = isPinCryptoSupported();

  const options = (
    <div className="card-elevated divide-border divide-y rounded-md">
      {biometricAvailable && (
        <SwitchField
          className="p-3"
          icon={<Fingerprint size={18} className="text-muted-foreground shrink-0" />}
          title="Unlock with biometrics"
          description="Use your fingerprint or face first. The PIN is always one tap away."
          checked={useBiometric}
          onCheckedChange={setUseBiometric}
        />
      )}
      <SwitchField
        className="p-3"
        icon={<EyeOff size={18} className="text-muted-foreground shrink-0" />}
        title="Hide amounts on exit"
        description="Automatically hide amounts when you leave the app."
        checked={autoHide}
        onCheckedChange={setAutoHide}
      />
    </div>
  );

  return (
    <SecretDialogShell
      open
      onOpenChange={(open) => !open && close()}
      title={step === 'applock' ? 'Use app lock PIN' : 'Set a PIN to show amounts'}
      description={
        step === 'applock'
          ? 'Enter your app lock PIN to confirm.'
          : 'Hiding amounts is always one tap. Showing them again will ask for this PIN. It is a screen privacy gate, not encryption.'
      }
    >
      {!secure ? (
        <p className="text-muted-foreground text-sm">
          Needs a secure connection (https or localhost).
        </p>
      ) : step === 'applock' ? (
        <div className="space-y-4">
          <PinDots filled={appLockPin.length} total={appLock?.pinLength ?? 4} error={!!error} />
          <div className="flex min-h-5 items-center justify-center">
            <SecretDialogError message={error} />
          </div>
          <PinPad
            value={appLockPin}
            onChange={(v) => {
              if (error) setError(null);
              setAppLockPin(v);
            }}
            maxLength={appLock?.pinLength ?? 4}
            onComplete={handleAppLockPin}
            disabled={busy}
            surface="card"
          />
          {options}
          <button
            onClick={() => {
              setError(null);
              setStep('first');
            }}
            className="text-primary w-full text-center text-sm underline"
          >
            Choose a separate PIN instead
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-2">
            {PIN_LENGTH_OPTIONS.map((l) => (
              <button
                key={l}
                disabled={first.length > 0 || step === 'second'}
                onClick={() => setLength(l)}
                aria-pressed={length === l}
                className={`rounded-full py-2 text-sm font-medium transition-colors disabled:opacity-60 ${
                  length === l
                    ? 'bg-grad-primary shadow-glow-primary text-white'
                    : 'bg-muted hover:bg-muted/70'
                }`}
              >
                {l} digits
              </button>
            ))}
          </div>

          <div className="space-y-1.5">
            <p className="text-center text-xs font-medium">
              {step === 'first' ? 'Enter PIN' : 'Confirm PIN'}
            </p>
            <PinDots filled={first.length} total={length} error={!!error} />
            <PinDots filled={second.length} total={length} error={!!error} />
          </div>
          <div className="flex min-h-5 items-center justify-center">
            <SecretDialogError message={error} />
          </div>

          <PinPad
            value={step === 'first' ? first : second}
            onChange={(v) => {
              if (error) setError(null);
              if (step === 'first') setFirst(v);
              else setSecond(v);
            }}
            maxLength={length}
            onComplete={step === 'first' ? handleFirst : handleSecond}
            disabled={busy}
            surface="card"
          />

          {options}

          {lockEnabled && (
            <button
              onClick={() => {
                setError(null);
                setFirst('');
                setSecond('');
                setStep('applock');
              }}
              className="text-primary w-full text-center text-sm underline"
            >
              Use my app lock PIN instead
            </button>
          )}
        </div>
      )}
    </SecretDialogShell>
  );
}
