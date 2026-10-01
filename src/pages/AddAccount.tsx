import { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import {
  ArrowLeft,
  Trash2,
  Landmark,
  PiggyBank,
  Banknote,
  CreditCard,
  TrendingUp,
  Wallet,
  Scale,
  Vault,
  CalendarClock,
  type LucideIcon,
} from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { COLOR_PALETTE } from '@/data/colorPalette';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { useConfirm } from '@/components/ui/use-confirm';
import { ReconcileAccountDialog } from '@/components/accounts/ReconcileAccountDialog';
import { DepositFields } from '@/components/accounts/DepositFields';
import {
  depositFormFromAccount,
  depositTermsFromForm,
  type DepositFormValues,
} from '@/components/accounts/depositForm';
import { activeAccounts, isLiquidAccount } from '@/utils/calculations';
import { accountDeleteBlockers, isDepositAccount } from '@/utils/deposit';
import type { AccountType } from '@/types';
import Header from '@/components/ui/header';
import Main from '@/components/ui/main';

const TYPE_ICONS: Record<string, LucideIcon> = {
  landmark: Landmark,
  'piggy-bank': PiggyBank,
  banknote: Banknote,
  'credit-card': CreditCard,
  'trending-up': TrendingUp,
  wallet: Wallet,
  vault: Vault,
  'calendar-clock': CalendarClock,
};

const accountTypes: { value: AccountType; label: string; icon: string }[] = [
  { value: 'checking', label: 'Checking', icon: 'landmark' },
  { value: 'savings', label: 'Savings', icon: 'piggy-bank' },
  { value: 'cash', label: 'Cash', icon: 'banknote' },
  { value: 'credit', label: 'Credit Card', icon: 'credit-card' },
  { value: 'investment', label: 'Investment', icon: 'trending-up' },
  { value: 'wallet', label: 'Wallet', icon: 'wallet' },
  { value: 'fd', label: 'Fixed Deposit', icon: 'vault' },
  { value: 'rd', label: 'Recurring Deposit', icon: 'calendar-clock' },
];

const accountColors = COLOR_PALETTE;

export default function AddAccount() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const { id } = useParams();
  const accounts = useFinanceStore((s) => s.accounts);
  const addAccount = useFinanceStore((s) => s.addAccount);
  const updateAccount = useFinanceStore((s) => s.updateAccount);
  const deleteAccount = useFinanceStore((s) => s.deleteAccount);
  const addDeposit = useFinanceStore((s) => s.addDeposit);
  const updateDeposit = useFinanceStore((s) => s.updateDeposit);
  const processRecurring = useFinanceStore((s) => s.processRecurring);
  const bulkDeleteTransactions = useFinanceStore((s) => s.bulkDeleteTransactions);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const existing = id ? accounts.find((a) => a.id === id) : null;

  const [name, setName] = useState(existing?.name ?? '');
  const [type, setType] = useState<AccountType>(existing?.type ?? 'checking');
  const [balance, setBalance] = useState(
    existing?.type === 'credit' ? '0' : (existing?.balance?.toString() ?? '0'),
  );
  const [due, setDue] = useState(
    existing?.type === 'credit' ? Math.abs(existing.balance).toString() : '0',
  );
  /**
   * `balance`/`due` only hold what the user typed. Until they type, the form follows the store,
   * so a reconcile adjustment (or its Undo) posted while this screen is open is reflected —
   * otherwise Update would write the stale mount-time figure back and shift `openingBalance`.
   */
  const [balanceDirty, setBalanceDirty] = useState(false);
  const followStore = !!existing && !balanceDirty && existing.type === type;
  const shownBalance = followStore && type !== 'credit' ? existing.balance.toString() : balance;
  const shownDue = followStore && type === 'credit' ? Math.abs(existing.balance).toString() : due;
  const [color, setColor] = useState(existing?.color ?? accountColors[0]);
  const [creditLimit, setCreditLimit] = useState(existing?.creditLimit?.toString() ?? '0');
  const [statementCloseDay, setStatementCloseDay] = useState(
    existing?.statementCloseDay?.toString() ?? '',
  );
  const [paymentDueDays, setPaymentDueDays] = useState(existing?.paymentDueDays?.toString() ?? '');
  const [minimumDuePercent, setMinimumDuePercent] = useState(
    existing?.minimumDuePercent?.toString() ?? '',
  );
  const [showReconcile, setShowReconcile] = useState(false);

  // Spendable accounts only — a deposit can't fund another, and a card can't be redeemed into.
  // An existing deposit keeps showing its linked account even if that one has since closed.
  const linkableAccounts = useMemo(
    () =>
      accounts.filter(
        (a) => isLiquidAccount(a) && (!a.archivedAt || a.id === existing?.deposit?.linkedAccountId),
      ),
    [accounts, existing],
  );
  const [depositForm, setDepositForm] = useState<DepositFormValues>(() =>
    depositFormFromAccount(existing, activeAccounts(accounts).filter(isLiquidAccount)[0]?.id ?? ''),
  );
  const updateDepositForm = (patch: Partial<DepositFormValues>) =>
    setDepositForm((prev) => ({ ...prev, ...patch }));

  const isDepositType = type === 'fd' || type === 'rd';
  const depositTerms = isDepositType
    ? depositTermsFromForm(type as 'fd' | 'rd', depositForm)
    : null;
  const canSubmit = Boolean(name.trim()) && (!isDepositType || depositTerms !== null);
  // Converting between a deposit and a regular account would orphan its terms or its history.
  const typeOptions = !existing
    ? accountTypes
    : isDepositAccount(existing)
      ? accountTypes.filter((t) => t.value === existing.type)
      : accountTypes.filter((t) => !isDepositAccount({ type: t.value }));

  const submitDeposit = () => {
    if (!depositTerms) return;
    if (existing) {
      updateDeposit(existing.id, {
        name: name.trim(),
        color,
        interestRate: depositTerms.interestRate,
        compounding: depositTerms.compounding,
        maturityDate: depositTerms.maturityDate,
      });
      navigate(-1);
      return;
    }
    addDeposit({
      type: type as 'fd' | 'rd',
      name: name.trim(),
      color,
      terms: depositTerms,
      deductPast: depositForm.deductPast,
    });
    if (type === 'rd' && depositForm.deductPast) {
      const posted = processRecurring();
      if (posted.length > 0) {
        const ids = posted.map((t) => t.id);
        toast.success(`Posted ${posted.length} past installment${posted.length === 1 ? '' : 's'}`, {
          action: { label: 'Undo', onClick: () => bulkDeleteTransactions(ids) },
        });
      }
    }
    navigate(-1);
  };

  const handleSubmit = () => {
    if (!canSubmit) return;
    if (isDepositType) {
      submitDeposit();
      return;
    }

    const isCredit = type === 'credit';
    const data = {
      name: name.trim(),
      type,
      balance: isCredit ? -(parseFloat(shownDue) || 0) : parseFloat(shownBalance) || 0,
      color,
      icon: existing?.icon ?? accountTypes.find((t) => t.value === type)?.icon ?? 'landmark',
      creditLimit: isCredit ? parseFloat(creditLimit) || undefined : undefined,
      statementCloseDay:
        isCredit && statementCloseDay.trim()
          ? Math.min(28, Math.max(1, parseInt(statementCloseDay, 10)))
          : undefined,
      paymentDueDays:
        isCredit && paymentDueDays.trim() ? Math.max(0, parseInt(paymentDueDays, 10)) : undefined,
      minimumDuePercent:
        isCredit && minimumDuePercent.trim()
          ? Math.max(0, parseFloat(minimumDuePercent))
          : undefined,
    };

    if (existing) {
      updateAccount(existing.id, data);
    } else {
      addAccount(data);
    }
    navigate(-1);
  };

  const handleDelete = async () => {
    if (!existing) return;
    const blockers = accountDeleteBlockers(accounts, existing.id);
    if (blockers.length > 0) {
      toast.error(`Can't delete "${existing.name}"`, {
        description: `${blockers.map((b) => `"${b}"`).join(', ')} pay${blockers.length === 1 ? 's' : ''} out to this account. Delete ${blockers.length === 1 ? 'that deposit' : 'those deposits'} first.`,
      });
      return;
    }
    const confirmed = await confirm({
      title: `Delete "${existing.name}"?`,
      description: isDepositAccount(existing)
        ? 'Every transaction on this deposit will be deleted, including the money moved into it — that money returns to the linked account. This cannot be undone.'
        : 'Every transaction on this account will be deleted as well. This cannot be undone.',
      confirmLabel: 'Delete',
    });
    if (confirmed) {
      deleteAccount(existing.id);
      navigate(-1);
    }
  };

  return (
    <>
      {/* Header */}
      <Header innerClassName="lg:max-w-xl">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="h-9 w-9">
          <ArrowLeft size={20} />
        </Button>
        <h1 className="text-base font-semibold">{existing ? 'Edit Account' : 'Add Account'}</h1>
        {existing ? (
          <Button
            variant="ghost"
            size="icon"
            onClick={handleDelete}
            className="text-destructive h-9 w-9"
          >
            <Trash2 size={18} />
          </Button>
        ) : (
          <div className="w-9" />
        )}
      </Header>

      <Main className="lg:max-w-xl">
        {/* Name */}
        <div>
          <Label
            htmlFor="accountName"
            className="text-muted-foreground mb-1.5 block text-xs font-medium"
          >
            Account Name
          </Label>
          <Input
            id="accountName"
            type="text"
            placeholder="e.g., HDFC Savings"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="bg-card h-auto rounded-sm px-4 py-3"
          />
        </div>

        {/* Type */}
        <div>
          <Label
            htmlFor="accountType"
            className="text-muted-foreground mb-1.5 block text-xs font-medium"
          >
            Account Type
          </Label>
          <div className="grid grid-cols-3 gap-2">
            {typeOptions.map((t) => (
              <button
                key={t.value}
                onClick={() => setType(t.value)}
                className={`rounded-sm border p-3 text-center transition-colors ${
                  type === t.value ? 'border-primary bg-primary/10' : 'border-border bg-card'
                }`}
              >
                {(() => {
                  const Icon = TYPE_ICONS[t.icon];
                  return Icon ? (
                    <Icon size={20} className="mx-auto mb-1" />
                  ) : (
                    <span className="mb-1 block text-lg">{t.icon}</span>
                  );
                })()}
                <span className="text-xs">{t.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Deposit terms replace the balance — the deposit is funded by real transfers */}
        {isDepositType ? (
          <DepositFields
            type={type as 'fd' | 'rd'}
            values={depositForm}
            onChange={updateDepositForm}
            linkableAccounts={linkableAccounts}
            locked={Boolean(existing)}
            hideAmounts={hideAmounts}
          />
        ) : type === 'credit' ? (
          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Current Due
            </Label>
            <NumberPad
              value={shownDue}
              onChange={(v) => {
                setBalanceDirty(true);
                setDue(v);
              }}
            />
          </div>
        ) : (
          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Current Balance
            </Label>
            <NumberPad
              value={shownBalance}
              onChange={(v) => {
                setBalanceDirty(true);
                setBalance(v);
              }}
            />
          </div>
        )}

        {/* Credit Limit */}
        {type === 'credit' && (
          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Credit Limit
            </Label>
            <NumberPad value={creditLimit} onChange={setCreditLimit} />
          </div>
        )}

        {/* Statement cycle — optional, unlocks the Dashboard payment-due card */}
        {type === 'credit' && (
          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Statement Cycle (optional)
            </Label>
            <div className="grid grid-cols-3 gap-2">
              <div>
                <Label
                  htmlFor="statementCloseDay"
                  className="text-muted-foreground mb-1 block text-[10px]"
                >
                  Closes on
                </Label>
                <Input
                  id="statementCloseDay"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={28}
                  placeholder="e.g. 5"
                  value={statementCloseDay}
                  onChange={(e) => setStatementCloseDay(e.target.value)}
                  className="bg-card h-auto rounded-sm px-3 py-2.5"
                />
              </div>
              <div>
                <Label
                  htmlFor="paymentDueDays"
                  className="text-muted-foreground mb-1 block text-[10px]"
                >
                  Due after (days)
                </Label>
                <Input
                  id="paymentDueDays"
                  type="number"
                  inputMode="numeric"
                  min={0}
                  placeholder="e.g. 20"
                  value={paymentDueDays}
                  onChange={(e) => setPaymentDueDays(e.target.value)}
                  className="bg-card h-auto rounded-sm px-3 py-2.5"
                />
              </div>
              <div>
                <Label
                  htmlFor="minimumDuePercent"
                  className="text-muted-foreground mb-1 block text-[10px]"
                >
                  Min due %
                </Label>
                <Input
                  id="minimumDuePercent"
                  type="number"
                  inputMode="decimal"
                  min={0}
                  max={100}
                  placeholder="5"
                  value={minimumDuePercent}
                  onChange={(e) => setMinimumDuePercent(e.target.value)}
                  className="bg-card h-auto rounded-sm px-3 py-2.5"
                />
              </div>
            </div>
            <p className="text-muted-foreground mt-1.5 text-[10px]">
              Set a close day and due offset to see a "payment due" reminder on the Dashboard.
            </p>
          </div>
        )}

        {/* Color */}
        <div>
          <Label
            htmlFor="accountColor"
            className="text-muted-foreground mb-1.5 block text-xs font-medium"
          >
            Color
          </Label>
          <div className="flex flex-wrap gap-4">
            {accountColors.map((c) => (
              <button
                key={c}
                onClick={() => setColor(c)}
                className={`h-8 w-8 rounded-full transition-transform ${
                  color === c ? 'ring-primary scale-110 ring-1 ring-offset-1' : ''
                }`}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </div>

        {/* Submit */}
        <Button
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="bg-grad-primary shadow-glow-primary h-auto w-full rounded-sm py-3.5 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
        >
          {existing ? 'Update Account' : 'Add Account'}
        </Button>

        {existing && !isDepositAccount(existing) && (
          <Button
            variant="secondary"
            onClick={() => setShowReconcile(true)}
            className="bg-muted text-muted-foreground h-auto w-full gap-2 rounded-sm py-3 text-sm font-medium"
          >
            <Scale size={16} />
            Reconcile Balance
          </Button>
        )}
      </Main>

      {existing && (
        <ReconcileAccountDialog
          account={existing}
          open={showReconcile}
          onOpenChange={setShowReconcile}
        />
      )}
    </>
  );
}
