import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Banknote,
  CreditCard,
  Landmark,
  PiggyBank,
  Sparkles,
  TrendingUp,
  Wallet,
  type LucideIcon,
} from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { loadSampleData } from '@/data/sampleData';
import { COLOR_PALETTE } from '@/data/colorPalette';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import type { AccountType, TrackingMode } from '@/types';
import { MAX_NAME_LENGTH, cleanText, stripLeading } from '@/utils/validation';

const ACCOUNT_TYPES: { value: AccountType; label: string; icon: string; Icon: LucideIcon }[] = [
  { value: 'checking', label: 'Checking', icon: 'landmark', Icon: Landmark },
  { value: 'savings', label: 'Savings', icon: 'piggy-bank', Icon: PiggyBank },
  { value: 'cash', label: 'Cash', icon: 'banknote', Icon: Banknote },
  { value: 'credit', label: 'Credit Card', icon: 'credit-card', Icon: CreditCard },
  { value: 'investment', label: 'Investment', icon: 'trending-up', Icon: TrendingUp },
  { value: 'wallet', label: 'Wallet', icon: 'wallet', Icon: Wallet },
];

const MODES: { value: TrackingMode; title: string; description: string }[] = [
  {
    value: 'full',
    title: 'Track everything',
    description: 'Accounts with balances, net worth, cash-flow forecast and card dues.',
  },
  {
    value: 'transactions',
    title: 'Just track transactions',
    description:
      'Record your income and spending and see where your money goes. No balances to maintain.',
  },
];

type Step = 'name' | 'mode' | 'account' | 'balance';

// Transactions-only mode keeps no balances, so it never asks for one.
const STEPS: Step[] = ['name', 'mode', 'account', 'balance'];
const TRACKER_STEPS: Step[] = ['name', 'mode', 'account'];

/**
 * First-run wizard: name → mode → first account → opening balance (full mode only).
 *
 * Rendered instead of the app when `settings.onboardedAt` is unset. Every step past the name
 * is skippable so someone reinstalling can get straight to Settings and restore a backup
 * rather than being forced to invent an account first. The account step also offers
 * `loadSampleData()` — a few months of realistic accounts/transactions/budgets/goals, for a
 * first look that isn't an empty dashboard.
 */
