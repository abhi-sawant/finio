import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { Target, ChevronRight, Repeat, CalendarIcon, CalendarX } from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { formatCurrency, formatShortDate, shouldCompactGroup } from '@/utils/formatters';
import { getTotalIncome, getTotalExpenses } from '@/utils/calculations';
import { isRulePaused } from '@/store/recurring';
import { SpendingDonut } from '@/components/charts/SpendingDonut';
import { IncomeExpenseBar } from '@/components/charts/IncomeExpenseBar';
import { BalanceTrend } from '@/components/charts/BalanceTrend';
import { LabelSpendingBar } from '@/components/charts/LabelSpendingBar';
import { InsightsFeed } from '@/components/analytics/InsightsFeed';
import { PeriodComparison } from '@/components/analytics/PeriodComparison';
import { SpendingHeatmap } from '@/components/analytics/SpendingHeatmap';
import { TopMerchants } from '@/components/analytics/TopMerchants';
import { CashFlowForecast } from '@/components/analytics/CashFlowForecast';
import { CashFlowCalendar } from '@/components/analytics/CashFlowCalendar';
import { NetWorthTrend } from '@/components/analytics/NetWorthTrend';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import Header from '@/components/ui/header';
import Main from '@/components/ui/main';
import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Calendar } from '@/components/ui/calendar';
import type { DateRange } from 'react-day-picker';
import { parseISO, startOfDay, endOfDay, subMonths } from 'date-fns';
import { monthPeriodStart, normalizeMonthStartDay, yearPeriodStart } from '@/utils/period';

type FilterType = 'all' | 'month' | '3months' | '6months' | 'year' | 'custom';

const FILTER_CHIPS: { value: Exclude<FilterType, 'custom'>; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'month', label: 'This month' },
  { value: '3months', label: 'Last 3 months' },
  { value: '6months', label: 'Last 6 months' },
  { value: 'year', label: 'This year' },
];

