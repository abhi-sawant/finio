import { describe, expect, it } from 'vitest';
import {
  cleanText,
  firstFreeScope,
  isPastDay,
  isRangeInverted,
  isValidEmail,
  stripLeading,
} from './validation';

describe('cleanText', () => {
  it('trims and caps', () => {
    expect(cleanText('  hi  ', 10)).toBe('hi');
    expect(cleanText('abcdef', 3)).toBe('abc');
  });
  it('never splits an emoji', () => {
    expect(cleanText('😀😀😀', 2)).toBe('😀😀');
  });
});

describe('stripLeading', () => {
  it('drops leading but keeps trailing whitespace', () => {
    expect(stripLeading('   a b ')).toBe('a b ');
  });
});

describe('isValidEmail', () => {
  it('accepts normal addresses', () => {
    expect(isValidEmail('a@b.co')).toBe(true);
    expect(isValidEmail(' a@b.com ')).toBe(true);
  });
  it('rejects malformed ones', () => {
    for (const bad of ['a@b', 'a b@c.com', '@x.com', 'a@b.c', '']) {
      expect(isValidEmail(bad)).toBe(false);
    }
  });
});

describe('isPastDay / isRangeInverted', () => {
  it('compares day keys', () => {
    expect(isPastDay('2026-09-30', '2026-10-01')).toBe(true);
    expect(isPastDay('2026-10-01', '2026-10-01')).toBe(false);
  });
  it('flags only a fully-set inverted range', () => {
    expect(isRangeInverted('2026-10-02', '2026-10-01')).toBe(true);
    expect(isRangeInverted('2026-10-01', '2026-10-01')).toBe(false);
    expect(isRangeInverted('', '2026-10-01')).toBe(false);
  });
});

describe('firstFreeScope', () => {
  it('returns the first unused candidate, or null', () => {
    expect(firstFreeScope(['a', 'b'], new Set())).toBe('a');
    expect(firstFreeScope(['a', 'b'], new Set(['a']))).toBe('b');
    expect(firstFreeScope(['a'], new Set(['a']))).toBeNull();
  });
});