export function Onboarding() {
  const addAccount = useFinanceStore((s) => s.addAccount);
  const updateSettings = useFinanceStore((s) => s.updateSettings);
  const addGoal = useFinanceStore((s) => s.addGoal);
  const addPerson = useFinanceStore((s) => s.addPerson);
  const addBudget = useFinanceStore((s) => s.addBudget);
  const addRecurring = useFinanceStore((s) => s.addRecurring);
  const addContribution = useFinanceStore((s) => s.addContribution);
  const addDebtEntry = useFinanceStore((s) => s.addDebtEntry);
  const bulkAddTransactions = useFinanceStore((s) => s.bulkAddTransactions);

  const [step, setStep] = useState<Step>('name');
  const [trackingMode, setTrackingMode] = useState<TrackingMode>('full');
  const [name, setName] = useState('');
  const [accountName, setAccountName] = useState('');
  const [accountType, setAccountType] = useState<AccountType>('savings');
  // The account's tint comes from its type; the stored colour is just a default.
  const color = COLOR_PALETTE[0];
  const [balance, setBalance] = useState('');

  const trimmedName = cleanText(name, MAX_NAME_LENGTH);
  const trimmedAccountName = cleanText(accountName, MAX_NAME_LENGTH);
  const steps = trackingMode === 'transactions' ? TRACKER_STEPS : STEPS;
  const stepIndex = steps.indexOf(step);

  const finish = (withAccount: boolean) => {
    if (withAccount && trimmedAccountName) {
      const parsed = parseFloat(balance);
      const opening = Number.isFinite(parsed) && trackingMode === 'full' ? parsed : 0;
      addAccount({
        name: trimmedAccountName,
        type: accountType,
        color,
        icon: ACCOUNT_TYPES.find((t) => t.value === accountType)?.icon ?? 'landmark',
        // A credit card's "balance" is money owed, so it starts negative.
        balance: accountType === 'credit' ? -Math.abs(opening) : opening,
      });
    }
    updateSettings({
      userName: trimmedName || 'there',
      trackingMode,
      onboardedAt: new Date().toISOString(),
    });
  };

  // Populates a few months of realistic-looking accounts, transactions, budgets, recurring
  // rules and goals — so a first look at Finio shows the app doing something, not an empty
  // dashboard, and completes onboarding the same way `finish(true)` does.
  const loadSample = () => {
    // The mode goes in first so the sample's transactions are posted under the right rules.
    updateSettings({ trackingMode });
    loadSampleData({
      addAccount,
      addGoal,
      addPerson,
      addBudget,
      addRecurring,
      addContribution,
      addDebtEntry,
      bulkAddTransactions,
    });
    updateSettings({ userName: trimmedName || 'there', onboardedAt: new Date().toISOString() });
  };

  return (
    <div className="flex min-h-dvh w-full items-center justify-center p-6">
      <div className="w-full max-w-sm space-y-6">
        {/* Progress */}
        <div className="flex gap-1.5" role="presentation">
          {steps.map((s, i) => (
            <span
              key={s}
              className={`h-1 flex-1 rounded-full transition-colors ${
                i <= stepIndex ? 'bg-grad-primary' : 'bg-muted'
              }`}
            />
          ))}
        </div>

        {step === 'name' && (
          <div className="space-y-5">
            <div className="space-y-1.5">
              <h1 className="text-2xl font-bold tracking-tight">Welcome to Finio</h1>
              <p className="text-muted-foreground text-sm">
                Everything stays on this device. Let&rsquo;s start with your name.
              </p>
            </div>
            <div>
              <Label htmlFor="onboarding-name" className="mb-1.5 block text-xs font-medium">
                What should we call you?
              </Label>
              <Input
                id="onboarding-name"
                autoFocus
                value={name}
                maxLength={MAX_NAME_LENGTH}
                onChange={(e) => setName(stripLeading(e.target.value))}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && trimmedName) setStep('mode');
                }}
                placeholder="Your name"
                className="w-full"
              />
            </div>
            <Button
              size="lg"
              className="bg-grad-primary shadow-glow-primary w-full text-white"
              disabled={!trimmedName}
              onClick={() => setStep('mode')}
            >
              Continue <ArrowRight size={16} />
            </Button>
          </div>
        )}

        {step === 'mode' && (
          <div className="space-y-5">
            <div className="space-y-1.5">
              <h1 className="text-2xl font-bold tracking-tight">How will you use Finio?</h1>
              <p className="text-muted-foreground text-sm">
                You can switch any time in Settings — nothing is lost either way.
              </p>
            </div>
            <div className="space-y-2">
              {MODES.map(({ value, title, description }) => (
                <button
                  key={value}
                  onClick={() => setTrackingMode(value)}
                  aria-pressed={trackingMode === value}
                  className={`w-full rounded-md border p-4 text-left transition-colors ${
                    trackingMode === value
                      ? 'bg-grad-primary shadow-glow-primary border-transparent text-white'
                      : 'border-[var(--glass-border)] bg-[var(--glass-strong)]'
                  }`}
                >
                  <p className="text-sm font-semibold">{title}</p>
                  <p
                    className={`mt-0.5 text-xs ${trackingMode === value ? 'opacity-90' : 'text-muted-foreground'}`}
                  >
                    {description}
                  </p>
                </button>
              ))}
            </div>
            <div className="space-y-2">
              <Button
                size="lg"
                className="bg-grad-primary shadow-glow-primary w-full text-white"
                onClick={() => setStep('account')}
              >
                Continue <ArrowRight size={16} />
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setStep('name')}>
                <ArrowLeft size={14} /> Back
              </Button>
            </div>
          </div>
        )}

        {step === 'account' && (
          <div className="space-y-5">
            <div className="space-y-1.5">
              <h1 className="text-2xl font-bold tracking-tight">
                {trackingMode === 'transactions'
                  ? 'Add a payment source'
                  : 'Add your first account'}
              </h1>
              <p className="text-muted-foreground text-sm">
                {trackingMode === 'transactions'
                  ? 'Where your spending comes from — a bank, a card or cash. No balance needed.'
                  : 'A bank account, a card, or just the cash in your wallet.'}
              </p>
            </div>

            <div>
              <Label htmlFor="onboarding-account" className="mb-1.5 block text-xs font-medium">
                Account name
              </Label>
              <Input
                id="onboarding-account"
                autoFocus
                value={accountName}
                maxLength={MAX_NAME_LENGTH}
                onChange={(e) => setAccountName(stripLeading(e.target.value))}
                placeholder="e.g. HDFC Savings"
                className="w-full"
              />
            </div>

            <div>
              <Label className="mb-1.5 block text-xs font-medium">Type</Label>
              <div className="grid grid-cols-3 gap-2">
                {ACCOUNT_TYPES.map(({ value, label, Icon }) => (
                  <button
                    key={value}
                    onClick={() => setAccountType(value)}
                    aria-pressed={accountType === value}
                    className={`flex flex-col items-center gap-1 rounded-sm px-2 py-3 text-xs font-medium transition-all ${
                      accountType === value
                        ? 'bg-grad-primary text-white shadow'
                        : 'bg-card text-muted-foreground'
                    }`}
                  >
                    <Icon size={16} />
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <Button
                size="lg"
                className="bg-grad-primary shadow-glow-primary w-full text-white"
                disabled={!trimmedAccountName}
                onClick={() =>
                  trackingMode === 'transactions' ? finish(true) : setStep('balance')
                }
              >
                {trackingMode === 'transactions' ? (
                  'Start tracking'
                ) : (
                  <>
                    Continue <ArrowRight size={16} />
                  </>
                )}
              </Button>
              <div className="flex items-center justify-between">
                <Button variant="ghost" size="sm" onClick={() => setStep('mode')}>
                  <ArrowLeft size={14} /> Back
                </Button>
                <Button variant="ghost" size="sm" onClick={() => finish(false)}>
                  Skip for now
                </Button>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="text-primary w-full"
                onClick={loadSample}
              >
                <Sparkles size={14} /> Explore with sample data instead
              </Button>
            </div>
          </div>
        )}

        {step === 'balance' && (
          <div className="space-y-5">
            <div className="space-y-1.5">
              <h1 className="text-2xl font-bold tracking-tight">
                {accountType === 'credit' ? 'How much do you owe?' : "What's in it right now?"}
              </h1>
              <p className="text-muted-foreground text-sm">
                {accountType === 'credit'
                  ? `The current outstanding balance on ${trimmedAccountName}. You can change it later.`
                  : `The opening balance for ${trimmedAccountName}. You can change it later.`}
              </p>
            </div>

            <NumberPad value={balance} onChange={setBalance} />

            <div className="space-y-2">
              <Button
                size="lg"
                className="bg-grad-primary shadow-glow-primary w-full text-white"
                onClick={() => finish(true)}
              >
                Start tracking
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setStep('account')}>
                <ArrowLeft size={14} /> Back
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