export default function Analytics() {
  const navigate = useNavigate();
  const transactions = useFinanceStore((s) => s.transactions);
  const budgets = useFinanceStore((s) => s.budgets);
  const recurring = useFinanceStore((s) => s.recurring);
  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const [twoMonths, setTwoMonths] = useState(
    () => typeof window === 'undefined' || window.matchMedia('(min-width: 768px)').matches,
  );
  useEffect(() => {
    const mq = window.matchMedia('(min-width: 768px)');
    const onChange = () => setTwoMonths(mq.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);
  const [selectedFilter, setSelectedFilter] = useState<FilterType>('month');
  const [date, setDate] = React.useState<DateRange | undefined>(undefined);

  const dateRange = useMemo(() => {
    const now = new Date();
    // Month windows follow the user's financial month, so Analytics agrees with the
    // Dashboard and with monthly budgets rather than snapping to the 1st.
    const monthStart = (d: Date) => monthPeriodStart(d, monthStartDay);
    if (selectedFilter === 'month') return { from: monthStart(now), to: now };
    if (selectedFilter === '3months') return { from: monthStart(subMonths(now, 2)), to: now };
    if (selectedFilter === '6months') return { from: monthStart(subMonths(now, 5)), to: now };
    if (selectedFilter === 'year') return { from: yearPeriodStart(now, monthStartDay), to: now };
    if (selectedFilter === 'custom' && date?.from) {
      return { from: startOfDay(date.from), to: date.to ? endOfDay(date.to) : endOfDay(date.from) };
    }
    // 'all': use earliest transaction date
    const sorted = [...transactions].sort((a, b) => a.date.localeCompare(b.date));
    return {
      from: sorted.length > 0 ? startOfDay(parseISO(sorted[0].date)) : monthStart(now),
      to: now,
    };
  }, [selectedFilter, date, transactions, monthStartDay]);

  const filteredTransactions = useMemo(() => {
    if (selectedFilter === 'all') return transactions;
    if (selectedFilter === 'custom' && !date?.from) return transactions;
    const { from, to } = dateRange;
    return transactions.filter((t) => {
      const d = parseISO(t.date);
      return d >= from && d <= to;
    });
  }, [selectedFilter, transactions, dateRange, date]);

  const totalIncome = useMemo(() => getTotalIncome(filteredTransactions), [filteredTransactions]);
  const totalExpenses = useMemo(
    () => getTotalExpenses(filteredTransactions),
    [filteredTransactions],
  );
  const net = totalIncome - totalExpenses;
  const summaryCompact = useMemo(
    () => shouldCompactGroup([totalIncome, totalExpenses, net]),
    [totalIncome, totalExpenses, net],
  );
  const activeRecurringCount = useMemo(
    () => recurring.filter((rule) => !isRulePaused(rule)).length,
    [recurring],
  );

  const handleFilterChange = (filter: FilterType) => {
    setSelectedFilter(filter);
    if (filter !== 'custom') setDate(undefined);
  };

  const handleDateSelect = (range: DateRange | undefined) => {
    setDate(range);
    if (range?.from) setSelectedFilter('custom');
  };

  return (
    <>
      <Header>
        <h1 className="text-2xl font-bold tracking-tight">Analytics</h1>
        <HideAmountsToggle />
      </Header>

      <Main>
        {transactions.length === 0 ? (
          <p className="text-muted-foreground py-12 text-center text-sm">
            Add transactions to see analytics
          </p>
        ) : (
          <>
            <div className="scrollbar-hide flex items-center gap-2 overflow-x-auto py-2 lg:flex-wrap lg:overflow-x-visible">
              {FILTER_CHIPS.map((chip) => (
                <Button
                  key={chip.value}
                  variant={selectedFilter === chip.value ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => handleFilterChange(chip.value)}
                >
                  {chip.label}
                </Button>
              ))}
              <Popover>
                <PopoverTrigger
                  render={
                    <Button
                      variant={selectedFilter === 'custom' ? 'default' : 'outline'}
                      id="date-picker-range"
                      className="justify-start px-2.5 font-normal"
                    >
                      <CalendarIcon />
                      {date?.from ? (
                        date.to ? (
                          <>
                            {formatShortDate(date.from)} - {formatShortDate(date.to)}
                          </>
                        ) : (
                          formatShortDate(date.from)
                        )
                      ) : (
                        <span>Pick a date</span>
                      )}
                    </Button>
                  }
                />
                <PopoverContent
                  className="w-auto max-w-[calc(100vw-1rem)] overflow-auto p-0"
                  align="start"
                >
                  <Calendar
                    mode="range"
                    defaultMonth={date?.from}
                    selected={date}
                    onSelect={handleDateSelect}
                    numberOfMonths={twoMonths ? 2 : 1}
                  />
                </PopoverContent>
              </Popover>
            </div>
            {/* Period Summary */}
            <div className="card-elevated bg-grad-surface rounded-md p-4">
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Income</p>
                  <p className="text-positive font-money text-base">
                    {formatCurrency(totalIncome, true, hideAmounts, {
                      forceCompact: summaryCompact,
                    })}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Expenses</p>
                  <p className="text-foreground font-money text-base">
                    {formatCurrency(totalExpenses, true, hideAmounts, {
                      forceCompact: summaryCompact,
                    })}
                  </p>
                </div>
                <div>
                  <p className="text-muted-foreground text-xs font-medium">Net</p>
                  <p
                    className={`font-money text-base ${net >= 0 ? 'text-positive' : 'text-destructive'}`}
                  >
                    {formatCurrency(net, true, hideAmounts, { forceCompact: summaryCompact })}
                  </p>
                </div>
              </div>
            </div>

            {/* Insights — always about the current month, so it sits outside the filter. */}
            <InsightsFeed />

            {filteredTransactions.length === 0 ? (
              <div className="card-elevated rounded-md px-4 py-10 text-center">
                <CalendarX size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
                <p className="text-sm font-medium">Nothing in this period</p>
                <p className="text-muted-foreground mt-1 text-xs">
                  No transactions fall in the selected range — try a wider one.
                </p>
                <Button
                  variant="outline"
                  className="mt-4 rounded-full px-5"
                  onClick={() => handleFilterChange('all')}
                >
                  Show all time
                </Button>
              </div>
            ) : (
              // Each card hides itself when it has nothing to draw, so an odd card can be left
              // last — it then spans both columns rather than leaving an empty cell beside it.
              <div className="space-y-4 lg:grid lg:grid-cols-2 lg:gap-4 lg:space-y-0 lg:[&>*:last-child:nth-child(odd)]:col-span-2">
                <SpendingDonut transactions={filteredTransactions} />
                <IncomeExpenseBar transactions={filteredTransactions} />
                <BalanceTrend from={dateRange.from} to={dateRange.to} />
                <LabelSpendingBar transactions={filteredTransactions} />
                <TopMerchants transactions={filteredTransactions} />
              </div>
            )}

            {/*
              The cards below each carry their own window — a forecast, a trend and a
              period-over-period view are all anchored to "now" rather than to whatever
              range the filter chips above are showing.
            */}
            <CashFlowForecast />
            <CashFlowCalendar />
            <NetWorthTrend />
            <PeriodComparison />
            <SpendingHeatmap />
          </>
        )}

        {/* Tools */}
        <div className="card-elevated divide-border divide-y rounded-md">
          <button
            onClick={() => navigate('/budgets')}
            className="flex w-full items-center justify-between px-4 py-3"
          >
            <div className="flex items-center gap-3">
              <Target size={18} className="text-muted-foreground shrink-0" aria-hidden />
              <div className="text-left">
                <p className="text-sm font-medium">Budgets</p>
                <p className="text-muted-foreground text-xs">
                  {budgets.length === 0 ? 'Set monthly limits' : `${budgets.length} active`}
                </p>
              </div>
            </div>
            <ChevronRight size={16} className="text-muted-foreground" />
          </button>
          <button
            onClick={() => navigate('/recurring')}
            className="flex w-full items-center justify-between px-4 py-3"
          >
            <div className="flex items-center gap-3">
              <Repeat size={18} className="text-muted-foreground shrink-0" aria-hidden />
              <div className="text-left">
                <p className="text-sm font-medium">Recurring transactions</p>
                <p className="text-muted-foreground text-xs">
                  {activeRecurringCount === 0
                    ? 'Automate repeating items'
                    : `${activeRecurringCount} active`}
                </p>
              </div>
            </div>
            <ChevronRight size={16} className="text-muted-foreground" />
          </button>
        </div>
      </Main>
    </>
  );
}
