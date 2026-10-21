import { useFinanceStore } from '@/store/useFinanceStore';

/** True in expense-tracker mode, where no account balance is maintained or shown. */
export function useTrackerMode(): boolean {
  return useFinanceStore((s) => s.settings.trackingMode === 'expenses');
}
