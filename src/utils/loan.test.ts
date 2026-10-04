import { describe, expect, it } from 'vitest';
import {
  buildAmortizationSchedule,
  calculateEmi,
  groupScheduleByYear,
  loanStatus,
  maxPrepayment,
  simulatePrepaymentImpact,
  type LoanScheduleInput,
} from './loan';

describe('calculateEmi', () => {
  it('matches the standard EMI formula for a textbook example', () => {
    // ₹1,00,000 @ 12% p.a. for 12 months — the commonly-cited reference value.
    expect(calculateEmi(100000, 12, 12)).toBeCloseTo(8884.88, 1);
  });

  it('falls back to a flat split at 0% interest', () => {
    expect(calculateEmi(12000, 0, 12)).toBe(1000);
  });

  it('is 0 for a non-positive principal or tenure', () => {
    expect(calculateEmi(0, 10, 12)).toBe(0);
    expect(calculateEmi(10000, 10, 0)).toBe(0);
  });
});

describe('buildAmortizationSchedule', () => {
  const loan: LoanScheduleInput = {
    principal: 100000,
    interestRate: 12,
    tenureMonths: 12,
    startDate: '2026-01-05T00:00:00.000Z',
  };

  it('pays off exactly at the original tenure with no prepayments', () => {
    const schedule = buildAmortizationSchedule(loan);
    expect(schedule).toHaveLength(12);
    expect(schedule[schedule.length - 1].closingBalance).toBe(0);
  });

  it('the sum of every principal portion equals the original principal', () => {
    const schedule = buildAmortizationSchedule(loan);
    const totalPrincipal = schedule.reduce((sum, row) => sum + row.principal, 0);
    expect(Math.round(totalPrincipal)).toBe(loan.principal);
  });

  it('each row is internally consistent: opening - principal (- prepayment) = closing', () => {
    const schedule = buildAmortizationSchedule(loan);
    for (const row of schedule) {
      expect(row.closingBalance).toBeCloseTo(
        row.openingBalance - row.principal - row.prepayment,
        2,
      );
      expect(row.emi).toBeCloseTo(row.principal + row.interest, 2);
    }
  });

  it('a dated prepayment shortens the schedule and is applied on the right installment', () => {
    const withPrepayment = buildAmortizationSchedule({
      ...loan,
      // Third installment is 2026-03-05 — a prepayment on that date should land on month 3.
      prepayments: [{ amount: 20000, date: '2026-03-05T00:00:00.000Z' }],
    });
    expect(withPrepayment.length).toBeLessThan(12);
    expect(withPrepayment[2].prepayment).toBe(20000);
    expect(withPrepayment[withPrepayment.length - 1].closingBalance).toBe(0);
  });

  it('never applies more prepayment than the remaining balance', () => {
    const schedule = buildAmortizationSchedule({
      ...loan,
      prepayments: [{ amount: 10_000_000, date: '2026-02-05T00:00:00.000Z' }],
    });
    expect(schedule.every((row) => row.closingBalance >= 0)).toBe(true);
  });

  it('ignores a zero or negative prepayment', () => {
    const schedule = buildAmortizationSchedule({
      ...loan,
      prepayments: [{ amount: 0, date: '2026-02-05T00:00:00.000Z' }],
    });
    expect(schedule).toHaveLength(12);
  });
});

describe('loanStatus', () => {
  const loan: LoanScheduleInput = {
    principal: 100000,
    interestRate: 12,
    tenureMonths: 12,
    startDate: '2026-01-05T00:00:00.000Z',
  };

  it('reports nothing paid before the first installment is due', () => {
    const status = loanStatus(loan, new Date('2026-01-01T00:00:00.000Z'));
    expect(status.paidInstallments).toBe(0);
    expect(status.outstandingBalance).toBe(loan.principal);
    expect(status.isPaidOff).toBe(false);
  });

  it('tracks paid installments and outstanding balance partway through', () => {
    const status = loanStatus(loan, new Date('2026-04-06T00:00:00.000Z'));
    expect(status.paidInstallments).toBe(4);
    expect(status.outstandingBalance).toBeLessThan(loan.principal);
    expect(status.outstandingBalance).toBeGreaterThan(0);
  });

  it('is paid off once every installment is behind `now`', () => {
    const status = loanStatus(loan, new Date('2027-06-01T00:00:00.000Z'));
    expect(status.isPaidOff).toBe(true);
    expect(status.outstandingBalance).toBe(0);
    expect(status.nextDueDate).toBeNull();
  });
});

