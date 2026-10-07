import {
  MAX_NAME_LENGTH,
  MAX_NOTE_LENGTH,
  MAX_PATTERN_LENGTH,
  cleanText,
  firstFreeScope,
  isPastDay,
  isRangeInverted,
  isValidEmail,
  stripLeading,
} from '@/utils/validation';
import { gc, type GoldenCase } from '../golden';

const texts = [
  '',
  'hello',
  '  hello  ',
  '\t\n hello world \r\n',
  ' nbsp ',
  '　ideographic　',
  '﻿bom﻿',
  ' line ',
  '\u001cnot-js-space\u001f',
  '\u0085nel\u0085',
  '​zero-width​',
  'a'.repeat(45),
  '😀'.repeat(45),
  'ab😀cd',
  '👨‍👩‍👧 family',
  'é combining',
  '\ud83d lone high',
  'x\ud83d',
  '   ',
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('constants', [], { MAX_NAME_LENGTH, MAX_NOTE_LENGTH, MAX_PATTERN_LENGTH }));
  for (const t of texts) {
    for (const max of [0, 1, 2, 3, 5, 40, 200, -1, -3]) {
      out.push(gc('cleanText', [t, max], cleanText(t, max)));
    }
    out.push(gc('stripLeading', [t], stripLeading(t)));
  }
  for (const e of [
    '',
    'a@b.co',
    'a@b.c',
    ' a@b.co ',
    'a@@b.co',
    'a b@c.co',
    '@b.co',
    'a@.co',
    'a@b.',
    'a@b..co',
    'a@b.c.d',
    'a@b.c.de',
    'first.last@sub.example.com',
    'a@b .co',
    'a@b.😀',
    'a@b.é',
    'a@b.co\n',
    'user+tag@example.in',
    'a@b',
    'a.b@c',
    'a@b.co@d.ef',
    'a@b.﻿co',
    'a@.b.co',
    'a@b.co.',
    'x@y.zz',
    'x@y.z　z',
  ]) {
    out.push(gc('isValidEmail', [e], isValidEmail(e)));
  }
  for (const [a, b] of [
    ['2026-10-04', '2026-10-05'],
    ['2026-10-05', '2026-10-05'],
    ['2026-10-06', '2026-10-05'],
    ['2025-12-31', '2026-01-01'],
    ['', '2026-01-01'],
    ['2026-01-01', ''],
    ['', ''],
  ]) {
    out.push(gc('isPastDay', [a, b], isPastDay(a, b)));
    out.push(gc('isRangeInverted', [a, b], isRangeInverted(a, b)));
  }
  const cands = ['', 'cat-1', 'cat-2', 'lbl:lbl-1'];
  for (const used of [[], [''], ['', 'cat-1'], ['', 'cat-1', 'cat-2', 'lbl:lbl-1'], ['cat-2']]) {
    out.push(gc('firstFreeScope', [cands, used], firstFreeScope(cands, new Set(used))));
  }
  out.push(gc('firstFreeScope', [[], []], firstFreeScope([], new Set())));
  return out;
}
