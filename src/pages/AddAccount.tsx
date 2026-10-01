import { useMemo, useRef, useState } from 'react';
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
import { formatInputAmount } from '@/utils/formatters';
import { COLOR_PALETTE } from '@/data/colorPalette';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { MAX_NAME_LENGTH, cleanText, stripLeading } from '@/utils/validation';
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
import { HeaderIconButton, HeaderIconSpacer } from '@/components/ui/header-icon-button';
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
  const [creditField, setCreditField] = useState<'due' | 'limit'>('due');

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
  const submitting = useRef(false);
  const isDuplicateName = () => {
    const key = name.trim().toLowerCase();
    return accounts.some(
      (a) => a.id !== existing?.id && !a.archivedAt && a.name.trim().toLowerCase() === key,
    );
  };
  // Converting between a deposit and a regular account would orphan its terms or its history.
  const typeOptions = !existing
    ? accountTypes
    : isDepositAccount(existing)
      ? accountTypes.filter((t) => t.value === existing.type)
      : accountTypes.filter((t) => !isDepositAccount({ type: t.value }));

  const submitDeposit = () => {
    if (!depositTerms) return;
    submitting.current = true;
    if (existing) {
      updateDeposit(existing.id, {
        name: cleanText(name, MAX_NAME_LENGTH),
        color,
        interestRate: depositTerms.interestRate,
        compounding: depositTerms.compounding,
        maturityDate: depositTerms.maturityDate,
      });
      toast.success('Account updated');
      navigate(-1);
      return;
    }
    addDeposit({
      type: type as 'fd' | 'rd',
      name: cleanText(name, MAX_NAME_LENGTH),
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
    toast.success('Account added');
    navigate(-1);
  };

  const handleSubmit = () => {
    if (submitting.current) return;
    if (!name.trim()) {
      toast.error('Enter a name');
      return;
    }
    if (isDuplicateName()) {
      toast.error('An account with this name already exists');
      return;
    }
    if (isDepositType) {
      if (!depositTerms) {
        toast.error(
          type === 'fd'
            ? 'Enter the amount, rate, linked account, and a maturity date after the start date'
            : 'Enter the installment, rate, linked account, and tenure in months',
        );
        return;
      }
      submitDeposit();
      return;
    }
    submitting.current = true;

    const isCredit = type === 'credit';
    const data = {
      name: cleanText(name, MAX_NAME_LENGTH),
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
      toast.success('Account updated');
    } else {
      addAccount(data);
      toast.success('Account added');
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
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">{existing ? 'Edit Account' : 'Add Account'}</h1>
        {existing ? (
          <HeaderIconButton onClick={handleDelete} aria-label="Delete" tone="destructive">
            <Trash2 />
          </HeaderIconButton>
        ) : (
          <HeaderIconSpacer />
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
            maxLength={MAX_NAME_LENGTH}
            onChange={(e) => setName(stripLeading(e.target.value))}
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
          <div className="space-y-2">
            <div className="bg-muted grid grid-cols-2 gap-1 rounded-sm p-1">
              {(
                [
                  { key: 'due', label: 'Current Due', value: shownDue },
                  { key: 'limit', label: 'Credit Limit', value: creditLimit },
                ] as const
              ).map((f) => (
                <button
                  key={f.key}
                  type="button"
                  onClick={() => setCreditField(f.key)}
                  aria-pressed={creditField === f.key}
                  className={`rounded-sm px-2 py-1.5 text-center transition-all ${
                    creditField === f.key
                      ? 'bg-primary text-primary-foreground shadow'
                      : 'text-muted-foreground'
                  }`}
                >
                  <span className="block text-[10px] font-medium">{f.label}</span>
                  <span className="block text-sm font-semibold">
                    {formatInputAmount(f.value) || '0'}
                  </span>
                </button>
              ))}
            </div>
            {creditField === 'due' ? (
              <NumberPad
                value={shownDue}
                onChange={(v) => {
                  setBalanceDirty(true);
                  setDue(v);
                }}
              />
            ) : (
              <NumberPad value={creditLimit} onChange={setCreditLimit} />
            )}
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
          className="bg-grad-primary shadow-glow-primary h-auto w-full rounded-sm py-3.5 text-sm font-medium text-white"
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
