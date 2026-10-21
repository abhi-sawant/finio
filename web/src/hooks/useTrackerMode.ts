import { useFinanceStore } from '@/store/useFinanceStore';

/** True in transactions-only mode, where no account balance is maintained or shown. */
export function useTrackerMode(): boolean {
  return useFinanceStore((s) => s.settings.trackingMode === 'transactions');
}
