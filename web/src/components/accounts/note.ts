import type { AccountType } from '@/types';

/**
 * Every account is printed like a rupee denomination, and the denomination is decided by the
 * account's *type* — never by its user-chosen colour — so the system stays learnable: a card is
 * always the ₹2000 magenta, a deposit always the ₹200 yellow. Tints live in index.css
 * (`--note-<value>` and `--note-<value>-ink`), each with a hand-tuned dark-mode pair.
 */
export type Denomination = '10' | '20' | '50' | '100' | '200' | '500' | '2000';

export const ACCOUNT_DENOMINATION: Record<AccountType, Denomination> = {
  checking: '100', // lavender
  savings: '500', // stone
  credit: '2000', // magenta
  fd: '200', // yellow
  rd: '200',
  cash: '10', // chocolate
  wallet: '50', // cyan
  investment: '20', // greenish-yellow
};

/** Inline style that paints an element in its account type's note tint. */
export function noteStyle(type: AccountType): React.CSSProperties {
  const d = ACCOUNT_DENOMINATION[type] ?? '100';
  return { backgroundImage: `var(--note-${d})`, color: `var(--note-${d}-ink)` };
}

/** Human labels for account types — the row captions and note tiles show these, never the raw key. */
export const ACCOUNT_TYPE_LABEL: Record<AccountType, string> = {
  checking: 'Bank account',
  savings: 'Savings',
  cash: 'Cash',
  credit: 'Credit card',
  investment: 'Investment',
  wallet: 'Wallet',
  fd: 'Fixed deposit',
  rd: 'Recurring deposit',
};
