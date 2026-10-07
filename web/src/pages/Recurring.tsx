import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { ArrowLeft, Pause, Pencil, Play, PiggyBank, Plus, Repeat, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import {
  isRulePaused,
  lastOccurrenceOnOrBefore,
  nextDueDate,
  previewBackfill,
  type BackfillPreview,
} from '@/store/recurring';
import {
  formatCurrency,
  formatShortDate,
  localDayKey,
  toLocalDateTimeInputValue,
} from '@/utils/formatters';
import { MAX_NOTE_LENGTH, cleanText, stripLeading } from '@/utils/validation';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { useConfirm } from '@/components/ui/use-confirm';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  activeAccounts,
  findTransferCategory,
  isCategoryValidForType,
  miscLast,
} from '@/utils/calculations';
import type { RecurrenceFrequency, RecurringTransaction, TransactionType } from '@/types';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

const FREQ_LABEL: Record<RecurrenceFrequency, string> = {
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
  yearly: 'Yearly',
};

type EndMode = 'never' | 'on' | 'after';

/**
 * Below this, a backfill isn't worth interrupting for — creating a rule dated today has always
 * generated today's transaction straight away.
 */
const BACKFILL_PROMPT_THRESHOLD = 2;

/** The rule the form currently describes, plus how it should be committed. */
interface PendingRule {
  rule: RecurringTransaction;
  editingId: string | null;
  preview: BackfillPreview;
}

