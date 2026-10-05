import { BACKUP_SCHEMA_VERSION, readBackupMeta, withBackupMeta } from '@/utils/backupMeta';
import { gc, type GoldenCase } from '../golden';

const NOW = new Date('2026-10-01T10:00:00.000Z');

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('BACKUP_SCHEMA_VERSION', [], BACKUP_SCHEMA_VERSION));
  for (const payload of [
    {},
    { accounts: [] },
    { accounts: [{ id: 'a' }], settings: { theme: 'dark' } },
    // Payload keys win over the stamp but the stamp's key order survives (object spread).
    { version: 7, accounts: [] },
    { exportedAt: 'payload', zeta: 1, alpha: 2 },
  ]) {
    for (const now of [NOW, new Date('1999-12-31T18:30:00.123Z')]) {
      // Encode as [key, value] pairs so key order is part of the assertion.
      out.push(gc('withBackupMeta', [payload, now], Object.entries(withBackupMeta(payload, now))));
    }
  }
  const raws: unknown[] = [
    null,
    'x',
    42,
    true,
    [],
    [1, 2],
    {},
    { accounts: [] },
    withBackupMeta({}, NOW),
    { version: '2', exportedAt: 'nope' },
    { version: 0, exportedAt: NOW.toISOString() },
    { version: 1.5 },
    { version: -1 },
    { version: 1 },
    { version: 2 },
    { version: 1e6 },
    { version: 1.0 },
    { version: null },
    { version: true },
    { version: [1] },
    { exportedAt: '' },
    { exportedAt: 123 },
    { exportedAt: null },
    { exportedAt: '2026-10-05' },
    { exportedAt: '2026-10-05T10:00' },
    { exportedAt: '2026-10-05T10:00:00+05:30' },
    { exportedAt: '2026-10-05T10:00:00.1234567Z' },
    { exportedAt: '2026-10-05 10:00' },
    { exportedAt: '2026-02-30' },
    { exportedAt: '2026-02-31' },
    { exportedAt: '2026-13-01' },
    { exportedAt: '2026-00-10' },
    { exportedAt: '2026-10-00' },
    { exportedAt: '2026-10-32' },
    { exportedAt: '2026' },
    { exportedAt: '2026-10' },
    { exportedAt: '2026-10-05T24:00:00Z' },
    { exportedAt: '2026-10-05T24:30:00Z' },
    { exportedAt: '2026-10-05T25:00:00Z' },
    { exportedAt: '2026-10-05T23:60:00Z' },
    { exportedAt: '2026-10-05T10:00Z' },
    { exportedAt: '2026-10-05T10:00:00z' },
    { exportedAt: '2026-10-05t10:00:00Z' },
    { exportedAt: '+002026-10-05T00:00:00.000Z' },
    { exportedAt: '-000001-01-01T00:00:00Z' },
    { exportedAt: '2026-10-05T10:00:00+0530' },
    { exportedAt: '2026-10-05T10:00:00+23:59' },
    { exportedAt: '2026-10-05T10:00:00+24:00' },
    { exportedAt: '2026-10-05T10:00:00+05' },
    { exportedAt: '20261005' },
    { exportedAt: 'nope' },
    { exportedAt: '1e3' },
    { version: 3, exportedAt: '2026-10-05T10:00:00.000Z', accounts: [] },
  ];
  for (const raw of raws) out.push(gc('readBackupMeta', [raw], readBackupMeta(raw)));
  return out;
}
