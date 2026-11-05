import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useAppLockStore } from '@/store/useAppLockStore';
import { useUnhidePinStore } from '@/store/useUnhidePinStore';
import { isUnhideProtected } from '@/utils/unhidePin';

/** Flips `hideAmounts` with no PIN check — only for the hide direction and for after a PIN. */
export function setAmountsHidden(hidden: boolean): void {
  useFinanceStore.getState().updateSettings({ hideAmounts: hidden });
}

/**
 * The single entry point for the eye button.
 *
 * Hiding is always free once a PIN exists. Revealing needs it. With no usable PIN (never set, or
 * pointed at an app lock that has since been turned off) either direction first opens the setup
 * prompt, which performs the toggle when it finishes.
 */
export function requestToggleHideAmounts(): void {
  const hidden = useFinanceStore.getState().settings.hideAmounts;
  const unhide = useUnhidePinStore.getState();
  const protectedNow = isUnhideProtected(unhide.config, useAppLockStore.getState().config);

  if (!protectedNow) {
    unhide.openDialog('setup');
    return;
  }
  if (hidden) {
    unhide.openDialog('verify');
    return;
  }
  setAmountsHidden(true);
}

export function notifyAmountsToggled(nowHidden: boolean): void {
  toast.success(nowHidden ? 'Amounts hidden' : 'Amounts visible');
}
