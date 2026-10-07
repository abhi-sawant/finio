import { vi } from 'vitest';
import {
  BACKUP_KEY_ITERATIONS,
  createVerifier,
  decryptJson,
  deriveEncryptionKey,
  encryptJson,
  generateBackupSalt,
  isEncryptedEnvelope,
  packEnvelope,
  verifyPassphraseAgainstConfig,
} from '@/utils/backupCrypto';
import { fromBase64Url, toBase64Url } from '@/utils/pinCrypto';
import { gc, type GoldenCase } from '../golden';

/**
 * The interop proof for end-to-end encrypted backups. `encryptJson` draws its IV from
 * `crypto.getRandomValues`, so it is pinned here to fixed bytes and the REAL function runs; the
 * Kotlin port must (a) produce byte-identical ciphertext for the same passphrase/salt/IV/payload
 * and (b) decrypt these web envelopes back to the same JSON.
 */
async function withFixedRandom<T>(bytes: number[], fn: () => Promise<T>): Promise<T> {
  const spy = vi.spyOn(crypto, 'getRandomValues').mockImplementation((array) => {
    const view = new Uint8Array(array.buffer, array.byteOffset, array.byteLength);
    view.set(bytes.slice(0, view.length));
    return array;
  });
  try {
    return await fn();
  } finally {
    spy.mockRestore();
  }
}

async function attempt<T>(fn: () => Promise<T>): Promise<T | { threw: true }> {
  try {
    return await fn();
  } catch {
    return { threw: true };
  }
}

const IV_A = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
const IV_B = [0xff, 0xfe, 0xfd, 0xfc, 0xfb, 0xfa, 0xf9, 0xf8, 0xf7, 0xf6, 0xf5, 0xf4];
const SALT_A = 'q83vASNFZ4mrze8BI0VniQ';
const SALT_B = 'AAAAAAAAAAAAAAAAAAAAAA';

const payloads: unknown[] = [
  { accounts: [{ id: 'a1', balance: 4200 }], note: 'ñ ünïcode 🎉' },
  {},
  [],
  'finio-backup-verify-v1',
  0,
  null,
  true,
  [1, 1.5, -2.25, 0.1, 0.30000000000000004, 1e21, 1.5e-7, 123456789012345680000, 5e-324, -0, 1e300],
  {
    b: 1,
    a: 2,
    '2': 'two',
    '10': 'ten',
    '1': 'one',
    '01': 'zero-one',
    '-1': 'neg',
    '4294967295': 'max',
  },
  {
    s: 'quote " backslash \\ slash / tab \t nl \n cr \r ff \f bs \b nul \u0000 us \u001f del \u007f',
    u: '   nbsp  bom﻿ lone\ud800 pair😀 rev\udc00\ud800',
  },
  {
    version: 3,
    exportedAt: '2026-10-05T09:30:00.000Z',
    accounts: [
      {
        id: 'acc-1',
        name: 'HDFC Savings',
        type: 'savings',
        color: '#4b36c7',
        icon: 'landmark',
        balance: 125430.5,
        openingBalance: 100000,
        createdAt: '2026-01-01T00:00:00.000Z',
      },
    ],
    transactions: Array.from({ length: 40 }, (_, i) => ({
      id: `tx-${i}`,
      type: i % 3 === 0 ? 'income' : 'expense',
      amount: Math.round((i * 137.31 + 0.07) * 100) / 100,
      accountId: 'acc-1',
      categoryId: `cat-${i % 5}`,
      date: new Date(Date.UTC(2026, i % 12, (i % 28) + 1)).toISOString(),
      note: i % 4 === 0 ? 'UPI/Swiggy/9921' : `Note ${i}`,
      labels: i % 2 ? ['lbl-essential'] : [],
      createdAt: '2026-06-01T00:00:00.000Z',
    })),
    settings: { theme: 'system', userName: 'Abhishek', monthStartDay: 25, hideAmounts: false },
  },
];

