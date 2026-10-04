import { format } from 'date-fns';
import { formatCurrency, formatShortDate } from '@/utils/formatters';
import {
  DEPOSIT_COMPOUNDING_OPTIONS,
  depositInvested,
  depositMaturityAmount,
  depositMaturityDate,
  rdInstallmentsOnOrBefore,
} from '@/utils/deposit';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { DatePicker } from '@/components/ui/date-picker';
import { SwitchField } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { Account, DepositCompounding } from '@/types';
import { depositTermsFromForm, type DepositFormValues } from './depositForm';

interface DepositFieldsProps {
  type: 'fd' | 'rd';
  values: DepositFormValues;
  onChange: (patch: Partial<DepositFormValues>) => void;
  /** Accounts that can fund the deposit and receive its payout. */
  linkableAccounts: Account[];
  /** Editing an existing deposit: amount, start, tenure and linked account are fixed. */
  locked: boolean;
  hideAmounts: boolean;
}

const fieldLabel = 'text-muted-foreground mb-1.5 block text-xs font-medium';

export function DepositFields({
  type,
  values,
  onChange,
  linkableAccounts,
  locked,
  hideAmounts,
}: DepositFieldsProps) {
  const isFd = type === 'fd';
  const terms = depositTermsFromForm(type, values);
  const preview = terms ? { type, deposit: terms } : null;
  const maturity = preview ? depositMaturityDate(preview) : null;
  const maturityAmount = preview ? depositMaturityAmount(preview) : 0;
  const invested = preview ? depositInvested(preview) : 0;
  const linkedName = linkableAccounts.find((a) => a.id === values.linkedAccountId)?.name;

  // Installments that already fell due before today — only matters for a new RD.
  const pastInstallments =
    !isFd && !locked && terms ? rdInstallmentsOnOrBefore({ ...terms }, new Date()) : 0;

  // A new FD dated before today — the funding transfer is optional for those.
  const fdStartedInPast =
    isFd && !locked && !!values.startDate && values.startDate < format(new Date(), 'yyyy-MM-dd');

  const money = (n: number) => formatCurrency(n, false, hideAmounts);

  return (
    <>
      <div>
        <Label className={fieldLabel}>{isFd ? 'Amount' : 'Monthly Installment'}</Label>
        {locked ? (
          <p className="bg-muted border-border flex h-10 items-center rounded-sm border px-3 text-base md:text-sm">
            {money(parseFloat(values.amount) || 0)}
          </p>
        ) : (
          <NumberPad value={values.amount} onChange={(amount) => onChange({ amount })} />
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <Label htmlFor="depositRate" className={fieldLabel}>
            Interest Rate (% p.a.)
          </Label>
          <Input
            id="depositRate"
            type="number"
            inputMode="decimal"
            min={0}
            step={0.01}
            placeholder="e.g. 6.65"
            value={values.interestRate}
            onChange={(e) => onChange({ interestRate: e.target.value })}
          />
        </div>
        {isFd ? (
          <div>
            <Label className={fieldLabel}>Compounding</Label>
            <Select
              value={values.compounding}
              onValueChange={(v) => v && onChange({ compounding: v as DepositCompounding })}
            >
              <SelectTrigger className="w-full">
                <SelectValue>
                  {DEPOSIT_COMPOUNDING_OPTIONS.find((o) => o.value === values.compounding)?.label}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                {DEPOSIT_COMPOUNDING_OPTIONS.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        ) : (
          <div>
            <Label htmlFor="depositTenure" className={fieldLabel}>
              Period (months)
            </Label>
            <Input
              id="depositTenure"
              type="number"
              inputMode="numeric"
              min={1}
              placeholder="e.g. 36"
              value={values.tenureMonths}
              disabled={locked}
              onChange={(e) => onChange({ tenureMonths: e.target.value })}
            />
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <Label className={fieldLabel}>{isFd ? 'Investment Date' : 'First Installment'}</Label>
          <DatePicker
            value={values.startDate}
            onChange={(startDate) => onChange({ startDate })}
            disabled={locked}
          />
        </div>
        {isFd && (
          <div>
            <Label className={fieldLabel}>Maturity Date</Label>
            <DatePicker
              value={values.maturityDate}
              onChange={(maturityDate) => onChange({ maturityDate })}
            />
          </div>
        )}
      </div>
      {!isFd && (
        <p className="text-muted-foreground -mt-2 text-[10px]">
          The installment is deducted on this day every month.
        </p>
      )}

      <div>
        <Label className={fieldLabel}>
          {isFd ? 'Funded From & Redeemed To' : 'Deduct From & Pay Out To'}
        </Label>
        {locked ? (
          <p className="bg-muted border-border flex h-10 items-center rounded-sm border px-3 text-base md:text-sm">
            {linkedName ?? 'Unknown account'}
          </p>
        ) : linkableAccounts.length === 0 ? (
          <p className="text-muted-foreground text-xs">
            Add a bank account first — a deposit needs one to fund it and receive the payout.
          </p>
        ) : (
          <Select
            value={values.linkedAccountId}
            onValueChange={(v) => onChange({ linkedAccountId: v ?? '' })}
          >
            <SelectTrigger className="w-full">
              <SelectValue>{linkedName ?? 'Choose account'}</SelectValue>
            </SelectTrigger>
            <SelectContent>
              {linkableAccounts.map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {a.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {pastInstallments > 0 && (
        <div className="card-elevated rounded-md p-4">
          <SwitchField
            title="Deduct past installments from account"
            description={
              values.deductPast
                ? `${pastInstallments} installment${pastInstallments === 1 ? '' : 's'} (${money(pastInstallments * (terms?.amount ?? 0))}) will be posted from ${linkedName ?? 'the account'} and show in its history.`
                : `${pastInstallments} installment${pastInstallments === 1 ? '' : 's'} (${money(pastInstallments * (terms?.amount ?? 0))}) already paid become the RD's opening balance.`
            }
            checked={values.deductPast}
            onCheckedChange={(deductPast) => onChange({ deductPast })}
          />
        </div>
      )}

      {isFd && fdStartedInPast && (
        <div className="card-elevated rounded-md p-4">
          <SwitchField
            title="Deduct amount from account"
            description={
              values.deductPast
                ? `${money(terms?.amount ?? 0)} will be transferred from ${linkedName ?? 'the account'} on the start date and show in its history.`
                : `${money(terms?.amount ?? 0)} was already invested; it becomes the FD's opening balance and ${linkedName ?? 'the account'} is left untouched.`
            }
            checked={values.deductPast}
            onCheckedChange={(deductPast) => onChange({ deductPast })}
          />
        </div>
      )}

      {preview && maturity && (
        <div className="card-elevated rounded-md p-4">
          <div className="grid grid-cols-3 gap-2 text-center">
            <div>
              <p className="text-muted-foreground text-xs font-medium">Invested</p>
              <p className="text-sm font-semibold">{money(invested)}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs font-medium">Interest</p>
              <p className="text-sm font-semibold">{money(maturityAmount - invested)}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs font-medium">At maturity</p>
              <p className="text-positive text-sm font-semibold">{money(maturityAmount)}</p>
            </div>
          </div>
          <p className="text-muted-foreground mt-3 text-center text-xs">
            Matures {formatShortDate(maturity)} — paid into {linkedName ?? 'the account'}{' '}
            automatically. Any TDS on the interest can be logged as an expense.
          </p>
        </div>
      )}
    </>
  );
}