describe('prepayments in the balance', () => {
  const loan: LoanScheduleInput = {
    principal: 100000,
    interestRate: 0,
    tenureMonths: 10,
    startDate: '2026-01-05T00:00:00.000Z',
  };

  it('subtracts a prepayment made this month even though its installment is not due yet', () => {
    // Four installments are behind `now` (Jan–Apr); the prepayment sits on the fifth.
    const now = new Date('2026-04-20T00:00:00.000Z');
    const withPrepay = {
      ...loan,
      prepayments: [{ amount: 20000, date: '2026-04-19T00:00:00.000Z' }],
    };
    expect(loanStatus(withPrepay, now).outstandingBalance).toBe(
      loanStatus(loan, now).outstandingBalance - 20000,
    );
  });

  it('offers no more than what is still owed, and never goes negative', () => {
    const now = new Date('2026-01-01T00:00:00.000Z');
    expect(maxPrepayment(loan, now)).toBe(100000);
    const huge = {
      ...loan,
      prepayments: [{ amount: 9_999_999, date: '2025-12-31T00:00:00.000Z' }],
    };
    expect(maxPrepayment(huge, now)).toBe(0);
  });
});

describe('simulatePrepaymentImpact', () => {
  const loan: LoanScheduleInput = {
    principal: 100000,
    interestRate: 12,
    tenureMonths: 12,
    startDate: '2026-01-05T00:00:00.000Z',
  };

  it('a meaningful lump sum saves months and interest', () => {
    const impact = simulatePrepaymentImpact(loan, {
      amount: 30000,
      date: '2026-02-05T00:00:00.000Z',
    });
    expect(impact.monthsSaved).toBeGreaterThan(0);
    expect(impact.interestSaved).toBeGreaterThan(0);
    expect(impact.newPayoffDate).not.toBeNull();
  });

  it('a zero prepayment changes nothing', () => {
    const impact = simulatePrepaymentImpact(loan, { amount: 0, date: '2026-02-05T00:00:00.000Z' });
    expect(impact.monthsSaved).toBe(0);
    expect(impact.interestSaved).toBe(0);
  });
});

describe('groupScheduleByYear', () => {
  // Mid-month dates keep every installment inside its calendar year in any time zone.
  const loan: LoanScheduleInput = {
    principal: 100000,
    interestRate: 12,
    tenureMonths: 24,
    startDate: '2026-10-15T00:00:00.000Z',
  };

  it('splits installments by calendar year of the due date, in order', () => {
    const groups = groupScheduleByYear(buildAmortizationSchedule(loan));
    expect(groups.map((g) => g.year)).toEqual([2026, 2027, 2028]);
    expect(groups.map((g) => g.rows.length)).toEqual([3, 12, 9]);
    expect(groups.flatMap((g) => g.rows.map((r) => r.month))).toEqual(
      Array.from({ length: 24 }, (_, i) => i + 1),
    );
  });

  it('totals add up to the whole schedule', () => {
    const schedule = buildAmortizationSchedule({
      ...loan,
      prepayments: [{ amount: 20000, date: '2027-03-20T00:00:00.000Z' }],
    });
    const groups = groupScheduleByYear(schedule);
    const sum = (f: (g: (typeof groups)[number]) => number) => groups.reduce((s, g) => s + f(g), 0);
    expect(sum((g) => g.totalPrincipal)).toBeCloseTo(loan.principal, 0);
    expect(sum((g) => g.totalInterest)).toBeCloseTo(
      schedule.reduce((s, r) => s + r.interest, 0),
      2,
    );
    const y2027 = groups.find((g) => g.year === 2027)!;
    expect(y2027.totalPaid).toBeCloseTo(
      y2027.rows.reduce((s, r) => s + r.emi + r.prepayment, 0),
      2,
    );
    expect(y2027.rows.some((r) => r.prepayment === 20000)).toBe(true);
  });

  it('is empty for an empty schedule', () => {
    expect(groupScheduleByYear([])).toEqual([]);
  });
});
