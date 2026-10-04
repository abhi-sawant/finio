import type { CSSProperties } from 'react';
import { formatCurrency } from '@/utils/formatters';

/**
 * Shared Recharts styling, built from the theme tokens so every chart reads the same in light
 * and dark. Recharts' own defaults (#666 ticks, #ccc grid, a white tooltip) are fixed hex
 * values that ignore the theme — they fall to ~2.5:1 on the dark indigo surface.
 */

/** Axis tick text — spread into `tick` on an XAxis/YAxis. */
export const AXIS_TICK = { fill: 'var(--muted-foreground)', fontSize: 10 } as const;

/** Common XAxis/YAxis props: token ticks, no tick marks, no axis line. */
export const AXIS_PROPS = {
  tick: AXIS_TICK,
  tickLine: false,
  axisLine: false,
} as const;

/** Props for `<CartesianGrid>`. */
export const GRID_PROPS = {
  strokeDasharray: '3 3',
  stroke: 'var(--border)',
  vertical: false,
} as const;

/** Tooltip box — the popover surface, ink text, hairline border. */
export const TOOLTIP_CONTENT_STYLE: CSSProperties = {
  background: 'var(--popover)',
  color: 'var(--popover-foreground)',
  border: '1px solid var(--border)',
  borderRadius: 12,
  fontSize: 12,
  boxShadow: 'none',
};

/** Tooltip row text stays ink — a series' own colour is too faint as text on dark. */
export const TOOLTIP_ITEM_STYLE: CSSProperties = { color: 'var(--popover-foreground)' };

/** Tooltip heading (the x value / slice name). */
export const TOOLTIP_LABEL_STYLE: CSSProperties = {
  color: 'var(--muted-foreground)',
  marginBottom: 2,
};

/** Spread into a `<Tooltip>` for the shared look. */
export const TOOLTIP_PROPS = {
  contentStyle: TOOLTIP_CONTENT_STYLE,
  itemStyle: TOOLTIP_ITEM_STYLE,
  labelStyle: TOOLTIP_LABEL_STYLE,
} as const;

const AXIS_FORMAT = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  notation: 'compact',
  maximumFractionDigits: 2,
});

/**
 * One money-axis tick format for every chart: always compact (₹90K, ₹1.35L, ₹2L), so a
 * single axis never mixes "₹1.8L" with "₹90,000", and with two decimals so ₹1.35L doesn't
 * round to a misleading ₹1.4L. Honours "hide amounts".
 */
export function formatAxisMoney(value: number, hidden = false): string {
  if (hidden) return formatCurrency(value, true, true);
  return AXIS_FORMAT.format(value);
}
