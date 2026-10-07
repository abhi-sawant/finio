import {
  formatCurrency,
  formatDate,
  formatDayMonth,
  formatFileSize,
  formatFullDate,
  formatInputAmount,
  formatOrdinal,
  formatPercentChange,
  formatShortDate,
  formatTime,
  localDayKey,
  shouldCompactGroup,
  toLocalDateTimeInputValue,
  todayKey,
} from '@/utils/formatters';
import { at, gc, type GoldenCase } from '../golden';

// Every amount below goes through the real Intl.NumberFormat('en-IN') — the Kotlin side
// reimplements it by hand, so this grid is what pins rounding (half-expand on the shortest
// decimal), Indian grouping, compact suffixes (K/L/Cr/KCr/LCr), the round-up-into-the-next-unit
// re-rounding, and compact's minimum grouping digits.
const amounts = [
  0,
  0.001,
  0.004,
  0.005,
  0.01,
  0.015,
  0.05,
  0.1,
  0.1 + 0.2,
  0.5,
  0.94,
  0.95,
  0.99,
  0.995,
  1,
  1.005,
  1.015,
  1.5,
  2.675,
  8.345,
  9.95,
  9.99,
  9.995,
  10,
  99.5,
  99.95,
  99.995,
  100,
  450,
  450.5,
  450.999,
  999,
  999.4,
  999.5,
  999.94,
  999.95,
  999.99,
  1000,
  1000.5,
  1049,
  1050,
  1099.99,
  1234,
  1306.5,
  3214.64,
  9949,
  9950,
  9999,
  9999.99,
  10000,
  12345.678,
  45000,
  90010,
  99949,
  99950,
  99999,
  99999.99,
  100000,
  100000.01,
  104999,
  105000,
  123456.78,
  149999,
  150000,
  225000,
  230000,
  235000,
  950000,
  999949,
  999950,
  1000000,
  1234567.895,
  9949999,
  9950000,
  9999999,
  10000000,
  12345678,
  99949999,
  99950000,
  123456789,
  999499999,
  999500000,
  1e9,
  9.99e9,
  9.9949e9,
  9.995e9,
  1e10,
  1.23e10,
  99.95e10,
  1e11,
  1e12,
  1.2345e13,
  1e14,
  1e15,
  1e16,
  1e17,
  1e18,
  1e21,
  1.5e22,
  123456789012.5,
  2 ** 53,
  2 ** 53 + 2,
  1.2345678901234567e19,
];
const signed = [...amounts, ...amounts.filter((a) => a !== 0).map((a) => -a)];

type Opts = { precise?: boolean; forceCompact?: boolean };
const combos: [boolean, boolean, Opts][] = [
  [false, false, {}],
  [true, false, {}],
  [false, false, { precise: false }],
  [true, false, { precise: false }],
  [false, false, { forceCompact: true }],
  [true, false, { forceCompact: true }],
  [true, false, { forceCompact: false }],
  [false, false, { precise: false, forceCompact: true }],
  [false, true, {}],
  [true, true, { forceCompact: true }],
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  for (const a of signed) {
    for (const [compact, hidden, opts] of combos) {
      out.push(
        gc('formatCurrency', [a, compact, hidden, opts], formatCurrency(a, compact, hidden, opts)),
      );
    }
  }

  for (const group of [
    [],
    [0],
    [90010, 45000],
    [230000, 90010],
    [-150000, 500],
    [99999.99],
    [100000],
    [-100000],
  ]) {
    out.push(gc('shouldCompactGroup', [group], shouldCompactGroup(group)));
  }

  const isoDates = [
    '2026-10-05T12:00:00',
    '2026-10-05T00:00:00',
    '2026-10-05',
    '2026-09-30T18:30:00.000Z',
    '2026-09-30T18:29:59.999Z',
    '2026-01-01T00:00:00.000Z',
    '2025-12-31T18:30:00.000Z',
    '2024-02-29T23:59:00',
    '2026-07-04T12:05:00+05:30',
    '2026-07-04T00:05:00-04:00',
    '2026-03-08T13:45:30.123Z',
    at(2026, 12, 25, 21, 7).toISOString(),
  ];
  for (const d of isoDates) {
    out.push(gc('formatFullDate', [d], formatFullDate(d)));
    out.push(gc('formatShortDate', [d], formatShortDate(d)));
    out.push(gc('formatDayMonth', [d], formatDayMonth(d)));
    out.push(gc('formatTime', [d], formatTime(d)));
    out.push(gc('localDayKey', [d], localDayKey(d)));
    out.push(gc('toLocalDateTimeInputValue', [d], toLocalDateTimeInputValue(d)));
  }
  // formatDate reads the clock, so only dates long gone (never "Today"/"Yesterday") are pinned
  // here; the Kotlin test passes a far-future `now`. Today/Yesterday are covered by JUnit.
  for (const d of [
    '2025-10-05T12:00:00',
    '2024-02-29',
    '2026-01-01T00:00:00.000Z',
    '2025-12-31T18:30:00.000Z',
    '2026-03-08T13:45:30.123Z',
    '2026-07-04T00:05:00-04:00',
  ]) {
    out.push(gc('formatDate', [d], formatDate(d)));
  }
  for (const d of [
    at(2026, 10, 1, 0, 10),
    at(2026, 9, 30, 23, 59, 59, 999),
    new Date('2026-09-30T18:30:00.000Z'),
  ]) {
    out.push(gc('todayKey', [d], todayKey(d)));
    out.push(gc('formatShortDate', [d], formatShortDate(d), 'date'));
    out.push(gc('formatDayMonth', [d], formatDayMonth(d), 'date'));
    out.push(gc('toLocalDateTimeInputValue', [d], toLocalDateTimeInputValue(d), 'date'));
  }

  for (const raw of [
    '',
    '0',
    '5',
    '12',
    '999',
    '1000',
    '122999',
    '122999.5',
    '122999.',
    '.5',
    '.',
    '00012',
    '1234567890',
    '12345678901234567890',
    '1.2.3',
    '-12345',
    '-0',
    '+77',
    '  42',
    'abc',
    '12abc',
    '1e5',
    '123456.789012',
    '٣',
    '0.00',
  ]) {
    out.push(gc('formatInputAmount', [raw], formatInputAmount(raw)));
  }

  for (const n of [
    -21, -11, -1, 0, 1, 2, 3, 4, 10, 11, 12, 13, 14, 21, 22, 23, 24, 28, 31, 100, 101, 102, 103,
    111, 112, 113, 121, 1001, 1011,
  ]) {
    out.push(gc('formatOrdinal', [n], formatOrdinal(n)));
  }

  for (const b of [
    0, 1, 512, 1023, 1024, 1075, 2400, 1048575, 1048576, 1572864, 1073741824, 5e12, 1023.5, -10,
  ]) {
    out.push(gc('formatFileSize', [b], formatFileSize(b)));
  }

  for (const r of [
    0,
    0.004,
    0.005,
    -0.004,
    -0.005,
    -0.006,
    0.125,
    -0.125,
    0.5,
    1,
    1.005,
    2.5,
    -1,
    -0.999,
    0.1 + 0.2,
    12.3456,
  ]) {
    out.push(gc('formatPercentChange', [r], formatPercentChange(r)));
  }
  return out;
}
