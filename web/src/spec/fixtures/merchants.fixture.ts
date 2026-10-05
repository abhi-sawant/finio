import type { Transaction } from '@/types';
import { summarizeMerchants, topMerchants, type MerchantSummary } from '@/utils/merchants';
import { gc, type GoldenCase } from '../golden';
import { L, buildLedger } from './calculations.fixture';

// Merchant grouping over the shared fixture ledger plus merchants.test.ts shapes. Transactions
// in the output are reduced to ids.

const summaryOut = (s: MerchantSummary) => ({ ...s, transactions: s.transactions.map((t) => t.id) });

function tx(id: string, note: string, amount: number, date: string, type: Transaction['type'] = 'expense'): Transaction {
  return {
    id,
    type,
    amount,
    accountId: 'acc-1',
    categoryId: 'cat-1',
    date,
    note,
    labels: [],
    createdAt: date,
  };
}

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const txns = ledger.transactions;
  const out: GoldenCase[] = [gc('ledger', [], ledger)];

  for (const type of ['expense', 'income'] as const) {
    out.push(gc('summarizeMerchants', [L('transactions'), type], summarizeMerchants(txns, type).map(summaryOut)));
  }
  for (const n of [0, 3, 100, -2]) {
    out.push(gc('topMerchants', [L('transactions'), n, 'expense'], topMerchants(txns, n).map(summaryOut)));
  }

  const small = [
    tx('a', 'Swiggy/9921', 300, '2026-06-01T10:00:00.000Z'),
    tx('b', 'Swiggy 449', 449, '2026-06-03T10:00:00.000Z'),
    tx('c', '  Swiggy 449 ', 120.1, '2026-06-02T10:00:00.000Z'),
    tx('d', 'swiggy', 0.2, '2026-06-02T10:00:00.000Z'),
    tx('e', 'UPI/Swiggy/9921', 99, '2026-06-04T10:00:00.000Z'),
    tx('f', 'Swiggy 449', 10, '2026-06-03', 'income'),
    tx('g', '', 500, '2026-06-05T10:00:00.000Z'),
    tx('h', '1234 / ##', 500, '2026-06-05T10:00:00.000Z'),
    tx('i', 'Rent', 22000, '2026-06-01T10:00:00.000Z', 'transfer'),
    tx('j', 'Café Coffee Day', 250.25, '2026-06-06T10:00:00.000Z'),
    tx('k', 'Cafe Coffee Day', 249.75, '2026-06-06T10:00:00.000Z'),
    tx('l', 'Tie A', 100, '2026-06-01T10:00:00.000Z'),
    tx('m', 'Tie-A', 100, '2026-06-01T10:00:00.000Z'),
  ];
  for (const type of ['expense', 'income'] as const) {
    out.push(gc('summarizeMerchants', [small, type], summarizeMerchants(small, type).map(summaryOut)));
  }
  out.push(gc('summarizeMerchants', [[], 'expense'], summarizeMerchants([], 'expense').map(summaryOut)));
  out.push(gc('topMerchants', [small, 2, 'expense'], topMerchants(small, 2).map(summaryOut)));
  return out;
}
