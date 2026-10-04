import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import {
  ArrowLeft,
  ChevronDown,
  History,
  Minus,
  Pencil,
  Plus,
  PiggyBank,
  Trash2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { COLOR_PALETTE } from '@/data/colorPalette';
import {
  formatCurrency,
  formatDayMonth,
  formatShortDate,
  localDayKey,
  todayKey,
} from '@/utils/formatters';
import {
  MAX_NAME_LENGTH,
  MAX_NOTE_LENGTH,
  cleanText,
  isPastDay,
  stripLeading,
} from '@/utils/validation';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { GoalIcon } from '@/components/goals/GoalIcon';
import { GOAL_ICONS } from '@/components/goals/goalIcons';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { DatePicker } from '@/components/ui/date-picker';
import { useConfirm } from '@/components/ui/use-confirm';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { computeGoalStatus, activeAccounts, type GoalStatus } from '@/utils/calculations';
import type { Goal } from '@/types';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

const NO_ACCOUNT = '__none__';

const goalColors = COLOR_PALETTE;

export default function Goals() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const goals = useFinanceStore((s) => s.goals);
  const contributions = useFinanceStore((s) => s.goalContributions);
  const accounts = useFinanceStore((s) => s.accounts);
  const addGoal = useFinanceStore((s) => s.addGoal);
  const updateGoal = useFinanceStore((s) => s.updateGoal);
  const deleteGoal = useFinanceStore((s) => s.deleteGoal);
  const addContribution = useFinanceStore((s) => s.addContribution);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const deleteContribution = useFinanceStore((s) => s.deleteContribution);
  const restoreContribution = useFinanceStore((s) => s.restoreContribution);

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [icon, setIcon] = useState(GOAL_ICONS[0]);
  const [color, setColor] = useState(goalColors[0]);
  const [targetAmount, setTargetAmount] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [linkedAccountId, setLinkedAccountId] = useState(NO_ACCOUNT);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [contributionGoal, setContributionGoal] = useState<{
    goal: Goal;
    mode: 'add' | 'withdraw';
  } | null>(null);
  const [contributionAmount, setContributionAmount] = useState('');
  const [contributionNote, setContributionNote] = useState('');

  const openAccounts = useMemo(() => activeAccounts(accounts), [accounts]);

  const statuses = useMemo(
    () => goals.map((g) => computeGoalStatus(g, contributions)),
    [goals, contributions],
  );

  // In-progress goals first (closest to done first), completed goals trail behind.
  const sortedStatuses = useMemo(
    () =>
      [...statuses].sort((a, b) => {
        if (a.isComplete !== b.isComplete) return a.isComplete ? 1 : -1;
        return b.percent - a.percent;
      }),
    [statuses],
  );

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setName('');
    setIcon(GOAL_ICONS[0]);
    setColor(goalColors[0]);
    setTargetAmount('');
    setTargetDate('');
    setLinkedAccountId(NO_ACCOUNT);
  };

  const startCreate = () => {
    resetForm();
    setShowForm(true);
  };

  const startEdit = (goal: Goal) => {
    setEditingId(goal.id);
    setName(goal.name);
    setIcon(goal.icon);
    setColor(goal.color);
    setTargetAmount(String(goal.targetAmount));
    setTargetDate(goal.targetDate ? localDayKey(goal.targetDate) : '');
    setLinkedAccountId(goal.linkedAccountId ?? NO_ACCOUNT);
    setShowForm(true);
  };

  const handleSubmit = () => {
    const parsed = parseFloat(targetAmount);
    const cleanName = cleanText(name, MAX_NAME_LENGTH);
    if (!cleanName) {
      toast.error('Enter a goal name');
      return;
    }
    if (!parsed || parsed <= 0) {
      toast.error('Enter a valid target amount');
      return;
    }
    // A deadline in the past is meaningless — but don't block saving an old goal whose date is
    // simply unchanged.
    const existingGoal = editingId ? goals.find((g) => g.id === editingId) : undefined;
    const unchangedDate =
      !!existingGoal?.targetDate && localDayKey(existingGoal.targetDate) === targetDate;
    if (targetDate && !unchangedDate && isPastDay(targetDate, todayKey())) {
      toast.error('Target date must be today or later');
      return;
    }

    const data = {
      name: cleanName,
      icon,
      color,
      targetAmount: parsed,
      // Explicit `undefined` (rather than omitting the key) so editing a goal to clear its
      // date or link actually overwrites the old value instead of leaving it untouched.
      targetDate: targetDate ? new Date(`${targetDate}T00:00:00`).toISOString() : undefined,
      linkedAccountId: linkedAccountId !== NO_ACCOUNT ? linkedAccountId : undefined,
    };

    if (editingId) {
      updateGoal(editingId, data);
      toast.success('Goal updated');
    } else {
      addGoal(data);
      toast.success('Goal created');
    }
    resetForm();
  };

  const openContribution = (goal: Goal, mode: 'add' | 'withdraw') => {
    setContributionGoal({ goal, mode });
    setContributionAmount('');
    setContributionNote('');
  };

  const withdrawLimit = contributionGoal
    ? Math.max(0, statuses.find((s) => s.goal.id === contributionGoal.goal.id)?.current ?? 0)
    : 0;

  const handleContributionSubmit = () => {
    if (!contributionGoal) return;
    const parsed = parseFloat(contributionAmount);
    if (!parsed || parsed <= 0) {
      toast.error('Enter a valid amount');
      return;
    }
    if (contributionGoal.mode === 'withdraw' && parsed > withdrawLimit + 0.005) {
      toast.error(
        withdrawLimit > 0
          ? `Only ${formatCurrency(withdrawLimit, false, hideAmounts)} is saved in this goal`
          : 'Nothing saved in this goal to withdraw',
      );
      return;
    }
    addContribution({
      goalId: contributionGoal.goal.id,
      amount: contributionGoal.mode === 'withdraw' ? -parsed : parsed,
      date: new Date().toISOString(),
      note: cleanText(contributionNote, MAX_NOTE_LENGTH),
    });
    toast.success(
      contributionGoal.mode === 'withdraw' ? 'Withdrawal logged' : 'Contribution added',
    );
    setContributionGoal(null);
  };

  return (
    <>
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Savings goals</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton
            onClick={() => (showForm ? resetForm() : startCreate())}
            aria-label="Add goal"
            tone="primary"
          >
            <Plus />
          </HeaderIconButton>
        </div>
      </Header>

      <Main className="lg:max-w-xl">
        {showForm && (
          <div className="card-elevated space-y-3 rounded-md p-4">
            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Goal name
              </Label>
              <Input
                type="text"
                placeholder="e.g., Emergency Fund"
                value={name}
                maxLength={MAX_NAME_LENGTH}
                onChange={(e) => setName(stripLeading(e.target.value))}
              />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">Icon</Label>
              <div className="grid grid-cols-6 gap-2">
                {GOAL_ICONS.map((i) => (
                  <button
                    key={i}
                    onClick={() => setIcon(i)}
                    className={`flex h-9 items-center justify-center rounded-full border transition-colors ${
                      icon === i ? 'border-primary bg-primary/10' : 'border-border bg-card'
                    }`}
                    aria-label={`Icon ${i}`}
                    aria-pressed={icon === i}
                  >
                    <GoalIcon icon={i} size={16} />
                  </button>
                ))}
              </div>
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Color
              </Label>
              <div className="flex flex-wrap gap-3">
                {goalColors.map((c) => (
                  <button
                    key={c}
                    onClick={() => setColor(c)}
                    className={`h-7 w-7 rounded-full transition-transform ${
                      color === c ? 'ring-primary scale-110 ring-2 ring-offset-2' : ''
                    }`}
                    style={{ backgroundColor: c }}
                    aria-label={`Color ${c}`}
                    aria-pressed={color === c}
                  />
                ))}
              </div>
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Target amount
              </Label>
              <NumberPad value={targetAmount} onChange={setTargetAmount} />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Target date (optional)
              </Label>
              <div className="flex gap-2">
                <DatePicker
                  value={targetDate}
                  onChange={setTargetDate}
                  minDate={todayKey()}
                  placeholder="No deadline"
                  className="flex-1"
                />
                {targetDate && (
                  <Button variant="secondary" onClick={() => setTargetDate('')} className="text-xs">
                    Clear
                  </Button>
                )}
              </div>
            </div>

            {openAccounts.length > 0 && (
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Linked account (optional)
                </Label>
                <Select
                  value={linkedAccountId}
                  onValueChange={(v) => setLinkedAccountId(v ?? NO_ACCOUNT)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {linkedAccountId === NO_ACCOUNT
                        ? 'None'
                        : (openAccounts.find((a) => a.id === linkedAccountId)?.name ?? 'None')}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_ACCOUNT}>None</SelectItem>
                    {openAccounts.map((a) => (
                      <SelectItem key={a.id} value={a.id}>
                        {a.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-muted-foreground mt-1.5 text-[10px]">
                  Just a label — this account's balance and transactions aren't affected.
                </p>
              </div>
            )}

            <div className="flex gap-2">
              <Button onClick={handleSubmit} className="flex-1">
                {editingId ? 'Save changes' : 'Save'}
              </Button>
              <Button variant="secondary" onClick={resetForm}>
                Cancel
              </Button>
            </div>
          </div>
        )}

        {sortedStatuses.length === 0 ? (
          <div className="py-12 text-center">
            <PiggyBank size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
            <p className="text-muted-foreground mb-4">No savings goals yet</p>
            <Button onClick={startCreate} className="rounded-full px-5 py-2.5">
              Create your first goal
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {sortedStatuses.map((status) => (
              <GoalCard
                key={status.goal.id}
                status={status}
                linkedAccountName={
                  status.goal.linkedAccountId
                    ? accounts.find((a) => a.id === status.goal.linkedAccountId)?.name
                    : undefined
                }
                contributions={contributions.filter((c) => c.goalId === status.goal.id)}
                expanded={expandedId === status.goal.id}
                onToggleHistory={() =>
                  setExpandedId((id) => (id === status.goal.id ? null : status.goal.id))
                }
                onAddFunds={() => openContribution(status.goal, 'add')}
                onWithdraw={() => openContribution(status.goal, 'withdraw')}
                onEdit={() => startEdit(status.goal)}
                onDelete={async () => {
                  const confirmed = await confirm({
                    title: `Delete "${status.goal.name}"?`,
                    description:
                      'Every contribution logged against this goal will be deleted too. This cannot be undone.',
                    confirmLabel: 'Delete goal',
                  });
                  if (confirmed) deleteGoal(status.goal.id);
                }}
                onDeleteContribution={(id) => {
                  const removed = deleteContribution(id);
                  if (!removed) return;
                  toast.success('Contribution removed', {
                    action: { label: 'Undo', onClick: () => restoreContribution(removed) },
                  });
                }}
              />
            ))}
          </div>
        )}
      </Main>

      {/* Add funds / withdraw dialog */}
      <Dialog
        open={contributionGoal !== null}
        onOpenChange={(v) => {
          if (!v) setContributionGoal(null);
        }}
      >
        <DialogContent className="bg-card mx-auto w-11/12">
          <DialogHeader>
            {/* Right padding keeps a long goal name wrapping clear of the close button. */}
            <DialogTitle className="pr-8 break-words">
              {contributionGoal?.mode === 'withdraw' ? 'Withdraw from' : 'Add funds to'}{' '}
              {contributionGoal?.goal.name}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <NumberPad value={contributionAmount} onChange={setContributionAmount} />
            {contributionGoal?.mode === 'withdraw' && (
              <p className="text-muted-foreground text-xs">
                {formatCurrency(withdrawLimit, false, hideAmounts)} saved — the most you can
                withdraw.
              </p>
            )}
            <Input
              type="text"
              placeholder="Note (optional)"
              value={contributionNote}
              maxLength={MAX_NOTE_LENGTH}
              onChange={(e) => setContributionNote(stripLeading(e.target.value))}
            />
            <div className="flex gap-2">
              <Button onClick={handleContributionSubmit} className="flex-1">
                {contributionGoal?.mode === 'withdraw' ? 'Withdraw' : 'Add'}
              </Button>
              <Button variant="secondary" onClick={() => setContributionGoal(null)}>
                Cancel
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

interface GoalCardProps {
  status: GoalStatus;
  linkedAccountName?: string;
  contributions: Array<{ id: string; amount: number; date: string; note: string }>;
  expanded: boolean;
  onToggleHistory: () => void;
  onAddFunds: () => void;
  onWithdraw: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onDeleteContribution: (id: string) => void;
}

function GoalCard({
  status,
  linkedAccountName,
  contributions,
  expanded,
  onToggleHistory,
  onAddFunds,
  onWithdraw,
  onEdit,
  onDelete,
  onDeleteContribution,
}: GoalCardProps) {
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const { goal } = status;

  return (
    <div className="card-elevated rounded-md p-4">
      <div className="mb-2 flex items-center justify-between">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{goal.name}</p>
          <p className="text-muted-foreground truncate text-[11px]">
            {linkedAccountName ? `Linked · ${linkedAccountName}` : 'No linked account'}
            {goal.targetDate ? ` · by ${formatShortDate(goal.targetDate)}` : ''}
          </p>
        </div>
        <div className="flex shrink-0 items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={onEdit}
            className="h-7 w-7"
            aria-label={`Edit ${goal.name}`}
          >
            <Pencil size={13} className="text-muted-foreground" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={onDelete}
            className="h-7 w-7"
            aria-label={`Delete ${goal.name}`}
          >
            <Trash2 size={13} className="text-destructive" />
          </Button>
        </div>
      </div>

      <div className="mb-1.5 flex justify-between text-xs">
        <span className={status.isComplete ? 'text-positive font-medium' : 'text-muted-foreground'}>
          {formatCurrency(status.current, false, hideAmounts)} of{' '}
          {formatCurrency(goal.targetAmount, false, hideAmounts)}
        </span>
        <span
          className={`font-medium ${status.isComplete ? 'text-positive' : 'text-muted-foreground'}`}
        >
          {Math.round(status.percent)}%
        </span>
      </div>
      <div className="bg-muted h-2 overflow-hidden rounded-full">
        <div
          className="thread-fill h-full transition-all"
          style={{ width: `${Math.min(Math.max(status.percent, 0), 100)}%` }}
        />
      </div>
      <p className="text-muted-foreground mt-1.5 text-[11px]">
        {status.isComplete
          ? 'Goal reached'
          : `${formatCurrency(status.remaining, false, hideAmounts)} to go`}
        {!status.isComplete && status.projectedDate && (
          <> · at this pace, by {formatShortDate(status.projectedDate.toISOString())}</>
        )}
      </p>

      <div className="mt-3 flex gap-2">
        <Button
          variant="secondary"
          onClick={onAddFunds}
          className="bg-positive/10 text-positive hover:bg-positive/15 flex-1 text-xs"
        >
          <Plus size={13} className="mr-1" /> Add funds
        </Button>
        <Button variant="secondary" onClick={onWithdraw} className="flex-1 text-xs">
          <Minus size={13} className="mr-1" /> Withdraw
        </Button>
      </div>

      <button
        onClick={onToggleHistory}
        className="text-muted-foreground mt-2 flex items-center gap-1 text-[11px] font-medium"
        aria-expanded={expanded}
      >
        <History size={12} />
        History
        <ChevronDown
          size={12}
          className={expanded ? 'rotate-180 transition-transform' : 'transition-transform'}
        />
      </button>

      {expanded && (
        <div className="border-border mt-2 space-y-1.5 border-t pt-2">
          {contributions.length === 0 ? (
            <p className="text-muted-foreground text-[11px]">No contributions logged yet.</p>
          ) : (
            contributions.map((c) => (
              <div key={c.id} className="flex items-center gap-2 text-[11px]">
                <span className="text-muted-foreground w-14 shrink-0">
                  {formatDayMonth(c.date)}
                </span>
                <span className="min-w-0 flex-1 truncate">
                  {c.note || (c.amount < 0 ? 'Withdrawal' : 'Contribution')}
                </span>
                <span
                  className={`shrink-0 font-medium ${c.amount < 0 ? 'text-destructive' : 'text-positive'}`}
                >
                  {c.amount < 0 ? '-' : '+'}
                  {formatCurrency(Math.abs(c.amount), true, hideAmounts)}
                </span>
                <button
                  onClick={() => onDeleteContribution(c.id)}
                  aria-label="Delete contribution"
                  className="text-muted-foreground shrink-0"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
