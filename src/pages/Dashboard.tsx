import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { differenceInCalendarDays, format, formatDistanceToNow } from 'date-fns';
import {
  Settings,
  Plus,
  PiggyBank,
  AlertTriangle,
  HandCoins,
  ChevronRight,
  ShieldCheck,
} from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useAuthStore } from '@/store/useAuthStore';
import { formatCurrency, formatPercentChange, shouldCompactGroup } from '@/utils/formatters';
import {
  activeAccounts,
  getTotalIncome,
  getTotalExpenses,
  getTotalAccountBalance,
  getTotalCreditOutstanding,
  getTotalDepositValue,
  getCreditCardDueInfo,
  getCurrentMonthTransactions,
  getPreviousMonthTransactions,
  getDashboardStats,
  sortTransactionsDateDesc,
  computeBudgetStatuses,
  computeGoalStatus,
  computePersonBalance,
  BUDGET_NEAR_LIMIT_PERCENT,
  type BudgetStatus,
  type CreditCardDueInfo,
} from '@/utils/calculations';
import { accountDisplayValue, isDepositAccount } from '@/utils/deposit';
import { BudgetProgressBar } from '@/components/budgets/BudgetHealthBadge';
import { NoteCard } from '@/components/ui/note-card';
import { Guilloche } from '@/components/ui/guilloche';
import { ACCOUNT_TYPE_LABEL, noteStyle } from '@/components/accounts/note';
import { GoalIcon } from '@/components/goals/GoalIcon';
import { PersonIcon } from '@/components/people/PersonIcon';
import { normalizeMonthStartDay, periodRange } from '@/utils/period';
import { isRulePaused, nextDueDate } from '@/store/recurring';

import { TransactionItem } from '@/components/transactions/TransactionItem';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import type { Account, RecurringTransaction } from '@/types';

type AlertItem =
  | { kind: 'budget'; status: BudgetStatus; label: string }
  | { kind: 'credit'; account: Account; dueInfo: CreditCardDueInfo }
  | { kind: 'recurring'; rule: RecurringTransaction; label: string; daysUntil: number };

const BUDGET_PERIOD_NOUN = { weekly: 'week', monthly: 'month', yearly: 'year' } as const;