export default async function cases(): Promise<GoldenCase[]> {
  const out: GoldenCase[] = [];
  out.push(gc('constants', [], { BACKUP_KEY_ITERATIONS }));

  // encryptJson (real function, pinned IV) → the web ciphertext. Kotlin re-encrypts and decrypts.
  const grid: [string, string, number | null, number[], unknown][] = [];
  for (const payload of payloads) grid.push(['hunter2 hunter2', SALT_A, 1000, IV_A, payload]);
  grid.push(['correct horse battery staple', SALT_B, 1, IV_B, payloads[0]]);
  grid.push(['', SALT_A, 1000, IV_A, payloads[0]]);
  grid.push(['पासवर्ड 🔐 \ud800', SALT_B, 1000, IV_B, payloads[0]]);
  grid.push(['correct horse battery staple', SALT_A, null, IV_A, payloads[10]]); // real 600k

  for (const [passphrase, salt, iterations, iv, payload] of grid) {
    const key = await deriveEncryptionKey(passphrase, salt, iterations ?? undefined);
    const encrypted = await withFixedRandom(iv, () => encryptJson(key, payload));
    out.push(gc('encryptJson', [passphrase, salt, iterations, iv, payload], encrypted));
    // The same envelope, decrypted by the real decryptJson.
    out.push(
      gc(
        'decryptJson',
        [passphrase, salt, iterations, encrypted.iv, encrypted.ciphertext],
        await decryptJson(key, encrypted.iv, encrypted.ciphertext),
      ),
    );
  }

  // Decrypt failures: wrong passphrase, wrong salt, tampered ciphertext / tag / iv, bad base64.
  const key = await deriveEncryptionKey('right passphrase', SALT_A, 1000);
  const good = await withFixedRandom(IV_A, () => encryptJson(key, { secret: true }));
  const flip = (b64: string, at: number) => {
    const b = fromBase64Url(b64);
    b[at] ^= 1;
    return toBase64Url(b);
  };
  const failures: [string, string, string, string][] = [
    ['right passphrase', SALT_A, good.iv, good.ciphertext],
    ['wrong passphrase', SALT_A, good.iv, good.ciphertext],
    ['right passphrase', SALT_B, good.iv, good.ciphertext],
    ['right passphrase', SALT_A, good.iv, flip(good.ciphertext, 0)],
    [
      'right passphrase',
      SALT_A,
      good.iv,
      flip(good.ciphertext, fromBase64Url(good.ciphertext).length - 1),
    ],
    ['right passphrase', SALT_A, flip(good.iv, 3), good.ciphertext],
    ['right passphrase', SALT_A, good.iv, good.ciphertext.slice(0, -2)],
    ['right passphrase', SALT_A, good.iv, '???'],
    ['right passphrase', SALT_A, '???', good.ciphertext],
  ];
  for (const [passphrase, salt, iv, ciphertext] of failures) {
    const k = await deriveEncryptionKey(passphrase, salt, 1000);
    out.push(
      gc(
        'decryptJson',
        [passphrase, salt, 1000, iv, ciphertext],
        await attempt(() => decryptJson(k, iv, ciphertext)),
      ),
    );
  }

  // createVerifier (pinned IV) + verifyPassphraseAgainstConfig.
  for (const [passphrase, salt, iv] of [
    ['my passphrase', SALT_A, IV_A],
    ['my passphrase', SALT_B, IV_B],
  ] as const) {
    const k = await deriveEncryptionKey(passphrase, salt, 1000);
    const verifier = await withFixedRandom([...iv], () => createVerifier(k));
    out.push(gc('createVerifier', [passphrase, salt, 1000, iv], verifier));
    for (const candidate of [passphrase, 'not my passphrase', '']) {
      const ck = await deriveEncryptionKey(candidate, salt, 1000);
      const v = { verifierIv: verifier.iv, verifierCiphertext: verifier.ciphertext };
      out.push(
        gc(
          'verifyPassphraseAgainstConfig',
          [candidate, salt, 1000, v],
          await verifyPassphraseAgainstConfig(ck, v),
        ),
      );
    }
  }
  // A verifier-shaped ciphertext of a different JSON value must not verify; nor malformed input.
  {
    const k = await deriveEncryptionKey('my passphrase', SALT_A, 1000);
    const other = await withFixedRandom(IV_A, () => encryptJson(k, 'finio-backup-verify-v2'));
    const wrapped = await withFixedRandom(IV_A, () => encryptJson(k, ['finio-backup-verify-v1']));
    for (const v of [
      { verifierIv: other.iv, verifierCiphertext: other.ciphertext },
      { verifierIv: wrapped.iv, verifierCiphertext: wrapped.ciphertext },
      { verifierIv: 'not-base64', verifierCiphertext: '???' },
      { verifierIv: '', verifierCiphertext: '' },
    ]) {
      out.push(
        gc(
          'verifyPassphraseAgainstConfig',
          ['my passphrase', SALT_A, 1000, v],
          await verifyPassphraseAgainstConfig(k, v),
        ),
      );
    }
  }

  // The envelope that actually travels to the server.
  {
    const salt = await withFixedRandom([9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6], async () =>
      generateBackupSalt(),
    );
    out.push(gc('generateBackupSalt', [[9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6]], salt));
    const k = await deriveEncryptionKey('travel passphrase', salt, 1000);
    const { iv, ciphertext } = await withFixedRandom(IV_B, () => encryptJson(k, payloads[0]));
    const envelope = packEnvelope({ salt, iterations: 1000, iv, ciphertext });
    out.push(gc('packEnvelope', [{ salt, iterations: 1000, iv, ciphertext }], envelope));
    out.push(
      gc(
        'decryptEnvelope',
        ['travel passphrase', envelope],
        await decryptJson(k, envelope.iv, envelope.ciphertext),
      ),
    );
  }

  for (const raw of [
    packEnvelope({ salt: 's', iterations: 1000, iv: 'i', ciphertext: 'c' }),
    { v: 1, enc: true, salt: 's', iv: 'i', ciphertext: 'c', iterations: 1.5 },
    { v: 1.0, enc: true, salt: 's', iv: 'i', ciphertext: 'c', iterations: 0 },
    { v: 2, enc: true, salt: 's', iv: 'i', ciphertext: 'c', iterations: 1000 },
    { v: '1', enc: true, salt: 's', iv: 'i', ciphertext: 'c', iterations: 1000 },
    { v: 1, enc: 'true', salt: 's', iv: 'i', ciphertext: 'c', iterations: 1000 },
    { v: 1, enc: 1, salt: 's', iv: 'i', ciphertext: 'c', iterations: 1000 },
    { v: 1, enc: true, salt: 1, iv: 'i', ciphertext: 'c', iterations: 1000 },
    { v: 1, enc: true, salt: 's', iv: null, ciphertext: 'c', iterations: 1000 },
    { v: 1, enc: true, salt: 's', iv: 'i', iterations: 1000 },
    { v: 1, enc: true, salt: 's', iv: 'i', ciphertext: 'c', iterations: '1000' },
    { v: 1, enc: true, salt: 's', iv: 'i', ciphertext: 'c' },
    { enc: true, v: 1 },
    { accounts: [], transactions: [] },
    [],
    'a string',
    null,
    42,
  ]) {
    out.push(gc('isEncryptedEnvelope', [raw], isEncryptedEnvelope(raw)));
  }
  return out;
}
