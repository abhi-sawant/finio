import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import {
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  ArrowDown,
  ChevronLeft,
  ChevronRight,
  Trophy,
} from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { buildYearInReview } from '@/utils/analytics';
import { normalizeMonthStartDay, periodRange, shiftPeriod } from '@/utils/period';
import {
  formatCurrency,
  formatPercentChange,
  formatShortDate,
  shouldCompactGroup,
} from '@/utils/formatters';
import { ChartDataTable } from '@/components/charts/ChartDataTable';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

function ratio(current: number, previous: number): number | null {
  if (previous === 0) return null;
  return (current - previous) / previous;
}

/** For spending, up is bad; for income and net, up is good. */
function ChangeBadge({ value, invert = false }: { value: number | null; invert?: boolean }) {
  if (value === null) return <span className="text-muted-foreground text-xs">new this year</span>;
  const isGood = invert ? value < 0 : value > 0;
  const Icon = value > 0 ? ArrowUp : ArrowDown;
  if (Math.round(value * 100) === 0) {
    return <span className="text-muted-foreground text-xs">flat vs last year</span>;
  }
  return (
    <span
      className={`inline-flex items-center gap-0.5 text-xs font-medium ${isGood ? 'text-positive' : 'text-destructive'}`}
    >
      <Icon size={11} aria-hidden />
      <span className="sr-only">{value > 0 ? 'up ' : 'down '}</span>
      {formatPercentChange(value).replace(/^[+-]/, '')} vs last year
    </span>
  );
}

