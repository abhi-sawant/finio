import {
  addMonths,
  differenceInCalendarDays,
  differenceInMonths,
  format,
  isAfter,
  parseISO,
} from 'date-fns';
import { roundMoney } from '@/store/balance';
import type { Account, DepositCompounding, DepositTerms } from '@/types';

/**
 * Fixed and recurring deposit math — pure, so it is testable without a store or a browser.
 * Only a deposit's terms are stored; its maturity amount and current value are always derived
 * here, the same way `loan.ts` derives an EMI.
 *
 * Conventions follow Indian bank practice:
 * - An FD compounds at its chosen frequency over the term counted in actual days / 365.
 * - An RD compounds quarterly, and each installment earns interest only from the month it was
 *   paid in: `Σ P(1 + r/4)^(monthsHeld / 3)`. Installments not yet paid earn nothing.
 */

export const DEPOSIT_COMPOUNDING_OPTIONS: { value: DepositCompounding; label: string }[] = [
  { value: 'quarterly', label: 'Quarterly' },
  { value: 'monthly', label: 'Monthly' },
  { value: 'half-yearly', label: 'Half-yearly' },
  { value: 'yearly', label: 'Yearly' },
  { value: 'simple', label: 'Simple interest' },
];

const PERIODS_PER_YEAR: Record<Exclude<DepositCompounding, 'simple'>, number> = {
  monthly: 12,
  quarterly: 4,
  'half-yearly': 2,
  yearly: 1,
};

export function isDepositAccount(account: Pick<Account, 'type'>): boolean {
  return account.type === 'fd' || account.type === 'rd';
}

function parseDate(value: string | undefined): Date | null {
  if (!value) return null;
  const date = parseISO(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** `from` → `to` in months, fractional within the last partial month. Exact on whole months. */
function fractionalMonths(from: Date, to: Date): number {
  if (!isAfter(to, from)) return 0;
  const whole = differenceInMonths(to, from);
  const anchor = addMonths(from, whole);
  const next = addMonths(from, whole + 1);
  return whole + (to.getTime() - anchor.getTime()) / (next.getTime() - anchor.getTime());
}

/** FD growth factor over `years` at `ratePercent`. */
function fdGrowth(ratePercent: number, years: number, compounding: DepositCompounding): number {
  const r = ratePercent / 100;
  if (years <= 0) return 1;
  if (compounding === 'simple') return 1 + r * years;
  const m = PERIODS_PER_YEAR[compounding];
  return Math.pow(1 + r / m, m * years);
}

/** The date of RD installment `index` (0-based), anchored to the start date — no month-end drift. */
export function rdInstallmentDate(terms: DepositTerms, index: number): Date | null {
  const start = parseDate(terms.startDate);
  return start ? addMonths(start, index) : null;
}

/** When the deposit matures — an FD's stated date, or `tenureMonths` after an RD's first installment. */
export function depositMaturityDate(account: Pick<Account, 'type' | 'deposit'>): Date | null {
  const terms = account.deposit;
  if (!terms) return null;
  if (account.type === 'rd') return rdInstallmentDate(terms, terms.tenureMonths ?? 0);
  return parseDate(terms.maturityDate);
}

/** How many RD installments fall on or before `date`, capped at the tenure. */
export function rdInstallmentsOnOrBefore(terms: DepositTerms, date: Date): number {
  const start = parseDate(terms.startDate);
  const tenure = terms.tenureMonths ?? 0;
  if (!start || isAfter(start, date)) return 0;
  return Math.min(tenure, differenceInMonths(date, start) + 1);
}

/** Total money put in over the deposit's life: the FD principal, or installment × tenure. */
export function depositInvested(account: Pick<Account, 'type' | 'deposit'>): number {
  const terms = account.deposit;
  if (!terms) return 0;
  if (account.type === 'rd') return roundMoney(terms.amount * (terms.tenureMonths ?? 0));
  return terms.amount;
}

/**
 * The deposit's accrued value on `date`, capped at maturity. For an FD that is the principal
 * grown to `date`; for an RD, only installments already paid count, each grown by how long it
 * has been held.
 */
export function depositValueAt(account: Pick<Account, 'type' | 'deposit'>, date: Date): number {
  const terms = account.deposit;
  if (!terms) return 0;
  const start = parseDate(terms.startDate);
  const maturity = depositMaturityDate(account);
  if (!start || !maturity) return 0;
  const at = isAfter(date, maturity) ? maturity : date;
  if (isAfter(start, at)) return 0;

  if (account.type === 'rd') {
    const paid = rdInstallmentsOnOrBefore(terms, at);
    const quarterly = terms.interestRate / 100 / 4;
    let value = 0;
    for (let i = 0; i < paid; i += 1) {
      const paidOn = addMonths(start, i);
      value += terms.amount * Math.pow(1 + quarterly, fractionalMonths(paidOn, at) / 3);
    }
    return roundMoney(value);
  }

  const years = differenceInCalendarDays(at, start) / 365;
  const growth = fdGrowth(terms.interestRate, years, terms.compounding ?? 'quarterly');
  return roundMoney(terms.amount * growth);
}

/** What the deposit pays out at maturity. */
export function depositMaturityAmount(account: Pick<Account, 'type' | 'deposit'>): number {
  const maturity = depositMaturityDate(account);
  return maturity ? depositValueAt(account, maturity) : 0;
}

/** Today's value of a deposit — what the account cards show instead of the book balance. */
export function depositCurrentValue(
  account: Pick<Account, 'type' | 'deposit'>,
  now: Date = new Date(),
): number {
  return depositValueAt(account, now);
}

/**
 * The figure to show for an account: an open deposit's value today, otherwise the book balance.
 * Once a deposit has paid out its (zero) balance is the truth.
 */
export function accountDisplayValue(account: Account, now: Date = new Date()): number {
  return isDepositAccount(account) && account.deposit && !account.deposit.maturedAt
    ? depositCurrentValue(account, now)
    : account.balance;
}

/** Open deposits whose maturity date has arrived, whose payout account still exists, and that haven't been paid out. */
export function planMaturities(accounts: Account[], now: Date): Account[] {
  const ids = new Set(accounts.map((a) => a.id));
  return accounts.filter((account) => {
    if (!isDepositAccount(account) || !account.deposit || account.deposit.maturedAt) return false;
    if (!ids.has(account.deposit.linkedAccountId)) return false;
    const maturity = depositMaturityDate(account);
    return maturity !== null && !isAfter(maturity, now);
  });
}

/**
 * The names of open deposits that pay out into `accountId`. A non-empty result means the
 * account can't be deleted — the deposit would have nowhere to mature into.
 */
export function accountDeleteBlockers(accounts: Account[], accountId: string): string[] {
  return accounts
    .filter(
      (a) =>
        a.id !== accountId &&
        isDepositAccount(a) &&
        a.deposit?.linkedAccountId === accountId &&
        !a.deposit.maturedAt,
    )
    .map((a) => a.name);
}

/** One-line caption for a deposit row, e.g. "FD · 6.65% · matures 1 Sep 2029". */
export function depositCaption(account: Pick<Account, 'type' | 'deposit'>): string {
  const terms = account.deposit;
  const kind = account.type === 'rd' ? 'RD' : 'FD';
  if (!terms) return kind;
  if (terms.maturedAt) return `${kind} · ${terms.interestRate}% · matured`;
  const maturity = depositMaturityDate(account);
  return `${kind} · ${terms.interestRate}%${maturity ? ` · matures ${format(maturity, 'd MMM yyyy')}` : ''}`;
}
