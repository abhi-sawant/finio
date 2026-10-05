import { format } from 'date-fns';
import type { Account, DepositCompounding, DepositTerms } from '@/types';

/** The deposit form's raw input — strings as typed, dates as `yyyy-MM-dd`. */
export interface DepositFormValues {
  amount: string;
  interestRate: string;
  startDate: string;
  maturityDate: string;
  tenureMonths: string;
  compounding: DepositCompounding;
  linkedAccountId: string;
  deductPast: boolean;
}

export function depositFormFromAccount(
  account: Account | null | undefined,
  defaultLinkedId: string,
): DepositFormValues {
  const terms = account?.deposit;
  return {
    amount: terms?.amount.toString() ?? '0',
    interestRate: terms?.interestRate.toString() ?? '',
    startDate: terms ? format(new Date(terms.startDate), 'yyyy-MM-dd') : '',
    maturityDate: terms?.maturityDate ? format(new Date(terms.maturityDate), 'yyyy-MM-dd') : '',
    tenureMonths: terms?.tenureMonths?.toString() ?? '',
    compounding: terms?.compounding ?? 'quarterly',
    linkedAccountId: terms?.linkedAccountId ?? defaultLinkedId,
    deductPast: false,
  };
}

const toIso = (day: string) => new Date(`${day}T00:00:00`).toISOString();

/** The terms these form values describe, or null while they're incomplete or inconsistent. */
export function depositTermsFromForm(
  type: 'fd' | 'rd',
  values: DepositFormValues,
): Omit<DepositTerms, 'recurringId' | 'maturedAt'> | null {
  const amount = parseFloat(values.amount) || 0;
  const interestRate = parseFloat(values.interestRate);
  if (amount <= 0 || !Number.isFinite(interestRate) || interestRate < 0) return null;
  if (!values.startDate || !values.linkedAccountId) return null;
  const base = {
    amount,
    interestRate,
    startDate: toIso(values.startDate),
    linkedAccountId: values.linkedAccountId,
  };
  if (type === 'fd') {
    if (!values.maturityDate || values.maturityDate <= values.startDate) return null;
    return { ...base, maturityDate: toIso(values.maturityDate), compounding: values.compounding };
  }
  const tenureMonths = parseInt(values.tenureMonths, 10);
  if (!Number.isFinite(tenureMonths) || tenureMonths < 1) return null;
  return { ...base, tenureMonths };
}
