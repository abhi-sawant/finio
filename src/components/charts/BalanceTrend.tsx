import { useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  ResponsiveContainer,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { useFinanceStore } from '@/store/useFinanceStore';
import { subDays, format, differenceInDays } from 'date-fns';
import { monthPeriodStart, addPeriods, normalizeMonthStartDay } from '@/utils/period';
import { formatCurrency, localDayKey } from '@/utils/formatters';
import { getNetWorth } from '@/utils/calculations';
import { sampleForTable } from '@/utils/chartTable';
import { ChartDataTable } from './ChartDataTable';
import { EmptyChart } from './EmptyChart';

interface Props {
  from: Date;
  to: Date;
}

export function BalanceTrend({ from, to }: Props) {
  const transactions = useFinanceStore((s) => s.transactions);
  const accounts = useFinanceStore((s) => s.accounts);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));

  const data = useMemo(() => {
    const today = new Date();
    // Matches the net worth shown elsewhere: closed accounts are excluded.
    const currentBalance = getNetWorth(accounts);

    // Build daily delta map from ALL transactions for accurate balance reconstruction.
    const dayDelta = new Map<string, number>();
    for (const t of transactions) {
      if (t.type === 'transfer') continue;
      const key = localDayKey(t.date);
      const delta = t.type === 'income' ? t.amount : -t.amount;
      dayDelta.set(key, (dayDelta.get(key) ?? 0) + delta);
    }

    const numDays = differenceInDays(to, from);

    if (numDays > 90) {
      // Monthly granularity: walk back month by month
      const daysFromToday = differenceInDays(today, from);
      let balance = currentBalance;
      const allDaily: { dateKey: string; balance: number }[] = [];

      for (let i = 0; i <= daysFromToday; i++) {
        const day = subDays(today, i);
        const dayKey = format(day, 'yyyy-MM-dd');
        allDaily.unshift({ dateKey: dayKey, balance });
        balance -= dayDelta.get(dayKey) ?? 0;
      }

      // Sample the first day of each financial month within [from, to]; the first point is
      // clamped to `from` so a mid-month range doesn't read from a day outside it.
      const monthly: { dateKey: string; date: string; balance: number }[] = [];
      const fromKey = format(from, 'yyyy-MM-dd');
      let cursor = monthPeriodStart(from, monthStartDay);
      while (cursor <= to) {
        const cursorKey = format(cursor, 'yyyy-MM-dd');
        const key = cursorKey < fromKey ? fromKey : cursorKey;
        const point = allDaily.find((p) => p.dateKey >= key);
        if (point) {
          monthly.push({
            dateKey: point.dateKey,
            date: format(cursor, 'MMM yy'),
            balance: point.balance,
          });
        }
        cursor = addPeriods('monthly', cursor, 1);
      }
      // Always include the 'to' endpoint (replacing the last bucket if it is the same month)
      const lastKey = format(to, 'yyyy-MM-dd');
      const lastPoint = allDaily.findLast
        ? allDaily.findLast((p) => p.dateKey <= lastKey)
        : [...allDaily].reverse().find((p) => p.dateKey <= lastKey);
      if (lastPoint) {
        const endLabel = format(to, 'MMM yy');
        const entry = { dateKey: lastPoint.dateKey, date: endLabel, balance: lastPoint.balance };
        const prev = monthly[monthly.length - 1];
        if (prev && prev.date === endLabel) monthly[monthly.length - 1] = entry;
        else monthly.push(entry);
      }
      return monthly;
    }

    // Daily granularity: reconstruct from today back to 'from'
    const daysFromToday = differenceInDays(today, from);
    let balance = currentBalance;
    const points: { dateKey: string; date: string; balance: number }[] = [];

    for (let i = 0; i <= daysFromToday; i++) {
      const day = subDays(today, i);
      const dayKey = format(day, 'yyyy-MM-dd');
      points.unshift({ dateKey: dayKey, date: format(day, 'd MMM'), balance });
      balance -= dayDelta.get(dayKey) ?? 0;
    }

    const fromKey = format(from, 'yyyy-MM-dd');
    const toKey = format(to, 'yyyy-MM-dd');
    return points
      .filter((p) => p.dateKey >= fromKey && p.dateKey <= toKey)
      .map(({ dateKey, date, balance }) => ({ dateKey, date, balance }));
  }, [transactions, accounts, from, to, monthStartDay]);

  const numDays = differenceInDays(to, from);
  const xAxisInterval = numDays <= 31 ? 4 : numDays <= 60 ? 9 : 'preserveStartEnd';

  const hasData = accounts.length > 0;
  if (!hasData) return null;

  const first = data[0];
  const last = data[data.length - 1];
  const money = (value: number) => formatCurrency(value, true, hideAmounts);
  const labelByKey = new Map(data.map((p) => [p.dateKey, p.date]));
  const table = sampleForTable(data);

  return (
    <div className="card-elevated rounded-md p-4">
      <h3 className="mb-3 text-sm font-semibold">Balance Trend</h3>
      <div
        className="h-44 lg:h-64"
        role="img"
        aria-label={
          first && last
            ? `Balance from ${first.date} to ${last.date}, ${money(first.balance)} to ${money(last.balance)}.`
            : 'Balance over time.'
        }
      >
        {data.length < 2 ? (
          <EmptyChart message="Not enough history in this range to draw a trend." />
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" opacity={0.12} />
              <XAxis
                dataKey="dateKey"
                tickFormatter={(k) => labelByKey.get(String(k)) ?? String(k)}
                fontSize={10}
                tickLine={false}
                axisLine={false}
                interval={xAxisInterval}
              />
              <YAxis
                fontSize={10}
                tickLine={false}
                axisLine={false}
                width={56}
                tickMargin={4}
                tickFormatter={money}
              />
              <Tooltip
                cursor={{ stroke: 'var(--muted-foreground)', strokeOpacity: 0.4, strokeWidth: 1 }}
                contentStyle={{
                  background: 'var(--card)',
                  border: '1px solid var(--border)',
                  borderRadius: 12,
                  fontSize: 12,
                }}
                formatter={(v) => formatCurrency(Number(v) || 0, false, hideAmounts)}
                labelFormatter={(k) => labelByKey.get(String(k)) ?? String(k)}
                labelStyle={{ color: 'var(--muted-foreground)' }}
              />
              <Line
                type="monotone"
                dataKey="balance"
                stroke="var(--primary)"
                strokeWidth={3}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
      <ChartDataTable
        caption="Balance over time"
        columns={['Date', 'Balance']}
        note={
          table.sampled ? `sampled to ${table.rows.length} of ${data.length} points` : undefined
        }
        rows={table.rows.map((point) => ({
          key: point.dateKey,
          cells: [point.date, money(point.balance)],
        }))}
      />
    </div>
  );
}
