import type { Account, NetWorthSnapshot, Transaction } from '@/types';
import {
  DEFAULT_NET_WORTH_MONTHS,
  MAX_SNAPSHOT_BACKFILL,
  accountBalancesAt,
  buildNetWorthSeries,
  netWorthAt,
  netWorthComponents,
  planNetWorthSnapshots,
  snapshotPeriodKey,
} from '@/utils/netWorth';
import { at, gc, type GoldenCase } from '../golden';
import { L, NOW, buildLedger } from './calculations.fixture';

// Net worth over the shared fixture ledger (see calculations.fixture.ts), plus the shapes from
// netWorth.test.ts. `"@ledger.<field>"` args point into the `ledger` case.

export default function cases(): GoldenCase[] {
  const ledger = buildLedger();
  const { accounts, transactions, snapshots } = ledger;
  const out: GoldenCase[] = [gc('ledger', [], ledger)];
  out.push(gc('constants', [], { DEFAULT_NET_WORTH_MONTHS, MAX_SNAPSHOT_BACKFILL }));

  out.push(gc('netWorthComponents', [L('accounts')], netWorthComponents(accounts)));
  const small: Account[] = [
    { ...accounts[0], id: 'a', balance: 0.1 },
    { ...accounts[0], id: 'b', balance: 0.2 },
    { ...accounts[3], id: 'c', balance: -0.3 },
    { ...accounts[0], id: 'd', balance: 0 },
    { ...accounts[0], id: 'e', balance: 999, archivedAt: '2026-01-01T00:00:00.000Z' },
  ];
  out.push(gc('netWorthComponents', [small], netWorthComponents(small)));
  out.push(gc('netWorthComponents', [[]], netWorthComponents([])));

  for (const d of [
    NOW,
    at(2026, 6, 1, 0, 0),
    at(2026, 5, 31, 23, 59, 59, 999),
    at(2026, 5, 24, 23, 59),
    at(2026, 5, 25, 0, 0),
    at(2026, 1, 10),
    at(2025, 12, 31, 23),
  ]) {
    for (const msd of [1, 25, 28]) {
      out.push(gc('snapshotPeriodKey', [d, msd], snapshotPeriodKey(d, msd)));
    }
  }

  const asOfs = [
    NOW,
    at(2030, 1, 1),
    at(2025, 10, 1),
    at(2026, 5, 31, 23, 59, 59, 999),
    at(2026, 5, 31, 0, 0),
    at(2026, 3, 15, 12),
    at(2026, 6, 1, 9, 0),
  ];
  for (const asOf of asOfs) {
    out.push(
      gc(
        'accountBalancesAt',
        [L('accounts'), L('transactions'), asOf],
        Object.fromEntries(accountBalancesAt(accounts, transactions, asOf)),
      ),
    );
    out.push(gc('netWorthAt', [L('accounts'), L('transactions'), asOf], netWorthAt(accounts, transactions, asOf)));
  }
  const junk: Transaction[] = [
    { ...transactions[0], id: 'bad', date: 'garbage', amount: 1e6 },
  ];
  out.push(
    gc('netWorthAt', [L('accounts'), junk, at(2020, 1, 1)], netWorthAt(accounts, junk, at(2020, 1, 1))),
  );

  // Series: snapshots for some closed months, one for the live month (ignored), msd 1 and 25.
  const seriesInputs: Array<{
    snapshots: NetWorthSnapshot[] | string;
    now: Date;
    monthStartDay?: number;
    months?: number;
  }> = [
    { snapshots: L('snapshots'), now: NOW },
    { snapshots: L('snapshots'), now: NOW, monthStartDay: 25 },
    { snapshots: L('snapshots'), now: NOW, months: 3 },
    { snapshots: L('snapshots'), now: NOW, months: 1 },
    { snapshots: L('snapshots'), now: NOW, months: 0 },
    { snapshots: L('snapshots'), now: NOW, months: -2 },
    { snapshots: [], now: at(2026, 1, 5), months: 4 },
    { snapshots: L('snapshots'), now: at(2026, 5, 24, 23), monthStartDay: 25, months: 6 },
  ];
  for (const input of seriesInputs) {
    const snaps = typeof input.snapshots === 'string' ? snapshots : input.snapshots;
    out.push(
      gc(
        'buildNetWorthSeries',
        [{ ...input, accounts: L('accounts'), transactions: L('transactions') }],
        buildNetWorthSeries({ ...input, snapshots: snaps, accounts, transactions }),
      ),
    );
  }

  const planInputs: Array<{
    accounts?: string | Account[];
    transactions?: string | Transaction[];
    snapshots: NetWorthSnapshot[] | string;
    now: Date;
    monthStartDay?: number;
    maxBackfill?: number;
  }> = [
    { snapshots: L('snapshots'), now: NOW },
    { snapshots: [], now: NOW },
    { snapshots: [], now: NOW, monthStartDay: 25 },
    { snapshots: L('snapshots'), now: NOW, maxBackfill: 3 },
    { snapshots: [], now: NOW, maxBackfill: 0 },
    { snapshots: [], now: NOW, maxBackfill: -4 },
    { snapshots: [], now: at(2027, 12, 1), maxBackfill: 30 },
    { snapshots: [], now: NOW, accounts: [] },
    { snapshots: [], now: NOW, transactions: [] },
    { snapshots: [], now: NOW, transactions: [{ ...transactions[0], date: 'bad' }] },
  ];
  for (const input of planInputs) {
    const resolved = {
      ...input,
      accounts: input.accounts && typeof input.accounts !== 'string' ? input.accounts : accounts,
      transactions:
        input.transactions && typeof input.transactions !== 'string' ? input.transactions : transactions,
      snapshots: typeof input.snapshots === 'string' ? snapshots : input.snapshots,
    };
    out.push(
      gc(
        'planNetWorthSnapshots',
        [
          {
            ...input,
            accounts: input.accounts ?? L('accounts'),
            transactions: input.transactions ?? L('transactions'),
          },
        ],
        planNetWorthSnapshots(resolved),
      ),
    );
  }
  return out;
}