export default function YearInReview() {
  const navigate = useNavigate();
  const transactions = useFinanceStore((s) => s.transactions);
  const accounts = useFinanceStore((s) => s.accounts);
  const categories = useFinanceStore((s) => s.categories);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));

  // 0 is the financial year in progress; negative steps walk backwards through history.
  const [yearOffset, setYearOffset] = useState(0);

  const review = useMemo(
    () => buildYearInReview({ transactions, accounts, monthStartDay, yearOffset }),
    [transactions, accounts, monthStartDay, yearOffset],
  );

  const money = (value: number) => formatCurrency(value, true, hideAmounts);
  const categoryFor = (id: string) => categories.find((c) => c.id === id);
  const heroCompact = shouldCompactGroup([
    review.current.income,
    review.current.expenses,
    review.current.net,
  ]);
  const busiestExpenses = review.busiestMonth?.expenses ?? 0;

  // How far back the year navigation may go: the financial year holding the oldest transaction.
  const minYearOffset = useMemo(() => {
    if (transactions.length === 0) return 0;
    const earliest = transactions.reduce(
      (min, t) => (t.date < min ? t.date : min),
      transactions[0].date,
    );
    const earliestTime = new Date(earliest).getTime();
    let range = periodRange('yearly', new Date(), monthStartDay);
    let offset = 0;
    // Bounded — a corrupt far-past date shouldn't spin this loop.
    while (range.start.getTime() > earliestTime && offset > -100) {
      range = shiftPeriod(range, -1);
      offset -= 1;
    }
    return offset;
  }, [transactions, monthStartDay]);

  const isEmpty = review.current.transactionCount === 0 && review.previous.transactionCount === 0;

  const yearNav = (
    <div className="flex items-center justify-center gap-3">
      <button
        onClick={() => setYearOffset((o) => Math.max(minYearOffset, o - 1))}
        disabled={yearOffset <= minYearOffset}
        className="hover:bg-muted text-muted-foreground flex h-8 w-8 items-center justify-center rounded-full transition-colors disabled:opacity-30"
        aria-label="Previous year"
      >
        <ChevronLeft size={16} />
      </button>
      <span className="min-w-20 text-center text-lg font-bold">{review.label}</span>
      <button
        onClick={() => setYearOffset((o) => Math.min(0, o + 1))}
        disabled={yearOffset >= 0}
        className="hover:bg-muted text-muted-foreground flex h-8 w-8 items-center justify-center rounded-full transition-colors disabled:opacity-30"
        aria-label="Next year"
      >
        <ChevronRight size={16} />
      </button>
    </div>
  );

  return (
    <>
      <Header innerClassName="lg:max-w-2xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Year in review</h1>
        <HideAmountsToggle />
      </Header>

      <Main className="lg:max-w-2xl">
        {yearNav}

        {isEmpty ? (
          <p className="text-muted-foreground py-12 text-center text-sm">
            {transactions.length > 0
              ? `No activity in ${review.label}`
              : 'Add some transactions to see a year in review.'}
          </p>
        ) : (
          <>
            {/* Hero */}
            <div className="card-elevated bg-grad-surface rounded-md p-4">
              <div className="grid grid-cols-3 gap-3 text-center">
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Income</p>
                  <p className="text-positive font-money text-base">
                    {formatCurrency(review.current.income, true, hideAmounts, {
                      forceCompact: heroCompact,
                    })}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Expenses</p>
                  <p className="text-foreground font-money text-base">
                    {formatCurrency(review.current.expenses, true, hideAmounts, {
                      forceCompact: heroCompact,
                    })}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Net</p>
                  <p
                    className={`font-money text-base ${review.current.net >= 0 ? 'text-positive' : 'text-destructive'}`}
                  >
                    {formatCurrency(review.current.net, true, hideAmounts, {
                      forceCompact: heroCompact,
                    })}
                  </p>
                </div>
              </div>
              <div className="border-border mt-3 grid grid-cols-3 gap-3 border-t pt-2 text-center">
                <ChangeBadge value={ratio(review.current.income, review.previous.income)} />
                <ChangeBadge
                  value={ratio(review.current.expenses, review.previous.expenses)}
                  invert
                />
                <ChangeBadge value={ratio(review.current.net, review.previous.net)} />
              </div>
            </div>

            {/* Net worth */}
            <div className="card-elevated rounded-md p-4">
              <h3 className="mb-3 text-sm font-semibold">Net worth</h3>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Start of year</p>
                  <p className="text-sm font-semibold">{money(review.netWorthStart)}</p>
                </div>
                <ArrowRight size={16} className="text-muted-foreground shrink-0" />
                <div className="text-right">
                  <p className="text-muted-foreground text-xs font-medium">
                    {yearOffset === 0 ? 'Now' : 'End of year'}
                  </p>
                  <p className="text-sm font-semibold">{money(review.netWorthEnd)}</p>
                </div>
              </div>
              <p
                className={`mt-2 text-center text-xs font-medium ${review.netWorthChange >= 0 ? 'text-positive' : 'text-destructive'}`}
              >
                {review.netWorthChange >= 0 ? '+' : ''}
                {money(review.netWorthChange)} this year
              </p>
            </div>

            {/* Monthly breakdown */}
            <div className="card-elevated rounded-md p-4">
              <h3 className="mb-3 text-sm font-semibold">Spending by month</h3>
              <div
                className="flex items-end gap-1.5"
                style={{ height: 96 }}
                role="img"
                aria-label={`Spending by month in ${review.label}${
                  review.busiestMonth
                    ? `, highest in ${review.busiestMonth.label} at ${money(review.busiestMonth.expenses)}`
                    : ''
                }. The figures are in the data table below.`}
              >
                {review.monthlyBreakdown.map((month) => (
                  <div key={month.key} className="flex flex-1 flex-col items-center gap-1">
                    <div
                      title={`${month.label}: ${money(month.expenses)}`}
                      className={`w-full rounded-t-sm ${
                        month.key === review.busiestMonth?.key ? 'bg-primary' : 'bg-primary/25'
                      }`}
                      style={{
                        height:
                          busiestExpenses > 0
                            ? `${Math.max(4, (month.expenses / busiestExpenses) * 72)}px`
                            : 4,
                      }}
                    />
                    <span className="text-muted-foreground text-xs" aria-hidden>
                      {month.label}
                    </span>
                  </div>
                ))}
              </div>
              {review.busiestMonth && (
                <p className="text-muted-foreground mt-3 flex items-center justify-center gap-1 text-xs">
                  <Trophy size={12} className="text-warning" aria-hidden />
                  Biggest spend: {review.busiestMonth.label} · {money(review.busiestMonth.expenses)}
                </p>
              )}
              <ChartDataTable
                caption={`Spending by month, ${review.label}`}
                columns={['Month', 'Spent']}
                rows={review.monthlyBreakdown.map((month) => ({
                  key: month.key,
                  cells: [month.label, money(month.expenses)],
                }))}
              />
            </div>

            {/* Top categories */}
            {review.topCategories.length > 0 && (
              <div className="card-elevated rounded-md p-4">
                <h3 className="mb-3 text-sm font-semibold">Top categories</h3>
                <ul className="space-y-2.5">
                  {review.topCategories.map((c) => {
                    const category = categoryFor(c.categoryId);
                    return (
                      <li key={c.categoryId} className="flex items-center gap-2.5">
                        <span className="min-w-0 flex-1 truncate text-xs font-medium">
                          {category?.name ?? 'Uncategorized'}
                        </span>
                        <span className="shrink-0 text-xs font-semibold">{money(c.amount)}</span>
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}

            {/* Biggest movers */}
            {review.movers.length > 0 && (
              <div className="card-elevated rounded-md p-4">
                <h3 className="mb-3 text-sm font-semibold">Biggest movers vs last year</h3>
                <ul className="space-y-2.5">
                  {review.movers.map((mover) => {
                    const category = categoryFor(mover.categoryId);
                    const isUp = mover.change > 0;
                    return (
                      <li key={mover.categoryId} className="flex items-center gap-2.5">
                        <span className="min-w-0 flex-1 truncate text-xs font-medium">
                          {category?.name ?? 'Uncategorized'}
                        </span>
                        <span
                          className={`shrink-0 text-xs font-semibold ${isUp ? 'text-destructive' : 'text-positive'}`}
                        >
                          {isUp ? '+' : '−'}
                          {money(Math.abs(mover.change))}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}

            {/* Biggest single expense */}
            {review.biggestExpense && (
              <div className="card-elevated rounded-md p-4">
                <h3 className="mb-2 text-sm font-semibold">Biggest single expense</h3>
                <div className="flex items-center justify-between gap-2">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">
                      {review.biggestExpense.note ||
                        categoryFor(review.biggestExpense.categoryId)?.name ||
                        'Expense'}
                    </p>
                    <p className="text-muted-foreground text-xs">
                      {formatShortDate(review.biggestExpense.date)}
                    </p>
                  </div>
                  <p className="text-destructive shrink-0 text-sm font-semibold">
                    {money(review.biggestExpense.amount)}
                  </p>
                </div>
              </div>
            )}

            <p className="text-muted-foreground pb-2 text-center text-xs">
              {review.current.transactionCount} transaction
              {review.current.transactionCount === 1 ? '' : 's'} this year
            </p>
          </>
        )}
      </Main>
    </>
  );
}
