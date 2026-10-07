import {
  MAX_PIN_LENGTH,
  MIN_PIN_LENGTH,
  PIN_HASH_BITS,
  PIN_HASH_ITERATIONS,
  PIN_LENGTH_OPTIONS,
  PIN_SALT_BYTES,
  derivePinHash,
  fromBase64Url,
  isValidPin,
  timingSafeEqualB64,
  toBase64Url,
  verifyPin,
} from '@/utils/pinCrypto';
import { gc, type GoldenCase } from '../golden';

/** `{ threw: true }` stands in for an exception, so the Kotlin side can assert it throws too. */
async function attempt<T>(fn: () => T | Promise<T>): Promise<T | { threw: true }> {
  try {
    return await fn();
  } catch {
    return { threw: true };
  }
}

const bytes = (length: number, seed: number) =>
  Array.from({ length }, (_, i) => (i * 37 + seed * 11 + 251) % 256);

export default async function cases(): Promise<GoldenCase[]> {
  const out: GoldenCase[] = [];
  out.push(
    gc('constants', [], {
      PIN_HASH_ITERATIONS,
      PIN_SALT_BYTES,
      PIN_HASH_BITS,
      MIN_PIN_LENGTH,
      MAX_PIN_LENGTH,
      PIN_LENGTH_OPTIONS,
    }),
  );

  // base64url at every padding boundary, plus the bytes that map to '+' and '/'.
  const arrays = [
    ...[0, 1, 2, 3, 4, 5, 15, 16, 17, 31, 32, 33].map((n) => bytes(n, n)),
    [0xfb, 0xff, 0xbf, 0xfe],
    [0, 0, 0],
    [255, 255, 255, 255],
  ];
  for (const a of arrays) out.push(gc('toBase64Url', [a], toBase64Url(new Uint8Array(a))));

  const encoded = [
    '',
    'AA',
    'AAA',
    'AAAA',
    'A',
    'AAAAA',
    '-_8',
    '-_-_',
    '+/+/',
    'AQID',
    'AQI=',
    'AQ==',
    'AQ=',
    'A===',
    'AQID=',
    'A=QI',
    'AQ I D',
    ' AQID',
    'AQ\nID',
    'AQ\tI\fD\r',
    'AQ ID',
    'not-base64',
    '???',
    'abc$',
    'ÿÿ',
    'AB', // non-zero leftover bits are discarded
    'AP',
    'AAAAAAAAAAAAAAAAAAAAAA',
    'q83vASNFZ4mrze8BI0VniQ',
  ];
  for (const s of encoded) {
    out.push(gc('fromBase64Url', [s], await attempt(() => Array.from(fromBase64Url(s)))));
  }

  for (const pin of [
    '',
    '1',
    '123',
    '0000',
    '1234',
    '12345',
    '123456',
    '1234567',
    '12345678',
    '123456789',
    '12a4',
    '1234 ',
    ' 1234',
    '１２３４',
    '١٢٣٤',
    '12.4',
    '-123',
    '😀😀',
    '😀😀😀😀',
  ]) {
    out.push(gc('isValidPin', [pin], isValidPin(pin)));
  }

  const salts = [
    'AAAAAAAAAAAAAAAAAAAAAA',
    'q83vASNFZ4mrze8BI0VniQ',
    '-_-_-_-_-_-_-_-_-_-_-w',
    'c2FsdA',
    '',
    'AQ ID',
    '???',
  ];
  const pins = ['0000', '1234', '4321', '12345678', '', 'ñ', '\ud800', 'pässwörd 🎉', '１２３４'];
  for (const salt of salts) {
    for (const pin of pins) {
      for (const iterations of [1, 1000]) {
        out.push(
          gc(
            'derivePinHash',
            [pin, salt, iterations],
            await attempt(() => derivePinHash(pin, salt, iterations)),
          ),
        );
      }
    }
  }
  out.push(
    gc('derivePinHash', ['1234', salts[0], 1001], await derivePinHash('1234', salts[0], 1001)),
  );
  out.push(
    gc(
      'derivePinHash',
      ['1234', salts[0], 0],
      await attempt(() => derivePinHash('1234', salts[0], 0)),
    ),
  );
  // The real production cost, through the default parameter.
  out.push(
    gc(
      'derivePinHash',
      ['1234', salts[1], null],
      await derivePinHash('1234', salts[1]),
      'default 310000',
    ),
  );
  out.push(
    gc(
      'derivePinHash',
      ['000000', salts[0], null],
      await derivePinHash('000000', salts[0]),
      'default 310000',
    ),
  );

  const h = toBase64Url(new Uint8Array([1, 2, 3, 4]));
  for (const [a, b] of [
    [h, h],
    [h, toBase64Url(new Uint8Array([1, 2, 3, 5]))],
    [toBase64Url(new Uint8Array([1, 2])), toBase64Url(new Uint8Array([1]))],
    ['', ''],
    ['AQID', 'AQI D'],
    ['AQID', 'AQID='],
    ['AQID', '???'],
    ['A', 'A'],
    ['AB', 'AA'], // same bytes, different (discarded) trailing bits
  ]) {
    out.push(gc('timingSafeEqualB64', [a, b], timingSafeEqualB64(a, b)));
  }

  const salt = 'q83vASNFZ4mrze8BI0VniQ';
  const hash = await derivePinHash('1234', salt, 1000);
  const records = [
    { salt, hash, iterations: 1000 },
    { salt, hash, iterations: 2000 },
    { salt: 'AAAAAAAAAAAAAAAAAAAAAA', hash, iterations: 1000 },
    { salt, hash: hash.slice(0, 10), iterations: 1000 },
    { salt, hash: hash + '=', iterations: 1000 },
    { salt, hash: '???', iterations: 1000 },
    { salt: '???', hash, iterations: 1000 },
    { salt, hash, iterations: 0 },
  ];
  for (const record of records) {
    for (const pin of ['1234', '4321', '12345']) {
      out.push(gc('verifyPin', [pin, record], await verifyPin(pin, record)));
    }
  }
  return out;
}
