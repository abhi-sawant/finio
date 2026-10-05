import { describe, expect, it } from 'vitest';
import {
  formatCurrency,
  formatDayMonth,
  formatFullDate,
  formatShortDate,
  localDayKey,
  shouldCompactGroup,
  todayKey,
} from './formatters';

describe('formatCurrency hidden masking', () => {
  it('masks the amount behind dots but keeps the currency symbol', () => {
    expect(formatCurrency(123456, false, true)).toBe('₹••••');
  });

  it('keeps a leading minus sign so direction stays visible without the amount', () => {
    expect(formatCurrency(-500, false, true)).toBe('-₹••••');
  });

  it('masks the same way regardless of the compact flag', () => {
    expect(formatCurrency(999999, true, true)).toBe('₹••••');
  });

  it('is unaffected when hidden is false', () => {
    expect(formatCurrency(1234, false, false)).toBe('₹1,234');
  });
});

describe('formatCurrency precision', () => {
  it('shows paise by default', () => {
    expect(formatCurrency(3214.64)).toBe('₹3,214.64');
  });

  it('rounds to whole rupees when precise is false', () => {
    expect(formatCurrency(3214.64, false, false, { precise: false })).toBe('₹3,215');
  });

  it('rounds a negative amount to whole rupees too', () => {
    expect(formatCurrency(-1867.25, false, false, { precise: false })).toBe('-₹1,867');
  });
});

describe('formatCurrency paise padding', () => {
  it('pads a single fractional digit to two', () => {
    expect(formatCurrency(450.5)).toBe('₹450.50');
    expect(formatCurrency(1306.5)).toBe('₹1,306.50');
    expect(formatCurrency(-450.5)).toBe('-₹450.50');
  });

  it('leaves whole amounts bare, including ones that round to whole', () => {
    expect(formatCurrency(450)).toBe('₹450');
    expect(formatCurrency(450.999)).toBe('₹451');
  });
});

describe('date formats', () => {
  it('formatShortDate / formatFullDate / formatDayMonth', () => {
    expect(formatShortDate('2026-10-05T12:00:00')).toBe('5 Oct 2026');
    expect(formatFullDate('2026-10-05T12:00:00')).toBe('5 October 2026');
    expect(formatDayMonth('2026-10-05T12:00:00')).toBe('5 Oct');
  });
});

describe('shouldCompactGroup', () => {
  it('is false when every member is under the threshold', () => {
    expect(shouldCompactGroup([90_010, 45_000])).toBe(false);
  });

  it('is true the moment any member crosses the threshold', () => {
    expect(shouldCompactGroup([230_000, 90_010])).toBe(true);
  });

  it('checks magnitude, not sign', () => {
    expect(shouldCompactGroup([-150_000, 500])).toBe(true);
  });
});

describe('formatCurrency forceCompact', () => {
  it('compacts a value that would not cross the threshold on its own', () => {
    // Paired with a value like ₹2.3L, ₹90,010 should read as ₹90K rather than mixing
    // notations — this is what shouldCompactGroup + forceCompact is for.
    expect(formatCurrency(90_010, true, false, { forceCompact: true })).toBe('₹90K');
  });

  it('leaves the normal per-value gate alone when forceCompact is false', () => {
    expect(formatCurrency(90_010, true, false, { forceCompact: false })).toBe('₹90,010');
  });

  it('leaves the normal per-value gate alone when forceCompact is omitted', () => {
    expect(formatCurrency(230_000, true, false)).toBe('₹2.3L');
    expect(formatCurrency(90_010, true, false)).toBe('₹90,010');
  });
});

describe('localDayKey / todayKey', () => {
  // Local midnight on 1 Oct in IST (UTC+5:30) is still 30 Sep in UTC.
  it('keys an instant by the local calendar day, not the UTC date', () => {
    expect(new Date('2026-10-01T00:00:00+05:30').getTimezoneOffset()).toBe(-330);
    expect(localDayKey('2026-09-30T18:30:00.000Z')).toBe('2026-10-01');
  });

  it('formats today from local fields', () => {
    expect(todayKey(new Date(2026, 9, 1, 0, 10))).toBe('2026-10-01');
  });
});
