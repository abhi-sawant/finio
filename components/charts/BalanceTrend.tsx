import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, useWindowDimensions } from 'react-native';
import Svg, {
  Path,
  Defs,
  LinearGradient,
  Stop,
  Line,
  Text as SvgText,
  Rect,
  G,
  Circle,
} from 'react-native-svg';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { formatCurrency } from '@/utils/formatters';
import { useFinanceStore } from '@/store/useFinanceStore';
import { getTotalAccountBalance } from '@/utils/calculations';
import { subDays, format } from 'date-fns';
import type { Currency } from '@/types';

const Y_LABEL_W = 52;
const CHART_H = 120;
const X_LABEL_H = 16;
const SVG_HEIGHT = CHART_H + X_LABEL_H;
const DAYS = 30;
const TOOLTIP_W = 118;
const TOOLTIP_H = 40;

export const BalanceTrend = React.memo(function BalanceTrend() {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);
  const { width: screenWidth } = useWindowDimensions();
  const SVG_WIDTH = screenWidth - 64;
  const PLOT_W = SVG_WIDTH - Y_LABEL_W;

  const transactions = useFinanceStore((s) => s.transactions);
  const accounts = useFinanceStore((s) => s.accounts);
  const currency = useFinanceStore((s) => s.settings.currency) as Currency;
  const currentBalance = useMemo(() => getTotalAccountBalance(accounts), [accounts]);
  const [activeIdx, setActiveIdx] = useState<number | null>(null);

  // Pre-group transactions by date string: O(n) — avoids the O(30×n) nested loop
  const txByDay = useMemo(() => {
    const map = new Map<string, typeof transactions>();
    for (const t of transactions) {
      const day = t.date.slice(0, 10);
      const existing = map.get(day);
      if (existing) existing.push(t);
      else map.set(day, [t]);
    }
    return map;
  }, [transactions]);

  // Build 30-day balance trend — O(30) after the O(n) pre-grouping above
  const pointData = useMemo(() => {
    const points: { value: number; date: Date }[] = [];
    let balance = currentBalance;

    for (let i = 0; i < DAYS; i++) {
      const date = subDays(new Date(), i);
      const dayStr = date.toISOString().slice(0, 10);
      const dayTxs = txByDay.get(dayStr) ?? [];
      for (const t of dayTxs) {
        if (t.type === 'expense') balance += t.amount;
        else if (t.type === 'income') balance -= t.amount;
      }
      points.unshift({ value: balance, date });
    }
    return points;
  }, [txByDay, currentBalance]);

  const { linePath, areaPath, toX, toY, minVal, range } = useMemo(() => {
    const values = pointData.map((p) => p.value);
    const minVal = Math.min(...values);
    const maxVal = Math.max(...values);
    const range = maxVal - minVal || 1;

    const tX = (i: number) => Y_LABEL_W + (i / (DAYS - 1)) * PLOT_W;
    const tY = (v: number) => CHART_H - ((v - minVal) / range) * (CHART_H - 12) - 6;

    const line = pointData
      .map((p, i) => `${i === 0 ? 'M' : 'L'} ${tX(i).toFixed(2)} ${tY(p.value).toFixed(2)}`)
      .join(' ');

    const area =
      line + ` L ${tX(DAYS - 1).toFixed(2)} ${CHART_H}` + ` L ${tX(0).toFixed(2)} ${CHART_H} Z`;

    return { linePath: line, areaPath: area, toX: tX, toY: tY, minVal, range };
  }, [pointData, PLOT_W]);

  const values = pointData.map((p) => p.value);
  const diff = (values[DAYS - 1] ?? 0) - (values[0] ?? 0);
  const isPositive = diff >= 0;

  // Active tooltip
  const active = activeIdx !== null ? pointData[activeIdx] : null;
  const ax = activeIdx !== null ? toX(activeIdx) : 0;
  const ay = activeIdx !== null ? toY(active!.value) : 0;
  const tooltipOnRight = ax + TOOLTIP_W + 8 <= SVG_WIDTH;
  const tooltipX = tooltipOnRight ? ax + 8 : ax - TOOLTIP_W - 8;
  const tooltipY = Math.max(4, ay - TOOLTIP_H / 2);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Balance Trend</Text>
        <Text style={[styles.diff, { color: isPositive ? colors.income : colors.expense }]}>
          {isPositive ? '+' : ''}
          {formatCurrency(diff, currency, true)}
        </Text>
      </View>

      <Svg width={SVG_WIDTH} height={SVG_HEIGHT}>
        <Defs>
          <LinearGradient id="balGrad" x1="0" y1="0" x2="0" y2="1">
            <Stop offset="0" stopColor={colors.primary} stopOpacity={0.25} />
            <Stop offset="1" stopColor={colors.primary} stopOpacity={0} />
          </LinearGradient>
        </Defs>

        {/* Y-axis grid lines + labels */}
        {[0, 0.5, 1].map((frac, i) => {
          const val = minVal + frac * range;
          const y = toY(val);
          return (
            <G key={i}>
              <Line
                x1={Y_LABEL_W}
                y1={y}
                x2={SVG_WIDTH}
                y2={y}
                stroke={colors.border}
                strokeWidth={0.5}
                {...(i === 1 ? { strokeDasharray: '4 3' } : {})}
              />
              <SvgText
                x={Y_LABEL_W - 4}
                y={y + 4}
                textAnchor="end"
                fill={colors.textMuted}
                fontSize={9}
              >
                {formatCurrency(val, currency, true)}
              </SvgText>
            </G>
          );
        })}

        {/* Area fill */}
        <Path d={areaPath} fill="url(#balGrad)" />

        {/* Line */}
        <Path
          d={linePath}
          stroke={colors.primary}
          strokeWidth={2}
          fill="none"
          strokeLinejoin="round"
          strokeLinecap="round"
        />

        {/* Hit zones — one vertical strip per day */}
        {pointData.map((_, i) => {
          const cx = toX(i);
          const halfW = PLOT_W / DAYS / 2;
          const hitX = i === 0 ? Y_LABEL_W : cx - halfW;
          const hitW = i === DAYS - 1 ? SVG_WIDTH - hitX : halfW * 2;
          return (
            <Rect
              key={i}
              x={hitX}
              y={0}
              width={hitW}
              height={CHART_H}
              fill="transparent"
              onPress={() => setActiveIdx(activeIdx === i ? null : i)}
            />
          );
        })}

        {/* Active point + tooltip */}
        {activeIdx !== null && active != null && (
          <G>
            {/* Vertical crosshair */}
            <Line
              x1={ax}
              y1={0}
              x2={ax}
              y2={CHART_H}
              stroke={colors.primary}
              strokeWidth={1}
              strokeDasharray="3 2"
              opacity={0.5}
            />
            {/* Dot */}
            <Circle cx={ax} cy={ay} r={5} fill={colors.primary} />
            <Circle cx={ax} cy={ay} r={2.5} fill={colors.surface} />

            {/* Tooltip */}
            <Rect
              x={tooltipX}
              y={tooltipY}
              width={TOOLTIP_W}
              height={TOOLTIP_H}
              rx={8}
              fill={colors.surfaceElevated}
              stroke={colors.border}
              strokeWidth={1}
            />
            <SvgText
              x={tooltipX + TOOLTIP_W / 2}
              y={tooltipY + 14}
              textAnchor="middle"
              fill={colors.textMuted}
              fontSize={9}
            >
              {format(active!.date, 'MMM d, yyyy')}
            </SvgText>
            <SvgText
              x={tooltipX + TOOLTIP_W / 2}
              y={tooltipY + 30}
              textAnchor="middle"
              fill={colors.textPrimary}
              fontSize={12}
              fontWeight="bold"
            >
              {formatCurrency(active!.value, currency, true)}
            </SvgText>
          </G>
        )}

        {/* X-axis date labels */}
        <SvgText x={toX(0)} y={SVG_HEIGHT - 2} fill={colors.textMuted} fontSize={9}>
          30d ago
        </SvgText>
        <SvgText
          x={SVG_WIDTH}
          y={SVG_HEIGHT - 2}
          fill={colors.textMuted}
          fontSize={9}
          textAnchor="end"
        >
          Today
        </SvgText>
      </Svg>

      <Text style={styles.currentLabel}>
        Current:{' '}
        <Text style={styles.currentValue}>{formatCurrency(currentBalance, currency, true)}</Text>
      </Text>
    </View>
  );
});

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    container: {
      gap: 12,
    },
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
    },
    title: {
      fontFamily: 'Sora_700Bold',
      fontSize: 15,
      color: colors.textPrimary,
    },
    diff: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 14,
    },
    currentLabel: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 13,
      color: colors.textMuted,
      textAlign: 'center',
    },
    currentValue: {
      fontFamily: 'DMSans_700Bold',
      color: colors.textPrimary,
    },
  });
}
