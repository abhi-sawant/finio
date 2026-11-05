import { useEffect, useState } from 'react';
import { ChevronRight, EyeOff, Fingerprint, KeyRound, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { isPlatformAuthenticatorAvailable, registerBiometric } from '@/services/appLockBiometric';
import { useAppLockStore } from '@/store/useAppLockStore';
import { useUnhidePinStore } from '@/store/useUnhidePinStore';
import { SwitchField } from '@/components/ui/switch';
import { isPinCryptoSupported } from '@/utils/pinCrypto';
import { isUnhideProtected } from '@/utils/unhidePin';

const rowClass = 'hover:bg-muted/50 flex w-full items-center justify-between p-4 transition-colors';

/** Settings → Security: the PIN that guards revealing amounts, and auto-hide on exit. */
export function UnhidePinSection() {
  const config = useUnhidePinStore((s) => s.config);
  const appLock = useAppLockStore((s) => s.config);
  const openDialog = useUnhidePinStore((s) => s.openDialog);
  const setAutoHide = useUnhidePinStore((s) => s.setAutoHideOnExit);
  const setBiometricId = useUnhidePinStore((s) => s.setBiometricCredentialId);
  const userName = useFinanceStore((s) => s.settings.userName);
  const [biometricAvailable, setBiometricAvailable] = useState(false);

  useEffect(() => {
    isPlatformAuthenticatorAvailable()
      .then(setBiometricAvailable)
      .catch(() => setBiometricAvailable(false));
  }, []);

  const handleToggleBiometric = async (next: boolean) => {
    if (!next) {
      setBiometricId(null);
      toast.success('Biometric unhide off');
      return;
    }
    try {
      setBiometricId(await registerBiometric(userName));
      toast.success('Biometric unhide is on');
    } catch {
      toast.error('Could not set up biometrics');
    }
  };

  if (!isPinCryptoSupported()) return null;
  const protectedNow = isUnhideProtected(config, appLock);

  return (
    <div className="card-elevated divide-border divide-y rounded-md">
      <div className="p-4">
        <p className="text-sm font-medium">Amount privacy</p>
        <p className="text-muted-foreground text-xs">
          {protectedNow
            ? config.source === 'applock'
              ? 'Showing hidden amounts asks for your app lock PIN.'
              : 'Showing hidden amounts asks for a PIN.'
            : 'Set a PIN so hidden amounts cannot be revealed with one tap.'}
        </p>
      </div>

      {protectedNow ? (
        <>
          {biometricAvailable && (
            <SwitchField
              className="p-4"
              icon={<Fingerprint size={18} className="text-muted-foreground shrink-0" />}
              title="Unlock with biometrics"
              description="Use your fingerprint or face first when showing amounts. The PIN always still works."
              checked={!!config.biometricCredentialId}
              onCheckedChange={handleToggleBiometric}
            />
          )}
          <SwitchField
            className="p-4"
            icon={<EyeOff size={18} className="text-muted-foreground shrink-0" />}
            title="Hide amounts on exit"
            description="Automatically hide amounts when you leave the app."
            checked={config.autoHideOnExit}
            onCheckedChange={setAutoHide}
          />
          <button onClick={() => openDialog('verify', 'change')} className={rowClass}>
            <div className="flex items-center gap-3">
              <KeyRound size={18} className="text-muted-foreground" />
              <span className="text-sm font-medium">Change unhide PIN</span>
            </div>
            <ChevronRight size={16} className="text-muted-foreground" />
          </button>
          <button
            onClick={() => openDialog('verify', 'remove')}
            className="hover:bg-muted/50 text-destructive flex w-full items-center gap-3 p-4 transition-colors"
          >
            <Trash2 size={18} className="shrink-0" />
            <span className="text-sm font-medium">Remove unhide PIN</span>
          </button>
        </>
      ) : (
        <button onClick={() => openDialog('setup', 'change')} className={rowClass}>
          <div className="flex items-center gap-3">
            <KeyRound size={18} className="text-muted-foreground" />
            <span className="text-sm font-medium">Set unhide PIN</span>
          </div>
          <ChevronRight size={16} className="text-muted-foreground" />
        </button>
      )}
    </div>
  );
}
