import { describe, expect, it } from 'vitest';
import { BACKUP_SCHEMA_VERSION, readBackupMeta, withBackupMeta } from './backupMeta';

const NOW = new Date('2026-10-01T10:00:00.000Z');

describe('withBackupMeta', () => {
  it('stamps version and exportedAt while keeping the payload', () => {
    const out = withBackupMeta({ accounts: [] }, NOW);
    expect(out).toEqual({
      version: BACKUP_SCHEMA_VERSION,
      exportedAt: '2026-10-01T10:00:00.000Z',
      accounts: [],
    });
  });

  it('does not mutate the payload', () => {
    const payload = { accounts: [] };
    withBackupMeta(payload, NOW);
    expect(payload).toEqual({ accounts: [] });
  });
});

describe('readBackupMeta', () => {
  it('round-trips a stamped file', () => {
    expect(readBackupMeta(withBackupMeta({}, NOW))).toEqual({
      version: BACKUP_SCHEMA_VERSION,
      exportedAt: NOW.toISOString(),
    });
  });

  it('returns nothing for legacy or non-object input', () => {
    expect(readBackupMeta({ accounts: [] })).toEqual({});
    expect(readBackupMeta(null)).toEqual({});
    expect(readBackupMeta('x')).toEqual({});
  });

  it('drops malformed fields independently', () => {
    expect(readBackupMeta({ version: '2', exportedAt: 'nope' })).toEqual({});
    expect(readBackupMeta({ version: 0, exportedAt: NOW.toISOString() })).toEqual({
      exportedAt: NOW.toISOString(),
    });
    expect(readBackupMeta({ version: 1.5 })).toEqual({});
  });
});
