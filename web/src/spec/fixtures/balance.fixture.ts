import {
  applyBalanceDelta,
  backfillOpeningBalances,
  diffBalances,
  reconciliationAdjustment,
  recomputeAccountBalances,
  roundMoney,
  sumTransactionDeltas,
} from '@/store/balance';
import type { Account, Transaction } from '@/types';
import { gc, type GoldenCase } from '../golden';

const acct = (id: string, balance: number, openingBalance?: number): Account =>
  ({
    id,
    name: id,
    type: 'checking',
    color: '#000000',
    icon: 'landmark',
    balance,
    ...(openingBalance === undefined ? {} : { openingBalance }),
    createdAt: '2026-01-01T00:00:00.000Z',
  }) as Account;

type Tx = Pick<Transaction, 'type' | 'accountId' | 'toAccountId' | 'amount'>;
const txs: Tx[] = [
  { type: 'expense', accountId: 'a', amount: 120.35 },
  { type: 'income', accountId: 'a', amount: 50000 },
  { type: 'transfer', accountId: 'a', toAccountId: 'b', amount: 0.1 },
  { type: 'transfer', accountId: 'b', toAccountId: 'c', amount: 0.2 },
  { type: 'expense', accountId: 'c', amount: 33.333 },
  { type: 'income', accountId: 'b', amount: 1e-3 },
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  for (const v of [0, 1.005, 2.675, -2.5, -0.125, 0.1 + 0.2, 123456.785, -1234.5649, 1e-7]) {
    out.push(gc('roundMoney', [v], roundMoney(v)));
  }
  const accounts = [acct('a', 1000, 1000), acct('b', 0, 0), acct('c', -500, -500)];
  for (const tx of txs) {
    for (const dir of [1, -1] as const) {
      out.push(gc('applyBalanceDelta', [accounts, tx, dir], applyBalanceDelta(accounts, tx, dir)));
    }
  }
  out.push(gc('sumTransactionDeltas', [txs], Object.fromEntries(sumTransactionDeltas(txs))));
  const legacy = [acct('a', 49879.55), acct('b', 0.1, 0), acct('c', 99.7)];
  out.push(gc('backfillOpeningBalances', [legacy, txs], backfillOpeningBalances(legacy, txs)));
  out.push(gc('recomputeAccountBalances', [legacy, txs], recomputeAccountBalances(legacy, txs)));
  out.push(gc('recomputeAccountBalances', [accounts, txs], recomputeAccountBalances(accounts, txs)));
  for (const [cur, stmt] of [
    [100, 100],
    [100, 150.25],
    [100, 99.995],
    [-2500, -2400.1],
    [0.1 + 0.2, 0.3],
  ]) {
    out.push(gc('reconciliationAdjustment', [cur, stmt], reconciliationAdjustment(cur, stmt)));
  }
  const after = recomputeAccountBalances(accounts, txs);
  out.push(gc('diffBalances', [accounts, after], diffBalances(accounts, after)));
  return out;
}
