import { useId, useMemo, useState } from 'react';
import { formatCurrency, todayKey } from '@/utils/formatters';
import { maxPrepayment, simulatePrepaymentImpact, type LoanScheduleInput } from '@/utils/loan';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { DatePicker } from '@/components/ui/date-picker';

interface PrepayCalculatorProps {
  loan: LoanScheduleInput;
  hideAmounts: boolean;
  onRecord: () => void;
}

/**
 * "What if I prepay?" — a read-only simulation against the loan's real schedule. It records
 * nothing; the real prepayment flow lives on the Loans page.
 */
export function PrepayCalculator({ loan, hideAmounts, onRecord }: PrepayCalculatorProps) {
  const amountId = useId();
  const messageId = useId();
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(todayKey());

  const limit = useMemo(() => maxPrepayment(loan), [loan]);
  const parsed = parseFloat(amount) || 0;
  const overLimit = parsed > limit + 0.005;

  const impact = useMemo(() => {
    if (parsed <= 0 || overLimit || !date) return null;
    return simulatePrepaymentImpact(loan, {
      amount: parsed,
      date: new Date(`${date}T00:00:00`).toISOString(),
    });
  }, [loan, parsed, overLimit, date]);

  return (
    <div className="card-elevated rounded-md p-4">
      <h2 className="text-base font-semibold">What if I prepay?</h2>
      <p className="text-muted-foreground mt-0.5 text-xs">
        See how an extra payment shortens this loan.
      </p>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor={amountId} className="text-muted-foreground text-xs">
            Extra amount
          </Label>
          <Input
            id={amountId}
            type="text"
            inputMode="decimal"
            placeholder="e.g. 50000"
            value={amount}
            aria-invalid={overLimit || undefined}
            aria-describedby={messageId}
            onChange={(e) => {
              const v = e.target.value.replace(/[^\d.]/g, '');
              // one decimal point, at most two paise digits
              if (/^\d*(\.\d{0,2})?$/.test(v)) setAmount(v);
            }}
          />
        </div>
        <div className="space-y-1.5">
          <span className="text-muted-foreground block text-xs font-medium">Paid on</span>
          <DatePicker value={date} onChange={setDate} />
        </div>
      </div>

      <div id={messageId} aria-live="polite" className="mt-3 text-sm">
        {overLimit ? (
          <p className="text-destructive text-xs">
            Only {formatCurrency(limit, false, hideAmounts)} is still owed — enter that or less.
          </p>
        ) : impact ? (
          impact.monthsSaved > 0 || impact.interestSaved > 0 ? (
            <p className="text-balance">
              You'd finish{' '}
              <span className="font-semibold">
                {impact.monthsSaved} month{impact.monthsSaved === 1 ? '' : 's'} earlier
              </span>{' '}
              and save{' '}
              <span className="text-positive font-semibold">
                {formatCurrency(impact.interestSaved, false, hideAmounts, { precise: false })}
              </span>{' '}
              in interest.
            </p>
          ) : (
            <p className="text-muted-foreground">
              That date falls after the loan ends, so it wouldn't change anything.
            </p>
          )
        ) : (
          <p className="text-muted-foreground text-xs">
            Up to {formatCurrency(limit, false, hideAmounts)} — what's still owed today.
          </p>
        )}
      </div>

      <div className="border-border mt-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 border-t pt-3">
        <p className="text-muted-foreground text-xs">This is a preview — nothing is recorded.</p>
        <button
          type="button"
          onClick={onRecord}
          className="text-primary -mx-2 px-2 py-2 text-xs font-medium hover:underline"
        >
          Record a prepayment
        </button>
      </div>
    </div>
  );
}