export default function Dashboard() {
  const navigate = useNavigate();
  const accounts = useFinanceStore((s) => s.accounts);
  const transactions = useFinanceStore((s) => s.transactions);
  const categories = useFinanceStore((s) => s.categories);
  const budgets = useFinanceStore((s) => s.budgets);
  const labels = useFinanceStore((s) => s.labels);
  const recurring = useFinanceStore((s) => s.recurring);
  const goals = useFinanceStore((s) => s.goals);
  const goalContributions = useFinanceStore((s) => s.goalContributions);
  const people = useFinanceStore((s) => s.people);
  const debtEntries = useFinanceStore((s) => s.debtEntries);
  const userName = useFinanceStore((s) => s.settings.userName);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const signedIn = useAuthStore((s) => s.token !== null);
  const lastBackupAt = useAuthStore((s) => s.lastBackupAt);

  const [alertsOpen, setAlertsOpen] = useState(false);
  const [howOpen, setHowOpen] = useState(false);

  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));

  const monthTxns = useMemo(
    () => getCurrentMonthTransactions(transactions, monthStartDay),
    [transactions, monthStartDay],
  );
  const prevMonthTxns = useMemo(
    () => getPreviousMonthTransactions(transactions, monthStartDay),
    [transactions, monthStartDay],
  );
  const openAccounts = useMemo(() => activeAccounts(accounts), [accounts]);
  const totalBalance = useMemo(() => getTotalAccountBalance(accounts), [accounts]);
  const creditOutstanding = useMemo(() => getTotalCreditOutstanding(accounts), [accounts]);
  const depositValue = useMemo(() => getTotalDepositValue(accounts), [accounts]);
  const afterDues = totalBalance - creditOutstanding;
  const monthIncome = useMemo(() => getTotalIncome(monthTxns), [monthTxns]);
  const monthExpenses = useMemo(() => getTotalExpenses(monthTxns), [monthTxns]);
  const accountsCompact = useMemo(
    () => shouldCompactGroup(openAccounts.map((a) => accountDisplayValue(a))),
    [openAccounts],
  );
  const recentTxns = useMemo(
    () => sortTransactionsDateDesc(transactions).slice(0, 5),
    [transactions],
  );
  const stats = useMemo(
    () => getDashboardStats(monthTxns, prevMonthTxns, categories, { monthStartDay }),
    [monthTxns, prevMonthTxns, categories, monthStartDay],
  );
  const allBudgetStatuses = useMemo(
    () => computeBudgetStatuses(budgets, transactions, { monthStartDay }),
    [budgets, transactions, monthStartDay],
  );
  const overallBudget = useMemo(
    () => allBudgetStatuses.find((s) => !s.budget.labelId && s.budget.categoryId === '') ?? null,
    [allBudgetStatuses],
  );
  const daysLeftInMonth = useMemo(() => {
    const range = periodRange('monthly', new Date(), monthStartDay);
    return Math.max(1, differenceInCalendarDays(range.end, new Date()) + 1);
  }, [monthStartDay]);
  // The overall budget always gets its own hero card below, so it's excluded from the
  // collapsed alert list — otherwise a near-limit overall budget would state the same fact twice.
  const nearLimitBudgets = useMemo(
    () =>
      allBudgetStatuses
        .filter(
          (s) => s.percent >= BUDGET_NEAR_LIMIT_PERCENT && s.budget.id !== overallBudget?.budget.id,
        )
        .sort((a, b) => Number(b.isOver) - Number(a.isOver) || b.percent - a.percent),
    [allBudgetStatuses, overallBudget],
  );
  const upcomingRecurring = useMemo(() => {
    const now = new Date();
    // Paused and finished rules have no next bill to warn about.
    return recurring
      .filter((rule) => !isRulePaused(rule))
      .flatMap((rule) => {
        const nextDue = nextDueDate(rule);
        if (!nextDue) return [];
        return [{ rule, nextDue, daysUntil: differenceInCalendarDays(nextDue, now) }];
      })
      .filter(({ daysUntil }) => daysUntil >= 0 && daysUntil <= 7)
      .sort((a, b) => a.nextDue.getTime() - b.nextDue.getTime());
  }, [recurring]);
  // Only expense rules draw down the budget, and only those landing before the period ends —
  // a bill due after rollover belongs to next period's budget.
  const upcomingBillsTotal = useMemo(
    () =>
      upcomingRecurring
        .filter(({ rule, daysUntil }) => rule.type === 'expense' && daysUntil < daysLeftInMonth)
        .reduce((sum, { rule }) => sum + rule.amount, 0),
    [upcomingRecurring, daysLeftInMonth],
  );
  // Floored, never rounded: a "safe" figure that rounds up could overspend by a rupee.
  const safePerDay = overallBudget
    ? Math.floor(Math.max(overallBudget.remaining - upcomingBillsTotal, 0) / daysLeftInMonth)
    : 0;
  const periodLabel = useMemo(() => {
    const range = periodRange('monthly', new Date(), monthStartDay);
    return `${format(range.start, 'd MMM')} – ${format(range.end, 'd MMM')}`;
  }, [monthStartDay]);
  const prevMonthIncome = useMemo(() => getTotalIncome(prevMonthTxns), [prevMonthTxns]);
  const prevMonthExpenses = useMemo(() => getTotalExpenses(prevMonthTxns), [prevMonthTxns]);
  // In-progress goals, closest to done first — completed ones have nothing left to track.
  const topGoals = useMemo(
    () =>
      goals
        .map((g) => computeGoalStatus(g, goalContributions))
        .filter((s) => !s.isComplete)
        .sort((a, b) => b.percent - a.percent)
        .slice(0, 2),
    [goals, goalContributions],
  );
  // Anyone with an open balance, biggest first — settled-up people have nothing to show.
  const topDebts = useMemo(
    () =>
      people
        .map((p) => computePersonBalance(p, debtEntries))
        .filter((s) => s.balance !== 0)
        .sort((a, b) => Math.abs(b.balance) - Math.abs(a.balance))
        .slice(0, 3),
    [people, debtEntries],
  );
  const creditDues = useMemo(() => {
    return openAccounts
      .filter((a) => a.type === 'credit')
      .flatMap((account) => {
        const dueInfo = getCreditCardDueInfo(account);
        return dueInfo && dueInfo.daysUntilDue <= 7 ? [{ account, dueInfo }] : [];
      })
      .sort((a, b) => a.dueInfo.daysUntilDue - b.dueInfo.daysUntilDue);
  }, [openAccounts]);

  const dueLabel = (daysUntil: number) =>
    daysUntil < 0
      ? `Overdue by ${Math.abs(daysUntil)} day${Math.abs(daysUntil) === 1 ? '' : 's'}`
      : daysUntil === 0
        ? 'due today'
        : daysUntil === 1
          ? 'due tomorrow'
          : `due in ${daysUntil} days`;

  const attentionItems: AlertItem[] = useMemo(() => {
    const budgetLabel = (s: BudgetStatus) =>
      s.budget.labelId
        ? (labels.find((l) => l.id === s.budget.labelId)?.name ?? 'Unknown label')
        : s.budget.categoryId === ''
          ? 'Overall expenses'
          : (categories.find((c) => c.id === s.budget.categoryId)?.name ?? 'Unknown');
    return [
      ...nearLimitBudgets.map((s) => ({
        kind: 'budget' as const,
        status: s,
        label: budgetLabel(s),
      })),
      ...creditDues.map(({ account, dueInfo }) => ({ kind: 'credit' as const, account, dueInfo })),
      ...upcomingRecurring.map(({ rule, daysUntil }) => {
        const cat = categories.find((c) => c.id === rule.categoryId);
        return {
          kind: 'recurring' as const,
          rule,
          label: rule.note || cat?.name || 'Recurring',
          daysUntil,
        };
      }),
    ];
  }, [nearLimitBudgets, creditDues, upcomingRecurring, labels, categories]);

  const describeAttention = (item: AlertItem): string => {
    if (item.kind === 'budget') {
      return item.status.isOver
        ? `${item.label} is ${formatCurrency(item.status.spent - item.status.limit, true, hideAmounts)} over budget`
        : `${item.label} is near its limit`;
    }
    if (item.kind === 'credit') {
      return `${item.account.name} ${dueLabel(item.dueInfo.daysUntilDue)}`;
    }
    return `${item.label} ${item.daysUntil === 0 ? 'posts today' : item.daysUntil === 1 ? 'posts tomorrow' : `posts in ${item.daysUntil} days`}`;
  };

  const attentionDetail = (item: AlertItem): string => {
    if (item.kind === 'budget') {
      return `${formatCurrency(item.status.spent, true, hideAmounts)} of ${formatCurrency(item.status.limit, true, hideAmounts)} · ${Math.round(item.status.percent)}%`;
    }
    if (item.kind === 'credit') {
      return `${formatCurrency(item.dueInfo.outstanding, true, hideAmounts)} · min ${formatCurrency(item.dueInfo.minimumDue, true, hideAmounts)}`;
    }
    return `Recurring · from ${accounts.find((a) => a.id === item.rule.accountId)?.name ?? 'account'}`;
  };

  const goToAttentionItem = (item: AlertItem) => {
    setAlertsOpen(false);
    if (item.kind === 'budget') navigate('/budgets');
    else if (item.kind === 'credit') navigate('/accounts');
    else navigate('/recurring');
  };

  const topAttention = attentionItems[0];

  return (
    <>
      {/* Header */}
      <Header>
        <div>
          <h1 className="text-2xl font-semibold">{userName}</h1>
          <p className="text-muted-foreground mt-0.5 flex items-center gap-1 text-xs">
            <ShieldCheck size={12} className="text-positive shrink-0" aria-hidden />
            {signedIn
              ? lastBackupAt
                ? `Kept on this device · backed up ${formatDistanceToNow(new Date(lastBackupAt), { addSuffix: true })}`
                : 'Kept on this device · cloud backup on'
              : 'Kept only on this device'}
          </p>
        </div>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton onClick={() => navigate('/settings')} aria-label="Settings">
            <Settings />
          </HeaderIconButton>
        </div>
      </Header>

      <Main className="flex flex-col gap-4 space-y-0 lg:grid lg:grid-cols-2 lg:items-start lg:space-y-0 lg:gap-x-8 lg:gap-y-6">
        {/* Hero */}
        <NoteCard className="lg:col-span-2 lg:row-start-1">
          {overallBudget ? (
            <>
              <p className="text-muted-foreground text-sm font-medium">Safe to spend today</p>
              <p
                className={`font-money mt-1 text-[2.75rem] leading-[1.05] ${overallBudget.isOver ? 'text-destructive' : ''}`}
              >
                {formatCurrency(safePerDay, false, hideAmounts, { precise: false })}
              </p>
              <p className="mt-1.5 text-sm text-balance">
                {overallBudget.isOver
                  ? `Over budget by ${formatCurrency(Math.abs(overallBudget.remaining), false, hideAmounts)}`
                  : `${formatCurrency(overallBudget.remaining, false, hideAmounts)} left this ${BUDGET_PERIOD_NOUN[overallBudget.budget.period]}`}
                {' · '}
                {daysLeftInMonth} day{daysLeftInMonth === 1 ? '' : 's'} to go
              </p>
              {upcomingBillsTotal > 0 && !overallBudget.isOver && (
                <p className="text-muted-foreground mt-1 text-xs">
                  After {formatCurrency(upcomingBillsTotal, true, hideAmounts)} of bills due this
                  week
                </p>
              )}
              {!overallBudget.isOver && (
                <button
                  onClick={() => setHowOpen(true)}
                  className="text-primary -mx-2 -my-2 mt-1 px-2 py-3 text-xs font-medium hover:underline"
                >
                  How this is worked out
                </button>
              )}
              {overallBudget.spent > 0 && (
                <>
                  <div className="mt-3">
                    <BudgetProgressBar
                      status={overallBudget}
                      okFill="var(--register)"
                      valueText={`${formatCurrency(overallBudget.spent, true, hideAmounts)} of ${formatCurrency(overallBudget.limit, true, hideAmounts)} spent`}
                    />
                  </div>
                  <p className="text-muted-foreground mt-1.5 text-xs" aria-hidden>
                    Spent {formatCurrency(overallBudget.spent, true, hideAmounts)} of{' '}
                    {formatCurrency(overallBudget.limit, true, hideAmounts)}
                  </p>
                </>
              )}
            </>
          ) : (
            <>
              <p className="text-muted-foreground text-sm font-medium">Total balance</p>
              <p className="font-money mt-1 text-[2.75rem] leading-[1.05]">
                {formatCurrency(totalBalance, false, hideAmounts)}
              </p>
              {creditOutstanding > 0 && (
                <p className="text-muted-foreground mt-1.5 text-xs">
                  {formatCurrency(afterDues, false, hideAmounts)} after card dues
                </p>
              )}
              {depositValue > 0 && (
                <p className="text-muted-foreground mt-1 text-xs">
                  + {formatCurrency(depositValue, false, hideAmounts)} locked in deposits
                </p>
              )}
              {accounts.length > 0 && (
                <button
                  onClick={() => navigate('/budgets')}
                  className="bg-grad-primary shadow-glow-primary mt-4 rounded-full px-5 py-2 text-sm font-medium text-white"
                >
                  Set a monthly budget
                </button>
              )}
            </>
          )}
        </NoteCard>

        {/* One alert surfaced, the rest collapsed behind a review sheet */}
        {topAttention && (
          <button
            onClick={() => setAlertsOpen(true)}
            className="bg-warning-band w-full rounded-lg border border-[var(--glass-border)] p-4 text-left shadow-[var(--shadow-card)] lg:col-span-2 lg:row-start-2"
          >
            <div className="flex items-center gap-3">
              <AlertTriangle size={18} className="text-warning-band-accent shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="text-warning-band-foreground text-sm font-semibold">
                  {describeAttention(topAttention)}
                </p>
                {attentionItems.length > 1 && (
                  <p className="text-warning-band-foreground mt-0.5 text-xs opacity-80">
                    {attentionItems.length - 1}{' '}
                    {attentionItems.length - 1 === 1 ? 'more thing needs' : 'more things need'}{' '}
                    attention this week
                  </p>
                )}
              </div>
              <span className="text-warning-band-accent shrink-0 text-xs font-semibold">
                Review
              </span>
            </div>
          </button>
        )}

        <Dialog open={alertsOpen} onOpenChange={setAlertsOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Needs attention</DialogTitle>
            </DialogHeader>
            <p className="text-muted-foreground -mt-2 text-xs">
              This week only. Everything else is fine.
            </p>
            <div className="-mx-4 max-h-96 overflow-y-auto">
              {attentionItems.map((item, i) => (
                <button
                  key={i}
                  onClick={() => goToAttentionItem(item)}
                  className="border-border hover:bg-muted/40 flex w-full items-center justify-between gap-3 border-b px-4 py-3 text-left last:border-b-0"
                >
                  <span className="min-w-0 flex-1 truncate text-sm font-medium">
                    {describeAttention(item)}
                  </span>
                  <span className="text-muted-foreground shrink-0 text-xs">
                    {attentionDetail(item)}
                  </span>
                </button>
              ))}
            </div>
          </DialogContent>
        </Dialog>

        <Dialog open={howOpen} onOpenChange={setHowOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>How "safe to spend" is worked out</DialogTitle>
            </DialogHeader>
            {overallBudget && (
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Budget left this month</dt>
                  <dd className="font-medium">
                    {formatCurrency(overallBudget.remaining, false, hideAmounts)}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Bills due before the month ends</dt>
                  <dd className="font-medium">
                    − {formatCurrency(upcomingBillsTotal, false, hideAmounts)}
                  </dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">Days to go</dt>
                  <dd className="font-medium">÷ {daysLeftInMonth}</dd>
                </div>
                <div className="border-border flex justify-between gap-4 border-t pt-2">
                  <dt className="font-semibold">Safe to spend each day</dt>
                  <dd className="text-primary font-semibold">
                    {formatCurrency(safePerDay, false, hideAmounts, { precise: false })}
                  </dd>
                </div>
              </dl>
            )}
            <p className="text-muted-foreground text-xs">
              Rounded down to the rupee. Only bills from your recurring list count, and card
              payments you haven't scheduled aren't included.
            </p>
          </DialogContent>
        </Dialog>

        {/* Where it sits */}
        {/* Source order is the mobile reading order (and so focus/screen-reader order); desktop
            places the same sections in two columns with explicit grid spans, no visual reordering. */}
        <div className="flex flex-col gap-4 lg:col-span-2 lg:row-start-3 lg:grid lg:grid-cols-2 lg:items-start lg:gap-x-8 lg:gap-y-6">
          <section className="lg:col-start-1 lg:row-start-1">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-semibold">Where it sits</h2>
              <button
                onClick={() => navigate('/accounts')}
                className="text-primary -my-3 px-1 py-3.5 text-xs font-medium hover:underline"
              >
                See all
              </button>
            </div>
            {overallBudget && accounts.length > 0 && (
              <div className="mb-3 flex items-baseline justify-between gap-3">
                <span className="text-muted-foreground text-xs">Total balance</span>
                <div className="text-right">
                  <p className="font-money text-xl">
                    {formatCurrency(totalBalance, false, hideAmounts)}
                  </p>
                  {creditOutstanding > 0 && (
                    <p className="text-muted-foreground text-xs">
                      {formatCurrency(afterDues, false, hideAmounts)} after card dues
                    </p>
                  )}
                  {depositValue > 0 && (
                    <p className="text-muted-foreground text-xs">
                      + {formatCurrency(depositValue, false, hideAmounts)} locked in deposits
                    </p>
                  )}
                </div>
              </div>
            )}
            {accounts.length === 0 ? (
              <button
                onClick={() => navigate('/add-account')}
                className="border-border hover:bg-muted flex w-full flex-col items-center gap-2 rounded-md border-2 border-dashed p-6 transition-colors"
              >
                <Plus size={24} className="text-primary" />
                <span className="text-primary text-sm font-medium">Add your first account</span>
                <span className="text-muted-foreground text-center text-xs">
                  You need at least one account to record transactions.
                </span>
              </button>
            ) : (
              <div className="scrollbar-hide -mx-3 flex snap-x scroll-px-3 gap-3 overflow-x-auto px-3 pt-1 pb-3 lg:mx-0 lg:grid lg:grid-cols-3 lg:overflow-x-visible lg:px-0">
                {openAccounts.map((account) => {
                  const value = accountDisplayValue(account);
                  return (
                    <button
                      key={account.id}
                      onClick={() => navigate(`/edit-account/${account.id}`)}
                      className="note-tile w-40 shrink-0 snap-start lg:w-auto"
                      style={noteStyle(account.type)}
                    >
                      <Guilloche className="note-tile-rosette" petals={14} rings={5} />
                      <span className="relative line-clamp-2 text-sm leading-tight font-semibold break-words">
                        {account.name}
                      </span>
                      <span className="relative truncate text-xs opacity-80">
                        {isDepositAccount(account)
                          ? `${account.type === 'rd' ? 'RD' : 'FD'} · current value`
                          : ACCOUNT_TYPE_LABEL[account.type]}
                      </span>
                      <span className="font-money relative mt-auto pt-3 text-lg">
                        {formatCurrency(value, true, hideAmounts, {
                          forceCompact: accountsCompact,
                        })}
                      </span>
                    </button>
                  );
                })}
              </div>
            )}
          </section>
          <section className="lg:col-start-2 lg:row-span-2 lg:row-start-1">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-semibold">Latest</h2>
              <button
                onClick={() => navigate('/transactions')}
                className="text-primary -my-3 px-1 py-3.5 text-xs font-medium hover:underline"
              >
                See all
              </button>
            </div>
            {recentTxns.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-muted-foreground text-sm">No transactions yet.</p>
                {accounts.length > 0 && (
                  <button
                    onClick={() => navigate('/add-transaction')}
                    className="text-primary mt-1 px-2 py-3 text-sm font-medium hover:underline"
                  >
                    Add your first transaction
                  </button>
                )}
              </div>
            ) : (
              <div className="card-elevated divide-border divide-y rounded-md">
                {recentTxns.map((tx) => (
                  <TransactionItem
                    key={tx.id}
                    transaction={tx}
                    categories={categories}
                    accounts={accounts}
                    labels={labels}
                    showDate
                    onClick={() => navigate(`/edit-transaction/${tx.id}`)}
                  />
                ))}
              </div>
            )}
          </section>

          {/* This month — IN/OUT/DAILY AVG/SAVED/TOP in one plain card, no tinted tiles */}
          <section className="lg:col-start-2 lg:row-start-3">
            <div className="mb-3 flex items-baseline justify-between gap-3">
              <h2 className="text-base font-semibold">This month</h2>
              <span className="text-muted-foreground text-xs">{periodLabel}</span>
            </div>
            <div className="card-elevated rounded-md p-4">
              {monthTxns.length > 0 ? (
                <div className="grid grid-cols-3 gap-3 lg:grid-cols-5">
                  <div>
                    <p className="text-muted-foreground text-xs font-medium">In</p>
                    <p className="font-money mt-0.5 text-base">
                      {formatCurrency(monthIncome, true, hideAmounts)}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs font-medium">Out</p>
                    <p className="font-money mt-0.5 text-base">
                      {formatCurrency(monthExpenses, true, hideAmounts)}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs font-medium">Daily avg</p>
                    <p className="font-money mt-0.5 text-base">
                      {formatCurrency(stats.dailyAverage, true, hideAmounts, { precise: false })}
                    </p>
                  </div>
                  <div>
                    <p className="text-muted-foreground text-xs font-medium">Saved</p>
                    <p className="font-money mt-0.5 text-base">
                      {Math.round(stats.savingsRate * 100)}%
                      {stats.savingsRateChange !== null && (
                        <span className="text-muted-foreground ml-1 font-sans text-xs font-normal tracking-normal">
                          {formatPercentChange(stats.savingsRateChange)}
                        </span>
                      )}
                    </p>
                  </div>
                  {stats.topCategory && (
                    <div>
                      <p className="text-muted-foreground text-xs font-medium">Top</p>
                      <p className="mt-0.5 truncate text-sm font-semibold">
                        {stats.topCategory.category.name}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-muted-foreground text-center text-sm">
                  {overallBudget
                    ? 'Nothing logged yet this month.'
                    : 'Set a budget and this becomes "safe to spend"'}
                </p>
              )}
              {monthTxns.length === 0 && overallBudget && prevMonthTxns.length > 0 && (
                <p className="text-muted-foreground mt-1 text-center text-xs">
                  Last month: {formatCurrency(prevMonthIncome, true, hideAmounts)} in ·{' '}
                  {formatCurrency(prevMonthExpenses, true, hideAmounts)} out
                </p>
              )}
            </div>
          </section>
          {(topGoals.length > 0 || topDebts.length > 0) && (
            <section className="lg:col-start-1 lg:row-span-2 lg:row-start-2">
              <h2 className="mb-3 text-base font-semibold">Also tracking</h2>
              <div className="card-elevated divide-border divide-y rounded-md">
                {topGoals.length > 0 && (
                  <button onClick={() => navigate('/goals')} className="w-full p-4 text-left">
                    <div className="mb-3 flex items-center gap-2">
                      <PiggyBank size={16} className="text-primary" />
                      <h3 className="text-sm font-semibold">Savings Goals</h3>
                      <ChevronRight size={14} className="text-muted-foreground ml-auto" />
                    </div>
                    <div className="space-y-3">
                      {topGoals.map((s) => (
                        <div key={s.goal.id}>
                          <div className="mb-1 flex items-center justify-between gap-2">
                            <span className="flex min-w-0 flex-1 items-center gap-1.5 text-xs font-medium">
                              <span className="shrink-0">
                                <GoalIcon icon={s.goal.icon} size={12} color={s.goal.color} />
                              </span>
                              <span className="truncate">{s.goal.name}</span>
                            </span>
                            <span className="text-muted-foreground shrink-0 text-xs font-semibold">
                              {formatCurrency(s.current, false, hideAmounts)} /{' '}
                              {formatCurrency(s.goal.targetAmount, false, hideAmounts)}
                            </span>
                          </div>
                          <div className="bg-muted h-2 overflow-hidden rounded-full">
                            <div
                              className="thread-fill h-full"
                              style={{
                                width: `${Math.min(Math.max(s.percent, 0), 100)}%`,
                              }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </button>
                )}
                {topDebts.length > 0 && (
                  <button onClick={() => navigate('/debts')} className="w-full p-4 text-left">
                    <div className="mb-3 flex items-center gap-2">
                      <HandCoins size={16} className="text-primary" />
                      <h3 className="text-sm font-semibold">Debts & Lending</h3>
                      <ChevronRight size={14} className="text-muted-foreground ml-auto" />
                    </div>
                    <div className="divide-border divide-y">
                      {topDebts.map((s) => (
                        <div
                          key={s.person.id}
                          className="flex items-center gap-3 py-2 first:pt-0 last:pb-0"
                        >
                          <PersonIcon icon={s.person.icon} size={14} color={s.person.color} />
                          <p className="min-w-0 flex-1 truncate text-xs font-medium">
                            {s.person.name}
                          </p>
                          <p
                            className={`shrink-0 text-xs font-semibold ${s.balance > 0 ? 'text-positive' : 'text-destructive'}`}
                          >
                            {s.balance > 0 ? 'Owes you ' : 'You owe '}
                            {formatCurrency(Math.abs(s.balance), true, hideAmounts)}
                          </p>
                        </div>
                      ))}
                    </div>
                  </button>
                )}
              </div>
            </section>
          )}
        </div>
      </Main>
    </>
  );
}