export default function Recurring() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const recurring = useFinanceStore((s) => s.recurring);
  const allAccounts = useFinanceStore((s) => s.accounts);
  const categories = useFinanceStore((s) => s.categories);
  const goals = useFinanceStore((s) => s.goals);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  // A closed account cannot take new charges, so it must not back a new rule. Existing rules
  // still resolve their account name from the full list below.
  const accounts = useMemo(() => activeAccounts(allAccounts), [allAccounts]);
  const addRecurring = useFinanceStore((s) => s.addRecurring);
  const updateRecurring = useFinanceStore((s) => s.updateRecurring);
  const setRecurringPaused = useFinanceStore((s) => s.setRecurringPaused);
  const deleteRecurring = useFinanceStore((s) => s.deleteRecurring);
  const processRecurring = useFinanceStore((s) => s.processRecurring);
  const bulkDeleteTransactions = useFinanceStore((s) => s.bulkDeleteTransactions);

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [type, setType] = useState<TransactionType>('expense');
  const [amount, setAmount] = useState('');
  const [accountId, setAccountId] = useState(accounts[0]?.id ?? '');
  const [toAccountId, setToAccountId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [note, setNote] = useState('');
  const [frequency, setFrequency] = useState<RecurrenceFrequency>('monthly');
  const [startDate, setStartDate] = useState(toLocalDateTimeInputValue(new Date()));
  const [endMode, setEndMode] = useState<EndMode>('never');
  const [endDate, setEndDate] = useState('');
  const [maxOccurrences, setMaxOccurrences] = useState('');
  const [goalId, setGoalId] = useState('');
  const [pending, setPending] = useState<PendingRule | null>(null);

  const filteredCategories = useMemo(
    () =>
      miscLast(
        type === 'transfer'
          ? categories.filter((c) => c.type === 'both')
          : categories.filter((c) => isCategoryValidForType(c, type)),
      ),
    [categories, type],
  );

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setType('expense');
    setAmount('');
    setAccountId(accounts[0]?.id ?? '');
    setToAccountId('');
    setCategoryId('');
    setNote('');
    setFrequency('monthly');
    setStartDate(toLocalDateTimeInputValue(new Date()));
    setEndMode('never');
    setEndDate('');
    setMaxOccurrences('');
    setGoalId('');
  };

  const startCreate = () => {
    resetForm();
    setShowForm(true);
  };

  const startEdit = (rule: RecurringTransaction) => {
    setEditingId(rule.id);
    setType(rule.type);
    setAmount(String(rule.amount));
    setAccountId(rule.accountId);
    setToAccountId(rule.toAccountId ?? '');
    setCategoryId(rule.categoryId);
    setNote(rule.note);
    setFrequency(rule.frequency);
    setStartDate(toLocalDateTimeInputValue(rule.startDate));
    setEndMode(rule.endDate ? 'on' : rule.maxOccurrences !== undefined ? 'after' : 'never');
    setEndDate(rule.endDate ? localDayKey(rule.endDate) : '');
    setMaxOccurrences(rule.maxOccurrences !== undefined ? String(rule.maxOccurrences) : '');
    setGoalId(rule.goalId ?? '');
    setShowForm(true);
  };

  /** Build the rule the form describes, or return the first validation error. */
  const buildRule = (): RecurringTransaction | string => {
    const parsed = parseFloat(amount);
    if (!parsed || parsed <= 0) return 'Enter a valid amount';
    if (!accountId) return 'Select an account';

    if (type === 'transfer') {
      if (!toAccountId) return 'Select a destination account';
      if (toAccountId === accountId) return 'Choose two different accounts';
    } else if (!categoryId) {
      return 'Select a category';
    }

    const start = new Date(startDate);
    if (Number.isNaN(start.getTime())) return 'Pick a valid start date';

    let resolvedEndDate: string | undefined;
    let resolvedMax: number | undefined;
    if (endMode === 'on') {
      if (!endDate) return 'Pick an end date';
      // The picker gives a date only — run the rule through the end of that day.
      const end = new Date(`${endDate}T23:59:59`);
      if (Number.isNaN(end.getTime())) return 'Pick a valid end date';
      if (end.getTime() < start.getTime()) return 'The end date is before the start date';
      resolvedEndDate = end.toISOString();
    } else if (endMode === 'after') {
      const count = parseInt(maxOccurrences, 10);
      if (!Number.isFinite(count) || count < 1) return 'Enter how many times it should run';
      resolvedMax = count;
    }

    const existing = editingId ? recurring.find((r) => r.id === editingId) : undefined;
    const transferCategory = findTransferCategory(categories);

    return {
      id: existing?.id ?? 'draft',
      type,
      amount: parsed,
      accountId,
      categoryId: type === 'transfer' ? (transferCategory?.id ?? categoryId ?? '') : categoryId,
      note: cleanText(note, MAX_NOTE_LENGTH),
      labels: existing?.labels ?? [],
      frequency,
      startDate: start.toISOString(),
      occurrenceCount: existing?.occurrenceCount ?? 0,
      lastRunDate: existing?.lastRunDate ?? null,
      createdAt: existing?.createdAt ?? new Date().toISOString(),
      ...(type === 'transfer' ? { toAccountId } : {}),
      ...(resolvedEndDate ? { endDate: resolvedEndDate } : {}),
      ...(resolvedMax !== undefined ? { maxOccurrences: resolvedMax } : {}),
      ...(existing?.pausedAt ? { pausedAt: existing.pausedAt } : {}),
      ...(goalId ? { goalId } : {}),
    };
  };

  const commit = (rule: RecurringTransaction, ruleEditingId: string | null, skipPast: boolean) => {
    // Skipping the backfill parks the schedule on the last occurrence that has already passed,
    // so the rule stays anchored to its start date but generates nothing for the past.
    const lastRunDate = skipPast
      ? (lastOccurrenceOnOrBefore(rule, new Date())?.toISOString() ?? rule.lastRunDate)
      : rule.lastRunDate;

    // Written out field by field so clearing "ends on" / "ends after" (or switching away from a
    // transfer) actually unsets the old value instead of leaving it behind.
    const fields = {
      type: rule.type,
      amount: rule.amount,
      accountId: rule.accountId,
      toAccountId: rule.toAccountId,
      categoryId: rule.categoryId,
      note: rule.note,
      labels: rule.labels,
      frequency: rule.frequency,
      startDate: rule.startDate,
      endDate: rule.endDate,
      maxOccurrences: rule.maxOccurrences,
      lastRunDate,
      goalId: rule.goalId,
    };

    if (ruleEditingId) {
      updateRecurring(ruleEditingId, fields);
    } else {
      addRecurring(fields);
    }

    const generated = processRecurring();
    toast.success(
      `${ruleEditingId ? 'Rule updated' : 'Recurring rule created'}${
        generated.length > 0
          ? ` · added ${generated.length} transaction${generated.length === 1 ? '' : 's'}`
          : ''
      }`,
      generated.length > 0
        ? {
            action: {
              label: 'Undo',
              onClick: () => bulkDeleteTransactions(generated.map((t) => t.id)),
            },
          }
        : undefined,
    );
    setPending(null);
    resetForm();
  };

  const handleSubmit = () => {
    const built = buildRule();
    if (typeof built === 'string') {
      toast.error(built);
      return;
    }

    const preview = previewBackfill(
      built,
      accounts.map((a) => a.id),
      new Date(),
    );

    // A rule dated in the past moves real balances — show what it will do before it does it.
    if (preview.count >= BACKFILL_PROMPT_THRESHOLD) {
      setPending({ rule: built, editingId, preview });
      return;
    }
    commit(built, editingId, false);
  };

  const accountName = (id: string | undefined) =>
    allAccounts.find((a) => a.id === id)?.name ?? 'Unknown';

  return (
    <>
      {/* Header */}
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Recurring</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton
            onClick={() => (showForm ? resetForm() : startCreate())}
            aria-label="Add recurring"
            tone="primary"
            disabled={accounts.length === 0}
          >
            <Plus />
          </HeaderIconButton>
        </div>
      </Header>

      <Main className="lg:max-w-xl">
        {accounts.length === 0 && (
          <p className="text-muted-foreground py-8 text-center text-sm">
            Add an account first to create recurring rules.
          </p>
        )}

        {showForm && (
          <div className="card-elevated space-y-3 rounded-md p-4">
            <div
              role="radiogroup"
              aria-label="Transaction type"
              className="bg-muted grid grid-cols-3 gap-1 rounded-full p-1"
            >
              {(['expense', 'income', 'transfer'] as const).map((t) => (
                <button
                  key={t}
                  type="button"
                  role="radio"
                  aria-checked={type === t}
                  onClick={() => {
                    setType(t);
                    setCategoryId('');
                  }}
                  className={`rounded-full py-2 text-xs font-medium capitalize transition-all ${
                    type === t
                      ? 'bg-grad-primary shadow-glow-primary text-white'
                      : 'text-muted-foreground'
                  }`}
                >
                  {t}
                </button>
              ))}
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Amount
              </Label>
              <NumberPad value={amount} onChange={setAmount} />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                {type === 'transfer' ? 'From account' : 'Account'}
              </Label>
              <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Account">
                    {accounts.find((a) => a.id === accountId)?.name}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {accounts.map((a) => (
                    <SelectItem key={a.id} value={a.id}>
                      {a.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {type === 'transfer' ? (
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  To account
                </Label>
                <Select value={toAccountId} onValueChange={(v) => setToAccountId(v ?? '')}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Destination">
                      {accounts.find((a) => a.id === toAccountId)?.name}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {accounts
                      .filter((a) => a.id !== accountId)
                      .map((a) => (
                        <SelectItem key={a.id} value={a.id}>
                          {a.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Category
                </Label>
                <Select value={categoryId} onValueChange={(v) => setCategoryId(v ?? '')}>
                  <SelectTrigger className="w-full" aria-label="Category">
                    <SelectValue placeholder="Choose a category">
                      {filteredCategories.find((c) => c.id === categoryId)?.name}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {filteredCategories.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            <div>
              <Label
                htmlFor="recurring-note"
                className="text-muted-foreground mb-1.5 block text-xs font-medium"
              >
                Note
              </Label>
              <Input
                id="recurring-note"
                type="text"
                placeholder="e.g. Netflix"
                value={note}
                maxLength={MAX_NOTE_LENGTH}
                onChange={(e) => setNote(stripLeading(e.target.value))}
              />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Frequency
              </Label>
              <Select
                value={frequency}
                onValueChange={(v) => setFrequency(v as RecurrenceFrequency)}
              >
                <SelectTrigger className="w-full" aria-label="Frequency">
                  <SelectValue placeholder="Frequency">{FREQ_LABEL[frequency]}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(FREQ_LABEL) as RecurrenceFrequency[]).map((f) => (
                    <SelectItem key={f} value={f}>
                      {FREQ_LABEL[f]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Starts
              </Label>
              <DateTimePicker value={startDate} onChange={setStartDate} />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">Ends</Label>
              <Select value={endMode} onValueChange={(v) => setEndMode((v as EndMode) ?? 'never')}>
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {endMode === 'never'
                      ? 'Never'
                      : endMode === 'on'
                        ? 'On a date'
                        : 'After N times'}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="never">Never</SelectItem>
                  <SelectItem value="on">On a date</SelectItem>
                  <SelectItem value="after">After N times</SelectItem>
                </SelectContent>
              </Select>
              {endMode === 'on' && (
                <DatePicker
                  value={endDate}
                  onChange={setEndDate}
                  placeholder="End date"
                  className="mt-2"
                />
              )}
              {endMode === 'after' && (
                <Input
                  type="number"
                  min={1}
                  inputMode="numeric"
                  placeholder="Number of occurrences"
                  value={maxOccurrences}
                  onChange={(e) => setMaxOccurrences(e.target.value)}
                  className="mt-2"
                />
              )}
            </div>

            {goals.length > 0 && (
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Fund a goal (optional)
                </Label>
                <Select
                  value={goalId || 'none'}
                  onValueChange={(v) => setGoalId(v === 'none' ? '' : (v ?? ''))}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {goalId ? goals.find((g) => g.id === goalId)?.name : 'None'}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {goals.map((g) => (
                      <SelectItem key={g.id} value={g.id}>
                        {g.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-muted-foreground mt-1 text-[11px]">
                  Each occurrence also logs a contribution to this goal, automatically.
                </p>
              </div>
            )}

            <div className="flex gap-2">
              <Button onClick={handleSubmit} className="flex-1">
                {editingId ? 'Save changes' : 'Save rule'}
              </Button>
              <Button variant="outline" onClick={resetForm} className="px-4">
                Cancel
              </Button>
            </div>
          </div>
        )}

        {recurring.length === 0 && !showForm && accounts.length > 0 && (
          <div className="py-12 text-center">
            <Repeat size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
            <p className="text-muted-foreground mb-4">No recurring rules yet</p>
            <Button onClick={startCreate} className="rounded-full px-5 py-2.5">
              Create a rule
            </Button>
          </div>
        )}

        {recurring.length > 0 && (
          <div className="card-elevated divide-border divide-y rounded-md">
            {recurring.map((r) => {
              const cat = categories.find((c) => c.id === r.categoryId);
              const paused = isRulePaused(r);
              const nextDue = nextDueDate(r);
              const fundedGoal = r.goalId ? goals.find((g) => g.id === r.goalId) : undefined;

              const schedule = paused
                ? 'Paused'
                : nextDue
                  ? `Next ${formatShortDate(nextDue)}`
                  : 'Ended';
              const limit =
                r.maxOccurrences !== undefined
                  ? `${r.occurrenceCount} of ${r.maxOccurrences}`
                  : r.endDate
                    ? `until ${formatShortDate(r.endDate)}`
                    : null;

              return (
                <div key={r.id} className="px-4 py-3">
                  <div className={`flex items-center gap-3 ${paused ? 'opacity-50' : ''}`}>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">
                        {r.note || cat?.name || 'Recurring'}
                      </p>
                      <p className="text-muted-foreground truncate text-xs">
                        {FREQ_LABEL[r.frequency]} ·{' '}
                        {r.type === 'transfer'
                          ? `${accountName(r.accountId)} → ${accountName(r.toAccountId)}`
                          : accountName(r.accountId)}
                      </p>
                    </div>
                    <p
                      className={`shrink-0 text-sm font-semibold ${
                        // Same rule as the transaction list: income green, everything else ink —
                        // magenta is reserved for overspend and destructive actions.
                        r.type === 'income' ? 'text-positive' : 'text-foreground'
                      }`}
                    >
                      {r.type === 'income' ? '+' : r.type === 'expense' ? '-' : ''}
                      {formatCurrency(r.amount, true, hideAmounts)}
                    </p>
                  </div>

                  {fundedGoal && (
                    <p className="text-muted-foreground mt-1.5 flex items-center gap-1 text-[11px]">
                      <PiggyBank size={11} />
                      Funds &ldquo;{fundedGoal.name}&rdquo;
                    </p>
                  )}

                  <div className="mt-1 flex items-center justify-between gap-2">
                    <p className="text-muted-foreground min-w-0 truncate text-[11px]">
                      {schedule}
                      {limit ? ` · ${limit}` : ''}
                    </p>
                    <div className="flex shrink-0 items-center">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => {
                          setRecurringPaused(r.id, !paused);
                          if (!paused) toast.success('Rule paused');
                          else {
                            const generated = processRecurring();
                            toast.success(
                              `Rule resumed${generated.length > 0 ? ` · added ${generated.length} due transaction${generated.length === 1 ? '' : 's'}` : ''}`,
                              generated.length > 0
                                ? {
                                    action: {
                                      label: 'Undo',
                                      onClick: () =>
                                        bulkDeleteTransactions(generated.map((t) => t.id)),
                                    },
                                  }
                                : undefined,
                            );
                          }
                        }}
                        className="h-7 w-7"
                        aria-label={paused ? 'Resume rule' : 'Pause rule'}
                      >
                        {paused ? (
                          <Play size={13} className="text-primary" />
                        ) : (
                          <Pause size={13} className="text-muted-foreground" />
                        )}
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => startEdit(r)}
                        className="h-7 w-7"
                        aria-label="Edit rule"
                      >
                        <Pencil size={13} className="text-muted-foreground" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={async () => {
                          const confirmed = await confirm({
                            title: 'Delete this recurring rule?',
                            description:
                              'Transactions it has already generated are kept — only future occurrences stop.',
                            confirmLabel: 'Delete rule',
                          });
                          if (confirmed) deleteRecurring(r.id);
                        }}
                        className="h-7 w-7"
                        aria-label="Delete rule"
                      >
                        <Trash2 size={13} className="text-destructive" />
                      </Button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Backfill preview — a past start date injects transactions and moves balances. */}
        <Dialog open={pending !== null} onOpenChange={(open) => !open && setPending(null)}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Add past transactions?</DialogTitle>
              <DialogDescription>
                {pending && (
                  <>
                    This rule started{' '}
                    {pending.preview.firstDate && formatShortDate(pending.preview.firstDate)}, so
                    saving it will create{' '}
                    <strong className="text-foreground">
                      {pending.preview.count} transaction
                      {pending.preview.count === 1 ? '' : 's'}
                    </strong>{' '}
                    totalling{' '}
                    <strong className="text-foreground">
                      {formatCurrency(pending.preview.total, false, hideAmounts)}
                    </strong>{' '}
                    and move your balances.
                    {pending.preview.capped &&
                      ' More will be added the next time the app opens — the catch-up is capped per run.'}
                  </>
                )}
              </DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-2">
              <Button
                onClick={() => pending && commit(pending.rule, pending.editingId, false)}
                className="w-full"
              >
                Add them
              </Button>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() => pending && commit(pending.rule, pending.editingId, true)}
                  className="flex-1"
                >
                  Start from today
                </Button>
                <Button variant="outline" onClick={() => setPending(null)} className="px-4">
                  Cancel
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </Main>
    </>
  );
}
