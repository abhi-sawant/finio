import { useMemo, useState } from 'react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { sumTransactionDeltas } from '@/store/balance';
import { activeAccounts } from '@/utils/calculations';
import { isDepositAccount } from '@/utils/deposit';
import { formatCurrency } from '@/utils/formatters';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface SwitchToFullDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * Leaving transactions-only mode: ask what each account really holds today, so its opening
 * balance can be set to `current − Σ transactions` and the books come out straight. Deposits are
 * left out — they are funded by transfers, so their balance is already right.
 */
export function SwitchToFullDialog({ open, onOpenChange }: SwitchToFullDialogProps) {
  const accounts = useFinanceStore((s) => s.accounts);
  const transactions = useFinanceStore((s) => s.transactions);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const switchToFullMode = useFinanceStore((s) => s.switchToFullMode);

  const [values, setValues] = useState<Record<string, string>>({});

  const rows = useMemo(() => {
    const deltas = sumTransactionDeltas(transactions);
    return activeAccounts(accounts)
      .filter((a) => !isDepositAccount(a))
      .map((account) => ({ account, logged: deltas.get(account.id) ?? 0 }));
  }, [accounts, transactions]);

  const handleOpenChange = (next: boolean) => {
    if (!next) setValues({});
    onOpenChange(next);
  };

  const handleConfirm = () => {
    const balances: Record<string, number> = {};
    for (const { account } of rows) {
      const parsed = parseFloat(values[account.id] ?? '');
      if (!Number.isFinite(parsed)) continue;
      // A card's figure is what is owed, stored as a negative balance.
      balances[account.id] = account.type === 'credit' ? -Math.abs(parsed) : parsed;
    }
    switchToFullMode(balances);
    handleOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Set your current balances</DialogTitle>
          <DialogDescription>
            Finio wasn&rsquo;t tracking balances, so it needs a starting point. Enter what each
            account holds right now and the opening balance is worked out from your transactions.
            Leave one blank to keep it as it is.
          </DialogDescription>
        </DialogHeader>

        <div className="max-h-[50dvh] space-y-4 overflow-y-auto">
          {rows.map(({ account, logged }) => {
            const isCredit = account.type === 'credit';
            return (
              <div key={account.id}>
                <Label
                  htmlFor={`balance-${account.id}`}
                  className="mb-1.5 block text-sm font-medium"
                >
                  {account.name}
                  <span className="text-muted-foreground ml-1.5 text-xs font-normal">
                    {isCredit ? 'amount owed' : 'balance now'}
                  </span>
                </Label>
                <Input
                  id={`balance-${account.id}`}
                  type="text"
                  inputMode="decimal"
                  placeholder="0"
                  value={values[account.id] ?? ''}
                  onChange={(e) =>
                    setValues((prev) => ({
                      ...prev,
                      [account.id]: e.target.value.replace(/[^0-9.-]/g, ''),
                    }))
                  }
                />
                <p className="text-muted-foreground mt-1 text-xs">
                  Transactions logged so far net {logged < 0 ? '−' : '+'}
                  {formatCurrency(Math.abs(logged), false, hideAmounts)}
                </p>
              </div>
            );
          })}
        </div>

        <div className="flex gap-2">
          <Button onClick={handleConfirm} className="flex-1">
            Switch to full tracking
          </Button>
          <Button variant="secondary" onClick={() => handleOpenChange(false)}>
            Cancel
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
