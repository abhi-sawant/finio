import {
  addPeriods,
  daysElapsedInPeriod,
  daysInPeriod,
  isWithinPeriod,
  monthPeriodStart,
  normalizeMonthStartDay,
  periodLabel,
  periodRange,
  periodShortLabel,
  periodStart,
  shiftPeriod,
  yearPeriodStart,
  PERIOD_TYPES,
  PERIOD_LABELS,
  type PeriodType,
} from '@/utils/period';
import { at, gc, type GoldenCase } from '../golden';

// Dates chosen to sit on every boundary the period math cares about: month ends (28/29/30/31),
// leap day, year ends, a monthStartDay's eve and day, Sundays/Mondays, the last ms of a day, and
// instants whose UTC date differs from the IST local date.
const dates: Date[] = [
  at(2026, 7, 27, 12),
  at(2026, 7, 25, 0),
  at(2026, 7, 24, 23, 59, 59, 999),
  at(2026, 1, 3, 12),
  at(2026, 1, 1, 0),
  at(2025, 12, 31, 23, 59, 59, 999),
  at(2024, 2, 29, 9),
  at(2025, 2, 28, 18),
  at(2026, 3, 31, 12),
  at(2026, 1, 31, 12),
  at(2026, 1, 24, 12),
  at(2026, 1, 25, 0),
  at(2026, 8, 2, 23), // Sunday
  at(2026, 8, 3, 0), // Monday
  at(2026, 10, 5, 3), // UTC still 4 Oct
  at(2026, 12, 28, 12),
  at(2027, 1, 14, 12),
  at(2026, 6, 15, 17, 30),
];
const startDays = [1, 2, 15, 25, 28];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('PERIOD_TYPES', [], PERIOD_TYPES));
  out.push(gc('PERIOD_LABELS', [], PERIOD_LABELS));

  for (const v of [
    undefined,
    null,
    Number.NaN,
    '25',
    0,
    -3,
    1,
    25,
    28,
    29,
    31,
    25.7,
    1.9,
    27.999,
    1e9,
    true,
    {},
  ]) {
    out.push(gc('normalizeMonthStartDay', [v], normalizeMonthStartDay(v)));
  }

  for (const d of dates) {
    for (const day of startDays) {
      out.push(gc('monthPeriodStart', [d, day], monthPeriodStart(d, day)));
      out.push(gc('yearPeriodStart', [d, day], yearPeriodStart(d, day)));
    }
    for (const type of PERIOD_TYPES) {
      out.push(gc('periodStart', [type, d, 25], periodStart(type, d, 25)));
      for (const day of [1, 25]) {
        const r = periodRange(type, d, day);
        out.push(gc('periodRange', [type, d, day], r));
        out.push(gc('daysInPeriod', [r], daysInPeriod(r)));
        out.push(gc('periodLabel', [r, day], periodLabel(r, day)));
        out.push(gc('periodShortLabel', [r], periodShortLabel(r)));
      }
    }
  }

  for (const type of PERIOD_TYPES as PeriodType[]) {
    for (const delta of [-25, -13, -12, -1, 0, 1, 2, 11, 12, 53]) {
      const start = periodStart(type, at(2026, 1, 31, 0), 28);
      out.push(gc('addPeriods', [type, start, delta], addPeriods(type, start, delta)));
      for (const day of [1, 28]) {
        const r = periodRange(type, at(2026, 1, 31, 12), day);
        out.push(gc('shiftPeriod', [r, delta], shiftPeriod(r, delta)));
      }
    }
  }

  const ranges = [
    periodRange('monthly', at(2026, 7, 10)),
    periodRange('monthly', at(2026, 7, 10), 25),
    periodRange('weekly', at(2026, 7, 30)),
    periodRange('yearly', at(2026, 7, 30), 15),
    periodRange('monthly', at(2026, 2, 10)),
  ];
  const probes = [
    at(2026, 6, 10),
    at(2026, 6, 24, 23, 59, 59, 999),
    at(2026, 6, 25, 0),
    at(2026, 7, 1, 0),
    at(2026, 7, 1, 23, 59, 59, 999),
    at(2026, 7, 10, 12),
    at(2026, 7, 24, 23),
    at(2026, 7, 25, 0),
    at(2026, 7, 31, 23, 59, 59, 999),
    at(2026, 8, 1, 0),
    at(2026, 9, 10),
    at(2027, 3, 1),
  ];
  for (const r of ranges) {
    for (const p of probes) {
      out.push(gc('isWithinPeriod', [p, r], isWithinPeriod(p, r)));
      out.push(gc('daysElapsedInPeriod', [r, p], daysElapsedInPeriod(r, p)));
    }
    // A label with a start day that disagrees with how the range was built.
    out.push(gc('periodLabel', [r, 25], periodLabel(r, 25)));
    out.push(gc('periodLabel', [r, 99], periodLabel(r, 99)));
  }
  return out;
}
