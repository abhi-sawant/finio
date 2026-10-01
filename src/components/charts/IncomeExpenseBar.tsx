import { useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, CartesianGrid, Tooltip } from 'recharts';
import { format, parseISO } from 'date-fns';
import { useFinanceStore } from '@/store/useFinanceStore';
import { formatCurrency } from '@/utils/formatters';
import { monthPeriodStart, normalizeMonthStartDay } from '@/utils/period';
import type { Transaction } from '@/types';
import { ChartDataTable } from './ChartDataTable';

interface Props {
  transactions: Transaction[];
}

export function IncomeExpenseBar({ transactions }: Props) {
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));
  const data = useMemo(() => {
    const monthMap = new Map<string, { income: number; expenses: number }>();

    for (const t of transactions) {
      if (t.type !== 'income' && t.type !== 'expense') continue;
      // The financial month this falls in, in local time — not the UTC `YYYY-MM` prefix.
      const key = format(monthPeriodStart(parseISO(t.date), monthStartDay), 'yyyy-MM-dd');
      const entry = monthMap.get(key) ?? { income: 0, expenses: 0 };
      if (t.type === 'income') entry.income += t.amount;
      else entry.expenses += t.amount;
      monthMap.set(key, entry);
    }

    return Array.from(monthMap.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, { income, expenses }]) => {
        return { key, month: format(parseISO(key), 'MMM yy'), income, expenses };
      });
  }, [transactions, monthStartDay]);

  const hasData = data.some((d) => d.income > 0 || d.expenses > 0);
  if (!hasData) return null;

  const money = (value: number) => formatCurrency(value, true, hideAmounts);
  const totals = data.reduce(
    (sum, d) => ({ income: sum.income + d.income, expenses: sum.expenses + d.expenses }),
    { income: 0, expenses: 0 },
  );

  return (
    <div className="card-elevated rounded-md p-4">
      <h3 className="mb-3 text-sm font-semibold">Income vs Expenses</h3>
      <div
        className="h-48 lg:h-64"
        role="img"
        aria-label={`Income against expenses across ${data.length} month${
          data.length === 1 ? '' : 's'
        }: ${money(totals.income)} earned, ${money(totals.expenses)} spent in total.`}
      >
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} barGap={4} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" opacity={0.12} />
            <XAxis dataKey="month" fontSize={11} tickLine={false} axisLine={false} />
            <YAxis
              fontSize={10}
              tickLine={false}
              axisLine={false}
              width={56}
              tickMargin={4}
              tickFormatter={money}
            />
            <Tooltip
              cursor={{ fill: 'var(--muted)', fillOpacity: 0.5 }}
              contentStyle={{
                background: 'var(--card)',
                border: '1px solid var(--border)',
                borderRadius: 12,
                fontSize: 12,
              }}
              formatter={(v) => formatCurrency(Number(v) || 0, false, hideAmounts)}
            />
            <Bar dataKey="income" fill="var(--primary)" radius={[6, 6, 0, 0]} />
            <Bar dataKey="expenses" fill="var(--destructive)" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <div className="mt-2 flex justify-center gap-4">
        <div className="flex items-center gap-1.5 text-xs">
          <div className="bg-primary h-2.5 w-2.5 rounded-full" aria-hidden />
          <span className="text-muted-foreground">Income</span>
        </div>
        <div className="flex items-center gap-1.5 text-xs">
          <div className="bg-destructive h-2.5 w-2.5 rounded-full" aria-hidden />
          <span className="text-muted-foreground">Expenses</span>
        </div>
      </div>
      <ChartDataTable
        caption="Income and expenses by month"
        columns={['Month', 'Income', 'Expenses']}
        rows={data.map((d) => ({
          key: d.key,
          cells: [d.month, money(d.income), money(d.expenses)],
        }))}
      />
    </div>
  );
}
