import { useEffect, useLayoutEffect } from 'react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useUnhidePinStore } from '@/store/useUnhidePinStore';
import { setAmountsHidden } from '@/services/hideAmounts';

/**
 * "Hide amounts when I leave the app".
 *
 * Mounted from `App()` beside `useAutoLock` for the same reason (AppRoutes sits inside a
 * `<Suspense>` that can tear effects down). Two triggers: the page going hidden, and — because a
 * killed tab never gets to say it was hidden — the first render after the finance store has
 * hydrated. The second is a layout effect so the amounts are masked before the first paint.
 */
export function useAutoHide(): void {
  const enabled = useUnhidePinStore((s) => s.config?.autoHideOnExit === true);
  const hydrated = useFinanceStore((s) => s.isHydrated);

  useLayoutEffect(() => {
    if (!hydrated || !enabled) return;
    if (!useFinanceStore.getState().settings.hideAmounts) setAmountsHidden(true);
    // Once per hydration, not whenever the preference flips — turning the option on in Settings
    // should not itself blank the screen.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hydrated]);

  useEffect(() => {
    if (!enabled) return;

    const hide = () => {
      if (!useFinanceStore.getState().settings.hideAmounts) setAmountsHidden(true);
    };
    const onVisibilityChange = () => {
      if (document.visibilityState === 'hidden') hide();
    };

    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('pagehide', hide);
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('pagehide', hide);
    };
  }, [enabled]);
}
