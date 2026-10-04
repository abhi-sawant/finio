import { Eye, EyeOff } from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { HeaderIconButton } from '@/components/ui/header-icon-button';

/** Header icon button that masks every rendered amount behind dots — the app's privacy toggle. */
export function HideAmountsToggle() {
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const updateSettings = useFinanceStore((s) => s.updateSettings);

  return (
    <HeaderIconButton
      onClick={() => updateSettings({ hideAmounts: !hideAmounts })}
      aria-label={hideAmounts ? 'Show amounts' : 'Hide amounts'}
      pressed={hideAmounts}
      aria-pressed={hideAmounts}
    >
      {hideAmounts ? <EyeOff /> : <Eye />}
    </HeaderIconButton>
  );
}
