import { useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { AlertTriangle, TrendingDown, Wallet } from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import {
  formatCurrency,
  formatDayMonth,
  formatShortDate,
  shouldCompactGroup,
} from '@/utils/formatters';
import { buildCashFlowForecast } from '@/utils/forecast';
import { Button } from '@/components/ui/button';
import { ChartDataTable } from '@/components/charts/ChartDataTable';
import { sampleForTable } from '@/utils/chartTable';
import {
  AXIS_PROPS,
  GRID_PROPS,
  TOOLTIP_PROPS,
  formatAxisMoney,
} from '@/components/charts/chartTheme';

const HORIZONS = [
  { days: 30, label: '30d' },
  { days: 60, label: '60d' },
  { days: 90, label: '90d' },
] as const;

export function CashFlowForecast() {
  const accounts = useFinanceStore((s) => s.accounts);
  const transactions = useFinanceStore((s) => s.transactions);
  const recurring = useFinanceStore((s) => s.recurring);
  const categories = useFinanceStore((s) => s.categories);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const [days, setDays] = useState<number>(90);

  const forecast = useMemo(
    () => buildCashFlowForecast({ accounts, transactions, recurring, days }),
    [accounts, transactions, recurring, days],
  );

  const chartData = useMemo(
    () => forecast.points.map((p) => ({ date: formatDayMonth(p.date), balance: p.balance })),
    [forecast],
  );

  const money = (value: number) => formatCurrency(value, true, hideAmounts);
  const categoryName = (id: string) => categories.find((c) => c.id === id)?.name ?? 'Uncategorized';

  if (forecast.isEmpty) return null;

  const upcoming = forecast.scheduled.slice(0, 4);
  // The three tiles are read side by side, so they share one format: compact together once
  // any of them is large, and never with paise — a projection isn't exact to the rupee.
  const tilesCompact = shouldCompactGroup([
    forecast.startBalance,
    forecast.endBalance,
    forecast.low?.balance ?? 0,
  ]);
  const tile = (value: number) =>
    formatCurrency(value, true, hideAmounts, { forceCompact: tilesCompact, precise: false });
  const projectedTable = sampleForTable(forecast.points);

  return (
    <section className="card-elevated rounded-md p-4">
      <div className="mb-1 flex items-center justify-between gap-2">
        <h3 className="text-sm font-semibold">Cash-flow forecast</h3>
        <div className="flex gap-1">
          {HORIZONS.map((horizon) => (
            <Button
              key={horizon.days}
              size="sm"
              variant={days === horizon.days ? 'default' : 'ghost'}
              className="h-7 px-2 text-xs"
              onClick={() => setDays(horizon.days)}
            >
              {horizon.label}
            </Button>
          ))}
        </div>
      </div>
      <p className="text-muted-foreground mb-3 text-xs">
        Liquid cash projected from your recurring rules plus your last {forecast.lookbackDays} days
        of everyday spending. Credit cards are excluded until the payment leaves an account.
      </p>

      <div
        className="h-40 lg:h-56"
        role="img"
        aria-label={`Projected balance over the next ${days} days, from ${money(forecast.startBalance)} today to ${money(forecast.endBalance)}.`}
      >
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid {...GRID_PROPS} />
            <XAxis dataKey="date" {...AXIS_PROPS} interval="preserveStartEnd" minTickGap={32} />
            <YAxis
              {...AXIS_PROPS}
              width={56}
              tickMargin={4}
              tickFormatter={(v: number) => formatAxisMoney(v, hideAmounts)}
            />
            <Tooltip
              cursor={{ stroke: 'var(--muted-foreground)', strokeOpacity: 0.4, strokeWidth: 1 }}
              {...TOOLTIP_PROPS}
              formatter={(v) => [
                formatCurrency(Number(v) || 0, false, hideAmounts, { precise: false }),
                'Projected balance',
              ]}
            />
            {/* Zero is the line that matters — everything below it is an overdraft. */}
            <ReferenceLine y={0} stroke="var(--muted-foreground)" strokeDasharray="4 4" />
            <Area
              type="monotone"
              dataKey="balance"
              stroke="var(--primary)"
              strokeWidth={2.5}
              fill="var(--primary)"
              fillOpacity={0.12}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <ChartDataTable
        caption="Projected liquid balance by day"
        columns={['Date', 'Projected balance']}
        note={
          projectedTable.sampled
            ? `sampled to ${projectedTable.rows.length} of ${forecast.points.length} days`
            : undefined
        }
        rows={projectedTable.rows.map((point) => ({
          key: point.date.toISOString(),
          cells: [formatDayMonth(point.date), money(point.balance)],
        }))}
      />

      <dl className="mt-3 grid grid-cols-3 gap-2">
        <div className="bg-muted/40 rounded-sm p-2.5">
          <dt className="text-muted-foreground flex items-center gap-1 text-xs font-medium">
            <Wallet size={10} /> Today
          </dt>
          <dd className="mt-0.5 text-xs font-semibold">{tile(forecast.startBalance)}</dd>
        </div>
        <div className="bg-muted/40 rounded-sm p-2.5">
          <dt className="text-muted-foreground text-xs font-medium">In {days} days</dt>
          <dd
            className={`mt-0.5 text-xs font-semibold ${forecast.endBalance < 0 ? 'text-destructive' : ''}`}
          >
            {tile(forecast.endBalance)}
          </dd>
        </div>
        <div className="bg-muted/40 rounded-sm p-2.5">
          <dt className="text-muted-foreground flex items-center gap-1 text-xs font-medium">
            <TrendingDown size={10} /> Lowest
          </dt>
          <dd
            className={`mt-0.5 text-xs font-semibold ${forecast.low && forecast.low.balance < 0 ? 'text-destructive' : ''}`}
          >
            {forecast.low ? tile(forecast.low.balance) : '—'}
            {forecast.low && (
              <span className="text-muted-foreground block font-normal">
                {formatDayMonth(forecast.low.date)}
              </span>
            )}
          </dd>
        </div>
      </dl>

      {forecast.shortfallDate && (
        <p className="bg-destructive/10 text-destructive mt-3 flex items-start gap-2 rounded-sm p-2.5 text-xs">
          <AlertTriangle size={13} className="mt-0.5 shrink-0" />
          <span>
            At this rate your liquid balance runs out around{' '}
            <strong className="font-semibold">{formatShortDate(forecast.shortfallDate)}</strong>.
          </span>
        </p>
      )}

      {upcoming.length > 0 && (
        <div className="mt-4">
          <p className="text-muted-foreground mb-2 text-xs font-medium">
            Scheduled next — {money(forecast.totals.scheduledOut)} out,{' '}
            {money(forecast.totals.scheduledIn)} in over {days} days
          </p>
          <ul className="space-y-1.5">
            {upcoming.map((flow) => (
              <li
                key={`${flow.ruleId}-${flow.date.toISOString()}`}
                className="flex items-center gap-2 text-xs"
              >
                <span className="text-muted-foreground w-14 shrink-0">
                  {formatDayMonth(flow.date)}
                </span>
                <span className="min-w-0 flex-1 truncate">
                  {flow.note || categoryName(flow.categoryId)}
                </span>
                <span
                  className={`shrink-0 font-semibold ${flow.delta > 0 ? 'text-positive' : 'text-destructive'}`}
                >
                  {flow.delta > 0 ? '+' : '−'}
                  {money(Math.abs(flow.delta))}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {forecast.dailyEstimate > 0 && (
        <p className="text-muted-foreground mt-3 text-xs">
          Everyday spend estimated at {money(forecast.dailyEstimate)} a day
          {forecast.categoryAverages.length > 0 && (
            <>
              , led by{' '}
              {forecast.categoryAverages
                .slice(0, 3)
                .map((average) => categoryName(average.categoryId))
                .join(', ')}
            </>
          )}
          .
        </p>
      )}
    </section>
  );
}
