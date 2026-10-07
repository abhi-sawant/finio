import { describe, expect, it } from 'vitest';
import type { Account, DepositTerms } from '@/types';
import {
  accountDeleteBlockers,
  depositCurrentValue,
  depositInvested,
  depositMaturityAmount,
  depositMaturityDate,
  planMaturities,
  rdInstallmentsOnOrBefore,
} from './deposit';

// Local-midnight dates, the same shape the forms store (`new Date('…T00:00:00').toISOString()`).
const iso = (day: string) => new Date(`${day}T00:00:00`).toISOString();

function deposit(type: 'fd' | 'rd', terms: Partial<DepositTerms>, extra: Partial<Account> = {}) {
  return {
    id: `${type}-1`,
    name: type.toUpperCase(),
    type,
    color: '#000',
    icon: 'vault',
    balance: 0,
    openingBalance: 0,
    createdAt: iso('2026-01-01'),
    deposit: {
      amount: 50000,
      interestRate: 6.65,
      startDate: iso('2026-10-01'),
      linkedAccountId: 'bank',
      ...terms,
    },
    ...extra,
  } satisfies Account;
}

const fd = (terms: Partial<DepositTerms> = {}, extra: Partial<Account> = {}) =>
  deposit('fd', { maturityDate: iso('2029-09-01'), ...terms }, extra);
const rd = (terms: Partial<DepositTerms> = {}, extra: Partial<Account> = {}) =>
  deposit('rd', { tenureMonths: 33, ...terms }, extra);

describe('fixed deposit', () => {
  it('compounds quarterly over actual days by default', () => {
    // ₹50,000 @ 6.65% from 1 Oct 2026 to 1 Sep 2029 — 1,066 days, quarterly compounding.
    expect(depositMaturityAmount(fd())).toBeCloseTo(60621.08, 1);
  });

  it('supports every common compounding frequency', () => {
    const years = { maturityDate: iso('2027-10-01') }; // exactly 365 days
    expect(depositMaturityAmount(fd({ ...years, compounding: 'monthly' }))).toBeCloseTo(
      53428.24,
      1,
    );
    expect(depositMaturityAmount(fd({ ...years, compounding: 'quarterly' }))).toBeCloseTo(
      53408.84,
      1,
    );
    expect(depositMaturityAmount(fd({ ...years, compounding: 'half-yearly' }))).toBeCloseTo(
      53380.28,
      1,
    );
    expect(depositMaturityAmount(fd({ ...years, compounding: 'yearly' }))).toBeCloseTo(53325, 1);
    expect(depositMaturityAmount(fd({ ...years, compounding: 'simple' }))).toBeCloseTo(53325, 1);
  });

  it('is worth the principal on the investment date and nothing before it', () => {
    expect(depositCurrentValue(fd(), new Date(iso('2026-10-01')))).toBe(50000);
    expect(depositCurrentValue(fd(), new Date(iso('2026-09-30')))).toBe(0);
  });

  it('stops growing after maturity', () => {
    expect(depositCurrentValue(fd(), new Date(iso('2031-01-01')))).toBe(
      depositMaturityAmount(fd()),
    );
  });

  it('invested is just the principal', () => {
    expect(depositInvested(fd())).toBe(50000);
  });
});

describe('recurring deposit', () => {
  it('matches the standard Indian RD formula', () => {
    // ₹50,000/month for 33 months @ 6.65%, quarterly compounding: Σ P(1+r/4)^(k/3).
    expect(depositMaturityAmount(rd())).toBeCloseTo(1814079.37, 1);
  });

  it('matures tenureMonths after the first installment', () => {
    expect(depositMaturityDate(rd())?.toISOString()).toBe(iso('2029-07-01'));
  });

  it('invested is installment × tenure', () => {
    expect(depositInvested(rd())).toBe(1650000);
  });

  it('only counts installments already paid', () => {
    // Three installments (1 Oct, 1 Nov, 1 Dec), valued on 1 Dec: held 2, 1 and 0 months.
    const r = 0.0665 / 4;
    const expected = 50000 * (Math.pow(1 + r, 2 / 3) + Math.pow(1 + r, 1 / 3) + 1);
    expect(depositCurrentValue(rd(), new Date(iso('2026-12-01')))).toBeCloseTo(expected, 1);
  });

  it('counts installments on or before a date, capped at the tenure', () => {
    const terms = rd().deposit;
    expect(rdInstallmentsOnOrBefore(terms, new Date(iso('2026-09-30')))).toBe(0);
    expect(rdInstallmentsOnOrBefore(terms, new Date(iso('2026-10-01')))).toBe(1);
    expect(rdInstallmentsOnOrBefore(terms, new Date(iso('2027-02-15')))).toBe(5);
    expect(rdInstallmentsOnOrBefore(terms, new Date(iso('2035-01-01')))).toBe(33);
  });
});

describe('planMaturities', () => {
  const bank: Account = { ...fd(), id: 'bank', type: 'savings', deposit: undefined };

  it('picks deposits whose maturity date has arrived', () => {
    const due = planMaturities([bank, fd()], new Date(iso('2029-09-01')));
    expect(due.map((a) => a.id)).toEqual(['fd-1']);
  });

  it('skips deposits not yet mature, already paid out, or without a payout account', () => {
    const now = new Date(iso('2029-09-01'));
    expect(planMaturities([bank, fd()], new Date(iso('2029-08-31')))).toEqual([]);
    expect(planMaturities([bank, fd({ maturedAt: iso('2029-09-01') })], now)).toEqual([]);
    expect(planMaturities([fd()], now)).toEqual([]);
  });
});

describe('accountDeleteBlockers', () => {
  it('names open deposits that pay out into the account', () => {
    const accounts = [
      fd(),
      rd({ linkedAccountId: 'other' }),
      fd({ maturedAt: iso('2029-09-01') }, { id: 'fd-2' }),
    ];
    expect(accountDeleteBlockers(accounts, 'bank')).toEqual(['FD']);
    expect(accountDeleteBlockers(accounts, 'nobody')).toEqual([]);
  });
});
