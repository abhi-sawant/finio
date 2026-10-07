/**
 * Provenance stamped onto local backup files (manual export and the auto-backup folder).
 * Cloud uploads are deliberately left alone — the server treats the body as opaque and its
 * E2EE envelope has its own versioning.
 *
 * Bump `BACKUP_SCHEMA_VERSION` when a backup's shape changes in a way an older app would
 * misread; `validateBackup` warns when a file claims a newer version than this build knows.
 */
export const BACKUP_SCHEMA_VERSION = 1;

export interface BackupMeta {
  version: number;
  /** ISO timestamp of when the file was written. */
  exportedAt: string;
}

export function withBackupMeta<T extends object>(
  payload: T,
  now: Date = new Date(),
): BackupMeta & T {
  return { version: BACKUP_SCHEMA_VERSION, exportedAt: now.toISOString(), ...payload };
}

/**
 * Reads the stamp back out of an untrusted parsed file. Each field is independent: a legacy
 * file yields `{}`, and a malformed value is dropped rather than trusted.
 */
export function readBackupMeta(raw: unknown): Partial<BackupMeta> {
  if (typeof raw !== 'object' || raw === null) return {};
  const r = raw as Record<string, unknown>;
  const meta: Partial<BackupMeta> = {};
  if (typeof r.version === 'number' && Number.isInteger(r.version) && r.version >= 1) {
    meta.version = r.version;
  }
  if (typeof r.exportedAt === 'string' && !Number.isNaN(Date.parse(r.exportedAt))) {
    meta.exportedAt = r.exportedAt;
  }
  return meta;
}
