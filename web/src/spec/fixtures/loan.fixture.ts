import {
  buildAmortizationSchedule,
  calculateEmi,
  groupScheduleByYear,
  loanStatus,
  maxPrepayment,
  monthlyRate,
  simulatePrepaymentImpact,
  type LoanScheduleInput,
} from '@/utils/loan';
import { at, gc, type GoldenCase } from '../golden';

const base: LoanScheduleInput = {
  principal: 100000,
  interestRate: 12,
  tenureMonths: 12,
  startDate: '2026-01-05T00:00:00.000Z',
};

const loans: [string, LoanScheduleInput][] = [
  ['textbook', base],
  ['zero-rate', { ...base, interestRate: 0, tenureMonths: 10 }],
  [
    'zero-rate-uneven',
    { principal: 100000, interestRate: 0, tenureMonths: 7, startDate: base.startDate },
  ],
  [
    'home',
    {
      principal: 4_500_000,
      interestRate: 8.65,
      tenureMonths: 240,
      startDate: '2024-03-10T00:00:00.000Z',
    },
  ],
  [
    'car-paise',
    {
      principal: 612345.67,
      interestRate: 9.25,
      tenureMonths: 60,
      startDate: '2025-08-31T18:30:00.000Z',
    },
  ],
  [
    'month-end-local',
    {
      principal: 50000,
      interestRate: 14,
      tenureMonths: 24,
      startDate: at(2026, 1, 31).toISOString(),
    },
  ],
  ['date-only', { principal: 25000, interestRate: 10.5, tenureMonths: 6, startDate: '2026-02-28' }],
  ['tiny', { principal: 1, interestRate: 36, tenureMonths: 3, startDate: base.startDate }],
  ['zero-principal', { ...base, principal: 0 }],
  ['zero-tenure', { ...base, tenureMonths: 0 }],
  ['negative-principal', { ...base, principal: -5000 }],
  ['with-prepay', { ...base, prepayments: [{ amount: 20000, date: '2026-03-01T00:00:00.000Z' }] }],
  [
    'prepay-many',
    {
      ...base,
      tenureMonths: 36,
      prepayments: [
        { amount: 10000, date: '2026-01-05T00:00:00.000Z' },
        { amount: 5000.555, date: '2026-01-20T00:00:00.000Z' },
        { amount: 0, date: '2026-02-01T00:00:00.000Z' },
        { amount: -500, date: '2026-03-01T00:00:00.000Z' },
        { amount: 2500.25, date: '2025-11-01T00:00:00.000Z' }, // before the start → month 1
        { amount: 15000, date: '2027-06-15T00:00:00.000Z' },
      ],
    },
  ],
  ['prepay-huge', { ...base, prepayments: [{ amount: 1e7, date: '2026-02-10T00:00:00.000Z' }] }],
  [
    'prepay-past-end',
    { ...base, prepayments: [{ amount: 1000, date: '2031-01-01T00:00:00.000Z' }] },
  ],
  [
    'prepay-this-month',
    {
      ...base,
      interestRate: 0,
      tenureMonths: 10,
      prepayments: [{ amount: 20000, date: '2026-04-19T00:00:00.000Z' }],
    },
  ],
];

const nows = [
  new Date('2025-12-01T00:00:00.000Z'),
  new Date('2026-01-05T00:00:00.000Z'),
  new Date('2026-01-04T23:59:59.999Z'),
  new Date('2026-04-20T00:00:00.000Z'),
  new Date('2027-06-01T00:00:00.000Z'),
  new Date('2050-01-01T00:00:00.000Z'),
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  for (const r of [0, 1, 8.5, 12, 36, -6]) out.push(gc('monthlyRate', [r], monthlyRate(r)));
  for (const [p, r, n] of [
    [100000, 12, 12],
    [12000, 0, 12],
    [0, 10, 12],
    [10000, 10, 0],
    [-1, 10, 12],
    [10000, 10, -3],
    [4_500_000, 8.65, 240],
    [1, 36, 3],
    [100000, 0, 7],
    [999999.99, 7.35, 360],
    [100000, -6, 12],
    [250000, 24, 1],
  ]) {
    out.push(gc('calculateEmi', [p, r, n], calculateEmi(p, r, n)));
  }
  for (const [name, loan] of loans) {
    const schedule = buildAmortizationSchedule(loan);
    out.push(gc('buildAmortizationSchedule', [loan], schedule, name));
    // Args carry the loan, not the (already-asserted) schedule, to keep the fixture small.
    out.push(gc('groupScheduleByYear', [loan], groupScheduleByYear(schedule), name));
    for (const now of nows) {
      out.push(gc('loanStatus', [loan, now], loanStatus(loan, now), name));
      out.push(gc('maxPrepayment', [loan, now], maxPrepayment(loan, now), name));
    }
    for (const extra of [
      { amount: 20000, date: '2026-03-01T00:00:00.000Z' },
      { amount: 0, date: '2026-03-01T00:00:00.000Z' },
      { amount: 1e9, date: '2026-01-01T00:00:00.000Z' },
      { amount: 750.5, date: '2028-12-31T18:30:00.000Z' },
    ]) {
      out.push(
        gc('simulatePrepaymentImpact', [loan, extra], simulatePrepaymentImpact(loan, extra), name),
      );
    }
  }
  out.push(gc('groupScheduleByYear', [[]], groupScheduleByYear([]), 'empty'));
  return out;
}
